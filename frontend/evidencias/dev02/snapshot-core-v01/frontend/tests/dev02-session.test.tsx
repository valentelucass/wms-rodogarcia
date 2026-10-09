import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import App from "../src/App";
import { useExerciseSession } from "../src/hooks/useExerciseSession";
import { realTransport } from "../src/api/client";
import { endpoint } from "../src/contracts/runtime";

vi.mock("../src/hooks/useExerciseSession", () => ({
    useExerciseSession: vi.fn(),
}));
afterEach(() => vi.unstubAllEnvs());
describe("DEV02 fronteira de sessao: nenhuma identidade ficticia em REAL", () => {
    it.each([undefined, "real", "invalid"])(
        "modo %s nao instancia sessao/exercicio nem envia rede",
        (mode) => {
            vi.stubEnv("VITE_DATA_MODE", mode);
            vi.stubEnv("MODE", "development");
            const fetcher = vi.spyOn(globalThis, "fetch");
            render(<App />);
            expect(
                screen.getByRole("heading", {
                    name: "Sessão real indisponível",
                }),
            ).toBeInTheDocument();
            expect(screen.getByRole("status")).toHaveTextContent(
                "Nenhuma consulta ou comando operacional foi enviado",
            );
            expect(
                screen.queryByLabelText("Perfil de apresentação fictício"),
            ).not.toBeInTheDocument();
            expect(
                screen.queryByText("Início da operação"),
            ).not.toBeInTheDocument();
            expect(useExerciseSession).not.toHaveBeenCalled();
            expect(fetcher).not.toHaveBeenCalled();
            fetcher.mockRestore();
        },
    );
    it("Bearer ausente recusa antes de fetch, sem token demo/cookie/fallback", async () => {
        const fetcher = vi.fn();
        await expect(
            realTransport({
                baseUrl: "http://127.0.0.1:25581",
                allowReal: true,
                token: () => null,
                onExpired: vi.fn(),
                fetcher,
            }).send({
                endpoint: endpoint("ClienteController.listar"),
                params: {},
                query: {},
                signal: new AbortController().signal,
            }),
        ).rejects.toMatchObject({
            code: "NAO_AUTENTICADO",
            status: 401,
            uncertain: false,
        });
        expect(fetcher).not.toHaveBeenCalled();
        expect(localStorage.length).toBe(0);
        expect(sessionStorage.length).toBe(0);
    });
});
