. (Join-Path $PSScriptRoot 'd21-flyway.ps1')
$WmsD27Database=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$WmsD27V10='V10__permitir_ajuste_estoque_em_fato_permanencia.sql'
function Assert-WmsD27Dev([string]$Banco){if($Banco -cne 'WMS_DEV'){throw 'D27_SOMENTE_DEV'}}
function Test-WmsD27Check([string]$Def,[switch]$Novo){
    if([string]::IsNullOrEmpty($Def)){return $false}
    $tipos=@('SEPARACAO','RETORNO_INTERNO','RETIRADA','AVARIA','REPARO');if($Novo){$tipos+='AJUSTE_ESTOQUE'}
    foreach($literal in $tipos){if($Def.IndexOf("'"+$literal+"'",[StringComparison]::Ordinal) -lt 0){return $false}}
    $n=$Def.Replace('[tipo]','tipo').Replace('(','').Replace(')','').Replace(' ','').Replace([string][char]9,'').Replace([string][char]10,'').Replace([string][char]13,'')
    $formaIN="tipoIN'"+($tipos -join "','")+"'"
    [array]::Reverse($tipos);$formaOR=@($tipos|ForEach-Object {"tipo='"+$_+"'"}) -join 'OR'
    [string]::Equals($n,$formaIN,[StringComparison]::Ordinal) -or [string]::Equals($n,$formaOR,[StringComparison]::Ordinal)
}
function Get-WmsD27Fontes {
    $fs=@(Get-WmsD21Fontes $WmsD27Database);$base=Get-Content (Join-Path $WmsD27Database 'evidencias/d20-baseline.json') -Raw -Encoding UTF8|ConvertFrom-Json
    $permitidos=@($base.migrations|ForEach-Object {$_.arquivo.Substring('database/'.Length)})+@('migrations/'+$WmsD27V10)
    foreach($f in $fs){if($f.arquivo -cnotin $permitidos){throw 'D27_MIGRATION_PENDENTE_INESPERADA'}}
    foreach($f in @(Get-ChildItem (Join-Path $WmsD27Database 'migrations') -Recurse -File -Filter '*.conf')){if($f.Name -cne ($WmsD27V10+'.conf')){throw 'D27_SIDECAR_INESPERADO'}}
    $fs
}
function Assert-WmsD27Revisao($Revisao,[string]$MigrationHash,[string]$ExecutorHash,[string]$ConfigHash){
    if($null -eq $Revisao -or $Revisao.demanda -cne 'D27' -or $Revisao.banco -cne 'WMS_DEV' -or $Revisao.vigiaAprovou -isnot [bool] -or $Revisao.farolContinuar -isnot [bool] -or $Revisao.vigiaAprovou -ne $true -or $Revisao.farolContinuar -ne $true -or $Revisao.migrationSha256 -cne $MigrationHash -or $Revisao.executorSha256 -cne $ExecutorHash -or $Revisao.configSha256 -cne $ConfigHash){throw 'D27_REVISAO_E_SINAL_AUSENTES_OU_DIVERGENTES'}
    foreach($f in @('d27-guardas.ps1','d27-preflight.ps1','d21-flyway.ps1','d21-guardas.ps1')){if($null -eq $Revisao.componentesSha256 -or $Revisao.componentesSha256.$f -cne (Get-FileHash (Join-Path $PSScriptRoot $f) -Algorithm SHA256).Hash){throw 'D27_COMPONENTE_REVISADO_DIVERGENTE'}}
}
function New-WmsD27StartInfo([string]$Servidor,[ValidateSet('PreValidate','Migrate','PostValidate','Info')][string]$Etapa,[string]$JavaHome){
    $psi=New-WmsD21StartInfo $WmsD27Database 'WMS_DEV' $Servidor $Etapa
    $psi.Arguments=$psi.Arguments.Replace(' validate flyway:', ' -Dflyway.target=10 -Dmssql-jdbc.version=13.4.0.jre11 validate flyway:')
    $psi.EnvironmentVariables['JAVA_HOME']=$JavaHome
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_INIT_SQL']=(Get-WmsD21InitSql 'WMS_DEV' $Servidor)+" IF EXISTS(SELECT 1 FROM wms.flyway_schema_history WHERE success=0 OR (type=N'SQL' AND version NOT IN(N'1',N'2',N'3',N'4',N'5',N'6',N'7',N'8',N'9',N'10'))) THROW 51027,'D27 historico inesperado; preservar.',1;"
    $psi
}
