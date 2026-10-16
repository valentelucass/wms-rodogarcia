import { expect, it, vi } from "vitest";
import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
} from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../src/api/client";
import {
    initialLoadSample as load,
    initialLoadReferences as refs,
} from "./support/initialLoad";

function setup(profile: "SUPERVISOR" | "OPERACAO" = "SUPERVISOR") {
    const requests: Request[] = [],
        onSelect = vi.fn();
    let resolve!: (receipt: Receipt) => void, reject!: (error: Error) => void;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            return new Promise<Receipt>((done, fail) => {
                resolve = done;
                reject = fail;
            });
        },
    };
    render(
        <FormReferences.Provider
            value={{
                ...refs,
                options: {
                    ...refs.options,
                    id: [{ value: load.id, title: load.referencia }],
                },
            }}
        >
            <Operation
                id="CargaInicialController.consultar"
                context={{ id: load.id }}
                transport={transport}
                perfil={profile}
                onSelect={onSelect}
                inDialog
                hideHeading
            />
        </FormReferences.Provider>,
    );
    return {
        requests,
        onSelect,
        done: (ficticio = true) =>
            resolve({
                data: load,
                raw: "",
                ficticio,
                replay: false,
                requestId: "CONSULTA-FICTICIA-ALFA-01",
            }),
        fail: () =>
            reject(
                new ApiError(
                    "Carga não encontrada nesta consulta.",
                    404,
                    "NAO_ENCONTRADA",
                    "CONSULTA-FICTICIA-ALFA-02",
                ),
            ),
    };
}

it("consulta pelo ID exato e apresenta a carga com uma única seleção do objeto completo", async () => {
    const state = setup();
    expect(
        screen.getByLabelText("Carga inicial *", { exact: true }),
    ).toHaveValue(load.id);
    fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
    expect(state.requests).toHaveLength(1);
    expect(state.requests[0].endpoint.method).toBe("GET");
    expect(state.requests[0].params.id).toBe(load.id);
    expect(
        screen.getByLabelText("Carga inicial *", { exact: true }),
    ).toBeDisabled();
    expect(
        screen.queryByRole("button", { name: "Confirmar agora" }),
    ).not.toBeInTheDocument();
    await act(async () => state.done());
    expect(
        await screen.findByRole("heading", { name: "Carga consultada" }),
    ).toBeInTheDocument();
    expect(
        screen.getByRole("article", { name: "Unidade 1" }),
    ).toHaveTextContent("Pallet");
    expect(
        screen.queryByRole("button", {
            name: /Selecionar registro 1|Ver registro 1/,
        }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Resposta FICTÍCIA");
    expect(
        screen.getByText("Dados da consulta").closest("details"),
    ).not.toHaveAttribute("open");
    expect(screen.getByText("CONSULTA-FICTICIA-ALFA-01")).toBeInTheDocument();
    fireEvent.click(
        screen.getByRole("button", { name: "Selecionar esta carga" }),
    );
    expect(state.onSelect).toHaveBeenCalledExactlyOnceWith(
        load,
        "CargaInicialDto.Resultado",
    );
    expect(state.requests).toHaveLength(1);
});

it("identifica o resultado anterior durante atualização e retira o resultado após erro, permitindo recuperar", async () => {
    const state = setup();
    fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
    await act(async () => state.done(false));
    expect(screen.getByRole("status")).toHaveTextContent("Consulta concluída.");
    expect(screen.getByRole("status")).not.toHaveTextContent("FICTÍCIA");
    fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
    expect(
        screen.getByRole("heading", {
            name: "Resultado anterior · aguardando atualização",
        }),
    ).toBeInTheDocument();
    expect(
        screen.getByRole("button", { name: "Selecionar esta carga" }),
    ).toBeDisabled();
    await act(async () => state.fail());
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Carga não encontrada",
    );
    expect(screen.getByRole("alert")).toHaveTextContent(
        "CONSULTA-FICTICIA-ALFA-02",
    );
    expect(
        screen.queryByRole("heading", { name: "Carga consultada" }),
    ).not.toBeInTheDocument();
    expect(
        screen.queryByRole("button", { name: "Selecionar esta carga" }),
    ).not.toBeInTheDocument();
    expect(
        screen.getByLabelText("Carga inicial *", { exact: true }),
    ).toHaveValue(load.id);
    fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
    await act(async () => state.done());
    await waitFor(() =>
        expect(
            screen.getByRole("heading", { name: "Carga consultada" }),
        ).toBeInTheDocument(),
    );
    expect(state.requests).toHaveLength(3);
});

it("não apresenta a consulta nem envia solicitação para Operação", () => {
    const state = setup("OPERACAO");
    expect(screen.getByRole("status")).toHaveTextContent("SUPERVISOR");
    expect(
        screen.queryByRole("button", { name: "Consultar" }),
    ).not.toBeInTheDocument();
    expect(state.requests).toHaveLength(0);
});
