# Leitor/coletor local desta evidencia: arquivos fixos, sem ambiente/rede/SQL/JVM.
$ErrorActionPreference='Stop'
try {
    $raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
    $prefixo='infra/evidencias/d19-preparacao-tecnica-final-2026-10-06'
    $destPrefixo=$prefixo+'-final'
    $complemento='database/evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar'
    $inicio=(Get-Date).ToString('o')
    $utf8=New-Object System.Text.UTF8Encoding($false)
    function Caminho([string]$relativo) {Join-Path $raiz $relativo}
    function Hash([string]$relativo) {
        $alvo=Caminho $relativo
        if(-not (Test-Path -LiteralPath $alvo -PathType Leaf)) {throw 'BLOQUEADO_ARQUIVO_AUSENTE'}
        (Get-FileHash -LiteralPath $alvo -Algorithm SHA256 -ErrorAction Stop).Hash
    }
    function LerJson([string]$relativo) {Get-Content -LiteralPath (Caminho $relativo) -Raw -Encoding UTF8 | ConvertFrom-Json}
    function Salvar([string]$relativo,$objeto) {
        $alvo=Caminho $relativo
        if(Test-Path -LiteralPath $alvo) {throw 'DESTINO_EXISTENTE'}
        [IO.File]::WriteAllText($alvo,($objeto|ConvertTo-Json -Depth 20),$utf8)
    }
    $destinos=@(($destPrefixo+'-depois.json'),($destPrefixo+'-checks-locais.json'),($destPrefixo+'-metadados.json'),($destPrefixo+'.sha256'))
    foreach($destino in $destinos) {if(Test-Path -LiteralPath (Caminho $destino)) {throw 'DESTINO_EXISTENTE'}}
    $antes=LerJson ($prefixo+'-antes.json')
    $antesComplemento=LerJson ($complemento+'-antes.json')
    $diag=LerJson ($prefixo+'-diagnostico-complemento-v9.json')
    $execTecnico=LerJson ($prefixo+'-execucao-complemento-v9.json')
    $schema=LerJson ($complemento+'-output.json')
    $execSchema=LerJson ($complemento+'-execucao.json')
    $contrato=LerJson 'infra/contratos/preparacao-final-local.json'
    $checks=New-Object System.Collections.Generic.List[object]
    function Conferir([string]$id,[bool]$ok) {$checks.Add([pscustomobject]@{id=$id;atendido=$ok})}
    $autorizados=@('database/README.md','database/migrations/README.md','database/permissoes-minimas.md','infra/README.md','infra/configuracao-externa.md','infra/recuperacao-e-ensaio.md','docs/33-preparacao-tecnica-local-backend.md','database/migrations/V9__contagem_carga_contingencia_e_encerramento.sql','database/contratos/v9-schema-doc31.json','database/verificar-schema-v9.ps1','database/procedimento-v9.md','database/contratos/v9-matriz-cobertura.md')
    $autorizadosComplemento=$autorizados+@('infra/contratos/preparacao-final-local.json','infra/preparacao-tecnica-final.md',($prefixo+'.md'))
    function CompararArquivos($lista,$permitidos) {
        @($lista|ForEach-Object {
            $existe=Test-Path -LiteralPath (Caminho $_.arquivo) -PathType Leaf
            $atual=$null;if($existe) {$atual=Hash $_.arquivo}
            [pscustomobject]@{arquivo=$_.arquivo;antes=$_.sha256;depois=$atual;preservado=($existe -and $atual -ceq $_.sha256);alteracaoAutorizada=($_.arquivo -cin $permitidos);ausente=(-not $existe)}
        })
    }
    $comparacao=CompararArquivos $antes.arquivos $autorizados
    $comparacaoComplemento=CompararArquivos $antesComplemento.arquivos $autorizadosComplemento
    Conferir 'baseline251.quantidade' (@($comparacao).Count -eq 251)
    Conferir 'baseline251.nenhum_ausente_ou_mudanca_fora_escopo' (@($comparacao|Where-Object {$_.ausente -or (-not $_.preservado -and -not $_.alteracaoAutorizada)}).Count -eq 0)
    Conferir 'baseline280.quantidade' (@($comparacaoComplemento).Count -eq 280)
    Conferir 'baseline280.nenhum_ausente_ou_mudanca_fora_escopo' (@($comparacaoComplemento|Where-Object {$_.ausente -or (-not $_.preservado -and -not $_.alteracaoAutorizada)}).Count -eq 0)
    $sqls=@($antes.baselineV1V9|ForEach-Object {[pscustomobject]@{arquivo=$_.arquivo;antes=$_.sha256;depois=(Hash $_.arquivo);preservado=((Hash $_.arquivo) -ceq $_.sha256)}})
    $v9='database/migrations/V9__contagem_carga_contingencia_e_encerramento.sql'
    Conferir 'V1V8.oito_hashes_intactos' (@($sqls|Where-Object {$_.arquivo -cne $v9 -and $_.preservado}).Count -eq 8)
    Conferir 'V9.baseline_anterior_copiada' ((Hash $contrato.historicoV9.copiaAnterior) -ceq $contrato.historicoV9.anterior)
    Conferir 'V9.atual_hash_autorizado' ((Hash $v9) -ceq $contrato.historicoV9.atual -and (Hash $v9) -ceq $schema.migrationSha256)
    Conferir 'V9.fonte_copiada_hash_exato' ((Hash $contrato.historicoV9.fonte) -ceq $contrato.historicoV9.fonteSha256 -and $schema.fonteSha256 -ceq $contrato.historicoV9.fonteSha256)
    $copias=@($antes.copias)+@($antesComplemento.copias)
    $documentosCopiados=Get-Content -LiteralPath (Caminho ($complemento+'-copias-documentos.json')) -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach($copia in $documentosCopiados) {$copias+=$copia}
    $copiasResultado=@($copias|ForEach-Object {[pscustomobject]@{origem=$_.origem;copia=$_.copia;esperado=$_.sha256;atual=(Hash $_.copia);igual=((Hash $_.copia) -ceq $_.sha256)}})
    Conferir 'copias23.hashes_anteriores_exatos' ($copiasResultado.Count -eq 23 -and @($copiasResultado|Where-Object {-not $_.igual}).Count -eq 0)
    Conferir 'original56.leitor_preservado' ((Hash $contrato.leitor56.arquivo) -ceq $contrato.leitor56.sha256)
    Conferir 'original56.output_preservado' ((Hash $contrato.leitor56.output) -ceq $contrato.leitor56.outputSha256)
    Conferir 'original56.releitura_aprovada' ($diag.original56.total -eq 56 -and $diag.original56.atendidos -eq 56)
    Conferir 'diagnostico194.output_integral_sem_falha' ($diag.total -eq 194 -and $diag.atendidos -eq 194 -and $diag.falhas -eq 0)
    Conferir 'schema424.output_integral_sem_divergencia' ($schema.totalChecks -eq 424 -and $schema.aprovados -eq 424 -and @($schema.divergencias).Count -eq 0)
    Conferir 'schema424.totais_formais' ($schema.totais.tabelas -eq 8 -and $schema.totais.colunas -eq 79 -and $schema.totais.fks -eq 18 -and $schema.totais.unicas -eq 8 -and $schema.totais.checksTabelas -eq 24 -and $schema.totais.indicesComuns -eq 10 -and $schema.totais.indicesUnicosFiltrados -eq 1 -and $schema.totais.checksCumulativosSubstituidos -eq 6)
    Conferir 'metadados.PowerShell_normal_LASTEXITCODE_null_sem_inventar_zero' ($execTecnico.powershellSuccess -and $execSchema.powershellSuccess -and $null -eq $execTecnico.lastExitCode -and $null -eq $execSchema.lastExitCode)
    Conferir 'documentos_seis_hashes_output_atual' (@($diag.documentos|Where-Object {(Hash $_.arquivo) -cne $_.sha256}).Count -eq 0 -and @($diag.documentos).Count -eq 6)
    Conferir 'fontes_tecnicas_estaveis_durante_leitura' (@($diag.fontesTecnicas|Where-Object {$_.antes -cne $_.depois}).Count -eq 0)
    $fontesDepois=@($antes.fontesTecnicas|ForEach-Object {[pscustomobject]@{arquivo=$_.arquivo;baseline=$_.sha256;depois=(Hash $_.arquivo);igualBaseline=((Hash $_.arquivo) -ceq $_.sha256)}})
    $asts=@()
    foreach($leitor in @('infra/verificar-preparacao-final-local.ps1','infra/verificar-preparacao-local.ps1','database/verificar-schema-v9.ps1')) {
        $tokens=$null;$erros=$null
        $ast=[Management.Automation.Language.Parser]::ParseFile((Caminho $leitor),[ref]$tokens,[ref]$erros)
        $envNos=@($ast.FindAll({param($n) $n -is [Management.Automation.Language.VariableExpressionAst] -and $n.VariablePath.UserPath -match '^env:'},$true))
        $comandos=@($ast.FindAll({param($n) $n -is [Management.Automation.Language.CommandAst]},$true)|ForEach-Object {$_.GetCommandName()}|Sort-Object -Unique)
        $asts+=[pscustomobject]@{arquivo=$leitor;erros=$erros.Count;referenciasVariavelEnv=$envNos.Count;comandos=$comandos;sha256=(Hash $leitor)}
        Conferir ($leitor+'.AST_sem_erro_sem_variavel_env') ($erros.Count -eq 0 -and $envNos.Count -eq 0)
    }
    $docs=@($autorizados|Where-Object {$_ -like '*.md'})+@('infra/preparacao-tecnica-final.md',($prefixo+'.md'),($complemento+'.md'))
    $links=@()
    foreach($doc in $docs) {
        $texto=Get-Content -LiteralPath (Caminho $doc) -Raw -Encoding UTF8
        foreach($link in [regex]::Matches($texto,'\[[^\]]*\]\(([^)]+)\)')) {
            $url=$link.Groups[1].Value.Trim('<','>')
            if($url -match '^(?:[a-zA-Z][a-zA-Z0-9+.-]*:|#)') {continue}
            $alvoRel=($url -split '#',2)[0];if(-not $alvoRel) {continue}
            $alvo=[IO.Path]::GetFullPath((Join-Path ([IO.Path]::GetDirectoryName((Caminho $doc))) $alvoRel))
            if(-not $alvo.StartsWith($raiz+'\',[StringComparison]::OrdinalIgnoreCase)) {throw 'LINK_FORA_RAIZ'}
            $relativo=$alvo.Substring($raiz.Length+1).Replace('\','/')
            $planejado=$relativo -cin $destinos
            $links+=[pscustomobject]@{documento=$doc;alvo=$relativo;existe=(Test-Path -LiteralPath $alvo);geradoNesteFecho=$planejado}
        }
    }
    Conferir 'links_locais_existentes_ou_destinos_deste_fecho' (@($links|Where-Object {-not $_.existe -and -not $_.geradoNesteFecho}).Count -eq 0)
    $falhas=@($checks|Where-Object {-not $_.atendido})
    $depois=[pscustomobject]@{capturadoEm=(Get-Date).ToString('o');comparacaoBaseline251=$comparacao;comparacaoAntesComplemento280=$comparacaoComplemento;V1V9=$sqls;V9AtualAutorizada=$contrato.historicoV9;copias23=$copiasResultado;fontesTecnicasDepois=$fontesDepois;limites='Preservacao de arquivos; backend em escrita, nenhuma baseline global/freeze/JPA/SQL ou artefato final inferida.'}
    Salvar ($destPrefixo+'-depois.json') $depois
    $qa=[pscustomobject]@{capturadoEm=(Get-Date).ToString('o');comando='./infra/evidencias/d19-preparacao-tecnica-final-2026-10-06-conferir-fecho.ps1';total=$checks.Count;aprovados=$checks.Count-$falhas.Count;falhas=$falhas;checks=$checks;ast=$asts;links=$links;linksPlanejados=@($links|Where-Object {$_.geradoNesteFecho}).Count;observacao='Destinos deste fecho sao gravados nesta invocacao e links reais reconferidos apos manifesto. Hashes e AST nao executam DDL/JPA.'}
    Salvar ($destPrefixo+'-checks-locais.json') $qa
    $meta=[pscustomobject]@{inicio=$inicio;capturadoEm=(Get-Date).ToString('o');natureza='Preparacao BE03/BE04/BE15 e complemento formal V9 somente arquivos';diagnostico194=$execTecnico;schema424=$execSchema;original56=$diag.original56;hashes=[pscustomobject]@{leitorTecnico=(Hash 'infra/verificar-preparacao-final-local.ps1');contratoTecnico=(Hash 'infra/contratos/preparacao-final-local.json');diagnostico194=(Hash ($prefixo+'-diagnostico-complemento-v9.json'));V9=(Hash $v9);leitorV9=(Hash 'database/verificar-schema-v9.ps1');transcricaoV9=(Hash 'database/contratos/v9-schema-doc31.json');output424=(Hash ($complemento+'-output.json'))};preservacao251=[pscustomobject]@{preservados=@($comparacao|Where-Object {$_.preservado}).Count;alteradosAutorizados=@($comparacao|Where-Object {-not $_.preservado -and $_.alteracaoAutorizada}).Count;ausentes=@($comparacao|Where-Object {$_.ausente}).Count};preservacao280=[pscustomobject]@{preservados=@($comparacaoComplemento|Where-Object {$_.preservado}).Count;alteradosAutorizados=@($comparacaoComplemento|Where-Object {-not $_.preservado -and $_.alteracaoAutorizada}).Count;ausentes=@($comparacaoComplemento|Where-Object {$_.ausente}).Count};fontesDiferentesBaseline=@($fontesDepois|Where-Object {-not $_.igualBaseline});semSqlJvmH2BuildRedeAmbienteSegredos=$true;artefatoFinal='AINDA_EM_CONSTRUCAO_NAO_CONFERIDO';JpaBE14='AGUARDA_FREEZE_TAREFA_SEPARADA';homologacaoExterna='NAO_EXECUTADA';manifesto=($prefixo+'.sha256');limiteLASTEXITCODE='Scripts concluiram normalmente com LASTEXITCODE null; nao declarar saida0 nativa.'}
    $meta.manifesto=$destPrefixo+'.sha256'
    Salvar ($destPrefixo+'-metadados.json') $meta
    $inventario=@(Get-ChildItem -LiteralPath (Caminho 'database'),(Caminho 'infra') -File -Recurse | ForEach-Object {$_.FullName})+@((Caminho 'docs/33-preparacao-tecnica-local-backend.md'))
    $linhasManifesto=@($inventario|Sort-Object|ForEach-Object {((Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash)+'  '+$_.Substring($raiz.Length+1).Replace('\','/')})
    [IO.File]::WriteAllText((Caminho ($destPrefixo+'.sha256')),($linhasManifesto -join "`n")+"`n",$utf8)
    $divManifesto=@($linhasManifesto|Where-Object {$m=[regex]::Match($_,'^([0-9A-F]{64})  (.+)$'); -not $m.Success -or (Hash $m.Groups[2].Value) -cne $m.Groups[1].Value})
    $linksAusentes=@($links|Where-Object {-not (Test-Path -LiteralPath (Caminho $_.alvo))})
    [pscustomobject]@{totalChecks=$checks.Count;aprovados=$checks.Count-$falhas.Count;falhas=$falhas;preservacao251=$meta.preservacao251;preservacao280=$meta.preservacao280;copias=$copiasResultado.Count;sqlV1V8Intactas=8;V9Atual=$contrato.historicoV9.atual;manifestoArquivos=$linhasManifesto.Count;manifestoSha256=(Hash ($destPrefixo+'.sha256'));manifestoDivergencias=$divManifesto.Count;links=$links.Count;linksAusentes=$linksAusentes.Count;fontesMudadasBaseline=@($fontesDepois|Where-Object {-not $_.igualBaseline}).Count} | ConvertTo-Json -Depth 8
    if($falhas.Count -or $divManifesto.Count -or $linksAusentes.Count) {exit 1}
} catch {
    [pscustomobject]@{bloqueio='BLOQUEADO_LEITURA_OU_FORMATO';rawExposto=$false} | ConvertTo-Json
    exit 1
}
