import {
    endpoint,
    isObject,
    type Endpoint,
    type Values,
} from "../contracts/runtime";
import type { ApiContracts } from "../contracts/types";
import { encode, parseResponse, parseExact } from "../contracts/codec";
export interface Request {
    endpoint: Endpoint;
    params: Values;
    query: Values;
    body?: unknown;
    file?: File;
    signal: AbortSignal;
}
export interface Receipt {
    data: unknown;
    requestId: string;
    raw: string;
    ficticio: boolean;
    replay: boolean;
}
export interface Transport {
    send(request: Request): Promise<Receipt>;
}
export class ApiError extends Error {
    constructor(
        message: string,
        public status: number,
        public code: string,
        public requestId = "",
        public uncertain = false,
    ) {
        super(message);
    }
}
export function route(e: Endpoint, params: Values, query: Values): string {
    let path = e.path;
    for (const f of e.params) {
        const v = params[f.name];
        if (v === undefined || v === "") throw new Error("Informe " + f.name);
        path = path.replace("{" + f.name + "}", encodeURIComponent(String(v)));
    }
    const q = new URLSearchParams();
    for (const f of e.query)
        if (
            query[f.name] !== undefined &&
            query[f.name] !== null &&
            query[f.name] !== ""
        )
            q.set(f.name, String(query[f.name]));
    return path + (q.size ? "?" + q : "");
}
/** Prepared adapter. The application never instantiates this in the authorized fictitious mode.
 * A future approved identity provider must inject the short-lived Bearer in memory.
 */
export function realTransport(options: {
    baseUrl: string;
    allowReal: boolean;
    token: () => string | null;
    onExpired: () => void;
    fetcher?: typeof fetch;
    timeoutMs?: number;
}): Transport {
    return {
        async send(r) {
            if (!options.allowReal)
                throw new ApiError(
                    "Integração real desabilitada.",
                    0,
                    "API_DESABILITADA",
                );
            const token = options.token();
            if (!token)
                throw new ApiError(
                    "Sessão do provedor necessária.",
                    401,
                    "NAO_AUTENTICADO",
                );
            const base = new URL(options.baseUrl);
            if (
                base.protocol !== "https:" &&
                base.hostname !== "127.0.0.1" &&
                base.hostname !== "localhost"
            )
                throw new Error("Origem da API inválida.");
            const path = route(r.endpoint, r.params, r.query);
            const requestId = crypto.randomUUID();
            const headers: Record<string, string> = {
                Authorization: "Bearer " + token,
                "X-Request-Id": requestId,
                Accept: "application/json",
            };
            let body: BodyInit | undefined;
            if (r.endpoint.request) {
                const content = encode(r.endpoint.request, r.body);
                if (r.endpoint.multipart) {
                    if (!r.file) throw new Error("Selecione o arquivo Excel.");
                    const form = new FormData();
                    form.append("arquivo", r.file);
                    form.append(
                        "comando",
                        new Blob([content], { type: "application/json" }),
                    );
                    body = form;
                } else {
                    headers["Content-Type"] = "application/json";
                    body = content;
                }
            }
            const controller = new AbortController();
            const stop = () => controller.abort(r.signal.reason);
            r.signal.addEventListener("abort", stop, { once: true });
            if (r.signal.aborted) stop();
            const timeout = setTimeout(
                () => controller.abort(),
                options.timeoutMs ?? 20000,
            );
            try {
                const response = await (options.fetcher ?? fetch)(
                    new URL(path, base),
                    {
                        method: r.endpoint.method,
                        headers,
                        body,
                        signal: controller.signal,
                        credentials: "omit",
                        redirect: "error",
                        cache: "no-store",
                    },
                );
                const raw = await response.text();
                // Um conector pode concluir mesmo após abort. Não permitir que
                // essa resposta altere a sessão que substituiu seu contexto.
                if (r.signal.aborted || controller.signal.aborted)
                    throw new DOMException("Contexto encerrado.", "AbortError");
                const correlation =
                    response.headers.get("X-Request-Id") ?? requestId;
                if (response.status === 401 && options.token() === token)
                    options.onExpired();
                if (!response.ok) {
                    const uncertainWrite =
                        response.status >= 500 && r.endpoint.method !== "GET";
                    let detail: Values = {};
                    try {
                        const parsed = parseExact(raw);
                        if (isObject(parsed)) detail = parsed;
                    } catch {
                        /* Connector errors can be non-JSON. */
                    }
                    throw new ApiError(
                        uncertainWrite
                            ? "Resposta do servidor sem confirmação do resultado. Consulte o estado atual; a operação pode ter sido registrada." +
                                  (typeof detail.detail === "string"
                                      ? " " + detail.detail
                                      : "")
                            : typeof detail.detail === "string"
                              ? detail.detail
                              : "Solicitação recusada pelo servidor.",
                        response.status,
                        typeof detail.codigo === "string"
                            ? detail.codigo
                            : "ERRO_HTTP",
                        correlation,
                        uncertainWrite,
                    );
                }
                if (!raw)
                    throw new ApiError(
                        "Resposta sem confirmação. Consulte o estado atual antes de repetir.",
                        response.status,
                        "CONFIRMACAO_AUSENTE",
                        correlation,
                        r.endpoint.method !== "GET",
                    );
                let data: unknown;
                try {
                    data = parseResponse(r.endpoint.response, raw);
                } catch {
                    throw new ApiError(
                        "Resposta incompatível com o contrato. Consulte o estado atual antes de repetir.",
                        response.status,
                        "RESPOSTA_INCOMPATIVEL",
                        correlation,
                        r.endpoint.method !== "GET",
                    );
                }
                return {
                    data,
                    raw,
                    requestId: correlation,
                    ficticio: false,
                    replay: false,
                };
            } catch (error) {
                if (error instanceof ApiError) throw error;
                if (r.signal.aborted)
                    throw new DOMException("Contexto encerrado.", "AbortError");
                throw new ApiError(
                    "Falha de comunicação. Consulte o estado atual; a operação pode ter sido registrada.",
                    0,
                    "RESPOSTA_DESCONHECIDA",
                    requestId,
                    r.endpoint.method !== "GET",
                );
            } finally {
                clearTimeout(timeout);
                r.signal.removeEventListener("abort", stop);
            }
        },
    };
}
export async function call<K extends keyof ApiContracts>(
    transport: Transport,
    id: K,
    body: ApiContracts[K]["request"],
    params: Values,
    query: Values,
    signal: AbortSignal,
): Promise<ApiContracts[K]["response"]> {
    return (
        await transport.send({
            endpoint: endpoint(id),
            params,
            query,
            body,
            signal,
        })
    ).data as ApiContracts[K]["response"];
}
export function downloadReceipt(
    receipt: Receipt,
    name = "demonstrativo-ficticio.json",
): void {
    const url = URL.createObjectURL(
        new Blob([receipt.raw], { type: "application/json;charset=utf-8" }),
    );
    const a = document.createElement("a");
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
}
export const snapshotFingerprint = (r: Request): string =>
    r.endpoint.method +
    " " +
    route(r.endpoint, r.params, r.query) +
    " " +
    (r.endpoint.request ? encode(r.endpoint.request, r.body) : "") +
    " " +
    (r.file?.name ?? "");
