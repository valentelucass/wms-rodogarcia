import { it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { JourneyPage } from "../src/components/JourneyPage";
import { journeys } from "../src/domain/journeys";
import { emptyWorkflow } from "../src/domain/workflow";
import type { Request, Transport } from "../src/api/client";

it.each(["GESTOR", "SUPERVISOR"] as const)(
    "%s troca a visão com foco preservado, contexto exato e comandos do perfil",
    async (perfil) => {
        const requests: Request[] = [];
        const onRecord = vi.fn();
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
                    requestId: "PRICING-LAYOUT-LOCAL",
                };
            },
        };
        const workflow = emptyWorkflow();
        workflow.selected["ConfiguracaoCobrancaDto.Servico"] = {
            id: "9007199254740997",
            codigo: "SRV-ALFA",
            descricao: "Serviço Alfa",
            tipo: "SAIDA",
        };
        render(
            <JourneyPage
                journey={journeys.find((j) => j.id === "precos")!}
                transport={transport}
                context={{
                    armazemId: "9007199254740993",
                    clienteId: "9007199254740995",
                }}
                perfil={perfil}
                workflow={workflow}
                onRecord={onRecord}
                onReceipt={vi.fn()}
                startAction="ConfiguracaoCobrancaController.tabelas"
                onNavigate={vi.fn()}
            />,
        );
        await waitFor(() => expect(requests).toHaveLength(1));
        expect(requests[0].endpoint.id).toBe(
            "ConfiguracaoCobrancaController.tabelas",
        );
        expect(requests[0].query.armazemId).toBe("9007199254740993");
        expect(
            screen.queryByRole("button", { name: "Nova tabela" }) !== null,
        ).toBe(perfil === "GESTOR");
        if (perfil === "GESTOR") {
            fireEvent.click(
                screen.getByRole("button", {
                    name: /Auditar Serviço Serviço Alfa/,
                }),
            );
            expect(onRecord).toHaveBeenCalledWith(
                {
                    dto: "ConfiguracaoCobrancaDto.Servico",
                    registroId: "9007199254740997",
                },
                "Auditoria.alvo",
            );
            expect(requests).toHaveLength(1);
        } else
            expect(
                screen.queryByRole("region", {
                    name: "Registro consultado para auditoria",
                }),
            ).not.toBeInTheDocument();
        screen.getByLabelText("Visão").focus();
        fireEvent.change(screen.getByLabelText("Visão"), {
            target: { value: "1" },
        });
        await waitFor(() => expect(requests).toHaveLength(2));
        expect(screen.getByLabelText("Visão")).toHaveFocus();
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
        expect(requests[1].endpoint.id).toBe(
            "ConfiguracaoCobrancaController.vinculos",
        );
        expect(requests[1].query).toMatchObject({
            armazemId: "9007199254740993",
            clienteId: "9007199254740995",
        });
        expect(
            screen.queryByRole("button", { name: "Nova tabela" }),
        ).not.toBeInTheDocument();
        fireEvent.change(screen.getByLabelText("Visão"), {
            target: { value: "0" },
        });
        await waitFor(() => expect(requests).toHaveLength(3));
        expect(screen.getByLabelText("Visão")).toHaveFocus();
        expect(requests[2].endpoint.id).toBe(
            "ConfiguracaoCobrancaController.tabelas",
        );
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
        expect(requests.every((r) => r.endpoint.method === "GET")).toBe(true);
    },
);
