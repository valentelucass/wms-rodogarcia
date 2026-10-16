import type { Values } from "../../contracts/runtime";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { display } from "../../domain/labels";
import { formatDate } from "../../domain/referencePresentation";
import { information } from "../../content/information";

export function CountConfirmation({
    context,
    values,
}: {
    context: Values;
    values: Values;
}) {
    return (
        <div className="count-confirmation">
            <p className="count-confirmation-intro">
                {information.countConfirmation.description}
            </p>
            <dl className="count-confirmation-context">
                <div>
                    <dt>Cliente</dt>
                    <dd>
                        <ReferenceValue
                            field="clienteId"
                            value={context.clienteId}
                        />
                    </dd>
                </div>
                <div>
                    <dt>Armazém</dt>
                    <dd>
                        <ReferenceValue
                            field="armazemId"
                            value={context.armazemId}
                        />
                    </dd>
                </div>
            </dl>
            <dl className="count-confirmation-reading">
                <div>
                    <dt>Quantidade contada</dt>
                    <dd className="count-confirmation-quantity">
                        {display(values.contado)}
                    </dd>
                </div>
                <div>
                    <dt>Observada em (Brasília)</dt>
                    <dd>{formatDate(values.observadoEm)}</dd>
                </div>
                <div className="count-confirmation-unit">
                    <dt>UUID da unidade</dt>
                    <dd>{display(values.codigoUnidade)}</dd>
                </div>
            </dl>
            <section
                className="count-confirmation-reason"
                aria-label="Motivo da contagem"
            >
                <h3>Motivo / justificativa</h3>
                <p>{display(values.motivo)}</p>
            </section>
            <p className="count-confirmation-notice">
                {information.countConfirmation.footer}
            </p>
            <details className="count-confirmation-technical">
                <summary>Dados técnicos da contagem</summary>
                <dl className="count-confirmation-context">
                    <div>
                        <dt>Versão da unidade</dt>
                        <dd>{display(values.versaoUnidade)}</dd>
                    </div>
                    <div>
                        <dt>Identificador da operação</dt>
                        <dd>{display(values.operacaoId)}</dd>
                    </div>
                    <div className="count-confirmation-unit">
                        <dt>Instante informado</dt>
                        <dd>{display(values.observadoEm)}</dd>
                    </div>
                </dl>
            </details>
        </div>
    );
}
