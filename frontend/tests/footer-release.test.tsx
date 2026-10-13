import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { AppFooter } from "../src/components/shell/AppFooter";

afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
});

function production() {
    vi.stubEnv("PROD", true);
    vi.stubEnv("VITE_WMS_PRODUCTION_RUN_ID", "active-run");
}
const reply = (runId = "active-run") =>
    new Response(
        JSON.stringify({
            runId,
            activatedUtc: "2026-10-13T22:08:09.1234567Z",
        }),
        { headers: { "Content-Type": "application/json" } },
    );

describe("rodapé da atualização de produção", () => {
    it("DEV não consulta nem exibe informação de publicação", () => {
        vi.stubEnv("PROD", false);
        vi.stubEnv("VITE_WMS_PRODUCTION_RUN_ID", "ignored");
        const fetcher = vi.fn();
        vi.stubGlobal("fetch", fetcher);
        render(<AppFooter />);
        expect(fetcher).not.toHaveBeenCalled();
        expect(
            screen.queryByText(/atualização|Atualizado/),
        ).not.toBeInTheDocument();
    });
    it("exibe o horário confirmado em Brasília, independente do relógio do navegador", async () => {
        production();
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue(reply()));
        render(<AppFooter />);
        const time = await screen.findByText("13/10/2026, 19:08:09");
        expect(time.tagName).toBe("TIME");
        expect(time).toHaveAttribute(
            "datetime",
            "2026-10-13T22:08:09.1234567Z",
        );
        expect(time.parentElement).toHaveTextContent("(Brasília)");
    });
    it("não inventa data quando o recibo está ausente ou inválido", async () => {
        production();
        const fetcher = vi
            .fn()
            .mockResolvedValueOnce(new Response("{}", { status: 503 }))
            .mockResolvedValueOnce(
                new Response(
                    JSON.stringify({
                        runId: "active-run",
                        activatedUtc: "invalid",
                    }),
                ),
            );
        vi.stubGlobal("fetch", fetcher);
        render(<AppFooter />);
        await screen.findByText("Atualização indisponível");
        fireEvent.focus(window);
        await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
        expect(document.querySelector("time")).toBeNull();
    });
    it("avisa uma aba antiga após nova publicação e não atribui a ela a data da nova versão", async () => {
        production();
        vi.stubGlobal(
            "fetch",
            vi
                .fn()
                .mockResolvedValueOnce(reply())
                .mockResolvedValueOnce(reply("new-run")),
        );
        render(<AppFooter />);
        await screen.findByText("13/10/2026, 19:08:09");
        fireEvent.focus(window);
        expect(
            await screen.findByRole("link", {
                name: "Nova versão disponível. Atualize a página.",
            }),
        ).toHaveAttribute("href", window.location.href);
        expect(document.querySelector("time")).toBeNull();
    });
});
