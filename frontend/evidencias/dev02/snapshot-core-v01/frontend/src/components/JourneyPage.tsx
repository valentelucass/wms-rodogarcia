import type { Request } from "../api/client";
import { useState } from "react";
import {
    type Journey,
    type NextAction,
    actionLabel,
    nextActions,
} from "../domain/journeys";
import {
    endpoint,
    canPresent,
    type Perfil,
    type Values,
} from "../contracts/runtime";
import type { Receipt, Transport } from "../api/client";
import {
    operationContext,
    referenceCatalog,
    type Workflow,
} from "../domain/workflow";
import { Operation } from "./Operation";
import { FormReferences, type ReferenceOption } from "./FormReferences";
import { ReferenceLookup } from "./ReferenceLookup";
import { ExpeditionStatus } from "../modules/saida/ExpeditionStatus";
import { AdjustmentOrigin } from "../modules/financeiro/AdjustmentOrigin";
import { ReceivingSummary } from "../modules/recebimento/ReceivingSummary";
import { AuditSelection } from "../modules/relatorios/AuditSelection";
import { DispatchReservations } from "../modules/saida/DispatchReservations";
export function JourneyPage({
    journey,
    transport,
    context,
    perfil,
    workflow,
    onRecord,
    onReceipt,
    startAction,
    onNavigate,
}: {
    journey: Journey;
    transport: Transport;
    context: Values;
    perfil: Perfil;
    workflow: Workflow;
    onRecord: (v: Values, type: string) => void;
    onReceipt: (
        r: Receipt,
        type: string,
        id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => void;
    startAction: string;
    onNavigate: (next: NextAction) => void;
}) {
    const [step, setStep] = useState(
        Math.max(
            0,
            journey.steps.findIndex((s) => s.actions.includes(startAction)),
        ),
    );
    const [action, setAction] = useState(
        startAction ||
            journey.steps[0].actions.find((id) =>
                canPresent(perfil, endpoint(id).permission),
            ) ||
            "",
    );
    const [refresh, setRefresh] = useState(0);
    const [selectionLocked, setSelectionLocked] = useState(false);
    const selectRecord = (v: Values, type: string) => {
        if (!selectionLocked) onRecord(v, type);
    };
    const current = journey.steps[step];
    const actions = current.actions.filter((id) =>
        canPresent(perfil, endpoint(id).permission),
    );
    const derived = action
        ? operationContext(action, context, workflow)
        : context;
    const options: Record<string, ReferenceOption[]> = {};
    for (const [field, type] of Object.entries(journey.referenceFields))
        options[field] = referenceCatalog(workflow, type).map((v) => ({
            value: String(v.id),
            title:
                String(v.id) +
                " · " +
                String(
                    v.sku ??
                        v.codigo ??
                        v.referencia ??
                        v.descricao ??
                        v.situacao ??
                        type.split(".")[0],
                ),
        }));
    const references = journey.references;
    return (
        <>
            <h1>{journey.title}</h1>
            {["saida", "fiscal"].includes(journey.id) && (
                <DispatchReservations
                    workflow={workflow}
                    onSelect={selectRecord}
                    disabled={selectionLocked}
                />
            )}
            {journey.id === "entrada" && (
                <ReceivingSummary workflow={workflow} />
            )}
            {journey.id === "relatorios" && perfil === "GESTOR" && (
                <AuditSelection
                    workflow={workflow}
                    onSelect={selectRecord}
                    disabled={selectionLocked}
                />
            )}
            {journey.id === "fechamento" && (
                <AdjustmentOrigin
                    workflow={workflow}
                    onPin={selectRecord}
                    disabled={selectionLocked}
                />
            )}
            {journey.id === "fiscal" && (
                <ExpeditionStatus workflow={workflow} />
            )}
            <section
                className="selection"
                aria-label="Referências confirmadas da jornada"
            >
                <h2>Referências da jornada no contexto atual</h2>
                <ul>
                    {references
                        .filter(([, t]) => workflow.selected[t])
                        .map(([name, t]) => {
                            const v = workflow.selected[t];
                            return (
                                <li key={t}>
                                    {name}:{" "}
                                    {String(
                                        v.id ??
                                            v.chaveFato ??
                                            "referência consultada",
                                    )}{" "}
                                    {v.referencia ? String(v.referencia) : ""}{" "}
                                    {v.versao !== undefined
                                        ? "· Revisão " + String(v.versao)
                                        : ""}{" "}
                                    {v.numero !== undefined
                                        ? "· Versão " + String(v.numero)
                                        : ""}{" "}
                                    {v.situacao
                                        ? "· " + String(v.situacao)
                                        : ""}
                                </li>
                            );
                        })}
                </ul>
                <p>
                    Uma lista com vários registros exige seleção explícita. IDs
                    de pedido, entrada, unidade, reserva e fechamento permanecem
                    separados.
                </p>
            </section>
            <ReferenceLookup
                journey={journey}
                context={derived}
                transport={transport}
                onRecord={selectRecord}
                selectionLocked={selectionLocked}
                onReceipt={onReceipt}
            />
            {selectionLocked && (
                <p role="status">
                    Aguarde a confirmação ou consulte o resultado incerto. As
                    referências do comando enviado permanecem conservadas.
                </p>
            )}
            <button
                type="button"
                disabled={selectionLocked}
                onClick={() => setRefresh((x) => x + 1)}
            >
                Usar referências consultadas no formulário (descarta edição
                atual)
            </button>
            <nav className="steps" aria-label={"Etapas de " + journey.title}>
                {journey.steps.map((s, i) => (
                    <button
                        key={s.title}
                        type="button"
                        disabled={selectionLocked}
                        aria-current={i === step ? "step" : undefined}
                        onClick={() => {
                            setStep(i);
                            setAction(
                                s.actions.find((id) =>
                                    canPresent(perfil, endpoint(id).permission),
                                ) ?? "",
                            );
                        }}
                    >
                        {s.title}
                    </button>
                ))}
            </nav>
            <section>
                <h2>{current.title}</h2>
                <p>{current.help}</p>
                <div
                    className="action-tabs"
                    role="group"
                    aria-label="Ações desta etapa"
                >
                    {actions.map((id) => (
                        <button
                            key={id}
                            type="button"
                            disabled={selectionLocked}
                            aria-pressed={id === action}
                            onClick={() => setAction(id)}
                        >
                            {actionLabel(id)}
                        </button>
                    ))}
                </div>
                {action ? (
                    <FormReferences.Provider
                        value={{
                            defaults: derived,
                            options,
                            records: Object.fromEntries(
                                Object.keys(workflow.catalogs).map((type) => [
                                    type,
                                    referenceCatalog(workflow, type),
                                ]),
                            ),
                        }}
                    >
                        <Operation
                            key={
                                action +
                                ":" +
                                refresh +
                                ":" +
                                workflow.selectionRevision
                            }
                            id={action}
                            transport={transport}
                            context={derived}
                            perfil={perfil}
                            onSelect={selectRecord}
                            onSelectionLockChange={setSelectionLocked}
                            onReceipt={onReceipt}
                            onContinue={
                                nextActions[action] ? onNavigate : undefined
                            }
                        />
                    </FormReferences.Provider>
                ) : (
                    <p>
                        Nenhuma ação desta etapa é apresentada ao perfil atual.
                        Selecione outra etapa.
                    </p>
                )}
            </section>
        </>
    );
}
