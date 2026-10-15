import { it, expect, vi } from "vitest";
import {
    render,
    screen,
    fireEvent,
    waitFor,
    within,
} from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import {
    ApiError,
    type Request,
    type Transport,
    type Receipt,
} from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";
import type { Perfil } from "../src/contracts/runtime";

const uuid = "00000000-0000-4000-8000-000000000002";
const first = "9007199254740993",
    second = "9007199254740995";
function setup({
    hold = false,
    fixed = false,
    perfil = "GESTOR" as Perfil,
} = {}) {
    const requests: Request[] = [];
    let reject!: (error: Error) => void;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            if (hold)
                return new Promise<Receipt>((_resolve, fail) => {
                    reject = fail;
                });
            return {
                data: fixture("AvariaDto.Confirmacao"),
                ficticio: true,
                raw: "",
                replay: false,
                requestId: "DAMAGE-UI-LOCAL",
            };
        },
    };
    render(
        <FormReferences.Provider
            value={{
                defaults: {},
                records: {},
                options: {
                    enderecoId: [
                        { value: first, title: "Posição A01" },
                        { value: second, title: "Posição B02" },
                    ],
                    conjuntoId: [],
                },
            }}
        >
            <Operation
                id="AvariaController.registrar"
                transport={transport}
                context={{ codigo: uuid, versaoUnidade: "7" }}
                perfil={perfil}
                onSelect={vi.fn()}
                onReceipt={vi.fn()}
                fixedFields={fixed ? ["codigo", "versaoUnidade"] : []}
            />
        </FormReferences.Provider>,
    );
    return {
        requests,
        fail: () =>
            reject(
                new ApiError(
                    "Revisão alterada; confira a unidade.",
                    409,
                    "VERSAO_DESATUALIZADA",
                ),
            ),
    };
}
const fill = (name: string, value: string) =>
    fireEvent.change(screen.getByLabelText(name, { exact: true }), {
        target: { value },
    });
function facts() {
    fill("Quantidade *", "0.000001");
    fill("Instante real do fato *", "2026-10-15T10:00:00.123");
    fill(
        "Motivo / justificativa *",
        "Fato fictício preservado para conferência",
    );
}
function add(address = first, code = "A01", index = 1) {
    fireEvent.click(screen.getByRole("button", { name: "Adicionar posição" }));
    const position = within(
        screen.getByRole("group", { name: `Posição ${index}` }),
    );
    fireEvent.change(position.getByLabelText("Endereço *"), {
        target: { value: address },
    });
    fireEvent.change(position.getByLabelText("Código lido *"), {
        target: { value: code },
    });
}

it("adiciona/remove destinos sem perder o fato e confirma IDs, revisão e decimais exatos", async () => {
    const { requests } = setup();
    facts();
    add();
    add(second, "B02", 2);
    fireEvent.click(screen.getByRole("button", { name: "Remover posição 1" }));
    expect(
        within(screen.getByRole("group", { name: "Posição 1" })).getByLabelText(
            "Endereço *",
        ),
    ).toHaveValue(second);
    expect(screen.getByLabelText("Quantidade *")).toHaveValue("0.000001");
    expect(screen.getByLabelText("Instante real do fato *")).toHaveValue(
        "2026-10-15T10:00:00.123",
    );
    fill("Resolver pendências (Gestor)", "true");
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    expect(requests).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(requests).toHaveLength(1));
    expect(requests[0].params.codigo).toBe(uuid);
    expect(requests[0].body).toMatchObject({
        versaoUnidade: "7",
        quantidade: "0.000001",
        ocorridaEm: "2026-10-15T13:00:00.123Z",
        destinos: [{ enderecoId: second, codigoLido: "B02" }],
        resolverPendentes: true,
        motivo: "Fato fictício preservado para conferência",
    });
});

it("bloqueia inclusão/remoção durante envio e conserva os dados após conflito", async () => {
    const state = setup({ hold: true });
    facts();
    add();
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() =>
        expect(
            screen.getByRole("button", {
                name: "Adicionar posição",
            }),
        ).toBeDisabled(),
    );
    expect(
        screen.getByRole("button", { name: "Remover posição 1" }),
    ).toBeDisabled();
    state.fail();
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Revisão alterada",
    );
    expect(screen.getByLabelText("Quantidade *")).toHaveValue("0.000001");
    expect(
        within(screen.getByRole("group", { name: "Posição 1" })).getByLabelText(
            "Código lido *",
        ),
    ).toHaveValue("A01");
    expect(state.requests).toHaveLength(1);
});

it("não repete a unidade fixa e mantém a resolução restrita ao Gestor", () => {
    setup({ fixed: true, perfil: "SUPERVISOR" });
    expect(
        screen.queryByRole("group", { name: "Unidade afetada" }),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Código *")).toBeDisabled();
    expect(screen.getByLabelText("Revisão atual da unidade *")).toBeDisabled();
    expect(
        screen.getByLabelText("Resolver pendências (Gestor)"),
    ).toBeDisabled();
    expect(screen.getByLabelText("Resolver pendências (Gestor)")).toHaveValue(
        "false",
    );
});
