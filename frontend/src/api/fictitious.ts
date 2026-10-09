import { ExampleDispatch } from "../modules/saida/ExampleDispatch";
import { ExampleRegistry } from "../modules/cadastros/ExampleRegistry";
import { ExampleCount } from "../modules/regularizacao/ExampleCount";
import { ExampleInitialLoad } from "../modules/regularizacao/ExampleInitialLoad";
import { ExampleContingency } from "../modules/regularizacao/ExampleContingency";
import { fixture } from "./mock/fixtures";
import { reflect } from "./mock/scriptedValues";
import { ExampleWarehouse } from "../modules/recebimento/ExampleWarehouse";
import { ExampleBilling } from "../modules/financeiro/ExampleBilling";
import { ExampleServiceConfiguration } from "../modules/financeiro/ExampleServiceConfiguration";
import { ExampleDamage } from "../modules/estoque/ExampleDamage";
import { exampleAudit } from "../modules/relatorios/exampleAudit";
/** Deliberately separate example responder. It demonstrates UI states, not warehouse rules. */
import { isObject, canPresent, type Perfil } from "../contracts/runtime";
import {
    toWire,
    parseResponse,
    stringifyExact,
    encode,
} from "../contracts/codec";
import {
    ApiError,
    snapshotFingerprint,
    type Receipt,
    type Request,
    type Transport,
} from "./client";
export type Scenario =
    | "sucesso"
    | "vazio"
    | "conflito"
    | "falha"
    | "perdida"
    | "expirada"
    | "negada"
    | "lento";
export class FictitiousTransport implements Transport {
    scenario: Scenario = "sucesso";
    requests: Request[] = [];
    private cache = new Map<
        string,
        { fingerprint: string; receipt: Receipt }
    >();
    private warehouse = new ExampleWarehouse();
    private billing = new ExampleBilling();
    private serviceConfiguration = new ExampleServiceConfiguration();
    private damage = new ExampleDamage();
    private dispatch = new ExampleDispatch();
    private registry = new ExampleRegistry();
    private count = new ExampleCount();
    private initialLoad = new ExampleInitialLoad();
    private contingency = new ExampleContingency();
    constructor(public perfil: Perfil = "GESTOR") {}
    async send(r: Request): Promise<Receipt> {
        const fingerprint = snapshotFingerprint(r);
        this.requests.push(r);
        // Only this fictitious responder emits these events. They permit an isolated
        // browser exercise to record exactly which commands were sent, without HTTP.
        if (typeof window !== "undefined")
            window.dispatchEvent(
                new CustomEvent("wms:fictitious-request", {
                    detail: {
                        id: r.endpoint.id,
                        params: structuredClone(r.params),
                        query: structuredClone(r.query),
                        wire: r.endpoint.request
                            ? encode(r.endpoint.request, r.body)
                            : null,
                        file: r.file
                            ? {
                                  name: r.file.name,
                                  size: r.file.size,
                                  type: r.file.type,
                              }
                            : null,
                    },
                }),
            );
        await new Promise<void>((resolve, reject) => {
            const timer = setTimeout(
                () => {
                    r.signal.removeEventListener("abort", stop);
                    resolve();
                },
                this.scenario === "lento" ? 1500 : 140,
            );
            const stop = () => {
                clearTimeout(timer);
                reject(new DOMException("Contexto encerrado.", "AbortError"));
            };
            if (r.signal.aborted) stop();
            else r.signal.addEventListener("abort", stop, { once: true });
        });
        if (
            !canPresent(this.perfil, r.endpoint.permission) ||
            this.scenario === "negada"
        )
            throw new ApiError(
                "Exercício: ação recusada ao perfil.",
                403,
                "ACESSO_NEGADO",
                "demo-403",
            );
        if (this.scenario === "expirada")
            throw new ApiError(
                "Exercício: sessão expirada.",
                401,
                "NAO_AUTENTICADO",
                "demo-401",
            );
        if (this.scenario === "conflito")
            throw new ApiError(
                "Exercício: revisão mudou ou disponibilidade foi ocupada. Consulte o estado atual.",
                409,
                "CONFLITO_CONCORRENTE",
                "demo-409",
            );
        if (this.scenario === "falha")
            throw new ApiError(
                "Exercício: serviço indisponível. Dados anteriores não são atuais.",
                503,
                "INDISPONIVEL",
                "demo-503",
                r.endpoint.method !== "GET",
            );
        const body = isObject(r.body) ? r.body : {};
        const key =
            typeof body.operacaoId === "string" ? body.operacaoId : undefined;
        if (key && this.cache.has(key)) {
            const previous = this.cache.get(key)!;
            if (previous.fingerprint !== fingerprint)
                throw new ApiError(
                    "Exercício: mesma operação com conteúdo diferente.",
                    409,
                    "OPERACAO_DIVERGENTE",
                );
            return { ...previous.receipt, replay: true };
        }
        let data = reflect(
            fixture(r.endpoint.response),
            { ...r.query, ...body },
            r.endpoint.id,
        );
        data = this.warehouse.respond(r, body, data);
        data = this.damage.respond(r, body, data);
        data = this.registry.respond(r, body, data);
        data = this.count.respond(r, body, data);
        data = this.initialLoad.respond(r, body, data);
        data = this.contingency.respond(r, body, data);
        data = this.dispatch.respond(
            r,
            body,
            data,
            this.warehouse.physicalReceipt,
        );
        data = this.billing.respond(r, body, data);
        data = this.serviceConfiguration.respond(r, body, data);
        data = exampleAudit(r, data);
        if (this.scenario === "vazio" && r.endpoint.method === "GET") {
            if (Array.isArray(data)) data = [];
            else if (isObject(data) && "itens" in data)
                data = { ...data, itens: [], totalItens: "0", totalPaginas: 0 };
        }
        // Pre-authored physical receipt: the demo does not infer withdrawal from fiscal registration.
        if (
            r.endpoint.id === "ExpedicaoController.retirar" &&
            isObject(data) &&
            isObject(data.expedicao)
        )
            data.expedicao.retiradaEm = "2026-10-08T12:30:00.123456Z";
        const raw = stringifyExact(toWire(r.endpoint.response, data));
        const receipt: Receipt = {
            data: parseResponse(r.endpoint.response, raw),
            raw,
            requestId: "ficticio-" + crypto.randomUUID(),
            ficticio: true,
            replay: false,
        };
        if (typeof window !== "undefined")
            window.dispatchEvent(
                new CustomEvent("wms:fictitious-receipt", {
                    detail: {
                        id: r.endpoint.id,
                        raw,
                        requestId: receipt.requestId,
                    },
                }),
            );
        if (key) this.cache.set(key, { fingerprint, receipt });
        if (this.scenario === "perdida" && r.endpoint.method !== "GET") {
            this.scenario = "sucesso";
            throw new ApiError(
                "Exercício: resposta perdida depois do registro fictício. Consulte ou repita o mesmo conteúdo com a mesma operação.",
                0,
                "RESPOSTA_DESCONHECIDA",
                "demo-perdida",
                true,
            );
        }
        return receipt;
    }
}
