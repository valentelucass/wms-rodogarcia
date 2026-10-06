[CmdletBinding()]param()
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'd21-catalogo.ps1')
$casos=@(
    @{nome='NOT IN reescrito pelo SQL Server';a="acao NOT IN ('A','B')";b="NOT ([acao]='B' OR [acao]='A')";igual=$true}
    @{nome='IN de um elemento';a="situacao IN ('A')";b="[situacao]='A'";igual=$true}
    @{nome='Negacao aninhada e De Morgan';a='NOT (x=1 AND (y=2 OR y=3))';b='x<>1 OR (y<>2 AND y<>3)';igual=$true}
    @{nome='Dupla negacao';a='NOT NOT (x=1)';b='x=1';igual=$true}
    @{nome='IN nao equivale a NOT IN';a="acao IN ('A','B')";b="acao NOT IN ('A','B')";igual=$false}
    @{nome='Termo removido continua divergente';a="acao NOT IN ('A','B')";b="acao <> 'A'";igual=$false}
    @{nome='NULL continua distinto de valor';a='x NOT IN (1,NULL)';b='x<>1';igual=$false}
    @{nome='Literal sensivel a caixa preservado';a="tipo='A'";b="tipo='a'";igual=$false}
    @{nome='Operador alterado continua divergente';a='x>=1';b='x>1';igual=$false}
)
$resultados=@(foreach($c in $casos){
    $a=Get-WmsD21Expressao $c.a;$b=Get-WmsD21Expressao $c.b
    [pscustomobject]@{caso=$c.nome;aprovado=(($a -ceq $b) -eq $c.igual)}
})
$falhas=@($resultados|Where-Object {-not $_.aprovado}).Count
[pscustomobject]@{natureza='D24_CATALOGO_OFFLINE';total=$resultados.Count;aprovados=$resultados.Count-$falhas;falhas=$falhas;casos=$resultados;sqlExecutado=$false}|ConvertTo-Json -Depth 4
if($falhas){exit 1}
