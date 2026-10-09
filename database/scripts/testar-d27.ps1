$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'd27-guardas.ps1')
$checks=New-Object 'Collections.Generic.List[object]'
function D27Teste([string]$Nome,[bool]$Obtido){$checks.Add([pscustomobject]@{caso=$Nome;passou=$Obtido})}
function D27Recusa([scriptblock]$Bloco){try{& $Bloco;return $false}catch{return $true}}
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$p=Get-Content (Join-Path $root 'orchestracao/.runtime/d27-prumo-preflight-real.json') -Raw -Encoding UTF8|ConvertFrom-Json
D27Teste 'DEV aceito' (-not(D27Recusa {Assert-WmsD27Dev 'WMS_DEV'}))
D27Teste 'PROD recusado' (D27Recusa {Assert-WmsD27Dev 'WMS_PROD'})
D27Teste 'nome livre recusado' (D27Recusa {Assert-WmsD27Dev 'OUTRO'})
D27Teste 'definicao REAL anterior coincide exatamente' (Test-WmsD27Check $p.check.definition)
D27Teste 'NULL recusado' (-not(Test-WmsD27Check $null))
D27Teste 'tautologia OR coluna-coluna recusada' (-not(Test-WmsD27Check ($p.check.definition+' OR[tipo]=[tipo]')))
D27Teste 'tipo extra recusado' (-not(Test-WmsD27Check ($p.check.definition+" OR[tipo]='EXTRA'")))
$novo="tipo IN ('SEPARACAO','RETORNO_INTERNO','RETIRADA','AVARIA','REPARO','AJUSTE_ESTOQUE')"
D27Teste 'dominio seis tipos aceito' (Test-WmsD27Check $novo -Novo)
D27Teste 'dominio seis mais tautologia recusado' (-not(Test-WmsD27Check ($novo+' OR[tipo]=[tipo]') -Novo))
$v10=Join-Path $root ('database/docs/propostas/d27/'+$WmsD27V10);$exe=Join-Path $PSScriptRoot 'd27-migrate-dev.ps1'
$mh=(Get-FileHash $v10).Hash;$eh=(Get-FileHash $exe).Hash;$ch=(Get-FileHash ($v10+'.conf')).Hash
$componentes=[ordered]@{};foreach($f in @('d27-guardas.ps1','d27-preflight.ps1','d21-flyway.ps1','d21-guardas.ps1')){$componentes[$f]=(Get-FileHash (Join-Path $PSScriptRoot $f)).Hash}
$review=[pscustomobject]@{demanda='D27';banco='WMS_DEV';vigiaAprovou=$true;farolContinuar=$true;migrationSha256=$mh;executorSha256=$eh;configSha256=$ch;componentesSha256=[pscustomobject]$componentes}
D27Teste 'review nulo recusado' (D27Recusa {Assert-WmsD27Revisao $null $mh $eh $ch})
D27Teste 'review ficticio completo aceito sem executar' (-not(D27Recusa {Assert-WmsD27Revisao $review $mh $eh $ch}))
$review.vigiaAprovou=$false;D27Teste 'sem review Vigia recusado' (D27Recusa {Assert-WmsD27Revisao $review $mh $eh $ch});$review.vigiaAprovou=$true
$review.farolContinuar=$false;D27Teste 'sem sinal Farol recusado' (D27Recusa {Assert-WmsD27Revisao $review $mh $eh $ch});$review.farolContinuar=$true
$review.vigiaAprovou='true';D27Teste 'booleano textual recusado' (D27Recusa {Assert-WmsD27Revisao $review $mh $eh $ch});$review.vigiaAprovou=$true
D27Teste 'checksum revisado divergente recusado' (D27Recusa {Assert-WmsD27Revisao $review 'DIVERGE' $eh $ch})
$review.banco='WMS_PROD';D27Teste 'review PROD recusado' (D27Recusa {Assert-WmsD27Revisao $review $mh $eh $ch});$review.banco='WMS_DEV'
$review.componentesSha256.'d27-preflight.ps1'='DIVERGE';D27Teste 'consumidor revisado mudou recusado' (D27Recusa {Assert-WmsD27Revisao $review $mh $eh $ch})
foreach($etapa in @('PreValidate','Migrate','PostValidate','Info')){
    $psi=New-WmsD27StartInfo 'ROD-SRVW-001' $etapa $p.javaHome
    D27Teste ($etapa+' target10/JDBC13.4/DEV fixos') ($psi.Arguments.Contains('-Dflyway.target=10') -and $psi.Arguments.Contains('-Dmssql-jdbc.version=13.4.0.jre11') -and $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_NAME'] -ceq 'WMS_DEV')
    D27Teste ($etapa+' ignore patterns fixos') ($psi.Arguments.Contains($(if($etapa -ceq 'PreValidate'){'-Dflyway.ignoreMigrationPatterns=*:pending '}else{'-Dflyway.ignoreMigrationPatterns= '})))
    D27Teste ($etapa+' senha ausente antes filho/identidade initSql') (-not $psi.EnvironmentVariables.ContainsKey('WMS_DB_BOOTSTRAP_PASSWORD') -and $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_INIT_SQL'].Contains("N'WMS_DEV'") -and $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_INIT_SQL'].Contains("N'ROD-SRVW-001'") -and $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_INIT_SQL'].Contains('ORIGINAL_LOGIN()'))
    $psi.EnvironmentVariables.Clear()
}
D27Teste 'servidor vazio recusado antes filho' (D27Recusa {New-WmsD27StartInfo '' 'Migrate' $p.javaHome})
$fontes=@(Get-WmsD27Fontes);D27Teste 'nove fontes congeladas intactas' ($fontes.Count -eq 9)
$saved=(Get-Command Get-WmsD21Fontes).ScriptBlock
try{function Get-WmsD21Fontes {param($DatabaseRoot) @([pscustomobject]@{arquivo='migrations/V11__inesperada.sql';sha256='FICTICIO'})};D27Teste 'outra pendente recusada' (D27Recusa {Get-WmsD27Fontes})}finally{Set-Item Function:Get-WmsD21Fontes $saved}
D27Teste 'V10 inativa antes review' (-not(Test-Path (Join-Path $root ('database/migrations/'+$WmsD27V10))))
D27Teste 'transacao explicita no sidecar' ((Get-Content ($v10+'.conf') -Raw).Trim() -ceq 'executeInTransaction=true')
$filhos=@();foreach($codigo in @(0,2)){
    $psi=New-Object Diagnostics.ProcessStartInfo;$psi.FileName=Join-Path $env:SystemRoot 'System32/cmd.exe';$psi.Arguments='/d /c exit '+$codigo;$psi.UseShellExecute=$false;$psi.CreateNoWindow=$true;$psi.RedirectStandardOutput=$true;$psi.RedirectStandardError=$true
    $fict=ConvertTo-SecureString 'D27-FICTICIO-OFFLINE-NUNCA-SQL' -AsPlainText -Force
    try{$ret=Invoke-WmsD21Processo $psi $fict 10000}finally{$fict.Dispose()}
    $filhos+=$ret
    D27Teste ('filho ficticio '+$codigo+' exit/PID/encerramento/limpeza') ($ret.exitCode -eq $codigo -and $ret.pid -gt 0 -and $ret.processoEncerrado -and $psi.EnvironmentVariables.Count -eq 0 -and -not(Get-Process -Id $ret.pid -ErrorAction SilentlyContinue))
}
$parse=@();foreach($name in @('d27-preflight.ps1','d27-guardas.ps1','d27-migrate-dev.ps1','testar-d27.ps1')){$t=$null;$e=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $name),[ref]$t,[ref]$e);$parse+=[pscustomobject]@{arquivo=$name;erros=$e.Count};D27Teste ('parser PS '+$name) ($e.Count -eq 0)}
Add-Type -Path 'C:/Program Files/Microsoft SQL Server Management Studio 22/Release/Common7/IDE/Extensions/Application/Microsoft.SqlServer.TransactSql.ScriptDom.dll'
$parser=New-Object Microsoft.SqlServer.TransactSql.ScriptDom.TSql160Parser($true);$errors=$null;$reader=New-Object IO.StringReader((Get-Content $v10 -Raw -Encoding UTF8));try{[void]$parser.Parse($reader,[ref]$errors)}finally{$reader.Dispose()}
D27Teste 'parser SQL160 offline' ($errors.Count -eq 0)
$r=[pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');natureza='D27_FIXTURES_OFFLINE_FOCAIS';checks=$checks.Count;falhas=@($checks|Where-Object {-not $_.passou}).Count;casos=$checks.ToArray();parsersPS=$parse;parserSQL=$errors.Count;filhosFicticios=$filhos;sqlExecutado=$false;credencialCarregada=$false;reviewRealCriado=$false;childFlywayIniciado=$false;migrationSha256=$mh;executorSha256=$eh;configSha256=$ch;componentesSha256=$componentes;segredoExibido=$false}
$r|ConvertTo-Json -Depth 8|Set-Content (Join-Path $root 'database/evidencias/d27-testes-offline.json') -Encoding UTF8
$r|Select-Object checks,falhas,parserSQL,sqlExecutado|ConvertTo-Json -Compress;if($r.falhas){exit 2}
