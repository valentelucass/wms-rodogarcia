import type { Perfil } from "../contracts/runtime";

export interface User {
    id: string;
    nome: string;
    email: string;
    perfil: Perfil;
    administrador: boolean;
    principal: boolean;
    ativo: boolean;
    trocarSenha: boolean;
    clientes: string[];
    armazens: string[];
    versao: number;
}
interface Tokens {
    accessToken: string;
    expiresIn: number;
    usuario: User;
}
export interface UsersPage {
    content: User[];
    number: number;
    totalPages: number;
    totalElements: number;
}
export class AuthError extends Error {
    constructor(
        message: string,
        public status: number,
    ) {
        super(message);
    }
}

/** Access token somente em memória; refresh somente em cookie HttpOnly. Sem repetição de escrita. */
export class AuthClient {
    private access: string | null = null;
    private expires = 0;
    private refreshing: Promise<User> | null = null;
    private generation = 0;
    private onExpired: () => void = () => {};
    constructor(
        private fetcher: typeof fetch = (input, init) => fetch(input, init),
    ) {}
    expired(callback: () => void) {
        this.onExpired = callback;
    }
    token = () => this.access;
    clear() {
        this.generation++;
        this.access = null;
        this.expires = 0;
    }
    private async request<T>(
        path: string,
        method = "GET",
        body?: unknown,
        bearer = true,
    ): Promise<T> {
        const headers: Record<string, string> = { Accept: "application/json" };
        if (bearer && this.access)
            headers.Authorization = `Bearer ${this.access}`;
        if (method !== "GET") {
            const csrf = await this.request<{ token: string; header: string }>(
                "/csrf",
                "GET",
                undefined,
                false,
            );
            headers[csrf.header] = csrf.token;
            headers["Content-Type"] = "application/json";
        }
        let response: Response;
        try {
            response = await this.fetcher(`/api/auth${path}`, {
                method,
                headers,
                body: body === undefined ? undefined : JSON.stringify(body),
                credentials: "same-origin",
                cache: "no-store",
                redirect: "error",
                signal: AbortSignal.timeout(20000),
            });
        } catch {
            throw new AuthError(
                "Não foi possível confirmar a resposta. Consulte o estado atual antes de repetir.",
                0,
            );
        }
        if (!response.ok) {
            if (response.status === 401 && bearer) {
                this.clear();
                this.onExpired();
            }
            let detail =
                "Não foi possível concluir. Confira os dados e tente novamente.";
            try {
                const problem = (await response.json()) as { detail?: unknown };
                if (typeof problem.detail === "string") detail = problem.detail;
            } catch {
                /* Resposta sem JSON. */
            }
            throw new AuthError(detail, response.status);
        }
        if (response.status === 204) return undefined as T;
        return (await response.json()) as T;
    }
    private accept(result: Tokens, generation: number): User {
        if (generation !== this.generation)
            throw new AuthError("A sessão foi encerrada.", 401);
        if (
            typeof result.accessToken !== "string" ||
            result.expiresIn !== 300 ||
            !result.usuario?.id ||
            !["GESTOR", "SUPERVISOR", "OPERACAO"].includes(
                result.usuario.perfil,
            )
        ) {
            this.clear();
            throw new AuthError("Resposta de login inválida.", 0);
        }
        this.access = result.accessToken;
        this.expires = Date.now() + result.expiresIn * 1000;
        return result.usuario;
    }
    async login(email: string, senha: string) {
        this.clear();
        const generation = this.generation;
        return this.cookieLock(async () =>
            this.accept(
                await this.request<Tokens>(
                    "/entrar",
                    "POST",
                    { email, senha },
                    false,
                ),
                generation,
            ),
        );
    }
    private cookieLock<T>(action: () => Promise<T>): Promise<T> {
        return typeof navigator !== "undefined" && navigator.locks
            ? navigator.locks.request("wms-refresh", action)
            : action();
    }
    refresh(): Promise<User> {
        if (this.refreshing) return this.refreshing;
        const generation = this.generation;
        const run = async () =>
            this.accept(
                await this.request<Tokens>(
                    "/renovar",
                    "POST",
                    undefined,
                    false,
                ),
                generation,
            );
        // Abas da mesma origem compartilham o cookie; serializar sua rotação.
        this.refreshing = this.cookieLock(run)
            .catch((error: unknown) => {
                if (generation === this.generation) {
                    this.clear();
                    this.onExpired();
                }
                throw error;
            })
            .finally(() => {
                this.refreshing = null;
            });
        return this.refreshing;
    }
    async fresh() {
        if (this.access && Date.now() >= this.expires - 30000)
            await this.refresh();
    }
    async logout() {
        this.clear();
        await this.cookieLock(() =>
            this.request<void>("/sair", "POST", undefined, false),
        );
    }
    async changePassword(senhaAtual: string, novaSenha: string) {
        await this.fresh();
        await this.request<void>("/senha", "POST", { senhaAtual, novaSenha });
        this.clear();
    }
    async users(page: number) {
        await this.fresh();
        return this.request<UsersPage>(`/usuarios?pagina=${page}`);
    }
    async create(body: unknown) {
        await this.fresh();
        return this.request<User>("/usuarios", "POST", body);
    }
    async edit(id: string, body: unknown) {
        await this.fresh();
        return this.request<User>(
            `/usuarios/${encodeURIComponent(id)}`,
            "PUT",
            body,
        );
    }
    async reset(id: string, senhaTemporaria: string, versao: number) {
        await this.fresh();
        return this.request<void>(
            `/usuarios/${encodeURIComponent(id)}/senha`,
            "POST",
            { senhaTemporaria, versao },
        );
    }
}
