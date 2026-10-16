import {
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { Result } from "../src/components/Result";
import { Dialog } from "../src/components/layout/Dialog";

afterEach(() => vi.restoreAllMocks());
const fullText =
    'Relato integral: avaria após reserva.\nManter lote "0000123" e quantidade 1.125000. <img src=x onerror=alert(1)> ' +
    "Detalhe da ocorrência. ".repeat(12);
function measure(initialWidth = 120) {
    let width = initialWidth;
    vi.spyOn(HTMLElement.prototype, "clientWidth", "get").mockImplementation(
        function (this: HTMLElement) {
            return this.classList.contains("table-cell-text") ? width : 0;
        },
    );
    vi.spyOn(HTMLElement.prototype, "scrollWidth", "get").mockImplementation(
        function (this: HTMLElement) {
            return this.classList.contains("table-cell-text")
                ? (this.textContent?.length ?? 0) * 7
                : 0;
        },
    );
    return (value: number) => {
        width = value;
        fireEvent.resize(window);
    };
}

it("abre o texto integral sem selecionar o registro, escapa HTML e fecha ao substituir o conteúdo consultado", () => {
    measure();
    const select = vi.fn();
    const row = { id: "9007199254740993", relato: fullText };
    const view = render(
        <Result
            data={[row]}
            type="List<AvariaDto.Ocorrencia>"
            onSelect={select}
        />,
    );
    const trigger = screen.getByRole("button", {
        name: "Ler conteúdo completo de Relato",
    });
    expect(trigger.textContent).toBe(fullText);
    fireEvent.click(trigger);
    const dialog = screen.getByRole("dialog", {
        name: "Conteúdo completo: Relato",
    });
    expect(dialog.querySelector(".table-text-content")?.textContent).toBe(
        fullText,
    );
    expect(dialog.querySelector("img")).toBeNull();
    expect(select).not.toHaveBeenCalled();
    view.rerender(
        <Result
            data={[{ ...row, relato: "Outro conteúdo autorizado" }]}
            type="List<AvariaDto.Ocorrencia>"
            onSelect={select}
        />,
    );
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.queryByText(fullText, { normalizer: (x) => x })).toBeNull();
});

it("só oferece popup quando o texto não cabe, acompanha redimensionamento e mantém zero e falso", () => {
    const resize = measure(5000);
    render(
        <Result
            data={[
                {
                    id: "9007199254740993",
                    relato: fullText,
                    proporcaoSuspensa: "0",
                    ativo: false,
                },
            ]}
            type="List<AvariaDto.Ocorrencia>"
            pending
            onSelect={vi.fn()}
        />,
    );
    expect(
        screen.queryByRole("button", {
            name: "Ler conteúdo completo de Relato",
        }),
    ).toBeNull();
    fireEvent.click(screen.getByText(fullText, { normalizer: (x) => x }));
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.getByText("0", { exact: true })).toBeVisible();
    expect(screen.getByText("Não", { exact: true })).toBeVisible();
    resize(120);
    fireEvent.keyDown(
        screen.getByRole("button", { name: "Ler conteúdo completo de Relato" }),
        { key: "Enter" },
    );
    const dialog = screen.getByRole("dialog", {
        name: "Conteúdo completo: Relato",
    });
    expect(dialog.querySelector(".table-text-content")?.textContent).toBe(
        fullText,
    );
    expect(
        screen.getByRole("button", { name: "Selecionar registro 1" }),
    ).toBeDisabled();
});

it("Space abre; fechar e cancelar devolvem o foco e preservam o diálogo de origem", async () => {
    measure();
    const closeParent = vi.fn();
    render(
        <Dialog title="Detalhe da avaria" onClose={closeParent}>
            <Result
                data={[{ id: "1", relato: fullText }]}
                type="List<AvariaDto.Ocorrencia>"
            />
        </Dialog>,
    );
    const trigger = screen.getByRole("button", {
        name: "Ler conteúdo completo de Relato",
    });
    trigger.focus();
    fireEvent.keyDown(trigger, { key: " " });
    const dialog = screen.getByRole("dialog", {
        name: "Conteúdo completo: Relato",
    });
    fireEvent.click(
        within(dialog).getByRole("button", { name: "Fechar diálogo" }),
    );
    await waitFor(() => expect(trigger).toHaveFocus());
    fireEvent.click(trigger);
    fireEvent(
        screen.getByRole("dialog", { name: "Conteúdo completo: Relato" }),
        new Event("cancel", { bubbles: true, cancelable: true }),
    );
    await waitFor(() => expect(trigger).toHaveFocus());
    expect(
        screen.queryByRole("dialog", { name: "Conteúdo completo: Relato" }),
    ).toBeNull();
    expect(
        screen.getByRole("dialog", { name: "Detalhe da avaria" }),
    ).toBeVisible();
    expect(closeParent).not.toHaveBeenCalled();
});
