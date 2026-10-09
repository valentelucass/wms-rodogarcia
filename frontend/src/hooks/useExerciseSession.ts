import { useState } from "react";
import { FictitiousTransport, type Scenario } from "../api/fictitious";
import type { Receipt, Request } from "../api/client";
import type { Perfil, Values } from "../contracts/runtime";
import { absorb, emptyWorkflow } from "../domain/workflow";

// Exercise state only: identity-provider login/token routes do not exist here.
export function useExerciseSession() {
    const [perfil, setPerfil] = useState<Perfil>("GESTOR");
    const [transport, setTransport] = useState(
        () => new FictitiousTransport("GESTOR"),
    );
    const [context, setContext] = useState<Values>({
        clienteId: "1",
        armazemId: "1",
    });
    const [client, setClient] = useState("1"),
        [warehouse, setWarehouse] = useState("1");
    const [active, setActive] = useState(true),
        [revision, setRevision] = useState(0);
    const [scenario, setScenario] = useState<Scenario>("sucesso");
    const [message, setMessage] = useState("");
    const [workflow, setWorkflow] = useState(emptyWorkflow);
    const reset = () => {
        setWorkflow(emptyWorkflow());
        setRevision((x) => x + 1);
    };
    const changePerfil = (v: Perfil) => {
        const next = new FictitiousTransport(v);
        next.scenario = scenario;
        setPerfil(v);
        setTransport(next);
        setActive(true);
        reset();
    };
    const applyContext = () => {
        const valid = (v: string) =>
            /^[1-9]\d*$/.test(v) && BigInt(v) <= 9223372036854775807n;
        if (!valid(client) || !valid(warehouse)) {
            setMessage("Informe IDs positivos dentro do domínio Long.");
            return;
        }
        setContext({ clienteId: client, armazemId: warehouse });
        reset();
        setMessage(
            "Contexto fictício alterado; dados e formulários anteriores descartados.",
        );
    };
    const exit = () => {
        setActive(false);
        setTransport(new FictitiousTransport(perfil));
        reset();
    };
    const changeScenario = (v: Scenario) => {
        setScenario(v);
        transport.scenario = v;
    };
    const onRecord = (v: Values, type: string) =>
        setWorkflow((old) => absorb(old, type, v, true));
    const onReceipt = (
        r: Receipt,
        type: string,
        _id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => setWorkflow((old) => absorb(old, type, r.data, false, request));
    return {
        perfil,
        transport,
        context,
        client,
        setClient,
        warehouse,
        setWarehouse,
        active,
        revision,
        scenario,
        message,
        workflow,
        changePerfil,
        applyContext,
        exit,
        changeScenario,
        onRecord,
        onReceipt,
    };
}
export type ExerciseSession = ReturnType<typeof useExerciseSession>;
