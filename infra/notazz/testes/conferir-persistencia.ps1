[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$EvidenceDirectory)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$area=[IO.Path]::GetFullPath($EvidenceDirectory)
if($area -ine (Join-Path $root 'orchestracao\.runtime\notazz-lifecycle-20261023')){throw 'NFH_AREA_INVALIDA'}
$cycle=Get-Content -LiteralPath (Join-Path $area 'ciclo.json') -Raw -Encoding UTF8|ConvertFrom-Json
if($cycle.state -cne 'PASS_SQL_REAL_TRANSPORTE_CONTROLADO' -or $cycle.prefix -cnotmatch '^NFH23-[a-f0-9]{8}$' -or $cycle.externalCalls -ne 0){throw 'NFH_CICLO_NAO_APROVADO'}
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'database/scripts/d26-credencial-aplicacao.ps1')
$guard=Invoke-WmsDev02Guard
if(!$guard.guardaRealAprovada -or @($guard.checks|Where-Object {!$_.passou}).Count){throw 'NFH_GUARDA_RECUSADA'}
$credential=$null;$connection=$null
$report=[ordered]@{id='BE10-NOTAZZ-SQL01-TESTEMUNHA';started=[DateTime]::UtcNow.ToString('o');state='PREPARO';database='WMS_DEV';login='WMSDEV';dml=0;ddl=0;externalCalls=0;failure=$null;checks=@();source=(Get-FileHash -LiteralPath (Join-Path $area 'ciclo.json')).Hash}
function Read-Nfh([string]$Sql){
 if($Sql.TrimStart() -cnotmatch '^SELECT\b'){throw 'NFH_SOMENTE_SELECT'}
 $cmd=$connection.CreateCommand();$cmd.CommandText=$Sql;$cmd.CommandTimeout=30
 foreach($pair in @(@('@cliente',[long]$cycle.fixtures.cliente.id),@('@armazem',[long]$cycle.fixtures.armazem.id))){$null=$cmd.Parameters.Add($pair[0],[Data.SqlDbType]::BigInt);$cmd.Parameters[$pair[0]].Value=$pair[1]}
 $rows=[Collections.Generic.List[object]]::new();$reader=$null
 try{$reader=$cmd.ExecuteReader();while($reader.Read()){$row=[ordered]@{};for($i=0;$i -lt $reader.FieldCount;$i++){$row[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}};$rows.Add([pscustomobject]$row)};$rows.ToArray()}finally{if($reader){$reader.Dispose()};$cmd.Dispose()}
}
function Check-Nfh([bool]$Ok,[string]$Code){if(!$Ok){throw ('NFH_'+$Code)};$report.checks+=,$Code}
try{
 $module=Import-Module ([IO.Path]::GetFullPath((Join-Path $root '../.runtime/sql-server/SqlServerProjetos.psm1'))) -PassThru -Force
 & $module {Import-SqlClient}
 $credential=Get-WmsDevApplicationCredential
 $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
 $builder.set_DataSource('tcp:127.0.0.1,1433');$builder.set_InitialCatalog('WMS_DEV');$builder.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory);$builder.set_TrustServerCertificate($false);$builder.set_ServerCertificate($guard.perfilProtegido.certificate);$builder.set_Pooling($false);$builder.set_ConnectTimeout(30);$builder.set_ConnectRetryCount(0);$builder.set_ApplicationName('WMS-NOTAZZ-SQL01-TESTEMUNHA-SELECT')
 $sqlCredential=New-Object Microsoft.Data.SqlClient.SqlCredential('WMSDEV',$credential.Password)
 $connection=New-Object Microsoft.Data.SqlClient.SqlConnection($builder.ConnectionString,$sqlCredential);$connection.Open()
 $i=@(Read-Nfh 'SELECT DB_NAME() banco,ORIGINAL_LOGIN() login,USER_NAME() usuario')
 Check-Nfh ($i.Count -eq 1 -and $i[0].banco -ceq 'WMS_DEV' -and $i[0].login -ceq 'WMSDEV' -and $i[0].usuario -ceq 'WMSDEV') 'IDENTIDADE'
 $scope=@(Read-Nfh 'SELECT c.codigo cliente,a.codigo armazem,c.documento_fiscal proprietario,a.documento_fiscal emitente FROM wms.cliente c JOIN wms.armazem a ON a.id=@armazem WHERE c.id=@cliente')
 Check-Nfh ($scope.Count -eq 1 -and $scope[0].cliente -ceq $cycle.fixtures.cliente.codigo -and $scope[0].armazem -ceq $cycle.prefix.ToUpperInvariant() -and $scope[0].proprietario -ceq '00000000090702' -and $scope[0].emitente -ceq '00000000090701') 'FIXTURE_EXCLUSIVA'
 $report.requests=@(Read-Nfh "SELECT s.id,s.operacao_id,s.natureza,s.situacao,s.numero,s.documento_id,s.documento_origem_id,s.pedido_entrada_id,s.regularizada_em,ISJSON(s.corpo_json) corpoValido,ISJSON(s.documento_json) snapshotValido,CASE WHEN s.xml IS NOT NULL THEN 1 ELSE 0 END xmlGravado FROM wms.solicitacao_nfe s JOIN wms.pedido_saida p ON p.id=s.pedido_id WHERE p.cliente_id=@cliente AND p.armazem_id=@armazem ORDER BY s.criada_em,s.id")
 Check-Nfh ($report.requests.Count -eq 7 -and @($report.requests|Where-Object situacao -ceq 'CANCELADA').Count -eq 1 -and @($report.requests|Where-Object situacao -ceq 'AUTORIZADA').Count -eq 6) 'SETE_SOLICITACOES_CONCILIADAS'
 Check-Nfh (@($report.requests|Where-Object {$_.corpoValido -ne 1 -or $_.snapshotValido -ne 1 -or $_.xmlGravado -ne 1 -or !$_.numero}).Count -eq 0) 'SNAPSHOTS_XML_NUMEROS_PERSISTIDOS'
 $incoming=@($report.requests|Where-Object natureza -ceq 'REGULARIZACAO_ENTRADA')
 Check-Nfh ($incoming.Count -eq 2 -and @($incoming|Where-Object {!$_.regularizada_em -or !$_.documento_origem_id -or !$_.pedido_entrada_id}).Count -eq 0 -and @($incoming.pedido_entrada_id|Select-Object -Unique).Count -eq 2) 'REGULARIZACOES_COM_E_SEM_TRANSITO'
 $report.internalAudit=@(Read-Nfh "SELECT a.id,a.registro_id,a.acao,JSON_VALUE(a.dados_depois,'$.tipoEntrada') tipoEntrada,JSON_VALUE(a.dados_depois,'$.quantidadeFisicaAlterada') quantidadeFisicaAlterada FROM wms.auditoria_cadastro a JOIN wms.pedido_entrada p ON p.id=a.registro_id WHERE a.tipo='PEDIDO_ENTRADA' AND p.cliente_id=@cliente AND p.armazem_id=@armazem AND JSON_VALUE(a.dados_depois,'$.tipoEntrada')='REGULARIZACAO_SEM_TRANSITO'")
 Check-Nfh ($report.internalAudit.Count -eq 1 -and $report.internalAudit[0].acao -ceq 'ENTRADA_EFETIVADA' -and $report.internalAudit[0].quantidadeFisicaAlterada -ceq 'false') 'REGULARIZACAO_INTERNA_AUDITADA_UMA_VEZ'
 $report.parcels=@(Read-Nfh 'SELECT pn.solicitacao_id,COUNT(*) parcelas,SUM(pn.quantidade) quantidade,SUM(pn.valor_total) valor FROM wms.parcela_nfe pn JOIN wms.solicitacao_nfe s ON s.id=pn.solicitacao_id JOIN wms.pedido_saida p ON p.id=s.pedido_id WHERE p.cliente_id=@cliente AND p.armazem_id=@armazem GROUP BY pn.solicitacao_id')
 Check-Nfh ($report.parcels.Count -eq 7 -and @($report.parcels|Where-Object {$_.parcelas -eq 1 -and $_.quantidade -eq 4 -and $_.valor -eq 40}).Count -eq 6 -and @($report.parcels|Where-Object {$_.parcelas -eq 2 -and $_.quantidade -eq 10 -and $_.valor -eq 100}).Count -eq 1) 'PARCELAS_QUANTIDADE_VALOR_EXATOS'
 $report.events=@(Read-Nfh "SELECT e.solicitacao_id,SUM(CASE WHEN e.tipo='CONFIRMADA' THEN 1 ELSE 0 END) confirmacoes,COUNT(*) eventos FROM wms.evento_nfe e JOIN wms.solicitacao_nfe s ON s.id=e.solicitacao_id JOIN wms.pedido_saida p ON p.id=s.pedido_id WHERE p.cliente_id=@cliente AND p.armazem_id=@armazem GROUP BY e.solicitacao_id")
 Check-Nfh ($report.events.Count -eq 7 -and @($report.events|Where-Object {$_.confirmacoes -ne 1 -or $_.eventos -lt 3}).Count -eq 0) 'HISTORICO_SEM_CONFIRMACAO_DUPLICADA'
 $report.stock=@(Read-Nfh 'SELECT u.id,u.codigo,u.quantidade,u.produto_id,(SELECT COUNT(*) FROM wms.ocupacao_endereco o WHERE o.unidade_id=u.id) posicoes FROM wms.unidade_logistica u JOIN wms.pedido_entrada p ON p.id=u.pedido_id WHERE p.cliente_id=@cliente AND p.armazem_id=@armazem AND u.ativa=1 ORDER BY u.id')
 Check-Nfh ($report.stock.Count -eq 4 -and ($report.stock|Measure-Object quantidade -Sum).Sum -eq 30 -and @($report.stock|Where-Object posicoes -ne 1).Count -eq 0) 'ESTOQUE_UNICO_TRINTA_ENDERECADO'
 $report.physical=@(Read-Nfh 'SELECT (SELECT COALESCE(SUM(b.quantidade),0) FROM wms.baixa_saida b JOIN wms.retirada_saida r ON r.id=b.retirada_id JOIN wms.pedido_saida p ON p.id=r.pedido_id WHERE p.cliente_id=@cliente AND p.armazem_id=@armazem) retirada,(SELECT COALESCE(SUM(d.quantidade),0) FROM wms.devolucao_saida d JOIN wms.baixa_saida b ON b.id=d.baixa_id JOIN wms.retirada_saida r ON r.id=b.retirada_id JOIN wms.pedido_saida p ON p.id=r.pedido_id WHERE p.cliente_id=@cliente AND p.armazem_id=@armazem) devolvida')
 Check-Nfh ($report.physical.Count -eq 1 -and $report.physical[0].retirada -eq 14 -and $report.physical[0].devolvida -eq 4) 'RETIRADAS_E_DEVOLUCAO_EXATAS'
 $report.state='PASS_SQL_REAL_PERSISTENCE'
}catch{
 $report.state='FAIL';$ex=$_.Exception;$code='NFH_TESTEMUNHA_FALHOU';$number=$null
 while($ex){if($ex.Message -cmatch '^NFH_[A-Z0-9_]+$'){$code=$ex.Message};if($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException'){$number=$ex.Number};$ex=$ex.InnerException}
 $report.failure=@{code=$code;sqlNumber=$number;line=$_.InvocationInfo.ScriptLineNumber}
}finally{
 if($connection){$connection.Dispose()};if($credential){$credential.Password.Dispose()}
 $report.finished=[DateTime]::UtcNow.ToString('o');$out=Join-Path $area ('testemunha-'+[guid]::NewGuid().ToString('N')+'.json')
 [IO.File]::WriteAllText($out,($report|ConvertTo-Json -Depth 10),[Text.UTF8Encoding]::new($false))
 [pscustomobject]@{state=$report.state;checks=$report.checks.Count;failure=$report.failure;evidence=$out}|ConvertTo-Json -Depth 4 -Compress
}
if($report.state -ceq 'FAIL'){exit 20}
