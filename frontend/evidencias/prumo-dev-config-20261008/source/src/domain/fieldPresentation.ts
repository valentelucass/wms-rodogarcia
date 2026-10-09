import { canPresent, type Perfil } from "../contracts/runtime";
// Conditional historical resolution gates from current services; presentation only.
const policies: Record<string, Record<string, Perfil>> = {
    "PedidoSaidaDto.Reservar": {
        justificativaId: "SUPERVISOR",
    },
    "AvariaDto.Registrar": {
        resolverPendentes: "GESTOR",
    },
    "AvariaDto.Reparar": {
        resolverPendentes: "GESTOR",
    },
    "CalculoCobrancaDto.Calcular": {
        resolucao: "GESTOR",
    },
    "ConfiguracaoCobrancaDto.CriarTabela": {
        resolucao: "GESTOR",
    },
    "ConfiguracaoCobrancaDto.Vincular": {
        resolucao: "GESTOR",
    },
    "ConfiguracaoCobrancaDto.Encerrar": {
        resolucao: "GESTOR",
    },
    "ConfiguracaoCobrancaDto.ConfigurarContrato": {
        resolucao: "GESTOR",
    },
    "EncerramentoDto.EncerrarVigencia": {
        resolucao: "GESTOR",
    },
    "ExpedicaoDto.Leitura": {
        resolverPendentes: "GESTOR",
    },
    "ExpedicaoDto.Separar": {
        resolverPendentes: "GESTOR",
    },
    "ExpedicaoDto.Documento": {
        resolverPendentes: "GESTOR",
    },
    "ExpedicaoDto.Retirar": {
        resolverPendentes: "GESTOR",
    },
    "ExpedicaoDto.Retornar": {
        resolverPendentes: "GESTOR",
    },
    "ExpedicaoDto.Devolver": {
        resolverPendentes: "GESTOR",
    },
    "FatoServicoDto.Registrar": {
        resolucao: "GESTOR",
    },
    "FatoServicoDto.Anular": {
        resolucao: "GESTOR",
    },
    "FechamentoCobrancaDto.Preparar": {
        resolucao: "GESTOR",
    },
    "FechamentoCobrancaDto.Decidir": {
        resolucao: "GESTOR",
    },
    "FechamentoCobrancaDto.Reabrir": {
        resolucao: "GESTOR",
    },
    "FechamentoCobrancaDto.Entregar": {
        resolucao: "GESTOR",
    },
    "FechamentoCobrancaDto.Confirmar": {
        resolucao: "GESTOR",
    },
    "FechamentoCobrancaDto.Nfse": {
        resolucao: "GESTOR",
    },
    "FechamentoCobrancaDto.Resolver": {
        resolucao: "GESTOR",
    },
    "FechamentoCobrancaDto.Ajustar": {
        resolucao: "GESTOR",
    },
    "FechamentoCobrancaDto.Tratar": {
        resolucao: "GESTOR",
    },
    "IndicadorEstoqueController.listar.query": {
        valor: "SUPERVISOR",
    },
    "IndicadorEstoqueController.consultar.query": {
        valor: "SUPERVISOR",
    },
};
export function isFieldDisabled(schema: string, field: string, perfil: Perfil) {
    const minimum = policies[schema]?.[field];
    return minimum ? !canPresent(perfil, minimum) : false;
}
