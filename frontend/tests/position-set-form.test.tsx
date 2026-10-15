import { describe, it, expect } from "vitest";
import {
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import type { Request, Transport } from "../src/api/client";
import { Operation } from "../src/components/Operation";
import { ReferenceCatalogProvider } from "../src/components/context/ReferenceCatalog";
import { fixture } from "../src/api/mock/fixtures";
import type { Perfil, Values } from "../src/contracts/runtime";
import { encode } from "../src/contracts/codec";

function setup(perfil: Perfil = "GESTOR", holdWrite = false) {
    const requests: Request[] = [];
    const paged = (itens: Values[]) => ({
        itens,
        pagina: 0,
        tamanho: 100,
        totalItens: String(itens.length),
        totalPaginas: 1,
    });
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            const action = request.endpoint.id;
            let data: unknown;
            if (action === "ArmazemController.listar")
                data = paged(
                    ["1", "2"].map((id) => ({
                        ...(fixture("ArmazemDto.Resposta") as Values),
                        id,
                        nome: `Armazém ${id}`,
                        codigo: `ARM-${id}`,
                    })),
                );
            else if (action === "EnderecoController.listar")
                data = paged(
                    ["1", "2"].map((index) => ({
                        ...(fixture("EnderecoDto.Resposta") as Values),
                        id: `${request.query.armazemId}${index}`,
                        armazemId: request.query.armazemId,
                        codigo: `POS-${request.query.armazemId}-${index}`,
                    })),
                );
            else if (holdWrite && action === "CapacidadeController.criar")
                return new Promise(() => {});
            else data = fixture(request.endpoint.response);
            return {
                data,
                ficticio: true,
                raw: "",
                replay: false,
                requestId: "POSITION-SET-LOCAL",
            };
        },
    };
    render(
        <ReferenceCatalogProvider transport={transport}>
            <Operation
                id="CapacidadeController.criar"
                context={{}}
                perfil={perfil}
                transport={transport}
                onSelect={() => {}}
                inDialog
            />
        </ReferenceCatalogProvider>,
    );
    return requests;
}

async function warehouse(id: string) {
    await waitFor(() =>
        expect(
            screen.getByRole("combobox", { name: "Armazém *" }),
        ).toBeEnabled(),
    );
    fireEvent.click(screen.getByRole("combobox", { name: "Armazém *" }));
    fireEvent.click(
        await screen.findByRole("option", {
            name: new RegExp(`Armazém ${id}`),
        }),
    );
    const first = screen.getByLabelText("Primeira posição *");
    await waitFor(() =>
        expect(
            within(first).getByRole("option", {
                name: new RegExp(`POS-${id}-1`),
            }),
        ).toBeInTheDocument(),
    );
}
function fill(name: string, value: string) {
    fireEvent.change(screen.getByLabelText(name), { target: { value } });
}
async function complete() {
    await warehouse("1");
    fill("Primeira posição *", "11");
    fill("Segunda posição *", "12");
    fill("Peso máximo (kg) *", "1234.567");
    fill("Altura máxima (m) *", "2.125");
    fill("Largura máxima (m) *", "3.750");
    fill("Profundidade máxima (m) *", "1.625");
    fill("Empilhamento máximo *", "2");
    fill("Motivo / justificativa *", "Conjunto aferido para duas posições");
}

describe("Criação organizada do conjunto de posições", () => {
    it("orienta o armazém primeiro e consulta posições no contexto escolhido", async () => {
        const requests = setup();
        expect(screen.getByLabelText("Primeira posição *")).toBeDisabled();
        expect(screen.getByLabelText("Segunda posição *")).toBeDisabled();
        expect(
            requests.filter(
                (r) => r.endpoint.id === "EnderecoController.listar",
            ),
        ).toHaveLength(0);
        await warehouse("1");
        expect(screen.getByLabelText("Primeira posição *")).toBeEnabled();
        const reads = requests.filter(
            (r) => r.endpoint.id === "EnderecoController.listar",
        );
        expect(reads.length).toBeGreaterThan(0);
        expect(reads.every((r) => r.query.armazemId === "1")).toBe(true);
    });

    it("limpa as posições ao trocar o armazém e preserva limites e motivo", async () => {
        setup();
        await complete();
        await warehouse("2");
        expect(screen.getByLabelText("Primeira posição *")).toHaveValue("");
        expect(screen.getByLabelText("Segunda posição *")).toHaveValue("");
        expect(screen.getByLabelText("Peso máximo (kg) *")).toHaveValue(
            "1234.567",
        );
        expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
            "Conjunto aferido para duas posições",
        );
        expect(
            within(screen.getByLabelText("Primeira posição *")).queryByRole(
                "option",
                { name: /POS-1-1/ },
            ),
        ).not.toBeInTheDocument();
    });

    it("mantém o contrato exato e só envia após a confirmação", async () => {
        const requests = setup();
        await complete();
        fireEvent.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        expect(requests.some((r) => r.endpoint.method !== "GET")).toBe(false);
        const preview = screen.getByRole("region", {
            name: "Confirmação da ação",
        });
        expect(preview).toHaveTextContent("Primeira posição");
        expect(preview).toHaveTextContent("Segunda posição");
        fireEvent.click(
            screen.getByRole("button", { name: "Confirmar agora" }),
        );
        await waitFor(() =>
            expect(
                requests.some(
                    (r) => r.endpoint.id === "CapacidadeController.criar",
                ),
            ).toBe(true),
        );
        const request = requests.find(
            (r) => r.endpoint.id === "CapacidadeController.criar",
        )!;
        expect(request.body).toMatchObject({
            armazemId: "1",
            enderecoAId: "11",
            enderecoBId: "12",
            limites: {
                pesoKg: "1234.567",
                alturaMetros: "2.125",
                larguraMetros: "3.750",
                profundidadeMetros: "1.625",
                empilhamentoMaximo: "2",
            },
            motivo: "Conjunto aferido para duas posições",
        });
        expect(encode(request.endpoint.request!, request.body)).toContain(
            '"pesoKg":1234.567',
        );
    });

    it("bloqueia os campos durante envio sem remover os valores", async () => {
        setup("GESTOR", true);
        await complete();
        fireEvent.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        fireEvent.click(
            screen.getByRole("button", { name: "Confirmar agora" }),
        );
        await screen.findByRole("button", { name: "Aguardando confirmação…" });
        expect(screen.getByLabelText("Peso máximo (kg) *")).toBeDisabled();
        expect(screen.getByLabelText("Peso máximo (kg) *")).toHaveValue(
            "1234.567",
        );
        expect(
            screen.getByLabelText("Motivo / justificativa *"),
        ).toBeDisabled();
    });

    it("preserva a restrição de criação ao Gestor", () => {
        setup("SUPERVISOR");
        expect(
            screen.queryByRole("button", { name: "Conferir e confirmar" }),
        ).not.toBeInTheDocument();
        expect(
            screen.queryByLabelText("Peso máximo (kg) *"),
        ).not.toBeInTheDocument();
    });
});
