$ErrorActionPreference = 'Stop'
$raiz = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$backend = [IO.Path]::GetFullPath((Join-Path $raiz 'backend')) + [IO.Path]::DirectorySeparatorChar
$itens = @()
foreach ($linha in (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'd25-freeze.sha256') -Encoding UTF8)) {
    if ($linha -notmatch '^([A-Fa-f0-9]{64})  (backend/.+)$') { throw 'Linha de freeze inválida.' }
    $esperado = $Matches[1]
    $relativo = $Matches[2]
    $arquivo = [IO.Path]::GetFullPath((Join-Path $raiz $relativo))
    if (!$arquivo.StartsWith($backend,[StringComparison]::OrdinalIgnoreCase)) { throw 'Freeze fora do backend.' }
    $atual = $null
    if (Test-Path -LiteralPath $arquivo -PathType Leaf) { $atual = (Get-FileHash -LiteralPath $arquivo -Algorithm SHA256).Hash }
    $itens += [pscustomobject]@{arquivo=$relativo;igual=($esperado -eq $atual)}
}
$falhas = @($itens | Where-Object { !$_.igual })
[pscustomobject]@{natureza='D25_FREEZE_VERIFICADO_LOCAL';utc=[DateTime]::UtcNow.ToString('o');arquivos=$itens.Count;divergencias=$falhas.Count;sql=$false;itens=$itens} |
    ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'd25-freeze-validacao.json') -Encoding UTF8
Write-Output "D25_FREEZE: $($itens.Count) arquivos; $($falhas.Count) divergencias."
if ($itens.Count -eq 0 -or $falhas.Count -gt 0) { throw 'Freeze D25 divergente ou vazio.' }
