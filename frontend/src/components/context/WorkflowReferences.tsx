import { useContext, type ReactNode } from "react";
import { FormReferences } from "../FormReferences";
import { referenceCatalog, type Workflow } from "../../domain/workflow";
import { businessTitle } from "../../domain/referencePresentation";
import type { Journey } from "../../domain/journeys";
import type { Values } from "../../contracts/runtime";

/** Dialog selections are local until acknowledged; labels must follow that same scope. */
export function WorkflowReferences({
    workflow,
    journey,
    children,
    defaults,
}: {
    workflow: Workflow;
    journey: Journey;
    children: ReactNode;
    defaults?: Values;
}) {
    const parent = useContext(FormReferences);
    const records = Object.fromEntries(
        Object.keys(workflow.catalogs).map((type) => [
            type,
            referenceCatalog(workflow, type),
        ]),
    );
    const options = Object.fromEntries(
        Object.entries(journey.referenceFields).map(([field, type]) => [
            field,
            (records[type] ?? []).map((v) => ({
                value: String(v.id),
                title:
                    businessTitle(v, type, records) ??
                    "Referência indisponível",
            })),
        ]),
    );
    return (
        <FormReferences.Provider
            value={{
                ...parent,
                defaults: { ...parent.defaults, ...defaults },
                records,
                options,
            }}
        >
            {children}
        </FormReferences.Provider>
    );
}
