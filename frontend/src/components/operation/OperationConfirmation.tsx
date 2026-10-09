import type { Values } from "../../contracts/runtime";
import { actionLabel } from "../../domain/journeys";
import { Dialog } from "../layout/Dialog";
import { Result } from "../Result";
export function OperationConfirmation({
    id,
    context,
    onConfirm,
    onBack,
    values,
}: {
    id: string;
    context: Values;
    onConfirm: () => void;
    onBack: () => void;
    values: Values;
}) {
    return (
        <Dialog
            title={`Confirmar ${actionLabel(id).toLowerCase()}?`}
            onClose={onBack}
        >
            <section className="confirmation" aria-label="Confirmação da ação">
                <p>
                    {context.clienteId && context.armazemId
                        ? `Cliente ${String(context.clienteId)} · Armazém ${String(context.armazemId)}. `
                        : ""}
                    Confira os dados do formulário antes de confirmar. O
                    resultado depende da confirmação do servidor ou da resposta
                    fictícia identificada.
                </p>
                <details open>
                    <summary>Dados para conferência</summary>
                    <Result data={values} />
                </details>
                <div className="actions">
                    <button type="button" onClick={onBack}>
                        Voltar à conferência
                    </button>
                    <button
                        className="primary"
                        type="button"
                        onClick={onConfirm}
                    >
                        Confirmar agora
                    </button>
                </div>
            </section>
        </Dialog>
    );
}
