import { it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import {
    Harness,
    lastMutation,
    obj,
    receipt,
    click,
    fill,
    consult,
    confirm,
    selected,
    body,
    saveProof,
    selectResultRow,
} from "./support/operationalHarness";
import { ApiError, type Request, type Transport } from "../src/api/client";
import { Operation } from "../src/components/Operation";
const codes = [
    "00000000-0000-4000-8000-000000000001",
    "00000000-0000-4000-8000-000000000002",
];
const rows = [
    obj("PedidoSaidaDto.Reserva", {
        id: "701",
        itemId: "611",
        unidadeId: "501",
        codigoUnidade: codes[0],
        notaOrigemId: "301",
        quantidade: "30.000000",
    }),
    obj("PedidoSaidaDto.Reserva", {
        id: "702",
        itemId: "611",
        unidadeId: "502",
        codigoUnidade: codes[1],
        notaOrigemId: "302",
        quantidade: "70.000000",
    }),
];
it("FE09 exceção FIFO integral duas reservas leitura UUID/revisões próprias separação completa e continuação", async () => {
    const calls: Request[] = [];
    let stage = 0;
    const order = (revision: string, status: string, reserves: unknown[]) =>
        obj("PedidoSaidaDto.Detalhe", {
            id: "601",
            versao: revision,
            situacao: status,
            itens: [
                obj("PedidoSaidaDto.Item", {
                    id: "611",
                    sku: "BOBINA-FICT",
                    quantidade: "100.000000",
                }),
            ],
            reservas: reserves,
        });
    const prepared = [
        order("0", "RASCUNHO", []),
        order("1", "RASCUNHO", []),
        order("2", "RESERVADO", rows),
        order("3", "EM_SEPARACAO", rows),
        order("4", "EM_SEPARACAO", rows),
        order("5", "EM_SEPARACAO", rows),
        order("6", "SEPARADO", rows),
    ];
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            const id = r.endpoint.id;
            let data: unknown;
            if (id === "PedidoSaidaController.listar")
                return receipt(r, {
                    itens: [prepared[stage]],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "1",
                    totalPaginas: 1,
                });
            if (id === "ExpedicaoController.consultar")
                return receipt(
                    r,
                    obj("ExpedicaoDto.Detalhe", { pedido: prepared[stage] }),
                );
            if (id === "PedidoSaidaController.consultar")
                data = prepared[stage];
            else if (id === "PedidoSaidaController.sugerir")
                data = obj("PedidoSaidaDto.Sugestao", {
                    versao: "0",
                    selecoes: [
                        obj("PedidoSaidaDto.Selecao", {
                            unidadeId: "501",
                            quantidade: "30.000000",
                        }),
                        obj("PedidoSaidaDto.Selecao", {
                            unidadeId: "502",
                            quantidade: "70.000000",
                        }),
                    ],
                });
            else if (
                id === "PedidoSaidaController.justificar" ||
                id === "PedidoSaidaController.reservar"
            ) {
                stage++;
                data = obj("PedidoSaidaDto.Confirmacao", {
                    pedido: prepared[stage],
                    operacaoId: body(r).operacaoId,
                });
            } else if (id === "UnidadeLogisticaController.etiqueta")
                data = obj("UnidadeLogisticaDto.Etiqueta", {
                    codigoLeitura: r.params.codigo,
                    versaoConteudo: r.params.codigo === codes[0] ? "11" : "12",
                    quantidadeProduto: "500.000000",
                });
            else if (id === "EnderecoController.listar")
                data = {
                    itens: [
                        obj("EnderecoDto.Resposta", {
                            id: "81",
                            codigo: "A101",
                        }),
                    ],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "1",
                    totalPaginas: 1,
                };
            else {
                stage++;
                data = obj("ExpedicaoDto.Confirmacao", {
                    expedicao: obj("ExpedicaoDto.Detalhe", {
                        pedido: prepared[stage],
                        retiradaEm: null,
                        documentos: [],
                        baixas: [],
                        devolucoes: [],
                        fatos: [],
                        separacoes: [],
                    }),
                });
            }
            return receipt(r, data);
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
    await click("2. Sugestão FIFO e reserva");
    await click("Consultar sugestão FIFO");
    await consult();
    await click("Justificar exceção FIFO");
    fill(
        "Motivo / justificativa *",
        "Seleção fictícia identificada de duas bobinas",
    );
    await confirm();
    const justification = body(lastMutation(calls)).operacaoId;
    expect(body(lastMutation(calls)).selecoes as unknown[]).toHaveLength(2);
    await click("Conferir reserva com esta justificativa FIFO");
    selected("Justificativa Id", String(justification));
    fill(
        "Motivo / justificativa *",
        "Reserva integral fictícia conferida pelo Supervisor",
    );
    await confirm();
    expect(body(lastMutation(calls)).justificativaId).toBe(justification);
    await click("Ler a etiqueta da reserva confirmada");
    expect(screen.getByLabelText("Reserva (ID) *")).toHaveValue("");
    for (let index = 0; index < 2; index++) {
        await click(
            `Selecionar reserva ${701 + index} · unidade ${501 + index}`,
        );
        selected("Reserva (ID) *", String(701 + index));
        selected("Código lido *", codes[index]);
        await click("Consultar etiqueta da unidade");
        await screen.findByText(index === 0 ? "11" : "12", { exact: true });

        selected(
            "Revisão do conteúdo da etiqueta *",
            index === 0 ? "11" : "12",
        );
        fill(
            "Motivo / justificativa *",
            "Leitura fictícia da etiqueta consultada",
        );
        await confirm();
        expect(lastMutation(calls).params.id).toBe("601");
        expect(body(lastMutation(calls)).reservaId).toBe(String(701 + index));
        expect(body(lastMutation(calls)).codigoLido).toBe(codes[index]);
        await click("Confirmar destino da reserva lida");
        await click("Consultar posições para separação");
        await screen.findByRole("button", { name: "Selecionar registro 1" });
        await click("Selecionar registro 1");

        selected("Destinacao / Reserva (ID) *", String(701 + index));
        fill(
            "Motivo / justificativa *",
            "Destino fictício conferido para esta reserva",
        );
        await confirm();
        expect(
            (body(lastMutation(calls)).destinacao as { reservaId: string })
                .reservaId,
        ).toBe(String(701 + index));
        expect(
            screen.getByRole("region", {
                name: "Referências confirmadas da jornada",
            }),
        ).toHaveTextContent(index === 0 ? "EM_SEPARACAO" : "SEPARADO");
        if (index === 0) {
            expect(
                screen.queryByRole("button", {
                    name: "Registrar documento existente deste pedido",
                }),
            ).toBeNull();
            await click("Selecionar e ler outra reserva deste pedido");
        }
    }
    expect(
        screen.getByRole("button", {
            name: "Registrar documento existente deste pedido",
        }),
    ).toBeInTheDocument();
    saveProof("FE09-01-02-duas-reservas", calls);
}, 20000);
it("FE09 bobinas4/6 pedido6 usa sugestão e justificativa recebidas sem fracionar unidade no cliente", async () => {
    const calls: Request[] = [];
    let revision = "0";
    const detail = () =>
        obj("PedidoSaidaDto.Detalhe", {
            id: "601",
            versao: revision,
            situacao: revision === "2" ? "RESERVADO" : "RASCUNHO",
            itens: [
                obj("PedidoSaidaDto.Item", {
                    id: "611",
                    sku: "BOBINA-FICT",
                    quantidade: "6.000000",
                }),
            ],
            reservas:
                revision === "2"
                    ? [
                          obj("PedidoSaidaDto.Reserva", {
                              id: "702",
                              unidadeId: "502",
                              codigoUnidade: codes[1],
                              quantidade: "6.000000",
                          }),
                      ]
                    : [],
        });
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            let data: unknown;
            if (r.endpoint.id === "EstoqueController.listar")
                return receipt(r, {
                    itens: [],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "0",
                    totalPaginas: 0,
                });
            if (r.endpoint.id === "PedidoSaidaController.consultar")
                return receipt(r, detail());
            if (r.endpoint.id === "PedidoEntradaController.listar")
                return receipt(r, {
                    itens: [],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "0",
                    totalPaginas: 0,
                });
            if (r.endpoint.id === "UnidadeLogisticaController.listar")
                data = {
                    itens: ["4.000000", "6.000000"].map((quantity, index) =>
                        obj("UnidadeLogisticaDto.Resumo", {
                            id: String(501 + index),
                            codigo: codes[index],
                            sku: "BOBINA-FICT",
                            tipo: "BOBINA",
                            quantidade: quantity,
                        }),
                    ),
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "2",
                    totalPaginas: 1,
                };
            else if (r.endpoint.id === "PedidoSaidaController.listar")
                data = {
                    itens: [detail()],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "1",
                    totalPaginas: 1,
                };
            else if (r.endpoint.id === "PedidoSaidaController.sugerir")
                data = obj("PedidoSaidaDto.Sugestao", {
                    versao: "0",
                    selecoes: [{ unidadeId: "502", quantidade: "6.000000" }],
                });
            else {
                revision =
                    r.endpoint.id === "PedidoSaidaController.justificar"
                        ? "1"
                        : "2";
                data = obj("PedidoSaidaDto.Confirmacao", {
                    operacaoId: body(r).operacaoId,
                    pedido: detail(),
                });
            }
            return receipt(r, data);
        }),
    };
    render(
        <Harness
            transport={t}
            perfil="SUPERVISOR"
            page="unidades"
            action="UnidadeLogisticaController.listar"
        />,
    );
    fill("Pedido (ID) *", "101");
    await consult();
    expect(screen.getByRole("cell", { name: "4.000000" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "6.000000" })).toBeInTheDocument();
    await selectResultRow("Unidades logísticas", "502");
    await click("Saída, FIFO e reserva");
    await consult();
    await selectResultRow("Pedidos de saída", "601");
    await click("2. Sugestão FIFO e reserva");
    await click("Consultar sugestão FIFO");
    await consult();
    await click("Justificar exceção FIFO");
    fill(
        "Motivo / justificativa *",
        "Bobina4/6: seleção fictícia da unidade6 inteira recebida, sem fracionar a unidade4",
    );
    await confirm();
    expect(body(lastMutation(calls)).selecoes).toEqual([
        { unidadeId: "502", quantidade: "6.000000" },
    ]);
    const justification = body(lastMutation(calls)).operacaoId;
    await click("Conferir reserva com esta justificativa FIFO");
    selected("Justificativa Id", String(justification));
    fill(
        "Motivo / justificativa *",
        "Reserva fictícia inteira6 conferida pelo Supervisor",
    );
    await confirm();
    expect(lastMutation(calls).params.id).toBe("601");
    expect(body(lastMutation(calls)).justificativaId).toBe(justification);
    // Reservar recebe a identidade da justificativa, não outro catálogo de seleções.
    expect(body(lastMutation(calls))).not.toHaveProperty("selecoes");
    expect(
        screen.getByRole("table", { name: /^Reservas do pedido ·/ }),
    ).toHaveTextContent("702");
    expect(
        screen.getByRole("table", { name: /^Reservas do pedido ·/ }),
    ).toHaveTextContent("6.000000");
    expect(
        screen.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toHaveTextContent("RESERVADO");
    saveProof("FE09-02-bobinas4-6", calls);
});
it("FE09 avaria/conflito não libera reserva; reversão recebe revisão própria e reconfirma estado", async () => {
    const calls: Request[] = [];
    let reversed = false;
    const current = obj("PedidoSaidaDto.Detalhe", {
        id: "601",
        versao: "2",
        situacao: "RESERVADO",
        reservas: rows,
    });
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            if (r.endpoint.id === "PedidoSaidaController.revalidar")
                throw new ApiError(
                    "Avaria recebida: expedição impedida; reservas preservadas",
                    409,
                    "AVARIA_IMPEDE_SAIDA",
                    "recusa-ficticia",
                );
            if (r.endpoint.id === "PedidoSaidaController.reverter") {
                reversed = true;
                return receipt(
                    r,
                    obj("PedidoSaidaDto.Confirmacao", {
                        pedido: obj("PedidoSaidaDto.Detalhe", {
                            id: "601",
                            versao: "3",
                            situacao: "RASCUNHO",
                            reservas: [],
                        }),
                    }),
                );
            }
            return receipt(
                r,
                r.endpoint.id === "PedidoSaidaController.listar"
                    ? {
                          itens: [
                              reversed
                                  ? {
                                        ...current,
                                        situacao: "RASCUNHO",
                                        versao: "3",
                                        reservas: [],
                                    }
                                  : current,
                          ],
                          pagina: 0,
                          tamanho: 20,
                          totalItens: "1",
                          totalPaginas: 1,
                      }
                    : reversed
                      ? obj("PedidoSaidaDto.Detalhe", {
                            id: "601",
                            versao: "3",
                            situacao: "RASCUNHO",
                            reservas: [],
                        })
                      : current,
            );
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
    await click("2. Sugestão FIFO e reserva");
    await click("Revalidar disponibilidade");
    await click("Conferir e confirmar");
    await click("Confirmar agora");
    await screen.findByText(/Avaria recebida/);
    expect(
        screen
            .getAllByRole("button", { name: /Selecionar reserva/ })
            .filter((b) => screen.getByRole("dialog").contains(b)),
    ).toHaveLength(2);
    expect(calls.filter((r) => r.endpoint.method !== "GET")).toHaveLength(1);
    await click("4. Reversão e cancelamento");
    await click("Reverter reserva");
    selected("Revisão atual *", "2");
    fill(
        "Motivo / justificativa *",
        "Reversão fictícia motivada de reserva não separada",
    );
    await confirm();
    await click("1. Pedido integral");
    await click("Consultar detalhe");
    selected("Identificador *", "601");
    await consult();
    expect(
        screen.queryByRole("region", { name: "Reservas atuais do pedido" }),
    ).toBeNull();
    expect(
        screen.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toHaveTextContent("Revisão 3");
    saveProof("FE09-conflito-reversao", calls);
}, 12000);
it("FE09 Operação pode registrar justificativa, mas não envia autorização da exceção", async () => {
    const send = vi.fn();
    render(
        <Operation
            id="PedidoSaidaController.reservar"
            transport={{ send }}
            context={{
                id: "601",
                versao: "1",
                justificativaId: crypto.randomUUID(),
            }}
            perfil="OPERACAO"
            onSelect={vi.fn()}
        />,
    );
    expect(screen.getByLabelText("Justificativa Id")).toBeDisabled();
    expect(screen.getByLabelText("Justificativa Id")).toHaveValue("");
    expect(send).not.toHaveBeenCalled();
});
