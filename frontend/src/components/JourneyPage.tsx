import type { Request } from "../api/client";
import { useState } from "react";
import { type Journey, type NextAction } from "../domain/journeys";
import { type Perfil, type Values } from "../contracts/runtime";
import type { Receipt, Transport } from "../api/client";
import {
    operationContext,
    referenceCatalog,
    type Workflow,
} from "../domain/workflow";
import { RecordWorkspace } from "./records/RecordWorkspace";
import { recordPages } from "../domain/recordPages";
import { canLeavePage } from "../domain/pageLeave";
import { PageHeader } from "./layout/PageHeader";
import { Icon } from "../design-system/Icon";
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
    const selectionLocked = false;
    const selectRecord = (v: Values, type: string) => onRecord(v, type);
    const derived = operationContext(
        recordPages[journey.id][step].source,
        context,
        workflow,
    );
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
    if (journey.id === "entrada")
        return <>
            <PageHeader title="Entrada e conferência" icon="entrada" description="Encontre o pedido e acompanhe notas, itens, conferência e efetivação no mesmo contexto." />
            <FormReferences.Provider value={{ defaults: derived, options, records: Object.fromEntries(Object.keys(workflow.catalogs).map((type) => [type, referenceCatalog(workflow, type)])) }}>
                <RecordWorkspace key={String(context.clienteId) + ":" + String(context.armazemId) + ":" + perfil} journey={journey} step={0} transport={transport} context={context} perfil={perfil} workflow={workflow} onRecord={onRecord} onReceipt={onReceipt} startAction={startAction} onNavigate={onNavigate} />
            </FormReferences.Provider>
        </>;
    return (
        <>
            <PageHeader
                title={journey.title}
                icon={journey.id}
                description="Selecione uma etapa, consulte os dados e confira a operação antes de confirmar."
            />
            <div className="journey-workspace">
                <aside className="journey-sidebar">
                    <div className="workspace-panel journey-navigation">
                        <h2>Etapas da jornada</h2>
                        <nav
                            className="steps"
                            aria-label={"Etapas de " + journey.title}
                        >
                            {journey.steps.map((s, i) => (
                                <button
                                    key={s.title}
                                    type="button"
                                    disabled={selectionLocked}
                                    aria-current={
                                        i === step ? "step" : undefined
                                    }
                                    onClick={() => {
                                        if (canLeavePage()) setStep(i);
                                    }}
                                >
                                    <span
                                        className="step-number"
                                        aria-hidden="true"
                                    >
                                        {i + 1}
                                    </span>
                                    <span>{s.title}</span>
                                </button>
                            ))}
                        </nav>
                    </div>
                    <div
                        className="workspace-panel journey-fill"
                        aria-hidden="true"
                    />
                </aside>
                <div className="journey-content">
                    <ReferenceLookup
                        journey={journey}
                        context={derived}
                        transport={transport}
                        onRecord={selectRecord}
                        selectionLocked={selectionLocked}
                        onReceipt={onReceipt}
                    />
                    <div className="journey-context">
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
                        {["relatorios", "precos"].includes(journey.id) &&
                            perfil === "GESTOR" && (
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
                    </div>
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
                        <RecordWorkspace
                            key={
                                step +
                                ":" +
                                String(context.clienteId) +
                                ":" +
                                String(context.armazemId) +
                                ":" +
                                perfil
                            }
                            journey={journey}
                            step={step}
                            transport={transport}
                            context={context}
                            perfil={perfil}
                            workflow={workflow}
                            onRecord={onRecord}
                            onReceipt={onReceipt}
                            startAction={
                                journey.steps[step].actions.includes(
                                    startAction,
                                )
                                    ? startAction
                                    : ""
                            }
                            onNavigate={onNavigate}
                        />
                    </FormReferences.Provider>
                    <section
                        className="selection journey-references"
                        aria-label="Referências confirmadas da jornada"
                    >
                        <div className="journey-references-heading">
                            <span className="journey-references-icon">
                                <Icon name="unidades" />
                            </span>
                            <h2>Referências da jornada no contexto atual</h2>
                        </div>
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
                                            {v.referencia
                                                ? String(v.referencia)
                                                : ""}{" "}
                                            {v.versao !== undefined
                                                ? "· Revisão " +
                                                  String(v.versao)
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
                        <div className="journey-references-footer">
                            <p>
                                Uma lista com vários registros exige seleção
                                explícita. IDs de pedido, entrada, unidade,
                                reserva e fechamento permanecem separados.
                            </p>
                            <div className="button-row">
                                {selectionLocked && (
                                    <p role="status">
                                        Aguarde a confirmação ou consulte o
                                        resultado incerto. As referências do
                                        comando enviado permanecem conservadas.
                                    </p>
                                )}
                            </div>
                        </div>
                    </section>
                </div>
            </div>
        </>
    );
}
