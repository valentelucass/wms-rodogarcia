import { useState } from "react";
import { it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { JourneyPage } from "../src/components/JourneyPage";
import { journeys } from "../src/domain/journeys";
import { absorb, emptyWorkflow } from "../src/domain/workflow";
import type { Request, Transport } from "../src/api/client";

it.each([
    ["Ver histórico de Serviço: Armazenagem Alfa", "SERVICO_COBRANCA"],
    [/^Consultar legado não atribuído do vínculo/, "VINCULO_COBRANCA_LEGADO"],
] as const)("abre a consulta correta pelo cartão %s", async (button, type) => {
    const requests: Request[] = [];
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            return {
                data: {
                    itens: [],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "0",
                    totalPaginas: 0,
                },
                ficticio: true,
                raw: "",
                replay: false,
                requestId: "AUDIT-TARGET-LOCAL",
            };
        },
    };
    function Harness() {
        const [workflow, setWorkflow] = useState(() => {
            const initial = emptyWorkflow();
            initial.selected["ConfiguracaoCobrancaDto.Servico"] = {
                id: "9007199254740993",
                descricao: "Armazenagem Alfa",
                codigo: "SRV-ALFA",
            };
            initial.selected["ConfiguracaoCobrancaDto.Vinculo"] = {
                id: "9007199254740993",
                codigo: "VINC-ALFA",
            };
            return initial;
        });
        return (
            <JourneyPage
                journey={journeys.find((j) => j.id === "precos")!}
                transport={transport}
                context={{}}
                perfil="GESTOR"
                workflow={workflow}
                startAction=""
                onNavigate={vi.fn()}
                onReceipt={vi.fn()}
                onRecord={(value, dto) =>
                    setWorkflow((old) => absorb(old, dto, value, true))
                }
            />
        );
    }
    render(<Harness />);
    await screen.findByRole("button", { name: "Novo serviço" });
    expect(
        requests.some((r) => r.endpoint.id === "AuditoriaController.listar"),
    ).toBe(false);
    fireEvent.click(screen.getByRole("button", { name: button }));
    await waitFor(() =>
        expect(
            requests.some(
                (r) => r.endpoint.id === "AuditoriaController.listar",
            ),
        ).toBe(true),
    );
    expect(screen.getByLabelText("Tipo *")).toHaveValue(type);
    expect(
        screen.getByRole("region", {
            name:
                type === "VINCULO_COBRANCA_LEGADO"
                    ? "Legado não atribuído"
                    : "Histórico de alterações",
        }),
    ).toBeInTheDocument();
    expect(
        requests.find((r) => r.endpoint.id === "AuditoriaController.listar")
            ?.query,
    ).toMatchObject({
        tipo: type,
        registroId: "9007199254740993",
    });
    expect(requests.every((r) => r.endpoint.method === "GET")).toBe(true);
});
