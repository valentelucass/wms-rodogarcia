[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$RuntimeReceipt)
$ErrorActionPreference='Stop'
if($PSVersionTable.PSEdition -ceq 'Desktop'){$env:PSModulePath=[Environment]::GetEnvironmentVariable('PSModulePath','Machine')}
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$area=Join-Path $root 'orchestracao/.runtime/coletor-profundo-20261029'
. (Join-Path $root 'infra/dev/artefato-backend.ps1')
. (Join-Path $root 'infra/notazz/runtime-validacao.ps1')
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'database/scripts/d26-credencial-aplicacao.ps1')
$report=[ordered]@{id='COLETOR-PROFUNDO01';inicioUtc=[DateTime]::UtcNow.ToString('o');estado='PREPARO';somenteSelect=$true;aberturas=0;checks=@();consultas=@();erro=$null}
$conn=$null;$credential=$null;$parameters=@{}
function Check-Col([bool]$ok,[string]$nome){$report.checks+=@{caso=$nome;passou=$ok};if(!$ok){throw ('COLETOR_SQL_'+$nome)}}
function Read-Col([string]$nome,[string]$sql){
 if($sql.TrimStart() -cnotmatch '^SELECT\b' -or $sql -match ';'){throw 'COLETOR_SQL_SELECT_INVALIDO'}
 $cmd=$conn.CreateCommand();$cmd.CommandText=$sql;$cmd.CommandTimeout=30
 $null=$cmd.Parameters.AddWithValue('@cliente',[long]$flow.fixtures.client.id)
 $null=$cmd.Parameters.AddWithValue('@armazem',[long]$flow.fixtures.warehouse.id)
 foreach($entry in $(if($parameters){$parameters.GetEnumerator()}else{@()})){$null=$cmd.Parameters.AddWithValue($entry.Key,$entry.Value)}
 $reader=$null;$rows=New-Object 'Collections.Generic.List[object]'
 try{$reader=$cmd.ExecuteReader();while($reader.Read()){$row=[ordered]@{};for($i=0;$i -lt $reader.FieldCount;$i++){$row[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}};$rows.Add([pscustomobject]$row)};$report.consultas+=@{nome=$nome;linhas=$rows.Count};$rows.ToArray()}finally{if($reader){$reader.Dispose()};$cmd.Dispose()}
}
try{
 $runtime=Assert-NotazzCurrentRuntime $RuntimeReceipt
 $flow=Get-Content (Join-Path $area 'profundo.json') -Raw|ConvertFrom-Json
 Check-Col ($flow.state -cin @('MOBILE_APROVADO','APLICACAO_APROVADA')) 'FLUXOS_HTTP_APROVADOS'
 $g=Invoke-WmsDev02Guard
 Check-Col ($g.guardaRealAprovada -and $g.tls.proveniencia.reciboPID -eq $runtime.runtime.sql.pid -and $g.tls.proveniencia.reciboInicioUtc -ceq $runtime.runtime.sql.startUtc) 'GUARDA_ATUAL'
 Write-WmsDevCreateOnlyJson (Join-Path $area ('guarda-sql-'+[guid]::NewGuid().ToString('N')+'.json')) $g
 $module=Import-Module ([IO.Path]::GetFullPath((Join-Path $root '../.runtime/sql-server/SqlServerProjetos.psm1'))) -PassThru -Force
 & $module {Import-SqlClient}
 $credential=Get-WmsDevApplicationCredential
 Check-Col ($credential.UserName -ceq 'WMSDEV') 'IDENTIDADE_PROTEGIDA'
 $b=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
 $b.set_DataSource('tcp:127.0.0.1,1433');$b.set_InitialCatalog('WMS_DEV');$b.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory);$b.set_TrustServerCertificate($false);$b.set_ServerCertificate($g.perfilProtegido.certificate);$b.set_Pooling($false);$b.set_ConnectTimeout(30);$b.set_ConnectRetryCount(0);$b.set_ApplicationName('WMS-COLETOR-PROFUNDO01-SELECT')
 $sc=New-Object Microsoft.Data.SqlClient.SqlCredential('WMSDEV',$credential.Password)
 $conn=New-Object Microsoft.Data.SqlClient.SqlConnection($b.ConnectionString,$sc);$report.aberturas=1;$conn.Open()
 $identity=@(Read-Col 'identidade' "SELECT DB_NAME() banco,ORIGINAL_LOGIN() login")
 $report.identidade=$identity
 $report.tls=@{encrypt=$b.Encrypt.ToString();trustServerCertificate=$b.TrustServerCertificate;estadoConexao=$conn.State.ToString();certificateSha256=$g.tls.certificateSha256}
 # A abertura SqlClient ja validou Mandatory/certificado privado/TSC=false; nao exigir DMV administrativa da aplicacao.
 Check-Col ($identity[0].banco -ceq 'WMS_DEV' -and $identity[0].login -ceq 'WMSDEV' -and $b.Encrypt.ToString() -cin @('True','Mandatory') -and !$b.TrustServerCertificate -and $conn.State.ToString() -ceq 'Open') 'SQL_DEV_WMSDEV_TLS'
 $parameters=@{
 '@produto'=[long]$flow.fixtures.product.id;'@entrada'=[long]$flow.fixtures.entry.id;
 '@pedido'=[long]$flow.fixtures.order.id;'@reversao'=[long]$flow.fixtures.reverseOrder.id;
 '@inventario'=[long]$flow.fixtures.inventory.id;'@cancelado'=[long]$flow.fixtures.cancelInventory.id;'@posicao'=[long]$flow.fixtures.positionInventory.id;'@mobile'=[long]$flow.fixtures.mobileInventory.id;
 '@manual'=[long]$flow.fixtures.units[0].id
 }
 $scope="((t.tipo='ENDERECAMENTO' AND t.origem_id IN (SELECT id FROM wms.unidade_logistica WHERE pedido_id=@entrada AND produto_id=@produto)) OR (t.tipo='SEPARACAO' AND t.origem_id IN (@pedido,@reversao)) OR (t.tipo='INVENTARIO' AND t.origem_id IN (@inventario,@cancelado,@posicao,@mobile)))"
 $types=@(Read-Col 'tarefas_exatas' ("SELECT t.tipo,COUNT_BIG(*) quantidade FROM wms.coletor_tarefa t WHERE t.cliente_id=@cliente AND t.armazem_id=@armazem AND "+$scope+" GROUP BY t.tipo"))
 Check-Col (@($types|Where-Object {$_.tipo -ceq 'ENDERECAMENTO' -and $_.quantidade -eq 4}).Count -eq 1 -and @($types|Where-Object {$_.tipo -ceq 'SEPARACAO' -and $_.quantidade -eq 2}).Count -eq 1 -and @($types|Where-Object {$_.tipo -ceq 'INVENTARIO' -and $_.quantidade -eq 4}).Count -eq 1) 'DEZ_TAREFAS_ORIGENS_EXATAS'
 $stock=@(Read-Col 'fisico_produto_novo' 'SELECT COUNT_BIG(*) mercadorias,SUM(u.quantidade) quantidade FROM wms.unidade_logistica u WHERE u.pedido_id=@entrada AND u.produto_id=@produto AND u.ativa=1')
 Check-Col ($stock[0].mercadorias -eq 4 -and $stock[0].quantidade -eq 20) 'QUATRO_MERCADORIAS_VINTE_UNIDADES_PRESERVADAS'
 $positions=@(Read-Col 'ocupacao_produto_novo' 'SELECT COUNT_BIG(*) posicoes,COUNT(DISTINCT o.unidade_id) mercadorias FROM wms.ocupacao_endereco o JOIN wms.unidade_logistica u ON u.id=o.unidade_id WHERE u.pedido_id=@entrada AND u.produto_id=@produto')
 Check-Col ($positions[0].posicoes -eq 5 -and $positions[0].mercadorias -eq 4) 'UNIDADE_DE_DUAS_POSICOES_NAO_DUPLICA_SALDO'
 $ops=@(Read-Col 'ledger_exato' ("SELECT o.tipo,COUNT_BIG(*) quantidade FROM wms.coletor_operacao o JOIN wms.coletor_tarefa t ON t.id=o.tarefa_id WHERE "+$scope+" GROUP BY o.tipo"))
 foreach($expected in @(@{tipo='ENDERECAR';n=3},@{tipo='SEPARAR';n=2},@{tipo='CONTAR';n=10},@{tipo='FINALIZAR';n=3},@{tipo='CANCELAR_INVENTARIO';n=1})){
  Check-Col (@($ops|Where-Object {$_.tipo -ceq $expected.tipo -and $_.quantidade -eq $expected.n}).Count -eq 1) ('LEDGER_EXATO_'+$expected.tipo)
 }
 $manual=@(Read-Col 'manual_sem_dono' 'SELECT t.responsavel,u.primeiro_enderecamento_em FROM wms.coletor_tarefa t JOIN wms.unidade_logistica u ON u.id=t.origem_id WHERE t.tipo=''ENDERECAMENTO'' AND t.origem_id=@manual AND t.cliente_id=@cliente AND t.armazem_id=@armazem')
 Check-Col ($manual.Count -eq 1 -and !$manual[0].responsavel -and $manual[0].primeiro_enderecamento_em) 'MANUAL_CONCLUIDO_SEM_RESPONSAVEL_ARTIFICIAL'
 $inventory=@(Read-Col 'inventarios_exatos' 'SELECT id,tipo,situacao,divergencias,ISJSON(relatorio_json) relatorio_valido FROM wms.inventario WHERE id IN (@inventario,@cancelado,@posicao,@mobile) AND cliente_id=@cliente AND armazem_id=@armazem')
 Check-Col (@($inventory|Where-Object {$_.id -eq $parameters['@inventario'] -and $_.tipo -ceq 'SKU' -and $_.situacao -ceq 'CONCLUIDO' -and $_.divergencias -eq 1 -and $_.relatorio_valido -eq 1}).Count -eq 1 -and @($inventory|Where-Object {$_.id -eq $parameters['@cancelado'] -and $_.tipo -ceq 'CEGAS' -and $_.situacao -ceq 'CANCELADO'}).Count -eq 1) 'INVENTARIO_DIVERGENTE_E_CANCELADO'
 Check-Col (@($inventory|Where-Object {$_.id -eq $parameters['@posicao'] -and $_.tipo -ceq 'POSICAO' -and $_.situacao -ceq 'CONCLUIDO' -and $_.divergencias -eq 0 -and $_.relatorio_valido -eq 1}).Count -eq 1) 'INVENTARIO_POSICAO_SEM_DIVERGENCIA'
 Check-Col (@($inventory|Where-Object {$_.id -eq $parameters['@mobile'] -and $_.tipo -ceq 'SKU' -and $_.situacao -ceq 'CONCLUIDO' -and $_.divergencias -eq 0 -and $_.relatorio_valido -eq 1}).Count -eq 1) 'INVENTARIO_MOVEL_FINAL_SEM_DIVERGENCIA'
 $readings=@(Read-Col 'leituras_consolidadas' 'SELECT COUNT_BIG(*) mercadorias,SUM(contado) contado FROM wms.inventario_item WHERE inventario_id=@inventario AND contado IS NOT NULL')
 Check-Col ($readings[0].mercadorias -eq 4 -and $readings[0].contado -eq 19) 'RECONTAGEM_QUATRO_REGISTROS_TOTAL_DEZENOVE'
 $orders=@(Read-Col 'pedidos_exatos' 'SELECT id,situacao FROM wms.pedido_saida WHERE id IN (@pedido,@reversao) AND cliente_id=@cliente AND armazem_id=@armazem')
 Check-Col (@($orders|Where-Object {$_.id -eq $parameters['@pedido'] -and $_.situacao -ceq 'SEPARADO'}).Count -eq 1 -and @($orders|Where-Object {$_.id -eq $parameters['@reversao'] -and $_.situacao -ceq 'CANCELADO'}).Count -eq 1) 'PEDIDOS_SEPARADO_E_CANCELADO'
 $separations=@(Read-Col 'separacoes_exatas' 'SELECT COUNT_BIG(*) linhas,SUM(r.quantidade) quantidade FROM wms.separacao_saida s JOIN wms.reserva_saida r ON r.id=s.reserva_id WHERE r.pedido_id=@pedido AND s.situacao=''SEPARADA''')
 Check-Col ($separations[0].linhas -eq 2 -and $separations[0].quantidade -eq 8) 'DUAS_SEPARACOES_OITO_PRODUTOS_SEM_DUPLICIDADE'
 $invalid=@(Read-Col 'json_ledger' ("SELECT COUNT_BIG(*) invalidos FROM wms.coletor_operacao o JOIN wms.coletor_tarefa t ON t.id=o.tarefa_id WHERE "+$scope+" AND ISJSON(o.resultado)<>1"))
 Check-Col ($invalid[0].invalidos -eq 0) 'RECIBOS_JSON_VALIDOS'
 $fiscal=@(Read-Col 'sem_fiscal' 'SELECT COUNT_BIG(*) emissoes FROM wms.solicitacao_nfe WHERE pedido_id IN (@pedido,@reversao)')
 Check-Col ($fiscal[0].emissoes -eq 0) 'SEM_EMISSAO_FISCAL'
 $report.dados=@{tarefas=$types;estoque=$stock;posicoes=$positions;operacoes=$ops;inventarios=$inventory;leituras=$readings;pedidos=$orders;separacoes=$separations};$report.sourceHash=$runtime.runtime.sourceHash;$report.jarSha256=$runtime.candidate.jar.sha256;$report.estado='APROVADO'
}catch{$number=$null;$ex=$_.Exception;while($ex){if($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException'){$number=$ex.Number};$ex=$ex.InnerException};$report.estado='INTERROMPIDO';$report.erro=@{tipo=$_.Exception.GetType().FullName;id=$_.FullyQualifiedErrorId;sqlNumber=$number;linha=$_.InvocationInfo.ScriptLineNumber;mensagemBrutaPublicada=$false}}
finally{if($conn){$conn.Dispose()};if($credential){$credential.Password.Dispose()};$report.fimUtc=[DateTime]::UtcNow.ToString('o');$out=Join-Path $area ('sql-conferencia-'+[guid]::NewGuid().ToString('N')+'.json');[IO.File]::WriteAllText($out,($report|ConvertTo-Json -Depth 10),[Text.UTF8Encoding]::new($false));[pscustomobject]@{estado=$report.estado;checks=$report.checks.Count;consultas=$report.consultas.Count;recibo=$out;erro=$report.erro}|ConvertTo-Json -Depth 4}
if($report.estado -cne 'APROVADO'){exit 20}
