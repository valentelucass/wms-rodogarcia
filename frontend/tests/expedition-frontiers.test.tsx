import { it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ApiError, type Request, type Transport } from "../src/api/client";
import { Operation } from "../src/components/Operation";
import {
    Harness,
    obj,
    receipt,
    click,
    fill,
    consult,
    confirm,
    body,
    wire,
    saveProof,
} from "./support/operationalHarness";
const reserve = (id: string, note: string, unit: string) =>
    obj("PedidoSaidaDto.Reserva", {
        id,
        itemId: "611",
        notaOrigemId: note,
        unidadeId: unit,
        quantidade: "5.000000",
    });
it("FE10 cobertura de duas notas no documento simbólico conserva reserva origem SKU e nenhum movimento físico", async () => {
    const calls: Request[] = [];
    let order = obj("PedidoSaidaDto.Detalhe", {
        id: "601",
        versao: "3",
        situacao: "SEPARADO",
        itens: [
            obj("PedidoSaidaDto.Item", {
                id: "611",
                sku: "SKU-DUAS-NOTAS",
                quantidade: "10.000000",
            }),
        ],
        reservas: [reserve("701", "301", "501"), reserve("702", "302", "502")],
    });
    let detail = obj("ExpedicaoDto.Detalhe", {
        pedido: order,
        documentos: [],
        baixas: [],
        fatos: [],
        devolucoes: [],
        retiradaEm: null,
    });
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            if (r.endpoint.id === "PedidoSaidaController.consultar")
                return receipt(r, order);
            if (r.endpoint.id === "ExpedicaoController.documento") {
                order = { ...order, versao: "4" };
                detail = {
                    ...detail,
                    pedido: order,
                    documentos: [
                        obj("ExpedicaoDto.DocumentoRegistrado", {
                            ...body(r),
                            id: "1601",
                            situacao: "AUTORIZADO",
                            canceladoEm: null,
                        }),
                    ],
                };
                return receipt(
                    r,
                    obj("ExpedicaoDto.Confirmacao", {
                        expedicao: detail,
                        pedidoEntradaId: null,
                    }),
                );
            }
            return receipt(r, detail);
        }),
    };
    render(
        <Harness
            transport={t}
            page="saida"
            action="PedidoSaidaController.consultar"
        />,
    );
    fill("Identificador *", "601");
    await consult();
    click("Fiscal e retirada");
    await consult();
    click("Registrar documento existente");
    click("Adicionar cobertura da reserva 701 · nota 301");
    click("Adicionar cobertura da reserva 702 · nota 302");
    fill("Origem *", "NOTAZZ");
    fill("Natureza *", "RETORNO_SIMBOLICO");
    fill("Protocolo existente NOTAZZ *", "SIMBOLICO-FICTICIO");
    click("Informar Nota");
    fill("Nota / Emitente Cnpj *", "12345678000190");
    fill("Nota / Serie *", "1");
    fill("Nota / Numero *", "456");
    fill("Nota / Emissao *", "2026-10-08");
    fill("Nota / Chave Acesso", "1".repeat(44));
    fill("Nota / Chave Acesso", "");
    fill(
        "Motivo / justificativa *",
        "Documento fictício de duas notas conferido",
    );
    await confirm();
    expect(body(calls.at(-1)!).coberturas).toEqual([
        {
            reservaId: "701",
            notaOrigemId: "301",
            sku: "SKU-DUAS-NOTAS",
            quantidade: "5.000000",
        },
        {
            reservaId: "702",
            notaOrigemId: "302",
            sku: "SKU-DUAS-NOTAS",
            quantidade: "5.000000",
        },
    ]);
    expect(
        (body(calls.at(-1)!).nota as Record<string, unknown>).chaveAcesso,
    ).toBeNull();
    expect(
        screen.getByRole("region", {
            name: "Fiscal e físico do pedido selecionado",
        }),
    ).toHaveTextContent("Retirada física: não registrada");
    expect(
        calls.some((r) => r.endpoint.id === "ExpedicaoController.retirar"),
    ).toBe(false);
    saveProof("FE10-duas-notas-simbolico", calls);
});

it.each(["COBERTURA_INCOMPLETA", "DOCUMENTO_SIMBOLICO", "AVARIA"])(
    "FE10 retirada recusada %s conserva pedido e não promete baixa",
    async (code) => {
        const calls: Request[] = [];
        const onReceipt = vi.fn();
        const t: Transport = {
            send: vi.fn(async (r) => {
                calls.push(r);
                throw new ApiError(
                    "Retirada impedida: " +
                        code +
                        ". Consulte a expedição atual.",
                    409,
                    code,
                    "fiscal409",
                );
            }),
        };
        render(
            <Operation
                id="ExpedicaoController.retirar"
                transport={t}
                perfil="SUPERVISOR"
                context={{
                    id: "601",
                    versao: "4",
                    clienteId: "1",
                    armazemId: "1",
                }}
                onSelect={vi.fn()}
                onReceipt={onReceipt}
            />,
        );
        click("Adicionar XMLs comprovantes da retirada");
        fill("XMLs comprovantes da retirada / 1 *", "<nfe>ficticia</nfe>");
        fill(
            "Motivo / justificativa *",
            "Retirada fictícia consultada para confirmação",
        );
        click("Conferir e confirmar");
        click("Confirmar agora");
        await screen.findByText(new RegExp("Retirada impedida: " + code));
        expect(calls[0].params.id).toBe("601");
        expect(body(calls[0]).versao).toBe("4");
        expect(wire(calls[0])).toContain('"versao":4');
        expect(
            screen.getByLabelText("Identificador *", { exact: true }),
        ).toHaveValue("601");
        expect(screen.queryByText(/Resposta FICTÍCIA/)).toBeNull();
        expect(onReceipt).not.toHaveBeenCalled();
        expect(
            screen.queryByRole("button", {
                name: "Conferir retirada física, baixas e documentos",
            }),
        ).toBeNull();
        saveProof("FE10-recusa-" + code, calls);
    },
);
it("FE10 Operação não dispõe de retirada física fiscal", () => {
    render(
        <Operation
            id="ExpedicaoController.retirar"
            transport={{ send: vi.fn() }}
            perfil="OPERACAO"
            context={{ id: "601" }}
            onSelect={vi.fn()}
        />,
    );
    expect(
        screen.queryByRole("button", { name: "Conferir e confirmar" }),
    ).toBeNull();
    expect(screen.getByRole("status")).toHaveTextContent("SUPERVISOR");
});
