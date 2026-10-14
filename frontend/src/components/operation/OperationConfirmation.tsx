import { endpoint, type Values } from "../../contracts/runtime";
import { automaticCodes } from "../../domain/automaticCodes";
import { operationContextFields } from "../../domain/operationContextFields";
import { actionLabel } from "../../domain/journeys";
import { Dialog } from "../layout/Dialog";
import { Result } from "../Result";
import { confirmedWithoutDivergence } from "../../modules/recebimento/finalization";
import { ReferenceValue } from "../context/ReferencePresentation";
export function OperationConfirmation({
    id,
    context,
    onConfirm,
    onBack,
    values,
    embedded = false,
}: {
    id: string;
    context: Values;
    onConfirm: () => void;
    onBack: () => void;
    values: Values;
    embedded?: boolean;
}) {
    const content = (
        <section className="confirmation" aria-label="Confirmação da ação">
            <p>
                {Boolean(context.clienteId && context.armazemId) && (
                    <>
                        <ReferenceValue
                            field="clienteId"
                            value={context.clienteId}
                        />{" "}
                        ·{" "}
                        <ReferenceValue
                            field="armazemId"
                            value={context.armazemId}
                        />
                        .{" "}
                    </>
                )}
                Confira os dados do formulário antes de confirmar. O resultado
                depende da confirmação do servidor ou da resposta fictícia
                identificada.
            </p>
            <details open>
                <summary>Dados para conferência</summary>
                <Result
                    type={endpoint(id).request ?? id}
                    data={Object.fromEntries(
                        Object.entries(values).filter(
                            ([key, value]) =>
                                !(
                                    id === "PedidoEntradaController.efetivar" &&
                                    key === "aceitarDivergencias" &&
                                    confirmedWithoutDivergence(
                                        context,
                                        values.id,
                                        values.versao,
                                    )
                                ) &&
                                !operationContextFields(id).includes(key) &&
                                (key !==
                                    automaticCodes[
                                        endpoint(id).request ?? ""
                                    ] ||
                                    value != null),
                        ),
                    )}
                />
            </details>
            <div className="actions">
                <button type="button" onClick={onBack}>
                    Voltar à conferência
                </button>
                <button className="primary" type="button" onClick={onConfirm}>
                    Confirmar agora
                </button>
            </div>
        </section>
    );
    return embedded ? (
        content
    ) : (
        <Dialog
            title={`Confirmar ${actionLabel(id).toLowerCase()}?`}
            onClose={onBack}
        >
            {content}
        </Dialog>
    );
}
