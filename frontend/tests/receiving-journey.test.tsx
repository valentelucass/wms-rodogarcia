import { it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { FictitiousTransport } from "../src/api/fictitious";
import { writeFileSync } from "node:fs";
import type { Request } from "../src/api/client";
import {
    Harness,
    lastMutation,
    click,
    fill,
    confirm,
    consult,
    selected,
    body,
    saveProof,
} from "./support/operationalHarness";
async function arrival(qty: string) {
    selected("Itens / Item 1 / Item da nota (ID) *", "401");
    fill("Chegada física real (ISO com fuso) *", "2026-10-08T12:00:00.123456Z");
    fill("Observacao *", "Conferência física fictícia");
    fill("Itens / Item 1 / Quantidade boa recebida *", qty);
    await confirm();
    await click("Comparar previsto e físico registrado");
    await consult();
}
it("FE05 XML sem chegada partes 50+48 divergência100/98 e efetivação supervisionada", async () => {
    const t = new FictitiousTransport("SUPERVISOR");
    const raws: string[] = [];
    const send = t.send.bind(t);
    t.send = async (request) => {
        const receipt = await send(request);
        raws.push(receipt.raw);
        return receipt;
    };
    render(
        <Harness
            transport={t}
            perfil="SUPERVISOR"
            page="entrada"
            action="PedidoEntradaController.criar"
        />,
    );
    fill("Referência *", "FICT-100-DIVERGENCIA");
    await confirm();
    expect(screen.getByText(/Pedido de entrada: 101/)).toBeInTheDocument();
    await click("2. Notas e itens");
    await click("Importar XML existente");
    selected("Identificador *", "101");
    fill("XML existente da NF-e *", "<nfe>nota ficticia prevista100</nfe>");
    await confirm();
    expect(body(lastMutation(t.requests)).versao).toBe("0");
    await click("Conferir itens importados (sem confirmar chegada)");
    await consult();
    expect(
        screen.getAllByRole("cell", { name: "100.000000" })[0],
    ).toBeInTheDocument();
    expect(screen.queryByText("98.000000", { exact: true })).toBeNull();
    await click("3. Conferência e chegadas");
    await click("Iniciar conferência");
    fill(
        "Motivo / justificativa *",
        "Conferência de XML fictício sem efeito fiscal",
    );
    await confirm();
    await click("Registrar chegada dos itens consultados");
    await arrival("50.000000");
    await click("3. Conferência e chegadas");
    await click("Registrar chegada física");
    selected("Revisão atual *", "3");
    await arrival("48.000000");
    expect(
        screen.getAllByRole("cell", { name: "98.000000" })[0],
    ).toBeInTheDocument();
    expect(
        screen.getAllByRole("cell", { name: "2.000000" })[0],
    ).toBeInTheDocument();
    await click("4. Divergência e efetivação");
    await click("Efetivar carga integral");
    selected("Revisão atual *", "4");
    fill("Aceitar Divergencias *", "true");
    fill(
        "Motivo / justificativa *",
        "Diferença fictícia aceita por Supervisor",
    );
    await confirm();
    expect(body(lastMutation(t.requests)).aceitarDivergencias).toBe(true);
    await click("Consultar entradas efetivadas para unitização");
    await consult();
    expect(
        screen.getAllByRole("cell", { name: "98.000000" })[0],
    ).toBeInTheDocument();
    expect(screen.getAllByText("Não", { exact: true }).length).toBeGreaterThan(
        0,
    );
    expect(raws.at(-1)).toContain('"id":201');
    expect(raws.at(-1)).toContain('"quantidadeTriagem":98.000000');
    expect(raws.at(-1)).toContain('"quantidadeQuarentena":0.000000');
    expect(raws.at(-1)).toContain('"unitizadaEm":null');
    expect(raws.at(-1)).toContain('"disponivelParaSaida":false');
    saveProof("FE05-01-02", t.requests);
}, 20000);
it("FE06 múltiplas unidades seleção explícita reimpressão divisão e reagrupamento", async () => {
    const t = new FictitiousTransport();
    const progress: {
        params: Request["params"];
        query: Request["query"];
        raw: string;
    }[] = [];
    const send = t.send.bind(t);
    t.send = async (request) => {
        const response = await send(request);
        if (request.endpoint.id === "UnidadeLogisticaController.progresso")
            progress.push({
                params: request.params,
                query: request.query,
                raw: response.raw,
            });
        return response;
    };
    render(
        <Harness
            transport={t}
            page="entrada"
            action="PedidoEntradaController.criar"
        />,
    );
    fill("Referência *", "FICT-1000-UNIDADES");
    await confirm();
    await click("2. Notas e itens");
    await click("Importar XML existente");
    fill("XML existente da NF-e *", "<nfe>prevista1000 ficticia</nfe>");
    await confirm();
    await click("Conferir itens importados (sem confirmar chegada)");
    await consult();
    await click("3. Conferência e chegadas");
    await click("Iniciar conferência");
    fill("Motivo / justificativa *", "Conferência física fictícia1000");
    await confirm();
    await click("Registrar chegada dos itens consultados");
    await arrival("1000.000000");
    await click("4. Divergência e efetivação");
    await click("Efetivar carga integral");
    fill("Motivo / justificativa *", "Efetivação física fictícia1000");
    await confirm();
    await click("Consultar entradas efetivadas para unitização");
    await consult();
    await click("Selecionar registro 1");
    await click("Unitizar a entrada conferida selecionada");
    await click("Consultar progresso");
    selected("Pedido (ID) *", "101");
    await consult();
    expect(progress.at(-1)?.params).toEqual({ pedidoId: "101" });
    expect(progress.at(-1)?.query).toEqual({});
    expect(progress.at(-1)?.raw).toContain('"pedidoId":101');
    expect(progress.at(-1)?.raw).toContain('"entradasConferidas":1');
    expect(progress.at(-1)?.raw).toContain('"entradasUnitizadas":0');
    expect(progress.at(-1)?.raw).toContain('"entradasPendentes":1');
    expect(progress.at(-1)?.raw).toContain('"concluida":false');
    expect(
        screen.getAllByText("Entradas Unitizadas").at(-1)?.nextElementSibling,
    ).toHaveTextContent("0");
    expect(
        screen.getAllByText("Entradas Pendentes").at(-1)?.nextElementSibling,
    ).toHaveTextContent("1");
    // Escolhe a entrada efetivada relacionada ao pedido antes de distribuir.
    await click("Selecionar registro 1");
    await click("Distribuir em unidades");
    await click("Consultar embalagens deste SKU");
    await screen.findByRole("button", { name: "Selecionar registro 1" });
    await click("Selecionar registro 1");
    for (let i = 1; i <= 2; i++) {
        await click("Adicionar Unidades");
        fill(`Unidades / Item ${i} / Tipo *`, "PALLET");
        fill(`Unidades / Item ${i} / Condicao *`, "BOA");
        fill(`Unidades / Item ${i} / Quantidade *`, "500.000000");
    }
    selected("Entrada conferida (ID) *", "201");
    selected("Pedido (ID) *", "101");
    fill("Motivo / justificativa *", "Duas unidades fictícias500 cada");
    await confirm();
    expect(
        screen.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).not.toHaveTextContent("Unidade logística: 501");
    await click("1. Distribuir quantidades");
    await click("Consultar progresso");
    selected("Pedido (ID) *", "101");
    await consult();
    expect(progress.at(-1)?.params).toEqual({ pedidoId: "101" });
    expect(progress.at(-1)?.query).toEqual({});
    expect(progress.at(-1)?.raw).toContain('"pedidoId":101');
    expect(progress.at(-1)?.raw).toContain('"entradasUnitizadas":1');
    expect(progress.at(-1)?.raw).toContain('"entradasPendentes":0');
    expect(progress.at(-1)?.raw).toContain('"concluida":true');
    expect(
        screen.getAllByText("Entradas Unitizadas").at(-1)?.nextElementSibling,
    ).toHaveTextContent("1");
    expect(
        screen.getAllByText("Entradas Pendentes").at(-1)?.nextElementSibling,
    ).toHaveTextContent("0");
    writeFileSync(
        "evidencias/marco02-fe06-progresso.json",
        JSON.stringify(
            {
                boundary:
                    "UI React/Transport fictício, sem cálculo/estoque local",
                progress,
            },
            null,
            2,
        ),
    );
    // Seleção explícita da segunda unidade pela lista, sem copiar UUID.
    await click("2. Corrigir organização física");
    await click("Consultar lista");
    await consult();
    await click("Selecionar registro 2");
    await click("3. Ler e reimprimir etiqueta");
    await click("Selecionar registro 2");
    await click("Consultar / reimprimir etiqueta");
    selected("Código *", "00000000-0000-4000-8000-000000000002");
    await consult();
    await consult();
    expect(
        t.requests
            .filter(
                (r) => r.endpoint.id === "UnidadeLogisticaController.etiqueta",
            )
            .map((r) => r.params.codigo),
    ).toEqual([
        "00000000-0000-4000-8000-000000000002",
        "00000000-0000-4000-8000-000000000002",
    ]);

    await click("2. Corrigir organização física");
    await click("Selecionar registro 2");
    await click("Dividir unidade");
    selected("Unidade logística (ID) *", "502");
    selected("Pedido (ID) *", "101");
    selected("Revisão atual *", "7");
    fill("Quantidade Nova Unidade *", "200.000000");
    fill("Motivo / justificativa *", "Divisão física fictícia identificada");
    await confirm();
    expect(lastMutation(t.requests).params).toEqual({
        pedidoId: "101",
        unidadeId: "502",
    });
    await click("Consultar identidades após divisão");
    await consult();
    await click("Selecionar registro 2");
    await click("2. Corrigir organização física");
    await click("Selecionar registro 2");
    await click("Reagrupar unidades");
    selected("Unidade logística (ID) *", "502");
    selected("Versao Destino *", "8");
    await click("Adicionar origem unidade 503 · revisão 0");
    fill(
        "Motivo / justificativa *",
        "Reagrupamento físico fictício identificado",
    );
    await confirm();
    expect(
        (
            body(lastMutation(t.requests)).origens as {
                unidadeId: string;
                versao: string;
            }[]
        )[0],
    ).toEqual({ unidadeId: "503", versao: "0" });
    await click("Reimprimir conteúdo após reagrupamento");
    await consult();
    expect(t.requests.at(-1)?.params.codigo).toBe(
        "00000000-0000-4000-8000-000000000002",
    );
    expect(screen.getByText("12", { exact: true })).toBeInTheDocument();
    saveProof("FE06-01-02-organizacao", t.requests);
}, 25000);
