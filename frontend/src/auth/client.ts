import { stringifyExact } from "../contracts/codec";
import {
    decodeTokens,
    decodeUser,
    decodeUsersPage,
    encodeRevisionBody,
    type Tokens,
    type User,
} from "./contracts";
import { AuthError, requestAuth, type AuthScope } from "./request";
export { AuthError } from "./request";
export type { User, UsersPage } from "./contracts";

/** Access somente em memória; refresh em cookie HttpOnly. Sem replay de escrita. */
export class AuthClient {
    private access: string | null = null;
    private currentUser: User | null = null;
    private expires = 0;
    private generation = 0;
    private lifetime = new AbortController();
    private refreshing: Promise<User> | null = null;
    private cookieQueue: Promise<unknown> = Promise.resolve();
    private onExpired: () => void = () => {};
    constructor(
        private fetcher: typeof fetch = (input, init) => fetch(input, init),
    ) {}
    expired(callback: () => void) {
        this.onExpired = callback;
    }
    token = () => this.access;
    user = () => this.currentUser;
    clear() {
        this.lifetime.abort();
        this.lifetime = new AbortController();
        this.generation++;
        this.access = null;
        this.currentUser = null;
        this.expires = 0;
        this.refreshing = null;
    }
    private scope(bearer = true): AuthScope {
        const generation = this.generation,
            token = this.access;
        return {
            signal: this.lifetime.signal,
            token,
            current: () =>
                generation === this.generation &&
                (!bearer || token === this.access),
            expire: () => {
                // Uma rotação também pode substituir Bearer sem trocar usuário.
                if (
                    generation !== this.generation ||
                    (bearer && token !== this.access)
                )
                    return;
                this.clear();
                this.onExpired();
            },
        };
    }
    private accept(
        result: Tokens,
        scope: AuthScope,
        signal?: AbortSignal,
    ): User {
        if (!scope.current() || scope.signal.aborted || signal?.aborted)
            throw new AuthError(
                "A sessão foi encerrada.",
                401,
                false,
                "AUTH_CONTEXT_ENDED",
            );
        this.access = result.accessToken;
        this.currentUser = result.usuario;
        this.expires = Date.now() + 300000;
        return result.usuario;
    }
    private cookieLock<T>(action: () => Promise<T>): Promise<T> {
        // A fila local existe também onde Web Locks não está disponível.
        const run = () =>
            typeof navigator !== "undefined" && navigator.locks
                ? navigator.locks.request("wms-refresh", action)
                : action();
        const pending = this.cookieQueue.then(run, run);
        this.cookieQueue = pending.catch(() => {});
        return pending;
    }
    async login(email: string, senha: string, signal?: AbortSignal) {
        this.clear();
        const scope = this.scope(false),
            body = stringifyExact({ email, senha });
        return this.cookieLock(async () =>
            this.accept(
                await requestAuth(this.fetcher, scope, {
                    path: "/entrar",
                    method: "POST",
                    body,
                    bearer: false,
                    decode: decodeTokens,
                    signal,
                }),
                scope,
                signal,
            ),
        );
    }
    refresh(): Promise<User> {
        if (this.refreshing) return this.refreshing;
        const scope = this.scope(false);
        const pending = this.cookieLock(async () =>
            this.accept(
                await requestAuth(this.fetcher, scope, {
                    path: "/renovar",
                    method: "POST",
                    bearer: false,
                    decode: decodeTokens,
                }),
                scope,
            ),
        )
            .catch((error: unknown) => {
                if (scope.current()) {
                    this.clear();
                    this.onExpired();
                }
                throw error;
            })
            .finally(() => {
                if (this.refreshing === pending) this.refreshing = null;
            });
        this.refreshing = pending;
        return pending;
    }
    async fresh() {
        if (this.access && Date.now() >= this.expires - 30000)
            return this.refresh();
    }
    private async authenticatedScope(signal?: AbortSignal) {
        const scope = this.scope(false);
        await this.fresh();
        if (!scope.current() || signal?.aborted)
            throw new AuthError(
                "A sessão foi encerrada.",
                401,
                false,
                "AUTH_CONTEXT_ENDED",
            );
        return this.scope();
    }
    async logout() {
        this.clear();
        const scope = this.scope(false);
        await this.cookieLock(() =>
            requestAuth<void>(this.fetcher, scope, {
                path: "/sair",
                method: "POST",
                bearer: false,
            }),
        );
    }
    async changePassword(
        senhaAtual: string,
        novaSenha: string,
        signal?: AbortSignal,
    ) {
        const scope = await this.authenticatedScope(signal);
        await requestAuth<void>(this.fetcher, scope, {
            path: "/senha",
            method: "POST",
            body: stringifyExact({ senhaAtual, novaSenha }),
            signal,
        });
        if (scope.current()) this.clear();
    }
    async users(page: number, signal?: AbortSignal) {
        const scope = await this.authenticatedScope(signal);
        return requestAuth(this.fetcher, scope, {
            path: `/usuarios?pagina=${page}`,
            decode: decodeUsersPage,
            signal,
        });
    }
    async create(body: unknown, signal?: AbortSignal) {
        const wire = stringifyExact(body),
            scope = await this.authenticatedScope(signal);
        return requestAuth(this.fetcher, scope, {
            path: "/usuarios",
            method: "POST",
            body: wire,
            decode: decodeUser,
            signal,
        });
    }
    async edit(id: string, body: unknown, signal?: AbortSignal) {
        const wire = encodeRevisionBody(body),
            scope = await this.authenticatedScope(signal);
        return requestAuth(this.fetcher, scope, {
            path: `/usuarios/${encodeURIComponent(id)}`,
            method: "PUT",
            body: wire,
            decode: decodeUser,
            signal,
        });
    }
    async reset(
        id: string,
        senhaTemporaria: string,
        versao: string,
        signal?: AbortSignal,
    ) {
        const wire = encodeRevisionBody({ senhaTemporaria, versao }),
            scope = await this.authenticatedScope(signal);
        return requestAuth<void>(this.fetcher, scope, {
            path: `/usuarios/${encodeURIComponent(id)}/senha`,
            method: "POST",
            body: wire,
            signal,
        });
    }
}
