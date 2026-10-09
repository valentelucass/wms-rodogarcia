# PROD-LAUNCH01. Dot-source apenas declara funcoes; build explicito sem banco/Boot/testes.
function Get-WmsProductionSourceFiles {
    param([string]$Root)
    $backend = Join-Path $Root 'backend'
    $files = @(Get-ChildItem -LiteralPath (Join-Path $backend 'src/main') -File -Recurse) +
        @(Get-ChildItem -LiteralPath (Join-Path $backend '.mvn') -File -Recurse)
    foreach ($name in @('pom.xml', 'mvnw', 'mvnw.cmd')) { $files += Get-Item -LiteralPath (Join-Path $backend $name) }
    @($files | Sort-Object FullName | ForEach-Object {
        if ($_.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'PROD_SOURCE_REPARSE_REFUSED' }
        [pscustomobject]@{ path = $_.FullName.Substring($backend.Length + 1).Replace('\', '/')
            sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant(); bytes = $_.Length }
    })
}

function Assert-WmsProductionCandidateDirectory {
    param([string]$CandidateDirectory)
    $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
    $base = [IO.Path]::GetFullPath((Join-Path $root 'orchestracao/.runtime/launcher-producao'))
    $candidate = [IO.Path]::GetFullPath($CandidateDirectory)
    if (-not $candidate.StartsWith($base + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'PROD_CANDIDATE_OUTSIDE_OWN_RUNTIME'
    }
    $walk = $candidate
    while ($walk -and $walk.Length -ge $base.Length) {
        if ((Test-Path -LiteralPath $walk) -and ((Get-Item -LiteralPath $walk -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
            throw 'PROD_CANDIDATE_REPARSE_REFUSED'
        }
        $walk = [IO.Path]::GetDirectoryName($walk)
    }
    if ((Test-Path -LiteralPath $candidate) -and @(Get-ChildItem -LiteralPath $candidate -Force).Count) {
        throw 'PROD_CANDIDATE_NOT_EMPTY'
    }
    $candidate
}

function Get-WmsProductionBuildChildHandles {
    param([object]$Parent)
    $children = New-Object 'Collections.Generic.List[object]'
    if ($Parent.HasExited) { return [pscustomobject]@{ Handles = @(); Complete = $true } }
    $complete = $true
    try {
        $parentStart = $Parent.StartTime.ToUniversalTime()
        $rows = @(Get-CimInstance -ClassName Win32_Process -Filter ('ParentProcessId=' + $Parent.Id) -ErrorAction Stop)
    } catch { return [pscustomobject]@{ Handles = @(); Complete = $false } }
    foreach ($row in $rows) {
        $child = $null
        try {
            if ($Parent.HasExited) { throw 'PROD_BUILD_CHILD_OWNERSHIP_NOT_CONFIRMED' }
            $child = Get-Process -Id $row.ProcessId -ErrorAction Stop
            $null = $child.Handle
            $start = $child.StartTime.ToUniversalTime()
            $observed = ([DateTime]$row.CreationDate).ToUniversalTime()
            if ($start -lt $parentStart -or [Math]::Abs(($start - $observed).TotalMilliseconds) -gt 100 -or $row.ParentProcessId -ne $Parent.Id) {
                throw 'PROD_BUILD_CHILD_OWNERSHIP_NOT_CONFIRMED'
            }
            # Referencias verificadas, posordem. Nao matar por PID/porta/executavel.
            $descendants = Get-WmsProductionBuildChildHandles $child
            foreach ($descendant in $descendants.Handles) { $children.Add($descendant) }
            if (-not $descendants.Complete) { $complete = $false }
            $children.Add($child); $child = $null
        } catch { $complete = $false } finally { if ($child) { $child.Dispose() } }
    }
    [pscustomobject]@{ Handles = $children.ToArray(); Complete = $complete }
}

function Stop-WmsProductionBuildHandle {
    param([Parameter(Mandatory = $true)][object]$Handle)
    if (-not (Get-Variable WmsProductionBuildHandles -Scope Script -ErrorAction SilentlyContinue) -or
        -not $script:WmsProductionBuildHandles.ContainsKey([string]$Handle.RunId) -or
        -not [object]::ReferenceEquals($script:WmsProductionBuildHandles[[string]$Handle.RunId], $Handle)) {
        throw 'PROD_FOREIGN_BUILD_HANDLE_REFUSED'
    }
    $process = $Handle.Process
    $childrenClosed = $true
    try {
        $children = [pscustomobject]@{ Handles = @($Handle.Children); Complete = $true }
        if (-not $process.HasExited) {
            $children = Get-WmsProductionBuildChildHandles $process
            $Handle.Children = @($children.Handles)
        }
        $cleanupCode = $null
        $retainedChildren = New-Object 'Collections.Generic.List[object]'
        foreach ($child in $children.Handles) {
            try { if (-not $child.HasExited) { $child.Kill(); $null = $child.WaitForExit(5000); if (-not $child.HasExited) { throw 'PROD_BUILD_CHILD_STOP_TIMEOUT' } } }
            catch { $cleanupCode = 'PROD_BUILD_CHILD_STOP_FAILED'; $childrenClosed = $false }
            finally { if ($child.HasExited) { $child.Dispose() } else { $retainedChildren.Add($child) } }
        }
        $Handle.Children = $retainedChildren.ToArray()
        if ($cleanupCode) { throw $cleanupCode }
        if (-not $children.Complete) { throw 'PROD_BUILD_CHILD_OWNERSHIP_NOT_CONFIRMED' }
    } finally {
        # Falha ao consultar filhos nunca pula encerramento/espera do handle proprio.
        if (-not $process.HasExited) { $process.Kill(); $null = $process.WaitForExit(5000); if (-not $process.HasExited) { throw 'PROD_BUILD_STOP_TIMEOUT' } }
        if ($process.HasExited -and $childrenClosed) { $null = $script:WmsProductionBuildHandles.Remove([string]$Handle.RunId) }
    }
}

function Build-WmsProductionBackend {
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][string]$CandidateDirectory,
        [string]$JavaPath = 'C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21/bin/java.exe')
    $ErrorActionPreference = 'Stop'
    $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
    $candidate = Assert-WmsProductionCandidateDirectory $CandidateDirectory
    $java = [IO.Path]::GetFullPath($JavaPath)
    $javaHome = [IO.Path]::GetDirectoryName([IO.Path]::GetDirectoryName($java))
    if (-not (Test-Path -LiteralPath $java -PathType Leaf) -or [IO.Path]::GetFileName($java) -ine 'java.exe' -or
        -not ([IO.File]::ReadAllText((Join-Path $javaHome 'release')) -match '(?m)^JAVA_VERSION="21[.\-"]')) {
        throw 'PROD_JDK21_REQUIRED'
    }
    if ($candidate -match '["&|<>^%!\r\n]' -or $javaHome -match '["&|<>^%!\r\n]') { throw 'PROD_BUILD_PATH_INVALID' }
    $initial = @(Get-WmsProductionSourceFiles $root)
    $null = New-Item -ItemType Directory -Path $candidate -Force
    foreach ($file in $initial) {
        $source = Join-Path (Join-Path $root 'backend') $file.path
        $destination = Join-Path $candidate $file.path
        $null = New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($destination)) -Force
        Copy-Item -LiteralPath $source -Destination $destination
        if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ine $file.sha256) { throw 'PROD_SOURCE_CHANGED_DURING_CAPTURE' }
    }
    $manifestPath = Join-Path $candidate 'source-manifest.json'
    $manifest = [ordered]@{ natureza = 'PROD_LAUNCH01_BACKEND_SOURCE_CAPTURE'; observadoUtc = [DateTime]::UtcNow.ToString('o')
        root = $root; files = $initial; testsCopied = $false; sqlExecuted = $false }
    [IO.File]::WriteAllText($manifestPath, ($manifest | ConvertTo-Json -Depth 6), (New-Object Text.UTF8Encoding($false)))
    $sourceSha = (Get-FileHash -LiteralPath $manifestPath -Algorithm SHA256).Hash.ToLowerInvariant()
    $receiptPath = Join-Path $candidate 'backend-build.json'
    $logPath = Join-Path $candidate 'backend-build.log'
    $receipt = [ordered]@{ natureza = 'PROD_LAUNCH01_BACKEND_BUILD'; estado = 'BLOCKED'; codigo = 'PROD_BUILD_NOT_COMPLETED'
        inicioUtc = [DateTime]::UtcNow.ToString('o'); finishedUtc = $null; sourceSha256 = $sourceSha
        sourceManifest = @{ path = $manifestPath; sha256 = $sourceSha }; sourceCount = $initial.Count
        jar = $null; config = $null; java = @{ path = $java; sha256 = (Get-FileHash -LiteralPath $java -Algorithm SHA256).Hash.ToLowerInvariant() }
        testsExecuted = 0; sqlExecuted = $false; backendStarted = $false; previousArtifactsChanged = $false }
    $process = $null; $started = $false; $handle = $null
    try {
        $info = New-Object Diagnostics.ProcessStartInfo
        $info.FileName = Join-Path ([Environment]::GetFolderPath('Windows')) 'System32/cmd.exe'
        $info.WorkingDirectory = $candidate; $info.UseShellExecute = $false; $info.CreateNoWindow = $true
        $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true
        $info.EnvironmentVariables.Clear()
        foreach ($name in @('SystemRoot', 'WINDIR', 'TEMP', 'TMP', 'USERPROFILE', 'LOCALAPPDATA', 'APPDATA')) {
            $value = [Environment]::GetEnvironmentVariable($name, 'Process')
            if ($value) { $info.EnvironmentVariables[$name] = $value }
        }
        $info.EnvironmentVariables['JAVA_HOME'] = $javaHome
        $info.EnvironmentVariables['PATH'] = (Join-Path $javaHome 'bin') + ';' + (Join-Path $env:SystemRoot 'System32') + ';' + (Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0')
        $info.EnvironmentVariables['MAVEN_OPTS'] = '-Xmx768m'
        $info.Arguments = '/d /s /c ""' + (Join-Path $candidate 'mvnw.cmd') + '" -o -B -ntp -Dmaven.test.skip=true -DskipTests=true -Dwms.migrations.skip=true -Dwms.bootstrap.skip=true package"'
        $process = New-Object Diagnostics.Process; $process.StartInfo = $info
        if (-not (Get-Variable WmsProductionBuildHandles -Scope Script -ErrorAction SilentlyContinue)) { $script:WmsProductionBuildHandles = @{} }
        $handle = [pscustomobject]@{ RunId = [Guid]::NewGuid().ToString('N'); Process = $process; Children = @() }
        $script:WmsProductionBuildHandles[$handle.RunId] = $handle
        if (-not $process.Start()) { throw 'PROD_BUILD_PROCESS_REFUSED' }
        $started = $true
        $stdout = $process.StandardOutput.ReadToEndAsync(); $stderr = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        [IO.File]::WriteAllText($logPath, $stdout.GetAwaiter().GetResult() + $stderr.GetAwaiter().GetResult(), (New-Object Text.UTF8Encoding($false)))
        $receipt.exitCode = $process.ExitCode
        if ($process.ExitCode -ne 0) { throw 'PROD_BACKEND_BUILD_FAILED' }
        $final = @(Get-WmsProductionSourceFiles $root)
        if (($initial | ConvertTo-Json -Depth 4 -Compress) -cne ($final | ConvertTo-Json -Depth 4 -Compress)) {
            throw 'PROD_LIVE_SOURCE_CHANGED_BUILD_NOT_PUBLISHED'
        }
        $jar = Join-Path $candidate 'target/wms-backend-0.0.1-SNAPSHOT.jar'
        $config = Join-Path $candidate 'src/main/resources/application-frontend-prod.properties'
        if (-not (Test-Path -LiteralPath $jar -PathType Leaf) -or -not (Test-Path -LiteralPath $config -PathType Leaf)) {
            throw 'PROD_BUILD_ARTIFACT_MISSING'
        }
        $receipt.jar = @{ path = $jar; sha256 = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant() }
        $receipt.config = @{ path = $config; sha256 = (Get-FileHash -LiteralPath $config -Algorithm SHA256).Hash.ToLowerInvariant() }
        $receipt.estado = 'PREPARED'; $receipt.codigo = 'PROD_BACKEND_CANDIDATE_BUILT_NO_START'
    } catch {
        $code = [string]$_.Exception.Message
        $receipt.codigo = if ($code -match '^PROD_[A-Z0-9_]+$') { $code } else { 'PROD_BACKEND_BUILD_FAILED_SANITIZED' }
        throw $receipt.codigo
    } finally {
        try {
            if ($started) { Stop-WmsProductionBuildHandle -Handle $handle }
        } finally {
            try {
                $receipt.finishedUtc = [DateTime]::UtcNow.ToString('o')
                [IO.File]::WriteAllText($receiptPath, ($receipt | ConvertTo-Json -Depth 6), (New-Object Text.UTF8Encoding($false)))
            } finally {
                # Recibo indisponivel nao pode impedir descarte. Se kill falhar, preservar handle registrado.
                if ($process -and -not $started) {
                    if ($handle -and (Get-Variable WmsProductionBuildHandles -Scope Script -ErrorAction SilentlyContinue)) { $null = $script:WmsProductionBuildHandles.Remove($handle.RunId) }
                    $process.Dispose()
                } elseif ($process -and $process.HasExited -and -not $script:WmsProductionBuildHandles.ContainsKey($handle.RunId)) { $process.Dispose() }
            }
        }
    }
    [pscustomobject]@{ jar = $receipt.jar; config = $receipt.config; java = $receipt.java; sourceSha256 = $sourceSha
        sourceManifest = $receipt.sourceManifest; CandidateDirectory = $candidate; ReceiptPath = $receiptPath
        ReceiptSha256 = (Get-FileHash -LiteralPath $receiptPath -Algorithm SHA256).Hash.ToLowerInvariant() }
}
