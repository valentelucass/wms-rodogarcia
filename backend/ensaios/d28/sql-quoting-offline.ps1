$ErrorActionPreference='Stop'
$root=(Resolve-Path "$PSScriptRoot/../../..").Path
$source=Join-Path $root 'orchestracao/.runtime/d26-prumo-persistencia.ps1'
$tokens=$null;$errors=$null
$ast=[Management.Automation.Language.Parser]::ParseFile($source,[ref]$tokens,[ref]$errors)
$commands=@($ast.FindAll({param($n) $n -is [Management.Automation.Language.CommandAst] -and $n.GetCommandName() -ceq 'PersistenciaSelect'},$true))
$saldo=@($commands|Where-Object {$_.Extent.Text.Contains('SUM(u.quantidade)')})
$cases=New-Object 'Collections.Generic.List[object]'
function Check([string]$name,[bool]$pass){$cases.Add([ordered]@{caso=$name;passou=$pass})}
Check 'fonte historica parseada sem executar script/global/credenciais' ($errors.Count -eq 0 -and $saldo.Count -eq 1)
Check 'comando saldo possui comando e um argumento SQL literal inteiro' ($saldo[0].CommandElements.Count -eq 2 -and $saldo[0].CommandElements[1] -is [Management.Automation.Language.StringConstantExpressionAst])
$sql=$saldo[0].CommandElements[1].Value
Check 'apostrofos SQL preservados no literal completo' ($sql.Contains("IN(N'ARMAZENAGEM',N'SEPARACAO')") -and $sql.EndsWith('AND u.ativa=1'))
$script:captured=$null
function Query-Sql([string]$Sql){$script:captured=[ordered]@{sql=$Sql;extras=$args.Count}}
# Executa exclusivamente o comando AST, com mock local. Nenhuma funcao SQL da fonte e importada.
$literal=$saldo[0].CommandElements[1].Extent.Text
& ([scriptblock]::Create('Query-Sql '+$literal))
Check 'mock Query-Sql recebeu SQL inteiro sem argumentos adicionais' ($captured.sql -ceq $sql -and $captured.extras -eq 0)
$escaped="Query-Sql `"SELECT N'O''Brien', N'`"`"D28`"`"' AS texto WHERE id=@id`""
$t=$null;$e=$null;$a=[Management.Automation.Language.Parser]::ParseInput($escaped,[ref]$t,[ref]$e)
$c=@($a.FindAll({param($n)$n -is [Management.Automation.Language.CommandAst]},$true))[0]
& ([scriptblock]::Create($c.Extent.Text))
Check 'aspas duplas e apostrofos SQL em um argumento' ($e.Count -eq 0 -and $c.CommandElements.Count -eq 2 -and $captured.sql -ceq "SELECT N'O''Brien', N'`"D28`"' AS texto WHERE id=@id" -and $captured.extras -eq 0)
$bad="Query-Sql 'SELECT CASE WHEN local IN(N'ARMAZENAGEM',N'SEPARACAO') THEN 1 END'"
$t=$null;$e=$null;$a=[Management.Automation.Language.Parser]::ParseInput($bad,[ref]$t,[ref]$e)
$c=@($a.FindAll({param($n)$n -is [Management.Automation.Language.CommandAst]},$true))[0]
$intact=($e.Count -eq 0 -and $c.CommandElements.Count -eq 2 -and $c.CommandElements[1] -is [Management.Automation.Language.StringConstantExpressionAst] -and $c.CommandElements[1].Value.Contains("N'ARMAZENAGEM'"))
Check 'negativo quoting externo simples detectado como SQL corrompido' (-not $intact)
$result=[ordered]@{incremento='D28';historico='D28-VIG-R13';utc=[DateTime]::UtcNow.ToString('o');fonte=$source.Substring($root.Length+1);fonteSHA256=(Get-FileHash -LiteralPath $source).Hash;linha=$saldo[0].Extent.StartLineNumber;comandoHistorico='PersistenciaSelect';mock='Query-Sql';SQLExecutado=$false;credencialCarregada=$false;checks=$cases.Count;falhas=@($cases|Where-Object {-not $_.passou}).Count;casos=$cases.ToArray()}
[IO.File]::WriteAllText((Join-Path $root 'backend/evidencias/d28-sql-quoting-offline.json'),($result|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
[pscustomobject]$result|Select-Object checks,falhas,SQLExecutado,credencialCarregada|ConvertTo-Json
if($result.falhas){exit 1}
