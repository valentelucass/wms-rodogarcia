import { s, type Journey, type NextAction } from "../../domain/journeyTypes";
export const journeys: Journey[] = [
    {
        id: "contagem",
        title: "Contagem e carga inicial",
        fe: "FE12",
        be: "BE14",
        steps: [
            s(
                "Consultar unidade para contagem",
                "Leia o UUID e consulte a unidade/versão no estoque antes de registrar a observação. Essa consulta não exige inventar um pedido de entrada. Depois abra Contagem física.",
                "EstoqueController",
                ["consultar"],
            ),
            s(
                "Contagem física",
                "Leia a unidade e sua versão, registre instante e quantidade observada. Consulte esperado, contado, diferença, reservado, origens e impedimento retornados. Contar não aplica ajuste. Somente Supervisor/Gestor confirma ajuste com revisão, origens/deltas, causa, destino e comprovação. Exercício preparado: leitura 8 sem reserva; leitura 7 retorna impedimento de reserva. O navegador não calcula diferença.",
                "ContagemController",
                ["listar", "consultar", "contar", "revisoes", "aplicar"],
            ),
            s(
                "Carga inicial excepcional",
                "Declare ausências sem inventar nota, FIFO ou valor. Criar mantém PENDENTE. O roteiro fictício conhecido usa a entrada 201 na revisão; outras origens continuam pendentes. Confira versão, revisão e hash devolvidos ao preparar. PREPARADA possui entrada e etiquetas, mas não libera saída. Confirme leitura da etiqueta fornecida e todas as etiquetas devolvidas, depois consulte REGULARIZADA. Exemplos não comprovam carga real.",
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
        lookups: [],
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
                "Fluxo manual após recuperação da rede: registre identidade do fato, instante real, operador, fonte, dependências e dados de CHEGADA pelo contrato. Consulte versão/hash antes de conciliar. EXECUTAR ou VINCULAR com prova da operação original são comandos explícitos. Dependência mantém pendência; conteúdo divergente retorna conflito. Repetição conserva a confirmação original; consulte o estado atual. Não há fila automática offline nem efeito real neste exercício.",
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
