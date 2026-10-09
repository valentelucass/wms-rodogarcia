import { isObject, type Values } from "../../contracts/runtime";
import { label, display } from "../../domain/labels";
import {
    recordIdentity,
    recordRoot,
    recordTitle,
} from "../../domain/recordContext";
import { resultName } from "../../domain/resultNames";
import { recordActionLabel } from "../../domain/recordPages";

const columns: Record<string, string[]> = {
    "ClienteDto.Resposta": ["nome", "codigo", "documentoFiscal", "situacao"],
    "ArmazemDto.Resposta": [
        "nome",
        "codigo",
        "documentoFiscal",
        "cidade",
        "uf",
        "situacao",
    ],
    "ProdutoDto.Resposta": ["sku", "descricao", "unidadeMedida", "situacao"],
    "EmbalagemDto.Resposta": [
        "codigoDun",
        "descricao",
        "produtoId",
        "quantidadeProduto",
        "situacao",
    ],
    "EnderecoDto.Resposta": [
        "codigo",
        "descricao",
        "rua",
        "nivel",
        "tipo",
        "situacao",
    ],
    "PedidoEntradaDto.Resumo": ["id", "referencia", "situacao", "versao"],
    "PedidoSaidaDto.Detalhe": ["id", "referencia", "situacao", "versao"],
    "EstoqueDto.Unidade": [
        "codigo",
        "sku",
        "quantidade",
        "condicao",
        "tipoLocalizacao",
        "bloqueada",
    ],
    "UnidadeLogisticaDto.Resumo": [
        "codigo",
        "sku",
        "quantidade",
        "condicao",
        "pedidoId",
    ],
    "CapacidadeDto.Conjunto": [
        "codigo",
        "armazemId",
        "tipoEndereco",
        "tipoUnidadePermitido",
        "situacao",
        "versao",
    ],
    "IndicadorEstoqueDto.Resultado": [
        "sku",
        "quantidadeEmEstagio",
        "quantidadeConferenciaPendente",
        "observadoContagem",
        "diferencaContagem",
        "valorConsultado",
    ],
    "ConfiguracaoCobrancaDto.Servico": [
        "codigo",
        "descricao",
        "tipo",
        "unidade",
        "situacao",
    ],
    "ConfiguracaoCobrancaDto.Tabela": [
        "codigo",
        "tipo",
        "clienteId",
        "armazemId",
        "vigenciaInicio",
        "vigenciaFim",
        "situacao",
    ],
    "ConfiguracaoCobrancaDto.Vinculo": [
        "id",
        "clienteId",
        "armazemId",
        "tabelaId",
        "vigenciaInicio",
        "vigenciaFim",
    ],
    "ConfiguracaoCobrancaDto.Contrato": [
        "id",
        "clienteId",
        "armazemId",
        "vigenciaInicio",
        "vigenciaFim",
        "modalidadeCiclo",
        "moeda",
    ],
    AuditoriaResponse: [
        "id",
        "tipo",
        "registroId",
        "acao",
        "usuario",
        "instante",
        "motivo",
    ],
    "FatoServicoDto.Fato": [
        "id",
        "servicoId",
        "origem",
        "executadoEm",
        "quantidade",
        "situacao",
    ],
    "FatoServicoDto.MarcoResposta": [
        "id",
        "avariaId",
        "quantidadeAfetada",
        "quantidadeBase",
        "equivalenciaBase",
        "validadoEm",
        "motivo",
    ],
    "CalculoCobrancaDto.Resultado": [
        "id",
        "clienteId",
        "armazemId",
        "periodoInicio",
        "periodoFim",
        "total",
        "situacao",
    ],
    "FechamentoCobrancaDto.Fechamento": [
        "id",
        "clienteId",
        "armazemId",
        "periodoInicio",
        "periodoFim",
        "versaoAtual",
        "situacao",
    ],
    "FechamentoCobrancaDto.Ajuste": [
        "id",
        "origemVersaoId",
        "destinoFechamentoId",
        "valorBase",
        "valorCorrigido",
        "diferenca",
        "situacao",
    ],
    "ContagemDto.Resultado": [
        "codigoUnidade",
        "esperado",
        "contado",
        "diferenca",
        "observadoEm",
        "situacao",
        "impedimento",
    ],
    "CargaInicialDto.Resultado": [
        "referencia",
        "produtoId",
        "quantidadeEstagio",
        "revisao",
        "situacao",
    ],
    "ContingenciaDto.Resultado": [
        "identidadeFato",
        "tipo",
        "ocorridaEm",
        "situacao",
        "pendencia",
        "conciliadaEm",
    ],
};
export function RecordTable({
    rows,
    type,
    pending,
    onOpen,
    canEdit,
    canOperate,
}: {
    rows: Values[];
    type: string;
    pending: boolean;
    onOpen: (row: Values, action?: string) => void;
    canEdit?: (row: Values) => string | undefined;
    canOperate?: (row: Values) => string | undefined;
}) {
    const view = (row: Values) => ({ ...row, ...recordRoot(row) });
    const keys =
        columns[type] ??
        [
            ...new Set(
                rows.flatMap((row) =>
                    Object.entries(view(row))
                        .filter(
                            ([, value]) =>
                                !isObject(value) && !Array.isArray(value),
                        )
                        .map(([key]) => key),
                ),
            ),
        ].slice(0, 7);
    return (
        <div className="table-scroll record-table">
            <table>
                <caption>
                    {resultName(type)} · {rows.length} registros nesta resposta
                </caption>
                <thead>
                    <tr>
                        {keys.map((key) => (
                            <th scope="col" key={key}>
                                {(
                                    {
                                        documentoFiscal: "CPF/CNPJ",
                                        sku: "SKU",
                                        condicao: "Condição",
                                        tipoLocalizacao: "Localização",
                                    } as Record<string, string>
                                )[key] ?? label(key)}
                            </th>
                        ))}
                        <th scope="col">Ações</th>
                    </tr>
                </thead>
                <tbody>
                    {rows.map((row) => {
                        const values = view(row),
                            edit = canEdit?.(row);
                        const identity = recordIdentity(row);
                        const operation = canOperate?.(row);
                        return (
                            <tr key={identity}>
                                {keys.map((key, i) => (
                                    <td key={key}>
                                        {i === 0 ? (
                                            <button
                                                className="record-link"
                                                type="button"
                                                disabled={pending}
                                                onClick={() => onOpen(row)}
                                                aria-label={`Ver detalhes de ${recordTitle(row)} · ${identity}`}
                                            >
                                                {display(values[key])}
                                            </button>
                                        ) : ["situacao", "condicao"].includes(
                                              key,
                                          ) ? (
                                            <span className="record-status">
                                                {display(values[key])}
                                            </span>
                                        ) : (
                                            display(values[key])
                                        )}
                                    </td>
                                ))}
                                <td>
                                    <div className="record-row-actions">
                                        <button
                                            type="button"
                                            disabled={pending}
                                            onClick={() => onOpen(row)}
                                        >
                                            Ver detalhes
                                        </button>
                                        {edit && (
                                            <button
                                                type="button"
                                                disabled={pending}
                                                onClick={() =>
                                                    onOpen(row, edit)
                                                }
                                            >
                                                Editar
                                            </button>
                                        )}
                                        {!edit && operation && (
                                            <button
                                                type="button"
                                                disabled={pending}
                                                onClick={() =>
                                                    onOpen(row, operation)
                                                }
                                            >
                                                {recordActionLabel(operation)}
                                            </button>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
