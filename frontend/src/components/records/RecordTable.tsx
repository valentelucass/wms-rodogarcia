import { isObject, type Values } from "../../contracts/runtime";
import { label, display } from "../../domain/labels";
import {
    recordIdentity,
    recordRoot,
    recordTitle,
} from "../../domain/recordContext";
import { resultName } from "../../domain/resultNames";
import { recordActionLabel } from "../../domain/recordPages";
import { StatusBadge } from "../../design-system/StatusBadge";
import { Icon } from "../../design-system/Icon";
import type { CSSProperties } from "react";

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
    title,
}: {
    rows: Values[];
    type: string;
    pending: boolean;
    onOpen: (row: Values, action?: string) => void;
    canEdit?: (row: Values) => string | undefined;
    canOperate?: (row: Values) => string | undefined;
    title?: string;
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
        <div
            className="table-scroll record-table"
            style={{ "--record-column-count": keys.length } as CSSProperties}
        >
            <table>
                <colgroup>
                    {keys.map((key) => (
                        <col key={key} />
                    ))}
                    <col className="record-column-actions" />
                </colgroup>
                <caption>
                    <span className="record-table-heading">
                        <span>
                            {resultName(type) === "Registros"
                                ? (title ?? resultName(type))
                                : resultName(type)}
                        </span>{" "}
                        <span className="record-count">
                            · {rows.length} registros nesta resposta
                        </span>
                    </span>
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
                                        situacao: "Situação",
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
                                    <td
                                        key={key}
                                        className={
                                            i === 0
                                                ? "record-cell-identity"
                                                : [
                                                        "codigo",
                                                        "documentoFiscal",
                                                        "sku",
                                                        "id",
                                                    ].includes(key)
                                                  ? "record-cell-code"
                                                  : undefined
                                        }
                                    >
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
                                            <StatusBadge value={values[key]} />
                                        ) : (
                                            display(values[key])
                                        )}
                                    </td>
                                ))}
                                <td className="record-cell-actions">
                                    <div className="record-row-actions">
                                        <button
                                            className="record-action-icon"
                                            type="button"
                                            title="Ver detalhes"
                                            disabled={pending}
                                            onClick={() => onOpen(row)}
                                        >
                                            <Icon name="eye" />
                                            <span className="sr-only">
                                                Ver detalhes
                                            </span>
                                        </button>
                                        {edit && (
                                            <button
                                                className="primary record-action-icon"
                                                type="button"
                                                title="Editar"
                                                disabled={pending}
                                                onClick={() =>
                                                    onOpen(row, edit)
                                                }
                                            >
                                                <Icon name="edit" />
                                                <span className="sr-only">
                                                    Editar
                                                </span>
                                            </button>
                                        )}
                                        {!edit && operation && (
                                            <button
                                                className={
                                                    "record-action-icon" +
                                                    (/\.(cancelar|estornar|encerrar|reverter|excluir)$/.test(
                                                        operation,
                                                    )
                                                        ? " record-action-danger"
                                                        : "")
                                                }
                                                type="button"
                                                title={recordActionLabel(
                                                    operation,
                                                )}
                                                disabled={pending}
                                                onClick={() =>
                                                    onOpen(row, operation)
                                                }
                                            >
                                                <Icon name="arrow" />
                                                <span className="sr-only">
                                                    {recordActionLabel(
                                                        operation,
                                                    )}
                                                </span>
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
