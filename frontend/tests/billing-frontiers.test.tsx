import { it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ApiError, type Request, type Transport } from "../src/api/client";
import { FictitiousTransport } from "../src/api/fictitious";
import { Operation } from "../src/components/Operation";
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
    selectResultRow,
    saveProof,
} from "./support/operationalHarness";

it("FE11 decisão recusada preserva ciclo integral versão e consulta histórica", async () => {
    const calls: Request[] = [];
    const closure = obj("FechamentoCobrancaDto.Fechamento", {
        id: "901",
        versao: "4",
        versaoAtual: 2,
        situacao: "PREPARADO",
    });
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            if (r.endpoint.method !== "GET")
                throw new ApiError(
                    "Decisão impedida por pendência do ciclo integral",
                    409,
                    "PENDENCIA_FINANCEIRA",
                    "ciclo409",
                );
            return receipt(
                r,
                r.endpoint.id === "FechamentoCobrancaController.listar"
                    ? {
                          itens: [closure],
                          pagina: 0,
                          tamanho: 20,
                          totalItens: "1",
                          totalPaginas: 1,
                      }
                    : closure,
            );
        }),
    };
    render(
        <Harness
            transport={t}
            page="fechamento"
            action="FechamentoCobrancaController.consultar"
        />,
    );
    fill("Identificador *", "901");
    await consult();
    await click("2. Decisão integral do Gestor");
    await click("Aprovar ciclo integral");
    selected("Identificador *", "901");
    selected("Revisão atual *", "4");
    selected("Numero *", "2");
    fill(
        "Motivo / justificativa *",
        "Decisão fictícia de todo o ciclo conferida",
    );
    await click("Conferir e confirmar");
    await click("Confirmar agora");
    await screen.findByText(/Decisão impedida por pendência do ciclo integral/);
    expect(body(lastMutation(calls))).toMatchObject({ versao: "4", numero: 2 });
    expect(body(lastMutation(calls))).not.toHaveProperty("linhaId");
    expect(screen.queryByText(/Resposta FICTÍCIA/)).toBeNull();
    await click("1. Ciclo e versões");
    await click("Consultar detalhe");
    await consult();
    expect(
        screen.getByText("PREPARADO", { exact: true, selector: "dd" }),
    ).toBeInTheDocument();
    saveProof("FE11-decisao-recusa", calls);
});

it("FE11 Gestor escolhe compromisso consultado na resolução explícita do fato e consulta resultado", async () => {
    const t = new FictitiousTransport();
    render(
        <Harness
            transport={t}
            page="fechamento"
            action="FechamentoCobrancaController.listar"
        />,
    );
    await consult();
    await selectResultRow("Fechamentos", "901");
    await click("Serviços e cálculo");
    await click("Registrar fato");
    await click("Consultar serviços para o fato");
    await screen.findByRole("button", { name: "Selecionar registro 1" });
    await click("Selecionar registro 1");
    fill("Origem *", "MANUAL");
    fill("Quantidade", "2.123456");
    fill("Categoria *", "");
    fill("Referencia Execucao", "EXEC-FICT-RESOLUCAO");
    fill("Criterio Rateio *", "Unidade e execução fictícia identificadas");
    fill(
        "Motivo / justificativa *",
        "Regularização histórica fictícia pelo Gestor",
    );
    await click("Informar Resolução financeira histórica (Gestor)");
    await click("Usar fechamento 901 como compromisso");
    selected("Resolução financeira histórica (Gestor) / Tipo *", "FECHAMENTO");
    selected(
        "Resolução financeira histórica (Gestor) / Compromisso Id *",
        "901",
    );
    await confirm();
    expect(body(lastMutation(t.requests)).resolucao).toEqual({
        tipo: "FECHAMENTO",
        compromissoId: "901",
    });
    await click("Consultar situação e origem deste fato");
    await consult();
    expect(screen.getByText("VALIDO", { exact: true })).toBeInTheDocument();
    saveProof("FE11-resolucao-gestor", t.requests);
}, 14000);

it("FE11 Supervisor lê pendência do fato e não recebe resolução/decisão do Gestor", async () => {
    const calls: Request[] = [];
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            return receipt(
                r,
                obj("FatoServicoDto.Fato", {
                    id: "2801",
                    situacao: "PENDENTE",
                }),
            );
        }),
    };
    const rendered = render(
        <Harness
            transport={t}
            perfil="SUPERVISOR"
            page="cobranca"
            action="FatoServicoController.consultar"
        />,
    );
    fill("Identificador *", "2801");
    await consult();
    expect(screen.getByText("PENDENTE", { exact: true })).toBeInTheDocument();
    await click("Registrar fato");
    expect(
        screen.getByRole("button", {
            name: "Informar Resolução financeira histórica (Gestor)",
        }),
    ).toBeDisabled();
    saveProof("FE11-pendencia-supervisor", calls);
    rendered.unmount();
    render(
        <Operation
            id="FechamentoCobrancaController.aprovar"
            transport={t}
            perfil="SUPERVISOR"
            context={{ id: "901" }}
            onSelect={vi.fn()}
        />,
    );
    expect(
        screen.queryByRole("button", { name: "Conferir e confirmar" }),
    ).toBeNull();
});
