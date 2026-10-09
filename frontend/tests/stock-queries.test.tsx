import { it, expect, vi } from "vitest";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { FictitiousTransport } from "../src/api/fictitious";
import { type Request, type Transport } from "../src/api/client";
import {
    Harness,
    click,
    fill,
    consult,
    selected,
    saveProof,
    receipt,
    obj,
} from "./support/operationalHarness";
const code = "00000000-0000-4000-8000-000000000002";
it("FE08 saldo filtro unidade selecionada origem localização histórico e paginação", async () => {
    const calls: Request[] = [];
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            if (r.endpoint.id === "EstoqueController.consultar")
                return receipt(
                    r,
                    obj("EstoqueDto.Unidade", {
                        unidade: obj("UnidadeLogisticaDto.Resumo", {
                            id: r.params.codigo === code ? "502" : "501",
                            codigo: r.params.codigo,
                        }),
                    }),
                );
            let data: unknown;
            if (r.endpoint.id === "ProdutoController.listar")
                data = {
                    itens: [obj("ProdutoDto.Resposta", { id: "11" })],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "1",
                    totalPaginas: 1,
                };
            else if (r.endpoint.id === "EstoqueController.saldo")
                data = obj("EstoqueDto.Saldo", {
                    produtoId: "11",
                    fisicoTotal: "500.000000",
                    disponivel: "400.000000",
                    reservado: "100.000000",
                    avariado: "5.000000",
                    emQuarentena: "5.000000",
                    bloqueado: "100.000000",
                });
            else if (r.endpoint.id === "EstoqueController.listar")
                data = {
                    itens: [
                        obj("EstoqueDto.Unidade", {
                            unidade: obj("UnidadeLogisticaDto.Resumo", {
                                id: "501",
                            }),
                        }),
                        obj("EstoqueDto.Unidade", {
                            unidade: obj("UnidadeLogisticaDto.Resumo", {
                                id: "502",
                                codigo: code,
                                notaId: "302",
                                dataEntrada: "2026-10-07T12:00:00.123456Z",
                            }),
                            inicioArmazenagemEm: "2026-10-08T08:00:00.123456Z",
                        }),
                    ],
                    pagina: 1,
                    tamanho: 2,
                    totalItens: "4",
                    totalPaginas: 2,
                };
            else if (r.endpoint.id === "UnidadeLogisticaController.consultar")
                data = obj("UnidadeLogisticaDto.Detalhe", {
                    unidade: obj("UnidadeLogisticaDto.Resumo", {
                        id: "502",
                        codigo: code,
                        notaId: "302",
                    }),
                    origens: [
                        obj("UnidadeLogisticaDto.Origem", {
                            entradaId: "202",
                            itemNotaId: "402",
                            quantidadeAtual: "500.000000",
                        }),
                    ],
                });
            else
                data = {
                    itens: [
                        obj("EstoqueDto.Movimento", {
                            id: "3001",
                            acao: "POSICIONAR",
                            motivo: "Histórico fictício preservado",
                        }),
                    ],
                    pagina: 1,
                    tamanho: 2,
                    totalItens: "3",
                    totalPaginas: 2,
                };
            return receipt(r, data);
        }),
    };
    render(
        <Harness
            transport={t}
            page="estoque"
            action="EstoqueController.listar"
        />,
    );
    await click("Consultar produtos para saldo");
    await screen.findByRole("button", { name: "Selecionar registro 1" });
    await click("Selecionar registro 1");
    await click("Consultar saldo do SKU");
    selected("Produto / SKU (ID) *", "11");
    await consult();
    expect(screen.getByText("500.000000", { exact: true })).toBeInTheDocument();
    expect(screen.getByText("400.000000", { exact: true })).toBeInTheDocument();
    await click("Consultar lista");
    await consult();
    const main = document.querySelector(".record-workspace") as HTMLElement;
    fireEvent.click(
        within(main).getByRole("button", {
            name: new RegExp("^Ver detalhes de " + code),
        }),
    );
    await screen.findByRole("dialog");
    await screen.findByText("502", { selector: "dd" });
    await click("Consultar origem da unidade selecionada");
    await screen.findByRole("heading", { name: "Origens" });
    expect(calls.at(-1)?.params.unidadeId).toBe("502");
    expect(calls.at(-1)?.params.pedidoId).toBe("101");
    expect(screen.getByRole("cell", { name: "202" })).toBeInTheDocument();
    await click("Consultar movimentos");
    selected("Código *", code);
    fill("Página (início 0)", "1");
    fill("Itens por página (1–100)", "2");
    await consult();
    expect(calls.at(-1)?.params.codigo).toBe(code);
    expect(calls.at(-1)?.query.pagina).toBe("1");
    expect(
        screen.getByRole("cell", { name: "Histórico fictício preservado" }),
    ).toBeInTheDocument();
    saveProof("FE08-01", calls);
}, 15000);
it.each([null, "0.000000", "9007199254740993.123456"])(
    "FE08 indicador exibe valor recebido %s e Operação nunca pede valor=true",
    async (value) => {
        const calls: Request[] = [];
        const t: Transport = {
            send: vi.fn(async (r) => {
                calls.push(r);
                return receipt(r, {
                    itens: [
                        obj("IndicadorEstoqueDto.Resultado", {
                            valorConsultado: value !== null,
                            valorExato: value,
                            valorConhecido: value,
                        }),
                    ],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "1",
                    totalPaginas: 1,
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
                perfil="OPERACAO"
                onSelect={vi.fn()}
            />,
        );
        expect(
            screen.getByLabelText("Incluir valor (Supervisor / Gestor)"),
        ).toBeDisabled();
        await consult();
        expect(calls[0].query.valor).not.toBe(true);
        expect(
            screen.getAllByText(value ?? "Não informado", { exact: true })
                .length,
        ).toBeGreaterThan(0);
        saveProof("FE08-02-" + (value ?? "null"), calls);
    },
);
it("FE08 falha na consulta não vira vazio nem saldo zero", async () => {
    const t = new FictitiousTransport();
    t.scenario = "falha";
    render(
        <Operation
            id="EstoqueController.saldo"
            transport={t}
            context={{ clienteId: "1", armazemId: "1", produtoId: "11" }}
            perfil="GESTOR"
            onSelect={vi.fn()}
        />,
    );
    await click("Consultar");
    await screen.findByText(/serviço indisponível/);
    expect(screen.queryByText("Nenhum registro encontrado.")).toBeNull();
    expect(screen.queryByText("0.000000")).toBeNull();
    saveProof("FE08-erro", t.requests);
});
it("FE12-04 relatório auditoria Gestor consulta filtros e Operação não tem ação", async () => {
    const t = new FictitiousTransport();
    const rendered = render(
        <Harness
            transport={t}
            page="relatorios"
            action="AuditoriaController.listar"
        />,
    );
    await click("Consultar clientes para auditoria");
    await screen.findByRole("button", { name: "Selecionar registro 1" });
    await click("Selecionar registro 1");
    await click("Auditar Cliente 1");
    selected("Tipo *", "CLIENTE");
    selected("Registro Id *", "1");
    await consult();
    expect(t.requests.at(-1)?.query).toMatchObject({
        tipo: "CLIENTE",
        registroId: "1",
    });
    expect(t.requests.at(-1)?.query.clienteId).toBeUndefined();
    expect(t.requests.at(-1)?.endpoint.method).toBe("GET");
    saveProof("FE12-04-auditoria", t.requests);
    rendered.unmount();
    render(
        <Harness
            transport={t}
            perfil="OPERACAO"
            page="relatorios"
            action="AuditoriaController.listar"
        />,
    );
    expect(screen.queryByRole("button", { name: "Consultar" })).toBeNull();
    expect(
        screen.getByText(/Seu perfil não permite consultar esta página/),
    ).toBeInTheDocument();
    expect(
        screen.queryByRole("region", {
            name: "Registro consultado para auditoria",
        }),
    ).toBeNull();
});
