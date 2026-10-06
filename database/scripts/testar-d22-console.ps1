[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'd22-console.ps1')
function Processo([int]$Id,[int]$Pai,[string]$Nome){[pscustomobject]@{ProcessId=$Id;ParentProcessId=$Pai;Name=$Nome}}
$ps=Processo 10 20 'powershell.exe';$forf=Processo 20 30 'cmd.exe';$bat=Processo 30 40 'cmd.exe'
$casos=@(
    [pscustomobject]@{caso='Explorer via FOR/F profundidade exata';esperado='EXPLORER';real=(Resolve-WmsD22Console $ps $forf $bat (Processo 40 50 'explorer.exe'))}
    [pscustomobject]@{caso='Caller via FOR/F sem console persistente';esperado='CALLER';real=(Resolve-WmsD22Console $ps $forf $bat (Processo 40 50 'powershell.exe'))}
    [pscustomobject]@{caso='Filho /k Origem cmd externo nao recursivo';esperado='CALLER';real=(Resolve-WmsD22Console $ps $forf $bat (Processo 40 50 'cmd.exe'))}
    [pscustomobject]@{caso='Metadados incompletos conservam caller';esperado='CALLER';real=(Resolve-WmsD22Console $ps $forf $bat $null)}
    [pscustomobject]@{caso='PIDs desconexos nao inferem Explorer';esperado='CALLER';real=(Resolve-WmsD22Console $ps $forf $bat (Processo 41 50 'explorer.exe'))}
)
foreach($c in $casos){$c|Add-Member -NotePropertyName aprovado -NotePropertyValue ($c.real -ceq $c.esperado)}
$falhas=@($casos|Where-Object {-not $_.aprovado}).Count
[pscustomobject]@{natureza='D22_FIXTURES_METADADOS_CONSOLE_SEM_SQL';total=$casos.Count;aprovados=$casos.Count-$falhas;falhas=$falhas;casos=$casos;sqlExecutado=$false;credencialRealLida=$false;explorerVisualExecutado=$false}|ConvertTo-Json -Depth 5
if($falhas){exit 1}
