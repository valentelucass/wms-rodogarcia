# Somente contrato em memoria e material FICTICIO. Sem SQL/credencial real/processos existentes.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'contrato-banco.ps1')
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$historicalPath = Join-Path $root 'orchestracao/.runtime/d29-farol-retomada-preflight.json'
$before = (Get-FileHash -LiteralPath $historicalPath).Hash
$historical = Get-Content -LiteralPath $historicalPath -Raw -Encoding UTF8 | ConvertFrom-Json
$migration = [pscustomobject]@{version='11';script='V11__login_usuarios_e_sessoes.sql';checksum=123}
$expected = Add-WmsAuthExpectedContract $historical $migration
$checks = [ordered]@{}
$checks.catalogo = $expected.catalogo.tabelas -eq 68 -and $expected.catalogo.colunas -eq 716
$checks.historicoPreservado = $historical.catalogo.tabelas -eq 64 -and (Get-FileHash -LiteralPath $historicalPath).Hash -eq $before
$checks.semPermissoesAmplas = @($expected.rights.objects | Where-Object { $_.tabela -in @('usuario_acesso','sessao_acesso','renovacao_acesso','evento_acesso') -and $_.permissao -notin @('SELECT','INSERT') -and $_.permitido -ne 0 }).Count -eq 0
$checks.principalImutavel = @($expected.rights.columns | Where-Object { $_.tabela -eq 'usuario_acesso' -and $_.coluna -in @('id','email','principal') -and $_.permitido -ne 0 }).Count -eq 0
$checks.auditoriaImutavel = @($expected.rights.columns | Where-Object { $_.tabela -eq 'evento_acesso' -and $_.permitido -ne 0 }).Count -eq 0
$rejected = $false
try { $null = Add-WmsAuthExpectedContract $historical ([pscustomobject]@{version='12';script='desconhecido.sql'}) } catch { $rejected = $true }
$checks.extensaoDesconhecidaRecusada = $rejected
$java = Join-Path $env:LOCALAPPDATA 'Programs/Eclipse Adoptium/jdk-21/bin/java.exe'
$info = [Diagnostics.ProcessStartInfo]::new()
$info.FileName=$java; $info.Arguments='"'+(Join-Path $PSScriptRoot 'PrepararLogin.java')+'"'
$info.UseShellExecute=$false; $info.CreateNoWindow=$true
$info.RedirectStandardInput=$true; $info.RedirectStandardOutput=$true; $info.RedirectStandardError=$true
$info.EnvironmentVariables.Remove('JAVA_TOOL_OPTIONS'); $info.EnvironmentVariables.Remove('_JAVA_OPTIONS'); $info.EnvironmentVariables.Remove('JDK_JAVA_OPTIONS')
$p = [Diagnostics.Process]::Start($info)
try {
    $output=$p.StandardOutput.ReadToEndAsync(); $errorOutput=$p.StandardError.ReadToEndAsync()
    $writer=[IO.StreamWriter]::new($p.StandardInput.BaseStream,[Text.UTF8Encoding]::new($false))
    try { $writer.WriteLine('Fixture-local-senha-123!'); $writer.Flush() } finally { $writer.Dispose() }
    if (-not $p.WaitForExit(60000)) { $p.Kill(); throw 'HELPER_FICTICIO_TIMEOUT' }
    $null=$errorOutput.Result
    if ($p.ExitCode -ne 0) { throw 'HELPER_FICTICIO_FALHOU' }
    $material=$output.Result | ConvertFrom-Json
    $checks.material = $material.schema -eq 1 -and $material.bootstrapHash -match '^\{pbkdf2-600k\}[0-9a-f]{96}$' -and ([Convert]::FromBase64String($material.privateKey)).Length -gt 1000
    $hex=$material.bootstrapHash.Substring('{pbkdf2-600k}'.Length)
    $salt=New-Object byte[] 16
    for($i=0;$i -lt 16;$i++) {$salt[$i]=[Convert]::ToByte($hex.Substring($i*2,2),16)}
    $pbkdf=[Security.Cryptography.Rfc2898DeriveBytes]::new('Fixture-local-senha-123!',$salt,600000,[Security.Cryptography.HashAlgorithmName]::SHA256)
    try { $computed=([BitConverter]::ToString($pbkdf.GetBytes(32))).Replace('-','').ToLowerInvariant() } finally { $pbkdf.Dispose() }
    $checks.hashInteropJavaDotnet = $computed -ceq $hex.Substring(32)
    $protected=ConvertTo-SecureString $output.Result -AsPlainText -Force
    try {
        $cipher=ConvertFrom-SecureString $protected
        $checks.dpapiSemLiteral = -not $cipher.Contains('Fixture-local') -and -not $cipher.Contains('privateKey')
    } finally { $protected.Dispose() }
} finally { $material=$null; $output=$null; $p.Dispose() }
$result=[ordered]@{natureza='D32_TESTE_ISOLADO';observadoUtc=[DateTime]::UtcNow.ToString('o');sql=$false;credencialReal=$false;checks=$checks;passed=@($checks.Values | Where-Object {$_}).Count;total=$checks.Count}
$directory=Join-Path $root 'orchestracao/.runtime/login-d32'
$null=New-Item -ItemType Directory -Path $directory -Force
$result|ConvertTo-Json -Depth 5|Set-Content -LiteralPath (Join-Path $directory 'scripts-local.json') -Encoding UTF8
$result|ConvertTo-Json -Depth 5
if ($result.passed -ne $result.total) { exit 1 }
