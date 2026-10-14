import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { LabelPreview } from "../src/components/LabelPreview";
import { FictitiousTransport } from "../src/api/fictitious";
import {
    Harness,
    click,
    consult,
    fill,
    confirm,
    body,
    lastMutation,
} from "./support/operationalHarness";
import { records } from "../src/contracts/runtime";
import { validateValue } from "../src/contracts/codec";
import { fixture } from "../src/api/mock/fixtures";
import { exampleFixtures } from "../src/modules/recebimento/exampleFixtures";

vi.mock("qrcode", () => ({
    default: {
        toDataURL: vi.fn(
            async (value: string) => `data:image/png;base64,${btoa(value)}`,
        ),
    },
}));

describe("Correção completa de cadastros", () => {
    it("corrige o tipo permitido diretamente na posição e envia motivo e revisão", async () => {
        const transport = new FictitiousTransport();
        render(
            <Harness
                transport={transport}
                page="cadastros"
                action="EnderecoController.listar"
            />,
        );
        await consult();
        await click("Selecionar registro 1");
        await click("Editar");
        expect(
            screen.getByText(
                /Uma unidade em uma posição não precisa de conjunto/,
            ),
        ).toBeInTheDocument();
        fireEvent.change(
            screen.getByRole("combobox", { name: "Tipo de unidade permitido" }),
            {
                target: { value: "BOBINA" },
            },
        );
        fill("Motivo / justificativa *", "Completar tipo permitido na posição");
        await confirm();
        expect(body(lastMutation(transport.requests))).toMatchObject({
            tipoUnidadePermitido: "BOBINA",
            versao: "0",
            motivo: "Completar tipo permitido na posição",
        });
    });
    it("oferece tipo permitido também no cadastro inicial da posição", () => {
        const data = {
            ...(fixture("EnderecoDto.Criar") as Record<string, unknown>),
            tipoUnidadePermitido: "BOBINA",
            capacidadePesoKg: "2000.000",
            alturaMetros: "2.000",
            larguraMetros: "2.000",
            profundidadeMetros: "2.000",
        };
        expect(
            records["EnderecoDto.Criar"].some(
                (f) => f.name === "tipoUnidadePermitido",
            ),
        ).toBe(true);
        expect(() =>
            validateValue("EnderecoDto.Criar", data, "posição"),
        ).not.toThrow();
    });
    it.each(["Cliente", "Armazem", "Produto", "Embalagem", "Endereco"])(
        "valida o formulário completo carregado de %s",
        (entity) => {
            const data = fixture(`${entity}Dto.Resposta`) as Record<
                string,
                unknown
            >;
            if (entity === "Armazem") data.cidade = "Marília";
            const command = Object.fromEntries(
                records[`${entity}Dto.Alterar`].map((f) => [
                    f.name,
                    f.name === "motivo" ? "Correção cadastral" : data[f.name],
                ]),
            );
            expect(() =>
                validateValue(`${entity}Dto.Alterar`, command, "cadastro"),
            ).not.toThrow();
        },
    );
    it("carrega CNPJ na edição e envia a correção com versão e motivo", async () => {
        const transport = new FictitiousTransport();
        render(
            <Harness
                transport={transport}
                page="cadastros"
                action="ClienteController.listar"
            />,
        );
        await consult();
        await click("Selecionar registro 1");
        await click("Editar");
        const field = screen.getByRole("textbox", { name: "CPF / CNPJ" });
        expect(field).not.toHaveAttribute("readonly");
        expect(field).not.toHaveValue("");
        fireEvent.change(field, { target: { value: "12.345.678/0001-90" } });
        fill("Motivo / justificativa *", "Correção do documento cadastrado");
        await confirm();
        const mutation = lastMutation(transport.requests);
        expect(body(mutation)).toMatchObject({
            documentoFiscal: "12.345.678/0001-90",
            versao: "0",
            motivo: "Correção do documento cadastrado",
        });
        expect(mutation.params.id).toBe("1");
    });
    it.each([
        ["Armazem", ["documentoFiscal", "cidade", "uf"]],
        [
            "Produto",
            [
                "sku",
                "unidadeMedida",
                "tipoQuantidade",
                "precisaoQuantidade",
                "controlaLote",
                "controlaValidade",
                "antecedenciaAvisoDias",
            ],
        ],
        ["Embalagem", ["codigoDun", "quantidadeProduto"]],
        [
            "Endereco",
            [
                "codigo",
                "rua",
                "nivel",
                "posicao",
                "tipo",
                "sequenciaColeta",
                "capacidadePesoKg",
                "alturaMetros",
                "larguraMetros",
                "profundidadeMetros",
                "empilhamentoMaximo",
                "tipoUnidadePermitido",
            ],
        ],
    ])(
        "contrato de %s contém os campos corrigíveis e mantém revisão",
        (type, fields) => {
            expect(records[`${type}Dto.Alterar`].map((f) => f.name)).toEqual(
                expect.arrayContaining([...fields, "versao", "motivo"]),
            );
        },
    );
});

describe("Etiqueta conforme referência", () => {
    const label = exampleFixtures["UnidadeLogisticaDto.Etiqueta"];
    it("mostra SKU, notas múltiplas, quantidade das notas, data física, autor, DUN e sequência", async () => {
        render(
            <LabelPreview
                data={{
                    ...label,
                    notas: [
                        { numero: "32328", serie: 1 },
                        { numero: "32329", serie: 2 },
                    ],
                    quantidadeProduto: "10",
                }}
            />,
        );
        expect(screen.getByText("600366")).toBeInTheDocument();
        expect(screen.getByText(/32328.*32329/)).toBeInTheDocument();
        expect(
            screen.getByText("Quantidade nas notas: 1.820,00 KG"),
        ).toBeInTheDocument();
        expect(screen.getByText("10/10/2026")).toBeInTheDocument();
        expect(
            screen.getByText(/CONFERENTE: Conferente de demonstração/),
        ).toBeInTheDocument();
        expect(screen.getByText("1/10")).toBeInTheDocument();
        expect(screen.getByText("BOBINAS")).toBeInTheDocument();
        expect(screen.queryByText("EXERCÍCIO FICTÍCIO")).toBeNull();
        await waitFor(() =>
            expect(
                screen.getByRole("button", { name: "Imprimir etiqueta" }),
            ).toBeEnabled(),
        );
        const barcode = screen.getByRole("img", {
            name: `Código de barras do UUID ${label.codigoLeitura}`,
        });
        expect(barcode.querySelectorAll("rect").length).toBeGreaterThan(100);
        expect(
            screen.getByRole("img", {
                name: `QR do UUID ${label.codigoLeitura}`,
            }),
        ).toHaveAttribute(
            "src",
            `data:image/png;base64,${btoa(String(label.codigoLeitura))}`,
        );
    });
    it("distingue exercício e não reutiliza QR anterior ao trocar de unidade", async () => {
        const view = render(<LabelPreview data={label} ficticio />);
        await screen.findByRole("img", {
            name: `QR do UUID ${label.codigoLeitura}`,
        });
        expect(screen.getByText("EXERCÍCIO FICTÍCIO")).toBeInTheDocument();
        view.rerender(
            <LabelPreview
                data={{ ...label, codigoLeitura: "invalido" }}
                ficticio
            />,
        );
        expect(screen.queryByRole("img")).toBeNull();
        expect(
            screen.getByRole("button", { name: "Imprimir prévia fictícia" }),
        ).toBeDisabled();
        expect(screen.getByRole("alert")).toHaveTextContent("UUID inválido");
    });
    it("não inventa conferente nem substitui chegada por FIFO", () => {
        render(
            <LabelPreview
                data={{ ...label, nomeConferente: null, chegadaReal: null }}
            />,
        );
        expect(
            screen.getByText(/Não identificado no histórico/),
        ).toBeInTheDocument();
        expect(screen.getByText("Não informada")).toBeInTheDocument();
    });
});
