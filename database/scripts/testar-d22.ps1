[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$Db=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
. (Join-Path $PSScriptRoot 'iniciar-bancos.ps1')
. (Join-Path $PSScriptRoot 'd22-fixture.ps1')
$casos=New-Object 'Collections.Generic.List[object]'
function Caso([string]$Nome,[scriptblock]$Teste){$erro=$null;try{& $Teste}catch{$erro=$_.Exception.Message};$casos.Add([pscustomobject]@{caso=$Nome;aprovado=($null -eq $erro);erro=$erro})}
function Exigir([bool]$Ok){if(-not $Ok){throw 'D22_FIXTURE_DIVERGENTE'}}
function Recusa([scriptblock]$Acao,[string]$Codigo){$falhou=$false;try{& $Acao}catch{$falhou=$_.Exception.Message -like ('*'+$Codigo+'*')};Exigir $falhou}
function Disposto([Security.SecureString]$Senha){try{$x=$Senha.Copy();$x.Dispose();$false}catch{$e=$_.Exception;while($e.InnerException){$e=$e.InnerException};$e -is [ObjectDisposedException]}}
function Descartes($F){Exigir (Disposto $F.estado.senha);foreach($c in $F.estado.copias){Exigir (Disposto $c)}}
Caso 'Offline sem credencial metadados prompt ou SQL' {
    $r=Invoke-WmsD22InicioAutomatico -Offline -Adaptadores @{Senha={throw 'indevido'};Ferramentas={throw 'indevido'}}
    Exigir ($r.resultado -ceq 'OFFLINE_OK' -and $r.senhaLida -eq $false -and $r.conexoes -eq 0)
}
Caso 'DPAPI REAL ficticia Export Import SecureString somente nenhuma SQL' {
    $f=New-WmsD22LauncherFixture
    $m=Get-WmsD22Metadados $f.estado.contexto;Exigir ($m.aclValida -and -not $m.conteudoLido)
    $s=Read-WmsD22Credencial $f.estado.contexto
    try{Exigir ($s -is [Security.SecureString] -and $s.IsReadOnly() -and $s.Length -eq 32)}finally{$s.Dispose()}
}
Caso 'Automatica sem confirmar DEV completo primeiro e 13 copias descartadas' {
    $f=New-WmsD22LauncherFixture;$f.adaptadores.Confirmar={throw 'PROMPT_INDEVIDO'}
    $r=Invoke-WmsD22InicioAutomatico -Adaptadores $f.adaptadores
    Exigir ($r.resultado -ceq 'DEV_PROD_BOOTSTRAP_UPGRADE_CONCLUIDO' -and $f.estado.copias.Count -eq 13)
    Exigir (($f.estado.eventos -join ',') -ceq 'Credencial,Preflight,Inspect,Create:WMS_DEV,PreValidate:WMS_DEV,Migrate:WMS_DEV,PostValidate:WMS_DEV,Info:WMS_DEV,Schema:WMS_DEV,Create:WMS_PROD,PreValidate:WMS_PROD,Migrate:WMS_PROD,PostValidate:WMS_PROD,Info:WMS_PROD,Schema:WMS_PROD')
    Descartes $f
}
Caso 'Ausente falha rapida antes preflight SQL sem prompt orienta setup' {
    $f=New-WmsD22LauncherFixture AUSENTE;$r=Invoke-WmsD22InicioAutomatico -Adaptadores $f.adaptadores
    Exigir ($r.resultado -ceq 'FALHA' -and $r.codigo -ceq 'D22_CREDENCIAL_AUSENTE' -and $r.explicacao.Contains('configurar-credencial.bat') -and ($f.estado.eventos -join ',') -ceq 'Credencial' -and $f.estado.ddl -eq 0)
}
Caso 'Owner SID corrente divergente recusa antes Import' {
    $f=New-WmsD22LauncherFixture;$ctx=$f.estado.contexto.PSObject.Copy();$ctx.sid='S-1-5-21-1-2-3-987'
    $m=Get-WmsD22Metadados $ctx;Exigir (-not $m.aclValida -and -not $m.conteudoLido)
    Recusa {Read-WmsD22Credencial $ctx} 'D22_CREDENCIAL_ACL_OWNER_INVALIDOS'
}
Caso 'ACL heranca reativada recusa sem import e sem reparo automatico' {
    $f=New-WmsD22LauncherFixture;$ctx=$f.estado.contexto
    $a=New-WmsD22Security $ctx.sid $false;$a.SetAccessRuleProtection($false,$true);[IO.File]::SetAccessControl($ctx.arquivo,$a)
    Recusa {Read-WmsD22Credencial $ctx} 'D22_CREDENCIAL_ACL_OWNER_INVALIDOS'
    $r=Invoke-WmsD22InicioAutomatico -Adaptadores $f.adaptadores;Exigir ($r.resultado -ceq 'FALHA' -and $f.estado.ddl -eq 0)
    Exigir (-not (Get-Acl -LiteralPath $ctx.arquivo).AreAccessRulesProtected)
}
Caso 'ACL terceiro SID recusado sem importar' {
    $f=New-WmsD22LauncherFixture;$ctx=$f.estado.contexto
    $a=New-WmsD22Security $ctx.sid $false
    $a.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule((New-Object Security.Principal.SecurityIdentifier('S-1-1-0')),[Security.AccessControl.FileSystemRights]::Read,[Security.AccessControl.AccessControlType]::Allow)))
    [IO.File]::SetAccessControl($ctx.arquivo,$a)
    Recusa {Read-WmsD22Credencial $ctx} 'D22_CREDENCIAL_ACL_OWNER_INVALIDOS'
}
Caso 'ACL file correta mas pasta privada insuficiente recusa' {
    $f=New-WmsD22LauncherFixture;$ctx=$f.estado.contexto
    $a=New-WmsD22Security $ctx.sid $true;$a.SetAccessRuleProtection($false,$true);[IO.Directory]::SetAccessControl($ctx.pasta,$a)
    Recusa {Read-WmsD22Credencial $ctx} 'D22_CREDENCIAL_ACL_OWNER_INVALIDOS'
}
Caso 'Clixml ficticio corrompido erro DPAPI/import sanitizado sem SQL prompt' {
    $f=New-WmsD22LauncherFixture;$ctx=$f.estado.contexto
    [IO.File]::WriteAllText($ctx.arquivo,'ficticio invalido sem segredo')
    $r=Invoke-WmsD22InicioAutomatico -Adaptadores $f.adaptadores
    Exigir ($r.codigo -ceq 'D22_CREDENCIAL_DPAPI_INVALIDA_NESTE_USUARIO_MAQUINA' -and $f.estado.ddl -eq 0 -and $r.explicacao.Contains('configurar-credencial.bat'))
}
Caso 'Tipo PSCredential invalido recusado' {
    $f=New-WmsD22LauncherFixture;[pscustomobject]@{ficticio=1}|Export-Clixml -LiteralPath $f.estado.contexto.arquivo -Force
    Recusa {Read-WmsD22Credencial $f.estado.contexto} 'D22_CREDENCIAL_TIPO_LOGIN_OU_SENHA_INVALIDOS'
}
Caso 'Login diferente de sa recusado DPAPI ficticia' {
    $f=New-WmsD22LauncherFixture;$s=New-Object Security.SecureString;$s.AppendChar('x')
    try{(New-Object Management.Automation.PSCredential('FICTICIO',$s))|Export-Clixml -LiteralPath $f.estado.contexto.arquivo -Force}finally{$s.Dispose()}
    Recusa {Read-WmsD22Credencial $f.estado.contexto} 'D22_CREDENCIAL_TIPO_LOGIN_OU_SENHA_INVALIDOS'
}
Caso 'Senha vazia DPAPI ficticia recusada' {
    $f=New-WmsD22LauncherFixture;$s=New-Object Security.SecureString
    try{(New-Object Management.Automation.PSCredential('sa',$s))|Export-Clixml -LiteralPath $f.estado.contexto.arquivo -Force}finally{$s.Dispose()}
    Recusa {Read-WmsD22Credencial $f.estado.contexto} 'D22_CREDENCIAL_TIPO_LOGIN_OU_SENHA_INVALIDOS'
}
Caso 'Caminho fora allowlist recusado sem abrir arquivo' {
    $f=New-WmsD22LauncherFixture;$ctx=$f.estado.contexto.PSObject.Copy();$ctx.arquivo=Join-Path $ctx.base 'nao-autorizado.clixml'
    Recusa {Read-WmsD22Credencial $ctx} 'D22_CAMINHO_FIXO_DIVERGENTE'
}
Caso 'Configurar ficticia novamente substitui atomico e sem tmp residual' {
    $f=New-WmsD22LauncherFixture;$s=New-Object Security.SecureString;$s.AppendChar('f');$s.AppendChar('2')
    try{$null=Save-WmsD22Credencial $f.estado.contexto $s;$r=Read-WmsD22Credencial $f.estado.contexto;try{Exigir ($r.Length -eq 2)}finally{$r.Dispose()}}finally{$s.Dispose()}
    Exigir (@(Get-ChildItem -LiteralPath $f.estado.contexto.pasta -Filter '*.tmp.clixml' -File).Count -eq 0)
}
Caso 'Erro DEV bloqueia PROD fluxo D21 preservado' {
    $f=New-WmsD22LauncherFixture DEV;$r=Invoke-WmsD22InicioAutomatico -Adaptadores $f.adaptadores
    Exigir ($r.resultado -ceq 'FALHA' -and @($f.estado.eventos|Where-Object {$_ -match 'WMS_PROD'}).Count -eq 0);Descartes $f
}
Caso 'Identidade divergente sem DDL e sem confirmar' {
    $f=New-WmsD22LauncherFixture IDENTIDADE;$r=Invoke-WmsD22InicioAutomatico -Adaptadores $f.adaptadores
    Exigir ($r.resultado -ceq 'FALHA' -and $f.estado.ddl -eq 0);Descartes $f
}
Caso 'AST normal nao possui ReadHost Confirmar pause parentenv senha' {
    $s=[IO.File]::ReadAllText((Join-Path $Db 'scripts/iniciar-bancos.ps1'))
    $c=[IO.File]::ReadAllText((Join-Path $Db 'scripts/d22-credencial.ps1'))
    $bat=[IO.File]::ReadAllText((Join-Path $Db 'iniciar-bancos.bat'))
    Exigir ($s -notmatch 'Read-Host|Adaptadores\.Confirmar' -and $c -notmatch 'GetNetworkCredential|SetEnvironmentVariable' -and $bat -notmatch 'pause|Read-Host|Bypass|ExecutionPolicy' -and $bat.Contains('-NonInteractive'))
}
Caso 'Historicos D21, migrations e manifestos preservados apos integracao D24' {
    $snap=Get-Content -LiteralPath (Join-Path $Db 'evidencias/d22-snapshot-d21.json') -Raw -Encoding UTF8|ConvertFrom-Json
    $raiz=Split-Path $Db -Parent
    foreach($f in $snap.copias){Exigir ((Get-FileHash -LiteralPath (Join-Path $raiz $f.copia) -Algorithm SHA256).Hash -ceq $f.sha256)}
    # D24 altera os adaptadores TLS/Flyway e a normalizacao do catalogo.
    # Suas copias historicas acima e todas as migrations continuam imutaveis.
    foreach($f in $snap.copias|Where-Object {$_.arquivo -match '(^database/migrations/.*\.sql$|^database/evidencias/.*manifesto)'}){
        Exigir ((Get-FileHash -LiteralPath (Join-Path $raiz $f.arquivo) -Algorithm SHA256).Hash -ceq $f.sha256)
    }
}
$falhas=@($casos|Where-Object {-not $_.aprovado})
[pscustomobject]@{natureza='D22_TESTES_CANAL_FICTICIO_DPAPI_REAL_SEM_SQL';total=$casos.Count;aprovados=$casos.Count-$falhas.Count;falhas=$falhas.Count;casos=$casos.ToArray();credencialWmsRealImportada=$false;credencialRealProvisionada=$false;sqlExecutado=$false;mavenExecutado=$false;normalBatCaminhoRealExecutado=$false}|ConvertTo-Json -Depth 7
if($falhas.Count){exit 1}
