import { fireEvent, render, screen, within } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Result } from "../src/components/Result";
import {
    resultRowDetailRows as rows,
    resultRowDetailType as type,
} from "./support/resultRowDetail";

it("abre o detalhe em outra linha abrangendo todas as colunas, preservando os limites", () => {
    const { container } = render(
        <Result data={rows} type={`List<${type}>`} onSelect={() => {}} />,
    );
    const trigger = screen.getByRole("button", { name: "Ver registro 1" });
    expect(trigger).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(trigger);
    const panel = screen.getByRole("region", {
        name: "Detalhes do registro 1",
    });
    expect(panel.id).toBe(trigger.getAttribute("aria-controls"));
    expect(trigger).toHaveAttribute("aria-expanded", "true");
    expect(panel.closest(".result-row-actions")).toBeNull();
    expect(panel.closest("tr")).toHaveClass("result-detail-row");
    expect(panel.closest("td")).toHaveAttribute(
        "colspan",
        String(container.querySelectorAll("thead > tr > th").length),
    );
    expect(within(panel).getAllByText("5.000")).toHaveLength(3);
    expect(within(panel).getByText("5", { exact: true })).toBeVisible();
    expect(
        within(panel).getByText("ARMAZENAGEM", { exact: true }),
    ).toBeVisible();
});

it("fecha pelo painel com foco no botão de origem e troca o detalhe sem selecionar automaticamente", () => {
    const select = vi.fn();
    render(<Result data={rows} type={`List<${type}>`} onSelect={select} />);
    const first = screen.getByRole("button", { name: "Ver registro 1" });
    fireEvent.click(first);
    fireEvent.click(
        screen.getByRole("button", { name: "Fechar detalhes do registro 1" }),
    );
    expect(
        screen.queryByRole("region", { name: "Detalhes do registro 1" }),
    ).toBeNull();
    expect(first).toHaveFocus();
    fireEvent.click(first);
    fireEvent.click(screen.getByRole("button", { name: "Ver registro 2" }));
    expect(first).toHaveAttribute("aria-expanded", "false");
    expect(
        screen.queryByRole("region", { name: "Detalhes do registro 1" }),
    ).toBeNull();
    expect(
        within(
            screen.getByRole("region", { name: "Detalhes do registro 2" }),
        ).getAllByText("0", { exact: true }),
    ).toHaveLength(2);
    expect(select).not.toHaveBeenCalled();
    fireEvent.click(
        screen.getByRole("button", { name: "Selecionar registro 2" }),
    );
    expect(select).toHaveBeenCalledExactlyOnceWith(rows[1], type);
});

it("não reaproveita o detalhe da linha anterior após troca dos registros consultados", () => {
    const view = render(<Result data={rows} type={`List<${type}>`} />);
    fireEvent.click(screen.getByRole("button", { name: "Ver registro 1" }));
    view.rerender(
        <Result
            data={[{ ...rows[0], id: "9007199254741011", codigo: "QA-OUTRO" }]}
            type={`List<${type}>`}
        />,
    );
    expect(
        screen.queryByRole("region", { name: "Detalhes do registro 1" }),
    ).toBeNull();
    expect(
        screen.getByRole("button", { name: "Ver registro 1" }),
    ).toHaveAttribute("aria-expanded", "false");
    expect(screen.getByRole("cell", { name: "QA-OUTRO" })).toBeVisible();
});

it("bloqueia seleção da linha e do detalhe enquanto uma página está atualizando", () => {
    const select = vi.fn(),
        page = {
            itens: rows,
            pagina: 0,
            totalPaginas: 1,
            totalItens: "2",
            tamanho: 20,
        };
    const view = render(
        <Result
            data={page}
            type={`PaginaResponse<${type}>`}
            onSelect={select}
        />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Ver registro 1" }));
    view.rerender(
        <Result
            data={page}
            type={`PaginaResponse<${type}>`}
            onSelect={select}
            pending
        />,
    );
    for (const button of screen.getAllByRole("button", {
        name: /Selecionar registro|Selecionar este registro/,
    })) {
        expect(button).toBeDisabled();
        fireEvent.click(button);
    }
    expect(select).not.toHaveBeenCalled();
    fireEvent.click(
        screen.getByRole("button", { name: "Fechar detalhes do registro 1" }),
    );
    expect(
        screen.queryByRole("region", { name: "Detalhes do registro 1" }),
    ).toBeNull();
});

it("mantém a identidade e série zero da nota, e não oferece seleção sem callback", () => {
    const notes = [
            {
                id: "9007199254741009",
                numero: "15102030",
                serie: 0,
                xmlVinculado: false,
            },
        ],
        select = vi.fn();
    const view = render(
        <Result
            data={notes}
            type="List<PedidoEntradaDto.Nota>"
            onSelect={select}
        />,
    );
    fireEvent.click(
        screen.getByRole("button", {
            name: "Ver detalhes da nota 15102030, série 0",
        }),
    );
    expect(
        screen.getByRole("region", {
            name: "Detalhes da nota 15102030, série 0",
        }),
    ).toHaveTextContent("Não");
    fireEvent.click(
        screen.getByRole("button", {
            name: "Selecionar nota 15102030, série 0",
        }),
    );
    expect(select).toHaveBeenCalledExactlyOnceWith(
        notes[0],
        "PedidoEntradaDto.Nota",
    );
    view.rerender(<Result data={notes} type="List<PedidoEntradaDto.Nota>" />);
    expect(screen.queryByRole("button", { name: /Selecionar/ })).toBeNull();
});
