# Diagnostico final de PREPARACAO: somente arquivos conhecidos. Nao resolve env ou URL.
[CmdletBinding()]
param([ValidatePattern('^target(?:-[A-Za-z0-9-]+)?$')][string]$DiretorioArtefato)
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$utf8=New-Object Text.UTF8Encoding($false,$true)
$checks=[ordered]@{}
$hashes=@()
function Ler([string]$p) {[IO.File]::ReadAllText((Join-Path $script:raiz $p),$script:utf8)}
function Check([string]$id,[bool]$ok) {$script:checks[$id]=$ok}
function Hash([string]$p) {(Get-FileHash -LiteralPath (Join-Path $script:raiz $p) -Algorithm SHA256).Hash}
try {
    $c=(Ler 'infra/contratos/preparacao-final-local.json')|ConvertFrom-Json
    # Conferir atual e historico antes de ler codigo corrente/original56.
    # Os contratos-origem copiados sao ancoras distintas; nunca referencias rasas.
    . (Join-Path $raiz 'infra/leitores/identidade-ciclos-preparacao.ps1')
    $identidades=ConferirIdentidadesWmsCiclos -raiz $raiz -c $c
    foreach($id in $identidades.checks.Keys){Check ('identidade.'+$id) ([bool]$identidades.checks[$id])}
    if($identidades.divergencias.Count){throw 'BLOQUEADO_IDENTIDADES_CICLOS_DIVERGENTES'}
    if($DiretorioArtefato -and $c.freezeFinal.estado -cne 'ATUAL_AUTORIZADO') {throw 'FREEZE_HISTORICO_RECONFERENCIA_PENDENTE'}
    if($DiretorioArtefato) {
        . (Join-Path $raiz 'database/scripts/leitores/v9-java-arquivos.ps1')
        $gateFreeze=V9Freeze $raiz $c.freezeFinal.arquivo
        if($gateFreeze.sha256 -cne $c.freezeFinal.sha256 -or (Hash 'docs/31-fechamento-contagem-e-contingencia.md') -cne $c.freezeFinal.doc31Sha256){throw 'BLOQUEADO_FREEZE_TECNICO_DIVERGENTE'}
        foreach($s in $c.fontesTecnicas){if($s.arquivo.StartsWith('backend/') -and (-not $gateFreeze.mapa.ContainsKey($s.arquivo) -or (Hash $s.arquivo) -cne $gateFreeze.mapa[$s.arquivo])){throw 'BLOQUEADO_FONTE_TECNICA_FORA_FREEZE'}}
    }
    $original=Join-Path $raiz $c.leitor56.arquivo
    Check 'original56.leitor_preservado' ((Hash $c.leitor56.arquivo) -ceq $c.leitor56.sha256)
    Check 'original56.output_preservado' ((Hash $c.leitor56.output) -ceq $c.leitor56.outputSha256)
    if (-not $checks['original56.leitor_preservado']) {throw 'LEITOR_ORIGINAL_ALTERADO'}
    # Original56 usa parser XML local: bloquear DTD/entidades antes de executa-lo.
    $pom=Ler 'backend/pom.xml'
    if ($pom -match '<!DOCTYPE|<!ENTITY') {throw 'XML_EXTERNO_NAO_PERMITIDO'}
    $originalSaida=& $original
    $originalSucesso=$?;$originalCodigo=$LASTEXITCODE
    $base=($originalSaida -join "`n")|ConvertFrom-Json
    if ($base.formato -cne 'wms-diagnostico-estatico-v1' -or $base.total -ne 56) {throw 'LEITURA_ORIGINAL_BLOQUEADA'}
    foreach ($v in $base.conferencias) {Check ('original56.'+$v.id) ([bool]$v.atendido)}
    Check 'original56.normal_sem_falhas' ($originalSucesso -and $base.falhas -eq 0)
    $migrations=@(Get-ChildItem -LiteralPath (Join-Path $raiz 'database/migrations') -Filter 'V*.sql' -File)
    Check 'migrations.exatamente_V1_V9' ($migrations.Count -eq 9 -and @($c.baselineV1V9).Count -eq 9)
    $inventario=@();$todas=@();$numero=1
    foreach ($m in $c.baselineV1V9) {
        $h=Hash $m.arquivo;$sql=Ler $m.arquivo;$limpo=$sql -replace '(?m)--.*$',''
        Check ('migration.hash.'+$numero) ($h -ceq $m.sha256)
        Check ('migration.nome_ordem.'+$numero) ([IO.Path]::GetFileName($m.arquivo).StartsWith('V'+$numero+'__'))
        Check ('migration.alvo_placeholder.'+$numero) ($limpo.Contains("IF DB_NAME() <> N'"+'${wmsDatabase}'+"'"))
        $dml=@([regex]::Matches($limpo,'(?im)^\s*(INSERT|UPDATE|DELETE|MERGE)\b[^;]*;')|ForEach-Object {($_.Value -replace '\s','').ToUpperInvariant()})
        $esperado=@($c.dmlTecnicoExistente.($m.arquivo)|Where-Object {$_}|ForEach-Object {($_ -replace '\s','').ToUpperInvariant()})
        $dmlIgual=if ($esperado.Count -eq 0) {$dml.Count -eq 0} else {$dml.Count -eq $esperado.Count -and @(Compare-Object $esperado $dml).Count -eq 0}
        Check ('migration.DML_preexistente_inventariado.'+$numero) $dmlIgual
        $tab=@([regex]::Matches($limpo,'CREATE TABLE wms\.(\w+)')|ForEach-Object {$_.Groups[1].Value})
        $todas+=$tab;$inventario+=[pscustomobject]@{migration=$m.arquivo;sha256=$h;tabelas=$tab;quantidade=$tab.Count;dmlTecnicoDeclarado=@($c.dmlTecnicoExistente.($m.arquivo)|Where-Object {$_})}
        $numero++
    }
    Check 'inventario.64_tabelas_sem_duplicar' ($todas.Count -eq 64 -and @($todas|Group-Object|Where-Object {$_.Count -gt 1}).Count -eq 0)
    $permissoes=Ler 'database/docs/permissoes-minimas.md'
    foreach ($t in $todas) {Check ('permissoes.tabela_documentada.'+$t) ($permissoes -match ('\b'+[regex]::Escape($t)+'\b'))}
    Check 'permissoes.identidades_separadas_sem_grant_aplicado' ($permissoes -match 'Aplica' -and $permissoes -match 'Migration WMS' -and $permissoes -match 'DBA/recupera' -and $permissoes -match 'Nenhum usu')
    Check 'permissoes.menor_privilegio_sem_writer_owner' ($permissoes -match 'db_datawriter' -and $permissoes -match 'db_owner' -and $permissoes -match 'Sem DELETE de hist')
    $fontes=@()
    foreach ($s in $c.fontesTecnicas) {
        $antes=Hash $s.arquivo;[void](Ler $s.arquivo);$depois=Hash $s.arquivo
        Check ('leitura.estavel.'+$s.arquivo) ($antes -ceq $depois)
        $fontes+=[pscustomobject]@{arquivo=$s.arquivo;baseline=$s.sha256;antes=$antes;depois=$depois;igualBaseline=($antes -ceq $s.sha256)}
    }
    $claims=Ler 'backend/src/main/java/br/com/rodogarcia/wms/config/JwtWmsValidator.java'
    Check 'jwt.prazo_nominal_e_emissao_futura_guardados' ($claims.Contains('fim.isAfter(inicio)') -and $claims.Contains('Duration.ofMinutes(15)') -and $claims.Contains('Instant.now().plusSeconds(60)'))
    $seg=Ler 'backend/src/main/java/br/com/rodogarcia/wms/config/CadastrosSegurancaConfig.java'
    Check 'jwt.logout_local_desabilitado_sem_revogacao_presumida' ($seg.Contains('.logout(AbstractHttpConfigurer::disable)') -and $seg.Contains('SessionCreationPolicy.STATELESS'))
    Check 'jwt.erros_401_403_padronizados' ($seg.Contains('HttpStatus.UNAUTHORIZED') -and $seg.Contains('HttpStatus.FORBIDDEN') -and $seg.Contains('WWW_AUTHENTICATE'))
    $config=Ler 'infra/configuracao-externa.md'
    Check 'plano.contas_criacao_alteracao_desligamento' ($config -match 'cria' -and $config -match 'desligamento' -and $config -match 'recupera')
    Check 'plano.renovacao_revogacao_pendente_explicita' ($config -match 'renova' -and $config -match 'revoga' -and $config -match 'JWT j')
    Check 'plano.JWKS_rotacao_indisponibilidade' ($config -match 'chave desconhecida' -and $config -match 'indisponibilidade' -and $config -match 'rota')
    Check 'plano.segredos_externos_sem_coleta' ($config -match 'Nenhum valor real' -and $config -match 'mecanismo externo protegido' -and $config -match 'chave privada')
    $ensaio=Ler 'infra/recuperacao-e-ensaio.md'
    Check 'ensaio.RPO_RTO_nao_definidos' ($ensaio -match 'RPO/RTO' -and $ensaio -match 'N[^.\r\n]*inventar frequ')
    Check 'ensaio.restauracao_mais_que_VERIFYONLY' ($ensaio -match 'VERIFYONLY' -and $ensaio -match 'n[^.]+restaura')
    Check 'ensaio.sem_repair_clean_baseline_automaticos' ($ensaio -match 'repair' -and $ensaio -match 'baseline' -and $ensaio -match 'clean' -and $ensaio -match 'automatic')
    Check 'ensaio.donos_SQL_operacao_fiscal_equipamentos' ($ensaio -match 'Lucas/TI' -and $ensaio -match 'Caio' -and $ensaio -match 'Natalina' -and $ensaio -match 'Mickael')
    $wrapper=Ler 'database/scripts/migrate.ps1'
    Check 'wrapper.Info_Validate_conectam_nao_executado' ($wrapper.Contains("'Info', 'Validate', 'Migrate'") -and $wrapper.Contains('$connection.Open()') -and $wrapper.IndexOf('$connection.Open()') -lt $wrapper.IndexOf('& ./mvnw.cmd'))
    Check 'wrapper.erro_driver_sem_dump' ($wrapper.Contains('Nao expor a string de conexao') -and $wrapper.Contains('Nenhuma migracao foi solicitada'))
    foreach ($doc in $c.documentos) {Check ('documento.existe.'+$doc) (Test-Path -LiteralPath (Join-Path $raiz $doc) -PathType Leaf);$hashes+=[pscustomobject]@{arquivo=$doc;sha256=Hash $doc}}
    $pacote=Ler 'infra/preparacao-tecnica-final.md'
    Check 'pacote.local_artefato_final_e_externo_separados' ($pacote -match 'artefato final' -and $pacote -match 'homologa' -and $pacote -match 'freeze BE14')
    Check 'pacote.donos_sem_provedor_inventado' ($pacote -match 'DBA a identificar' -and $pacote -match 'Lucas' -and $pacote -match 'Gestor/comercial' -and $pacote -match 'provedor')
    $artefato=[pscustomobject]@{situacao='NAO_SOLICITADO_NESTA_EXECUCAO';executado=$false}
    $jpa='NAO_SOLICITADO_NESTA_EXECUCAO'
    if($DiretorioArtefato) {
        $a=$c.artefatoFinal
        Check 'final.diretorio_explicito_conforme_contrato' ($DiretorioArtefato -ceq $a.diretorio)
        if(-not $checks['final.diretorio_explicito_conforme_contrato']){throw 'DIRETORIO_ARTEFATO_NAO_CONTRATADO'}
        . (Join-Path $raiz 'database/scripts/leitores/v9-java-arquivos.ps1')
        $freeze=V9Freeze $raiz $c.freezeFinal.arquivo
        Check 'final.manifesto_hash_explicito' ($freeze.sha256 -ceq $c.freezeFinal.sha256)
        Check 'final.manifesto_289_fontes' ($freeze.mapa.Count -eq 289)
        foreach($s in $c.fontesTecnicas) {
            if($s.arquivo.StartsWith('backend/')){Check ('final.fonte_no_freeze.'+$s.arquivo) ($freeze.mapa.ContainsKey($s.arquivo) -and (Hash $s.arquivo) -ceq $freeze.mapa[$s.arquivo])}
        }
        Check 'final.fonte31_hash' ((Hash 'docs/31-fechamento-contagem-e-contingencia.md') -ceq $c.freezeFinal.doc31Sha256)
        $jar='backend/'+$DiretorioArtefato+'/'+$a.nome
        $jarPath=Join-Path $raiz $jar
        Check 'final.jar_presente' (Test-Path -LiteralPath $jarPath -PathType Leaf)
        $hashJar=Hash $jar;$bytes=(Get-Item -LiteralPath $jarPath -ErrorAction Stop).Length
        Check 'final.jar_bytes_exatos' ($bytes -eq $a.bytes)
        Check 'final.jar_hash_exato' ($hashJar -ceq $a.sha256)
        Check 'final.jar_hash_estavel' ((Hash $jar) -ceq $hashJar)
        $resumo=(Ler $a.resumo)|ConvertFrom-Json;$log=Ler $a.log
        Check 'final.resumo_testes_contratados_sem_falhas' ($resumo.testes -eq $a.testes -and $resumo.falhas -eq 0 -and $resumo.erros -eq 0 -and $resumo.ignorados -eq 0 -and $resumo.quantidadeXml -eq 17)
        Check 'final.log_build_success_testes_contratados' ($log -match '\[INFO\] BUILD SUCCESS' -and $log -match ('Tests run: '+$a.testes+', Failures: 0, Errors: 0, Skipped: 0') -and $log -match [regex]::Escape($a.finalizado))
        Check 'final.resumo_saida_target_be14' ($resumo.comando -match '(?:^|\s)-Dwms.build.directory=target-be14(?:\s|$)' -and $resumo.log -ceq [IO.Path]::GetFileName($a.log))
        $xmls=@(Get-ChildItem -LiteralPath (Join-Path $raiz $a.xmls) -Filter '*.xml' -File);$soma=0;$falhasXml=0;$errosXml=0;$ignoradosXml=0;$hashesXml=@()
        Check 'final.xml_17_copias' ($xmls.Count -eq 17 -and @($resumo.suites).Count -eq 17)
        foreach($x in $xmls) {
            $rel=$a.xmls+'/'+$x.Name;$h=Hash $rel;$e=@($resumo.suites|Where-Object {$_.xml -ceq $x.Name})
            Check ('final.xml_hash.'+$x.Name) ($e.Count -eq 1 -and $h -ceq $e[0].sha256)
            $settings=New-Object Xml.XmlReaderSettings;$settings.DtdProcessing=[Xml.DtdProcessing]::Prohibit;$settings.XmlResolver=$null
            $xr=[Xml.XmlReader]::Create($x.FullName,$settings)
            try {$doc=New-Object Xml.XmlDocument;$doc.XmlResolver=$null;$doc.Load($xr)} finally {$xr.Dispose()}
            $suite=$doc.DocumentElement
            if($suite.LocalName -cne 'testsuite'){throw 'XML_TESTE_FORMATO_NAO_CONTRATADO'}
            $soma+=[int]$suite.GetAttribute('tests');$falhasXml+=[int]$suite.GetAttribute('failures');$errosXml+=[int]$suite.GetAttribute('errors');$ignoradosXml+=[int]$suite.GetAttribute('skipped')
            $hashesXml+=[pscustomobject]@{arquivo=$rel;sha256=$h}
        }
        Check 'final.xml_soma_testes_contratados_sem_falhas' ($soma -eq $a.testes -and $falhasXml -eq 0 -and $errosXml -eq 0 -and $ignoradosXml -eq 0)
        $evidenciasJpa=@()
        foreach($e in $c.evidenciasJpaFinal) {
            $v=(Ler $e.arquivo)|ConvertFrom-Json
            $total=if($e.tipo -ceq 'principal'){$v.totalChecks}else{$v.total}
            $manifesto=if($e.tipo -ceq 'principal'){$v.comparacaoJpa.freeze}else{$v.freeze}
            Check ('final.evidencia_'+$e.tipo+'_integral') ($total -eq $e.total -and $v.aprovados -eq $e.total -and @($v.divergencias).Count -eq 0)
            Check ('final.evidencia_'+$e.tipo+'_mesmo_freeze') ($manifesto.sha256 -ceq $c.freezeFinal.sha256)
            $evidenciasJpa+=[pscustomobject]@{arquivo=$e.arquivo;sha256=Hash $e.arquivo;total=$total;aprovados=$v.aprovados;limite='Leitura estrutural/lexical, nao JPA executado ou regra de negocio aceita.'}
        }
        $artefato=[pscustomobject]@{situacao='REFERENCIA_ESTATICA_CONFERIDA';arquivo=$jar;bytes=$bytes;sha256=$hashJar;executado=$false;log=$a.log;logSha256=Hash $a.log;resumo=$a.resumo;resumoSha256=Hash $a.resumo;xmls=$hashesXml;buildReportado=[pscustomobject]@{testes=$soma;falhas=$falhasXml;erros=$errosXml;ignorados=$ignoradosXml;origem='Cedro/log e XML preservados, nao build executado por este leitor.'};evidenciasJpa=$evidenciasJpa}
        $jpa='REFERENCIAS_DE_LEITURA_FINAL_CONFERIDAS_SEM_EXECUTAR_JPA'
        Check 'final.manifesto_estavel_apos_leituras' ((Hash $c.freezeFinal.arquivo) -ceq $freeze.sha256)
    }
    $falhas=@($checks.Keys|Where-Object {-not $checks[$_]})
    [pscustomobject]@{formato='wms-preparacao-final-estatica-v1';capturadoEm=(Get-Date).ToString('o');natureza=$c.natureza;total=$checks.Count;atendidos=$checks.Count-$falhas.Count;falhas=$falhas.Count;divergencias=$falhas;conferencias=$checks;identidadeCiclos=$identidades.resumo;original56=[pscustomobject]@{total=$base.total;atendidos=$base.atendidos;powershellSuccess=$originalSucesso;lastExitCode=$originalCodigo};inventarioV1V9=$inventario;fontesTecnicas=$fontes;documentos=$hashes;semValoresAmbienteSegredos=$true;semRedeSqlJvmBuildFlyway=$true;artefatoFinal=$artefato;JpaBE14=$jpa;homologacaoExterna='NAO_EXECUTADA';limites=$c.limites}|ConvertTo-Json -Depth 12
    if ($falhas.Count) {exit 1}
} catch {
    $codigo='BLOQUEADO_LEITURA_OU_FORMATO';if($_.Exception.Message -match '^(?:BLOQUEADO|PARSER|FREEZE)_[A-Z0-9_]+$'){$codigo=$_.Exception.Message}
    [pscustomobject]@{formato='wms-preparacao-final-estatica-v1';status=$codigo;sem_raw=$true;sem_detalhes_sensiveis=$true}|ConvertTo-Json
    exit 1
}
