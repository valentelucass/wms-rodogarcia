param([int]$Codigo=0,[switch]$Lento)
if($Lento){Start-Sleep -Seconds 5}
Write-Output $env:WMS_DB_BOOTSTRAP_PASSWORD
[Console]::Error.WriteLine('checksum '+$env:WMS_DB_BOOTSTRAP_PASSWORD)
exit $Codigo