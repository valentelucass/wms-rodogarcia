import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import { StoredValueCard } from "../src/components/dashboard/StoredValueCard";
import type { VisaoOperacaoDto_Resumo } from "../src/contracts/types";
import { obj } from "./support/operationalHarness";

function summary(patch: Partial<VisaoOperacaoDto_Resumo> = {}) {
    return obj("VisaoOperacaoDto.Resumo", {
        financeiroPermitido: true,
        valorCompleto: false,
        valorArmazenado: null,
        valoracao: {
            conhecido: "125.25",
            total: null,
            unidadesValoradas: "1",
            unidadesPendentes: "2",
            entradasValoradas: "0",
            entradasPendentes: "1",
            pendencias: [
                {
                    codigo: "VALOR_NOTA_AUSENTE",
                    ocorrencias: "3",
                    detalhe: "Falta o valor da nota de origem.",
                },
            ],
        },
        ...patch,
    }) as unknown as VisaoOperacaoDto_Resumo;
}

it("mostra a parcela conhecida com pendências e explica as origens no popup", () => {
    render(<StoredValueCard perfil="GESTOR" data={summary()} />);
    expect(screen.getByText("R$ 125,25")).toBeInTheDocument();
    expect(
        screen.getByText("Parcial — valoração incompleta"),
    ).toBeInTheDocument();
    expect(
        screen.getByText("Sem valoração: 2 unidades · 1 item de entrada"),
    ).toBeInTheDocument();
    fireEvent.focus(screen.getByRole("article"));
    expect(screen.getByRole("tooltip")).toHaveTextContent(
        "Confira o valor fixo e a quantidade do item na nota de entrada.",
    );
});

it.each([false, true])(
    "zero com uma parcela apurada é parcial; zero sem parcela apurável fica indisponível (%s)",
    (known) => {
        const data = summary();
        data.valoracao!.conhecido = "0.00";
        data.valoracao!.unidadesValoradas = known ? "1" : "0";
        render(<StoredValueCard perfil="GESTOR" data={data} />);
        expect(
            screen.getByRole("article").querySelector("strong"),
        ).toHaveTextContent(known ? "R$ 0,00" : "—");
    },
);

it("usa o total completo, preservando precisão acima do limite de Number", () => {
    render(
        <StoredValueCard
            perfil="GESTOR"
            data={summary({
                valorCompleto: true,
                valorArmazenado: "9007199254741009.26",
            })}
        />,
    );
    expect(screen.getByText("R$ 9.007.199.254.741.009,26")).toBeInTheDocument();
    expect(screen.queryByText(/Parcial/)).not.toBeInTheDocument();
});

it("perfil sem acesso não exibe nem o parcial que um payload indevido trouxer", () => {
    render(
        <StoredValueCard
            perfil="GESTOR"
            data={summary({ financeiroPermitido: false })}
        />,
    );
    expect(
        screen.getByText("Disponível para Supervisor e Gestor"),
    ).toBeInTheDocument();
    expect(screen.queryByText("R$ 125,25")).not.toBeInTheDocument();
    fireEvent.focus(screen.getByRole("article"));
    expect(screen.getByRole("tooltip")).not.toHaveTextContent(
        "Valor de origem ausente",
    );
});

it("backend sem detalhamento conserva indisponibilidade, sem inventar um parcial", () => {
    render(
        <StoredValueCard perfil="GESTOR" data={summary({ valoracao: null })} />,
    );
    expect(
        screen.getByRole("article").querySelector("strong"),
    ).toHaveTextContent("—");
});

it("troca para Operação oculta dados financeiros já carregados", () => {
    const data = summary();
    const { rerender } = render(
        <StoredValueCard perfil="GESTOR" data={data} />,
    );
    expect(screen.getByText("R$ 125,25")).toBeInTheDocument();
    rerender(<StoredValueCard perfil="OPERACAO" data={data} />);
    expect(screen.queryByText("R$ 125,25")).not.toBeInTheDocument();
    expect(
        screen.getByText("Disponível para Supervisor e Gestor"),
    ).toBeInTheDocument();
});
