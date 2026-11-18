import { fireEvent, render, screen } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { ResponsiveTable } from "../src/components/layout/ResponsiveTable";

it("mantém ações e cabeçalhos de linha e atualiza rótulos ao trocar a consulta", () => {
    const open = vi.fn();
    const view = (summary: boolean) => (
        <ResponsiveTable aria-label="Consulta">
            <thead>
                <tr>
                    <th>{summary ? "Produto" : "Pedido"}</th>
                    <th>Ações</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <th scope="row">Referência longa</th>
                    <td>
                        <button onClick={open}>Consultar</button>
                    </td>
                </tr>
                <tr>
                    <td colSpan={2}>Resumo da consulta</td>
                </tr>
            </tbody>
        </ResponsiveTable>
    );
    const { rerender } = render(view(false));
    expect(screen.getByRole("rowheader")).toHaveAttribute(
        "data-label",
        "Pedido",
    );
    fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
    expect(open).toHaveBeenCalledOnce();
    rerender(view(true));
    expect(screen.getByRole("rowheader")).toHaveAttribute(
        "data-label",
        "Produto",
    );
    expect(screen.getByText("Resumo da consulta")).not.toHaveAttribute(
        "data-label",
    );
});
