[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$KnownChildPids,
      [string]$PipeName='rpc.sock',
      [switch]$ReadOnly)
$ErrorActionPreference='Stop'
# A conexão identifica o daemon real; não usa PID encontrado por nome/data.
Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class WmsPm2PipeOwner {
  [DllImport("kernel32.dll", SetLastError=true)]
  public static extern bool GetNamedPipeServerProcessId(IntPtr handle, out uint serverProcessId);
}
'@
$pipe=[IO.Pipes.NamedPipeClientStream]::new('.',$PipeName,[IO.Pipes.PipeDirection]::InOut)
$phase='PIPE_CONNECT'
try {
    $pipe.Connect(3000)
    $phase='PIPE_OWNER'
    [uint32]$serverProcessId=0
    if(-not [WmsPm2PipeOwner]::GetNamedPipeServerProcessId($pipe.SafePipeHandle.DangerousGetHandle(),[ref]$serverProcessId)){throw 'PROD_PM2_MAIN_OWNER_UNCONFIRMED'}
    $phase='PROCESS_OWNER'
    $server=Get-CimInstance Win32_Process -Filter ('ProcessId='+$serverProcessId)
    # O daemon pode ser iniciado por serviço/wrapper: CommandLine não precisa conter Daemon.js.
    # Vincula o servidor real do pipe aos PIDs devolvidos pela consulta PM2 atual.
    $children=@($KnownChildPids -split ',' | ForEach-Object {
        if($_ -notmatch '^[1-9][0-9]*$'){throw 'PROD_PM2_MAIN_CHILD_INVALID'}
        Get-CimInstance Win32_Process -Filter ('ProcessId='+$_)
    })
    if($server.Name -ne 'node.exe' -or @($children | Where-Object ParentProcessId -eq $serverProcessId).Count -eq 0){throw 'PROD_PM2_MAIN_OWNER_UNCONFIRMED'}
    $phase='PID_METADATA'
    $pidPath=Join-Path $env:USERPROFILE '.pm2/pm2.pid'
    # Corrige somente o resíduo conhecido da consulta recusada desta entrega.
    if(-not $ReadOnly -and (Test-Path -LiteralPath $pidPath) -and ([IO.File]::ReadAllText($pidPath)).Trim() -eq '42760' -and
       -not (Get-Process -Id 42760 -ErrorAction SilentlyContinue)) {
        [IO.File]::WriteAllText($pidPath,[string]$serverProcessId)
    }
    Write-Output 'PROD_PM2_MAIN_OWNER_CONFIRMED'
} catch {
    $reason='PROD_PM2_MAIN_'+$phase+'_FAILED'
    if($_.Exception.Message -match '^PROD_[A-Z0-9_]+$'){$reason=$_.Exception.Message}
    Write-Output $reason
    exit 20
} finally { $pipe.Dispose() }
