import { render, screen } from "@testing-library/react";
import { expect, it } from "vitest";
import { Result } from "../src/components/Result";

it("compacta coleções vazias sem omitir zero, falso ou dados de coleções preenchidas", () => {
    const { container } = render(
        <Result
            data={{
                versao: 0,
                ativo: false,
                reservas: [],
                documentos: {},
                itens: [{ codigo: "ITEM-PRESERVADO" }],
            }}
        />,
    );
    expect(screen.getByText("0", { selector: "dd" })).toBeVisible();
    expect(screen.getByText("Não", { selector: "dd" })).toBeVisible();
    expect(screen.getAllByText("Nenhum registro informado.")).toHaveLength(2);
    expect(container.querySelectorAll(".result-section")).toHaveLength(1);
    expect(screen.getByRole("cell", { name: "ITEM-PRESERVADO" })).toBeVisible();
});

it("explica respostas sem campos em vez de desenhar uma lista vazia", () => {
    const { container } = render(<Result data={{}} />);
    expect(screen.getByText("Nenhum dado informado.")).toBeVisible();
    expect(container.querySelector("dl")).toBeNull();
});
