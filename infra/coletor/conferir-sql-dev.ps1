[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$RuntimeReceipt)
$ErrorActionPreference='Stop'
if($PSVersionTable.PSEdition -ceq 'Desktop'){$env:PSModulePath=[Environment]::GetEnvironmentVariable('PSModulePath','Machine')}
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$area=Join-Path $root 'orchestracao/.runtime/coletor-tarefas-20261028'
. (Join-Path $root 'infra/dev/artefato-backend.ps1')
. (Join-Path $root 'infra/notazz/runtime-validacao.ps1')
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'database/scripts/d26-credencial-aplicacao.ps1')
$report=[ordered]@{id='COLETOR-TAREFAS01';inicioUtc=[DateTime]::UtcNow.ToString('o');estado='PREPARO';somenteSelect=$true;aberturas=0;checks=@();consultas=@();erro=$null}
$conn=$null;$credential=$null
function Check-Col([bool]$ok,[string]$nome){$report.checks+=@{caso=$nome;passou=$ok};if(!$ok){throw ('COLETOR_SQL_'+$nome)}}
function Read-Col([string]$nome,[string]$sql){
 if($sql.TrimStart() -cnotmatch '^SELECT\b' -or $sql -match ';'){throw 'COLETOR_SQL_SELECT_INVALIDO'}
 $cmd=$conn.CreateCommand();$cmd.CommandText=$sql;$cmd.CommandTimeout=30
 $null=$cmd.Parameters.AddWithValue('@cliente',[long]$flow.fixtures.client.id)
 $null=$cmd.Parameters.AddWithValue('@armazem',[long]$flow.fixtures.warehouse.id)
 $null=$cmd.Parameters.AddWithValue('@manual',[long]$flow.fixtures.units[0].id)
 $null=$cmd.Parameters.AddWithValue('@tarefa',[long]$flow.fixtures.addressTask.id)
 $reader=$null;$rows=New-Object 'Collections.Generic.List[object]'
 try{$reader=$cmd.ExecuteReader();while($reader.Read()){$row=[ordered]@{};for($i=0;$i -lt $reader.FieldCount;$i++){$row[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}};$rows.Add([pscustomobject]$row)};$report.consultas+=@{nome=$nome;linhas=$rows.Count};$rows.ToArray()}finally{if($reader){$reader.Dispose()};$cmd.Dispose()}
}
try{
 $runtime=Assert-NotazzCurrentRuntime $RuntimeReceipt
 $flow=Get-Content (Join-Path $area 'fluxos.json') -Raw|ConvertFrom-Json
 Check-Col ($flow.state -ceq 'APROVADO') 'FLUXOS_HTTP_APROVADOS'
 $g=Invoke-WmsDev02Guard
 Check-Col ($g.guardaRealAprovada -and $g.tls.proveniencia.reciboPID -eq $runtime.runtime.sql.pid -and $g.tls.proveniencia.reciboInicioUtc -ceq $runtime.runtime.sql.startUtc) 'GUARDA_ATUAL'
 Write-WmsDevCreateOnlyJson (Join-Path $area ('guarda-sql-'+[guid]::NewGuid().ToString('N')+'.json')) $g
 $module=Import-Module ([IO.Path]::GetFullPath((Join-Path $root '../.runtime/sql-server/SqlServerProjetos.psm1'))) -PassThru -Force
 & $module {Import-SqlClient}
 $credential=Get-WmsDevApplicationCredential
 Check-Col ($credential.UserName -ceq 'WMSDEV') 'IDENTIDADE_PROTEGIDA'
 $b=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
 $b.set_DataSource('tcp:127.0.0.1,1433');$b.set_InitialCatalog('WMS_DEV');$b.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory);$b.set_TrustServerCertificate($false);$b.set_ServerCertificate($g.perfilProtegido.certificate);$b.set_Pooling($false);$b.set_ConnectTimeout(30);$b.set_ConnectRetryCount(0);$b.set_ApplicationName('WMS-COLETOR-TAREFAS01-SELECT')
 $sc=New-Object Microsoft.Data.SqlClient.SqlCredential('WMSDEV',$credential.Password)
 $conn=New-Object Microsoft.Data.SqlClient.SqlConnection($b.ConnectionString,$sc);$report.aberturas=1;$conn.Open()
 $identity=@(Read-Col 'identidade' "SELECT DB_NAME() banco,ORIGINAL_LOGIN() login")
 $report.identidade=$identity
 $report.tls=@{encrypt=$b.Encrypt.ToString();trustServerCertificate=$b.TrustServerCertificate;estadoConexao=$conn.State.ToString();certificateSha256=$g.tls.certificateSha256}
 # A abertura SqlClient ja validou Mandatory/certificado privado/TSC=false; nao exigir DMV administrativa da aplicacao.
 Check-Col ($identity[0].banco -ceq 'WMS_DEV' -and $identity[0].login -ceq 'WMSDEV' -and $b.Encrypt.ToString() -cin @('True','Mandatory') -and !$b.TrustServerCertificate -and $conn.State.ToString() -ceq 'Open') 'SQL_DEV_WMSDEV_TLS'
 $types=@(Read-Col 'tarefas' 'SELECT tipo,COUNT_BIG(*) quantidade FROM wms.coletor_tarefa WHERE cliente_id=@cliente AND armazem_id=@armazem GROUP BY tipo')
 Check-Col (@($types|Where-Object {$_.tipo -ceq 'ENDERECAMENTO' -and $_.quantidade -eq 2}).Count -eq 1 -and @($types|Where-Object {$_.tipo -ceq 'SEPARACAO' -and $_.quantidade -eq 1}).Count -eq 1 -and @($types|Where-Object {$_.tipo -ceq 'INVENTARIO' -and $_.quantidade -eq 2}).Count -eq 1) 'CINCO_TAREFAS_ORIGENS_EXATAS'
 $stock=@(Read-Col 'fisico' 'SELECT COUNT_BIG(*) mercadorias,SUM(u.quantidade) quantidade FROM wms.unidade_logistica u JOIN wms.pedido_entrada p ON p.id=u.pedido_id WHERE p.cliente_id=@cliente AND p.armazem_id=@armazem AND u.ativa=1')
 Check-Col ($stock[0].mercadorias -eq 2 -and $stock[0].quantidade -eq 10) 'DUAS_MERCADORIAS_DEZ_UNIDADES_PRESERVADAS'
 $ops=@(Read-Col 'operacoes_enderecamento' 'SELECT tipo,COUNT_BIG(*) quantidade FROM wms.coletor_operacao WHERE tarefa_id=@tarefa GROUP BY tipo')
 Check-Col (@($ops|Where-Object {$_.tipo -ceq 'INICIAR' -and $_.quantidade -eq 2}).Count -eq 1 -and @($ops|Where-Object {$_.tipo -ceq 'LIBERAR' -and $_.quantidade -eq 1}).Count -eq 1 -and @($ops|Where-Object {$_.tipo -ceq 'ENDERECAR' -and $_.quantidade -eq 1}).Count -eq 1) 'LEDGER_SEM_REPETICAO_APOS_REPLAY'
 $manual=@(Read-Col 'manual' 'SELECT t.responsavel,u.primeiro_enderecamento_em FROM wms.coletor_tarefa t JOIN wms.unidade_logistica u ON u.id=t.origem_id WHERE t.tipo=''ENDERECAMENTO'' AND t.origem_id=@manual AND t.cliente_id=@cliente AND t.armazem_id=@armazem')
 Check-Col ($manual.Count -eq 1 -and !$manual[0].responsavel -and $manual[0].primeiro_enderecamento_em) 'MANUAL_CONCLUIDO_SEM_RESPONSAVEL_ARTIFICIAL'
 $inventory=@(Read-Col 'inventarios' 'SELECT situacao,COUNT_BIG(*) quantidade FROM wms.inventario WHERE cliente_id=@cliente AND armazem_id=@armazem GROUP BY situacao')
 Check-Col (@($inventory|Where-Object {$_.situacao -ceq 'CONCLUIDO' -and $_.quantidade -eq 1}).Count -eq 1 -and @($inventory|Where-Object {$_.situacao -ceq 'CANCELADO' -and $_.quantidade -eq 1}).Count -eq 1) 'INVENTARIO_CONCLUIDO_E_CANCELADO'
 $invalid=@(Read-Col 'json' 'SELECT COUNT_BIG(*) invalidos FROM wms.coletor_operacao o JOIN wms.coletor_tarefa t ON t.id=o.tarefa_id WHERE t.cliente_id=@cliente AND t.armazem_id=@armazem AND ISJSON(o.resultado)<>1')
 Check-Col ($invalid[0].invalidos -eq 0) 'RECIBOS_JSON_VALIDOS'
 $fiscal=@(Read-Col 'fiscal' 'SELECT COUNT_BIG(*) emissoes FROM wms.solicitacao_nfe s JOIN wms.pedido_saida p ON p.id=s.pedido_id WHERE p.cliente_id=@cliente AND p.armazem_id=@armazem')
 Check-Col ($fiscal[0].emissoes -eq 0) 'SEM_EMISSAO_FISCAL'
 $report.dados=@{tarefas=$types;estoque=$stock;operacoes=$ops;inventarios=$inventory};$report.sourceHash=$runtime.runtime.sourceHash;$report.jarSha256=$runtime.candidate.jar.sha256;$report.estado='APROVADO'
}catch{$number=$null;$ex=$_.Exception;while($ex){if($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException'){$number=$ex.Number};$ex=$ex.InnerException};$report.estado='INTERROMPIDO';$report.erro=@{tipo=$_.Exception.GetType().FullName;id=$_.FullyQualifiedErrorId;sqlNumber=$number;linha=$_.InvocationInfo.ScriptLineNumber;mensagemBrutaPublicada=$false}}
finally{if($conn){$conn.Dispose()};if($credential){$credential.Password.Dispose()};$report.fimUtc=[DateTime]::UtcNow.ToString('o');$out=Join-Path $area ('sql-conferencia-'+[guid]::NewGuid().ToString('N')+'.json');[IO.File]::WriteAllText($out,($report|ConvertTo-Json -Depth 10),[Text.UTF8Encoding]::new($false));[pscustomobject]@{estado=$report.estado;checks=$report.checks.Count;consultas=$report.consultas.Count;recibo=$out;erro=$report.erro}|ConvertTo-Json -Depth 4}
if($report.estado -cne 'APROVADO'){exit 20}
