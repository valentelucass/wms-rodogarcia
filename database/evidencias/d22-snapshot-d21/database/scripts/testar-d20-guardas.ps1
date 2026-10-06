[CmdletBinding()]
param()
$WmsD20Pathtestard20guardas=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

$ErrorActionPreference='Stop'
. (Join-Path $WmsD20Pathtestard20guardas 'scripts/d20-guardas.ps1')
$resultados=New-Object 'Collections.Generic.List[object]'
function Caso([string]$Nome,[scriptblock]$Teste,[string]$Erro='') {
    $falha=$null
    try { & $Teste } catch { $falha=$_.Exception.Message }
    $ok=if($Erro){$falha -ceq $Erro}else{$null -eq $falha}
    $resultados.Add([pscustomobject]@{caso=$Nome;aprovado=$ok;erro=$falha})
}
function Alvo([string]$Banco='WMS_DEV',[string]$Acao='MigrarDev',[string]$HostSql='localhost',[string]$Porta='1433',[string]$Confirmado='', [string]$Servidor='FICTICIO') {
    if (-not $Confirmado) {$Confirmado="${HostSql}:${Porta}/${Banco}"}
    Assert-WmsD20Alvo $HostSql $Porta $Banco $Confirmado $Servidor $Acao
}
Caso 'DEV exato' { Alvo }
Caso 'PROD info/validate/migrate recusado' { Alvo 'WMS_PROD' } 'D20_PROD_MIGRATION_CARGA_RECUSADA'
Caso 'PROD criar somente como PROD' { Alvo 'WMS_PROD' 'CriarDev' } 'D20_PROD_MIGRATION_CARGA_RECUSADA'
Caso 'PROD CREATE vazio permitido condicionalmente' { Alvo 'WMS_PROD' 'CriarProd' }
Caso 'DEV nao usa acao criar PROD' { Alvo 'WMS_DEV' 'CriarProd' } 'D20_CRIACAO_ACAO_DIVERGENTE'
Caso 'PROD plano somente' { Alvo 'WMS_PROD' 'PlanejarCriacao' }
Caso 'nome minusculo recusado' { Alvo 'wms_dev' } 'D20_BANCO_NAO_AUTORIZADO'
Caso 'nome de outro banco recusado' { Alvo 'FICTICIO' } 'D20_BANCO_NAO_AUTORIZADO'
Caso 'injecao no nome' { Alvo 'WMS_DEV;DROP' } 'D20_BANCO_NAO_AUTORIZADO'
Caso 'porta zero' { Alvo 'WMS_DEV' 'MigrarDev' 'localhost' '0' } 'D20_FORMATO_ALVO_INVALIDO'
Caso 'porta excedida' { Alvo 'WMS_DEV' 'MigrarDev' 'localhost' '65536' } 'D20_FORMATO_ALVO_INVALIDO'
Caso 'host com SQL' { Alvo 'WMS_DEV' 'MigrarDev' "x';" } 'D20_FORMATO_ALVO_INVALIDO'
Caso 'alvo distinto' { Alvo 'WMS_DEV' 'MigrarDev' 'localhost' '1433' 'localhost:1433/WMS_PROD' } 'D20_ALVO_NAO_CONFIRMADO'
Caso 'servidor ausente' { Alvo 'WMS_DEV' 'MigrarDev' 'localhost' '1433' 'localhost:1433/WMS_DEV' '' } 'D20_ALVO_NAO_CONFIRMADO'
Caso 'ensaio loopback' { Alvo 'WMS_DEV' 'DiagnosticarLocal' '127.0.0.1' }
Caso 'ensaio nao aceita host externo' { Alvo 'WMS_DEV' 'DiagnosticarLocal' 'servidor.ficticio' } 'D20_ENSAIO_EXIGE_LOOPBACK'
Caso 'identidade real correta' { Assert-WmsD20Identidade 'FICTICIO' 'WMS_DEV' 'FICTICIO' 'WMS_DEV' }
Caso 'identidade real redirecionada' { Assert-WmsD20Identidade 'OUTRO' 'WMS_DEV' 'FICTICIO' 'WMS_DEV' } 'D20_IDENTIDADE_DIVERGENTE'
Caso 'banco real redirecionado' { Assert-WmsD20Identidade 'FICTICIO' 'WMS_PROD' 'FICTICIO' 'WMS_DEV' } 'D20_IDENTIDADE_DIVERGENTE'
Caso 'Flyway override mesmo vazio' { Assert-WmsD20Overrides @{FLYWAY_URL=''} } 'D20_OVERRIDE_FLYWAY_RECUSADO'
Caso 'Flyway clean override' { Assert-WmsD20Overrides @{MAVEN_OPTS='-Dflyway.cleanDisabled=false'} } 'D20_OVERRIDE_MAVEN_RECUSADO'
Caso 'Maven alvo override' { Assert-WmsD20Overrides @{JAVA_TOOL_OPTIONS='-Denv.WMS_DB_NAME=FICTICIO'} } 'D20_OVERRIDE_MAVEN_RECUSADO'
Caso 'Maven POM alternativo' { Assert-WmsD20Overrides @{MAVEN_ARGS='-f outro.xml'} } 'D20_OVERRIDE_MAVEN_RECUSADO'
Caso 'Maven memoria comum' { Assert-WmsD20Overrides @{MAVEN_OPTS='-Xmx512m'} }
Caso 'Maven goal injetado' { Assert-WmsD20Overrides @{MAVEN_ARGS='flyway:clean'} } 'D20_OVERRIDE_MAVEN_RECUSADO'
Caso 'Maven skip override' { Assert-WmsD20Overrides @{MAVEN_OPTS='-Dwms.migrations.skip=true'} } 'D20_OVERRIDE_MAVEN_RECUSADO'
Caso 'DEV existente planeja preservar e conferir' { $p=Get-WmsD20PlanoCriacao 'WMS_DEV' $true $false;if($p.acao -cne 'PRESERVAR_E_CONFERIR_EXISTENTE' -or $p.permiteExecutar){throw 'plano incorreto'} }
Caso 'PROD existente planeja preservar e conferir' { $p=Get-WmsD20PlanoCriacao 'WMS_PROD' $true $true;if($p.acao -cne 'PRESERVAR_E_CONFERIR_EXISTENTE' -or $p.permiteExecutar){throw 'plano incorreto'} }
Caso 'existente exato online preservavel' { Assert-WmsD20BancoExistente 'WMS_DEV' 'WMS_DEV' $true }
Caso 'existente nome divergente recusado' { Assert-WmsD20BancoExistente 'wms_dev' 'WMS_DEV' $true } 'D20_EXISTENTE_NOME_OU_ESTADO_DIVERGENTE'
Caso 'existente offline preservado e recusado para continuar' { Assert-WmsD20BancoExistente 'WMS_DEV' 'WMS_DEV' $false } 'D20_EXISTENTE_NOME_OU_ESTADO_DIVERGENTE'
Caso 'acao observada Create inexistente mantem Create' {if((Get-WmsD20AcaoObservada Create $false) -cne 'Create'){throw 'acao incorreta'}}
Caso 'corrida nome aparece Create vira Check sem DDL' {if((Get-WmsD20AcaoObservada Create $true) -cne 'Check'){throw 'acao incorreta'}}
Caso 'acao observada Check existente preserva' {if((Get-WmsD20AcaoObservada Check $true) -cne 'Check'){throw 'acao incorreta'}}
Caso 'Check banco desapareceu recusa sem criar' {Get-WmsD20AcaoObservada Check $false} 'D20_EXISTENTE_NAO_ENCONTRADO'
Caso 'DEV plano nunca executa' { $p=Get-WmsD20PlanoCriacao 'WMS_DEV' $false $false; if($p.permiteExecutar -or $p.ordem -ne 1){throw 'plano incorreto'} }
Caso 'PROD depende DEV e nunca executa' { $p=Get-WmsD20PlanoCriacao 'WMS_PROD' $false $false; if($p.permiteExecutar -or $p.depende -cne 'DEV_PRIMEIRO'){throw 'plano incorreto'} }
Caso 'PROD comprovado ainda nao executa' { $p=Get-WmsD20PlanoCriacao 'WMS_PROD' $false $true; if($p.permiteExecutar){throw 'plano incorreto'} }
Caso 'PROD sem DEV recusa CREATE' { Assert-WmsD20Criacao 'WMS_PROD' $false $false } 'D20_DEV_PRIMEIRO'
Caso 'PROD com DEV permite somente CREATE' { Assert-WmsD20Criacao 'WMS_PROD' $false $true }
Caso 'DEV existente recusa CREATE' { Assert-WmsD20Criacao 'WMS_DEV' $true $false } 'D20_BANCO_EXISTENTE_NAO_SOBRESCREVER'
Caso 'sessao todas opcoes filtradas' { $s=Get-WmsD20SessaoSql; foreach($o in @('ANSI_NULLS','ANSI_PADDING','ANSI_WARNINGS','ARITHABORT','CONCAT_NULL_YIELDS_NULL','QUOTED_IDENTIFIER')){if($s -notmatch "SET $o ON;"){throw 'SET ausente'}}; if($s -notmatch 'SET NUMERIC_ROUNDABORT OFF;'){throw 'SET ausente'} }
Caso 'identidades distintas' { Assert-WmsD20Credenciais 'APP_FICTICIA' 'MIGRATION_FICTICIA' }
Caso 'sa nao e migration' { Assert-WmsD20Credenciais 'APP_FICTICIA' 'sa' } 'D20_SA_RESERVADO_CRIACAO'
Caso 'sa nao e aplicacao' { Assert-WmsD20Credenciais 'sa' 'MIGRATION_FICTICIA' } 'D20_SA_RESERVADO_CRIACAO'
Caso 'initSQL recusa roles amplas e resposta desconhecida' { $s=Get-WmsD20FlywayInitSql 'FICTICIO' 'MIGRATION_FICTICIA';foreach($p in @('sysadmin','db_owner','db_securityadmin','db_accessadmin','db_ddladmin','CONTROL SERVER')){if(-not $s.Contains($p)){throw 'guarda ausente'}};if($s -notmatch 'COALESCE' -or $s -notmatch 'THROW 51026'){throw 'guarda ausente'} }
Caso 'identidade compartilhada mesmo outra caixa' { Assert-WmsD20Credenciais 'APP_FICTICIA' 'app_ficticia' } 'D20_IDENTIDADES_TECNICAS_NAO_SEPARADAS'
Caso 'initSQL servidor banco login mesma sessao' { $s=Get-WmsD20FlywayInitSql 'FICTICIO' 'MIGRATION_FICTICIA'; if($s -notmatch 'ServerName' -or $s -notmatch 'ORIGINAL_LOGIN' -or $s -notmatch 'WMS_DEV'){throw 'guarda ausente'} }
Caso 'literal initSQL escapado' { $s=Get-WmsD20FlywayInitSql "FIC'TICIO" 'MIGRATION_FICTICIA'; if($s -notmatch "FIC''TICIO"){throw 'literal livre'} }
Caso 'initSQL rejeita controle' { Get-WmsD20FlywayInitSql "FICTICIO`n" 'MIGRATION_FICTICIA' } 'D20_IDENTIDADE_INITSQL_INVALIDA'
foreach($entrada in @('FLYWAY_USER','FLYWAY_PASSWORD','FLYWAY_URL','FLYWAY_LOCATIONS','FLYWAY_SCHEMAS','FLYWAY_DEFAULT_SCHEMA','FLYWAY_CONFIG_FILES','FLYWAY_INIT_SQL','FLYWAY_CLEAN_DISABLED','FLYWAY_BASELINE_ON_MIGRATE','FLYWAY_PLACEHOLDERS_WMSDATABASE')){
    $dados=@{};$dados[$entrada]='FICTICIO';Caso ("override $entrada antes conector") { Assert-WmsD20Overrides $dados } 'D20_OVERRIDE_FLYWAY_RECUSADO'
}
function EstadoVazio { [pscustomobject]@{banco='WMS_DEV';metadataCompleta=1;objetos=0;tipos=0;schemas=0;principais=0;membros=0;permissoes=0} }
Caso 'pos-condicao vazio comprovado' { Assert-WmsD20Vazio (EstadoVazio) 'WMS_DEV' }
Caso 'pos-condicao metadata parcial' { $e=EstadoVazio;$e.metadataCompleta=0;Assert-WmsD20Vazio $e 'WMS_DEV' } 'D20_METADADOS_INSUFICIENTES'
Caso 'pos-condicao metadata ausente' { Assert-WmsD20Vazio ([pscustomobject]@{banco='WMS_DEV'}) 'WMS_DEV' } 'D20_METADADOS_INSUFICIENTES'
Caso 'existente aceita objetos e acessos preservados com metadata completa' {$e=EstadoVazio;$e.objetos=64;$e.principais=2;$e.permissoes=12;Assert-WmsD20MetadadosExistente $e 'WMS_DEV'}
Caso 'existente nao aceita metadata parcial' {$e=EstadoVazio;$e.metadataCompleta=0;Assert-WmsD20MetadadosExistente $e 'WMS_DEV'} 'D20_METADADOS_INSUFICIENTES'
Caso 'pos-condicao outro banco' { Assert-WmsD20Vazio (EstadoVazio) 'WMS_PROD' } 'D20_VAZIO_BANCO_DIVERGENTE'
foreach($item in @('objetos','tipos','schemas','principais','membros','permissoes')){
    $e=EstadoVazio;$e.$item=1;Caso ("pos-condicao $item inesperado") {Assert-WmsD20Vazio $e 'WMS_DEV'} 'D20_CONTEUDO_OU_ACESSO_INESPERADO'
}
Caso 'CREATE exige autocommit nos dois SQLs' {
    foreach($db in @('WMS_DEV','WMS_PROD')){
        $s=[IO.File]::ReadAllText((Join-Path $WmsD20Pathtestard20guardas ('criacao/CREATE_'+$db+'.sql'))) -replace '(?m)--.*$',''
        if($s -notmatch '@@TRANCOUNT <> 0' -or $s -notmatch 'SET IMPLICIT_TRANSACTIONS OFF;' -or $s -match '\b(?:DROP|ALTER|GRANT|COLLATE|RESTORE)\b'){throw 'CREATE indevido'}
        if(@([regex]::Matches($s,'CREATE DATABASE')).Count -ne 1){throw 'DDL indevido'}
    }
}
Caso 'credencial local ausente nao conecta' { Get-WmsD20CredencialCriacao } 'D20_CREDENCIAL_CRIACAO_PROTEGIDA_AUSENTE_SEM_CONEXAO'
Caso 'SecureString vazio nao conecta' { Get-WmsD20CredencialCriacao -SenhaLocal (New-Object Security.SecureString) } 'D20_CREDENCIAL_CRIACAO_PROTEGIDA_AUSENTE_SEM_CONEXAO'
function SenhaFicticia { $s=New-Object Security.SecureString;foreach($n in @(70,73,67,84,73,67,73,79)){$s.AppendChar([char]$n)};return $s }
Caso 'SecureString local protegido nao cria conexao' {
    $s=SenhaFicticia;try{$c=Get-WmsD20CredencialCriacao -SenhaLocal $s;if($c.UserId -cne 'sa' -or -not $c.Password.IsReadOnly()){throw 'credencial incorreta'}}finally{if($c){$c.Password.Dispose()};$s.Dispose()}
}
Caso 'PSCredential local protegido nao cria conexao' {
    $s=SenhaFicticia;try{$p=New-Object Management.Automation.PSCredential('sa',$s);$c=Get-WmsD20CredencialCriacao -CredencialLocal $p;if($c.UserId -cne 'sa'){throw 'credencial incorreta'}}finally{if($c){$c.Password.Dispose()};$s.Dispose()}
}
Caso 'PSCredential identidade diferente recusada' {
    $s=SenhaFicticia;try{Get-WmsD20CredencialCriacao -CredencialLocal (New-Object Management.Automation.PSCredential('FICTICIO',$s))}finally{$s.Dispose()}
} 'D20_CREDENCIAL_CRIACAO_IDENTIDADE_DIVERGENTE'
Caso 'credenciais locais conflitantes recusadas' {
    $s=SenhaFicticia;try{Get-WmsD20CredencialCriacao -CredencialLocal (New-Object Management.Automation.PSCredential('sa',$s)) -SenhaLocal $s}finally{$s.Dispose()}
} 'D20_CREDENCIAL_ENTRADAS_CONFLITANTES'
$falhas=@($resultados|Where-Object {-not $_.aprovado})
[pscustomobject]@{natureza='TESTE_OFFLINE_SEM_SQL_SEM_AMBIENTE';total=$resultados.Count;aprovados=($resultados.Count-$falhas.Count);falhas=$falhas.Count;casos=$resultados.ToArray()} | ConvertTo-Json -Depth 6
if($falhas.Count){exit 1}
