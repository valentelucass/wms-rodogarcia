import { it, expect, vi } from "vitest";
import { readFileSync } from "node:fs";
import { render, screen, fireEvent } from "@testing-library/react";
import { FictitiousTransport } from "../src/api/fictitious";
import { ApiError, type Request, type Transport } from "../src/api/client";
import {
    Harness,
    click,
    fill,
    confirm,
    consult,
    selected,
    body,
    saveProof,
    receipt,
    obj,
} from "./support/operationalHarness";
it("FE04 consulta seleção alteração revisão e estado atual de cadastro", async () => {
    const t = new FictitiousTransport();
    render(
        <Harness
            transport={t}
            page="cadastros"
            action="ClienteController.listar"
        />,
    );
    await consult();
    click("Selecionar registro 1");
    click("Corrigir descrição");
    selected("Identificador *", "1");
    selected("Revisão atual *", "0");
    expect(screen.queryByLabelText("Código *", { exact: true })).toBeNull();
    expect(screen.queryByLabelText("CPF / CNPJ *", { exact: true })).toBeNull();
    fill("Nome *", "Nome fictício atualizado");
    fill("Motivo / justificativa *", "Correção cadastral fictícia motivada");
    await confirm();
    expect(body(t.requests.at(-1)!).versao).toBe("0");
    expect(t.requests.at(-1)!.params.id).toBe("1");
    click("Conferir o cadastro atualizado");
    await consult();
    expect(
        screen.getByText("Nome fictício atualizado", { exact: true }),
    ).toBeInTheDocument();
    saveProof("FE04-01", t.requests);
});
it("FE04 prévia Excel consulta e confirmação conservam arquivo warehouse versão e hash", async () => {
    const t = new FictitiousTransport();
    render(
        <Harness
            transport={t}
            page="cadastros"
            action="ImportacaoEnderecoController.previa"
        />,
    );
    fireEvent.change(screen.getByLabelText("Planilha de endereços .xlsx *"), {
        target: {
            files: [
                new File(
                    [
                        new Uint8Array(
                            readFileSync(
                                "tests/fixtures/enderecos-ficticios.xlsx",
                            ),
                        ),
                    ],
                    "enderecos-ficticios.xlsx",
                    {
                        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    },
                ),
            ],
        },
    });
    fill("Motivo / justificativa *", "Prévia Excel fictícia conferida");
    await confirm();
    expect(t.requests.at(-1)?.params.id).toBe("1");
    expect(t.requests.at(-1)?.file?.name).toBe("enderecos-ficticios.xlsx");
    click("Consultar esta prévia Excel");
    selected("Identificador *", "2101");
    await consult();
    click("Confirmar a prévia consultada sem erros");
    selected("Revisão atual *", "0");
    selected("Arquivo Hash *", "a".repeat(64));
    fill("Motivo / justificativa *", "Confirmação da prévia fictícia exata");
    await confirm();
    expect(body(t.requests.at(-1)!).arquivoHash).toBe("a".repeat(64));
    click("Conferir endereços da importação confirmada");
    await consult();
    expect(
        screen.getByRole("region", { name: "Situação da prévia Excel" }),
    ).toHaveTextContent("CONFIRMADA");
    saveProof("FE04-02", t.requests);
});
it("FE04 prévia com erro fica pendente e não oferece confirmação", async () => {
    const calls: Request[] = [];
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            return receipt(
                r,
                obj("ImportacaoEnderecoDto.Resultado", {
                    id: "2102",
                    erros: [
                        obj("ImportacaoEnderecoDto.Erro", {
                            linha: 2,
                            coluna: "codigo",
                            mensagem: "Código incompatível",
                        }),
                    ],
                    situacao: "COM_ERROS",
                    confirmadaEm: null,
                    enderecos: [],
                }),
            );
        }),
    };
    render(
        <Harness
            transport={t}
            page="cadastros"
            action="ImportacaoEnderecoController.consultar"
        />,
    );
    fill("Identificador *", "2102");
    await consult();
    expect(screen.getByRole("alert")).toHaveTextContent(
        "Linha 2 · coluna codigo",
    );
    expect(
        screen.queryByRole("button", {
            name: "Confirmar a prévia consultada sem erros",
        }),
    ).toBeNull();
    saveProof("FE04-02-erros", calls);
});
it("FE04 encerramento consulta impedimentos solicitação inativação e histórico", async () => {
    const t = new FictitiousTransport();
    render(
        <Harness
            transport={t}
            page="cadastros"
            action="EncerramentoController.consultar"
        />,
    );
    fill("Tipo *", "CLIENTE");
    fill("Identificador *", "1");
    await consult();
    click("Solicitar encerramento deste cadastro");
    selected("Revisão atual *", "0");
    fill("Motivo / justificativa *", "Encerramento fictício solicitado");
    await confirm();
    click("Conferir inativação do cadastro solicitado");
    selected("Revisão atual *", "1");
    fill("Motivo / justificativa *", "Inativação fictícia conferida");
    await confirm();
    click("Consultar situação e impedimentos atuais");
    await consult();
    expect(screen.getByText("INATIVO", { exact: true })).toBeInTheDocument();
    expect(
        t.requests.filter((r) => r.endpoint.id.endsWith("inativar")),
    ).toHaveLength(1);
    saveProof("FE04-03", t.requests);
});
it("FE04 revisão antiga recusa não antecipa alteração nem apaga impedimento", async () => {
    const calls: Request[] = [];
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            if (r.endpoint.method !== "GET")
                throw new ApiError(
                    "Revisão antiga; cadastro conserva histórico",
                    409,
                    "REVISAO_DIVERGENTE",
                    "cad-409",
                );
            return receipt(
                r,
                obj("EncerramentoDto.Resultado", {
                    tipo: "CLIENTE",
                    id: "1",
                    impedimentos: [
                        obj("EncerramentoDto.Impedimento", {
                            codigo: "ESTOQUE_EXISTENTE",
                            detalhe: "Estoque impede a inativação",
                        }),
                    ],
                }),
            );
        }),
    };
    render(
        <Harness
            transport={t}
            page="cadastros"
            action="EncerramentoController.consultar"
        />,
    );
    fill("Tipo *", "CLIENTE");
    fill("Identificador *", "1");
    await consult();
    expect(
        screen.getByRole("cell", { name: "Estoque impede a inativação" }),
    ).toBeInTheDocument();
    click("Solicitar encerramento deste cadastro");
    fill("Motivo / justificativa *", "Encerramento fictício motivado");
    click("Conferir e confirmar");
    click("Confirmar agora");
    await screen.findByText(/Revisão antiga/);
    expect(screen.queryByText(/Resposta FICTÍCIA/)).toBeNull();
    saveProof("FE04-03-recusa", calls);
});
