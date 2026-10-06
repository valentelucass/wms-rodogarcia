# Comparacao suplementar de arquivos do freeze V7; nao inicia JPA/banco/build.
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$contrato=Get-Content -LiteralPath (Join-Path $raiz 'database/contratos/v7-schema-doc29.json') -Raw -Encoding UTF8|ConvertFrom-Json
$sql=Get-Content -LiteralPath (Join-Path $raiz 'database/migrations/V7__cadastros_servicos_e_calculo.sql') -Raw -Encoding UTF8
$checks=[ordered]@{};$operacoes=@();$auditorias=@();$fontes=@();$divergencias=@()
function Registrar([string]$nome,[bool]$ok){$script:checks[$nome]=$ok;if(-not $ok){$script:divergencias+=$nome}}
function Linha([string]$src,[int]$indice){[regex]::Matches($src.Substring(0,$indice),'\n').Count+1}
$servicos=@('FiscalCadastroService','ImportacaoEnderecoService','ConfiguracaoCobrancaService','FatoServicoService','MarcoFinanceiroAvariaService','CalculoCobrancaService')
foreach($nome in $servicos){
$rel='backend/src/main/java/br/com/rodogarcia/wms/services/'+$nome+'.java'
$src=Get-Content -LiteralPath (Join-Path $raiz $rel) -Raw -Encoding UTF8
$fontes+=[pscustomobject]@{arquivo=$rel;sha256=(Get-FileHash -LiteralPath (Join-Path $raiz $rel) -Algorithm SHA256).Hash}
$salvas=@([regex]::Matches($src,'operacoes\.salvar\s*\(\s*[^,]+,\s*"(?<tipo>[A-Z_]+)"'))
Registrar ($nome+'.salvar_tipos_literais_cobertos') ($salvas.Count -eq [regex]::Matches($src,'operacoes\.salvar\s*\(').Count)
foreach($m in $salvas){
$tipo=$m.Groups['tipo'].Value
$aceito=$contrato.auditoria.tipoOperacaoAdministrativa -ccontains $tipo
$linha=Linha $src $m.Index
$operacoes+=[pscustomobject]@{tipo=$tipo;arquivo=$rel;linha=$linha;aceitoNoCheckV7=$aceito}
Registrar ($nome+'.operacao.'+$linha+'.'+$tipo) $aceito
}
$pares=@([regex]::Matches($src,'auditoria\.registrar\s*\(\s*"(?<tipo>[A-Z_]+)"\s*,\s*[^,]+,\s*"(?<acao>[A-Z_]+)"'))
Registrar ($nome+'.auditoria_pares_literais_cobertos') ($pares.Count -eq [regex]::Matches($src,'auditoria\.registrar\s*\(').Count)
foreach($m in $pares){
$tipo=$m.Groups['tipo'].Value;$acao=$m.Groups['acao'].Value
$ok=($contrato.auditoria.tiposAnteriores+$contrato.auditoria.tiposNovos) -ccontains $tipo
$ok=$ok -and (($contrato.auditoria.acoesAnteriores+$contrato.auditoria.acoesNovas) -ccontains $acao)
if($contrato.auditoria.acoesNovas -ccontains $acao){$ok=$ok -and ($contrato.auditoria.paresNovos.$acao -ccontains $tipo)}
if($contrato.auditoria.tiposNovos -ccontains $tipo){$ok=$ok -and ($contrato.auditoria.paresTiposNovos.$tipo -ccontains $acao)}
$linha=Linha $src $m.Index
$auditorias+=[pscustomobject]@{tipo=$tipo;acao=$acao;arquivo=$rel;linha=$linha;compativel=$ok}
Registrar ($nome+'.auditoria.'+$linha+'.'+$tipo+'/'+$acao) $ok
}
Registrar ($nome+'.sem_gravacao_fisica') ($src -notmatch 'movimentos\.save|movimentos\.registrar|new MovimentoEstoque|new FatoPermanencia')
}
# As colunas de dominio novas sao Strings no JPA; conferir as listas recebidas nos DTOs.
$dto=Get-Content -LiteralPath (Join-Path $raiz 'backend/src/main/java/br/com/rodogarcia/wms/dto/ConfiguracaoCobrancaDto.java') -Raw -Encoding UTF8
$alvos=@(
@('CriarServico','tipo','servico_cobranca','ck_servico_cobranca_tipo'),
@('CriarServico','unidade','servico_cobranca','ck_servico_cobranca_unidade'),
@('CriarTabela','tipo','tabela_cobranca','ck_tabela_cobranca_tipo'),
@('ConfigurarContrato','moeda','contrato_cobranca','ck_contrato_cobranca_moeda'),
@('ConfigurarContrato','modalidadeCiclo','contrato_cobranca','ck_contrato_cobranca_modalidade'),
@('ConfigurarContrato','minimoModo','contrato_cobranca','ck_contrato_cobranca_minimo_modo'),
@('ConfigurarContrato','grisModo','contrato_cobranca','ck_contrato_cobranca_gris_modo'),
@('ConfigurarContrato','minimoProporcao','contrato_cobranca','ck_contrato_cobranca_minimo_proporcao'),
@('ConfigurarContrato','grisBase','contrato_cobranca','ck_contrato_cobranca_gris_base'),
@('ConfigurarContrato','grisPeriodicidade','contrato_cobranca','ck_contrato_cobranca_gris_periodicidade'),
@('ConfigurarContrato','grisProporcao','contrato_cobranca','ck_contrato_cobranca_gris_proporcao'))
$dominios=@()
foreach($a in $alvos){
$record=[regex]::Match($dto,('(?s)public record '+$a[0]+'\((?<corpo>.*?)\) \{\}')).Groups['corpo'].Value
$observado=[regex]::Match($record,('@Pattern\s*\(\s*regexp\s*=\s*"(?<dominio>[^"]+)"\s*\)\s+String\s+'+$a[1]+'\b')).Groups['dominio'].Value -split '\|'
$tabela=$contrato.tabelas|Where-Object{$_.nome -ceq $a[2]}
$esperado=@([regex]::Matches($tabela.checks.($a[3]),"'([^']+)'")|ForEach-Object{$_.Groups[1].Value})
$ok=($observado.Count -eq $esperado.Count) -and (@(Compare-Object $observado $esperado -CaseSensitive).Count -eq 0)
$dominios+=[pscustomobject]@{record=$a[0];campo=$a[1];esperado=$esperado;observado=$observado;igual=$ok}
Registrar ('dominioDTO.'+$a[0]+'.'+$a[1]) $ok
}
$calcDto=Get-Content -LiteralPath (Join-Path $raiz 'backend/src/main/java/br/com/rodogarcia/wms/dto/CalculoCobrancaDto.java') -Raw -Encoding UTF8
$calcSvc=Get-Content -LiteralPath (Join-Path $raiz 'backend/src/main/java/br/com/rodogarcia/wms/services/CalculoCobrancaService.java') -Raw -Encoding UTF8
Registrar 'JSON.diarias_regras_segmentos_intervalosValor' ($calcDto -match 'List<RegraDiaria> regras' -and $calcDto -match 'List<Segmento> segmentos' -and $calcDto -match 'List<SegmentoValor> intervalosValor')
Registrar 'JSON.origens_valores_parcelas_ajustes' ($calcDto -match 'record OrigemValor' -and $calcDto -match 'record ValorEntrada' -and $calcDto -match 'List<Parcela> parcelas' -and $calcDto -match 'Ajustes ajustes')
Registrar 'JSON.serializacao_memoria_diaria_existente' ($calcSvc -match 'mapper.writeValueAsString\(memoria\)' -and $calcSvc -match 'mapper.writeValueAsString\(m\)')
Registrar 'JSON.singular_item_tarifa_nulo_multi_regra' ($calcSvc -match 'unica = rs.size\(\) == 1 \? rs.getFirst\(\) : null' -and $calcSvc -match 'unica == null \? null : unica.itemTabelaId\(\)' -and $calcSvc -match 'unica == null \? null : unica.tarifa\(\)')
$svc=Get-Content -LiteralPath (Join-Path $raiz 'backend/src/main/java/br/com/rodogarcia/wms/models/ServicoCobranca.java') -Raw -Encoding UTF8
Registrar 'servico_situacao_VARCHAR24' ($svc -match '@Column\(name = "situacao", nullable = false, length = 24\)')
$falhas=@($checks.Keys|Where-Object{-not $checks[$_]})
[pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');natureza='Leitura de literais/DTOs e JSON; nao e teste de servico, SQL ou Hibernate';verificacoes=$checks.Count;atendidas=$checks.Count-$falhas.Count;checks=$checks;divergencias=$divergencias;operacoes=$operacoes;auditorias=$auditorias;dominios=$dominios;fontes=$fontes;limite='Strings/literais diretos no recorte, sem interpretacao de fluxo nem prova de SQL emitido'}|ConvertTo-Json -Depth 6
if($falhas.Count){exit 1}

