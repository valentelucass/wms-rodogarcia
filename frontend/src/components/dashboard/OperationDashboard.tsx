import { useEffect, useRef, useState } from "react";
import { ApiError, call, type Transport } from "../../api/client";
import type { DashboardDto_Resumo } from "../../contracts/types";
import type { Perfil, Values } from "../../contracts/runtime";
import type { Navigate } from "../../hooks/useNavigation";
import { Icon } from "../../design-system/Icon";
import { Pagination } from "../layout/Pagination";

const areaNames: Record<string, string> = {
    ARMAZENAGEM: "Armazenagem",
    TRIAGEM: "Triagem",
    QUARENTENA: "Quarentena",
    SEPARACAO: "Separação",
};
const queueNames: Record<string, string> = {
    RASCUNHO: "Em elaboração",
    RESERVADO: "Reservados",
    EM_SEPARACAO: "Em separação",
    SEPARADO: "Aguardando retirada",
};
const segments = [
    ["disponivel", "Disponível"],
    ["reservado", "Reservado"],
    ["indisponivel", "Demais indisponíveis"],
    ["pendenteUnitizacao", "Sem unitização"],
] as const;

export function formatQuantity(value: string): string {
    const [whole, fraction] = value.split(".");
    const decimals = (fraction ?? "").replace(/0+$/, "");
    return (
        whole.replace(/\B(?=(\d{3})+(?!\d))/g, ".") +
        (decimals ? "," + decimals : "")
    );
}

/** Só geometria: preserva os lexemas exatos nos rótulos, inclusive Long acima de 2^53. */
export function barShare(value: string, maximum: string): number {
    const scale = Math.max(
        value.split(".")[1]?.length ?? 0,
        maximum.split(".")[1]?.length ?? 0,
    );
    const integer = (v: string) => {
        const [whole, fraction = ""] = v.split(".");
        return BigInt(whole + fraction.padEnd(scale, "0"));
    };
    const total = integer(maximum),
        part = integer(value);
    if (total <= 0n || part <= 0n) return 0;
    return (
        Number(
            (part * 100000n) / total > 100000n
                ? 100000n
                : (part * 100000n) / total,
        ) / 100
    );
}

function SingleBar({
    value,
    maximum,
    series = "blue",
}: {
    value: string;
    maximum: string;
    series?: string;
}) {
    return (
        <svg
            className="dashboard-bar"
            viewBox="0 0 1000 16"
            preserveAspectRatio="none"
            aria-hidden="true"
        >
            <rect className="dashboard-track" width="1000" height="16" rx="3" />
            <rect
                className={`dashboard-series dashboard-series--${series}`}
                width={barShare(value, maximum)}
                height="16"
                rx="3"
            />
        </svg>
    );
}

export function OperationDashboard({
    transport,
    context,
    perfil,
    navigate,
    ficticio = false,
}: {
    transport: Transport;
    context: Values;
    perfil: Perfil;
    navigate: Navigate;
    ficticio?: boolean;
}) {
    const clienteId = String(context.clienteId ?? ""),
        armazemId = String(context.armazemId ?? "");
    const valid = (v: string) =>
        /^[1-9]\d*$/.test(v) && BigInt(v) <= 9223372036854775807n;
    const configured = valid(clienteId) && valid(armazemId);
    const [pagina, setPagina] = useState(0),
        [revision, setRevision] = useState(0);
    const productsHeading = useRef<HTMLHeadingElement>(null);
    const focusProducts = useRef(false);
    const scope = `${perfil}/${clienteId}/${armazemId}`;
    const key = `${scope}/${pagina}/${revision}`;
    const [result, setResult] = useState<{
        key: string;
        scope?: string;
        data?: DashboardDto_Resumo;
        error?: string;
    }>({ key: "" });
    useEffect(() => {
        if (!configured) return;
        const controller = new AbortController();
        let current = true;
        void call(
            transport,
            "DashboardController.consultar",
            undefined,
            {},
            {
                clienteId,
                armazemId,
                pagina,
                tamanho: 6,
                fuso: Intl.DateTimeFormat().resolvedOptions().timeZone,
            },
            controller.signal,
        )
            .then((data) => {
                if (current) setResult({ key, scope, data });
            })
            .catch((error: unknown) => {
                if (current)
                    setResult({
                        key,
                        scope,
                        error:
                            error instanceof ApiError && error.status === 403
                                ? "Seu acesso não permite consultar este contexto."
                                : "Não foi possível consultar os indicadores. Tente atualizar.",
                    });
            });
        return () => {
            current = false;
            controller.abort();
        };
    }, [transport, configured, clienteId, armazemId, pagina, key, scope]);
    const data = result.scope === scope ? result.data : undefined;
    const error = result.key === key ? result.error : undefined;
    const loading = configured && result.key !== key;
    useEffect(() => {
        if (!loading && data && focusProducts.current) {
            productsHeading.current?.focus();
            focusProducts.current = false;
        }
    }, [loading, data]);
    const areas = data?.areas ?? [],
        fila = data?.fila ?? [];
    const maximum = (values: string[]) =>
        values.reduce((a, b) => (BigInt(a) > BigInt(b) ? a : b), "0");
    const areaMax = maximum(areas.map((a) => a.posicoesCliente));
    const queueMax = maximum(fila.map((f) => f.pedidos));
    const products = data?.produtos.itens ?? [];
    return (
        <section
            className="operation-dashboard"
            aria-labelledby="dashboard-title"
            aria-busy={loading}
        >
            <header className="dashboard-heading">
                <div>
                    <span className="dashboard-eyebrow">
                        {ficticio
                            ? "Exercício fictício"
                            : "Cliente e armazém selecionados"}
                    </span>
                    <h2 id="dashboard-title">Visão da operação</h2>
                    <p>
                        {data
                            ? `Cliente ${data.clienteId} · Armazém ${data.armazemId} · Atualizado às ${new Date(data.consultadoEm).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit", timeZone: data.fuso })}`
                            : "Estoque, espaço e pedidos no contexto atual."}
                    </p>
                </div>
                <button
                    type="button"
                    className="dashboard-refresh"
                    disabled={!configured || loading}
                    onClick={() => setRevision((r) => r + 1)}
                >
                    <Icon name="relatorios" />
                    {loading ? "Consultando…" : "Atualizar indicadores"}
                </button>
            </header>
            {!configured ? (
                <div className="dashboard-notice">
                    <Icon name="estoque" />
                    <p>
                        Selecione cliente e armazém no topo para consultar os
                        indicadores da operação.
                    </p>
                </div>
            ) : error ? (
                <div
                    className="dashboard-notice dashboard-notice--error"
                    role="alert"
                >
                    <Icon name="contingencia" />
                    <p>{error}</p>
                </div>
            ) : loading && !data ? (
                <div className="dashboard-loading" role="status">
                    <p>Consultando os indicadores do contexto…</p>
                    <div className="dashboard-skeleton-grid">
                        {[1, 2, 3, 4].map((i) => (
                            <span key={i} className="dashboard-skeleton" />
                        ))}
                    </div>
                </div>
            ) : (
                data && (
                    <>
                        {loading && (
                            <p className="dashboard-footnote" role="status">
                                Exibindo a consulta anterior enquanto os dados
                                deste contexto são atualizados…
                            </p>
                        )}
                        <div className="dashboard-metrics">
                            {[
                                [
                                    "estoque",
                                    "Posições ocupadas",
                                    formatQuantity(data.posicoesCliente),
                                    "Ocupação física do cliente",
                                ],
                                [
                                    "unidades",
                                    "Unidades disponíveis",
                                    formatQuantity(data.unidadesDisponiveis),
                                    "Pallets e bobinas elegíveis à saída",
                                ],
                                [
                                    "saida",
                                    "Pedidos de saída abertos",
                                    formatQuantity(data.pedidosAbertos),
                                    "Da elaboração até a retirada",
                                ],
                                [
                                    "contingencia",
                                    "Avisos de validade",
                                    data.unidadesComAviso === null
                                        ? "—"
                                        : formatQuantity(data.unidadesComAviso),
                                    data.antecedenciaValidade === null
                                        ? "Antecedência ainda não configurada"
                                        : `Vencidas ou a vencer em até ${data.antecedenciaValidade} dias`,
                                ],
                            ].map(([icon, label, value, description]) => (
                                <article
                                    className="dashboard-metric"
                                    key={label}
                                >
                                    <span className="dashboard-metric-icon">
                                        <Icon name={icon} />
                                    </span>
                                    <span>{label}</span>
                                    <strong>{value}</strong>
                                    <small>{description}</small>
                                </article>
                            ))}
                        </div>
                        <div className="dashboard-charts">
                            <article
                                className="dashboard-chart"
                                aria-labelledby="dashboard-occupation-title"
                            >
                                <header>
                                    <span className="dashboard-chart-icon">
                                        <Icon name="estoque" />
                                    </span>
                                    <div>
                                        <h3 id="dashboard-occupation-title">
                                            Ocupação física
                                        </h3>
                                        <p>
                                            Posições ocupadas pelo cliente, por
                                            área.
                                        </p>
                                    </div>
                                </header>
                                <div className="dashboard-rows">
                                    {areas.map((a) => (
                                        <div
                                            className="dashboard-row"
                                            key={a.tipo}
                                        >
                                            <div className="dashboard-row-label">
                                                <span>{areaNames[a.tipo]}</span>
                                                <strong>
                                                    {formatQuantity(
                                                        a.posicoesCliente,
                                                    )}{" "}
                                                    <small>posições</small>
                                                </strong>
                                            </div>
                                            <SingleBar
                                                value={a.posicoesCliente}
                                                maximum={areaMax}
                                            />
                                            {data.visaoArmazem && (
                                                <small>
                                                    {formatQuantity(
                                                        a.livresArmazem ?? "0",
                                                    )}{" "}
                                                    livres de{" "}
                                                    {formatQuantity(
                                                        a.capacidadeAtiva ??
                                                            "0",
                                                    )}{" "}
                                                    posições ativas no armazém
                                                </small>
                                            )}
                                        </div>
                                    ))}
                                </div>
                                <p className="dashboard-scale">
                                    Escala de 0 a {formatQuantity(areaMax)}{" "}
                                    posições do cliente.
                                </p>
                                <footer>
                                    <span>
                                        {data.posicoesCliente === "0"
                                            ? "Nenhuma posição ocupada neste contexto."
                                            : "Uma unidade pode ocupar duas posições."}
                                    </span>
                                    <button
                                        type="button"
                                        onClick={() => navigate("estoque")}
                                    >
                                        Consultar estoque <Icon name="arrow" />
                                    </button>
                                </footer>
                            </article>
                            <article
                                className="dashboard-chart"
                                aria-labelledby="dashboard-queue-title"
                            >
                                <header>
                                    <span className="dashboard-chart-icon">
                                        <Icon name="saida" />
                                    </span>
                                    <div>
                                        <h3 id="dashboard-queue-title">
                                            Fila de saída
                                        </h3>
                                        <p>
                                            Pedidos que ainda aguardam
                                            conclusão.
                                        </p>
                                    </div>
                                </header>
                                <div className="dashboard-rows">
                                    {fila.map((f) => (
                                        <div
                                            className="dashboard-row"
                                            key={f.situacao}
                                        >
                                            <div className="dashboard-row-label">
                                                <span>
                                                    {queueNames[f.situacao]}
                                                </span>
                                                <strong>
                                                    {formatQuantity(f.pedidos)}{" "}
                                                    <small>pedidos</small>
                                                </strong>
                                            </div>
                                            <SingleBar
                                                value={f.pedidos}
                                                maximum={queueMax}
                                                series={
                                                    f.situacao === "SEPARADO"
                                                        ? "orange"
                                                        : "blue"
                                                }
                                            />
                                        </div>
                                    ))}
                                </div>
                                <p className="dashboard-scale">
                                    Escala de 0 a {formatQuantity(queueMax)}{" "}
                                    pedidos.
                                </p>
                                <footer>
                                    <span>
                                        {data.pedidosAbertos === "0"
                                            ? "Nenhum pedido de saída aberto."
                                            : "Reserva permanece até retirada ou cancelamento."}
                                    </span>
                                    <button
                                        type="button"
                                        onClick={() => navigate("saida")}
                                    >
                                        Abrir saídas <Icon name="arrow" />
                                    </button>
                                </footer>
                            </article>
                            <article
                                className="dashboard-chart dashboard-chart--products"
                                aria-labelledby="dashboard-products-title"
                            >
                                <header>
                                    <span className="dashboard-chart-icon">
                                        <Icon name="unidades" />
                                    </span>
                                    <div>
                                        <h3
                                            id="dashboard-products-title"
                                            ref={productsHeading}
                                            tabIndex={-1}
                                        >
                                            Disponibilidade por produto
                                        </h3>
                                        <p>
                                            Composição do saldo físico de cada
                                            SKU, na sua unidade de medida.
                                        </p>
                                    </div>
                                </header>
                                <div className="dashboard-legend">
                                    {segments.map(([field, label]) => (
                                        <span key={field}>
                                            <i
                                                className={`dashboard-swatch dashboard-series--${field}`}
                                            />
                                            {label}
                                        </span>
                                    ))}
                                </div>
                                <div className="dashboard-product-rows">
                                    {products.map((p) => {
                                        let offset = 0;
                                        return (
                                            <div
                                                className="dashboard-product"
                                                key={p.produtoId}
                                            >
                                                <div className="dashboard-row-label">
                                                    <strong>{p.sku}</strong>
                                                    <span>
                                                        {formatQuantity(
                                                            p.fisicoTotal,
                                                        )}{" "}
                                                        <small>
                                                            {p.unidadeMedida}
                                                        </small>
                                                    </span>
                                                </div>
                                                <svg
                                                    className="dashboard-bar"
                                                    viewBox="0 0 1000 16"
                                                    preserveAspectRatio="none"
                                                    aria-hidden="true"
                                                >
                                                    <rect
                                                        className="dashboard-track"
                                                        width="1000"
                                                        height="16"
                                                        rx="3"
                                                    />
                                                    {segments.map(([field]) => {
                                                        const width = barShare(
                                                                p[field],
                                                                p.fisicoTotal,
                                                            ),
                                                            x = offset;
                                                        offset += width;
                                                        return (
                                                            <rect
                                                                key={field}
                                                                className={`dashboard-series dashboard-series--${field}`}
                                                                x={x}
                                                                width={width}
                                                                height="16"
                                                            />
                                                        );
                                                    })}
                                                </svg>
                                                <div className="dashboard-product-values">
                                                    {segments.map(
                                                        ([field, label]) => (
                                                            <span key={field}>
                                                                {label}:{" "}
                                                                <strong>
                                                                    {formatQuantity(
                                                                        p[
                                                                            field
                                                                        ],
                                                                    )}{" "}
                                                                    {
                                                                        p.unidadeMedida
                                                                    }
                                                                </strong>
                                                            </span>
                                                        ),
                                                    )}
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                                {!products.length && (
                                    <p className="dashboard-empty">
                                        Nenhum produto nesta página do contexto.
                                    </p>
                                )}
                                <footer>
                                    <Pagination
                                        page={data.produtos.pagina}
                                        pages={data.produtos.totalPaginas}
                                        total={formatQuantity(
                                            data.produtos.totalItens,
                                        )}
                                        count={products.length}
                                        size={6}
                                        disabled={loading}
                                        onPage={(page) => {
                                            focusProducts.current = true;
                                            setPagina(page);
                                        }}
                                    />
                                </footer>
                            </article>
                        </div>
                        <p className="dashboard-footnote">
                            Posições, unidades logísticas e quantidades por SKU
                            são medidas distintas. Quarentena, avaria e
                            bloqueios não liberam disponibilidade.
                        </p>
                    </>
                )
            )}
        </section>
    );
}
