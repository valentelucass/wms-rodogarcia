# Executar somente apos tarefa/freeze BE14 explicitamente comunicados por Farol.
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$ManifestoFreeze)
$WmsD20Pathverificarv9freezesuplementar=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

$ErrorActionPreference='Stop'
try {
    $raiz=[IO.Path]::GetFullPath((Join-Path $WmsD20Pathverificarv9freezesuplementar '..'))
    . (Join-Path $WmsD20Pathverificarv9freezesuplementar 'scripts/leitores/v9-java-arquivos.ps1')
    . (Join-Path $WmsD20Pathverificarv9freezesuplementar 'scripts/leitores/v9-comparar-jpa.ps1')
    $freeze=V9Freeze $raiz $ManifestoFreeze
    $c=Get-Content -LiteralPath (Join-Path $raiz 'database/contratos/v9-schema-doc31.json') -Raw -Encoding UTF8 -ErrorAction Stop|ConvertFrom-Json
    if($c.jpa.fase -cmatch 'HISTORICO|AGUARDA_NOVA_TAREFA') {throw 'BLOQUEADO_FREEZE_HISTORICO_P2_LOCKS'}
    if($freeze.sha256 -cne $c.jpa.manifestoFinal.sha256) {throw 'BLOQUEADO_MANIFESTO_DIFERENTE_DO_CONTRATO'}
    $regras=Get-Content -LiteralPath (Join-Path $raiz 'database/contratos/v9-guardas-leitura.json') -Raw -Encoding UTF8 -ErrorAction Stop|ConvertFrom-Json
    $checks=[ordered]@{};$evidencias=@();$mutabilidade=@()
    $fonte=V9LerCongelado $raiz $freeze $c.fonte
    $checks['fonte.hash_contrato_atual']=((Get-FileHash -LiteralPath (Join-Path $raiz $c.fonte) -Algorithm SHA256 -ErrorAction Stop).Hash -ceq $c.fonteSha256)
    $checks['V9.hash_intacto']=((Get-FileHash -LiteralPath (Join-Path $raiz $c.migration) -Algorithm SHA256 -ErrorAction Stop).Hash -ceq $c.jpa.V9Sha256)
    foreach($t in $c.tabelas) {
        $m=V9ModelCongelado $raiz $freeze $t.model
        $checks[($t.nome+'.parser_sem_lacuna')]=(@($m.problemas).Count -eq 0)
        foreach($col in $t.colunas) {
            $f=@($m.campos|Where-Object {$_.nome -ceq $col.nome})
            $ok=$f.Count -eq 1 -and $f[0].mutavel -eq $col.mutavel
            $checks[($t.nome+'.'+$col.nome+'.mutabilidade')]=[bool]$ok
            $mutabilidade+=[pscustomobject]@{tabela=$t.nome;coluna=$col.nome;esperadoMutavel=$col.mutavel;observadoMutavel=if($f.Count -eq 1){$f[0].mutavel}else{$null};campoJava=@($f.campoJava);limite='Anotacao/ID, nao prova de UPDATE emitido, setter seguro ou efeito operacional.'}
        }
    }
    foreach($regra in $regras.regras) {
        $s=V9SemComentarios (V9LerCongelado $raiz $freeze $regra.arquivo)
        $metodos=@(V9Metodos $s|Where-Object {$_.nome -ceq $regra.metodo});$ok=$metodos.Count -eq 1;$ocorrencias=@()
        if($ok) {
            $corpo=$metodos[0].corpo
            foreach($padrao in @($regra.presentes)) {$m=[regex]::Match($corpo,$padrao);$ok=$ok -and $m.Success;if($m.Success){$ocorrencias+=[pscustomobject]@{padrao=$padrao;linha=([regex]::Matches($s.Substring(0,$metodos[0].aberto+1+$m.Index),'\n').Count+1)}}}
            foreach($padrao in @($regra.ausentes)) {$ok=$ok -and -not [regex]::IsMatch($corpo,$padrao)}
            foreach($ordem in @($regra.ordens)) {$a=[regex]::Match($corpo,$ordem.antes);$b=[regex]::Match($corpo,$ordem.depois);$ok=$ok -and $a.Success -and $b.Success -and $a.Index -lt $b.Index}
        }
        $checks[$regra.id]=[bool]$ok
        $evidencias+=[pscustomobject]@{id=$regra.id;arquivo=$regra.arquivo;metodo=$regra.metodo;metodoUnico=($metodos.Count -eq 1);ocorrencias=$ocorrencias;atendidoLexicalmente=[bool]$ok;limite='Presenca/ausencia/ordem textual dentro do metodo; nao prova ramo, chamada, transacao ou guarda completa.'}
    }
    foreach($f in $freeze.fontes){$checks[('hash_depois.'+$f.arquivo)]=((Get-FileHash -LiteralPath (Join-Path $raiz $f.arquivo) -Algorithm SHA256 -ErrorAction Stop).Hash -ceq $f.antes)}
    $checks['manifesto.hash_estavel']=((Get-FileHash -LiteralPath (Join-Path $raiz $freeze.arquivo) -Algorithm SHA256 -ErrorAction Stop).Hash -ceq $freeze.sha256)
    $falhas=@($checks.Keys|Where-Object {-not $checks[$_]})
    [pscustomobject]@{capturadoEm=(Get-Date).ToString('o');natureza='Suplemento de leitura lexical/imutabilidade, nao aceite de regras BE14';total=$checks.Count;aprovados=$checks.Count-$falhas.Count;divergencias=$falhas;checks=$checks;mutabilidade=$mutabilidade;evidencias=$evidencias;freeze=[pscustomobject]@{arquivo=$freeze.arquivo;sha256=$freeze.sha256;fontes=$freeze.fontes.ToArray()};regrasSha256=(Get-FileHash -LiteralPath (Join-Path $raiz 'database/contratos/v9-guardas-leitura.json') -Algorithm SHA256).Hash;semSql=$true;semJvm=$true;semH2=$true;semBuild=$true;semAmbienteSegredosRede=$true;limites=@($regras.limites)}|ConvertTo-Json -Depth 16
    if($falhas.Count){exit 1}
} catch {
    $codigo='BLOQUEADO_LEITURA_PARSER_OU_FREEZE'
    if($_.Exception.Message -match '^(?:BLOQUEADO|PARSER)_[A-Z0-9_]+$'){$codigo=$_.Exception.Message}
    [pscustomobject]@{bloqueio=$codigo;rawExposto=$false;suplemento='NAO_CONCLUIDO';semSql=$true;semJvm=$true}|ConvertTo-Json
    exit 1
}
