import { expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { ApiError, type Request, type Transport } from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";
import { automaticCodes } from "../src/domain/automaticCodes";
import { presentationDefaults } from "../src/domain/fieldPresentation";
import { initialValue } from "../src/contracts/codec";
import { records, type Values } from "../src/contracts/runtime";

it("criações automáticas não herdam o código de um registro selecionado e preservam códigos externos", () => {
    for (const [schema, field] of Object.entries(automaticCodes)) {
        const defaults = presentationDefaults(
            schema,
            { [field]: "ANTIGO-01" },
            "GESTOR",
        );
        expect((initialValue(schema, defaults) as Values)[field]).toBeNull();
        expect(records[schema].find((f) => f.name === field)?.required).toBe(
            false,
        );
    }
    expect(
        presentationDefaults(
            "ProdutoDto.Criar",
            { sku: "SKU-CLIENTE" },
            "GESTOR",
        ).sku,
    ).toBe("SKU-CLIENTE");
    expect(
        presentationDefaults(
            "EnderecoDto.Criar",
            { codigo: "R01-02" },
            "GESTOR",
        ).codigo,
    ).toBe("R01-02");
});

it("serviço sem código digitado esconde UUID e conserva a mesma chave no reenvio incerto", async () => {
    const calls: Request[] = [];
    const transport: Transport = {
        send: vi.fn(async (request) => {
            calls.push(request);
            if (calls.length === 1)
                throw new ApiError(
                    "Resposta incerta",
                    503,
                    "INCERTA",
                    "teste",
                    true,
                );
            return {
                data: {
                    ...(fixture("ConfiguracaoCobrancaDto.Servico") as Values),
                    codigo: "SRV-000001",
                },
                raw: "",
                ficticio: true,
                replay: false,
                requestId: "teste",
            };
        }),
    };
    render(
        <Operation
            id="ConfiguracaoCobrancaController.criarServico"
            transport={transport}
            context={{}}
            perfil="GESTOR"
            onSelect={vi.fn()}
        />,
    );
    expect(screen.queryByLabelText("Código *")).toBeNull();
    expect(
        screen.queryByLabelText("Identificador desta operação *"),
    ).toBeNull();
    for (const [name, value] of [
        ["Descricao *", "Serviço de teste"],
        ["Tipo *", "ADICIONAL"],
        ["Unidade *", "QUANTIDADE_PRODUTO"],
        ["Motivo / justificativa *", "Cadastro de serviço para teste"],
    ]) {
        fireEvent.change(screen.getByLabelText(name, { exact: true }), {
            target: { value },
        });
    }
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    expect(
        screen.getByRole("region", { name: "Confirmação da ação" }),
    ).not.toHaveTextContent("Identificador desta operação");
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await screen.findByText(/Resultado desconhecido/);
    fireEvent.click(
        screen.getByRole("button", {
            name: "Repetir exatamente a mesma operação",
        }),
    );
    await screen.findByText("SRV-000001");
    await waitFor(() => expect(calls).toHaveLength(2));
    expect(calls[1].body).toEqual(calls[0].body);
    expect(calls[0].body).toMatchObject({
        codigo: null,
        operacaoId: expect.stringMatching(/^[a-f\d-]{36}$/),
    });
    const key = (calls[0].body as Values).operacaoId;
    expect(screen.queryByText(String(key))).toBeNull();
});
