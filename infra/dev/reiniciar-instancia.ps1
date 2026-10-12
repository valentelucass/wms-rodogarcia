# Biblioteca local: nao inicia processos, SQL ou encerramento ao ser carregada.
Set-StrictMode -Version Latest

function Assert-WmsDevRestartPorts([int]$BackendPort, [int]$FrontendPort) {
    if ($BackendPort -ne 25580 -or $FrontendPort -ne 25581) { throw 'DEV_RESTART_PORTS_NOT_ALLOWED' }
}

function Get-WmsDevListeners {
    @(Get-NetTCPConnection -State Listen -ErrorAction Stop)
}

function Split-WmsDevCommandLine([string]$CommandLine) {
    if (-not ('WmsDevRestart.CommandLine' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
namespace WmsDevRestart {
    public static class CommandLine {
        [DllImport("shell32.dll", SetLastError=true, CharSet=CharSet.Unicode)]
        public static extern IntPtr CommandLineToArgvW(string command, out int count);
        [DllImport("kernel32.dll")] public static extern IntPtr LocalFree(IntPtr memory);
    }
}
'@
    }
    $count = 0
    $pointer = [WmsDevRestart.CommandLine]::CommandLineToArgvW($CommandLine, [ref]$count)
    if ($pointer -eq [IntPtr]::Zero) { throw 'DEV_RESTART_COMMAND_UNREADABLE' }
    try {
        for ($i=0; $i -lt $count; $i++) {
            [Runtime.InteropServices.Marshal]::PtrToStringUni([Runtime.InteropServices.Marshal]::ReadIntPtr($pointer, $i * [IntPtr]::Size))
        }
    } finally { $null = [WmsDevRestart.CommandLine]::LocalFree($pointer) }
}

function Test-WmsDevProcessProof($Receipt, $Process, [string[]]$Arguments, [string]$Executable, [string]$Repository, [int]$Port) {
    try {
        $root = [IO.Path]::GetFullPath($Repository).TrimEnd('\','/')
        if ([IO.Path]::GetFullPath($Receipt.projectPath).TrimEnd('\','/') -ine $root -or
            $Receipt.natureza -cne 'D31_DEV02_LAUNCHER_REAL' -or $Receipt.modo -cne 'real' -or
            $Receipt.database -cne 'WMS_DEV' -or $Receipt.login -cne 'WMSDEV' -or
            $Receipt.backendPort -ne 25580 -or $Receipt.frontendPort -ne 25581 -or
            $Port -notin @(25580,25581)) { return $false }
        if ($Port -eq 25580) {
            $be = $Receipt.backend
            $proof = $be.backend
            if ($be.natureza -cne 'D31_DEV02_BACKEND_READINESS' -or $be.profile -cne 'sqlserver-dev' -or
                $be.database -cne 'WMS_DEV' -or $be.login -cne 'WMSDEV' -or $proof.port -ne $Port -or
                $Executable -ine $be.java.path -or [IO.Path]::GetFileName($Executable) -ine 'java.exe') { return $false }
            $jar = [IO.Path]::GetFullPath($be.artifact.path)
            if (-not $jar.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) { return $false }
            if($be.PSObject.Properties.Name -contains 'id' -and $be.id -ceq 'BE02-DEV-FONTE01'){
                . (Join-Path $root 'infra/dev/artefato-backend.ps1')
                if($be.estado -cne 'READY' -or !$be.jdbcProviderProved -or $be.observationEnabled -ne $false -or
                    $be.bootstrapDisabled -ne $true -or $be.candidate.path -ine (Join-Path (Split-Path $be.guard.path -Parent) 'preparo-candidato.json')){return $false}
                foreach($ref in @($be.artifact,$be.config,$be.java,$be.candidate,$be.guard,$be.jdbc)){
                    if((Get-FileHash -LiteralPath $ref.path).Hash -ine $ref.sha256){return $false}
                }
                $g=Get-Content -LiteralPath $be.guard.path -Raw -Encoding UTF8|ConvertFrom-Json
                $access=@{mode=$be.accessMode}
                $plan=@{root=$root;jar=$jar;config=$be.config.path;guard=$g;truststore=$g.perfilProtegido.truststore;run=(Split-Path $be.guard.path -Parent);runId=$be.runId;sourceHash=$be.sourceHash;jarSha256=$be.artifact.sha256;access=$access;localOrigin=$be.localOrigin}
                $expected=@($Executable)+@(Get-WmsDevCurrentBackendArguments $plan)
            }else{
                $config = Join-Path $root 'backend/src/main/resources/application-frontend-dev.properties'
                $expected = @($Executable, '-jar', $jar, '--spring.profiles.active=sqlserver-dev',
                '--spring.config.location=classpath:/application.properties,classpath:/application-sqlserver-dev.properties',
                ('--spring.config.additional-location=' + ([Uri]$config).AbsoluteUri),
                '--server.address=127.0.0.1', '--server.port=25580', '--wms.cadastros.enabled=true',
                '--spring.autoconfigure.exclude=', '--spring.jpa.hibernate.ddl-auto=validate', '--spring.jpa.generate-ddl=false',
                '--spring.sql.init.mode=never', '--spring.flyway.enabled=false', '--spring.liquibase.enabled=false',
                '--wms.database.name=WMS_DEV', '--wms.database.user=WMSDEV')
            }
        } else {
            $proof = $Receipt.frontend
            if ($proof.mode -cne 'real' -or [IO.Path]::GetFileName($Executable) -ine 'node.exe') { return $false }
            $expected = @($Executable, (Join-Path $root 'frontend/node_modules/vite/bin/vite.js'), '--config', 'vite.dev.config.ts')
        }
        $processStart=if($proof.PSObject.Properties.Name -contains 'nativeStartUtc'){$proof.nativeStartUtc}else{$proof.startUtc}
        if (-not $proof.listenerOwned -or $proof.pid -ne $Process.Id -or
            [DateTimeOffset]::Parse($processStart).UtcDateTime.Ticks -ne $Process.StartTime.ToUniversalTime().Ticks -or
            $Arguments.Count -ne $expected.Count) { return $false }
        for ($i=0; $i -lt $expected.Count; $i++) {
            if ($Arguments[$i] -cne $expected[$i]) { return $false }
        }
        return $true
    } catch { return $false }
}

function Read-WmsDevRestartReceipt([string]$Path) {
    if ((Get-Item -LiteralPath $Path).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'DEV_RESTART_RECEIPT_CHANGED' }
    # Parse e hash dos MESMOS bytes: nao combinar duas leituras de um recibo mutavel.
    $bytes=[IO.File]::ReadAllBytes($Path)
    $hash=[Security.Cryptography.SHA256]::Create()
    try {$sha=([BitConverter]::ToString($hash.ComputeHash($bytes))).Replace('-','')}finally{$hash.Dispose()}
    $body=[Text.Encoding]::UTF8.GetString($bytes).TrimStart([char]0xfeff)|ConvertFrom-Json
    return @{path=$Path;sha256=$sha;body=$body}
}

function Write-WmsDevRestartSnapshot([string]$Path,$Body) {
    $stream=[IO.File]::Open($Path,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
    try {$bytes=[Text.Encoding]::UTF8.GetBytes(($Body|ConvertTo-Json -Depth 32)+[Environment]::NewLine);$stream.Write($bytes,0,$bytes.Length)}finally{$stream.Dispose()}
    return @{path=$Path;sha256=(Get-FileHash -LiteralPath $Path).Hash}
}

function New-WmsDevRestartSnapshot($Target,[string]$Repository,[string]$Directory) {
    Assert-WmsDevRestartTarget $Target $Repository @(Get-WmsDevListeners)
    $metadata=Get-CimInstance Win32_Process -Filter ('ProcessId='+$Target.Process.Id) -ErrorAction Stop
    $arguments=@(Split-WmsDevCommandLine $metadata.CommandLine)
    $hash=[Security.Cryptography.SHA256]::Create()
    try {$commandSha=([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes(($arguments -join [char]0))))).Replace('-','')}finally{$hash.Dispose()}
    $snapshot=[ordered]@{schema=1;natureza='WMS_DEV_RESTART_PROCESS_SNAPSHOT';projectPath=[IO.Path]::GetFullPath($Repository);previousRun=$Target.PreviousRun
        pid=$Target.Process.Id;startUtc=$Target.Process.StartTime.ToUniversalTime().ToString('o');port=$Target.Port
        executable=$metadata.ExecutablePath;commandSha256=$commandSha;capturedUtc=[DateTime]::UtcNow.ToString('o')
        originalReceipt=@{path=$Target.ReceiptPath;sha256=$Target.ReceiptSha256};receipt=$Target.Receipt}
    $ref=Write-WmsDevRestartSnapshot (Join-Path $Directory ('posse-'+$Target.Port+'-'+$Target.Process.Id+'.json')) $snapshot
    $Target|Add-Member NoteProperty Snapshot $ref
}

function Assert-WmsDevRestartReceipt($Target,$OwnStops) {
    $current=Read-WmsDevRestartReceipt $Target.ReceiptPath
    if($current.sha256 -ieq $Target.ReceiptSha256){return}
    # A unica mutacao admitida e a transicao exata do monitor, depois de uma parada
    # propria atestada neste mesmo recibo. Nunca absorver o novo recibo como posse.
    $witness=@($OwnStops|Where-Object {$_.receiptPath -ieq $Target.ReceiptPath -and $_.receiptSha256 -ieq $Target.ReceiptSha256 -and $_.process.HasExited})
    if(!$witness.Count -or $Target.Receipt.estado -cne 'PROCESSES_READY_ROUNDTRIP_PENDING' -or $current.body.estado -cne 'OWN_PROCESS_EXITED'){throw 'DEV_RESTART_RECEIPT_CHANGED'}
    $observed=[DateTimeOffset]::Parse($current.body.observadoUtc)
    if($observed -gt [DateTimeOffset]::UtcNow -or !@($witness|Where-Object {$observed -ge [DateTimeOffset]::Parse($_.stopRequestedUtc)}).Count){throw 'DEV_RESTART_RECEIPT_CHANGED'}
    $before=$Target.Receipt|Select-Object * -ExcludeProperty estado,observadoUtc|ConvertTo-Json -Depth 32 -Compress
    $after=$current.body|Select-Object * -ExcludeProperty estado,observadoUtc|ConvertTo-Json -Depth 32 -Compress
    if($before -cne $after){throw 'DEV_RESTART_RECEIPT_CHANGED'}
    if($Target.PSObject.Properties.Name -contains 'Snapshot' -and $Target.PSObject.Properties.Name -notcontains 'ReceiptTransition'){
        $proof=@{natureza='WMS_DEV_OWN_STOP_RECEIPT_TRANSITION';snapshot=$Target.Snapshot;originalReceipt=@{path=$Target.ReceiptPath;sha256=$Target.ReceiptSha256};afterSha256=$current.sha256
            observedUtc=$current.body.observadoUtc;verifiedUtc=[DateTime]::UtcNow.ToString('o');unchangedFields=$true
            ownStops=@($witness|ForEach-Object{@{pid=$_.process.Id;startUtc=$_.startUtc;port=$_.port;stopRequestedUtc=$_.stopRequestedUtc}})}
        $ref=Write-WmsDevRestartSnapshot (Join-Path (Split-Path $Target.Snapshot.path -Parent) ('transicao-posse-'+$Target.Port+'-'+$Target.Process.Id+'.json')) $proof
        $Target|Add-Member NoteProperty ReceiptTransition $ref
    }
}

function Assert-WmsDevRestartTarget($Target, [string]$Repository, $Listeners, $OwnStops=@()) {
    $receipt=$Target.Receipt
    $snapshot=$null
    if($Target.PSObject.Properties.Name -contains 'Snapshot'){
        $read=Read-WmsDevRestartReceipt $Target.Snapshot.path
        if($read.sha256 -ine $Target.Snapshot.sha256){throw 'DEV_RESTART_SNAPSHOT_CHANGED'}
        $snapshot=$read.body
        if($snapshot.natureza -cne 'WMS_DEV_RESTART_PROCESS_SNAPSHOT' -or $snapshot.projectPath -ine [IO.Path]::GetFullPath($Repository) -or
            $snapshot.pid -ne $Target.Process.Id -or $snapshot.port -ne $Target.Port -or $snapshot.previousRun -cne $Target.PreviousRun -or
            $snapshot.originalReceipt.path -ine $Target.ReceiptPath -or $snapshot.originalReceipt.sha256 -ine $Target.ReceiptSha256){throw 'DEV_RESTART_SNAPSHOT_CHANGED'}
        $receipt=$snapshot.receipt
        $Target.Receipt=$receipt
    }
    if($Target.PSObject.Properties.Name -contains 'ReceiptPath'){Assert-WmsDevRestartReceipt $Target $OwnStops}
    if ($Target.Process.HasExited) { return }
    $owned = @($Listeners | Where-Object { $_.OwningProcess -eq $Target.Process.Id })
    if(!$owned.Count){throw 'DEV_RESTART_OWNER_NOT_CONFIRMED'}
    if (@($owned | Where-Object { $_.LocalPort -ne $Target.Port -or $_.LocalAddress -cne '127.0.0.1' }).Count) {
        throw 'DEV_RESTART_OTHER_LISTENER_REFUSED'
    }
    $metadata = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $Target.Process.Id) -ErrorAction Stop
    if ($Target.Process.HasExited) { return }
    if ($null -eq $metadata -or [string]::IsNullOrWhiteSpace($metadata.CommandLine)) { throw 'DEV_RESTART_PROCESS_UNREADABLE' }
    $arguments = @(Split-WmsDevCommandLine $metadata.CommandLine)
    if($null -ne $snapshot){
        $hash=[Security.Cryptography.SHA256]::Create()
        try {$commandSha=([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes(($arguments -join [char]0))))).Replace('-','')}finally{$hash.Dispose()}
        if($snapshot.executable -cne $metadata.ExecutablePath -or $snapshot.commandSha256 -cne $commandSha -or
            [DateTimeOffset]::Parse($snapshot.startUtc).UtcDateTime.Ticks -ne $Target.Process.StartTime.ToUniversalTime().Ticks){throw 'DEV_RESTART_OWNER_NOT_CONFIRMED'}
    }
    if (-not (Test-WmsDevProcessProof $receipt $Target.Process $arguments $metadata.ExecutablePath $Repository $Target.Port)) {
        throw 'DEV_RESTART_OWNER_NOT_CONFIRMED'
    }
}

function Get-WmsDevRestartPlan([string]$Repository, [int]$BackendPort, [int]$FrontendPort, [string]$SnapshotDirectory='') {
    Assert-WmsDevRestartPorts $BackendPort $FrontendPort
    $listeners = @(Get-WmsDevListeners)
    $plan = [Collections.Generic.List[object]]::new()
    try {
        foreach ($port in @($FrontendPort,$BackendPort)) {
            $owners = @($listeners | Where-Object { $_.LocalPort -eq $port } | Select-Object -ExpandProperty OwningProcess -Unique)
            if (-not $owners.Count) { continue }
            if ($owners.Count -ne 1) { throw 'DEV_RESTART_OWNER_NOT_CONFIRMED' }
            $process = Get-Process -Id $owners[0] -ErrorAction Stop
            $matched = $null
            try {
                # Fixar o handle nativo antes da validacao impede encerrar um PID reutilizado.
                $null = $process.Handle
                $directories=@()
                foreach($runs in @((Join-Path $Repository 'orchestracao/.runtime/frontend-integracao-dev-runs'),(Join-Path $Repository 'orchestracao/.runtime/be02-dev-fonte01/prumo/runs'))){
                    if(!(Test-Path -LiteralPath $runs -PathType Container)){continue}
                    if ((Get-Item -LiteralPath $runs).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'DEV_RESTART_OWNER_NOT_CONFIRMED' }
                    $directories+=@(Get-ChildItem -LiteralPath $runs -Directory -ErrorAction Stop)
                }
                foreach ($directory in @($directories|Sort-Object FullName)) {
                    if ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
                    $path = Join-Path $directory.FullName 'launcher.json'
                    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { continue }
                    if ((Get-Item -LiteralPath $path).Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
                    try {
                        $read=Read-WmsDevRestartReceipt $path
                        $candidate = [pscustomobject]@{Port=$port;Process=$process;Receipt=$read.body;PreviousRun=$directory.Name;ReceiptPath=$path;ReceiptSha256=$read.sha256}
                        Assert-WmsDevRestartTarget $candidate $Repository $listeners
                        $matched = $candidate
                        break
                    } catch { continue }
                }
                if ($null -eq $matched) { throw 'DEV_RESTART_OWNER_NOT_CONFIRMED' }
                if($SnapshotDirectory){New-WmsDevRestartSnapshot $matched $Repository $SnapshotDirectory}
                $plan.Add($matched)
            } finally { if ($null -eq $matched) { $process.Dispose() } }
        }
        return $plan.ToArray()
    } catch {
        foreach ($target in $plan) { $target.Process.Dispose() }
        throw
    }
}

function Stop-WmsDevRestartPlan($Plan, [string]$Repository, [scriptblock]$OnStopped) {
    # Conferir TODOS antes do primeiro encerramento, inclusive trocas de dono durante a guarda SQL.
    $listeners = @(Get-WmsDevListeners)
    foreach ($port in @(25580,25581)) {
        foreach ($listener in @($listeners | Where-Object { $_.LocalPort -eq $port })) {
            if (-not @($Plan | Where-Object { $_.Port -eq $port -and -not $_.Process.HasExited -and $_.Process.Id -eq $listener.OwningProcess }).Count) {
                throw 'DEV_RESTART_OWNER_CHANGED'
            }
        }
    }
    $ownStops=[Collections.Generic.List[object]]::new()
    foreach($target in $Plan){if($target.PSObject.Properties.Name -contains 'StopWitness'){$ownStops.Add($target.StopWitness)}}
    foreach ($target in $Plan) { Assert-WmsDevRestartTarget $target $Repository $listeners $ownStops.ToArray() }
    foreach ($target in $Plan) {
        Assert-WmsDevRestartTarget $target $Repository @(Get-WmsDevListeners) $ownStops.ToArray()
        if ($target.Process.HasExited) {
            & $OnStopped ([ordered]@{pid=$target.Process.Id;port=$target.Port;previousRun=$target.PreviousRun;stoppedUtc=[DateTime]::UtcNow.ToString('o');mechanism='OWN_PROCESS_ALREADY_EXITED'})
            continue
        }
        $targetStartUtc=$target.Process.StartTime.ToUniversalTime().ToString('o')
        $stopRequestedUtc=[DateTime]::UtcNow.ToString('o')
        try { $target.Process.Kill() }
        catch {
            # O launcher anterior pode limpar seu backend ao observar o frontend sair.
            if ($target.Process.HasExited) { continue }
            throw 'DEV_RESTART_STOP_FAILED'
        }
        $witness=@{process=$target.Process;port=$target.Port;startUtc=$targetStartUtc;stopRequestedUtc=$stopRequestedUtc;receiptPath=$target.ReceiptPath;receiptSha256=$target.ReceiptSha256}
        $ownStops.Add($witness)
        $target|Add-Member NoteProperty StopWitness $witness
        & $OnStopped ([ordered]@{pid=$target.Process.Id;port=$target.Port;previousRun=$target.PreviousRun;startUtc=$targetStartUtc;stoppedUtc=[DateTime]::UtcNow.ToString('o');mechanism='CURRENT_OWNERSHIP_GATE_KILL'})
        if (-not $target.Process.WaitForExit(5000)) { throw 'DEV_RESTART_STOP_TIMEOUT' }
    }
    foreach($target in $Plan){Assert-WmsDevRestartTarget $target $Repository @(Get-WmsDevListeners) $ownStops.ToArray()}
    if (@(Get-WmsDevListeners | Where-Object { $_.LocalPort -in @(25580,25581) }).Count) { throw 'DEV_RESTART_PORT_NOT_RELEASED' }
}
