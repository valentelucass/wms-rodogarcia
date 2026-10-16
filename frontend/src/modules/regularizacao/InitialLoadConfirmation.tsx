import type { Values } from "../../contracts/runtime";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { information } from "../../content/information";
import { display } from "../../domain/labels";
import { StatusBadge } from "../../design-system/StatusBadge";

export function InitialLoadConfirmation({
    context,
    values,
}: {
    context: Values;
    values: Values;
}) {
    const labels = Array.isArray(values.etiquetasUnidades)
        ? values.etiquetasUnidades
        : undefined;
    return (
        <div className="initial-load-confirmation">
            <p className="initial-load-confirmation-intro">
                {information.initialLoadConfirmation.description}
            </p>
            <dl className="initial-load-values initial-load-confirmation-context">
                <div>
                    <dt>Carga inicial</dt>
                    <dd className="initial-load-confirmation-subject">
                        <ReferenceValue
                            field="cargaInicialId"
                            value={values.id}
                        />
                        {context.situacao != null && (
                            <StatusBadge value={context.situacao} />
                        )}
                    </dd>
                </div>
                <div>
                    <dt>Revisão em conferência</dt>
                    <dd>{display(values.revisao)}</dd>
                </div>
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
            <section className="initial-load-confirmation-section">
                <h3>Leitura da etiqueta fornecida</h3>
                <p className="initial-load-confirmation-reading">
                    {display(values.leitura)}
                </p>
            </section>
            <section className="initial-load-confirmation-section">
                <header>
                    <h3>Etiquetas das unidades</h3>
                    {labels && (
                        <span className="hint">
                            {labels.length}{" "}
                            {labels.length === 1 ? "etiqueta" : "etiquetas"}
                        </span>
                    )}
                </header>
                {labels?.length ? (
                    <ol className="initial-load-labels">
                        {labels.map((value, i) => (
                            <li key={i}>
                                <span className="hint">Etiqueta {i + 1}</span>
                                <span>{display(value)}</span>
                            </li>
                        ))}
                    </ol>
                ) : (
                    <p className="hint">
                        {labels
                            ? "Nenhuma etiqueta de unidade informada."
                            : "Etiquetas das unidades não informadas."}
                    </p>
                )}
            </section>
            <section className="initial-load-confirmation-section">
                <h3>Motivo / justificativa</h3>
                <p className="initial-load-source">{display(values.motivo)}</p>
            </section>
            <details className="initial-load-technical">
                <summary>Dados técnicos da revisão</summary>
                <p className="hint">
                    {information.initialLoadConfirmationTechnical.description}
                </p>
                <dl className="initial-load-values">
                    <div>
                        <dt>Identificador da carga</dt>
                        <dd>{display(values.id)}</dd>
                    </div>
                    <div>
                        <dt>Versão da carga</dt>
                        <dd>{display(values.versao)}</dd>
                    </div>
                    <div className="initial-load-confirmation-hash">
                        <dt>SHA-256 do conteúdo</dt>
                        <dd>{display(values.conteudoHash)}</dd>
                    </div>
                </dl>
            </details>
        </div>
    );
}
