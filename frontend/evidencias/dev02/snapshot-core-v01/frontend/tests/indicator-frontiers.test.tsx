import { it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import type { Request, Transport } from "../src/api/client";
import {
    obj,
    receipt,
    fill,
    consult,
    saveProof,
} from "./support/operationalHarness";
it.each(["SUPERVISOR", "GESTOR"] as const)(
    "FE08 %s consulta valor no alcance mantendo KG/UN e total da página distintos",
    async (perfil) => {
        const calls: Request[] = [];
        const t: Transport = {
            send: vi.fn(async (r) => {
                calls.push(r);
                return receipt(r, {
                    itens: [
                        obj("IndicadorEstoqueDto.Resultado", {
                            produtoId: "11",
                            sku: "KG-FICT",
                            saldo: obj("EstoqueDto.Saldo", {
                                unidadeMedida: "KG",
                                fisicoTotal: "2.123456",
                            }),
                            valorExato: "9007199254740993.123456",
                            valorConhecido: "9007199254740993.123456",
                            valorConsultado: true,
                        }),
                        obj("IndicadorEstoqueDto.Resultado", {
                            produtoId: "12",
                            sku: "UN-FICT",
                            saldo: obj("EstoqueDto.Saldo", {
                                unidadeMedida: "UN",
                                fisicoTotal: "3.000000",
                            }),
                            valorExato: null,
                            valorConhecido: "0.000000",
                            valorConsultado: true,
                        }),
                    ],
                    pagina: 1,
                    tamanho: 2,
                    totalItens: "20",
                    totalPaginas: 10,
                });
            }),
        };
        render(
            <Operation
                id="IndicadorEstoqueController.listar"
                transport={t}
                context={{
                    clienteId: "1",
                    armazemId: "1",
                    fuso: "America/Sao_Paulo",
                }}
                perfil={perfil}
                onSelect={vi.fn()}
            />,
        );
        fill("Incluir valor (Supervisor / Gestor)", "true");
        fill("Página (início 0)", "1");
        fill("Itens por página (1–100)", "2");
        await consult();
        expect(calls[0].query).toMatchObject({
            clienteId: "1",
            armazemId: "1",
            valor: true,
            pagina: "1",
            tamanho: "2",
        });
        expect(
            screen.getAllByText("KG", { exact: true }).length,
        ).toBeGreaterThan(0);
        expect(
            screen.getAllByText("UN", { exact: true }).length,
        ).toBeGreaterThan(0);
        expect(
            screen.getAllByText("9007199254740993.123456", { exact: true })
                .length,
        ).toBeGreaterThan(0);
        expect(screen.queryByText("5.123456", { exact: true })).toBeNull();
        expect(screen.getByText("20", { exact: true })).toBeInTheDocument();
        saveProof("FE08-valor-medidas-" + perfil, calls);
    },
);
