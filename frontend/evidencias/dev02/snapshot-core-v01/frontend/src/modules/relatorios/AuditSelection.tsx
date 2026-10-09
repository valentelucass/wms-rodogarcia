import type { Workflow } from "../../domain/workflow";
import type { Values } from "../../contracts/runtime";
import { auditTargets } from "./auditTargets";
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
        <section aria-label="Registro consultado para auditoria">
            <h2>Auditoria de um registro consultado</h2>
            <p>
                Selecione o cadastro ou pedido para consultar seu histórico
                preservado.
            </p>
            {available.length === 0 && (
                <p>
                    Consulte um cliente abaixo ou selecione um registro em sua
                    jornada.
                </p>
            )}
            {available.map((t) => (
                <button
                    key={t.dto}
                    type="button"
                    disabled={disabled}
                    onClick={() =>
                        onSelect(
                            {
                                dto: t.dto,
                                registroId: workflow.selected[t.dto].id,
                            },
                            "Auditoria.alvo",
                        )
                    }
                >
                    Auditar {t.label} {String(workflow.selected[t.dto].id)}
                </button>
            ))}
        </section>
    );
}
