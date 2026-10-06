# Apenas nomes/PIDs locais: decide manter console no duplo clique, sem ler CommandLine.
$ErrorActionPreference='Stop'
try{
    $ps=Get-CimInstance Win32_Process -Filter ('ProcessId='+$PID)
    $cmd=Get-CimInstance Win32_Process -Filter ('ProcessId='+$ps.ParentProcessId)
    $pai=Get-CimInstance Win32_Process -Filter ('ProcessId='+$cmd.ParentProcessId)
    if($cmd.Name -ieq 'cmd.exe' -and $pai.Name -ieq 'explorer.exe'){'EXPLORER'}else{'CALLER'}
}catch{'CALLER'}
