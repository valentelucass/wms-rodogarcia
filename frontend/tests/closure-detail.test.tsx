import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { Result } from "../src/components/Result";
import { FormReferences } from "../src/components/FormReferences";
import type { Values } from "../src/contracts/runtime";

const data: Values = {
    tipo: "CLIENTE",
    id: "9007199254740993",
    versao: "7",
    situacao: "ENCERRAMENTO_PENDENTE",
    impedimentos: [
        {
            codigo: "ENTRADA_PENDENTE",
            recurso: "PEDIDO_ENTRADA",
            id: "9007199254740995",
            detalhe:
                "Pedido ainda possui compromisso de recebimento/conferência.",
        },
    ],
};

function show(value = data, onSelect?: (row: Values, type: string) => void) {
    return render(
        <FormReferences.Provider
            value={{
                defaults: {},
                options: {},
                records: {
                    "PedidoEntradaDto.Resumo": [
                        { id: "9007199254740995", codigo: "PE-ALFA" },
                    ],
                    "ClienteDto.Resposta": [
                        {
                            id: "9007199254740993",
                            nome: "Cliente Alfa",
                            codigo: "CLI-ALFA",
                        },
                    ],
                },
            }}
        >
            <Result
                type="EncerramentoDto.Resultado"
                data={value}
                onSelect={onSelect}
            />
        </FormReferences.Provider>,
    );
}

describe("Consulta de encerramento com impedimentos", () => {
    it("identifica o cadastro e apresenta a pendência sem tabela ou seleção do impedimento", () => {
        const select = vi.fn();
        show(data, select);
        expect(
            screen.getByText("Cliente Alfa · CLI-ALFA", { exact: true }),
        ).toBeInTheDocument();
        expect(
            screen.getByText("Encerramento pendente", { exact: true }),
        ).toBeInTheDocument();
        expect(
            screen.getByText("Entrada pendente", { exact: true }),
        ).toBeInTheDocument();
        expect(
            screen.getByText("Pedido de entrada", { exact: true }),
        ).toBeInTheDocument();
        expect(
            screen.getByText(
                "Pedido ainda possui compromisso de recebimento/conferência.",
                { exact: true },
            ),
        ).toBeInTheDocument();
        expect(screen.queryByRole("table")).not.toBeInTheDocument();
        expect(
            screen.getByText("PE-ALFA", { exact: false }),
        ).toBeInTheDocument();
        expect(screen.queryByText("Ver registro 1")).not.toBeInTheDocument();
        expect(screen.getAllByRole("button")).toHaveLength(1);
        fireEvent.click(
            screen.getByRole("button", { name: "Selecionar este registro" }),
        );
        expect(select).toHaveBeenCalledWith(data, "EncerramentoDto.Resultado");
    });

    it.each([
        {
            impedimentos: [],
            message: "Nenhum impedimento informado nesta consulta.",
        },
        {
            impedimentos: null,
            message:
                "A consulta não informou a lista de impedimentos. Consulte novamente para conferir.",
        },
    ])(
        "distingue lista vazia de não informada: $message",
        ({ impedimentos, message }) => {
            show({ ...data, impedimentos });
            expect(
                screen.getByText(message, { exact: true }),
            ).toBeInTheDocument();
            expect(
                screen.queryByText(/liberado|autorizado/i),
            ).not.toBeInTheDocument();
            expect(screen.queryByRole("button")).not.toBeInTheDocument();
        },
    );

    it("mantém todos os impedimentos e seus detalhes sem criar operações por linha", () => {
        show({
            ...data,
            impedimentos: [
                ...(data.impedimentos as Values[]),
                {
                    codigo: "PENDENCIA_FUTURA",
                    recurso: "RECURSO_FUTURO",
                    detalhe: "Detalhe novo recebido do servidor",
                },
            ],
        });
        expect(screen.getAllByRole("listitem")).toHaveLength(2);
        expect(
            screen.getByText("2 informados", { exact: true }),
        ).toBeInTheDocument();
        expect(
            screen.getByText("Detalhe novo recebido do servidor", {
                exact: true,
            }),
        ).toBeInTheDocument();
        expect(screen.queryByRole("button")).not.toBeInTheDocument();
    });

    it("não transforma um item sem detalhes em ausência de impedimentos", () => {
        show({ ...data, impedimentos: [null] });
        expect(
            screen.getByText("1 informado", { exact: true }),
        ).toBeInTheDocument();
        expect(
            screen.getByText("Detalhe não informado pelo servidor.", {
                exact: true,
            }),
        ).toBeInTheDocument();
        expect(
            screen.queryByText("Nenhum impedimento informado nesta consulta."),
        ).not.toBeInTheDocument();
    });
});
