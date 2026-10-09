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
    selected,
    body,
    saveProof,
} from "./support/operationalHarness";
const uuid = "00000000-0000-4000-8000-000000000001";
it("FE05 FE10 avaria reconhecimento Gestor reparo consulta e liberação mantêm fato e identidade", async () => {
    const t = new FictitiousTransport();
    render(
        <Harness
            transport={t}
            page="estoque"
            action="EstoqueController.consultar"
        />,
    );
    fill("Código *", uuid);
    await consult();
    await click("Avarias e reparos");
    await click("Registrar fato");
    selected("Código *", uuid);
    selected("Revisão atual da unidade *", "0");
    await click("Consultar posições para avaria e reparo");
    await screen.findByRole("button", { name: "Selecionar registro 1" });
    await click("Selecionar registro 1");
    selected("Posições de destino / Item 1 / Endereço (ID) *", "81");
    fill("Quantidade *", "5.000000");
    fill(
        "Instante real do fato (ISO com fuso) *",
        "2026-10-08T13:00:00.123456Z",
    );
    fill(
        "Motivo / justificativa *",
        "Avaria fictícia observada com destino seguro",
    );
    await confirm();
    expect(body(lastMutation(t.requests)).destinos).toEqual([
        { enderecoId: "81", codigoLido: "A101" },
    ]);
    await click("Consultar ocorrência e condição registradas");
    await consult();
    await click("Selecionar registro 1");
    await click("Reconhecer responsabilidade");
    selected("Identificador *", "3101");
    selected("Revisão atual *", "0");
    fill("Responsabilidade *", "RODOGARCIA");
    fill(
        "Motivo / justificativa *",
        "Responsabilidade fictícia reconhecida por Gestor",
    );
    await confirm();
    await click("Consultar responsabilidade reconhecida");
    await consult();
    expect(
        screen.getByRole("cell", { name: "RODOGARCIA" }),
    ).toBeInTheDocument();
    await click("Selecionar registro 1");
    await click("Registrar reparo");
    selected("Identificador *", "3101");
    selected("Revisão atual *", "1");
    selected("Revisão atual da unidade *", "8");
    fill(
        "Motivo / justificativa *",
        "Reparo fictício físico confirmado no exercício",
    );
    await confirm();
    await click("Consultar condição e bloqueio após reparo");
    await consult();
    expect(screen.getAllByText("Sim", { exact: true }).length).toBeGreaterThan(
        0,
    );
    await click("Bloqueio e liberação");
    await click("Confirmar liberação");
    selected("Revisão atual da unidade *", "9");
    fill(
        "Motivo / justificativa *",
        "Liberação fictícia após consulta do reparo",
    );
    await confirm();
    await click("Conferir a unidade após liberação");
    await consult();
    expect(
        t.requests.find((r) => r.endpoint.id === "AvariaController.reparar")
            ?.params.id,
    ).toBe("3101");
    expect(
        body(
            t.requests.find(
                (r) => r.endpoint.id === "AvariaController.reparar",
            )!,
        ).versao,
    ).toBe("1");
    saveProof("FE10-avaria-tratativa-liberacao", t.requests);
}, 18000);
it("FE10 Supervisor registra/repara mas reconhecimento é apresentado somente a Gestor", async () => {
    const t = new FictitiousTransport("SUPERVISOR");
    render(
        <Harness
            transport={t}
            perfil="SUPERVISOR"
            page="estoque"
            action="AvariaController.registrar"
        />,
    );
    expect(
        screen.getByRole("button", { name: "Registrar fato" }),
    ).toBeInTheDocument();
    expect(
        screen.getByLabelText("Resolver pendências (Gestor)"),
    ).toBeDisabled();
    await click("Voltar à lista");
    await click("Selecionar registro 1");
    expect(screen.getByRole("dialog").textContent).toContain(
        "Registrar reparo",
    );
    expect(
        screen.queryByRole("button", {
            name: "Reconhecer responsabilidade",
        }),
    ).toBeNull();
});
