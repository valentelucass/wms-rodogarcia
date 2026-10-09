// Gerado de fontes Java atuais. Long/BigDecimal são lexemas internos; no fio são números JSON.
export type CargaInicialDto_FiltroSituacao = "PENDENTE" | "PREPARADA" | "REGULARIZADA" | "CANCELADA";
export type ContagemDto_FiltroSituacao = "PENDENTE" | "PENDENTE_RESERVA" | "RECONCILIADA" | "SUBSTITUIDA" | "APLICADA";
export type ContingenciaDto_FiltroSituacao = "PENDENTE" | "CONCILIADA";
export type ContingenciaDto_FiltroTipo = "CHEGADA" | "RESERVA" | "SEPARACAO" | "RETIRADA" | "RETORNO" | "ENTRADA" | "AVARIA" | "FATO_SERVICO" | "CONTAGEM" | "AJUSTE" | "REMANEJAMENTO";
export type EncerramentoDto_Tipo = "CLIENTE" | "ARMAZEM" | "PRODUTO" | "EMBALAGEM" | "ENDERECO" | "CONJUNTO_POSICOES" | "SERVICO_COBRANCA";
export type CondicaoMercadoria = "BOA" | "AVARIADA";
export type SituacaoCadastro = "ATIVO" | "ENCERRAMENTO_PENDENTE" | "INATIVO";
export type SituacaoCargaInicial = "PENDENTE" | "PREPARADA" | "REGULARIZADA" | "CANCELADA";
export type SituacaoContingencia = "PENDENTE" | "CONCILIADA";
export type SituacaoPedidoEntrada = "RASCUNHO" | "EM_CONFERENCIA" | "QUARENTENA" | "EFETIVADO" | "CANCELADO";
export type SituacaoPedidoSaida = "RASCUNHO" | "RESERVADO" | "EM_SEPARACAO" | "SEPARADO" | "RETIRADO" | "CANCELADO";
export type SituacaoReservaSaida = "ATIVA" | "CANCELADA" | "REVERTIDA" | "RETIRADA";
export type SituacaoRevisaoContagem = "PENDENTE" | "PENDENTE_RESERVA" | "RECONCILIADA" | "SUBSTITUIDA" | "APLICADA";
export type TipoContingencia = "CHEGADA" | "RESERVA" | "SEPARACAO" | "RETIRADA" | "RETORNO" | "ENTRADA" | "AVARIA" | "FATO_SERVICO" | "CONTAGEM" | "AJUSTE" | "REMANEJAMENTO";
export type TipoEndereco = "ARMAZENAGEM" | "TRIAGEM" | "QUARENTENA" | "SEPARACAO";
export type TipoQuantidade = "CONTAGEM" | "MEDIDA";
export type TipoUnidadeLogistica = "PALLET" | "BOBINA" | "VOLUME";
export interface AcessoDtos_Login {
  email: string;
  senha: string;
}
export interface AcessoDtos_TrocaSenha {
  senhaAtual: string;
  novaSenha: string;
}
export interface AcessoDtos_CriarUsuario {
  nome: string;
  email: string;
  senhaTemporaria: string;
  perfil: string;
  administrador: boolean;
  clientes: Array<string>;
  armazens: Array<string>;
}
export interface AcessoDtos_EditarUsuario {
  nome: string;
  perfil: string;
  administrador: boolean;
  ativo: boolean;
  clientes: Array<string>;
  armazens: Array<string>;
  versao: string;
}
export interface AcessoDtos_RedefinirSenha {
  senhaTemporaria: string;
  versao: string;
}
export interface AcessoDtos_Usuario {
  id: string | null;
  nome: string | null;
  email: string | null;
  perfil: string | null;
  administrador: boolean;
  principal: boolean;
  ativo: boolean;
  trocarSenha: boolean;
  clientes: Array<string> | null;
  armazens: Array<string> | null;
  versao: string;
}
export interface AcessoDtos_Tokens {
  accessToken: string | null;
  expiresIn: string;
  usuario: AcessoDtos_Usuario | null;
}
export interface AcessoDtos_PaginaUsuarios {
  content: Array<AcessoDtos_Usuario> | null;
  number: number;
  totalPages: number;
  totalElements: string;
}
export interface ArmazemDto_Criar {
  codigo: string;
  nome: string;
  documentoFiscal: string;
  cidade: string;
  uf: string;
}
export interface ArmazemDto_Alterar {
  versao: string;
  nome: string;
  motivo: string;
}
export interface ArmazemDto_Resposta {
  id: string | null;
  versao: string;
  situacao: SituacaoCadastro | null;
  criadoEm: string | null;
  alteradoEm: string | null;
  codigo: string | null;
  nome: string | null;
  documentoFiscal: string | null;
  cidade: string | null;
  uf: string | null;
}
export interface AuditoriaResponse {
  id: string | null;
  tipo: string | null;
  registroId: string | null;
  acao: string | null;
  usuario: string | null;
  instante: string | null;
  idOperacao: string | null;
  motivo: string | null;
  dadosAntes: string | null;
  dadosDepois: string | null;
  tipoFisico: string | null;
}
export interface AvariaDto_Registrar {
  operacaoId: string;
  versaoUnidade: string;
  quantidade: string;
  ocorridaEm: string;
  conjuntoId: string | null;
  destinos: Array<EstoqueDto_Destino>;
  motivo: string;
  resolverPendentes: boolean | null;
}
export interface AvariaDto_Reconhecer {
  operacaoId: string;
  versao: string;
  responsabilidade: string;
  motivo: string;
}
export interface AvariaDto_Reparar {
  operacaoId: string;
  versao: string;
  versaoUnidade: string;
  conjuntoId: string | null;
  destinos: Array<EstoqueDto_Destino>;
  motivo: string;
  resolverPendentes: boolean | null;
}
export interface AvariaDto_Ocorrencia {
  id: string | null;
  versao: string;
  unidadeId: string | null;
  quantidade: string | null;
  quantidadeBase: string | null;
  equivalenciaBase: string | null;
  cicloId: string | null;
  ocorridaEm: string | null;
  registradaEm: string | null;
  responsabilidade: string | null;
  relato: string | null;
  reconhecidaEm: string | null;
  validadaPor: string | null;
  tratativa: string | null;
  resolvidaEm: string | null;
  proporcaoSuspensa: string | null;
  inicioSuspensao: string | null;
}
export interface AvariaDto_Confirmacao {
  operacaoId: string | null;
  avaria: AvariaDto_Ocorrencia | null;
  estoque: EstoqueDto_Unidade | null;
}
export interface CalculoCobrancaDto_Calcular {
  operacaoId: string;
  clienteId: string;
  armazemId: string;
  periodoInicio: string;
  periodoFim: string;
  motivo: string;
  resolucao?: FechamentoCobrancaDto_Resolucao | null;
}
export interface CalculoCobrancaDto_Pendencia {
  codigo: string | null;
  data: string | null;
  unidadeId: string | null;
  fatoId: string | null;
  detalhe: string | null;
}
export interface CalculoCobrancaDto_Contribuicao {
  unidadeId: string | null;
  codigo: string | null;
  categoria: string | null;
  quantidade: string | null;
  equivalenciaFisica: string | null;
  quantidadeAvariada: string | null;
  equivalenciaSuspensa: string | null;
  equivalenciaLiquida: string | null;
  valorEstoque: string | null;
  avarias: Array<string> | null;
  fatos: Array<string> | null;
  origens: Array<CalculoCobrancaDto_OrigemValor> | null;
}
export interface CalculoCobrancaDto_OrigemValor {
  entradaId: string | null;
  notaId: string | null;
  itemNotaId: string | null;
  quantidade: string | null;
  quantidadeItem: string | null;
  valorItem: string | null;
  valorUnitario: string | null;
  valorHistorico: string | null;
}
export interface CalculoCobrancaDto_ValorEntrada {
  entradaId: string | null;
  notaId: string | null;
  itemNotaId: string | null;
  quantidadeBoa: string | null;
  quantidadeAvariada: string | null;
  valorHistorico: string | null;
}
export interface CalculoCobrancaDto_SegmentoValor {
  inicio: string | null;
  fim: string | null;
  total: string | null;
  unidades: Array<CalculoCobrancaDto_Contribuicao> | null;
  entradasNaoUnitizadas: Array<CalculoCobrancaDto_ValorEntrada> | null;
}
export interface CalculoCobrancaDto_Segmento {
  inicio: string | null;
  fim: string | null;
  contribuicoes: Array<CalculoCobrancaDto_Contribuicao> | null;
}
export interface CalculoCobrancaDto_RegraDiaria {
  tabelaId: string | null;
  itemTabelaId: string | null;
  servicoId: string | null;
  categoria: string | null;
  pico: string | null;
  tarifa: string | null;
  valor: string | null;
  instantePico: string | null;
}
export interface CalculoCobrancaDto_Diaria {
  data: string | null;
  tabelaId: string | null;
  itemTabelaId: string | null;
  picoCobravel: string | null;
  equivalenciaSuspensa: string | null;
  valorEstoque: string | null;
  tarifa: string | null;
  valor: string | null;
  regras: Array<CalculoCobrancaDto_RegraDiaria> | null;
  segmentos: Array<CalculoCobrancaDto_Segmento> | null;
  intervalosValor: Array<CalculoCobrancaDto_SegmentoValor> | null;
}
export interface CalculoCobrancaDto_Parcela {
  notaId: string | null;
  cota: string | null;
  valor: string | null;
}
export interface CalculoCobrancaDto_Servico {
  fatoId: string | null;
  servicoId: string | null;
  chaveFato: string | null;
  executadoEm: string | null;
  tabelaId: string | null;
  itemTabelaId: string | null;
  quantidade: string | null;
  preco: string | null;
  percentual: string | null;
  valorBase: string | null;
  valor: string | null;
  antesArredondamento: string | null;
  parcelas: Array<CalculoCobrancaDto_Parcela> | null;
}
export interface CalculoCobrancaDto_Ajustes {
  cicloInicio: string | null;
  cicloFim: string | null;
  diasIncluidos: string;
  diasNominais: string;
  abrangenciaMinimo: Array<string> | null;
  subtotalElegivel: string | null;
  minimoAplicavel: string | null;
  minimoComplemento: string | null;
  grisBase: string | null;
  grisPeriodicidade: string | null;
  grisProporcao: string | null;
  grisPercentual: string | null;
  baseGris: string | null;
  valorGris: string | null;
  regraGris: string | null;
}
export interface CalculoCobrancaDto_Memoria {
  diarias: Array<CalculoCobrancaDto_Diaria> | null;
  servicos: Array<CalculoCobrancaDto_Servico> | null;
  ajustes: CalculoCobrancaDto_Ajustes | null;
}
export interface CalculoCobrancaDto_Resultado {
  id: string | null;
  clienteId: string | null;
  armazemId: string | null;
  contratoId: string | null;
  periodoInicio: string | null;
  periodoFim: string | null;
  fuso: string | null;
  moeda: string | null;
  situacao: string | null;
  regraVersao: number;
  entradasHash: string | null;
  subtotalConhecido: string | null;
  minimoCalculado: string | null;
  grisCalculado: string | null;
  total: string | null;
  calculadoEm: string | null;
  pendencias: Array<CalculoCobrancaDto_Pendencia> | null;
  memoria: CalculoCobrancaDto_Memoria | null;
}
export interface CapacidadeDto_Limites {
  pesoKg: string;
  alturaMetros: string;
  larguraMetros: string;
  profundidadeMetros: string;
  empilhamentoMaximo: number;
}
export interface CapacidadeDto_ConfigurarEndereco {
  versao: string;
  tipoUnidadePermitido: TipoUnidadeLogistica;
  limites: CapacidadeDto_Limites;
  motivo: string;
}
export interface CapacidadeDto_CriarConjunto {
  armazemId: string;
  codigo: string;
  enderecoAId: string;
  enderecoBId: string;
  limites: CapacidadeDto_Limites;
  motivo: string;
}
export interface CapacidadeDto_Conjunto {
  id: string | null;
  versao: string;
  situacao: SituacaoCadastro | null;
  armazemId: string | null;
  codigo: string | null;
  enderecoAId: string | null;
  enderecoBId: string | null;
  tipoEndereco: TipoEndereco | null;
  tipoUnidadePermitido: TipoUnidadeLogistica | null;
  limites: CapacidadeDto_Limites | null;
}
export interface CargaInicialDto_Dados {
  entradaExistenteId: string | null;
  referenciaPedido: string | null;
  nota: PedidoEntradaDto_NotaManual | null;
  chegadaReal: string | null;
  dataFifo: string | null;
  lote: string | null;
  validade: string | null;
  quantidadeBoa: string | null;
  quantidadeAvariada: string | null;
  unidades: Array<UnidadeLogisticaDto_NovaUnidade> | null;
  fonte: string | null;
}
export interface CargaInicialDto_Criar {
  operacaoId: string;
  clienteId: string;
  armazemId: string;
  produtoId: string;
  referencia: string;
  etiquetaFornecida: string | null;
  quantidade: string;
  dados: CargaInicialDto_Dados;
  motivo: string;
}
export interface CargaInicialDto_Revisar {
  operacaoId: string;
  versao: string;
  dados: CargaInicialDto_Dados;
  motivo: string;
}
export interface CargaInicialDto_Confirmar {
  operacaoId: string;
  versao: string;
  revisao: number;
  conteudoHash: string;
  leitura: string;
  etiquetasUnidades: Array<string>;
  motivo: string;
}
export interface CargaInicialDto_Cancelar {
  operacaoId: string;
  versao: string;
  motivo: string;
}
export interface CargaInicialDto_ResolverCancelamento {
  operacaoId: string;
  versao: string;
  pedidoResolucaoId: string | null;
  contagensIds: Array<string>;
  motivo: string;
}
export interface CargaInicialDto_Revisao {
  numero: number;
  conteudoHash: string | null;
  dados: CargaInicialDto_Dados | null;
}
export interface CargaInicialDto_Resultado {
  id: string | null;
  versao: string;
  clienteId: string | null;
  armazemId: string | null;
  produtoId: string | null;
  referencia: string | null;
  etiquetaFornecida: string | null;
  quantidadeEstagio: string | null;
  situacao: SituacaoCargaInicial | null;
  revisao: CargaInicialDto_Revisao | null;
  entradaId: string | null;
  etiquetas: Array<string> | null;
  pendencias: Array<string> | null;
}
export interface ClienteDto_Criar {
  codigo: string;
  nome: string;
  documentoFiscal: string;
}
export interface ClienteDto_Alterar {
  versao: string;
  nome: string;
  motivo: string;
}
export interface ClienteDto_Resposta {
  id: string | null;
  versao: string;
  situacao: SituacaoCadastro | null;
  criadoEm: string | null;
  alteradoEm: string | null;
  codigo: string | null;
  nome: string | null;
  documentoFiscal: string | null;
}
export interface ConfiguracaoCobrancaDto_CriarServico {
  operacaoId: string;
  codigo: string;
  descricao: string;
  tipo: string;
  unidade: string;
  motivo: string;
}
export interface ConfiguracaoCobrancaDto_Servico {
  id: string | null;
  versao: string;
  codigo: string | null;
  descricao: string | null;
  tipo: string | null;
  unidade: string | null;
  situacao: string | null;
}
export interface ConfiguracaoCobrancaDto_Item {
  servicoId: string;
  categoria: string;
  preco: string | null;
  percentual: string | null;
}
export interface ConfiguracaoCobrancaDto_CriarTabela {
  operacaoId: string;
  armazemId: string;
  clienteId: string | null;
  codigo: string;
  descricao: string;
  tipo: string;
  vigenciaInicio: string;
  vigenciaFim: string | null;
  itens: Array<ConfiguracaoCobrancaDto_Item>;
  motivo: string;
  resolucao?: FechamentoCobrancaDto_Resolucao | null;
}
export interface ConfiguracaoCobrancaDto_ItemResposta {
  id: string | null;
  servicoId: string | null;
  categoria: string | null;
  preco: string | null;
  percentual: string | null;
}
export interface ConfiguracaoCobrancaDto_Tabela {
  id: string | null;
  versao: string;
  armazemId: string | null;
  clienteId: string | null;
  codigo: string | null;
  tipo: string | null;
  vigenciaInicio: string | null;
  vigenciaFim: string | null;
  situacao: string | null;
  itens: Array<ConfiguracaoCobrancaDto_ItemResposta> | null;
}
export interface ConfiguracaoCobrancaDto_Vincular {
  operacaoId: string;
  clienteId: string;
  armazemId: string;
  tabelaId: string;
  vigenciaInicio: string;
  vigenciaFim: string | null;
  motivo: string;
  resolucao?: FechamentoCobrancaDto_Resolucao | null;
}
export interface ConfiguracaoCobrancaDto_Vinculo {
  id: string | null;
  versao: string;
  clienteId: string | null;
  armazemId: string | null;
  tabelaId: string | null;
  vigenciaInicio: string | null;
  vigenciaFim: string | null;
}
export interface ConfiguracaoCobrancaDto_Encerrar {
  operacaoId: string;
  versao: string;
  vigenciaFim: string;
  motivo: string;
  resolucao?: FechamentoCobrancaDto_Resolucao | null;
}
export interface ConfiguracaoCobrancaDto_ConfigurarContrato {
  operacaoId: string;
  clienteId: string;
  armazemId: string;
  vigenciaInicio: string;
  vigenciaFim: string | null;
  fuso: string;
  moeda: string;
  modalidadeCiclo: string;
  diaCorte: number | null;
  duracaoDias: number | null;
  minimoModo: string;
  grisModo: string;
  minimoValor: string | null;
  minimoProporcao: string | null;
  servicosMinimo: Array<string>;
  grisPercentual: string | null;
  grisBase: string | null;
  grisPeriodicidade: string | null;
  grisProporcao: string | null;
  motivo: string;
  resolucao?: FechamentoCobrancaDto_Resolucao | null;
}
export interface ConfiguracaoCobrancaDto_Contrato {
  id: string | null;
  versao: string;
  clienteId: string | null;
  armazemId: string | null;
  vigenciaInicio: string | null;
  vigenciaFim: string | null;
  fuso: string | null;
  moeda: string | null;
  modalidadeCiclo: string | null;
  diaCorte: number | null;
  duracaoDias: number | null;
  minimoModo: string | null;
  grisModo: string | null;
  minimoValor: string | null;
  minimoProporcao: string | null;
  servicosMinimo: Array<string> | null;
  grisPercentual: string | null;
  grisBase: string | null;
  grisPeriodicidade: string | null;
  grisProporcao: string | null;
}
export interface ContagemDto_Contar {
  operacaoId: string;
  codigoUnidade: string;
  versaoUnidade: string;
  contado: string;
  observadoEm: string;
  motivo: string;
}
export interface ContagemDto_DeltaOrigem {
  entradaId: string;
  delta: string;
}
export interface ContagemDto_Aplicar {
  operacaoId: string;
  revisao: number;
  versaoUnidade: string;
  motivo: string;
  causa: string;
  destino: string;
  comprovacao: string;
  origens: Array<ContagemDto_DeltaOrigem>;
}
export interface ContagemDto_Origem {
  entradaId: string | null;
  itemNotaId: string | null;
  quantidade: string | null;
}
export interface ContagemDto_Resultado {
  id: string | null;
  versao: string;
  unidadeId: string | null;
  codigoUnidade: string | null;
  revisao: number;
  esperado: string | null;
  contado: string | null;
  diferenca: string | null;
  reservado: string | null;
  observadoEm: string | null;
  situacao: SituacaoRevisaoContagem | null;
  impedimento: boolean;
  origens: Array<ContagemDto_Origem> | null;
  efeitoJson: string | null;
}
export interface ContingenciaDto_Registrar {
  operacaoId: string;
  identidadeFato: string;
  clienteId: string;
  armazemId: string;
  tipo: TipoContingencia;
  ocorridaEm: string;
  operador: string;
  fonte: string;
  efeitoRegistradoNoWms: boolean;
  dependencias: Array<string>;
  dados: Record<string, unknown>;
  motivo: string;
}
export interface ContingenciaDto_Conteudo {
  dados: Record<string, unknown> | null;
  dependencias: Array<string> | null;
  efeitoRegistradoNoWms: boolean;
}
export interface ContingenciaDto_Prova {
  operacaoOriginal: string;
  conteudoHash: string;
}
export interface ContingenciaDto_Conciliar {
  operacaoId: string;
  versao: string;
  modo: string;
  prova: ContingenciaDto_Prova | null;
  motivo: string;
}
export interface ContingenciaDto_Resultado {
  id: string | null;
  versao: string;
  identidadeFato: string | null;
  clienteId: string | null;
  armazemId: string | null;
  tipo: TipoContingencia | null;
  ocorridaEm: string | null;
  conteudoHash: string | null;
  conteudo: ContingenciaDto_Conteudo | null;
  situacao: SituacaoContingencia | null;
  pendencia: string | null;
  conciliadaEm: string | null;
  resultado: unknown | null;
}
export interface DashboardDto_Area {
  tipo: TipoEndereco;
  posicoesCliente: string;
  capacidadeAtiva: string | null;
  livresArmazem: string | null;
}
export interface DashboardDto_Fila {
  situacao: SituacaoPedidoSaida;
  pedidos: string;
}
export interface DashboardDto_Produto {
  produtoId: string;
  sku: string;
  unidadeMedida: string;
  fisicoTotal: string;
  disponivel: string;
  reservado: string;
  indisponivel: string;
  pendenteUnitizacao: string;
}
export interface DashboardDto_Resumo {
  clienteId: string;
  armazemId: string;
  consultadoEm: string;
  fuso: string;
  posicoesCliente: string;
  unidadesDisponiveis: string;
  pedidosAbertos: string;
  unidadesComAviso: string | null;
  antecedenciaValidade: number | null;
  visaoArmazem: boolean;
  areas: Array<DashboardDto_Area>;
  fila: Array<DashboardDto_Fila>;
  produtos: PaginaResponse<DashboardDto_Produto>;
}
export interface EmbalagemDto_Criar {
  produtoId: string;
  codigoDun: string;
  descricao: string;
  quantidadeProduto: string;
}
export interface EmbalagemDto_Alterar {
  versao: string;
  descricao: string;
  motivo: string;
}
export interface EmbalagemDto_Resposta {
  id: string | null;
  versao: string;
  situacao: SituacaoCadastro | null;
  criadoEm: string | null;
  alteradoEm: string | null;
  produtoId: string | null;
  codigoDun: string | null;
  descricao: string | null;
  quantidadeProduto: string | null;
}
export interface EncerramentoDto_Confirmar {
  operacaoId: string;
  versao: string;
  motivo: string;
}
export interface EncerramentoDto_Impedimento {
  codigo: string | null;
  recurso: string | null;
  id: string | null;
  detalhe: string | null;
}
export interface EncerramentoDto_Resultado {
  tipo: EncerramentoDto_Tipo | null;
  id: string | null;
  versao: string;
  situacao: string | null;
  impedimentos: Array<EncerramentoDto_Impedimento> | null;
}
export interface EncerramentoDto_Unidade {
  unidadeId: string;
  versao: string;
  quantidade: string;
}
export interface EncerramentoDto_Remanescente {
  operacaoId: string;
  clienteId: string;
  armazemId: string;
  referencia: string;
  unidades: Array<EncerramentoDto_Unidade>;
  cargaInicialId: string | null;
  etiquetas: Array<string>;
  justificativaFifo: string | null;
  motivo: string;
}
export interface EncerramentoDto_EncerrarVigencia {
  operacaoId: string;
  versao: string;
  corte: string;
  resolucao: FechamentoCobrancaDto_Resolucao;
  motivo: string;
}
export interface EncerramentoDto_Vigencia {
  tipo: string | null;
  id: string | null;
  versao: string;
  inicio: string | null;
  fim: string | null;
}
export interface EnderecoDto_Criar {
  armazemId: string;
  codigo: string;
  rua: string;
  nivel: number;
  posicao: string;
  descricao: string;
  tipo: TipoEndereco;
  capacidadePesoKg: string | null;
  alturaMetros: string | null;
  larguraMetros: string | null;
  profundidadeMetros: string | null;
  empilhamentoMaximo: number | null;
  sequenciaColeta: number;
}
export interface EnderecoDto_Alterar {
  versao: string;
  descricao: string;
  motivo: string;
}
export interface EnderecoDto_Resposta {
  id: string | null;
  versao: string;
  situacao: SituacaoCadastro | null;
  criadoEm: string | null;
  alteradoEm: string | null;
  armazemId: string | null;
  codigo: string | null;
  rua: string | null;
  nivel: number;
  posicao: string | null;
  descricao: string | null;
  tipo: TipoEndereco | null;
  capacidadePesoKg: string | null;
  alturaMetros: string | null;
  larguraMetros: string | null;
  profundidadeMetros: string | null;
  empilhamentoMaximo: number | null;
  sequenciaColeta: number;
  tipoUnidadePermitido: TipoUnidadeLogistica | null;
}
export interface ErroCampoResponse {
  campo: string | null;
  codigo: string | null;
}
export interface EstoqueDto_Medidas {
  pesoKg: string;
  alturaMetros: string;
  larguraMetros: string;
  profundidadeMetros: string;
  empilhamento: number;
  posicoesNecessarias: number;
}
export interface EstoqueDto_Destino {
  enderecoId: string;
  codigoLido: string;
}
export interface EstoqueDto_Posicionar {
  operacaoId: string;
  versaoUnidade: string;
  medidas: EstoqueDto_Medidas | null;
  conjuntoId: string | null;
  destinos: Array<EstoqueDto_Destino>;
  motivo: string;
}
export interface EstoqueDto_Bloqueio {
  operacaoId: string;
  versaoUnidade: string;
  motivo: string;
}
export interface EstoqueDto_Posicao {
  enderecoId: string | null;
  codigo: string | null;
  tipo: TipoEndereco | null;
  situacao: SituacaoCadastro | null;
  limites: CapacidadeDto_Limites | null;
}
export interface EstoqueDto_Unidade {
  unidade: UnidadeLogisticaDto_Resumo | null;
  medidas: EstoqueDto_Medidas | null;
  tipoLocalizacao: TipoEndereco | null;
  bloqueada: boolean;
  avariaPosterior: boolean;
  avariaInicialReparada: boolean;
  primeiroEnderecamentoEm: string | null;
  inicioArmazenagemEm: string | null;
  posicoesEquivalentes: number;
  conjuntoId: string | null;
  posicoes: Array<EstoqueDto_Posicao> | null;
}
export interface EstoqueDto_Confirmacao {
  operacaoId: string | null;
  pedidoId: string | null;
  versaoPedido: string;
  estoque: EstoqueDto_Unidade | null;
  avaria: AvariaDto_Ocorrencia | null;
}
export interface EstoqueDto_Movimento {
  id: string | null;
  operacaoId: string | null;
  acao: string | null;
  usuario: string | null;
  motivo: string | null;
  instante: string | null;
  antes: EstoqueDto_Unidade | null;
  depois: EstoqueDto_Confirmacao | null;
}
export interface EstoqueDto_Saldo {
  clienteId: string | null;
  armazemId: string | null;
  produtoId: string | null;
  unidadeMedida: string | null;
  fisicoTotal: string | null;
  pendenteUnitizacao: string | null;
  fisicoUnitizado: string | null;
  disponivel: string | null;
  reservado: string | null;
  bloqueado: string | null;
  naoEnderecado: string | null;
  emTriagem: string | null;
  emQuarentena: string | null;
  emArmazenagem: string | null;
  avariado: string | null;
}
export interface ExpedicaoDto_Leitura {
  operacaoId: string;
  versao: string;
  reservaId: string;
  codigoLido: string;
  revisaoConteudo: string;
  motivo: string;
  resolverPendentes: boolean | null;
}
export interface ExpedicaoDto_Destinacao {
  reservaId: string;
  conjuntoId: string | null;
  destinos: Array<EstoqueDto_Destino>;
}
export interface ExpedicaoDto_Separar {
  operacaoId: string;
  versao: string;
  destinacao: ExpedicaoDto_Destinacao;
  motivo: string;
  resolverPendentes: boolean | null;
}
export interface ExpedicaoDto_Nota {
  emitenteCnpj: string;
  serie: string;
  numero: string;
  emissao: string;
  chaveAcesso: string | null;
}
export interface ExpedicaoDto_Cobertura {
  reservaId: string;
  notaOrigemId: string;
  sku: string;
  quantidade: string;
}
export interface ExpedicaoDto_Documento {
  operacaoId: string;
  versao: string;
  origem: string;
  natureza: string;
  nota: ExpedicaoDto_Nota | null;
  protocolo: string;
  xml: string | null;
  coberturas: Array<ExpedicaoDto_Cobertura>;
  motivo: string;
  resolverPendentes: boolean | null;
}
export interface ExpedicaoDto_CancelarDocumento {
  operacaoId: string;
  versao: string;
  motivo: string;
}
export interface ExpedicaoDto_Retirar {
  operacaoId: string;
  versao: string;
  xmls: Array<string>;
  remanescentes: Array<ExpedicaoDto_Destinacao>;
  motivo: string;
  resolverPendentes: boolean | null;
}
export interface ExpedicaoDto_Retornar {
  operacaoId: string;
  versao: string;
  unidades: Array<ExpedicaoDto_Destinacao>;
  motivo: string;
  resolverPendentes: boolean | null;
}
export interface ExpedicaoDto_ItemDevolucao {
  baixaId: string;
  quantidade: string;
  quantidadeAvariada: string;
}
export interface ExpedicaoDto_Devolver {
  operacaoId: string;
  versao: string;
  referencia: string;
  nota: ExpedicaoDto_Nota;
  chegadaReal: string;
  itens: Array<ExpedicaoDto_ItemDevolucao>;
  motivo: string;
  resolverPendentes: boolean | null;
}
export interface ExpedicaoDto_Separacao {
  id: string | null;
  reservaId: string | null;
  situacao: string | null;
  revisaoConteudoLida: string;
  lidaEm: string | null;
  separadaEm: string | null;
  encerradaEm: string | null;
  posicoesOrigem: Array<EstoqueDto_Destino> | null;
  conjuntoOrigemId: string | null;
}
export interface ExpedicaoDto_DocumentoRegistrado {
  id: string | null;
  origem: string | null;
  natureza: string | null;
  nota: ExpedicaoDto_Nota | null;
  protocolo: string | null;
  situacao: string | null;
  registradoEm: string | null;
  canceladoEm: string | null;
  coberturas: Array<ExpedicaoDto_Cobertura> | null;
}
export interface ExpedicaoDto_Baixa {
  id: string | null;
  reservaId: string | null;
  entradaOrigemId: string | null;
  quantidade: string | null;
  dataFifo: string | null;
  inicioArmazenagemEm: string | null;
  posicoesEquivalentes: string | null;
}
export interface ExpedicaoDto_Devolucao {
  id: string | null;
  baixaId: string | null;
  pedidoEntradaId: string | null;
  entradaNovaId: string | null;
  quantidade: string | null;
  dataFifo: string | null;
  registradaEm: string | null;
}
export interface ExpedicaoDto_Fato {
  id: string | null;
  unidadeId: string | null;
  tipo: string | null;
  ocorridaEm: string | null;
  registradaEm: string | null;
  quantidadeAntes: string | null;
  quantidadeDepois: string | null;
  equivalenciaAntes: string | null;
  equivalenciaDepois: string | null;
}
export interface ExpedicaoDto_Detalhe {
  pedido: PedidoSaidaDto_Detalhe | null;
  separacoes: Array<ExpedicaoDto_Separacao> | null;
  documentos: Array<ExpedicaoDto_DocumentoRegistrado> | null;
  retiradaEm: string | null;
  baixas: Array<ExpedicaoDto_Baixa> | null;
  devolucoes: Array<ExpedicaoDto_Devolucao> | null;
  fatos: Array<ExpedicaoDto_Fato> | null;
}
export interface ExpedicaoDto_Confirmacao {
  operacaoId: string | null;
  expedicao: ExpedicaoDto_Detalhe | null;
  pedidoEntradaId: string | null;
}
export interface FatoServicoDto_Cota {
  notaId: string;
  cota: string;
}
export interface FatoServicoDto_Registrar {
  operacaoId: string;
  clienteId: string;
  armazemId: string;
  servicoId: string;
  origem: string;
  unidadeId: string | null;
  pedidoEntradaId: string | null;
  pedidoSaidaId: string | null;
  produtoId: string | null;
  referenciaExecucao: string | null;
  executadoEm: string | null;
  quantidade: string | null;
  categoria: string;
  valorBase: string | null;
  cotas: Array<FatoServicoDto_Cota>;
  criterioRateio: string;
  motivo: string;
  resolucao?: FechamentoCobrancaDto_Resolucao | null;
}
export interface FatoServicoDto_Anular {
  operacaoId: string;
  versao: string;
  motivo: string;
  resolucao?: FechamentoCobrancaDto_Resolucao | null;
}
export interface FatoServicoDto_Fato {
  id: string | null;
  versao: string;
  clienteId: string | null;
  armazemId: string | null;
  servicoId: string | null;
  chaveFato: string | null;
  origem: string | null;
  unidadeId: string | null;
  pedidoEntradaId: string | null;
  pedidoSaidaId: string | null;
  produtoId: string | null;
  referenciaExecucao: string | null;
  executadoEm: string | null;
  quantidade: string | null;
  categoria: string | null;
  valorBase: string | null;
  criterioRateio: string | null;
  situacao: string | null;
  cotas: Array<FatoServicoDto_Cota> | null;
}
export interface FatoServicoDto_Sugestao {
  servicoId: string | null;
  unidadeId: string | null;
  pedidoSaidaId: string | null;
  produtoId: string | null;
  chaveFato: string | null;
  executadoEm: string | null;
  quantidade: string | null;
  categoria: string | null;
  pendencia: string | null;
}
export interface FatoServicoDto_Marco {
  operacaoId: string;
  fatoPermanenciaId: string;
  quantidadeAfetada: string;
  motivo: string;
}
export interface FatoServicoDto_MarcoResposta {
  id: string | null;
  avariaId: string | null;
  fatoPermanenciaId: string | null;
  quantidadeAfetada: string | null;
  quantidadeBase: string | null;
  equivalenciaBase: string | null;
  validadoEm: string | null;
  usuario: string | null;
  motivo: string | null;
}
export interface FechamentoCobrancaDto_Resolucao {
  tipo: string;
  compromissoId: string;
}
export interface FechamentoCobrancaDto_Preparar {
  operacaoId: string;
  calculoId: string;
  motivo: string;
  resolucao: FechamentoCobrancaDto_Resolucao | null;
}
export interface FechamentoCobrancaDto_Decidir {
  operacaoId: string;
  versao: string;
  numero: number;
  motivo: string;
  resolucao: FechamentoCobrancaDto_Resolucao | null;
}
export interface FechamentoCobrancaDto_Reabrir {
  operacaoId: string;
  versao: string;
  numero: number;
  calculoId: string;
  motivo: string;
  resolucao: FechamentoCobrancaDto_Resolucao | null;
}
export interface FechamentoCobrancaDto_Entregar {
  operacaoId: string;
  versao: string;
  numero: number;
  layoutVersao: number;
  arquivoHash: string;
  destinoReferencia: string;
  entregueEm: string;
  motivo: string;
  resolucao: FechamentoCobrancaDto_Resolucao | null;
}
export interface FechamentoCobrancaDto_Confirmar {
  operacaoId: string;
  versao: string;
  numero: number;
  entregaId: string | null;
  fonte: string;
  confirmadaPor: string;
  confirmadaEm: string;
  motivo: string;
  resolucao: FechamentoCobrancaDto_Resolucao | null;
}
export interface FechamentoCobrancaDto_Nfse {
  operacaoId: string;
  versao: string;
  numero: number;
  emissorDocumento: string;
  referenciaExterna: string;
  numeroDocumento: string | null;
  serie: string | null;
  emitidaEm: string;
  fonte: string;
  conferidaPor: string;
  motivo: string;
  resolucao: FechamentoCobrancaDto_Resolucao | null;
}
export interface FechamentoCobrancaDto_Resolver {
  operacaoId: string;
  versao: string;
  numero: number;
  referenciaExterna: string;
  fonte: string;
  confirmadaPor: string;
  confirmadaEm: string;
  motivo: string;
  resolucao: FechamentoCobrancaDto_Resolucao | null;
}
export interface FechamentoCobrancaDto_Ajustar {
  operacaoId: string;
  origemVersaoId: string;
  destinoFechamentoId: string;
  versaoDestino: string;
  calculoCorrigidoId: string;
  motivo: string;
  evidencia: string;
  resolucao: FechamentoCobrancaDto_Resolucao | null;
}
export interface FechamentoCobrancaDto_Fechamento {
  id: string | null;
  versao: string;
  clienteId: string | null;
  armazemId: string | null;
  contratoId: string | null;
  periodoInicio: string | null;
  periodoFim: string | null;
  situacao: string | null;
  versaoAtual: number;
  criadoEm: string | null;
  alteradoEm: string | null;
}
export interface FechamentoCobrancaDto_Versao {
  id: string | null;
  fechamentoId: string | null;
  numero: number;
  calculoId: string | null;
  situacao: string | null;
  estadoExterno: string | null;
  conteudoHash: string | null;
  saldo: string | null;
  natureza: string | null;
  criadaEm: string | null;
  decididaEm: string | null;
  decisor: string | null;
  motivoDecisao: string | null;
  entregas: Array<FechamentoCobrancaDto_Entrega> | null;
  confirmacoes: Array<FechamentoCobrancaDto_Confirmacao> | null;
  nfse: Array<FechamentoCobrancaDto_Documento> | null;
  resolucaoFinanceira: FechamentoCobrancaDto_ResolucaoSaldo | null;
  ajustes: Array<FechamentoCobrancaDto_Ajuste> | null;
}
export interface FechamentoCobrancaDto_DocumentoConferido {
  referenciaId: string;
  situacao: string;
}
export interface FechamentoCobrancaDto_Tratar {
  operacaoId: string;
  versao: string;
  numeroResultado: number;
  resultado: string;
  referencias: Array<FechamentoCobrancaDto_DocumentoConferido>;
  fonte: string;
  conferidaPor: string;
  conferidaEm: string;
  motivo: string;
  resolucao: FechamentoCobrancaDto_Resolucao | null;
  destinoAjustesId: string | null;
  versaoDestinoAjustes: string | null;
  regularizacaoOrigem?: FechamentoCobrancaDto_RegularizacaoOrigem | null;
}
export interface FechamentoCobrancaDto_RegularizacaoOrigem {
  ajustesDependentesIds: Array<string>;
  destinoFechamentoId: string;
  versaoDestino: string;
  diferencaEsperada: string;
}
export interface FechamentoCobrancaDto_Tratativa {
  id: string | null;
  fechamentoId: string | null;
  versaoResultadoId: string | null;
  resultado: string | null;
  referencias: Array<FechamentoCobrancaDto_DocumentoConferido> | null;
  fonte: string | null;
  conferidaPor: string | null;
  conferidaEm: string | null;
  registradaEm: string | null;
  usuario: string | null;
  motivo: string | null;
  versaoBaseAnteriorId: string | null;
  dependenciasOrigem: Array<FechamentoCobrancaDto_Ajuste> | null;
}
export interface FechamentoCobrancaDto_Entrega {
  id: string | null;
  sequencia: number;
  layoutVersao: number;
  arquivoHash: string | null;
  destinoReferencia: string | null;
  entregueEm: string | null;
  registradaEm: string | null;
  usuario: string | null;
  motivo: string | null;
}
export interface FechamentoCobrancaDto_Confirmacao {
  id: string | null;
  entregaId: string | null;
  situacao: string | null;
  fonte: string | null;
  confirmadaPor: string | null;
  confirmadaEm: string | null;
  registradaEm: string | null;
  usuario: string | null;
  motivo: string | null;
}
export interface FechamentoCobrancaDto_Documento {
  id: string | null;
  emissorDocumento: string | null;
  referenciaExterna: string | null;
  numero: string | null;
  serie: string | null;
  emitidaEm: string | null;
  registradaEm: string | null;
  fonte: string | null;
  conferidaPor: string | null;
  usuario: string | null;
  motivo: string | null;
}
export interface FechamentoCobrancaDto_ResolucaoSaldo {
  id: string | null;
  tipo: string | null;
  referenciaExterna: string | null;
  fonte: string | null;
  confirmadaPor: string | null;
  confirmadaEm: string | null;
  registradaEm: string | null;
  usuario: string | null;
  motivo: string | null;
}
export interface FechamentoCobrancaDto_Ajuste {
  id: string | null;
  origemVersaoId: string | null;
  destinoFechamentoId: string | null;
  calculoBaseId: string | null;
  calculoCorrigidoId: string | null;
  hashCorrecao: string | null;
  valorBase: string | null;
  valorCorrigido: string | null;
  diferenca: string | null;
  situacao: string | null;
  aplicadoVersaoId: string | null;
  aplicadoEm: string | null;
  motivo: string | null;
  evidencia: string | null;
  usuario: string | null;
  registradoEm: string | null;
  tipo?: string | null;
  tratativaOrigemId?: string | null;
}
export interface FechamentoCobrancaDto_ConfirmacaoComando {
  fechamento: FechamentoCobrancaDto_Fechamento | null;
  versao: FechamentoCobrancaDto_Versao | null;
}
export interface FechamentoCobrancaDto_Identificacao {
  id: string | null;
  codigo: string | null;
  nome: string | null;
  documentoFiscal: string | null;
  dados: FiscalCadastroDto_Dados | null;
}
export interface FechamentoCobrancaDto_Ciclo {
  modalidade: string | null;
  ancora: string | null;
  diaNominal: number | null;
  duracaoDias: number | null;
  cicloInicio: string | null;
  cicloFim: string | null;
  diasNominais: string;
}
export interface FechamentoCobrancaDto_Demonstrativo {
  layoutVersao: number;
  fechamentoId: string | null;
  numero: number;
  cliente: FechamentoCobrancaDto_Identificacao | null;
  armazem: FechamentoCobrancaDto_Identificacao | null;
  periodoInicio: string | null;
  periodoFim: string | null;
  ciclo: FechamentoCobrancaDto_Ciclo | null;
  calculo: CalculoCobrancaDto_Resultado | null;
  ajustes: Array<FechamentoCobrancaDto_Ajuste> | null;
  saldo: string | null;
  natureza: string | null;
}
export interface FiscalCadastroDto_Dados {
  razaoSocial: string | null;
  inscricaoEstadual: string | null;
  logradouro: string | null;
  numeroEndereco: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  cep: string | null;
  pais: string | null;
  contatoNome: string | null;
  contatoEmail: string | null;
  contatoTelefone: string | null;
  faturamentoEmail: string | null;
  faturamentoReferencia: string | null;
}
export interface FiscalCadastroDto_Complementar {
  operacaoId: string;
  versao: string;
  dados: FiscalCadastroDto_Dados;
  motivo: string;
}
export interface FiscalCadastroDto_Complemento {
  id: string | null;
  versao: string;
  codigo: string | null;
  documentoFiscal: string | null;
  dados: FiscalCadastroDto_Dados | null;
}
export interface FiscalCadastroDto_Referenciar {
  operacaoId: string;
  armazemId: string;
  operacao: string;
  versao: string;
  ncm: string | null;
  cfop: string | null;
  cest: string | null;
  enquadramento: string | null;
  aliquotaIcms: string | null;
  aliquotaIpi: string | null;
  fonte: string;
  conferidaPor: string | null;
  conferidaEm: string | null;
  motivo: string;
}
export interface FiscalCadastroDto_Referencia {
  id: string | null;
  versao: string;
  produtoId: string | null;
  armazemId: string | null;
  operacao: string | null;
  ncm: string | null;
  cfop: string | null;
  cest: string | null;
  enquadramento: string | null;
  aliquotaIcms: string | null;
  aliquotaIpi: string | null;
  fonte: string | null;
  conferidaPor: string | null;
  conferidaEm: string | null;
}
export interface ImportacaoEnderecoDto_Previa {
  operacaoId: string;
  motivo: string;
}
export interface ImportacaoEnderecoDto_Confirmar {
  operacaoId: string;
  versao: string;
  arquivoHash: string;
  motivo: string;
}
export interface ImportacaoEnderecoDto_Linha {
  linha: number;
  endereco: EnderecoDto_Criar;
  tipoUnidadePermitido: TipoUnidadeLogistica | null;
}
export interface ImportacaoEnderecoDto_Erro {
  linha: number;
  coluna: string | null;
  codigo: string | null;
  mensagem: string | null;
}
export interface ImportacaoEnderecoDto_Resultado {
  id: string | null;
  versao: string;
  armazemId: string | null;
  arquivoHash: string | null;
  layoutVersao: number;
  situacao: string | null;
  quantidadeLinhas: number;
  linhas: Array<ImportacaoEnderecoDto_Linha> | null;
  erros: Array<ImportacaoEnderecoDto_Erro> | null;
  enderecos: Array<EnderecoDto_Resposta> | null;
  criadaEm: string | null;
  confirmadaEm: string | null;
}
export interface IndicadorEstoqueDto_ConfigurarAviso {
  operacaoId: string;
  clienteId: string;
  armazemId: string;
  versao: string;
  diasAntecedencia: number;
  motivo: string;
}
export interface IndicadorEstoqueDto_Configuracao {
  id: string | null;
  versao: string;
  clienteId: string | null;
  armazemId: string | null;
  diasAntecedencia: number | null;
  configurada: boolean;
}
export interface IndicadorEstoqueDto_Aviso {
  unidadeId: string | null;
  codigo: string | null;
  validade: string | null;
  situacao: string | null;
}
export interface IndicadorEstoqueDto_ValorOrigem {
  entradaId: string | null;
  notaId: string | null;
  itemNotaId: string | null;
  quantidade: string | null;
  valorConhecido: string | null;
  completo: boolean;
  unidades: Array<string> | null;
}
export interface IndicadorEstoqueDto_Resultado {
  produtoId: string | null;
  sku: string | null;
  saldo: EstoqueDto_Saldo | null;
  quantidadeEmEstagio: string | null;
  quantidadeConferenciaPendente: string | null;
  observadoContagem: string | null;
  diferencaContagem: string | null;
  valorConsultado: boolean;
  valorConhecido: string | null;
  valorExato: string | null;
  origens: Array<IndicadorEstoqueDto_ValorOrigem> | null;
  pendencias: Array<string> | null;
  avisos: Array<IndicadorEstoqueDto_Aviso> | null;
}
export interface NfeEntradaDto {
  emitente: string | null;
  serie: number;
  numero: string;
  emissao: string | null;
  chaveAcesso: string | null;
  itens: Array<NfeEntradaDto_Item> | null;
}
export interface NfeEntradaDto_Item {
  numeroItem: number;
  sku: string | null;
  unidade: string | null;
  quantidade: string | null;
  valorMercadoria: string | null;
}
export interface PaginaResponse<T> {
  itens: Array<T> | null;
  pagina: number;
  tamanho: number;
  totalItens: string;
  totalPaginas: number;
}
export interface PedidoEntradaDto_Criar {
  clienteId: string;
  armazemId: string;
  referencia: string;
}
export interface PedidoEntradaDto_ItemNota {
  numeroItem: number;
  produtoId: string;
  quantidadePrevista: string;
  valorMercadoria: string | null;
}
export interface PedidoEntradaDto_NotaManual {
  versao: string;
  serie: number;
  numero: string;
  emissao: string;
  chaveAcesso: string | null;
  itens: Array<PedidoEntradaDto_ItemNota>;
}
export interface PedidoEntradaDto_ImportarXml {
  versao: string;
  xml: string;
}
export interface PedidoEntradaDto_Efetivar {
  versao: string;
  aceitarDivergencias: boolean;
  motivo: string;
}
export interface PedidoEntradaDto_Resumo {
  id: string | null;
  versao: string;
  clienteId: string | null;
  armazemId: string | null;
  referencia: string | null;
  situacao: SituacaoPedidoEntrada | null;
  criadoEm: string | null;
  alteradoEm: string | null;
  efetivadoEm: string | null;
  motivoConclusao: string | null;
}
export interface PedidoEntradaDto_ItemConferencia {
  id: string | null;
  numeroItem: number;
  produtoId: string | null;
  sku: string | null;
  prevista: string | null;
  recebidaBoa: string | null;
  recebidaAvariada: string | null;
  diferenca: string | null;
  valorMercadoria: string | null;
}
export interface PedidoEntradaDto_Nota {
  id: string | null;
  emitente: string | null;
  serie: number;
  numero: string;
  emissao: string | null;
  chaveAcesso: string | null;
  xmlVinculado: boolean;
  primeiraChegada: string | null;
  itens: Array<PedidoEntradaDto_ItemConferencia> | null;
}
export interface PedidoEntradaDto_Detalhe {
  pedido: PedidoEntradaDto_Resumo | null;
  divergente: boolean;
  notas: Array<PedidoEntradaDto_Nota> | null;
}
export interface PedidoSaidaDto_ItemCriar {
  produtoId: string;
  quantidade: string;
}
export interface PedidoSaidaDto_Criar {
  operacaoId: string;
  clienteId: string;
  armazemId: string;
  referencia: string;
  itens: Array<PedidoSaidaDto_ItemCriar>;
  motivo: string;
}
export interface PedidoSaidaDto_ImportarXml {
  operacaoId: string;
  clienteId: string;
  armazemId: string;
  xml: string;
  motivo: string;
}
export interface PedidoSaidaDto_ConfirmacaoXml {
  operacaoId: string | null;
  xmlHash: string | null;
  documento: NfeEntradaDto | null;
  pedido: PedidoSaidaDto_Detalhe | null;
}
export interface PedidoSaidaDto_Selecao {
  unidadeId: string;
  quantidade: string;
}
export interface PedidoSaidaDto_Justificar {
  operacaoId: string;
  versao: string;
  selecoes: Array<PedidoSaidaDto_Selecao>;
  motivo: string;
}
export interface PedidoSaidaDto_Reservar {
  operacaoId: string;
  versao: string;
  justificativaId: string | null;
  motivo: string;
}
export interface PedidoSaidaDto_Comando {
  operacaoId: string;
  versao: string;
  motivo: string;
}
export interface PedidoSaidaDto_Item {
  id: string | null;
  produtoId: string | null;
  sku: string | null;
  unidadeMedida: string | null;
  quantidade: string | null;
}
export interface PedidoSaidaDto_Reserva {
  id: string | null;
  itemId: string | null;
  unidadeId: string | null;
  codigoUnidade: string | null;
  notaOrigemId: string | null;
  lote: string | null;
  dataFifo: string | null;
  quantidade: string | null;
  situacao: SituacaoReservaSaida | null;
  criadaEm: string | null;
  encerradaEm: string | null;
}
export interface PedidoSaidaDto_Detalhe {
  id: string | null;
  versao: string;
  clienteId: string | null;
  armazemId: string | null;
  referencia: string | null;
  situacao: SituacaoPedidoSaida | null;
  criadoEm: string | null;
  alteradoEm: string | null;
  itens: Array<PedidoSaidaDto_Item> | null;
  reservas: Array<PedidoSaidaDto_Reserva> | null;
  unidadesImpedidas: Array<string> | null;
  podeProsseguir: boolean;
}
export interface PedidoSaidaDto_Confirmacao {
  operacaoId: string | null;
  pedido: PedidoSaidaDto_Detalhe | null;
  selecoes: Array<PedidoSaidaDto_Selecao> | null;
  excecaoFifo: boolean;
  justificadaPor: string | null;
  justificativa: string | null;
}
export interface PedidoSaidaDto_Sugestao {
  versao: string;
  selecoes: Array<PedidoSaidaDto_Selecao> | null;
}
export interface ProdutoDto_Criar {
  clienteId: string;
  sku: string;
  descricao: string;
  unidadeMedida: string;
  tipoQuantidade: TipoQuantidade;
  precisaoQuantidade: number;
  controlaLote: boolean;
  controlaValidade: boolean;
  antecedenciaAvisoDias: number | null;
}
export interface ProdutoDto_Alterar {
  versao: string;
  descricao: string;
  motivo: string;
}
export interface ProdutoDto_Resposta {
  id: string | null;
  versao: string;
  situacao: SituacaoCadastro | null;
  criadoEm: string | null;
  alteradoEm: string | null;
  clienteId: string | null;
  sku: string | null;
  descricao: string | null;
  unidadeMedida: string | null;
  tipoQuantidade: TipoQuantidade | null;
  precisaoQuantidade: number;
  controlaLote: boolean;
  controlaValidade: boolean;
  antecedenciaAvisoDias: number | null;
}
export interface RecebimentoDto_Item {
  itemNotaId: string;
  lote: string | null;
  validade: string | null;
  quantidadeBoa: string;
  quantidadeAvariada: string;
}
export interface RecebimentoDto_RegistrarChegada {
  versao: string;
  operacaoId: string;
  chegouEm: string;
  observacao: string;
  itens: Array<RecebimentoDto_Item>;
}
export interface RecebimentoDto_ItemFisico {
  id: string | null;
  itemNotaId: string | null;
  lote: string | null;
  validade: string | null;
  quantidadeBoa: string | null;
  quantidadeAvariada: string | null;
}
export interface RecebimentoDto_Chegada {
  id: string | null;
  operacaoId: string | null;
  chegouEm: string | null;
  registradaEm: string | null;
  usuario: string | null;
  observacao: string | null;
  estornadaEm: string | null;
  estornadaPor: string | null;
  motivoEstorno: string | null;
  itens: Array<RecebimentoDto_ItemFisico> | null;
}
export interface RecebimentoDto_Entrada {
  id: string | null;
  itemChegadaId: string | null;
  notaId: string | null;
  itemNotaId: string | null;
  produtoId: string | null;
  lote: string | null;
  validade: string | null;
  chegadaReal: string | null;
  dataFifo: string | null;
  efetivadaEm: string | null;
  quantidadeTriagem: string | null;
  quantidadeQuarentena: string | null;
  unitizadaEm: string | null;
  disponivelParaSaida: boolean;
}
export interface RevisaoCadastroRequest {
  versao: string;
  motivo: string;
}
export interface StatusResponse {
  aplicacao: string | null;
  status: string | null;
  instante: string | null;
}
export interface UnidadeLogisticaDto_NovaUnidade {
  embalagemId: string;
  tipo: TipoUnidadeLogistica;
  condicao: CondicaoMercadoria;
  quantidade: string;
}
export interface UnidadeLogisticaDto_Unitizar {
  operacaoId: string;
  versaoPedido: string;
  motivo: string;
  unidades: Array<UnidadeLogisticaDto_NovaUnidade>;
}
export interface UnidadeLogisticaDto_Dividir {
  operacaoId: string;
  versao: string;
  quantidadeNovaUnidade: string;
  motivo: string;
}
export interface UnidadeLogisticaDto_RevisaoUnidade {
  unidadeId: string;
  versao: string;
}
export interface UnidadeLogisticaDto_Reagrupar {
  operacaoId: string;
  versaoDestino: string;
  origens: Array<UnidadeLogisticaDto_RevisaoUnidade>;
  motivo: string;
}
export interface UnidadeLogisticaDto_Resumo {
  id: string | null;
  versao: string;
  codigo: string | null;
  pedidoId: string | null;
  clienteId: string | null;
  armazemId: string | null;
  notaId: string | null;
  serieNota: number;
  numeroNota: string;
  produtoId: string | null;
  sku: string | null;
  unidadeMedida: string | null;
  controlaLote: boolean;
  lote: string | null;
  validade: string | null;
  embalagemId: string | null;
  codigoDun: string | null;
  quantidadeProdutoPorDun: string | null;
  tipo: TipoUnidadeLogistica | null;
  condicao: CondicaoMercadoria | null;
  quantidade: string | null;
  dataFifo: string | null;
  chegadaReal: string | null;
  criadaEm: string | null;
  ativa: boolean;
  disponivelParaSaida: boolean;
}
export interface UnidadeLogisticaDto_Origem {
  entradaId: string | null;
  itemChegadaId: string | null;
  itemNotaId: string | null;
  quantidadeAtual: string | null;
}
export interface UnidadeLogisticaDto_Detalhe {
  unidade: UnidadeLogisticaDto_Resumo | null;
  origens: Array<UnidadeLogisticaDto_Origem> | null;
}
export interface UnidadeLogisticaDto_Resultado {
  operacaoId: string | null;
  pedidoId: string | null;
  versaoPedido: string;
  unidades: Array<UnidadeLogisticaDto_Detalhe> | null;
}
export interface UnidadeLogisticaDto_Progresso {
  pedidoId: string | null;
  entradasConferidas: string;
  entradasUnitizadas: string;
  entradasPendentes: string;
  concluida: boolean;
}
export interface UnidadeLogisticaDto_Etiqueta {
  codigoLeitura: string | null;
  versaoConteudo: string;
  sku: string | null;
  clienteId: string | null;
  armazemId: string | null;
  notaId: string | null;
  serieNota: number;
  numeroNota: string;
  dataEntrada: string | null;
  chegadaReal: string | null;
  controlaLote: boolean;
  lote: string | null;
  validade: string | null;
  tipo: TipoUnidadeLogistica | null;
  condicao: CondicaoMercadoria | null;
  quantidadeProduto: string | null;
  unidadeMedida: string | null;
  codigoDun: string | null;
  quantidadeProdutoPorDun: string | null;
}
export interface VisaoOperacaoDto_Resumo {
  clienteId: string | null;
  armazemId: string | null;
  consultadoEm: string;
  fuso: string;
  capacidade: string | null;
  posicoesOcupadas: string | null;
  posicoesLivres: string | null;
  ocupacao: string | null;
  unidadesArmazenadas: string;
  valorArmazenado: string | null;
  valorCompleto: boolean;
  emQuarentena: string;
  reservasAtivas: string;
  entradasAbertas: string;
  saidasAbertas: string;
  faturamentoMes: string | null;
  faturamentoParcial: boolean;
  financeiroPermitido: boolean;
  competencia: string;
  mapa: PaginaResponse<VisaoOperacaoDto_Posicao>;
}
export interface VisaoOperacaoDto_Posicao {
  id: string;
  armazemId: string;
  armazem: string;
  codigo: string;
  rua: string;
  nivel: number;
  posicao: string;
  tipo: TipoEndereco;
  situacao: SituacaoCadastro;
  estado: string;
  disponivel: boolean;
  ocupada: boolean;
  bloqueada: boolean;
  reservada: boolean;
  quarentena: boolean;
  capacidadePesoKg: string | null;
}
export interface VisaoOperacaoDto_Unidade {
  id: string;
  clienteId: string;
  codigo: string;
  produto: string;
  sku: string;
  lote: string | null;
  quantidade: string;
  unidadeMedida: string;
  pedidoEntradaId: string;
  bloqueada: boolean;
  reservada: boolean;
  quarentena: boolean;
  ultimaMovimentacao: string | null;
}
export interface VisaoOperacaoDto_Detalhe {
  endereco: VisaoOperacaoDto_Posicao;
  unidades: Array<VisaoOperacaoDto_Unidade>;
  unidadesVisiveis: string;
  conteudoRestrito: boolean;
  alturaMetros: string | null;
  larguraMetros: string | null;
  profundidadeMetros: string | null;
  empilhamentoMaximo: number | null;
  tipoUnidadePermitido: TipoUnidadeLogistica | null;
}
export interface ApiContracts {
  "AjusteFechamentoController.ajustar": { request: FechamentoCobrancaDto_Ajustar; response: FechamentoCobrancaDto_Ajuste };
  "AjusteFechamentoController.listar": { request: undefined; response: Array<FechamentoCobrancaDto_Ajuste> };
  "ArmazemController.listar": { request: undefined; response: PaginaResponse<ArmazemDto_Resposta> };
  "ArmazemController.consultar": { request: undefined; response: ArmazemDto_Resposta };
  "ArmazemController.criar": { request: ArmazemDto_Criar; response: ArmazemDto_Resposta };
  "ArmazemController.alterar": { request: ArmazemDto_Alterar; response: ArmazemDto_Resposta };
  "ArmazemController.encerrar": { request: RevisaoCadastroRequest; response: ArmazemDto_Resposta };
  "ArmazemController.reativar": { request: RevisaoCadastroRequest; response: ArmazemDto_Resposta };
  "AuditoriaController.listar": { request: undefined; response: PaginaResponse<AuditoriaResponse> };
  "AvariaController.listar": { request: undefined; response: Array<AvariaDto_Ocorrencia> };
  "AvariaController.registrar": { request: AvariaDto_Registrar; response: AvariaDto_Confirmacao };
  "AvariaController.reconhecer": { request: AvariaDto_Reconhecer; response: AvariaDto_Confirmacao };
  "AvariaController.reparar": { request: AvariaDto_Reparar; response: AvariaDto_Confirmacao };
  "CalculoCobrancaController.listar": { request: undefined; response: PaginaResponse<CalculoCobrancaDto_Resultado> };
  "CalculoCobrancaController.consultar": { request: undefined; response: CalculoCobrancaDto_Resultado };
  "CalculoCobrancaController.calcular": { request: CalculoCobrancaDto_Calcular; response: CalculoCobrancaDto_Resultado };
  "CapacidadeController.configurar": { request: CapacidadeDto_ConfigurarEndereco; response: EnderecoDto_Resposta };
  "CapacidadeController.criar": { request: CapacidadeDto_CriarConjunto; response: CapacidadeDto_Conjunto };
  "CapacidadeController.listar": { request: undefined; response: PaginaResponse<CapacidadeDto_Conjunto> };
  "CapacidadeController.encerrar": { request: RevisaoCadastroRequest; response: CapacidadeDto_Conjunto };
  "CargaInicialController.criar": { request: CargaInicialDto_Criar; response: CargaInicialDto_Resultado };
  "CargaInicialController.consultar": { request: undefined; response: CargaInicialDto_Resultado };
  "CargaInicialController.listar": { request: undefined; response: PaginaResponse<CargaInicialDto_Resultado> };
  "CargaInicialController.revisoes": { request: undefined; response: PaginaResponse<CargaInicialDto_Revisao> };
  "CargaInicialController.revisar": { request: CargaInicialDto_Revisar; response: CargaInicialDto_Resultado };
  "CargaInicialController.preparar": { request: CargaInicialDto_Confirmar; response: CargaInicialDto_Resultado };
  "CargaInicialController.confirmar": { request: CargaInicialDto_Confirmar; response: CargaInicialDto_Resultado };
  "CargaInicialController.cancelar": { request: CargaInicialDto_Cancelar; response: CargaInicialDto_Resultado };
  "CargaInicialController.resolverCancelamento": { request: CargaInicialDto_ResolverCancelamento; response: CargaInicialDto_Resultado };
  "ClienteController.listar": { request: undefined; response: PaginaResponse<ClienteDto_Resposta> };
  "ClienteController.consultar": { request: undefined; response: ClienteDto_Resposta };
  "ClienteController.criar": { request: ClienteDto_Criar; response: ClienteDto_Resposta };
  "ClienteController.alterar": { request: ClienteDto_Alterar; response: ClienteDto_Resposta };
  "ClienteController.encerrar": { request: RevisaoCadastroRequest; response: ClienteDto_Resposta };
  "ClienteController.reativar": { request: RevisaoCadastroRequest; response: ClienteDto_Resposta };
  "ConfiguracaoCobrancaController.servicos": { request: undefined; response: PaginaResponse<ConfiguracaoCobrancaDto_Servico> };
  "ConfiguracaoCobrancaController.criarServico": { request: ConfiguracaoCobrancaDto_CriarServico; response: ConfiguracaoCobrancaDto_Servico };
  "ConfiguracaoCobrancaController.tabelas": { request: undefined; response: Array<ConfiguracaoCobrancaDto_Tabela> };
  "ConfiguracaoCobrancaController.tabela": { request: undefined; response: ConfiguracaoCobrancaDto_Tabela };
  "ConfiguracaoCobrancaController.criarTabela": { request: ConfiguracaoCobrancaDto_CriarTabela; response: ConfiguracaoCobrancaDto_Tabela };
  "ConfiguracaoCobrancaController.encerrarTabela": { request: ConfiguracaoCobrancaDto_Encerrar; response: ConfiguracaoCobrancaDto_Tabela };
  "ConfiguracaoCobrancaController.vinculos": { request: undefined; response: Array<ConfiguracaoCobrancaDto_Vinculo> };
  "ConfiguracaoCobrancaController.vincular": { request: ConfiguracaoCobrancaDto_Vincular; response: ConfiguracaoCobrancaDto_Vinculo };
  "ConfiguracaoCobrancaController.encerrarVinculo": { request: ConfiguracaoCobrancaDto_Encerrar; response: ConfiguracaoCobrancaDto_Vinculo };
  "ConfiguracaoCobrancaController.contratos": { request: undefined; response: Array<ConfiguracaoCobrancaDto_Contrato> };
  "ConfiguracaoCobrancaController.configurar": { request: ConfiguracaoCobrancaDto_ConfigurarContrato; response: ConfiguracaoCobrancaDto_Contrato };
  "ConfiguracaoCobrancaController.encerrarContrato": { request: ConfiguracaoCobrancaDto_Encerrar; response: ConfiguracaoCobrancaDto_Contrato };
  "ContagemController.contar": { request: ContagemDto_Contar; response: ContagemDto_Resultado };
  "ContagemController.consultar": { request: undefined; response: ContagemDto_Resultado };
  "ContagemController.listar": { request: undefined; response: PaginaResponse<ContagemDto_Resultado> };
  "ContagemController.revisoes": { request: undefined; response: PaginaResponse<ContagemDto_Resultado> };
  "ContagemController.aplicar": { request: ContagemDto_Aplicar; response: ContagemDto_Resultado };
  "ContingenciaController.registrar": { request: ContingenciaDto_Registrar; response: ContingenciaDto_Resultado };
  "ContingenciaController.conciliar": { request: ContingenciaDto_Conciliar; response: ContingenciaDto_Resultado };
  "ContingenciaController.consultar": { request: undefined; response: ContingenciaDto_Resultado };
  "ContingenciaController.listar": { request: undefined; response: PaginaResponse<ContingenciaDto_Resultado> };
  "DashboardController.consultar": { request: undefined; response: DashboardDto_Resumo };
  "EmbalagemController.listar": { request: undefined; response: PaginaResponse<EmbalagemDto_Resposta> };
  "EmbalagemController.consultar": { request: undefined; response: EmbalagemDto_Resposta };
  "EmbalagemController.criar": { request: EmbalagemDto_Criar; response: EmbalagemDto_Resposta };
  "EmbalagemController.alterar": { request: EmbalagemDto_Alterar; response: EmbalagemDto_Resposta };
  "EmbalagemController.encerrar": { request: RevisaoCadastroRequest; response: EmbalagemDto_Resposta };
  "EmbalagemController.reativar": { request: RevisaoCadastroRequest; response: EmbalagemDto_Resposta };
  "EncerramentoController.consultar": { request: undefined; response: EncerramentoDto_Resultado };
  "EncerramentoController.solicitar": { request: EncerramentoDto_Confirmar; response: EncerramentoDto_Resultado };
  "EncerramentoController.inativar": { request: EncerramentoDto_Confirmar; response: EncerramentoDto_Resultado };
  "EncerramentoController.remanescente": { request: EncerramentoDto_Remanescente; response: PedidoSaidaDto_Confirmacao };
  "EncerramentoController.vigencia": { request: EncerramentoDto_EncerrarVigencia; response: EncerramentoDto_Vigencia };
  "EnderecoController.listar": { request: undefined; response: PaginaResponse<EnderecoDto_Resposta> };
  "EnderecoController.consultar": { request: undefined; response: EnderecoDto_Resposta };
  "EnderecoController.criar": { request: EnderecoDto_Criar; response: EnderecoDto_Resposta };
  "EnderecoController.alterar": { request: EnderecoDto_Alterar; response: EnderecoDto_Resposta };
  "EnderecoController.encerrar": { request: RevisaoCadastroRequest; response: EnderecoDto_Resposta };
  "EnderecoController.reativar": { request: RevisaoCadastroRequest; response: EnderecoDto_Resposta };
  "EstoqueController.listar": { request: undefined; response: PaginaResponse<EstoqueDto_Unidade> };
  "EstoqueController.saldo": { request: undefined; response: EstoqueDto_Saldo };
  "EstoqueController.consultar": { request: undefined; response: EstoqueDto_Unidade };
  "EstoqueController.historico": { request: undefined; response: PaginaResponse<EstoqueDto_Movimento> };
  "EstoqueController.posicionar": { request: EstoqueDto_Posicionar; response: EstoqueDto_Confirmacao };
  "EstoqueController.bloquear": { request: EstoqueDto_Bloqueio; response: EstoqueDto_Confirmacao };
  "EstoqueController.liberar": { request: EstoqueDto_Bloqueio; response: EstoqueDto_Confirmacao };
  "EstoqueController.avariar": { request: EstoqueDto_Bloqueio; response: EstoqueDto_Confirmacao };
  "ExpedicaoController.consultar": { request: undefined; response: ExpedicaoDto_Detalhe };
  "ExpedicaoController.fatos": { request: undefined; response: Array<ExpedicaoDto_Fato> };
  "ExpedicaoController.ler": { request: ExpedicaoDto_Leitura; response: ExpedicaoDto_Confirmacao };
  "ExpedicaoController.separar": { request: ExpedicaoDto_Separar; response: ExpedicaoDto_Confirmacao };
  "ExpedicaoController.documento": { request: ExpedicaoDto_Documento; response: ExpedicaoDto_Confirmacao };
  "ExpedicaoController.cancelar": { request: ExpedicaoDto_CancelarDocumento; response: ExpedicaoDto_Confirmacao };
  "ExpedicaoController.retirar": { request: ExpedicaoDto_Retirar; response: ExpedicaoDto_Confirmacao };
  "ExpedicaoController.retornar": { request: ExpedicaoDto_Retornar; response: ExpedicaoDto_Confirmacao };
  "ExpedicaoController.devolver": { request: ExpedicaoDto_Devolver; response: ExpedicaoDto_Confirmacao };
  "FatoServicoController.listar": { request: undefined; response: PaginaResponse<FatoServicoDto_Fato> };
  "FatoServicoController.consultar": { request: undefined; response: FatoServicoDto_Fato };
  "FatoServicoController.sugestoes": { request: undefined; response: PaginaResponse<FatoServicoDto_Sugestao> };
  "FatoServicoController.registrar": { request: FatoServicoDto_Registrar; response: FatoServicoDto_Fato };
  "FatoServicoController.anular": { request: FatoServicoDto_Anular; response: FatoServicoDto_Fato };
  "FatoServicoController.marcos": { request: undefined; response: Array<FatoServicoDto_MarcoResposta> };
  "FatoServicoController.marco": { request: FatoServicoDto_Marco; response: FatoServicoDto_MarcoResposta };
  "FechamentoCobrancaController.listar": { request: undefined; response: PaginaResponse<FechamentoCobrancaDto_Fechamento> };
  "FechamentoCobrancaController.consultar": { request: undefined; response: FechamentoCobrancaDto_Fechamento };
  "FechamentoCobrancaController.versoes": { request: undefined; response: Array<FechamentoCobrancaDto_Versao> };
  "FechamentoCobrancaController.versao": { request: undefined; response: FechamentoCobrancaDto_Versao };
  "FechamentoCobrancaController.demonstrativo": { request: undefined; response: FechamentoCobrancaDto_Demonstrativo };
  "FechamentoCobrancaController.tratativas": { request: undefined; response: Array<FechamentoCobrancaDto_Tratativa> };
  "FechamentoCobrancaController.tratar": { request: FechamentoCobrancaDto_Tratar; response: FechamentoCobrancaDto_ConfirmacaoComando };
  "FechamentoCobrancaController.preparar": { request: FechamentoCobrancaDto_Preparar; response: FechamentoCobrancaDto_ConfirmacaoComando };
  "FechamentoCobrancaController.aprovar": { request: FechamentoCobrancaDto_Decidir; response: FechamentoCobrancaDto_ConfirmacaoComando };
  "FechamentoCobrancaController.rejeitar": { request: FechamentoCobrancaDto_Decidir; response: FechamentoCobrancaDto_ConfirmacaoComando };
  "FechamentoCobrancaController.reabrir": { request: FechamentoCobrancaDto_Reabrir; response: FechamentoCobrancaDto_ConfirmacaoComando };
  "FechamentoCobrancaController.entregar": { request: FechamentoCobrancaDto_Entregar; response: FechamentoCobrancaDto_ConfirmacaoComando };
  "FechamentoCobrancaController.confirmarNaoEmissao": { request: FechamentoCobrancaDto_Confirmar; response: FechamentoCobrancaDto_ConfirmacaoComando };
  "FechamentoCobrancaController.registrarNfse": { request: FechamentoCobrancaDto_Nfse; response: FechamentoCobrancaDto_ConfirmacaoComando };
  "FechamentoCobrancaController.resolverSaldo": { request: FechamentoCobrancaDto_Resolver; response: FechamentoCobrancaDto_ConfirmacaoComando };
  "FiscalCadastroController.cliente": { request: undefined; response: FiscalCadastroDto_Complemento };
  "FiscalCadastroController.armazem": { request: undefined; response: FiscalCadastroDto_Complemento };
  "FiscalCadastroController.complementarCliente": { request: FiscalCadastroDto_Complementar; response: FiscalCadastroDto_Complemento };
  "FiscalCadastroController.complementarArmazem": { request: FiscalCadastroDto_Complementar; response: FiscalCadastroDto_Complemento };
  "FiscalCadastroController.referencias": { request: undefined; response: Array<FiscalCadastroDto_Referencia> };
  "FiscalCadastroController.referenciar": { request: FiscalCadastroDto_Referenciar; response: FiscalCadastroDto_Referencia };
  "ImportacaoEnderecoController.previa": { request: ImportacaoEnderecoDto_Previa; response: ImportacaoEnderecoDto_Resultado };
  "ImportacaoEnderecoController.consultar": { request: undefined; response: ImportacaoEnderecoDto_Resultado };
  "ImportacaoEnderecoController.confirmar": { request: ImportacaoEnderecoDto_Confirmar; response: ImportacaoEnderecoDto_Resultado };
  "IndicadorEstoqueController.listar": { request: undefined; response: PaginaResponse<IndicadorEstoqueDto_Resultado> };
  "IndicadorEstoqueController.consultar": { request: undefined; response: IndicadorEstoqueDto_Configuracao };
  "IndicadorEstoqueController.configurar": { request: IndicadorEstoqueDto_ConfigurarAviso; response: IndicadorEstoqueDto_Configuracao };
  "LoginController.csrf": { request: undefined; response: Record<string, unknown> };
  "LoginController.jwks": { request: undefined; response: Record<string, unknown> };
  "LoginController.entrar": { request: AcessoDtos_Login; response: AcessoDtos_Tokens };
  "LoginController.renovar": { request: undefined; response: AcessoDtos_Tokens };
  "LoginController.sair": { request: undefined; response: void };
  "LoginController.senha": { request: AcessoDtos_TrocaSenha; response: void };
  "LoginController.eu": { request: undefined; response: AcessoDtos_Usuario };
  "LoginController.usuarios": { request: undefined; response: AcessoDtos_PaginaUsuarios };
  "LoginController.criar": { request: AcessoDtos_CriarUsuario; response: AcessoDtos_Usuario };
  "LoginController.editar": { request: AcessoDtos_EditarUsuario; response: AcessoDtos_Usuario };
  "LoginController.redefinir": { request: AcessoDtos_RedefinirSenha; response: void };
  "PedidoEntradaController.criar": { request: PedidoEntradaDto_Criar; response: PedidoEntradaDto_Resumo };
  "PedidoEntradaController.listar": { request: undefined; response: PaginaResponse<PedidoEntradaDto_Resumo> };
  "PedidoEntradaController.consultar": { request: undefined; response: PedidoEntradaDto_Detalhe };
  "PedidoEntradaController.nota": { request: PedidoEntradaDto_NotaManual; response: PedidoEntradaDto_Resumo };
  "PedidoEntradaController.xml": { request: PedidoEntradaDto_ImportarXml; response: PedidoEntradaDto_Resumo };
  "PedidoEntradaController.iniciar": { request: RevisaoCadastroRequest; response: PedidoEntradaDto_Resumo };
  "PedidoEntradaController.chegada": { request: RecebimentoDto_RegistrarChegada; response: PedidoEntradaDto_Resumo };
  "PedidoEntradaController.estornar": { request: RevisaoCadastroRequest; response: PedidoEntradaDto_Resumo };
  "PedidoEntradaController.efetivar": { request: PedidoEntradaDto_Efetivar; response: PedidoEntradaDto_Resumo };
  "PedidoEntradaController.cancelar": { request: RevisaoCadastroRequest; response: PedidoEntradaDto_Resumo };
  "PedidoEntradaController.chegadas": { request: undefined; response: PaginaResponse<RecebimentoDto_Chegada> };
  "PedidoEntradaController.entradas": { request: undefined; response: PaginaResponse<RecebimentoDto_Entrada> };
  "PedidoSaidaController.criar": { request: PedidoSaidaDto_Criar; response: PedidoSaidaDto_Confirmacao };
  "PedidoSaidaController.importarXml": { request: PedidoSaidaDto_ImportarXml; response: PedidoSaidaDto_ConfirmacaoXml };
  "PedidoSaidaController.listar": { request: undefined; response: PaginaResponse<PedidoSaidaDto_Detalhe> };
  "PedidoSaidaController.consultar": { request: undefined; response: PedidoSaidaDto_Detalhe };
  "PedidoSaidaController.sugerir": { request: undefined; response: PedidoSaidaDto_Sugestao };
  "PedidoSaidaController.justificar": { request: PedidoSaidaDto_Justificar; response: PedidoSaidaDto_Confirmacao };
  "PedidoSaidaController.reservar": { request: PedidoSaidaDto_Reservar; response: PedidoSaidaDto_Confirmacao };
  "PedidoSaidaController.cancelar": { request: PedidoSaidaDto_Comando; response: PedidoSaidaDto_Confirmacao };
  "PedidoSaidaController.reverter": { request: PedidoSaidaDto_Comando; response: PedidoSaidaDto_Confirmacao };
  "PedidoSaidaController.revalidar": { request: undefined; response: PedidoSaidaDto_Detalhe };
  "ProdutoController.listar": { request: undefined; response: PaginaResponse<ProdutoDto_Resposta> };
  "ProdutoController.consultar": { request: undefined; response: ProdutoDto_Resposta };
  "ProdutoController.criar": { request: ProdutoDto_Criar; response: ProdutoDto_Resposta };
  "ProdutoController.alterar": { request: ProdutoDto_Alterar; response: ProdutoDto_Resposta };
  "ProdutoController.encerrar": { request: RevisaoCadastroRequest; response: ProdutoDto_Resposta };
  "ProdutoController.reativar": { request: RevisaoCadastroRequest; response: ProdutoDto_Resposta };
  "StatusController.consultar": { request: undefined; response: StatusResponse };
  "UnidadeLogisticaController.unitizar": { request: UnidadeLogisticaDto_Unitizar; response: UnidadeLogisticaDto_Resultado };
  "UnidadeLogisticaController.dividir": { request: UnidadeLogisticaDto_Dividir; response: UnidadeLogisticaDto_Resultado };
  "UnidadeLogisticaController.reagrupar": { request: UnidadeLogisticaDto_Reagrupar; response: UnidadeLogisticaDto_Resultado };
  "UnidadeLogisticaController.progresso": { request: undefined; response: UnidadeLogisticaDto_Progresso };
  "UnidadeLogisticaController.listar": { request: undefined; response: PaginaResponse<UnidadeLogisticaDto_Resumo> };
  "UnidadeLogisticaController.consultar": { request: undefined; response: UnidadeLogisticaDto_Detalhe };
  "UnidadeLogisticaController.lerCodigo": { request: undefined; response: UnidadeLogisticaDto_Detalhe };
  "UnidadeLogisticaController.etiqueta": { request: undefined; response: UnidadeLogisticaDto_Etiqueta };
  "VisaoOperacaoController.consultar": { request: undefined; response: VisaoOperacaoDto_Resumo };
  "VisaoOperacaoController.detalhe": { request: undefined; response: VisaoOperacaoDto_Detalhe };
}
