[CmdletBinding()]
param()
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
$pipe=[IO.Pipes.NamedPipeClientStream]::new('.','rpc.sock',[IO.Pipes.PipeDirection]::InOut)
try {
    $pipe.Connect(3000)
    [uint32]$serverProcessId=0
    if(-not [WmsPm2PipeOwner]::GetNamedPipeServerProcessId($pipe.SafePipeHandle.DangerousGetHandle(),[ref]$serverProcessId)){throw 'PROD_PM2_MAIN_OWNER_UNCONFIRMED'}
    $server=Get-CimInstance Win32_Process -Filter ('ProcessId='+$serverProcessId)
    if($server.Name -ne 'node.exe' -or $server.CommandLine -notmatch 'pm2[\\/]lib[\\/]Daemon\.js'){throw 'PROD_PM2_MAIN_OWNER_UNCONFIRMED'}
    $pidPath=Join-Path $env:USERPROFILE '.pm2/pm2.pid'
    # Corrige somente o resíduo conhecido da consulta recusada desta entrega.
    if((Test-Path -LiteralPath $pidPath) -and ([IO.File]::ReadAllText($pidPath)).Trim() -eq '42760' -and
       -not (Get-Process -Id 42760 -ErrorAction SilentlyContinue)) {
        [IO.File]::WriteAllText($pidPath,[string]$serverProcessId)
    }
    Write-Output 'PROD_PM2_MAIN_OWNER_CONFIRMED'
} finally { $pipe.Dispose() }
