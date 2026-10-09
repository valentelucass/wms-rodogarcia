import {
    cadastro,
    s,
    type Journey,
    type NextAction,
} from "../../domain/journeyTypes";
export const journeys: Journey[] = [
    {
        id: "cadastros",
        title: "Cadastros",
        fe: "FE04",
        be: "BE05",
        steps: [
            cadastro("Clientes", "ClienteController"),
            cadastro("Armazéns", "ArmazemController"),
            cadastro("Produtos / SKU", "ProdutoController"),
            cadastro("Embalagens / DUN", "EmbalagemController"),
            cadastro("Endereços", "EnderecoController"),
            s(
                "Capacidade e conjuntos",
                "Limites ausentes significam não configurados. O backend confere as duas posições juntas.",
                "CapacidadeController",
                ["listar", "configurar", "criar", "encerrar"],
            ),
            s(
                "Importar endereços Excel",
                "Selecione .xlsx, confira cada linha na prévia e confirme a importação completa pelo hash retornado.",
                "ImportacaoEnderecoController",
                ["previa", "consultar", "confirmar"],
            ),
            s(
                "Dados fiscais cadastrais",
                "Referências de cliente/armazém/produto; configuração não emite documento.",
                "FiscalCadastroController",
                [
                    "cliente",
                    "complementarCliente",
                    "armazem",
                    "complementarArmazem",
                    "referencias",
                    "referenciar",
                ],
            ),
            s(
                "Encerramento e impedimentos",
                "Consulte os compromissos antes de solicitar ou confirmar inativação. Não apague estoque ou histórico.",
                "EncerramentoController",
                [
                    "consultar",
                    "solicitar",
                    "inativar",
                    "remanescente",
                    "vigencia",
                ],
            ),
        ],
        lookups: [],
        references: [
            ["Prévia Excel", "ImportacaoEnderecoDto.Resultado"],
            ["Encerramento", "EncerramentoDto.Resultado"],
        ],
        referenceFields: {},
    },
];
export const nextActions: Record<string, NextAction> = {
    "ClienteController.alterar": {
        page: "cadastros",
        action: "ClienteController.consultar",
        title: "Conferir o cadastro atualizado",
    },
    "ImportacaoEnderecoController.previa": {
        page: "cadastros",
        action: "ImportacaoEnderecoController.consultar",
        title: "Consultar esta prévia Excel",
    },
    "ImportacaoEnderecoController.consultar": {
        page: "cadastros",
        action: "ImportacaoEnderecoController.confirmar",
        title: "Confirmar a prévia consultada sem erros",
    },
    "ImportacaoEnderecoController.confirmar": {
        page: "cadastros",
        action: "ImportacaoEnderecoController.consultar",
        title: "Conferir endereços da importação confirmada",
    },
    "EncerramentoController.consultar": {
        page: "cadastros",
        action: "EncerramentoController.solicitar",
        title: "Solicitar encerramento deste cadastro",
    },
    "EncerramentoController.solicitar": {
        page: "cadastros",
        action: "EncerramentoController.inativar",
        title: "Conferir inativação do cadastro solicitado",
    },
    "EncerramentoController.inativar": {
        page: "cadastros",
        action: "EncerramentoController.consultar",
        title: "Consultar situação e impedimentos atuais",
    },
};
