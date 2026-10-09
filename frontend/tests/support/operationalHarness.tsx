import { useState } from "react";
import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { expect } from "vitest";
import { JourneyPage } from "../../src/components/JourneyPage";
import { Navigation } from "../../src/components/shell/Navigation";
import { journeys } from "../../src/domain/journeys";
import { useExerciseSession } from "../../src/hooks/useExerciseSession";
import type { Perfil, Values } from "../../src/contracts/runtime";
import type { Transport, Request } from "../../src/api/client";
import { encode, stringifyExact, toWire } from "../../src/contracts/codec";
import { fixture } from "../../src/api/mock/fixtures";
import { writeFileSync } from "node:fs";
export const obj = (type: string, patch: Values = {}) => ({
    ...(fixture(type) as Values),
    ...patch,
});
export function Harness({
    transport,
    page,
    action,
    perfil = "GESTOR",
}: {
    transport: Transport;
    page: string;
    action: string;
    perfil?: Perfil;
}) {
    const s = useExerciseSession();
    const [navigation, setNavigation] = useState({ page, action, revision: 0 });
    return (
        <>
            <Navigation
                page={navigation.page}
                navigate={(page, action = "") =>
                    setNavigation({
                        page,
                        action,
                        revision: navigation.revision + 1,
                    })
                }
            />
            <JourneyPage
                key={navigation.revision}
                journey={journeys.find((j) => j.id === navigation.page)!}
                transport={transport}
                context={s.context}
                perfil={perfil}
                workflow={s.workflow}
                onRecord={s.onRecord}
                onReceipt={s.onReceipt}
                startAction={navigation.action}
                onNavigate={(next) =>
                    setNavigation({
                        ...next,
                        revision: navigation.revision + 1,
                    })
                }
            />
        </>
    );
}
export const click = (name: string) =>
    fireEvent.click(screen.getByRole("button", { name }));
export function selectResultRow(entity: string, id: string) {
    const table = screen.getByRole("table", {
        name: new RegExp("^" + entity + " ·"),
    });
    const row = within(table).getByRole("cell", { name: id }).closest("tr");
    const button = row?.querySelector("td:last-child > button");
    if (!button)
        throw Error(
            "Seleção da entidade consultada ausente: " + entity + " " + id,
        );
    fireEvent.click(button);
}
export const fill = (name: string, value: string) =>
    fireEvent.change(screen.getByLabelText(name, { exact: true }), {
        target: { value },
    });
export async function confirm() {
    click("Conferir e confirmar");
    const validation = screen.queryByRole("alert");
    if (validation) throw Error("Validação visível: " + validation.textContent);
    await screen.findByRole("region", { name: "Confirmação da ação" });
    click("Confirmar agora");
    await waitFor(() =>
        expect(
            screen.queryByRole("button", { name: "Aguardando confirmação…" }),
        ).toBeNull(),
    );
    await screen.findByText(/Resposta FICTÍCIA de exercício recebida/);
}
export async function consult() {
    click("Consultar");
    await waitFor(() =>
        expect(
            screen.queryByRole("button", { name: "Aguardando confirmação…" }),
        ).toBeNull(),
    );
    await screen.findByText(/Resposta FICTÍCIA de exercício recebida/);
}
export function selected(name: string, value: string) {
    expect(screen.getByLabelText(name, { exact: true })).toHaveValue(value);
}
export const body = (r: Request) => r.body as Values;
export const wire = (r: Request) => encode(r.endpoint.request!, r.body);
export function receipt(r: Request, data: unknown) {
    return {
        data,
        raw: stringifyExact(toWire(r.endpoint.response, data)),
        requestId: "local-ficticio",
        ficticio: true,
        replay: false,
    };
}
export async function waitRequest(t: { requests: Request[] }, id: string) {
    await waitFor(() => expect(t.requests.at(-1)?.endpoint.id).toBe(id));
    return t.requests.at(-1)!;
}
export function saveProof(name: string, requests: Request[]) {
    const stamp = new Date().toISOString().replace(/[:.]/g, "-");
    writeFileSync(
        "evidencias/marco02-ui-" + name + "-" + stamp + ".json",
        JSON.stringify(
            {
                name,
                at: new Date().toISOString(),
                ui: document.body.textContent,
                requests: requests.map((r) => ({
                    id: r.endpoint.id,
                    route: r.endpoint.path,
                    params: r.params,
                    query: r.query,
                    wire: r.endpoint.request ? wire(r) : null,
                    file: r.file
                        ? { name: r.file.name, size: r.file.size }
                        : null,
                })),
                boundary:
                    "componentes React reais + transport ficticio; zero HTTP/backend",
            },
            null,
            2,
        ),
    );
}
