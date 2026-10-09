import { useState } from "react";
import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { AuditSelection } from "../src/modules/relatorios/AuditSelection";
import {
    absorb,
    emptyWorkflow,
    operationContext,
} from "../src/domain/workflow";
import { Operation } from "../src/components/Operation";
import type { Request, Transport } from "../src/api/client";
import { obj, receipt } from "./support/operationalHarness";
import { parseResponse } from "../src/contracts/codec";
import { route } from "../src/api/client";
function AuditHarness({ transport }: { transport: Transport }) {
    const initial = emptyWorkflow();
    for (const type of [
        "ConfiguracaoCobrancaDto.Contrato",
        "ConfiguracaoCobrancaDto.Tabela",
        "ConfiguracaoCobrancaDto.Vinculo",
    ])
        initial.selected[type] = { id: "9007199254740993" };
    const [workflow, setWorkflow] = useState(initial);
    return (
        <>
            <AuditSelection
                workflow={workflow}
                disabled={false}
                onSelect={(v, type) =>
                    setWorkflow((old) => absorb(old, type, v, true))
                }
            />
            <Operation
                key={workflow.selectionRevision}
                id="AuditoriaController.listar"
                transport={transport}
                context={operationContext(
                    "AuditoriaController.listar",
                    {},
                    workflow,
                )}
                perfil="GESTOR"
                onSelect={vi.fn()}
            />
        </>
    );
}
describe("005 vínculo lógico e legado separado", () => {
    it("005 mesmoID contrato/tabela/vinculo envia namespace próprio; legado não atribuído", async () => {
        const calls: Request[] = [],
            transport: Transport = {
                send: vi.fn(async (request) => {
                    calls.push(request);
                    const tipo = String(request.query.tipo);
                    const result = receipt(request, {
                        itens: [
                            obj("AuditoriaResponse", {
                                id: "51",
                                registroId: "9007199254740993",
                                tipo:
                                    tipo === "VINCULO_COBRANCA_LEGADO"
                                        ? "LEGADO_NAO_ATRIBUIDO"
                                        : tipo,
                                acao: "ENCERRAMENTO_VIGENCIA",
                                tipoFisico:
                                    tipo === "VINCULO_COBRANCA_LEGADO"
                                        ? "TABELA_COBRANCA"
                                        : tipo === "VINCULO_COBRANCA"
                                          ? "CONTRATO_COBRANCA"
                                          : tipo,
                            }),
                        ],
                        pagina: 0,
                        tamanho: 20,
                        totalItens: "1",
                        totalPaginas: 1,
                    });
                    return {
                        ...result,
                        data: parseResponse(
                            request.endpoint.response,
                            result.raw,
                        ),
                    };
                }),
            };
        render(<AuditHarness transport={transport} />);
        for (const [label, tipo] of [
            ["Contrato", "CONTRATO_COBRANCA"],
            ["Tabela", "TABELA_COBRANCA"],
            ["Vínculo", "VINCULO_COBRANCA"],
        ]) {
            fireEvent.click(
                screen.getByRole("button", {
                    name: `Auditar ${label} 9007199254740993`,
                }),
            );
            fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
            await waitFor(() => expect(calls.at(-1)?.query.tipo).toBe(tipo));
            await screen.findByText(/Resposta FICTÍCIA/);
            expect(calls.at(-1)?.query.registroId).toBe("9007199254740993");
            expect(calls.at(-1)?.endpoint.method).toBe("GET");
            expect(
                route(calls.at(-1)!.endpoint, {}, calls.at(-1)!.query),
            ).toContain(`tipo=${tipo}&registroId=9007199254740993`);
            expect(
                screen.getByRole("columnheader", {
                    name: "Origem física da auditoria",
                }),
            ).toBeInTheDocument();
        }
        fireEvent.click(
            screen.getByRole("button", {
                name: "Consultar legado não atribuído do vínculo 9007199254740993",
            }),
        );
        fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
        await waitFor(() =>
            expect(calls.at(-1)?.query.tipo).toBe("VINCULO_COBRANCA_LEGADO"),
        );
        await screen.findByRole("cell", { name: "LEGADO_NAO_ATRIBUIDO" });
        expect(
            screen.getByText(/não são histórico confirmado deste vínculo/),
        ).toBeInTheDocument();
        expect(calls).toHaveLength(4);
    });
    it("005 seleção inválida/legado de outra entidade não troca o alvo", () => {
        let state = emptyWorkflow();
        state.selected["ConfiguracaoCobrancaDto.Contrato"] = { id: "1" };
        state.selected["ConfiguracaoCobrancaDto.Vinculo"] = { id: "2" };
        state = absorb(
            state,
            "Auditoria.alvo",
            { dto: "ConfiguracaoCobrancaDto.Vinculo", registroId: "2" },
            true,
        );
        expect(state.followUps["AuditoriaController.listar"]).toMatchObject({
            tipo: "VINCULO_COBRANCA",
            registroId: "2",
        });
        state = absorb(
            state,
            "Auditoria.alvo",
            { dto: "ConfiguracaoCobrancaDto.Vinculo", registroId: "1" },
            true,
        );
        expect(state.followUps["AuditoriaController.listar"].registroId).toBe(
            "2",
        );
        state = absorb(
            state,
            "Auditoria.alvo",
            {
                dto: "ConfiguracaoCobrancaDto.Contrato",
                registroId: "1",
                legado: true,
            },
            true,
        );
        expect(state.followUps["AuditoriaController.listar"].tipo).toBe(
            "VINCULO_COBRANCA",
        );
    });
});
