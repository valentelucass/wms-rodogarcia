import type { Workflow } from "../../domain/workflow";
import type { Values } from "../../contracts/runtime";
import { auditTargets } from "./auditTargets";
import { businessTitle } from "../../domain/referencePresentation";
import { information } from "../../content/information";
import { Icon } from "../../design-system/Icon";
export function AuditSelection({
    workflow,
    onSelect,
    disabled,
}: {
    workflow: Workflow;
    onSelect: (v: Values, type: string) => void;
    disabled: boolean;
}) {
    const available = auditTargets.filter(
        (t) => workflow.selected[t.dto]?.id !== undefined,
    );
    return (
        <section
            className="audit-selection"
            aria-label="Registro consultado para auditoria"
        >
            <header className="audit-selection-heading">
                <h2>{information.auditHistory.title}</h2>
                <p>{information.auditHistory.description}</p>
            </header>
            {available.length === 0 && (
                <p>
                    Consulte um cliente abaixo ou selecione um registro em sua
                    jornada.
                </p>
            )}
            {available.length > 0 && (
                <div className="audit-selection-actions">
                    {available.map((t) => {
                        const record = workflow.selected[t.dto];
                        const service =
                            t.dto === "ConfiguracaoCobrancaDto.Servico";
                        const name = service
                            ? record.descricao
                                ? String(record.descricao)
                                : undefined
                            : businessTitle(record, t.dto, workflow.catalogs);
                        const code =
                            service &&
                            record.codigo != null &&
                            record.codigo !== ""
                                ? String(record.codigo)
                                : undefined;
                        return (
                            <article
                                key={t.dto}
                                aria-label={`Registro selecionado: ${t.label}`}
                                className={
                                    "audit-selection-target" +
                                    (t.tipo === "VINCULO_COBRANCA"
                                        ? " audit-selection-target--legacy"
                                        : "")
                                }
                            >
                                <div className="audit-target-main">
                                    <div className="audit-target-summary">
                                        <span className="audit-target-kind">
                                            Registro selecionado · {t.label}
                                        </span>
                                        {name && (
                                            <strong className="audit-target-name">
                                                {name}
                                            </strong>
                                        )}
                                        {(code || !name) && (
                                            <dl className="audit-target-code">
                                                <div>
                                                    <dt>
                                                        {code
                                                            ? "Código"
                                                            : "Identificador"}
                                                    </dt>
                                                    <dd>
                                                        {code ??
                                                            String(record.id)}
                                                    </dd>
                                                </div>
                                            </dl>
                                        )}
                                    </div>
                                    <button
                                        type="button"
                                        className="action-details"
                                        aria-label={`Ver histórico de ${t.label}: ${name ?? (code ? `código ${code}` : `registro ${record.id}`)}`}
                                        disabled={disabled}
                                        onClick={() =>
                                            onSelect(
                                                {
                                                    dto: t.dto,
                                                    registroId: record.id,
                                                },
                                                "Auditoria.alvo",
                                            )
                                        }
                                    >
                                        <Icon name="history" />
                                        Ver histórico
                                    </button>
                                </div>
                                {t.tipo === "VINCULO_COBRANCA" && (
                                    <div className="audit-target-legacy">
                                        <strong>
                                            {
                                                information.auditHistoryLegacy
                                                    .title
                                            }
                                        </strong>
                                        <p>
                                            {
                                                information.auditHistoryLegacy
                                                    .description
                                            }
                                        </p>
                                        <button
                                            type="button"
                                            className="action-details"
                                            disabled={disabled}
                                            onClick={() =>
                                                onSelect(
                                                    {
                                                        dto: t.dto,
                                                        registroId:
                                                            workflow.selected[
                                                                t.dto
                                                            ].id,
                                                        legado: true,
                                                    },
                                                    "Auditoria.alvo",
                                                )
                                            }
                                        >
                                            Consultar legado não atribuído do
                                            vínculo{" "}
                                            {businessTitle(
                                                workflow.selected[t.dto],
                                                t.dto,
                                                workflow.catalogs,
                                            )}
                                        </button>
                                    </div>
                                )}
                            </article>
                        );
                    })}
                </div>
            )}
        </section>
    );
}
