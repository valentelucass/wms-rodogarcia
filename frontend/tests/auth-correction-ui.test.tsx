import { fixture } from "../src/api/mock/fixtures";
import { toWire, stringifyExact } from "../src/contracts/codec";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AuthClient, AuthError, type User } from "../src/auth/client";
import { UsersPage } from "../src/auth/UsersPage";
import { AuthApp } from "../src/auth/AuthApp";
const user: User = {
    id: "fixture-user",
    nome: "Pessoa sintetica",
    email: "pessoa@fixture.invalid",
    perfil: "OPERACAO",
    administrador: true,
    principal: false,
    ativo: true,
    trocarSenha: false,
    clientes: ["1"],
    armazens: ["1"],
    versao: "0",
};
const page = { content: [user], number: 0, totalPages: 1, totalElements: "1" };
afterEach(() => {
    vi.unstubAllGlobals();
    history.replaceState(null, "", "/");
});
describe("006 atual DS externo, sem patch Lume", () => {
    it.each(["click", "Enter"])(
        "006 skip %s preserva rota/jornada/contexto/foco",
        async (mode) => {
            vi.stubGlobal(
                "fetch",
                vi.fn<typeof fetch>().mockImplementation(
                    async (input) =>
                        new Response(
                            stringifyExact(
                                toWire(
                                    String(input).includes("clientes")
                                        ? "PaginaResponse<ClienteDto.Resposta>"
                                        : String(input).includes("armazens")
                                          ? "PaginaResponse<ArmazemDto.Resposta>"
                                          : "unknown",
                                    String(input).includes("/api/v1/")
                                        ? {
                                              itens: [
                                                  fixture(
                                                      String(input).includes(
                                                          "clientes",
                                                      )
                                                          ? "ClienteDto.Resposta"
                                                          : "ArmazemDto.Resposta",
                                                  ),
                                              ],
                                              pagina: 0,
                                              tamanho: 100,
                                              totalItens: 1,
                                              totalPaginas: 1,
                                          }
                                        : String(input).endsWith("/csrf")
                                          ? {
                                                token: "csrf-fixture",
                                                header: "X-XSRF-TOKEN",
                                            }
                                          : {
                                                accessToken: "access-fixture",
                                                expiresIn: 300,
                                                usuario: { ...user, versao: 0 },
                                            },
                                ),
                            ),
                        ),
                ),
            );
            render(<AuthApp />);
            await screen.findByRole("button", { name: "Cadastros" });
            fireEvent.click(screen.getByRole("button", { name: "Cadastros" }));
            await screen.findByRole("heading", { name: "Cadastros" });
            for (const title of ["Cliente", "Armazém"]) {
                const input = screen.getByRole("combobox", { name: title });
                await waitFor(() => expect(input).toBeEnabled());
                fireEvent.click(input);
                fireEvent.change(input, { target: { value: "Exemplo" } });
                fireEvent.keyDown(input, { key: "Enter" });
            }
            fireEvent.submit(
                screen
                    .getByRole("button", { name: "Aplicar contexto" })
                    .closest("form")!,
            );
            const skip = screen.getByRole("link", {
                name: "Ir para o conteúdo",
            });
            skip.focus();
            if (mode === "click") fireEvent.click(skip);
            else await userEvent.setup().keyboard("{Enter}");
            await act(async () => {
                await new Promise((resolve) => setTimeout(resolve, 30));
            });
            expect(location.hash).toBe("#cadastros");
            expect(
                screen.getByRole("heading", { name: "Cadastros" }),
            ).toBeInTheDocument();
            expect(
                screen.getByText("Contexto selecionado."),
            ).toBeInTheDocument();
            expect(document.activeElement).toBe(
                document.getElementById("conteudo"),
            );
        },
    );
});
describe("007 consulta usuarios e003 feedback escrita", () => {
    it.each([401, 403])(
        "007 recusa%i termina loading e permite recuperar consulta",
        async (status) => {
            const auth = new AuthClient();
            vi.spyOn(auth, "users")
                .mockRejectedValueOnce(
                    new AuthError("Recusa sintetica", status),
                )
                .mockResolvedValueOnce(page);
            render(<UsersPage auth={auth} current={user} />);
            await screen.findByRole("alert");
            expect(screen.queryByText("Carregando usuários…")).toBeNull();
            fireEvent.click(
                screen.getByRole("button", { name: "Consultar novamente" }),
            );
            await screen.findByText("Pessoa sintetica");
            expect(screen.queryByRole("alert")).toBeNull();
        },
    );
    it("007 vazio e erro de rede distintos", async () => {
        const auth = new AuthClient();
        vi.spyOn(auth, "users")
            .mockRejectedValueOnce(new AuthError("Rede indisponível", 0))
            .mockResolvedValueOnce({
                ...page,
                content: [],
                totalElements: "0",
                totalPages: 0,
            });
        render(<UsersPage auth={auth} current={user} />);
        await screen.findByRole("alert");
        expect(screen.queryByText("Nenhum usuário encontrado.")).toBeNull();
        fireEvent.click(
            screen.getByRole("button", { name: "Consultar novamente" }),
        );
        await screen.findByText("Nenhum usuário encontrado.");
        expect(screen.queryByRole("alert")).toBeNull();
    });
    it("007 pagina antiga cancelada nao substitui pagina atual", async () => {
        const auth = new AuthClient();
        let finish!: (p: typeof page) => void;
        const users = vi
            .spyOn(auth, "users")
            .mockResolvedValueOnce({ ...page, totalPages: 2 })
            .mockImplementationOnce(
                () =>
                    new Promise((resolve) => {
                        finish = resolve;
                    }),
            )
            .mockResolvedValueOnce({
                ...page,
                content: [{ ...user, nome: "Pessoa atual" }],
            });
        const mounted = render(<UsersPage auth={auth} current={user} />);
        await screen.findByText("Pessoa sintetica");
        fireEvent.click(screen.getByRole("button", { name: "Próxima" }));
        await waitFor(() => expect(finish).toBeTypeOf("function"));
        const oldSignal = users.mock.calls[1][1];
        mounted.unmount();
        render(<UsersPage auth={auth} current={user} />);
        await screen.findByText("Pessoa atual");
        await act(async () => {
            finish(page);
        });
        expect(oldSignal?.aborted).toBe(true);
        expect(screen.queryByText("Pessoa sintetica")).toBeNull();
    });
    it("003 criar201null nao indica sucesso; resultado incerto exige consulta sem replay", async () => {
        const fetcher = vi.fn<typeof fetch>().mockImplementation(
            async (input, init) =>
                new Response(
                    JSON.stringify(
                        String(input).includes("/api/v1/")
                            ? {
                                  itens: [
                                      fixture(
                                          String(input).includes("clientes")
                                              ? "ClienteDto.Resposta"
                                              : "ArmazemDto.Resposta",
                                      ),
                                  ],
                                  pagina: 0,
                                  tamanho: 100,
                                  totalItens: 1,
                                  totalPaginas: 1,
                              }
                            : String(input).endsWith("/csrf")
                              ? { token: "fixture", header: "X-XSRF-TOKEN" }
                              : init?.method === "POST"
                                ? null
                                : {
                                      ...page,
                                      content: [{ ...user, versao: 0 }],
                                      totalElements: 1,
                                  },
                    ),
                    { status: init?.method === "POST" ? 201 : 200 },
                ),
        );
        render(<UsersPage auth={new AuthClient(fetcher)} current={user} />);
        await screen.findByText("Pessoa sintetica");
        fireEvent.click(screen.getByRole("button", { name: "Criar usuário" }));
        fireEvent.change(screen.getByLabelText("Nome"), {
            target: { value: "Pessoa nova" },
        });
        fireEvent.change(screen.getByLabelText("E-mail"), {
            target: { value: "nova@fixture.invalid" },
        });
        fireEvent.change(screen.getByLabelText(/^Senha temporária/), {
            target: { value: "Sintetica-temporaria-123" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Salvar usuário" }));
        expect(await screen.findByRole("alert")).toHaveTextContent("incerto");
        expect(screen.queryByText(/Usuário criado\./)).toBeNull();
        expect(
            fetcher.mock.calls.filter(([, init]) => init?.method === "POST"),
        ).toHaveLength(1);
        expect(
            screen.getByRole("button", { name: "Salvar usuário" }),
        ).toBeDisabled();
        fireEvent.click(
            screen.getByRole("button", { name: "Consultar novamente" }),
        );
        await waitFor(() =>
            expect(
                screen.queryByRole("button", { name: "Salvar usuário" }),
            ).toBeNull(),
        );
        expect(
            fetcher.mock.calls.filter(([, init]) => init?.method === "POST"),
        ).toHaveLength(1);
    });
});
