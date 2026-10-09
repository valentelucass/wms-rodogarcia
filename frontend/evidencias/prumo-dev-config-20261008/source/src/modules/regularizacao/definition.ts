import { s, type Journey, type NextAction } from "../../domain/journeyTypes";
export const journeys: Journey[] = [
    {
        id: "contagem",
        title: "Contagem e carga inicial",
        fe: "FE12",
        be: "BE14",
        steps: [
            s(
                "Contagem física",
                "Leia a unidade, registre quantidade observada e consulte diferenças/reservas/impedimentos. Somente Supervisor/Gestor aplica ajuste.",
                "ContagemController",
                ["listar", "consultar", "contar", "revisoes", "aplicar"],
            ),
            s(
                "Carga inicial excepcional",
                "Não invente nota/FIFO/valor ausentes. Levante ausências, revise, prepare e confirme somente após tratativa.",
                "CargaInicialController",
                [
                    "listar",
                    "consultar",
                    "criar",
                    "revisoes",
                    "revisar",
                    "preparar",
                    "confirmar",
                    "cancelar",
                    "resolverCancelamento",
                ],
            ),
        ],
        lookups: [
            [
                "Consultar unidades para contagem",
                "UnidadeLogisticaController.listar",
            ],
        ],
        references: [
            ["Unidade logística", "UnidadeLogisticaDto.Resumo"],
            ["Contagem", "ContagemDto.Resultado"],
            ["Carga inicial", "CargaInicialDto.Resultado"],
        ],
        referenceFields: {
            unidadeId: "UnidadeLogisticaDto.Resumo",
            produtoId: "ProdutoDto.Resposta",
        },
    },
    {
        id: "contingencia",
        title: "Contingência",
        fe: "FE12",
        be: "BE14",
        steps: [
            s(
                "Registrar fatos da planilha",
                "Fluxo manual após recuperação da rede. Identidade, instante real, operador, dependências e prova preservados; não há fila automática offline.",
                "ContingenciaController",
                ["listar", "consultar", "registrar", "conciliar"],
            ),
        ],
        lookups: [],
        references: [
            ["Fato manual", "ContingenciaDto.Resultado"],
            ["Unidade logística", "UnidadeLogisticaDto.Resumo"],
        ],
        referenceFields: { unidadeId: "UnidadeLogisticaDto.Resumo" },
    },
];
export const nextActions: Record<string, NextAction> = {
    "ContagemController.contar": {
        page: "contagem",
        action: "ContagemController.consultar",
        title: "Conferir contagem sem efeito no estoque",
    },
    "ContagemController.consultar": {
        page: "contagem",
        action: "ContagemController.aplicar",
        title: "Conferir ajuste motivado desta contagem",
    },
    "ContagemController.aplicar": {
        page: "contagem",
        action: "ContagemController.consultar",
        title: "Consultar o efeito confirmado do ajuste",
    },
    "CargaInicialController.criar": {
        page: "contagem",
        action: "CargaInicialController.consultar",
        title: "Consultar pendências desta carga excepcional",
    },
    "CargaInicialController.consultar": {
        page: "contagem",
        action: "CargaInicialController.revisar",
        title: "Revisar os dados levantados desta carga",
    },
    "CargaInicialController.revisar": {
        page: "contagem",
        action: "CargaInicialController.preparar",
        title: "Conferir preparação pela revisão e hash recebidos",
    },
    "CargaInicialController.preparar": {
        page: "contagem",
        action: "CargaInicialController.confirmar",
        title: "Confirmar leitura e etiquetas da carga preparada",
    },
    "CargaInicialController.confirmar": {
        page: "contagem",
        action: "CargaInicialController.consultar",
        title: "Consultar a entrada regularizada",
    },
    "ContingenciaController.registrar": {
        page: "contingencia",
        action: "ContingenciaController.consultar",
        title: "Consultar o fato manual registrado",
    },
    "ContingenciaController.consultar": {
        page: "contingencia",
        action: "ContingenciaController.conciliar",
        title: "Conferir conciliação manual deste fato",
    },
    "ContingenciaController.conciliar": {
        page: "contingencia",
        action: "ContingenciaController.consultar",
        title: "Consultar resultado e pendências da conciliação",
    },
};
