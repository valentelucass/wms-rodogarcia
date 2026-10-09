import { expect, it } from "vitest";
import { writeFileSync } from "node:fs";
import { endpoint, type Values } from "../src/contracts/runtime";
import { type Request } from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";
import { ExampleBilling } from "../src/modules/financeiro/ExampleBilling";

it("FE-VIG-008 rejeições preservam identidade, revisão, hash e versão histórica", () => {
    const billing = new ExampleBilling();
    const commands: {
        id: string;
        params: Values;
        body: Values;
        data: Values;
    }[] = [];
    const respond = (id: string, body: Values = {}, numero?: number) => {
        const operation = endpoint(id);
        const request: Request = {
            endpoint: operation,
            params: { id: "901", ...(numero ? { numero } : {}) },
            query: {},
            body,
            signal: new AbortController().signal,
        };
        const data = structuredClone(
            billing.respond(request, body, fixture(operation.response)),
        ) as Values;
        commands.push({ id, params: request.params, body, data });
        return data;
    };
    const first = respond("FechamentoCobrancaController.rejeitar", {
        numero: 1,
        versao: "0",
        motivo: "Rejeição integral fictícia da versão 1",
    });
    const rejectedVersion = first.versao as Values;
    expect(first.fechamento).toMatchObject({
        id: "901",
        situacao: "REJEITADO",
        versaoAtual: 1,
    });
    expect(rejectedVersion).toMatchObject({
        id: "1001",
        fechamentoId: "901",
        numero: 1,
        calculoId: "801",
        situacao: "REJEITADA",
        conteudoHash: "a".repeat(64),
        decisor: "GESTOR-FICTICIO",
        motivoDecisao: "Rejeição integral fictícia da versão 1",
    });
    expect(rejectedVersion.decididaEm).not.toBeNull();
    const old = respond("FechamentoCobrancaController.versao", {}, 1);
    expect(old).toEqual(rejectedVersion);
    expect(respond("FechamentoCobrancaController.consultar")).toEqual(
        first.fechamento,
    );

    respond("FechamentoCobrancaController.reabrir", {
        versao: (first.fechamento as Values).versao,
        motivo: "Nova preparação fictícia sem apagar a decisão anterior",
    });
    const prepared = respond("FechamentoCobrancaController.versao", {}, 2);
    expect(prepared).toMatchObject({
        id: "1002",
        numero: 2,
        conteudoHash: "e".repeat(64),
        situacao: "PREPARADA",
        decididaEm: null,
        decisor: null,
        motivoDecisao: null,
    });
    const second = respond("FechamentoCobrancaController.rejeitar", {
        numero: 2,
        motivo: "Rejeição integral fictícia da versão 2",
    });
    expect(second.versao).toMatchObject({
        id: prepared.id,
        fechamentoId: prepared.fechamentoId,
        numero: prepared.numero,
        calculoId: prepared.calculoId,
        conteudoHash: prepared.conteudoHash,
        situacao: "REJEITADA",
        motivoDecisao: "Rejeição integral fictícia da versão 2",
    });
    expect(second.fechamento).toMatchObject({
        id: "901",
        versaoAtual: 2,
        situacao: "REJEITADO",
    });
    expect(respond("FechamentoCobrancaController.consultar")).toEqual(
        second.fechamento,
    );
    expect(respond("FechamentoCobrancaController.versao", {}, 1)).toEqual(old);
    expect(respond("FechamentoCobrancaController.versao", {}, 2)).toEqual(
        second.versao,
    );
    writeFileSync(
        "evidencias/fe-vig-008-historico.json",
        JSON.stringify(
            {
                boundary:
                    "ExampleBilling: respostas de estágio preparadas; sem API/backend ou decisão financeira real",
                commands,
            },
            null,
            2,
        ) + "\n",
    );
});
