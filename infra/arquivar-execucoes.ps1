# Inventaria execuções encerradas. -Aplicar transfere os diretórios no mesmo volume.
[CmdletBinding()]
param([switch]$Aplicar)
$ErrorActionPreference='Stop'
$repo=(Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$docs=[Environment]::GetFolderPath('MyDocuments')
$archive=[IO.Path]::GetFullPath((Join-Path $docs 'Armazenamento-Projetos/wms-rodogarcia/execucoes'))
$cutoff=[DateTime]::UtcNow.AddDays(-14)
$protected=[Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
$idPattern='[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}|[a-f0-9]{32}'
function Protect-Ids([string]$Text) {
    foreach($m in [regex]::Matches($Text,$idPattern,'IgnoreCase')){$null=$protected.Add($m.Value)}
}
function Assert-LocalDirectory([string]$Path,[string]$Parent) {
    $full=[IO.Path]::GetFullPath($Path)
    if(!$full.StartsWith($Parent.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase)){throw "Diretório fora do escopo: $full"}
    for($p=$full;$p -and $p.Length -ge $Parent.Length;$p=[IO.Path]::GetDirectoryName($p)){
        if([IO.Directory]::Exists($p) -and ((Get-Item -Force -LiteralPath $p).Attributes -band [IO.FileAttributes]::ReparsePoint)){throw "Vínculo inesperado: $p"}
    }
}
function Test-ClosedRun([IO.DirectoryInfo]$Run) {
    $pending=[Collections.Generic.Stack[IO.DirectoryInfo]]::new()
    $pending.Push($Run)
    while($pending.Count){
        foreach($item in $pending.Pop().EnumerateFileSystemInfos()){
            if($item.LastWriteTimeUtc -ge $cutoff){return $false}
            if($item.Attributes -band [IO.FileAttributes]::ReparsePoint){
                if($item.Name -ine 'node_modules' -or (Get-Item -LiteralPath $item.FullName).Target -ine (Join-Path $repo 'frontend/node_modules')){return $false}
            }elseif($item.Attributes -band [IO.FileAttributes]::Directory){$pending.Push($item)}
            elseif($item.Name -like '.env*' -or $item.Extension -in '.mdf','.ndf','.ldf','.bak','.bkp','.trn','.db','.sqlite','.sqlite3','.clixml','.pfx','.p12','.dpapi','.key','.keystore'){return $false}
        }
    }
    return $true
}
if($Aplicar -and ($repo -ine (Join-Path $docs 'projetos/wms-rodogarcia') -or [IO.Path]::GetPathRoot($repo) -ine [IO.Path]::GetPathRoot($archive))){throw 'Verifique o projeto e o volume de arquivo.'}
$processes=@(Get-CimInstance Win32_Process -ErrorAction Stop)
foreach($p in $processes){if($p.CommandLine -and $p.CommandLine.Contains($repo)){Protect-Ids $p.CommandLine}}
# Se uma porta do WMS tiver dono inacessível, não é possível excluir a execução da lista.
foreach($listener in @(Get-NetTCPConnection -State Listen -ErrorAction Stop|Where-Object LocalPort -in 25580,25581,25590,25591)){
    $owner=@($processes|Where-Object ProcessId -eq $listener.OwningProcess)
    if($owner.Count -ne 1 -or [string]::IsNullOrWhiteSpace($owner[0].CommandLine)){throw 'Dono de porta WMS não verificável.'}
}
$areas=[ordered]@{
    'dev'='.runtime/dev/runs'; 'prod'='.runtime/prod/runs'
    'dev-anterior'='orchestracao/.runtime/be02-dev-fonte01/prumo/runs'
    'prod-anterior'='orchestracao/.runtime/launcher-producao/runs'
}
foreach($base in @('.runtime/prod','orchestracao/.runtime/launcher-producao')){
    $pm2=Join-Path $repo ($base+'/pm2')
    if(!(Test-Path -LiteralPath $pm2)){continue}
    if(Get-ChildItem -LiteralPath $pm2 -Filter '*operation.lock'){throw 'Atualização do PM2 em andamento.'}
    foreach($name in @('selected.json','prepared.json','api-current.json','ui-current.json')){
        $file=Join-Path $pm2 $name
        if(Test-Path -LiteralPath $file){Protect-Ids ([IO.File]::ReadAllText($file))}
    }
    foreach($activation in @(Get-ChildItem -LiteralPath $pm2 -Filter 'activation-*.json'|Sort-Object LastWriteTimeUtc -Descending|Select-Object -First 2)){Protect-Ids $activation.Name}
}
# Manter acessíveis as execuções citadas pelo próprio repositório.
$references=git -C $repo grep -h -I -i -o -E $idPattern -- '*.ps1' '*.java' '*.cjs' '*.mjs' '*.ts' '*.py' '*.md'
if($LASTEXITCODE -gt 1){throw 'Falha na verificação das referências Git.'}
foreach($id in $references){$null=$protected.Add($id)}
foreach($kind in $areas.Keys){
    $source=Join-Path $repo $areas[$kind]
    if(!(Test-Path -LiteralPath $source)){continue}
    Assert-LocalDirectory $source $repo
    $runs=@(Get-ChildItem -Directory -Force -LiteralPath $source|Where-Object {$_.Name -match "^($idPattern)$" -and !($_.Attributes -band [IO.FileAttributes]::ReparsePoint)}|Sort-Object LastWriteTimeUtc -Descending)
    foreach($recent in @($runs|Select-Object -First 2)){$null=$protected.Add($recent.Name)}
    foreach($run in $runs){
        if($run.LastWriteTimeUtc -ge $cutoff -or $protected.Contains($run.Name)){continue}
        if(!(Test-ClosedRun $run)){continue}
        $dest=Join-Path (Join-Path $archive $kind) $run.Name
        Assert-LocalDirectory $run.FullName $source
        Assert-LocalDirectory $dest $docs
        if($Aplicar){
            if(Test-Path -LiteralPath $dest){throw "Destino já existente: $dest"}
            $null=[IO.Directory]::CreateDirectory((Split-Path $dest))
            Move-Item -LiteralPath $run.FullName -Destination $dest -ErrorAction Stop
        }
        [pscustomobject]@{ambiente=$kind;origem=$run.FullName;destino=$dest;arquivado=[bool]$Aplicar}
    }
}
