import {
    endpoint,
    canPresent,
    type Perfil,
    type Values,
} from "../contracts/runtime";
import { actionLabel } from "./actionLabels";

export interface RecordPage {
    source: string;
    detail?: string;
    headers: string[];
    title?: string;
    actions?: string[];
    context?: Values;
    views?: RecordPage[];
    related?: string[];
    contextHeaders?: string[];
}
const page = (
    source: string,
    detail: string | undefined,
    headers: string[] = [],
): RecordPage => ({ source, detail, headers });
// Each entry is a reviewed domain page, in the existing order of its journey.
// Process stages deliberately share their order queue; their commands remain distinct.
export const recordPages: Record<string, RecordPage[]> = {
    cadastros: [
        ...["Cliente", "Armazem", "Produto", "Embalagem", "Endereco"].map(
            (name) =>
                page(
                    `${name}Controller.listar`,
                    `${name}Controller.consultar`,
                    [`${name}Controller.criar`],
                ),
        ),
        page("CapacidadeController.listar", undefined, [
            "CapacidadeController.criar",
            "CapacidadeController.configurar",
        ]),
        page(
            "ImportacaoEnderecoController.consultar",
            "ImportacaoEnderecoController.consultar",
            ["ImportacaoEnderecoController.previa"],
        ),
        {
            ...page(
                "ClienteController.listar",
                "FiscalCadastroController.cliente",
            ),
            views: [
                {
                    ...page(
                        "ClienteController.listar",
                        "FiscalCadastroController.cliente",
                    ),
                    title: "Clientes",
                    actions: ["FiscalCadastroController.complementarCliente"],
                },
                {
                    ...page(
                        "ArmazemController.listar",
                        "FiscalCadastroController.armazem",
                    ),
                    title: "Armazéns",
                    actions: ["FiscalCadastroController.complementarArmazem"],
                },
                {
                    ...page(
                        "ProdutoController.listar",
                        "ProdutoController.consultar",
                    ),
                    title: "Produtos",
                    actions: [
                        "FiscalCadastroController.referencias",
                        "FiscalCadastroController.referenciar",
                    ],
                },
            ],
        },
        {
            ...page(
                "ClienteController.listar",
                "EncerramentoController.consultar",
            ),
            views: [
                "Cliente",
                "Armazem",
                "Produto",
                "Embalagem",
                "Endereco",
            ].map((entity) => ({
                ...page(
                    `${entity}Controller.listar`,
                    "EncerramentoController.consultar",
                ),
                title: {
                    Cliente: "Clientes",
                    Armazem: "Armazéns",
                    Produto: "Produtos",
                    Embalagem: "Embalagens",
                    Endereco: "Endereços",
                }[entity],
                context: { tipo: entity.toUpperCase() },
            })),
        },
    ],
    entrada: [
        page(
            "PedidoEntradaController.listar",
            "PedidoEntradaController.consultar",
            ["PedidoEntradaController.criar"],
        ),
        page(
            "PedidoEntradaController.listar",
            "PedidoEntradaController.consultar",
        ),
        page(
            "PedidoEntradaController.listar",
            "PedidoEntradaController.consultar",
        ),
        page(
            "PedidoEntradaController.listar",
            "PedidoEntradaController.consultar",
        ),
    ],
    unidades: [
        {
            ...page(
                "PedidoEntradaController.listar",
                "PedidoEntradaController.consultar",
                ["UnidadeLogisticaController.unitizar"],
            ),
            contextHeaders: ["UnidadeLogisticaController.unitizar"],
            related: [
                "PedidoEntradaController.entradas",
                "UnidadeLogisticaController.progresso",
            ],
        },
        page(
            "EstoqueController.listar",
            "UnidadeLogisticaController.consultar",
        ),
        page(
            "EstoqueController.listar",
            "UnidadeLogisticaController.lerCodigo",
        ),
    ],
    estoque: [
        page("EstoqueController.listar", "EstoqueController.consultar"),
        page("EstoqueController.listar", "EstoqueController.consultar"),
        {
            ...page("EstoqueController.listar", "EstoqueController.consultar", [
                "AvariaController.registrar",
            ]),
            related: ["AvariaController.listar"],
        },
        page("IndicadorEstoqueController.listar", undefined, [
            "IndicadorEstoqueController.configurar",
        ]),
    ],
    saida: [
        page(
            "PedidoSaidaController.listar",
            "PedidoSaidaController.consultar",
            [
                "PedidoSaidaController.criar",
                "PedidoSaidaController.importarXml",
            ],
        ),
        page("PedidoSaidaController.listar", "PedidoSaidaController.consultar"),
        page("PedidoSaidaController.listar", "ExpedicaoController.consultar"),
        page("PedidoSaidaController.listar", "PedidoSaidaController.consultar"),
    ],
    fiscal: [
        page("PedidoSaidaController.listar", "ExpedicaoController.consultar"),
        page("PedidoSaidaController.listar", "ExpedicaoController.consultar"),
        page("PedidoSaidaController.listar", "ExpedicaoController.consultar"),
    ],
    precos: [
        page("ConfiguracaoCobrancaController.servicos", undefined, [
            "ConfiguracaoCobrancaController.criarServico",
        ]),
        page(
            "ConfiguracaoCobrancaController.tabelas",
            "ConfiguracaoCobrancaController.tabela",
            ["ConfiguracaoCobrancaController.criarTabela"],
        ),
        page("ConfiguracaoCobrancaController.vinculos", undefined, [
            "ConfiguracaoCobrancaController.vincular",
        ]),
        page("ConfiguracaoCobrancaController.contratos", undefined, [
            "ConfiguracaoCobrancaController.configurar",
        ]),
        page("AuditoriaController.listar", undefined),
    ],
    cobranca: [
        page(
            "FatoServicoController.listar",
            "FatoServicoController.consultar",
            ["FatoServicoController.registrar"],
        ),
        page("FatoServicoController.marcos", undefined, [
            "FatoServicoController.marco",
        ]),
        page(
            "CalculoCobrancaController.listar",
            "CalculoCobrancaController.consultar",
            ["CalculoCobrancaController.calcular"],
        ),
    ],
    fechamento: [
        page(
            "FechamentoCobrancaController.listar",
            "FechamentoCobrancaController.consultar",
            ["FechamentoCobrancaController.preparar"],
        ),
        page(
            "FechamentoCobrancaController.listar",
            "FechamentoCobrancaController.consultar",
        ),
        page(
            "FechamentoCobrancaController.listar",
            "FechamentoCobrancaController.consultar",
        ),
        page(
            "FechamentoCobrancaController.listar",
            "FechamentoCobrancaController.consultar",
        ),
        page("AjusteFechamentoController.listar", undefined, [
            "AjusteFechamentoController.ajustar",
        ]),
    ],
    contagem: [
        page("EstoqueController.listar", "EstoqueController.consultar"),
        page("ContagemController.listar", "ContagemController.consultar", [
            "ContagemController.contar",
        ]),
        page(
            "CargaInicialController.listar",
            "CargaInicialController.consultar",
            ["CargaInicialController.criar"],
        ),
    ],
    contingencia: [
        page(
            "ContingenciaController.listar",
            "ContingenciaController.consultar",
            ["ContingenciaController.registrar"],
        ),
    ],
    relatorios: [
        page("EstoqueController.listar", "EstoqueController.consultar"),
        page("IndicadorEstoqueController.listar", undefined),
        page(
            "PedidoEntradaController.listar",
            "PedidoEntradaController.consultar",
        ),
        page("PedidoSaidaController.listar", "PedidoSaidaController.consultar"),
        page(
            "CalculoCobrancaController.listar",
            "CalculoCobrancaController.consultar",
        ),
        page("AuditoriaController.listar", undefined),
    ],
};
const names: Record<string, string> = {
    Cliente: "cliente",
    Armazem: "armazém",
    Produto: "produto",
    Embalagem: "embalagem",
    Endereco: "endereço",
};
export function recordActionLabel(id: string): string {
    const [controller, command] = id.split(".");
    const entity = names[controller.replace("Controller", "")];
    if (entity) {
        if (command === "criar")
            return `Nov${entity === "embalagem" ? "a" : "o"} ${entity}`;
        if (command === "alterar") return "Editar";
        if (command === "encerrar") return `Solicitar desativação de ${entity}`;
        if (command === "reativar") return `Reativar ${entity}`;
    }
    const labels: Record<string, string> = {
        "PedidoEntradaController.criar": "Novo pedido de entrada",
        "PedidoSaidaController.criar": "Nova saída",
        "ContagemController.contar": "Nova contagem",
        "CargaInicialController.criar": "Nova carga inicial",
        "ContingenciaController.registrar": "Registrar ocorrência",
        "CalculoCobrancaController.calcular": "Calcular serviços",
        "FechamentoCobrancaController.preparar": "Preparar fechamento",
        "ConfiguracaoCobrancaController.criarServico": "Novo serviço",
        "ConfiguracaoCobrancaController.criarTabela": "Nova tabela",
    };
    return labels[id] ?? actionLabel(id);
}
export function visibleRecordActions(
    ids: string[],
    perfil: Perfil,
    row?: Values,
) {
    return ids.filter((id) => {
        if (!canPresent(perfil, endpoint(id).permission)) return false;
        if (!row) return true;
        const command = id.split(".")[1];
        // CadastroSupport forbids ordinary reactivation of definitively inactive registrations.
        if (names[id.split(".")[0].replace("Controller", "")]) {
            if (command === "encerrar") return row.situacao === "ATIVO";
            if (command === "reativar")
                return row.situacao === "ENCERRAMENTO_PENDENTE";
            if (command === "alterar") return row.situacao !== "INATIVO";
        }
        const situation = String(row.situacao ?? "");
        const allowed: Record<string, string[]> = {
            "UnidadeLogisticaController.unitizar": ["EFETIVADO"],
            "PedidoEntradaController.nota": ["RASCUNHO"],
            "PedidoEntradaController.xml": ["RASCUNHO", "EM_CONFERENCIA", "QUARENTENA", "EFETIVADO"],
            "PedidoEntradaController.iniciar": ["RASCUNHO"],
            "PedidoEntradaController.chegada": ["EM_CONFERENCIA", "QUARENTENA"],
            "PedidoEntradaController.efetivar": [
                "EM_CONFERENCIA",
                "QUARENTENA",
            ],
            "PedidoEntradaController.estornar": [
                "EM_CONFERENCIA",
                "QUARENTENA",
            ],
            "PedidoEntradaController.cancelar": ["RASCUNHO", "EM_CONFERENCIA"],
            "PedidoSaidaController.reservar": ["RASCUNHO"],
            "PedidoSaidaController.justificar": ["RASCUNHO"],
            "PedidoSaidaController.reverter": ["RESERVADO"],
            "PedidoSaidaController.cancelar": ["RASCUNHO", "RESERVADO"],
            "ExpedicaoController.ler": ["RESERVADO", "EM_SEPARACAO"],
            "ExpedicaoController.separar": ["RESERVADO", "EM_SEPARACAO"],
            "ExpedicaoController.documento": ["SEPARADO"],
            "ExpedicaoController.retirar": ["SEPARADO"],
            "ExpedicaoController.retornar": [
                "RESERVADO",
                "EM_SEPARACAO",
                "SEPARADO",
            ],
            "ExpedicaoController.devolver": ["RETIRADO"],
            "CargaInicialController.preparar": ["PENDENTE"],
            "CargaInicialController.revisar": ["PENDENTE"],
            "CargaInicialController.cancelar": ["PENDENTE"],
            "CargaInicialController.confirmar": ["PREPARADA"],
            "CargaInicialController.resolverCancelamento": ["PREPARADA"],
            "ContingenciaController.conciliar": ["PENDENTE"],
        };
        if (allowed[id] && situation) return allowed[id].includes(situation);
        if (id === "AvariaController.reconhecer" && row.reconhecidaEm)
            return false;
        if (id === "AvariaController.reparar" && row.resolvidaEm) return false;
        return true;
    });
}
