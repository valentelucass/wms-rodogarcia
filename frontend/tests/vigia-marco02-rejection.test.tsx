import { it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { writeFileSync } from "node:fs";
import { FictitiousTransport } from "../src/api/fictitious";
import type { Values } from "../src/contracts/runtime";
import { encode } from "../src/contracts/codec";
import {
    Harness,
    click,
    fill,
    consult,
    confirm,
    selectResultRow,
} from "./support/operationalHarness";
it("FE-VIG-008 rejeição integral recebida e consultada deve apresentar REJEITADO REJEITADA", async () => {
    const transport = new FictitiousTransport();
    const receipts: {
        id: string;
        params: Values;
        wire: string | null;
        raw: string;
        data: unknown;
    }[] = [];
    const send = transport.send.bind(transport);
    transport.send = async (r) => {
        const result = await send(r);
        receipts.push({
            id: r.endpoint.id,
            params: r.params,
            wire: r.endpoint.request
                ? encode(r.endpoint.request, r.body)
                : null,
            raw: result.raw,
            data: structuredClone(result.data),
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
    await selectResultRow("Cálculos", "801");
    await click("Fechamentos e ESL");
    await consult();
    await selectResultRow("Fechamentos", "901");
    await click("1. Ciclo e versões");
    await click("Consultar versão e hash");
    await consult();
    await click("2. Decisão integral do Gestor");
    await click("Rejeitar ciclo integral");
    fill(
        "Motivo / justificativa *",
        "Rejeição integral fictícia para reconferir estágio",
    );
    await confirm();
    const rejected = receipts.findLast(
        (r) => r.id === "FechamentoCobrancaController.rejeitar",
    )!;
    const confirmation = rejected.data as Values;
    const uiAfterDecision = document.body.textContent;
    await click("1. Ciclo e versões");
    await click("Consultar detalhe");
    await consult();
    const consulted = receipts.at(-1)!.data as Values;
    writeFileSync(
        "evidencias/vigia-marco02-rejeicao.json",
        JSON.stringify(
            {
                boundary:
                    "React/jsdom + FictitiousTransport real da aplicação; zero HTTP/API/BE",
                receipts,
                uiAfterDecision,
                uiAfterConsult: document.body.textContent,
                expected: { closure: "REJEITADO", version: "REJEITADA" },
                actual: {
                    closure: (confirmation.fechamento as Values).situacao,
                    version: (confirmation.versao as Values).situacao,
                    decidedAt: (confirmation.versao as Values).decididaEm,
                    consulted: consulted.situacao,
                },
            },
            null,
            2,
        ),
    );
    expect.soft((confirmation.fechamento as Values).situacao).toBe("REJEITADO");
    expect.soft((confirmation.versao as Values).situacao).toBe("REJEITADA");
    expect.soft(consulted.situacao).toBe("REJEITADO");
    expect(
        screen.getAllByText("REJEITADO", { exact: true }).length,
    ).toBeGreaterThan(0);
}, 15000);
