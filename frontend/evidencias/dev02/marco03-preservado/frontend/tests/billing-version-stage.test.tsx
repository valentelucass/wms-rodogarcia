import { it, expect } from "vitest";
import { render } from "@testing-library/react";
import { writeFileSync } from "node:fs";
import { FictitiousTransport } from "../src/api/fictitious";
import { ExampleBilling } from "../src/modules/financeiro/ExampleBilling";
import { fixture } from "../src/api/mock/fixtures";
import { endpoint, type Values } from "../src/contracts/runtime";
import { type Request } from "../src/api/client";
import {
    Harness,
    click,
    fill,
    consult,
    confirm,
    selected,
    selectResultRow,
    saveProof,
} from "./support/operationalHarness";

it("FE11 R18 reabertura: demonstrativo deve corresponder a numero2 escolhido", async () => {
    const transport = new FictitiousTransport();
    const receipts: {
        id: string;
        params: Values;
        raw: string;
        data: unknown;
    }[] = [];
    const send = transport.send.bind(transport);
    transport.send = async (request) => {
        const result = await send(request);
        receipts.push({
            id: request.endpoint.id,
            params: request.params,
            raw: result.raw,
            data: result.data,
        });
        return result;
    };
    render(
        <Harness
            transport={transport}
            page="cobranca"
            action="CalculoCobrancaController.listar"
        />,
    );
    await consult();
    selectResultRow("Cálculos", "801");
    click("Fechamentos e ESL");
    await consult();
    selectResultRow("Fechamentos", "901");
    click("1. Ciclo e versões");
    click("Consultar versão e hash");
    await consult();
    click("2. Decisão integral do Gestor");
    click("Reabrir ciclo");
    fill(
        "Motivo / justificativa *",
        "Reabertura fictícia para reconferir a versão preparada",
    );
    await confirm();
    click("Consultar a nova versão após reabertura");
    selected("Numero *", "2");
    await consult();
    click("Consultar demonstrativo da versão selecionada");
    selected("Numero *", "2");
    await consult();
    saveProof("VIGIA-R18-FE11-demonstrativo-versao2", transport.requests);
    writeFileSync(
        "evidencias/vigia-r18-stage-demonstrativo.json",
        JSON.stringify(
            {
                boundary: "React/jsdom + fictitious transport, no HTTP/backend",
                receipts,
            },
            null,
            2,
        ),
    );
    const result = receipts.at(-1)!;
    expect(result.id).toBe("FechamentoCobrancaController.demonstrativo");
    expect(String(result.params.numero)).toBe("2");
    expect((result.data as Values).numero).toBe(2);
}, 15000);

it("FE11 R18 documentos da versao1 permanecem historicos ao entregar versao2", () => {
    const billing = new ExampleBilling();
    const commands: {
        id: string;
        params: Values;
        body: Values;
        result: unknown;
    }[] = [];
    function request(id: string, body: Values = {}, numero?: string) {
        const e = endpoint(id);
        const r: Request = {
            endpoint: e,
            params: { id: "901", ...(numero ? { numero } : {}) },
            query: {},
            body,
            signal: new AbortController().signal,
        };
        const result = billing.respond(r, body, fixture(e.response));
        commands.push({
            id,
            params: r.params,
            body,
            result: structuredClone(result),
        });
        return result as Values;
    }
    request("FechamentoCobrancaController.aprovar");
    request("FechamentoCobrancaController.registrarNfse", {
        referenciaExterna: "NFSE-FICT-HIST-1",
        numeroDocumento: "FICT-1",
        emissorDocumento: "ESL-FICTICIO",
        emitidaEm: "2026-10-08T12:00:00Z",
    });
    request("FechamentoCobrancaController.reabrir");
    const beforeDelivery = request(
        "FechamentoCobrancaController.versao",
        {},
        "2",
    );
    request("FechamentoCobrancaController.aprovar", {
        numero: 2,
        versao: "2",
        motivo: "Aprovar a nova versao ficticia integral",
    });
    request("FechamentoCobrancaController.entregar", {
        arquivoHash: "e".repeat(64),
        destinoReferencia: "DESTINO-FICTICIO",
        entregueEm: "2026-10-08T13:00:00Z",
    });
    const historical = request("FechamentoCobrancaController.versao", {}, "1");
    const current = request("FechamentoCobrancaController.versao", {}, "2");
    writeFileSync(
        "evidencias/vigia-r18-stage-history-approved.json",
        JSON.stringify(
            {
                boundary:
                    "ExampleBilling pure frontend prepared receipts; reopened version approval before delivery, no HTTP/backend or fiscal execution",
                commands,
            },
            null,
            2,
        ),
    );
    expect(historical.numero).toBe(1);
    expect(historical.nfse).toHaveLength(1);
    expect(beforeDelivery.nfse).toHaveLength(0);
    expect(beforeDelivery.id).toBe("1002");
    expect(beforeDelivery.conteudoHash).toBe("e".repeat(64));
    expect(beforeDelivery.situacao).toBe("PREPARADA");
    expect(beforeDelivery.decididaEm).toBeNull();
    expect(beforeDelivery.decisor).toBeNull();
    expect(current.numero).toBe(2);
    expect(current.id).toBe("1002");
    expect(current.fechamentoId).toBe("901");
    expect(current.nfse).toHaveLength(0);
    expect(historical.conteudoHash).not.toBe(current.conteudoHash);
    expect(current.conteudoHash).toBe("e".repeat(64));
    expect(current.entregas).toHaveLength(1);
    expect((current.entregas as Values[])[0].arquivoHash).toBe(
        current.conteudoHash,
    );
    expect(historical.entregas).toHaveLength(0);
    const historicalDemo = request(
        "FechamentoCobrancaController.demonstrativo",
        {},
        "1",
    );
    const currentDemo = request(
        "FechamentoCobrancaController.demonstrativo",
        {},
        "2",
    );
    expect(historicalDemo.numero).toBe(1);
    expect(currentDemo.numero).toBe(2);
    expect((historicalDemo.calculo as Values).id).toBe(historical.calculoId);
    expect((currentDemo.calculo as Values).id).toBe(current.calculoId);
    writeFileSync(
        "evidencias/vigia-r18-stage-history-approved.json",
        JSON.stringify(
            {
                boundary:
                    "prepared per-version receipts, no API/backend/fiscal",
                commands,
            },
            null,
            2,
        ),
    );
});
