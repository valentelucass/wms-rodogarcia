# Somente AST/JSON/regex/links/hash. Nao invoca leitor JPA/suplemento/backend.
$ErrorActionPreference='Stop'
try {
    $raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
    $p='database/evidencias/d19-v9-leitor-jpa-preparo-2026-10-06'
    $utf8=New-Object System.Text.UTF8Encoding($false)
    function PathP([string]$rel){Join-Path $raiz $rel}
    function HashP([string]$rel){if(-not (Test-Path -LiteralPath (PathP $rel) -PathType Leaf)){throw 'BLOQUEADO_ARQUIVO_AUSENTE'};(Get-FileHash -LiteralPath (PathP $rel) -Algorithm SHA256 -ErrorAction Stop).Hash}
    function JsonP([string]$rel){Get-Content -LiteralPath (PathP $rel) -Raw -Encoding UTF8 -ErrorAction Stop|ConvertFrom-Json}
    function SaveP([string]$rel,$obj){if(Test-Path -LiteralPath (PathP $rel)){throw 'DESTINO_EXISTENTE'};[IO.File]::WriteAllText((PathP $rel),($obj|ConvertTo-Json -Depth 20),$utf8)}
    $destinos=@(($p+'-depois.json'),($p+'-checks-locais.json'),($p+'.sha256'))
    foreach($d in $destinos){if(Test-Path -LiteralPath (PathP $d)){throw 'DESTINO_EXISTENTE'}}
    $checks=[ordered]@{}
    $a=JsonP ($p+'-antes.json');$c=JsonP 'database/contratos/v9-schema-doc31.json';$g=JsonP 'database/contratos/v9-guardas-leitura.json'
    $permitidos=@('database/verificar-schema-v9.ps1','database/contratos/v9-schema-doc31.json','database/procedimento-v9.md','database/contratos/v9-matriz-cobertura.md')
    $comparacao=@($a.arquivos307|ForEach-Object {$h=HashP $_.arquivo;[pscustomobject]@{arquivo=$_.arquivo;antes=$_.sha256;depois=$h;preservado=($h -ceq $_.sha256);alteracaoAutorizada=($_.arquivo -cin $permitidos)}})
    $checks['baseline307.inicial_sem_divergencia']=(@($a.arquivos307).Count -eq 307 -and @($a.arquivos307|Where-Object {$_.sha256 -cne $_.atual}).Count -eq 0)
    $checks['baseline307.somente_quatro_arquivos_autorizados']=(@($comparacao|Where-Object {-not $_.preservado -and -not $_.alteracaoAutorizada}).Count -eq 0 -and @($comparacao|Where-Object {-not $_.preservado}).Count -eq 4)
    $checks['manifesto307.byte_intacto']=((HashP $a.manifesto307) -ceq $a.manifestoSha256)
    $copias=@($a.copias|ForEach-Object {[pscustomobject]@{origem=$_.origem;copia=$_.copia;antes=$_.sha256;atual=(HashP $_.copia);igual=((HashP $_.copia) -ceq $_.sha256)}})
    $checks['copias4.byte_exatas']=($copias.Count -eq 4 -and @($copias|Where-Object {-not $_.igual}).Count -eq 0)
    $sqls=@($a.arquivos307|Where-Object {$_.arquivo -match '^database/migrations/V[1-9]__.+\.sql$'})
    $checks['V1V9.nove_sqls_intactos']=($sqls.Count -eq 9 -and @($sqls|Where-Object {(HashP $_.arquivo) -cne $_.sha256}).Count -eq 0)
    $checks['V9.hash_exato_17C6C2']=((HashP $c.migration) -ceq '17C6C2F9FB5B362242CF54AB2CD615B5646011EB7AB81D052C8FCE6833DF88B8' -and $c.jpa.V9Sha256 -ceq $a.V9Sha256)
    $antigo=JsonP ($p+'-antes-contratos-v9-schema-doc31.json')
    $checks['contrato.fonte_baselines_auditoria_sem_mudanca']=($c.fonteSha256 -ceq $antigo.fonteSha256 -and ($c.baselineV1V8|ConvertTo-Json -Depth 12) -ceq ($antigo.baselineV1V8|ConvertTo-Json -Depth 12) -and ($c.auditoria|ConvertTo-Json -Depth 15) -ceq ($antigo.auditoria|ConvertTo-Json -Depth 15))
    $cols=0
    foreach($t in $c.tabelas){$prev=@($antigo.tabelas|Where-Object {$_.nome -ceq $t.nome});$checks[($t.nome+'.colunas_checks_invariantes')]=($prev.Count -eq 1 -and ($t.colunas|ConvertTo-Json -Depth 12) -ceq ($prev[0].colunas|ConvertTo-Json -Depth 12) -and ($t.checks|ConvertTo-Json -Depth 12) -ceq ($prev[0].checks|ConvertTo-Json -Depth 12));$checks[($t.nome+'.unicas_so_nomeJpa_adicional')]=(($t.unicas|Select-Object nome,colunas|ConvertTo-Json -Depth 12) -ceq ($prev[0].unicas|Select-Object nome,colunas|ConvertTo-Json -Depth 12));$cols+=$t.colunas.Count}
    $checks['contrato.oito_models79_colunas_semDDL_extra']=($c.tabelas.Count -eq 8 -and $cols -eq 79 -and @($c.tabelas|Where-Object {-not $_.model}).Count -eq 0)
    $checks['contrato.sete_cadastros_legados_preparados']=($c.jpa.cadastros.Count -eq 7 -and @($c.jpa.cadastros|Where-Object {$_.tabela -eq 'servico_cobranca'}).Count -eq 1)
    $checks['contrato.guardas17_e_IDs_unicos']=($g.regras.Count -eq 17 -and @($g.regras.id|Group-Object|Where-Object {$_.Count -gt 1}).Count -eq 0)
    $regex=0;foreach($r in $g.regras){foreach($exp in @($r.presentes)+@($r.ausentes)+@($r.ordens.antes)+@($r.ordens.depois)){if($exp){$null=New-Object Text.RegularExpressions.Regex($exp);$regex++}}}
    $checks['guardas.regex_somente_compiladas_nao_pareadas_backend']=($regex -eq 104)
    $scripts=@('database/verificar-schema-v9.ps1','database/verificar-v9-freeze-suplementar.ps1','database/verificar-parser-v9-fixtures.ps1','database/leitores/v9-java-arquivos.ps1','database/leitores/v9-comparar-jpa.ps1');$asts=@()
    foreach($s in $scripts){$tokens=$null;$erros=$null;$ast=[Management.Automation.Language.Parser]::ParseFile((PathP $s),[ref]$tokens,[ref]$erros);$env=@($ast.FindAll({param($n)$n -is [Management.Automation.Language.VariableExpressionAst] -and $n.VariablePath.UserPath -match '^env:'},$true));$comandos=@($ast.FindAll({param($n)$n -is [Management.Automation.Language.CommandAst]},$true)|ForEach-Object {$_.GetCommandName()}|Sort-Object -Unique);$externos=@($comandos|Where-Object {$_ -match '^(?:java|mvnw?|flyway|sqlcmd|Invoke-WebRequest|Invoke-RestMethod|Invoke-Expression|Start-Process)$'});$checks[($s+'.AST_sem_erro_sem_env_comando_externo')]=($erros.Count -eq 0 -and $env.Count -eq 0 -and $externos.Count -eq 0);$asts+=[pscustomobject]@{arquivo=$s;sha256=(HashP $s);erros=$erros.Count;referenciasEnv=$env.Count;comandos=$comandos};if($s -eq 'database/verificar-schema-v9.ps1'){$params=@($ast.ParamBlock.Parameters.Name.VariablePath.UserPath);$checks['interface.CompararJpa_ManifestoFreeze_FonteDoc31']=('CompararJpa' -in $params -and 'ManifestoFreeze' -in $params -and 'FonteDoc31' -in $params)}}
    $fixture=JsonP ($p+'-fixtures-fecho.json');$exec=JsonP ($p+'-fixtures-fecho-execucao.json')
    $checks['fixtures31.aprovadas_sem_backendJpa']=($fixture.total -eq 31 -and $fixture.aprovados -eq 31 -and -not $fixture.backendLido -and -not $fixture.comparacaoJpaExecutada)
    $checks['fixtures31.PowerShell_normal_LASTEXITCODE_null']=($exec.powershellSuccess -and $null -eq $exec.lastExitCode -and -not $exec.comparacaoJpaBackendExecutada)
    $fontes=@();$listaFontes=@($c.fonte)+@($c.tabelas|ForEach-Object {'backend/src/main/java/br/com/rodogarcia/wms/models/'+$_.model+'.java'})+@('backend/src/main/java/br/com/rodogarcia/wms/models/CadastroBase.java')+@($c.jpa.servicesAuditoria)+@($c.jpa.enumsRepasse.arquivo)
    foreach($f in $listaFontes|Sort-Object -Unique){$fontes+=[pscustomobject]@{arquivo=$f;sha256=(HashP $f);natureza='Hash observado para inventario de construcao; nao freeze/pareamento/compatibilidade'} }
    $docs=@('database/procedimento-v9.md','database/contratos/v9-matriz-cobertura.md','database/contratos/v9-leitor-jpa.md',($p+'.md'));$links=@()
    foreach($doc in $docs){$texto=Get-Content -LiteralPath (PathP $doc) -Raw -Encoding UTF8 -ErrorAction Stop;foreach($m in [regex]::Matches($texto,'\[[^\]]*\]\(([^)]+)\)')){$url=$m.Groups[1].Value.Trim('<','>');if($url -match '^[a-zA-Z][a-zA-Z0-9+.-]*:|^#'){continue};$alvo=[IO.Path]::GetFullPath((Join-Path ([IO.Path]::GetDirectoryName((PathP $doc))) (($url -split '#',2)[0])));if(-not $alvo.StartsWith($raiz+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'BLOQUEADO_LINK_FORA_RAIZ'};$rel=$alvo.Substring($raiz.Length+1).Replace('\','/');$links+=[pscustomobject]@{documento=$doc;alvo=$rel;existe=(Test-Path -LiteralPath $alvo);geradoNesteFecho=($rel -cin $destinos)}}}
    $checks['links.locais_existentes_ou_gerados_no_fecho']=(@($links|Where-Object {-not $_.existe -and -not $_.geradoNesteFecho}).Count -eq 0)
    $falhas=@($checks.Keys|Where-Object {-not $checks[$_]})
    $depois=[pscustomobject]@{capturadoEm=(Get-Date).ToString('o');revisao='Preparacao posterior ao307; comparacao final/JPA/suplemento NAO executados';baseline307=$comparacao;copias4=$copias;fontesInventariadasParaConstrucao=$fontes;V9Sha256=(HashP $c.migration);execucaoFixture31=$exec;leitoresAst=$asts;manifesto307Sha256=(HashP $a.manifesto307);semSqlJvmH2BuildRedeAmbienteSegredos=$true}
    SaveP ($p+'-depois.json') $depois
    SaveP ($p+'-checks-locais.json') ([pscustomobject]@{capturadoEm=(Get-Date).ToString('o');comando='./database/evidencias/d19-v9-leitor-jpa-preparo-2026-10-06-conferir-preparo.ps1';total=$checks.Count;aprovados=$checks.Count-$falhas.Count;divergencias=$falhas;checks=$checks;regexCompiladas=$regex;links=$links;comparacaoJpaExecutada=$false;suplementoExecutado=$false;backendExecutado=$false;semSqlJvmH2BuildRedeAmbienteSegredos=$true})
    $novos=@(Get-ChildItem -LiteralPath (PathP 'database/evidencias') -File|Where-Object {$_.Name -like 'd19-v9-leitor-jpa-preparo-2026-10-06*'}|ForEach-Object {$_.FullName.Substring($raiz.Length+1).Replace('\','/')})+@('database/leitores/v9-java-arquivos.ps1','database/leitores/v9-comparar-jpa.ps1','database/verificar-v9-freeze-suplementar.ps1','database/verificar-parser-v9-fixtures.ps1','database/contratos/v9-guardas-leitura.json','database/contratos/v9-leitor-jpa.md')
    $alvos=@(@($a.arquivos307.arquivo)+@($a.manifesto307)+$novos|Sort-Object -Unique)
    $mf=@($alvos|ForEach-Object {(HashP $_)+'  '+$_})
    [IO.File]::WriteAllText((PathP ($p+'.sha256')),($mf -join "`n")+"`n",$utf8)
    $div=@($mf|Where-Object {$m=[regex]::Match($_,'^([0-9A-F]{64})  (.+)$');(HashP $m.Groups[2].Value) -cne $m.Groups[1].Value})
    $faltam=@($links|Where-Object {-not (Test-Path -LiteralPath (PathP $_.alvo))})
    [pscustomobject]@{checks=$checks.Count;aprovados=$checks.Count-$falhas.Count;divergencias=$falhas;baseline307Preservados=@($comparacao|Where-Object {$_.preservado}).Count;alteradosAutorizados=@($comparacao|Where-Object {-not $_.preservado}).Count;manifestoArquivos=$mf.Count;manifestoSha256=(HashP ($p+'.sha256'));manifestoDivergencias=$div.Count;links=$links.Count;linksAusentes=$faltam.Count;V9Sha256=(HashP $c.migration);jpaExecutado=$false;suplementoExecutado=$false}|ConvertTo-Json -Depth 6
    if($falhas.Count -or $div.Count -or $faltam.Count){exit 1}
} catch {
    [pscustomobject]@{bloqueio='BLOQUEADO_LEITURA_OU_FORMATO';rawExposto=$false;jpaExecutado=$false;suplementoExecutado=$false}|ConvertTo-Json
    exit 1
}
