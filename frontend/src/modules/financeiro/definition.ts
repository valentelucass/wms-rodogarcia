import { s, type Journey, type NextAction } from "../../domain/journeyTypes";
export const journeys: Journey[] = [
    {
        id: "precos",
        title: "Serviços e tabelas",
        fe: "FE04 / FE11",
        be: "BE05 / BE12",
        steps: [
            s(
                "Serviços contratados",
                "Preços e parâmetros são configuração. Exemplos fictícios não são tabela comercial aprovada.",
                "ConfiguracaoCobrancaController",
                ["servicos", "criarServico"],
            ),
            s(
                "Tabelas e vigências",
                "Preserve histórico e intervalos de vigência. Não preencha parâmetro ausente com zero.",
                "ConfiguracaoCobrancaController",
                ["tabelas", "tabela", "criarTabela", "encerrarTabela"],
            ),
            s(
                "Vínculos por cliente",
                "Vincule uma tabela no contexto correto. Encerramento não apaga cálculo anterior.",
                "ConfiguracaoCobrancaController",
                ["vinculos", "vincular", "encerrarVinculo"],
            ),
            s(
                "Contrato de fechamento",
                "Configure corte, mínimo e GRIS somente com dados do contrato. Convenções AC04–08 permanecem propostas para validação comercial.",
                "ConfiguracaoCobrancaController",
                ["contratos", "configurar", "encerrarContrato"],
            ),
        ],
        lookups: [],
        references: [
            ["Serviço configurado", "ConfiguracaoCobrancaDto.Servico"],
            ["Tabela", "ConfiguracaoCobrancaDto.Tabela"],
            ["Vínculo", "ConfiguracaoCobrancaDto.Vinculo"],
            ["Cálculo", "CalculoCobrancaDto.Resultado"],
            ["Fechamento", "FechamentoCobrancaDto.Fechamento"],
            ["Versão financeira", "FechamentoCobrancaDto.Versao"],
        ],
        referenceFields: {
            calculoId: "CalculoCobrancaDto.Resultado",
            servicoId: "ConfiguracaoCobrancaDto.Servico",
            tabelaId: "ConfiguracaoCobrancaDto.Tabela",
        },
    },
    {
        id: "cobranca",
        title: "Serviços e cálculo",
        fe: "FE11",
        be: "BE12",
        steps: [
            s(
                "1. Fatos e origem",
                "Consulte sugestões e registre execução real. Adicional pode ser associado posteriormente ao pedido pertinente; não crie saída fictícia.",
                "FatoServicoController",
                ["listar", "sugestoes", "consultar", "registrar", "anular"],
            ),
            s(
                "2. Avaria financeira",
                "Gestor registra marcos identificados. Condição física e suspensão financeira são informações distintas.",
                "FatoServicoController",
                ["marcos", "marco"],
            ),
            s(
                "3. Memória do servidor",
                "Solicite cálculo e confira origens, diárias, pico cobrável, vigências, mínimo, GRIS e pendências. O navegador não recalcula valores.",
                "CalculoCobrancaController",
                ["listar", "consultar", "calcular"],
            ),
        ],
        lookups: [
            [
                "Consultar serviços para o fato",
                "ConfiguracaoCobrancaController.servicos",
            ],
        ],
        references: [
            ["Fato", "FatoServicoDto.Fato"],
            ["Origem consultada", "FatoServicoDto.Sugestao"],
            ["Cálculo", "CalculoCobrancaDto.Resultado"],
            ["Fechamento", "FechamentoCobrancaDto.Fechamento"],
            ["Versão financeira", "FechamentoCobrancaDto.Versao"],
        ],
        referenceFields: { calculoId: "CalculoCobrancaDto.Resultado" },
    },
    {
        id: "fechamento",
        title: "Fechamentos e ESL",
        fe: "FE11",
        be: "BE13",
        steps: [
            s(
                "1. Ciclo e versões",
                "Prepare o ciclo com cálculo identificado. Consulte a versão e baixe os bytes do demonstrativo; consulta não registra entrega.",
                "FechamentoCobrancaController",
                [
                    "listar",
                    "consultar",
                    "preparar",
                    "versoes",
                    "versao",
                    "demonstrativo",
                ],
            ),
            s(
                "2. Decisão integral do Gestor",
                "Aprove/rejeite o ciclo inteiro. Reabertura exige condições confirmadas pelo servidor; preserve a versão antiga.",
                "FechamentoCobrancaController",
                ["aprovar", "rejeitar", "reabrir"],
            ),
            s(
                "3. Entrega manual e NFS-e",
                "Registre entrega efetuada fora do WMS, declaração de não emissão ou referência NFS-e existente. Não há envio ao ESL nem emissão automática.",
                "FechamentoCobrancaController",
                [
                    "entregar",
                    "confirmarNaoEmissao",
                    "registrarNfse",
                    "resolverSaldo",
                ],
            ),
            s(
                "4. Conflitos e ajustes",
                "Trate conflito externo e ajustes por origem/base/delta sem alterar documento emitido ou cálculo fechado.",
                "FechamentoCobrancaController",
                ["tratativas", "tratar"],
            ),
            s(
                "Ajustes de ciclo",
                "Ajuste é identificado e mantém a origem. O servidor calcula/verifica consequências.",
                "AjusteFechamentoController",
                ["listar", "ajustar"],
            ),
        ],
        lookups: [],
        references: [
            ["Cálculo", "CalculoCobrancaDto.Resultado"],
            ["Fechamento", "FechamentoCobrancaDto.Fechamento"],
            ["Versão financeira", "FechamentoCobrancaDto.Versao"],
        ],
        referenceFields: { calculoId: "CalculoCobrancaDto.Resultado" },
    },
];
export const nextActions: Record<string, NextAction> = {
    "ConfiguracaoCobrancaController.criarServico": {
        page: "precos",
        action: "ConfiguracaoCobrancaController.criarTabela",
        title: "Configurar tabela com este serviço fictício",
    },
    "ConfiguracaoCobrancaController.criarTabela": {
        page: "precos",
        action: "ConfiguracaoCobrancaController.tabela",
        title: "Consultar tabela e vigência recebidas",
    },
    "ConfiguracaoCobrancaController.tabela": {
        page: "precos",
        action: "ConfiguracaoCobrancaController.vincular",
        title: "Vincular esta tabela ao cliente do contexto",
    },
    "ConfiguracaoCobrancaController.vincular": {
        page: "precos",
        action: "ConfiguracaoCobrancaController.vinculos",
        title: "Consultar vínculo de tabela confirmado",
    },
    "FatoServicoController.registrar": {
        page: "cobranca",
        action: "FatoServicoController.consultar",
        title: "Consultar situação e origem deste fato",
    },
    "FatoServicoController.consultar": {
        page: "cobranca",
        action: "CalculoCobrancaController.calcular",
        title: "Conferir solicitação de cálculo após consulta do fato",
    },
    "AjusteFechamentoController.ajustar": {
        page: "fechamento",
        action: "AjusteFechamentoController.listar",
        title: "Consultar ajustes identificados do fechamento",
    },
    "FechamentoCobrancaController.reabrir": {
        page: "fechamento",
        action: "FechamentoCobrancaController.versao",
        title: "Consultar a nova versão após reabertura",
    },
    "CalculoCobrancaController.calcular": {
        page: "fechamento",
        action: "FechamentoCobrancaController.preparar",
        title: "Preparar fechamento com este cálculo",
    },
    "FechamentoCobrancaController.preparar": {
        page: "fechamento",
        action: "FechamentoCobrancaController.versao",
        title: "Conferir memória e hash da versão preparada",
    },
    "FechamentoCobrancaController.versao": {
        page: "fechamento",
        action: "FechamentoCobrancaController.demonstrativo",
        title: "Consultar demonstrativo da versão selecionada",
    },
    "FechamentoCobrancaController.demonstrativo": {
        page: "fechamento",
        action: "FechamentoCobrancaController.aprovar",
        title: "Conferir e decidir o ciclo inteiro",
    },
    "FechamentoCobrancaController.aprovar": {
        page: "fechamento",
        action: "FechamentoCobrancaController.entregar",
        title: "Registrar entrega manual desta versão",
    },
    "FechamentoCobrancaController.entregar": {
        page: "fechamento",
        action: "FechamentoCobrancaController.tratativas",
        title: "Conferir estado externo e tratativas",
    },
    "FechamentoCobrancaController.registrarNfse": {
        page: "fechamento",
        action: "FechamentoCobrancaController.tratativas",
        title: "Consultar referências e conflitos externos",
    },
    "FechamentoCobrancaController.tratativas": {
        page: "fechamento",
        action: "FechamentoCobrancaController.tratar",
        title: "Registrar conferência das referências externas",
    },
    "FechamentoCobrancaController.tratar": {
        page: "fechamento",
        action: "FechamentoCobrancaController.consultar",
        title: "Conferir o fechamento após a tratativa",
    },
};
