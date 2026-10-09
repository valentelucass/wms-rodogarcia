import { useOperation, type OperationOptions } from "../hooks/useOperation";
import { OperationForm } from "./operation/OperationForm";
import { OperationConfirmation } from "./operation/OperationConfirmation";
import { OperationFeedback } from "./operation/OperationFeedback";
import { OperationResults } from "./operation/OperationResults";
import { actionLabel, type NextAction } from "../domain/journeys";
import type { Values } from "../contracts/runtime";
import { useLayoutEffect } from "react";
export interface OperationProps extends OperationOptions {
    onSelect: (v: Values, type: string) => void;
    onContinue?: (next: NextAction) => void;
    onSelectionLockChange?: (locked: boolean) => void;
    collector?: boolean;
}
export function Operation(props: OperationProps) {
    const s = useOperation(props);
    const referenceMismatch =
        s.params.id !== undefined && s.params.id !== props.context.id;
    const lock = props.onSelectionLockChange;
    useLayoutEffect(() => {
        lock?.(s.pending || s.uncertain);
        return () => lock?.(false);
    }, [lock, s.pending, s.uncertain]);
    if (!s.allowed)
        return (
            <p role="status">
                Esta ação é apresentada a {s.e.permission}. O backend revalida
                perfil e alcance.
            </p>
        );
    return (
        <section className="operation">
            <h3>{actionLabel(props.id)}</h3>
            <OperationForm
                state={s}
                perfil={props.perfil}
                collector={props.collector}
            />
            {s.confirm && (
                <OperationConfirmation
                    id={props.id}
                    context={props.context}
                    values={{ ...s.params, ...s.query, ...s.body }}
                    onConfirm={() => void s.send()}
                    onBack={s.cancelConfirmation}
                />
            )}
            <OperationFeedback state={s} />
            {s.receipt && (
                <>
                    {referenceMismatch && (
                        <p role="status">
                            O pedido consultado difere da seleção da jornada.
                            Consulte e selecione o pedido desejado antes de
                            continuar.
                        </p>
                    )}
                    <OperationResults
                        id={props.id}
                        receipt={s.receipt}
                        type={s.e.response}
                        collector={props.collector}
                        onSelect={props.onSelect}
                        pending={s.pending}
                        onPage={
                            s.e.method === "GET" &&
                            s.e.query.some((f) => f.name === "pagina")
                                ? s.changePage
                                : undefined
                        }
                        onContinue={
                            referenceMismatch ? undefined : props.onContinue
                        }
                    />
                </>
            )}
        </section>
    );
}
