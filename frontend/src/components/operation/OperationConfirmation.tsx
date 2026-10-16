import { endpoint, type Values } from "../../contracts/runtime";
import { automaticCodes } from "../../domain/automaticCodes";
import { operationContextFields } from "../../domain/operationContextFields";
import { actionLabel } from "../../domain/journeys";
import { Dialog } from "../layout/Dialog";
import { Result } from "../Result";
import { confirmedWithoutDivergence } from "../../modules/recebimento/finalization";
import { ReferenceValue } from "../context/ReferencePresentation";
import { InitialLoadConfirmation } from "../../modules/regularizacao/InitialLoadConfirmation";
import { CountConfirmation } from "../../modules/estoque/CountConfirmation";
import { ServiceFactConfirmation } from "../../modules/financeiro/ServiceFactConfirmation";
import { information } from "../../content/information";
import { useLayoutEffect, useRef } from "react";
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
    const initialLoad = id === "CargaInicialController.confirmar";
    const count = id === "ContagemController.contar";
    const serviceFact = id === "FatoServicoController.registrar";
    const confirmation = useRef<HTMLElement>(null);
    useLayoutEffect(() => {
        if (!initialLoad && !count && !serviceFact) return;
        const section = confirmation.current;
        const dialog = section?.closest("dialog");
        if (dialog) dialog.scrollTop = 0;
        section
            ?.querySelector<HTMLButtonElement>(".actions > button")
            ?.focus({ preventScroll: true });
    }, [initialLoad, count, serviceFact]);
    const content = (
        <section
            ref={confirmation}
            className={
                "confirmation" +
                (count ? " confirmation--count" : "") +
                (serviceFact ? " confirmation--service-fact" : "")
            }
            aria-label="Confirmação da ação"
        >
            {initialLoad ? (
                <InitialLoadConfirmation context={context} values={values} />
            ) : count ? (
                <CountConfirmation context={context} values={values} />
            ) : serviceFact ? (
                <ServiceFactConfirmation values={values} />
            ) : (
                <>
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
                        Confira os dados do formulário antes de confirmar. O
                        resultado depende da confirmação do servidor ou da
                        resposta fictícia identificada.
                    </p>
                    <details open>
                        <summary>Dados para conferência</summary>
                        <Result
                            type={endpoint(id).request ?? id}
                            data={Object.fromEntries(
                                Object.entries(values).filter(
                                    ([key, value]) =>
                                        !(
                                            id ===
                                                "PedidoEntradaController.efetivar" &&
                                            key === "aceitarDivergencias" &&
                                            confirmedWithoutDivergence(
                                                context,
                                                values.id,
                                                values.versao,
                                            )
                                        ) &&
                                        !operationContextFields(id).includes(
                                            key,
                                        ) &&
                                        (key !==
                                            automaticCodes[
                                                endpoint(id).request ?? ""
                                            ] ||
                                            value != null),
                                ),
                            )}
                        />
                    </details>
                </>
            )}
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
            title={
                initialLoad
                    ? "Confirmar carga inicial"
                    : count
                      ? information.countConfirmation.title
                      : serviceFact
                        ? information.serviceFactConfirmation.title
                        : `Confirmar ${actionLabel(id).toLowerCase()}?`
            }
            className={
                initialLoad
                    ? "initial-load-confirmation-dialog"
                    : count
                      ? "count-confirmation-dialog"
                      : serviceFact
                        ? "service-fact-confirmation-dialog"
                        : undefined
            }
            icon={
                initialLoad
                    ? "unidades"
                    : count
                      ? "contagem"
                      : serviceFact
                        ? "servicos"
                        : undefined
            }
            onClose={onBack}
        >
            {content}
        </Dialog>
    );
}
