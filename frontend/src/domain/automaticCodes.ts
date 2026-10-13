/** Códigos internos atribuídos pelo backend; códigos externos/físicos não entram aqui. */
export const automaticCodes: Record<string, string> = {
    "ClienteDto.Criar": "codigo",
    "ArmazemDto.Criar": "codigo",
    "CapacidadeDto.CriarConjunto": "codigo",
    "ConfiguracaoCobrancaDto.CriarServico": "codigo",
    "ConfiguracaoCobrancaDto.CriarTabela": "codigo",
    "PedidoEntradaDto.Criar": "referencia",
    "NfeImportacaoDto.Confirmar": "referencia",
    "PedidoSaidaDto.Criar": "referencia",
    "CargaInicialDto.Criar": "referencia",
    "CargaInicialDto.Dados": "referenciaPedido",
    "EncerramentoDto.Remanescente": "referencia",
    "ExpedicaoDto.Devolver": "referencia",
};
