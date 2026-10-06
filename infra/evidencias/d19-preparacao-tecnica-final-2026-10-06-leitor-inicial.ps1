# Diagnostico final de PREPARACAO: somente arquivos conhecidos. Nao resolve env ou URL.
[CmdletBinding()]
param()
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
        Check ('migration.sem_DML_historico.'+$numero) ($limpo -notmatch '\b(INSERT|UPDATE|DELETE|MERGE)\b')
        $tab=@([regex]::Matches($limpo,'CREATE TABLE wms\.(\w+)')|ForEach-Object {$_.Groups[1].Value})
        $todas+=$tab;$inventario+=[pscustomobject]@{migration=$m.arquivo;sha256=$h;tabelas=$tab;quantidade=$tab.Count}
        $numero++
    }
    Check 'inventario.64_tabelas_sem_duplicar' ($todas.Count -eq 64 -and @($todas|Group-Object|Where-Object {$_.Count -gt 1}).Count -eq 0)
    $permissoes=Ler 'database/permissoes-minimas.md'
    foreach ($t in $todas) {Check ('permissoes.tabela_documentada.'+$t) ($permissoes -match ('\b'+[regex]::Escape($t)+'\b'))}
    Check 'permissoes.identidades_separadas_sem_grant_aplicado' ($permissoes -match 'Aplicac' -and $permissoes -match 'Migration WMS' -and $permissoes -match 'DBA/recupera' -and $permissoes -match 'Nenhum usu')
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
    Check 'ensaio.RPO_RTO_nao_definidos' ($ensaio -match 'RPO/RTO' -and $ensaio -match 'N[a-zA-Z\W]*inventar frequ')
    Check 'ensaio.restauracao_mais_que_VERIFYONLY' ($ensaio -match 'VERIFYONLY' -and $ensaio -match 'n[^.]+restaura')
    Check 'ensaio.sem_repair_clean_baseline_automaticos' ($ensaio -match 'repair' -and $ensaio -match 'baseline' -and $ensaio -match 'clean' -and $ensaio -match 'automatic')
    Check 'ensaio.donos_SQL_operacao_fiscal_equipamentos' ($ensaio -match 'Lucas/TI' -and $ensaio -match 'Caio' -and $ensaio -match 'Natalina' -and $ensaio -match 'Mickael')
    $wrapper=Ler 'database/migrate.ps1'
    Check 'wrapper.Info_Validate_conectam_nao_executado' ($wrapper.Contains("'Info', 'Validate', 'Migrate'") -and $wrapper.Contains('$connection.Open()') -and $wrapper.IndexOf('$connection.Open()') -lt $wrapper.IndexOf('& ./mvnw.cmd'))
    Check 'wrapper.erro_driver_sem_dump' ($wrapper.Contains('Nao expor a string de conexao') -and $wrapper.Contains('Nenhuma migracao foi solicitada'))
    foreach ($doc in $c.documentos) {Check ('documento.existe.'+$doc) (Test-Path -LiteralPath (Join-Path $raiz $doc) -PathType Leaf);$hashes+=[pscustomobject]@{arquivo=$doc;sha256=Hash $doc}}
    $pacote=Ler 'infra/preparacao-tecnica-final.md'
    Check 'pacote.local_artefato_final_e_externo_separados' ($pacote -match 'artefato final' -and $pacote -match 'homologa' -and $pacote -match 'freeze BE14')
    Check 'pacote.donos_sem_provedor_inventado' ($pacote -match 'DBA a identificar' -and $pacote -match 'Lucas' -and $pacote -match 'Gestor/comercial' -and $pacote -match 'provedor')
    $falhas=@($checks.Keys|Where-Object {-not $checks[$_]})
    [pscustomobject]@{formato='wms-preparacao-final-estatica-v1';capturadoEm=(Get-Date).ToString('o');natureza=$c.natureza;total=$checks.Count;atendidos=$checks.Count-$falhas.Count;falhas=$falhas.Count;divergencias=$falhas;conferencias=$checks;original56=[pscustomobject]@{total=$base.total;atendidos=$base.atendidos;powershellSuccess=$originalSucesso;lastExitCode=$originalCodigo};inventarioV1V9=$inventario;fontesTecnicas=$fontes;documentos=$hashes;semValoresAmbienteSegredos=$true;semRedeSqlJvmBuildFlyway=$true;artefatoFinal='AINDA_EM_CONSTRUCAO_NAO_CONFERIDO';JpaBE14='AGUARDA_FREEZE_TAREFA_SEPARADA';homologacaoExterna='NAO_EXECUTADA';limites=$c.limites}|ConvertTo-Json -Depth 12
    if ($falhas.Count) {exit 1}
} catch {
    '{"formato":"wms-preparacao-final-estatica-v1","status":"BLOQUEADO_LEITURA_OU_FORMATO","sem_raw":true,"sem_detalhes_sensiveis":true}'
    exit 1
}
