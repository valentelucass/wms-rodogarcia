import { isObject } from "../contracts/runtime";
import { parseExact } from "../contracts/codec";
import { decodeAuth, decodeCsrf } from "./contracts";

export class AuthError extends Error {
    constructor(
        message: string,
        public status: number,
        public uncertain = false,
        public code = "AUTH_HTTP",
        public requestId = "",
    ) {
        super(message);
    }
}
export interface AuthScope {
    signal: AbortSignal;
    token: string | null;
    current: () => boolean;
    expire: () => void;
}
/** Uma requisição mantém a identidade capturada durante CSRF, fetch e leitura.
 * Fetchers podem responder apesar do abort; a guarda deve anteceder efeitos. */
export async function requestAuth<T>(
    fetcher: typeof fetch,
    scope: AuthScope,
    options: {
        path: string;
        method?: string;
        body?: string;
        bearer?: boolean;
        decode?: (value: unknown) => T;
        signal?: AbortSignal;
    },
): Promise<T> {
    const { path, method = "GET", body, bearer = true } = options;
    const controller = new AbortController();
    const signals = [scope.signal, options.signal].filter(
        (v): v is AbortSignal => !!v,
    );
    const stop = () => controller.abort();
    signals.forEach((signal) => {
        signal.addEventListener("abort", stop, { once: true });
        if (signal.aborted) stop();
    });
    const timeout = setTimeout(stop, 20000);
    let sentWrite = false;
    const assertCurrent = () => {
        if (!scope.current() || controller.signal.aborted)
            throw new AuthError(
                sentWrite
                    ? "Contexto encerrado. O resultado do envio não foi confirmado; consulte o estado atual."
                    : "A solicitação foi cancelada ou a sessão foi encerrada.",
                401,
                sentWrite,
                "AUTH_CONTEXT_ENDED",
            );
    };
    async function fetchStage(
        stagePath: string,
        stageMethod: string,
        headers: Record<string, string>,
        stageBody?: string,
    ) {
        assertCurrent();
        if (stageMethod !== "GET") sentWrite = true;
        const response = await fetcher(`/api/auth${stagePath}`, {
            method: stageMethod,
            headers,
            body: stageBody,
            credentials: "same-origin",
            cache: "no-store",
            redirect: "error",
            signal: controller.signal,
        });
        assertCurrent();
        const raw = await response.text();
        assertCurrent();
        if (!response.ok) {
            if (response.status === 401 && bearer && stagePath === path)
                scope.expire();
            let detail =
                response.status === 401
                    ? "Sua sessão terminou. Entre novamente."
                    : response.status === 403
                      ? "Você não tem permissão para esta ação."
                      : "Não foi possível concluir. Confira os dados e tente novamente.";
            try {
                const problem = parseExact(raw);
                if (isObject(problem) && typeof problem.detail === "string")
                    detail = problem.detail;
            } catch {
                /* HTML/null/JSON inválido mantêm status. */
            }
            const uncertain = sentWrite && response.status >= 500;
            throw new AuthError(
                uncertain
                    ? "O servidor não confirmou o resultado. Consulte o estado atual antes de repetir."
                    : detail,
                response.status,
                uncertain,
                "AUTH_HTTP",
                response.headers.get("X-Request-Id") ?? "",
            );
        }
        return { response, raw };
    }
    try {
        const headers: Record<string, string> = { Accept: "application/json" };
        if (bearer && scope.token)
            headers.Authorization = `Bearer ${scope.token}`;
        if (method !== "GET") {
            const csrf = await fetchStage("/csrf", "GET", {
                Accept: "application/json",
            });
            let value;
            try {
                value = decodeAuth(csrf.raw, decodeCsrf);
            } catch {
                throw new AuthError(
                    "Resposta CSRF incompatível. Nenhum comando foi enviado.",
                    csrf.response.status,
                    false,
                    "AUTH_CONTRACT",
                );
            }
            headers[value.header] = value.token;
            headers["Content-Type"] = "application/json";
        }
        const { response, raw } = await fetchStage(path, method, headers, body);
        try {
            if (!options.decode) {
                if (response.status !== 204 || raw !== "")
                    throw new Error("Confirmação204 esperada.");
                return undefined as T;
            }
            if (response.status === 204)
                throw new Error("DTO obrigatório ausente.");
            return decodeAuth(raw, options.decode);
        } catch {
            throw new AuthError(
                sentWrite
                    ? "Resposta incompatível. O resultado do envio é incerto; consulte o estado atual antes de repetir."
                    : "Resposta incompatível com o contrato. Consulte novamente.",
                response.status,
                sentWrite,
                "AUTH_CONTRACT",
                response.headers.get("X-Request-Id") ?? "",
            );
        }
    } catch (error) {
        if (error instanceof AuthError) throw error;
        assertCurrent();
        throw new AuthError(
            sentWrite
                ? "Não foi possível confirmar o envio. Consulte o estado atual antes de repetir."
                : "Não foi possível consultar o WMS. Tente novamente.",
            0,
            sentWrite,
            "AUTH_NETWORK",
        );
    } finally {
        clearTimeout(timeout);
        signals.forEach((signal) => signal.removeEventListener("abort", stop));
    }
}
