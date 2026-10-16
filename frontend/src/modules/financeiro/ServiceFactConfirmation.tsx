import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { isObject, type Values } from "../../contracts/runtime";
import { information } from "../../content/information";
import { display } from "../../domain/labels";
import { formatDate } from "../../domain/referencePresentation";

const shown = (value: unknown) =>
    value === "" || value == null ? "Não informado" : display(value);
const origins: Record<string, string> = {
    MANUAL: "Manual",
    SUGESTAO: "Sugestão",
};
const commitments: Record<string, string> = {
    PEDIDO_ENTRADA: "Pedido de entrada",
    PEDIDO_SAIDA: "Pedido de saída",
    FECHAMENTO: "Fechamento de cobrança",
};

export function ServiceFactConfirmation({ values }: { values: Values }) {
    const reference = (field: string) => (
        <ReferenceValue
            field={field}
            value={values[field]}
            row={values}
            type="FatoServicoDto.Registrar"
        />
    );
    const shares = Array.isArray(values.cotas) ? values.cotas : [];
    const resolution = isObject(values.resolucao) ? values.resolucao : null;
    return (
        <div className="service-fact-confirmation">
            <p className="service-fact-confirmation-intro">
                {information.serviceFactConfirmation.description}
            </p>
            <dl className="service-fact-confirmation-context">
                <div>
                    <dt>Cliente</dt>
                    <dd>{reference("clienteId")}</dd>
                </div>
                <div>
                    <dt>Armazém</dt>
                    <dd>{reference("armazemId")}</dd>
                </div>
            </dl>
            <section
                className="service-fact-confirmation-section"
                aria-label="Serviço e execução"
            >
                <h3>Serviço e execução</h3>
                <dl className="service-fact-confirmation-values">
                    <div className="service-fact-confirmation-wide">
                        <dt>Serviço</dt>
                        <dd className="service-fact-confirmation-service">
                            {reference("servicoId")}
                        </dd>
                    </div>
                    <div>
                        <dt>Origem do registro</dt>
                        <dd>
                            <span className="service-fact-confirmation-origin">
                                {origins[String(values.origem)] ??
                                    shown(values.origem)}
                            </span>
                        </dd>
                    </div>
                    <div>
                        <dt>Executado em (Brasília)</dt>
                        <dd>
                            {values.executadoEm
                                ? formatDate(values.executadoEm)
                                : "Não informado"}
                        </dd>
                    </div>
                    <div className="service-fact-confirmation-wide">
                        <dt>Referência da execução</dt>
                        <dd>{shown(values.referenciaExecucao)}</dd>
                    </div>
                </dl>
            </section>
            <section
                className="service-fact-confirmation-section"
                aria-label="Vínculos de origem"
            >
                <h3>Vínculos de origem</h3>
                <dl className="service-fact-confirmation-values">
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
            <section
                className="service-fact-confirmation-section"
                aria-label="Quantidade e valor"
            >
                <h3>Quantidade e valor</h3>
                <dl className="service-fact-confirmation-values service-fact-confirmation-amounts">
                    <div>
                        <dt>Quantidade</dt>
                        <dd>{shown(values.quantidade)}</dd>
                    </div>
                    <div>
                        <dt>Categoria</dt>
                        <dd>{shown(values.categoria)}</dd>
                    </div>
                    <div>
                        <dt>Valor base</dt>
                        <dd>{shown(values.valorBase)}</dd>
                    </div>
                </dl>
            </section>
            <section
                className="service-fact-confirmation-section"
                aria-label="Critério de rateio"
            >
                <h3>Critério de rateio</h3>
                <p className="service-fact-confirmation-text">
                    {shown(values.criterioRateio)}
                </p>
                <h4>Cotas por nota</h4>
                {shares.length ? (
                    <ol className="service-fact-confirmation-shares">
                        {shares.map((share, index) => {
                            const row = isObject(share) ? share : {};
                            return (
                                <li key={index}>
                                    <dl className="service-fact-confirmation-values">
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
                                        <div>
                                            <dt>Cota informada</dt>
                                            <dd>{shown(row.cota)}</dd>
                                        </div>
                                    </dl>
                                </li>
                            );
                        })}
                    </ol>
                ) : (
                    <p className="hint">Nenhuma cota informada.</p>
                )}
            </section>
            <section
                className="service-fact-confirmation-section service-fact-confirmation-reason"
                aria-label="Justificativa do fato"
            >
                <h3>Motivo / justificativa</h3>
                <p className="service-fact-confirmation-text">
                    {shown(values.motivo)}
                </p>
            </section>
            <section
                className="service-fact-confirmation-section"
                aria-label="Resolução financeira histórica"
            >
                <h3>Resolução financeira histórica</h3>
                {resolution ? (
                    <dl className="service-fact-confirmation-values">
                        <div>
                            <dt>Tipo de compromisso</dt>
                            <dd>
                                {commitments[String(resolution.tipo)] ??
                                    shown(resolution.tipo)}
                            </dd>
                        </div>
                        <div>
                            <dt>Compromisso</dt>
                            <dd>
                                <ReferenceValue
                                    field="compromissoId"
                                    value={resolution.compromissoId}
                                    row={resolution}
                                    type="FechamentoCobrancaDto.Resolucao"
                                />
                            </dd>
                        </div>
                    </dl>
                ) : (
                    <p className="hint">Não informada.</p>
                )}
            </section>
            <details className="service-fact-confirmation-technical">
                <summary>Identificadores e instante original</summary>
                <dl className="service-fact-confirmation-values">
                    <div>
                        <dt>Identificador da operação</dt>
                        <dd>{shown(values.operacaoId)}</dd>
                    </div>
                    <div>
                        <dt>ID do serviço</dt>
                        <dd>{shown(values.servicoId)}</dd>
                    </div>
                    <div className="service-fact-confirmation-wide">
                        <dt>Instante informado</dt>
                        <dd>{shown(values.executadoEm)}</dd>
                    </div>
                </dl>
            </details>
        </div>
    );
}
