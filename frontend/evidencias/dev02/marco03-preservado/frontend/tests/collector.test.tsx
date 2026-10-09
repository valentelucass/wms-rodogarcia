import { it, expect, vi } from "vitest";
import { render, screen, fireEvent, act } from "@testing-library/react";
import { Collector } from "../src/components/Collector";
import { fixture } from "../src/api/mock/fixtures";
import { demoCode } from "../src/api/mock/constants";
import type { Transport, Receipt } from "../src/api/client";
it.each(["código", "modo", "contexto", "desmontagem"])(
    "resposta atrasada após mudar %s não reaproveita unidade anterior",
    async (change) => {
        const resolvers: ((r: Receipt) => void)[] = [];
        const transport: Transport = {
            send: vi.fn(
                () =>
                    new Promise<Receipt>((resolve) => resolvers.push(resolve)),
            ),
        };
        const { unmount, rerender } = render(
            <Collector
                transport={transport}
                context={{ clienteId: "1", armazemId: "1" }}
                perfil="OPERACAO"
            />,
        );
        fireEvent.change(screen.getByLabelText("1. Leia o UUID da unidade"), {
            target: { value: demoCode },
        });
        fireEvent.click(
            screen.getByRole("button", {
                name: "Consultar unidade e posições",
            }),
        );
        if (change === "código")
            fireEvent.change(
                screen.getByLabelText("1. Leia o UUID da unidade"),
                { target: { value: "00000000-0000-4000-8000-000000000002" } },
            );
        else if (change === "modo")
            fireEvent.change(screen.getByLabelText("Tarefa"), {
                target: { value: "contar" },
            });
        else if (change === "contexto")
            rerender(
                <Collector
                    transport={transport}
                    context={{ clienteId: "2", armazemId: "2" }}
                    perfil="OPERACAO"
                />,
            );
        else unmount();
        await act(async () => {
            resolvers[0]({
                data: fixture("EstoqueDto.Unidade"),
                raw: "",
                requestId: "demo",
                ficticio: true,
                replay: false,
            });
            resolvers[1]({
                data: fixture("PaginaResponse<EnderecoDto.Resposta>"),
                raw: "",
                requestId: "demo",
                ficticio: true,
                replay: false,
            });
        });
        expect(
            screen.queryByLabelText("2. Leia o código da posição"),
        ).not.toBeInTheDocument();
        expect(
            screen.queryByText(
                "Identidade, condição e localização consultadas",
            ),
        ).not.toBeInTheDocument();
    },
);
