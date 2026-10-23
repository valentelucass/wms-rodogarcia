import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { isObject, type Values } from "../../contracts/runtime";
import { display } from "../../domain/labels";
import { formatDate } from "../../domain/referencePresentation";
import { StatusBadge } from "../../design-system/StatusBadge";

const shown = (value: unknown) =>
    value === "" || value == null ? "Não informado" : display(value);
const origins: Record<string, string> = {
    MANUAL: "Manual",
    SUGESTAO: "Sugestão",
};
const states: Record<string, string> = {
    CONFIRMADO: "Confirmado",
    ANULADO: "Anulado",
};

export function ServiceFactDetail({ data }: { data: Values }) {
    const reference = (field: string) => (
        <ReferenceValue
            field={field}
            value={data[field]}
            row={data}
            type="FatoServicoDto.Fato"
        />
    );
    const field = (title: string, value: unknown) => (
        <div>
            <dt>{title}</dt>
            <dd>{shown(value)}</dd>
        </div>
    );
    const shares = Array.isArray(data.cotas) ? data.cotas : null;
    return (
        <article
            className="service-fact-detail"
            aria-label="Fato de serviço consultado"
        >
            <header className="service-fact-detail-header">
                <div>
                    <span className="hint">Serviço</span>
                    <h3>{reference("servicoId")}</h3>
                </div>
                <div>
                    <span className="hint">Situação consultada</span>
                    <StatusBadge
                        value={data.situacao}
                        label={
                            states[String(data.situacao)] ??
                            shown(data.situacao)
                        }
                    />
                </div>
            </header>
            <dl className="service-fact-detail-context">
                <div>
                    <dt>Cliente</dt>
                    <dd>{reference("clienteId")}</dd>
                </div>
                <div>
                    <dt>Armazém</dt>
                    <dd>{reference("armazemId")}</dd>
                </div>
            </dl>
            <section aria-label="Execução consultada">
                <h4>Execução</h4>
                <dl className="service-fact-detail-values">
                    {field(
                        "Origem do registro",
                        origins[String(data.origem)] ?? data.origem,
                    )}
                    {field(
                        "Executado em (Brasília)",
                        data.executadoEm ? formatDate(data.executadoEm) : null,
                    )}
                    {field("Referência da execução", data.referenciaExecucao)}
                </dl>
                <dl className="service-fact-detail-values service-fact-detail-amounts">
                    {field("Quantidade", data.quantidade)}
                    {field("Categoria", data.categoria)}
                    {field("Valor base", data.valorBase)}
                </dl>
            </section>
            <section aria-label="Vínculos consultados">
                <h4>Vínculos de origem</h4>
                <dl className="service-fact-detail-values">
                    <div>
                        <dt>Unidade logística</dt>
                        <dd>{reference("unidadeId")}</dd>
                    </div>
                    <div>
                        <dt>Produto / SKU</dt>
                        <dd>{reference("produtoId")}</dd>
                    </div>
                    <div>
                        <dt>Pedido de entrada</dt>
                        <dd>{reference("pedidoEntradaId")}</dd>
                    </div>
                    <div>
                        <dt>Pedido de saída</dt>
                        <dd>{reference("pedidoSaidaId")}</dd>
                    </div>
                </dl>
            </section>
            <section aria-label="Rateio consultado">
                <h4>Critério de rateio</h4>
                <p className="service-fact-detail-text">
                    {shown(data.criterioRateio)}
                </p>
                <h4>
                    Cotas por nota{shares?.length ? ` (${shares.length})` : ""}
                </h4>
                {shares?.length ? (
                    <ul className="service-fact-detail-shares">
                        {shares.map((share, index) => {
                            const row = isObject(share) ? share : {};
                            return (
                                <li key={index}>
                                    <dl className="service-fact-detail-values">
                                        <div>
                                            <dt>Nota de origem</dt>
                                            <dd>
                                                <ReferenceValue
                                                    field="notaId"
                                                    value={row.notaId}
                                                    row={row}
                                                    type="FatoServicoDto.Cota"
                                                />
                                            </dd>
                                        </div>
                                        {field("Cota informada", row.cota)}
                                    </dl>
                                </li>
                            );
                        })}
                    </ul>
                ) : (
                    <p className="hint">
                        {shares
                            ? "Nenhuma cota informada."
                            : "Cotas não informadas pelo servidor."}
                    </p>
                )}
            </section>
            <details className="service-fact-detail-technical">
                <summary>Identificadores e instante original</summary>
                <dl className="service-fact-detail-values">
                    {field("ID do fato", data.id)}
                    {field("Revisão", data.versao)}
                    {field("Chave do fato", data.chaveFato)}
                    {field("ID do serviço", data.servicoId)}
                    {field("Instante original", data.executadoEm)}
                </dl>
            </details>
        </article>
    );
}
