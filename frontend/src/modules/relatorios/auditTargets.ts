import type { Values } from "../../contracts/runtime";
import type { Workflow } from "../../domain/workflow";
// Tipos aceitos por AuditoriaService.listar; unidade logística não é alvo dessa API.
export const auditTargets = [
    { dto: "ClienteDto.Resposta", tipo: "CLIENTE", label: "Cliente" },
    { dto: "ArmazemDto.Resposta", tipo: "ARMAZEM", label: "Armazém" },
    { dto: "ProdutoDto.Resposta", tipo: "PRODUTO", label: "Produto" },
    { dto: "EmbalagemDto.Resposta", tipo: "EMBALAGEM", label: "Embalagem" },
    { dto: "EnderecoDto.Resposta", tipo: "ENDERECO", label: "Endereço" },
    {
        dto: "PedidoEntradaDto.Resumo",
        tipo: "PEDIDO_ENTRADA",
        label: "Pedido de entrada",
    },
    {
        dto: "PedidoSaidaDto.Detalhe",
        tipo: "PEDIDO_SAIDA",
        label: "Pedido de saída",
    },
    {
        dto: "ConfiguracaoCobrancaDto.Servico",
        tipo: "SERVICO_COBRANCA",
        label: "Serviço",
    },
    {
        dto: "ConfiguracaoCobrancaDto.Tabela",
        tipo: "TABELA_COBRANCA",
        label: "Tabela",
    },
    {
        dto: "ConfiguracaoCobrancaDto.Contrato",
        tipo: "CONTRATO_COBRANCA",
        label: "Contrato",
    },
    {
        dto: "ConfiguracaoCobrancaDto.Vinculo",
        tipo: "VINCULO_COBRANCA",
        label: "Vínculo",
    },
    {
        dto: "FatoServicoDto.Fato",
        tipo: "FATO_SERVICO",
        label: "Fato de serviço",
    },
    {
        dto: "CalculoCobrancaDto.Resultado",
        tipo: "CALCULO_COBRANCA",
        label: "Cálculo",
    },
    {
        dto: "ContagemDto.Resultado",
        tipo: "CONTAGEM_ESTOQUE",
        label: "Contagem",
    },
    {
        dto: "CargaInicialDto.Resultado",
        tipo: "CARGA_INICIAL",
        label: "Carga inicial",
    },
    {
        dto: "ContingenciaDto.Resultado",
        tipo: "CONTINGENCIA",
        label: "Contingência",
    },
    {
        dto: "FechamentoCobrancaDto.Fechamento",
        tipo: "FECHAMENTO_COBRANCA",
        label: "Fechamento",
    },
];
export function applyAuditTarget(state: Workflow, value: Values): void {
    const target = auditTargets.find((t) => t.dto === value.dto);
    const selected = target && state.selected[target.dto];
    if (!selected || selected.id !== value.registroId) return;
    if (value.legado && target.tipo !== "VINCULO_COBRANCA") return;
    state.followUps["AuditoriaController.listar"] = {
        tipo: value.legado ? "VINCULO_COBRANCA_LEGADO" : target.tipo,
        registroId: selected.id,
    };
}
