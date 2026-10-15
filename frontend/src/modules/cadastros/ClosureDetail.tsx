import { isObject, type Values } from "../../contracts/runtime";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { StatusBadge } from "../../design-system/StatusBadge";
import { information } from "../../content/information";

const subjects: Record<string, { label: string; type: string }> = {
    CLIENTE: { label: "Cliente", type: "ClienteDto.Resposta" },
    ARMAZEM: { label: "Armazém", type: "ArmazemDto.Resposta" },
    PRODUTO: { label: "Produto", type: "ProdutoDto.Resposta" },
    EMBALAGEM: { label: "Embalagem", type: "EmbalagemDto.Resposta" },
    ENDERECO: { label: "Endereço", type: "EnderecoDto.Resposta" },
    CONJUNTO_POSICOES: {
        label: "Conjunto de posições",
        type: "CapacidadeDto.Conjunto",
    },
    SERVICO_COBRANCA: {
        label: "Serviço de cobrança",
        type: "ConfiguracaoCobrancaDto.Servico",
    },
};
const readable = (value: unknown) =>
    typeof value === "string" && value.trim()
        ? value
              .replaceAll("_", " ")
              .toLocaleLowerCase("pt-BR")
              .replace(/^./u, (letter) => letter.toLocaleUpperCase("pt-BR"))
        : undefined;
const resources: Record<string, string> = {
    PEDIDO_ENTRADA: "Pedido de entrada",
    PEDIDO_SAIDA: "Pedido de saída",
    ENTRADA_CONFERIDA: "Entrada conferida",
    CARGA_INICIAL: "Carga inicial",
    UNIDADE_LOGISTICA: "Unidade logística",
};
const resourceTypes: Record<string, string> = {
    PEDIDO_ENTRADA: "PedidoEntradaDto.Resumo",
    PEDIDO_SAIDA: "PedidoSaidaDto.Detalhe",
    CARGA_INICIAL: "CargaInicialDto.Resultado",
};

export function ClosureDetail({
    data,
    subjectTitle,
    onSelect,
}: {
    data: Values;
    subjectTitle?: string;
    onSelect?: (row: Values, type: string) => void;
}) {
    const subject = subjects[String(data.tipo)];
    const issues = Array.isArray(data.impedimentos)
        ? data.impedimentos
        : undefined;
    return (
        <section
            className="closure-detail"
            aria-label="Situação e impedimentos do cadastro"
        >
            <header className="closure-summary">
                <div>
                    <span className="hint">{subject?.label ?? "Cadastro"}</span>
                    <strong>
                        {subjectTitle &&
                        subjectTitle !== "Registro consultado" ? (
                            subjectTitle
                        ) : data.id != null && subject ? (
                            <ReferenceValue
                                field="id"
                                value={data.id}
                                type={subject.type}
                            />
                        ) : (
                            "Cadastro não identificado na consulta"
                        )}
                    </strong>
                </div>
                <StatusBadge value={data.situacao} />
            </header>
            <section
                className="closure-impediments"
                aria-label="Impedimentos do cadastro"
            >
                <div className="closure-impediments-heading">
                    <h3>Impedimentos</h3>
                    {issues && (
                        <span className="closure-issue-count">
                            {issues.length}{" "}
                            {issues.length === 1 ? "informado" : "informados"}
                        </span>
                    )}
                </div>
                {issues && issues.length > 0 ? (
                    <ul className="closure-issue-list">
                        {issues.map((value, index) => {
                            const issue = isObject(value) ? value : {};
                            return (
                                <li className="closure-issue" key={index}>
                                    <span
                                        className="closure-issue-number"
                                        aria-hidden="true"
                                    >
                                        {index + 1}
                                    </span>
                                    <div>
                                        <div className="closure-issue-heading">
                                            <h4>
                                                {readable(issue.codigo) ??
                                                    "Impedimento informado"}
                                            </h4>
                                            {issue.recurso != null && (
                                                <span className="closure-resource">
                                                    {resources[
                                                        String(issue.recurso)
                                                    ] ??
                                                        readable(issue.recurso)}
                                                </span>
                                            )}
                                        </div>
                                        <p>
                                            {typeof issue.detalhe ===
                                                "string" && issue.detalhe.trim()
                                                ? issue.detalhe
                                                : "Detalhe não informado pelo servidor."}
                                        </p>
                                        {issue.id != null && (
                                            <p className="closure-issue-reference">
                                                Referência:{" "}
                                                {resourceTypes[
                                                    String(issue.recurso)
                                                ] ? (
                                                    <ReferenceValue
                                                        field="id"
                                                        value={issue.id}
                                                        type={
                                                            resourceTypes[
                                                                String(
                                                                    issue.recurso,
                                                                )
                                                            ]
                                                        }
                                                    />
                                                ) : (
                                                    String(issue.id)
                                                )}
                                            </p>
                                        )}
                                    </div>
                                </li>
                            );
                        })}
                    </ul>
                ) : (
                    <p className="closure-empty">
                        {issues
                            ? information.closureNoImpediments.description
                            : information.closureImpedimentsUnavailable
                                  .description}
                    </p>
                )}
                <p className="hint closure-impediments-help">
                    {information.closureImpediments.description}
                </p>
            </section>
            {onSelect && data.id != null && (
                <div className="closure-selection">
                    <button
                        type="button"
                        className="action-select"
                        onClick={() =>
                            onSelect(data, "EncerramentoDto.Resultado")
                        }
                    >
                        Selecionar este registro
                    </button>
                </div>
            )}
        </section>
    );
}
