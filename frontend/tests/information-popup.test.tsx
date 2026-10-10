import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import {
    InformationCard,
    InformationHint,
    useInformationPopup,
} from "../src/components/information/InformationPopup";
import { information, metricInformation } from "../src/content/information";

afterEach(() => vi.useRealTimers());
const tick = (ms: number) => act(() => vi.advanceTimersByTime(ms));

it("cancela a abertura pendente ao rolar ou redimensionar", () => {
    vi.useFakeTimers();
    render(<InformationHint topic="free" />);
    const button = screen.getByRole("button");
    let top = 0;
    vi.spyOn(button, "getBoundingClientRect").mockImplementation(
        () => ({ top, left: 0 }) as DOMRect,
    );
    for (const event of ["scroll", "resize"]) {
        fireEvent.pointerEnter(screen.getByRole("button"));
        tick(100);
        top += 20;
        fireEvent(window, new Event(event));
        tick(300);
        expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    }
});

it("não abre ao cruzar um cartão rapidamente e permite percorrer o popup", () => {
    vi.useFakeTimers();
    render(
        <InformationCard content={information.occupancy}>
            Ocupação atual
        </InformationCard>,
    );
    const card = screen.getByRole("article");
    fireEvent.pointerEnter(card);
    tick(100);
    fireEvent.pointerLeave(card);
    tick(400);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    fireEvent.pointerEnter(card);
    tick(300);
    const tip = screen.getByRole("tooltip");
    expect(card).toHaveAttribute("aria-describedby", tip.id);
    fireEvent.pointerLeave(card);
    tick(80);
    fireEvent.pointerEnter(tip);
    tick(400);
    expect(tip).toBeInTheDocument();
    fireEvent.pointerLeave(tip);
    tick(200);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
});

it("mantém somente uma explicação aberta e fecha com Escape sem tirar o foco", () => {
    render(
        <>
            <InformationHint topic="free" />
            <InformationHint topic="occupied" />
        </>,
    );
    const buttons = screen.getAllByRole("button");
    act(() => buttons[0].focus());
    expect(screen.getByRole("tooltip")).toHaveTextContent(
        information.free.description,
    );
    act(() => buttons[1].focus());
    expect(screen.getAllByRole("tooltip")).toHaveLength(1);
    expect(screen.getByRole("tooltip")).toHaveTextContent(
        information.occupied.description,
    );
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    expect(buttons[1]).toHaveFocus();
});

it("preserva o clique do atalho e não dispara sua ação ao focar", () => {
    const click = vi.fn();
    render(
        <InformationCard
            as="button"
            content={information.receivingShortcut}
            onClick={click}
        >
            Receber
        </InformationCard>,
    );
    const button = screen.getByRole("button");
    fireEvent.focus(button);
    expect(click).not.toHaveBeenCalled();
    fireEvent.click(button);
    expect(click).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
});

it("não reabre conteúdo antigo após ocultar ou desabilitar o gatilho", () => {
    function Example({ disabled }: { disabled: boolean }) {
        const { anchorProps, popup } = useInformationPopup<HTMLButtonElement>(
            information.stored,
            disabled,
        );
        return (
            <>
                <button {...anchorProps} disabled={disabled}>
                    Unidades
                </button>
                {popup}
            </>
        );
    }
    const view = render(<Example disabled={false} />);
    fireEvent.focus(screen.getByRole("button"));
    expect(screen.getByRole("tooltip")).toBeInTheDocument();
    view.rerender(<Example disabled />);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    view.rerender(<Example disabled={false} />);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
});

it("fecha por clique externo e rolagem, mas permite rolar seu próprio conteúdo", () => {
    render(<InformationHint topic="warehouseMap" />);
    fireEvent.click(screen.getByRole("button"));
    fireEvent.scroll(screen.getByRole("tooltip"));
    expect(screen.getByRole("tooltip")).toBeInTheDocument();
    fireEvent.scroll(window);
    expect(screen.getByRole("tooltip")).toBeInTheDocument();
    const button = screen.getByRole("button");
    const rect = button.getBoundingClientRect();
    vi.spyOn(button, "getBoundingClientRect").mockReturnValue({
        ...rect,
        top: 20,
        left: 0,
    } as DOMRect);
    fireEvent.scroll(window);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button"));
    fireEvent.pointerDown(document.body);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
});

it("preserva valor ausente/restrito e limpa abertura pendente ao desmontar", () => {
    vi.useFakeTimers();
    const content = metricInformation(
        "storedValue",
        "—",
        "Disponível para Supervisor e Gestor",
    );
    const view = render(
        <InformationCard content={content}>Valor armazenado</InformationCard>,
    );
    fireEvent.focus(screen.getByRole("article"));
    expect(screen.getByRole("tooltip")).toHaveTextContent("—");
    expect(screen.getByRole("tooltip")).toHaveTextContent(
        "Disponível para Supervisor e Gestor",
    );
    fireEvent.keyDown(window, { key: "Escape" });
    fireEvent.pointerEnter(screen.getByRole("article"));
    view.unmount();
    tick(500);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
});
