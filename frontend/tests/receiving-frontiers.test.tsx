import { it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { FictitiousTransport } from "../src/api/fictitious";
import {
    Harness,
    lastMutation,
    click,
    fill,
    confirm,
    consult,
    body,
    saveProof,
} from "./support/operationalHarness";

it("FE05 condição95boa5avaria efetivada permanece triagem/quarentena sem disponibilidade", async () => {
    const t = new FictitiousTransport("SUPERVISOR");
    render(
        <Harness
            transport={t}
            perfil="SUPERVISOR"
            page="entrada"
            action="PedidoEntradaController.criar"
        />,
    );
    fill("Referência *", "FICT-AVARIA-95-5");
    await confirm();
    await click("2. Notas e itens");
    await click("Importar XML existente");
    fill("XML existente da NF-e *", "<nfe>ficticia100</nfe>");
    await confirm();
    await click("Conferir itens importados (sem confirmar chegada)");
    await consult();
    await click("3. Conferência e chegadas");
    await click("Iniciar conferência");
    fill("Motivo / justificativa *", "Conferência de condição fictícia");
    await confirm();
    await click("Registrar chegada dos itens consultados");
    fill("Chegada física real (ISO com fuso) *", "2026-10-08T12:00:00.123456Z");
    fill("Observacao *", "Quantidade boa e avariada verificadas no exercício");
    fill("Itens / Item 1 / Quantidade boa recebida *", "95.000000");
    fill("Itens / Item 1 / Quantidade avariada *", "5.000000");
    await confirm();
    const arrival = body(lastMutation(t.requests));
    expect((arrival.itens as Record<string, unknown>[])[0]).toMatchObject({
        itemNotaId: "401",
        quantidadeBoa: "95.000000",
        quantidadeAvariada: "5.000000",
    });
    await click("Comparar previsto e físico registrado");
    await consult();
    expect(
        screen.getByRole("region", {
            name: "Conferência física e notas do pedido",
        }),
    ).toHaveTextContent("95.0000005.0000000.000000");
    await click("4. Divergência e efetivação");
    await click("Efetivar carga integral");
    fill(
        "Motivo / justificativa *",
        "Efetivação supervisionada com condição recebida",
    );
    await confirm();
    await click("Consultar entradas efetivadas para unitização");
    await consult();
    const entries = screen.getByRole("table", {
        name: /^Entradas conferidas ·/,
    });
    expect(entries).toHaveTextContent("201");
    expect(entries).toHaveTextContent("95.000000");
    expect(entries).toHaveTextContent("5.000000");
    expect(screen.getAllByText("Não", { exact: true }).length).toBeGreaterThan(
        0,
    );
    saveProof("FE05-condicao95-5", t.requests);
}, 16000);

it("FE05 Operação consulta conferência mas não efetiva carga", async () => {
    const t = new FictitiousTransport("OPERACAO");
    render(
        <Harness
            transport={t}
            perfil="OPERACAO"
            page="entrada"
            action="PedidoEntradaController.consultar"
        />,
    );
    fill("Identificador *", "101");
    await consult();
    await click("4. Divergência e efetivação");
    expect(
        screen.queryByRole("button", {
            name: "Efetivar carga integral",
        }),
    ).toBeNull();
    expect(t.requests.every((r) => r.endpoint.method === "GET")).toBe(true);
    saveProof("FE05-perfil-operacao", t.requests);
});
