import { it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { endpoint, type Perfil } from "../src/contracts/runtime";
import type { Request, Transport } from "../src/api/client";
import {
    fill,
    confirm,
    obj,
    receipt,
    body,
    saveProof,
} from "./support/operationalHarness";
const code = "00000000-0000-4000-8000-000000000001";
it.each(["OPERACAO", "SUPERVISOR"] as Perfil[])(
    "GEN01 %s utiliza leitura e separação comuns sem resolver pendências históricas",
    async (perfil) => {
        for (const action of ["ler", "separar"]) {
            const id = "ExpedicaoController." + action;
            expect(endpoint(id).permission).toBe("OPERACAO");
            const calls: Request[] = [];
            const t: Transport = {
                send: vi.fn(async (r) => {
                    calls.push(r);
                    return receipt(
                        r,
                        obj("ExpedicaoDto.Confirmacao", {
                            operacaoId: body(r).operacaoId,
                            expedicao: obj("ExpedicaoDto.Detalhe", {
                                pedido: obj("PedidoSaidaDto.Detalhe", {
                                    id: "601",
                                    versao: action === "ler" ? "3" : "4",
                                    situacao:
                                        action === "ler"
                                            ? "EM_SEPARACAO"
                                            : "SEPARADO",
                                }),
                                retiradaEm: null,
                                documentos: [],
                                baixas: [],
                                fatos: [],
                            }),
                        }),
                    );
                }),
            };
            const rendered = render(
                <Operation
                    id={id}
                    perfil={perfil}
                    transport={t}
                    context={{
                        id: "601",
                        versao: action === "ler" ? "2" : "3",
                        reservaId: "701",
                        codigoLido: code,
                        revisaoConteudo: "11",
                        destinacao: {
                            reservaId: "701",
                            conjuntoId: null,
                            destinos: [
                                { enderecoId: "81", codigoLido: "A101" },
                            ],
                        },
                        resolverPendentes: true,
                    }}
                    onSelect={vi.fn()}
                />,
            );
            expect(
                screen.getByLabelText("Resolver pendências (Gestor)"),
            ).toBeDisabled();
            expect(
                screen.getByLabelText("Resolver pendências (Gestor)"),
            ).toHaveValue("false");
            fill(
                "Motivo / justificativa *",
                "Confirmação comum fictícia no alcance do perfil",
            );
            await confirm();
            expect(calls).toHaveLength(1);
            expect(calls[0].params).toEqual({ id: "601" });
            expect(body(calls[0]).resolverPendentes).toBe(false);
            if (action === "ler")
                expect(body(calls[0])).toMatchObject({
                    reservaId: "701",
                    codigoLido: code,
                    revisaoConteudo: "11",
                    versao: "2",
                });
            else
                expect(body(calls[0])).toMatchObject({
                    versao: "3",
                    destinacao: {
                        reservaId: "701",
                        destinos: [{ enderecoId: "81", codigoLido: "A101" }],
                    },
                });
            expect(
                screen.getByText(
                    action === "ler" ? "EM_SEPARACAO" : "SEPARADO",
                    { exact: true },
                ),
            ).toBeInTheDocument();
            saveProof("GEN01-" + perfil + "-" + action, calls);
            rendered.unmount();
        }
    },
);
it("GEN01 Gestor explicita resolverPendentes=true sem alterar o caminho comum", async () => {
    const calls: Request[] = [];
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            return receipt(
                r,
                obj("ExpedicaoDto.Confirmacao", {
                    operacaoId: body(r).operacaoId,
                }),
            );
        }),
    };
    render(
        <Operation
            id="ExpedicaoController.ler"
            perfil="GESTOR"
            transport={t}
            context={{
                id: "601",
                versao: "2",
                reservaId: "701",
                codigoLido: code,
                revisaoConteudo: "11",
            }}
            onSelect={vi.fn()}
        />,
    );
    expect(screen.getByLabelText("Resolver pendências (Gestor)")).toBeEnabled();
    fill("Resolver pendências (Gestor)", "true");
    fill(
        "Motivo / justificativa *",
        "Resolução histórica fictícia explicitada pelo Gestor",
    );
    await confirm();
    expect(body(calls[0]).resolverPendentes).toBe(true);
    saveProof("GEN01-GESTOR-resolucao-explicita", calls);
});
