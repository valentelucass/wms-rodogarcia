import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Result } from "../src/components/Result";
import { FormReferences } from "../src/components/FormReferences";
import {
    businessTitle,
    formatDate,
    indexRecords,
    referenceText,
} from "../src/domain/referencePresentation";
import {
    createReferenceReader,
    ReferencePresentationProvider,
    ReferenceValue,
} from "../src/components/context/ReferencePresentation";
import { ScalarField } from "../src/components/fields/ScalarField";
import type { Request, Transport } from "../src/api/client";
import type { Values } from "../src/contracts/runtime";
import { ScopeChoices } from "../src/auth/ScopeChoices";
import { ReferenceCatalogProvider } from "../src/components/context/ReferenceCatalog";

const order = {
    id: "91",
    referencia: "ENT-000009",
    clienteId: "2",
    armazemId: "3",
    versao: "4",
};
const item = {
    id: "99",
    numeroItem: 1,
    produtoId: "86",
    sku: "000600366",
    unidadeMedida: "KG",
};
const detail = {
    pedido: order,
    notas: [{ id: "82", numero: "32332", serie: 5, itens: [item] }],
};
const entries: Values[] = [
    {
        id: "113",
        itemChegadaId: "117",
        notaId: "82",
        itemNotaId: "99",
        produtoId: "86",
        chegadaReal: "2026-10-10T13:00:00Z",
        dataFifo: "2026-10-10T13:00:00Z",
        efetivadaEm: "2026-10-14T12:57:33.216861Z",
        quantidadeTriagem: "1564.000000",
        quantidadeQuarentena: "0.000000",
        unitizadaEm: null,
    },
];
const known = indexRecords(detail, "PedidoEntradaDto.Detalhe");
const receipt = (data: unknown) => ({
    data,
    raw: "",
    requestId: "TESTE-REFERENCIAS",
    ficticio: true,
    replay: false,
});

describe("Referências de negócio", () => {
    it("mostra nota, item/SKU, KG e datas; selecionar conserva exatamente o registro original", () => {
        const select = vi.fn();
        render(
            <FormReferences.Provider
                value={{ defaults: {}, options: {}, records: known }}
            >
                <Result
                    data={entries}
                    type="List<RecebimentoDto.Entrada>"
                    onSelect={select}
                />
                <Result
                    data={{ pedidoId: "91", entradasPendentes: 1 }}
                    type="UnidadeLogisticaDto.Progresso"
                />
            </FormReferences.Provider>,
        );
        expect(screen.getByText("NF 32332 · série 5")).toBeVisible();
        expect(screen.getByText("1.564,00")).toBeVisible();
        expect(screen.getByText("ENT-000009")).toBeVisible();
        for (const id of ["91", "113", "117", "82", "99", "86"])
            expect(
                screen.queryByText(id, { exact: true }),
            ).not.toBeInTheDocument();
        fireEvent.click(screen.getByText("Datas e rastreabilidade"));
        expect(screen.getAllByText("10/10/2026, 10:00").length).toBe(2);
        fireEvent.click(
            screen.getByRole("button", {
                name: "Selecionar Item 1 · 000600366",
            }),
        );
        expect(select).toHaveBeenCalledWith(
            entries[0],
            "RecebimentoDto.Entrada",
        );
        expect(entries[0].quantidadeTriagem).toBe("1564.000000");
    });
    it("não associa IDs iguais de tipos diferentes e conserva zeros e Longs grandes", () => {
        const refs = {
            "ProdutoDto.Resposta": [
                { id: "9223372036854775807", sku: "000600366" },
            ],
            "ClienteDto.Resposta": [
                {
                    id: "9223372036854775807",
                    nome: "Cliente correto",
                    codigo: "CLI-000001",
                },
            ],
        };
        expect(
            referenceText("produtoId", "9223372036854775807", {}, "", refs),
        ).toBe("000600366");
        expect(
            referenceText("clienteId", "9223372036854775807", {}, "", refs),
        ).toBe("Cliente correto · CLI-000001");
        expect(
            referenceText("produtoId", "9223372036854775806", {}, "", refs),
        ).toBeUndefined();
    });
    it("mantém SKU repetido em itens diferentes e não inventa código para registro sem nome", () => {
        expect(
            businessTitle(
                { ...item, numeroItem: 2 },
                "PedidoEntradaDto.ItemConferencia",
            ),
        ).toBe("Item 2 · 000600366");
        expect(businessTitle({ id: "91" })).toBeUndefined();
        expect(formatDate("2026-10-14")).toBe("14/10/2026");
    });
    it("deduplica consultas autorizadas e não grava nem envia a identidade como nome", async () => {
        const send = vi.fn(async (r: Request) =>
            receipt({
                id: r.params.id,
                sku: "SKU-TESTE",
                descricao: "Produto teste",
            }),
        );
        const reader = createReferenceReader({ send });
        reader.read("ProdutoDto.Resposta", "86");
        reader.read("ProdutoDto.Resposta", "86");
        await waitFor(() =>
            expect(reader.snapshot()["ProdutoDto.Resposta"]?.[0]?.sku).toBe(
                "SKU-TESTE",
            ),
        );
        expect(send).toHaveBeenCalledTimes(1);
        expect(send.mock.calls[0][0].endpoint.method).toBe("GET");
        reader.close();
    });
    it("ignora resposta de outro registro e não repete consulta recusada", async () => {
        const send = vi.fn(async () => receipt({ id: "999", sku: "ERRADO" }));
        const reader = createReferenceReader({ send });
        reader.read("ProdutoDto.Resposta", "86");
        await new Promise((resolve) => setTimeout(resolve, 0));
        expect(reader.snapshot()).toEqual({});
        reader.read("ProdutoDto.Resposta", "86");
        expect(send).toHaveBeenCalledTimes(1);
        reader.close();
    });
    it("descarta resposta tardia depois de encerrar o escopo", async () => {
        let finish!: (value: ReturnType<typeof receipt>) => void;
        const reader = createReferenceReader({
            send: () =>
                new Promise((resolve) => {
                    finish = resolve;
                }),
        });
        reader.read("ProdutoDto.Resposta", "86");
        reader.close();
        finish(receipt({ id: "86", sku: "NAO-EXIBIR" }));
        await new Promise((resolve) => setTimeout(resolve, 0));
        expect(reader.snapshot()).toEqual({});
    });
    it("limita consultas simultâneas a três", async () => {
        const pending: (() => void)[] = [];
        const send = vi.fn(
            (r: Request) =>
                new Promise<ReturnType<typeof receipt>>((resolve) => {
                    pending.push(() =>
                        resolve(receipt({ id: r.params.id, sku: "SKU" })),
                    );
                }),
        );
        const reader = createReferenceReader({ send });
        for (let i = 1; i <= 5; i++)
            reader.read("ProdutoDto.Resposta", String(i));
        expect(send).toHaveBeenCalledTimes(3);
        pending[0]();
        await waitFor(() => expect(send).toHaveBeenCalledTimes(4));
        reader.close();
        pending.forEach((f) => f());
    });
    it("consulta produtos no cliente correto e envia só o ID ao selecionar pelo SKU", async () => {
        const change = vi.fn();
        const send = vi.fn(async () =>
            receipt({
                itens: [{ id: "86", sku: "000600366", descricao: "Bobina" }],
                totalPaginas: 1,
            }),
        );
        render(
            <ReferencePresentationProvider transport={{ send }}>
                <FormReferences.Provider
                    value={{ defaults: { clienteId: "2" }, options: {} }}
                >
                    <ScalarField
                        f={{ name: "produtoId", type: "Long", required: true }}
                        value=""
                        path="produtoId"
                        onChange={change}
                        disabled={false}
                    />
                </FormReferences.Provider>
            </ReferencePresentationProvider>,
        );
        await screen.findByRole("option", { name: "000600366 · Bobina" });
        fireEvent.change(screen.getByRole("combobox"), {
            target: { value: "86" },
        });
        expect(change).toHaveBeenCalledWith("86");
        expect(send.mock.calls.length).toBe(1);
    });
    it("não mostra nome antigo depois da troca de escopo", async () => {
        const first: Transport = {
            send: async () => receipt({ id: "86", sku: "ESCOPO-ANTIGO" }),
        };
        const second: Transport = {
            send: async () => {
                throw new Error("403");
            },
        };
        const view = render(
            <ReferencePresentationProvider transport={first} scope="A">
                <ReferenceValue field="produtoId" value="86" />
            </ReferencePresentationProvider>,
        );
        await screen.findByText("ESCOPO-ANTIGO");
        view.rerender(
            <ReferencePresentationProvider transport={second} scope="B">
                <ReferenceValue field="produtoId" value="86" />
            </ReferencePresentationProvider>,
        );
        expect(screen.queryByText("ESCOPO-ANTIGO")).not.toBeInTheDocument();
        expect(screen.getByText("Referência indisponível")).toBeVisible();
    });
    it("não consulta nem oferece produtos de outro cliente quando o contexto está vazio", () => {
        const send = vi.fn();
        render(
            <ReferencePresentationProvider transport={{ send }}>
                <FormReferences.Provider
                    value={{
                        defaults: { clienteId: "" },
                        options: {},
                        records: {
                            "ProdutoDto.Resposta": [
                                { id: "86", sku: "OUTRO-CLIENTE" },
                            ],
                        },
                    }}
                >
                    <ScalarField
                        f={{ name: "produtoId", type: "Long", required: true }}
                        value=""
                        path="produtoId"
                        onChange={vi.fn()}
                        disabled={false}
                    />
                </FormReferences.Provider>
            </ReferencePresentationProvider>,
        );
        expect(send).not.toHaveBeenCalled();
        expect(
            screen.queryByRole("option", { name: "OUTRO-CLIENTE" }),
        ).not.toBeInTheDocument();
    });
    it("volta à primeira página e descarta opções anteriores quando muda o cliente", async () => {
        const send = vi.fn(async (r: Request) =>
            receipt({
                itens: [
                    {
                        id: `${r.query.clienteId}${r.query.pagina}`,
                        sku: `CLIENTE-${r.query.clienteId}-PAGINA-${r.query.pagina}`,
                    },
                ],
                totalPaginas: 2,
            }),
        );
        const transport = { send };
        const field = (clienteId: string) => (
            <ReferencePresentationProvider transport={transport}>
                <FormReferences.Provider
                    value={{ defaults: { clienteId }, options: {} }}
                >
                    <ScalarField
                        f={{ name: "produtoId", type: "Long", required: true }}
                        value=""
                        path="produtoId"
                        onChange={vi.fn()}
                        disabled={false}
                    />
                </FormReferences.Provider>
            </ReferencePresentationProvider>
        );
        const view = render(field("2"));
        await screen.findByRole("option", { name: "CLIENTE-2-PAGINA-0" });
        fireEvent.click(screen.getByRole("button", { name: "Mais opções" }));
        await screen.findByRole("option", { name: "CLIENTE-2-PAGINA-1" });
        view.rerender(field("3"));
        expect(
            screen.queryByRole("option", { name: "CLIENTE-2-PAGINA-1" }),
        ).not.toBeInTheDocument();
        await screen.findByRole("option", { name: "CLIENTE-3-PAGINA-0" });
        expect(send.mock.calls.at(-1)?.[0].query).toMatchObject({
            clienteId: "3",
            pagina: 0,
        });
    });
    it("administração seleciona pelo nome e preserva vínculo existente que não veio na consulta", async () => {
        const send = vi.fn(async (r: Request) =>
            receipt({
                itens: r.endpoint.id.startsWith("Cliente")
                    ? [
                          {
                              id: "2",
                              nome: "Cliente de teste",
                              codigo: "CLI-000002",
                              situacao: "ATIVO",
                          },
                      ]
                    : [],
                totalPaginas: 1,
                totalItens: 1,
            }),
        );
        const view = render(
            <ReferenceCatalogProvider transport={{ send }}>
                <ScopeChoices kind="clientes" selected={["999"]} />
            </ReferenceCatalogProvider>,
        );
        const checkbox = await screen.findByRole("checkbox", {
            name: "Cliente de teste · CLI-000002",
        });
        fireEvent.click(checkbox);
        expect(
            view.container.querySelector<HTMLInputElement>(
                'input[name="clientes"]',
            )?.value,
        ).toBe("999,2");
        fireEvent.click(checkbox);
        expect(
            view.container.querySelector<HTMLInputElement>(
                'input[name="clientes"]',
            )?.value,
        ).toBe("999");
        expect(screen.queryByText("999")).not.toBeInTheDocument();
    });
});
