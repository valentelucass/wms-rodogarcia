import { it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ApiError, type Request, type Transport } from "../src/api/client";
import { Operation } from "../src/components/Operation";
import {
    Harness,
    click,
    fill,
    consult,
    selected,
    receipt,
    obj,
    saveProof,
    body,
} from "./support/operationalHarness";

it("FE04 prévia válida consultada recusa hash/revisão sem confirmar nem criar endereços", async () => {
    const calls: Request[] = [];
    const t: Transport = {
        send: vi.fn(async (r) => {
            calls.push(r);
            if (r.endpoint.method !== "GET")
                throw new ApiError(
                    "Arquivo/revisão da prévia mudou; consulte novamente",
                    409,
                    "ARQUIVO_DIVERGENTE",
                    "excel409",
                );
            return receipt(
                r,
                obj("ImportacaoEnderecoDto.Resultado", {
                    id: "2101",
                    versao: "3",
                    arquivoHash: "b".repeat(64),
                    situacao: "PREPARADA",
                    erros: [],
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
    fill("Identificador *", "2101");
    await consult();
    click("Confirmar a prévia consultada sem erros");
    selected("Identificador *", "2101");
    selected("Revisão atual *", "3");
    selected("Arquivo Hash *", "b".repeat(64));
    fill("Motivo / justificativa *", "Confirmar a prévia fictícia consultada");
    click("Conferir e confirmar");
    click("Confirmar agora");
    await screen.findByText(/Arquivo\/revisão da prévia mudou/);
    expect(body(calls.at(-1)!)).toMatchObject({
        versao: "3",
        arquivoHash: "b".repeat(64),
    });
    expect(screen.queryByText(/Resposta FICTÍCIA/)).toBeNull();
    expect(
        screen.queryByRole("button", {
            name: "Conferir endereços da importação confirmada",
        }),
    ).toBeNull();
    saveProof("FE04-hash-revisao-recusa", calls);
});

it.each([
    ["ProdutoController.alterar", ["SKU *", "Unidade Medida *"]],
    [
        "EmbalagemController.alterar",
        ["DUN da embalagem *", "Produto / SKU (ID) *"],
    ],
    ["EnderecoController.alterar", ["Código *", "Armazém (ID) *"]],
])("FE04 formato mutável %s não expõe identidades imutáveis", (id, absent) => {
    const t: Transport = { send: vi.fn() };
    render(
        <Operation
            id={id}
            context={{ id: "9007199254740993", versao: "4" }}
            perfil="GESTOR"
            transport={t}
            onSelect={vi.fn()}
        />,
    );
    selected("Identificador *", "9007199254740993");
    selected("Revisão atual *", "4");
    absent.forEach((name) =>
        expect(screen.queryByLabelText(name, { exact: true })).toBeNull(),
    );
});
