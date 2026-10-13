# V10 historica DEV permanece imutavel. PROD usa equivalente com nome/checksum proprios.
function Get-WmsProdFlywayLocation([string]$DatabaseRoot){
    $root=[IO.Path]::GetFullPath((Join-Path $DatabaseRoot '..'))
    $common=Join-Path $DatabaseRoot 'migrations'
    $prod=Join-Path $DatabaseRoot 'migrations-prod'
    $files=@(Get-ChildItem -LiteralPath $common -File | Where-Object {$_.Name -cmatch '^V[0-9]+__.+\.sql(\.conf)?$' -and $_.Name -cnotmatch '^V10__'})
    $files+=@(Get-Item -LiteralPath (Join-Path $prod 'V10__permitir_ajuste_estoque_producao.sql'))
    $files+=@(Get-Item -LiteralPath (Join-Path $prod 'V10__permitir_ajuste_estoque_producao.sql.conf'))
    $manifest=@($files|Sort-Object Name|ForEach-Object {@{name=$_.Name;sha256=(Get-FileHash -LiteralPath $_.FullName).Hash}})
    $sha=[Security.Cryptography.SHA256]::Create()
    try{$id=([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(($manifest|ConvertTo-Json -Compress))))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
    $target=Join-Path $root ('orchestracao/.runtime/migrations-prod/'+$id)
    $walk=$target
    while($walk){if((Test-Path -LiteralPath $walk) -and ((Get-Item -LiteralPath $walk).Attributes -band [IO.FileAttributes]::ReparsePoint)){throw 'D21_PROD_SOURCE_REPARSE'};$parent=[IO.Directory]::GetParent($walk);if(-not $parent){break};$walk=$parent.FullName}
    if(-not (Test-Path -LiteralPath $target)){
        $null=New-Item -ItemType Directory -Path $target
        foreach($f in $files){Copy-Item -LiteralPath $f.FullName -Destination $target}
    }
    if(@(Get-ChildItem -LiteralPath $target -Force).Count -ne $manifest.Count){throw 'D21_PROD_SOURCE_COUNT'}
    foreach($m in $manifest){if((Get-FileHash -LiteralPath (Join-Path $target $m.name)).Hash -cne $m.sha256){throw 'D21_PROD_SOURCE_CHANGED'}}
    'filesystem:'+($target.Replace('\','/'))
}
