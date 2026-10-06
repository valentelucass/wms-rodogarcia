# Apenas nomes/PIDs locais. FOR /F acrescenta um cmd entre PowerShell e o BAT.
$ErrorActionPreference='Stop'
function Resolve-WmsD22Console($Ps,$ForF,$Bat,$Origem){
    if($null -eq $Ps -or $null -eq $ForF -or $null -eq $Bat -or $null -eq $Origem){return 'CALLER'}
    if($Ps.Name -in @('powershell.exe','pwsh.exe') -and
       $Ps.ParentProcessId -eq $ForF.ProcessId -and $ForF.Name -ieq 'cmd.exe' -and
       $ForF.ParentProcessId -eq $Bat.ProcessId -and $Bat.Name -ieq 'cmd.exe' -and
       $Bat.ParentProcessId -eq $Origem.ProcessId -and $Origem.Name -ieq 'explorer.exe'){
        return 'EXPLORER'
    }
    # No filho /k, Origem e o cmd externo: nao relancar nem procurar Explorer acima.
    'CALLER'
}
if($MyInvocation.InvocationName -ne '.'){
    try{
        $ps=Get-CimInstance Win32_Process -Filter ('ProcessId='+$PID) -Property Name,ProcessId,ParentProcessId
        $forf=Get-CimInstance Win32_Process -Filter ('ProcessId='+$ps.ParentProcessId) -Property Name,ProcessId,ParentProcessId
        $bat=Get-CimInstance Win32_Process -Filter ('ProcessId='+$forf.ParentProcessId) -Property Name,ProcessId,ParentProcessId
        $origem=Get-CimInstance Win32_Process -Filter ('ProcessId='+$bat.ParentProcessId) -Property Name,ProcessId,ParentProcessId
        Resolve-WmsD22Console $ps $forf $bat $origem
    }catch{'CALLER'}
}
