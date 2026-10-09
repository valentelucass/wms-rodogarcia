import type { Values } from "../../contracts/runtime";
import { actionLabel } from "../../domain/journeys";
export function OperationConfirmation({
    id,
    context,
    onConfirm,
    onBack,
}: {
    id: string;
    context: Values;
    onConfirm: () => void;
    onBack: () => void;
}) {
    return (
        <section className="confirmation" aria-label="Confirmação da ação">
            <h4>Confirmar {actionLabel(id).toLowerCase()}?</h4>
            <p>
                Cliente {String(context.clienteId)} · Armazém{" "}
                {String(context.armazemId)}. Confira os dados acima. O resultado
                depende da confirmação do servidor ou da resposta fictícia
                identificada.
            </p>
            <button className="primary" type="button" onClick={onConfirm}>
                Confirmar agora
            </button>
            <button type="button" onClick={onBack}>
                Voltar à conferência
            </button>
        </section>
    );
}
