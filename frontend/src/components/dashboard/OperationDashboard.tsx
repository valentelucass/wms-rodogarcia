import { useEffect, useRef, useState } from "react";
import { ApiError, call, type Transport } from "../../api/client";
import type { DashboardDto_Resumo } from "../../contracts/types";
import type { Perfil, Values } from "../../contracts/runtime";
import type { Navigate } from "../../hooks/useNavigation";
import { Icon } from "../../design-system/Icon";
import { metricInformation } from "../../content/information";
import {
    InformationCard,
    InformationHint,
} from "../information/InformationPopup";
import { Pagination } from "../layout/Pagination";
import { useReferenceCatalog } from "../context/ReferenceCatalog";
import { OperationLineChart } from "./OperationLineChart";
import { formatQuantity } from "./chartFormatting";
import { ProductAvailability } from "./ProductAvailability";
export { barShare, formatQuantity } from "./chartFormatting";

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
const queueShortNames: Record<string, string> = {
    RASCUNHO: "Elaboração",
    RESERVADO: "Reservados",
    EM_SEPARACAO: "Separação",
    SEPARADO: "Retirada",
};

export function OperationDashboard({
    transport,
    context,
    perfil,
    navigate,
    ficticio = false,
    showMetrics = true,
}: {
    transport: Transport;
    context: Values;
    perfil: Perfil;
    navigate: Navigate;
    ficticio?: boolean;
    showMetrics?: boolean;
}) {
    const catalog = useReferenceCatalog();
    const clienteId = String(context.clienteId ?? ""),
        armazemId = String(context.armazemId ?? "");
    const client = catalog?.clientes.find((c) => c.id === clienteId)?.nome;
    const warehouse = catalog?.armazens.find((a) => a.id === armazemId)?.nome;
    const valid = (v: string) =>
        /^[1-9]\d*$/.test(v) && BigInt(v) <= 9223372036854775807n;
    const configured =
        (!clienteId || valid(clienteId)) && (!armazemId || valid(armazemId));
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
                clienteId: clienteId || undefined,
                armazemId: armazemId || undefined,
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
                            : "Contexto da operação"}
                    </span>
                    <h2 id="dashboard-title">
                        {showMetrics
                            ? "Visão da operação"
                            : "Gráficos da operação"}
                    </h2>
                    <p>
                        {data
                            ? `${clienteId ? (client ?? `Cliente ${data.clienteId}`) : "Todos os clientes permitidos"} · ${armazemId ? (warehouse ?? `Armazém ${data.armazemId}`) : "Todos os armazéns permitidos"} · Atualizado às ${new Date(data.consultadoEm).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit", timeZone: data.fuso })}`
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
                    {loading
                        ? "Consultando…"
                        : showMetrics
                          ? "Atualizar indicadores"
                          : "Atualizar gráficos"}
                </button>
            </header>
            {!configured ? (
                <div className="dashboard-notice">
                    <Icon name="estoque" />
                    <p>
                        Confira a seleção de cliente e armazém no topo para
                        consultar os
                        {showMetrics ? "indicadores" : "gráficos"} da operação.
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
                        {showMetrics && (
                            <div className="dashboard-metrics">
                                {(
                                    [
                                        [
                                            "estoque",
                                            "Posições ocupadas",
                                            formatQuantity(
                                                data.posicoesCliente,
                                            ),
                                            "Ocupação física no contexto",
                                            "occupied",
                                        ],
                                        [
                                            "unidades",
                                            "Unidades disponíveis",
                                            formatQuantity(
                                                data.unidadesDisponiveis,
                                            ),
                                            "Pallets e bobinas elegíveis à saída",
                                            "availableUnits",
                                        ],
                                        [
                                            "saida",
                                            "Pedidos de saída abertos",
                                            formatQuantity(data.pedidosAbertos),
                                            "Da elaboração até a retirada",
                                            "outbound",
                                        ],
                                        [
                                            "contingencia",
                                            "Avisos de validade",
                                            data.unidadesComAviso === null
                                                ? "—"
                                                : formatQuantity(
                                                      data.unidadesComAviso,
                                                  ),
                                            data.antecedenciaValidade === null
                                                ? "Antecedência ainda não configurada"
                                                : `Vencidas ou a vencer em até ${data.antecedenciaValidade} dias`,
                                            "expiry",
                                        ],
                                    ] as const
                                ).map(
                                    ([
                                        icon,
                                        label,
                                        value,
                                        description,
                                        topic,
                                    ]) => (
                                        <InformationCard
                                            content={metricInformation(
                                                topic,
                                                value,
                                                description,
                                            )}
                                            className="dashboard-metric"
                                            key={label}
                                        >
                                            <span className="dashboard-metric-icon">
                                                <Icon name={icon} />
                                            </span>
                                            <span>{label}</span>
                                            <strong>{value}</strong>
                                            <small>{description}</small>
                                        </InformationCard>
                                    ),
                                )}
                            </div>
                        )}
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
                                        <div className="information-heading">
                                            <h3 id="dashboard-occupation-title">
                                                Ocupação física
                                            </h3>
                                            <InformationHint topic="physicalChart" />
                                        </div>
                                        <p>
                                            Posições ocupadas no contexto, por
                                            área.
                                        </p>
                                    </div>
                                </header>
                                <OperationLineChart
                                    variant="physical"
                                    total={data.posicoesCliente}
                                    unit="posições"
                                    points={areas.map((a) => ({
                                        id: a.tipo,
                                        label: areaNames[a.tipo] ?? a.tipo,
                                        shortLabel:
                                            {
                                                ARMAZENAGEM: "Armaz.",
                                                TRIAGEM: "Triagem",
                                                QUARENTENA: "Quarent.",
                                                SEPARACAO: "Separação",
                                            }[a.tipo] ?? a.tipo,
                                        value: a.posicoesCliente,
                                        note:
                                            data.visaoArmazem &&
                                            a.capacidadeAtiva != null &&
                                            a.livresArmazem != null
                                                ? `${formatQuantity(a.livresArmazem)} livres de ${formatQuantity(a.capacidadeAtiva)} posições ativas no armazém`
                                                : `${formatQuantity(a.posicoesCliente)} posições ocupadas no contexto.`,
                                    }))}
                                />
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
                                        <div className="information-heading">
                                            <h3 id="dashboard-queue-title">
                                                Fila de saída
                                            </h3>
                                            <InformationHint topic="queueChart" />
                                        </div>
                                        <p>
                                            Pedidos que ainda aguardam
                                            conclusão.
                                        </p>
                                    </div>
                                </header>
                                <OperationLineChart
                                    variant="queue"
                                    total={data.pedidosAbertos}
                                    unit="pedidos"
                                    points={fila.map((f) => ({
                                        id: f.situacao,
                                        label:
                                            queueNames[f.situacao] ??
                                            f.situacao,
                                        shortLabel:
                                            queueShortNames[f.situacao] ??
                                            f.situacao,
                                        value: f.pedidos,
                                    }))}
                                />
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
                                        <div className="information-heading">
                                            <h3
                                                id="dashboard-products-title"
                                                ref={productsHeading}
                                                tabIndex={-1}
                                            >
                                                Disponibilidade por produto
                                            </h3>
                                            <InformationHint topic="productChart" />
                                        </div>
                                        <p>
                                            Selecione um SKU para conferir a
                                            composição do saldo.
                                        </p>
                                    </div>
                                </header>
                                <ProductAvailability
                                    key={`${scope}/${data.produtos.pagina}`}
                                    products={products}
                                    clientName={(id) =>
                                        clienteId
                                            ? undefined
                                            : (catalog?.clientes.find(
                                                  (client) => client.id === id,
                                              )?.nome ?? `Cliente ${id}`)
                                    }
                                />
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
