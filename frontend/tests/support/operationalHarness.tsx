import { useState } from "react";
import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { expect, vi } from "vitest";
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
    vi.spyOn(window, "confirm").mockReturnValue(true);
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
const activeQueries = () => {
    const dialogs = screen.queryAllByRole("dialog");
    return dialogs.length ? within(dialogs.at(-1)!) : screen;
};
export async function click(name: string) {
    const alias: Record<string, string> = {
        "Corrigir descrição": "Editar",
        "Registrar contagem física": "Nova contagem",
        "Solicitar cálculo ao servidor": "Calcular serviços",
        "Solicitar encerramento": "Solicitar desativação de cliente",
    };
    name = alias[name] ?? name;
    if (/^Selecionar registro \d+$/.test(name))
        await waitFor(() =>
            expect(
                activeQueries().queryByText("Consultando referências…"),
            ).toBeNull(),
        );
    if (
        name === "Consultar lista" &&
        document.querySelector(".record-workspace")
    ) {
        const close = activeQueries().queryByRole("button", {
            name: /Voltar à lista|Fechar detalhes/,
        });
        if (close) fireEvent.click(close);
        await consult();
        return;
    }
    const lookup = activeQueries().queryByRole("region", {
        name: "Referências consultadas",
    });
    let button = lookup
        ? within(lookup).queryByRole("button", { name })
        : undefined;
    button ??=
        activeQueries().queryAllByRole("button", { name }).at(0) ??
        screen.queryAllByRole("button", { name }).at(0);
    if (!button && /^Selecionar registro \d+$/.test(name)) {
        const index = Number(name.split(" ").at(-1)) - 1;
        const links = await screen.findAllByRole("button", {
            name: /^Ver detalhes de /,
        });
        fireEvent.click(links[index]);
        await loadedRecord();
        return;
    }
    if (!button) {
        const scalar = screen.queryByRole("button", {
            name: "Operações deste registro",
        });
        if (scalar) {
            fireEvent.click(scalar);
            await loadedRecord();
            button = await activeQueries().findByRole("button", { name });
        }
    }
    if (!button) {
        const link = (
            await screen.findAllByRole("button", { name: /^Ver detalhes de / })
        )[0];
        fireEvent.click(link);
        await loadedRecord();
        if (name === "Consultar detalhe") return;
        button = await activeQueries().findByRole("button", { name });
    }
    fireEvent.click(button);
    await waitFor(() =>
        expect(
            screen.queryByText(
                /Carregando os dados atuais do registro|Atualizando notas, itens e conferência/,
            ),
        ).toBeNull(),
    );
}
export const activeRecord = activeQueries;
async function loadedRecord() {
    await waitFor(() =>
        expect(
            screen.queryByText(
                /Carregando os dados atuais do registro|Atualizando notas, itens e conferência/,
            ),
        ).toBeNull(),
    );
    const failure = activeQueries().queryByRole("alert");
    if (failure) throw Error(failure.textContent ?? "Falha no detalhe");
}
export async function selectResultRow(entity: string, id: string) {
    const table = screen.getByRole("table", {
        name: new RegExp("^" + entity + " ·"),
    });
    const row = within(table).getByRole("cell", { name: id }).closest("tr");
    const button = row?.querySelector("td:last-child button");
    if (!button)
        throw Error(
            "Seleção da entidade consultada ausente: " + entity + " " + id,
        );
    fireEvent.click(button);
    await loadedRecord();
}
export const fill = (name: string, value: string) =>
    fireEvent.change(activeQueries().getByLabelText(name, { exact: true }), {
        target: { value },
    });
export async function confirm() {
    await click("Conferir e confirmar");
    const validation = activeQueries().queryByRole("alert");
    if (validation) throw Error("Validação visível: " + validation.textContent);
    await screen.findByRole("region", { name: "Confirmação da ação" });
    await click("Confirmar agora");
    await waitFor(() =>
        expect(
            screen.queryByRole("button", { name: "Aguardando confirmação…" }),
        ).toBeNull(),
    );
    await activeQueries().findByText(/Resposta FICTÍCIA de exercício recebida/);
    await loadedRecord();
}
export async function consult() {
    const button = activeQueries().queryByRole("button", { name: "Consultar" });
    if (!button) {
        const apply = document.querySelector(".record-filters button.primary");
        if (
            apply &&
            screen.queryByText(
                "Informe as referências obrigatórias nos filtros para carregar esta visão.",
            )
        )
            fireEvent.click(apply);
        await waitFor(() =>
            expect(
                screen.queryByText(
                    /Carregando registros|Atualizando os registros apresentados/,
                ),
            ).toBeNull(),
        );
        return;
    }
    fireEvent.click(button);
    await waitFor(() =>
        expect(
            screen.queryByRole("button", { name: "Consultando…" }),
        ).toBeNull(),
    );
    await activeQueries().findByText(/Resposta FICTÍCIA de exercício recebida/);
}
export function selected(name: string, value: string) {
    const input = activeQueries().queryByLabelText(name, { exact: true });
    if (input) expect(input).toHaveValue(value);
    else
        expect(
            activeQueries().getAllByText(value, { exact: true, selector: "dd" })
                .length,
        ).toBeGreaterThan(0);
}
export const lastMutation = (requests: Request[]) =>
    requests.filter((request) => request.endpoint.method !== "GET").at(-1)!;
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
