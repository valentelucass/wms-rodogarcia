import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import { ApiError, call, type Transport } from "../../api/client";
import type { Values } from "../../contracts/runtime";
import type {
    VisaoOperacaoDto_Resumo,
    VisaoOperacaoDto_Posicao,
    VisaoOperacaoDto_Detalhe,
} from "../../contracts/types";
import { useReferenceCatalog } from "../context/ReferenceCatalog";
import { ReferenceSelect } from "../context/ReferenceSelect";
import { Dialog } from "../layout/Dialog";
import { Pagination } from "../layout/Pagination";
import { Icon } from "../../design-system/Icon";
import { formatQuantity } from "./OperationDashboard";
import { expandedDecimal } from "../../contracts/codec";

export const positionState = (p: VisaoOperacaoDto_Posicao) =>
    p.ocupada
        ? "Ocupado"
        : p.disponivel
          ? "Disponível"
          : ({
                INATIVO: "Inativo",
                QUARENTENA: "Quarentena",
                TRIAGEM: "Triagem",
                SEPARACAO: "Separação",
            }[p.estado] ?? "Indisponível");
const quantity = (v: string | null | undefined) =>
    v == null ? "—" : formatQuantity(v);
export function overviewMoney(v: string | null | undefined): string {
    if (v == null) return "—";
    const { negative, whole, fraction } = expandedDecimal(v);
    const cents =
        BigInt(whole) * 100n +
        BigInt(fraction.padEnd(2, "0").slice(0, 2)) +
        (Number(fraction[2] ?? "0") >= 5 ? 1n : 0n);
    return `R$ ${negative && cents > 0n ? "-" : ""}${formatQuantity(String(cents / 100n))},${String(cents % 100n).padStart(2, "0")}`;
}
const money = overviewMoney;
type Props = {
    transport: Transport;
    context: Values;
    onScope: (v: Values) => void;
    onAddress: (p: VisaoOperacaoDto_Posicao) => void;
    ficticio?: boolean;
    children?: ReactNode;
};

export function WarehouseOverview(props: Props) {
    return (
        <Overview
            key={`${props.context.clienteId ?? ""}/${props.context.armazemId ?? ""}`}
            {...props}
        />
    );
}
function Overview({
    transport,
    context,
    onScope,
    onAddress,
    ficticio = false,
    children,
}: Props) {
    const catalog = useReferenceCatalog();
    const [revision, setRevision] = useState(0),
        [page, setPage] = useState(0),
        [filter, setFilter] = useState("TODAS"),
        [search, setSearch] = useState(""),
        [code, setCode] = useState("");
    const [selected, setSelected] = useState<VisaoOperacaoDto_Posicao>();
    const [filtersOpen, setFiltersOpen] = useState(false);
    const filtersId = useId();
    const key = `${revision}/${page}/${filter}/${code}`;
    const [result, setResult] = useState<{
        key: string;
        data?: VisaoOperacaoDto_Resumo;
        error?: string;
    }>({ key: "" });
    const loading = result.key !== key;
    const data = result.data;
    useEffect(() => {
        const abort = new AbortController();
        let active = true;
        void call(
            transport,
            "VisaoOperacaoController.consultar",
            undefined,
            {},
            {
                clienteId: context.clienteId,
                armazemId: context.armazemId,
                fuso: Intl.DateTimeFormat().resolvedOptions().timeZone,
                codigo: code,
                estado: filter,
                pagina: page,
                tamanho: 100,
            },
            abort.signal,
        )
            .then((data) => {
                if (active) setResult({ key, data });
            })
            .catch((error: unknown) => {
                if (active)
                    setResult({
                        key,
                        error:
                            error instanceof ApiError && error.status === 403
                                ? "Seu acesso não permite consultar este contexto."
                                : "Não foi possível consultar a visão da operação. Tente atualizar.",
                    });
            });
        return () => {
            active = false;
            abort.abort();
        };
    }, [
        transport,
        context.clienteId,
        context.armazemId,
        code,
        filter,
        page,
        key,
    ]);
    const warehouse = catalog?.armazens.find(
        (p) => p.id === String(context.armazemId),
    )?.nome;
    const client = catalog?.clientes.find(
        (p) => p.id === String(context.clienteId),
    )?.nome;
    const measures: [string, string, string, string][] = [
        [
            "Ocupação",
            data?.ocupacao == null ? "—" : `${quantity(data.ocupacao)}%`,
            `Sobre ${quantity(data?.capacidade)} posições de armazenagem ativas`,
            "estoque",
        ],
        [
            "Posições ocupadas",
            quantity(data?.posicoesOcupadas),
            "Posições físicas de armazenagem",
            "armazens",
        ],
        [
            "Posições livres",
            quantity(data?.posicoesLivres),
            "Armazenagem ativa e desocupada",
            "cadastros",
        ],
        [
            "Unidades armazenadas",
            quantity(data?.unidadesArmazenadas),
            "Unidades logísticas ativas",
            "unidades",
        ],
        [
            "Valor armazenado",
            money(data?.valorArmazenado),
            data && !data.financeiroPermitido
                ? "Disponível para Supervisor e Gestor"
                : data && !data.valorCompleto
                  ? "Valoração pendente de informações"
                  : "Valoração atual do estoque confirmado",
            "servicos",
        ],
        [
            "Em quarentena",
            quantity(data?.emQuarentena),
            "Unidades na área de quarentena",
            "contingencia",
        ],
        [
            "Reservas ativas",
            quantity(data?.reservasAtivas),
            "Reservas ainda não encerradas",
            "saida",
        ],
        [
            "Entradas abertas",
            quantity(data?.entradasAbertas),
            "Pedidos ainda não efetivados",
            "entrada",
        ],
        [
            "Saídas abertas",
            quantity(data?.saidasAbertas),
            "Pedidos aguardando conclusão",
            "saida",
        ],
        [
            "Faturamento do mês",
            money(data?.faturamentoMes),
            data && !data.financeiroPermitido
                ? "Disponível para Supervisor e Gestor"
                : `NFS-e registradas · ${data?.competencia ?? "mês atual"} · parcial`,
            "fechamento",
        ],
    ];
    return (
        <section
            className="warehouse-overview"
            aria-label="Visão geral da operação"
            aria-busy={loading}
        >
            <header className="overview-heading">
                <div>
                    <h2>
                        {warehouse ??
                            (context.armazemId
                                ? "Armazém selecionado"
                                : "Todos os armazéns")}
                    </h2>
                    <p>
                        {ficticio ? "Exercício fictício · " : ""}
                        {client ??
                            (context.clienteId
                                ? "Cliente selecionado"
                                : "Todos os clientes permitidos")}
                        {data &&
                            ` · Atualizado às ${new Date(data.consultadoEm).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit", timeZone: data.fuso })}`}
                    </p>
                </div>
                <button
                    type="button"
                    onClick={() => {
                        setRevision((r) => r + 1);
                        catalog?.refresh();
                    }}
                    disabled={loading}
                >
                    <Icon name="relatorios" />
                    Atualizar visão
                </button>
            </header>
            {loading && (
                <p className="muted" role="status">
                    Consultando a operação…
                </p>
            )}
            {result.error && (
                <p className="error" role="alert">
                    {result.error}
                </p>
            )}
            <div className="overview-metrics">
                {measures.map(([label, value, note, icon], i) => (
                    <article
                        key={label}
                        className={`overview-metric overview-metric--${i}`}
                    >
                        <div>
                            <span>{label}</span>
                            <Icon name={icon} />
                        </div>
                        <strong>{value}</strong>
                        <small>{note}</small>
                    </article>
                ))}
            </div>
            {children}
            <section
                className="warehouse-map"
                aria-labelledby="warehouse-map-title"
            >
                <header className="overview-map-heading">
                    <h2 id="warehouse-map-title">Mapa do armazém</h2>
                </header>
                <div className="map-controls">
                    <ReferenceSelect
                        kind="armazens"
                        title="Visualizar armazém"
                        all
                        compact
                        value={String(context.armazemId ?? "")}
                        onChange={(armazemId) =>
                            onScope({
                                clienteId: context.clienteId,
                                armazemId: armazemId || undefined,
                            })
                        }
                    />
                    <div className="map-legend" aria-label="Legenda do mapa">
                        <span className="map-state--free">Livre</span>
                        <span className="map-state--occupied">Ocupada</span>
                        <span className="map-state--other">Indisponível</span>
                        <span className="map-legend-flags">
                            <Icon name="senha" /> Bloqueada
                        </span>
                        <span className="map-legend-flags">
                            <Icon name="bookmark" /> Reservada
                        </span>
                    </div>
                    <button
                        type="button"
                        className="map-filter-toggle"
                        aria-expanded={filtersOpen}
                        aria-controls={filtersId}
                        onClick={() => setFiltersOpen((open) => !open)}
                    >
                        Filtros{code || filter !== "TODAS" ? " · ativos" : ""}
                        <Icon name="chevron-down" />
                    </button>
                </div>
                <div
                    id={filtersId}
                    className="map-filter-panel"
                    hidden={!filtersOpen}
                >
                    <div className="map-tools">
                        <form
                            onSubmit={(e) => {
                                e.preventDefault();
                                setCode(search.trim());
                                setPage(0);
                            }}
                        >
                            <label>
                                Buscar endereço
                                <input
                                    value={search}
                                    maxLength={40}
                                    onChange={(e) => setSearch(e.target.value)}
                                    placeholder="Código do endereço"
                                />
                            </label>
                            <button disabled={loading}>Buscar</button>
                            {code && (
                                <button
                                    type="button"
                                    onClick={() => {
                                        setSearch("");
                                        setCode("");
                                        setPage(0);
                                    }}
                                >
                                    Limpar
                                </button>
                            )}
                        </form>
                        <label>
                            Mostrar posições
                            <select
                                value={filter}
                                onChange={(e) => {
                                    setFilter(e.target.value);
                                    setPage(0);
                                }}
                            >
                                <option value="TODAS">Todas</option>
                                <option value="DISPONIVEL">Disponíveis</option>
                                <option value="OCUPADO">Ocupadas</option>
                            </select>
                        </label>
                    </div>
                    <p className="map-note">
                        O mapa e a ocupação física consideram todos os clientes
                        do armazém. Os demais indicadores seguem o cliente
                        selecionado. Busca e estado filtram somente o mapa.
                    </p>
                    <p className="map-note">
                        Um endereço de armazenagem ativo equivale a uma posição;
                        áreas especiais e endereços inativos ficam fora do
                        percentual. Os indicadores consideram todo o contexto,
                        independentemente da página do mapa.
                    </p>
                </div>
                {!loading &&
                    !result.error &&
                    data?.mapa.itens?.length === 0 && (
                        <p className="map-empty" role="status">
                            {code || filter !== "TODAS"
                                ? "Nenhum endereço corresponde à busca e ao estado selecionado."
                                : "Nenhum endereço cadastrado neste contexto."}
                        </p>
                    )}
                {data && !result.error && (
                    <>
                        <AddressMap
                            positions={data.mapa.itens ?? []}
                            onSelect={setSelected}
                            disabled={loading}
                        />
                        {data.mapa.totalPaginas > 1 ? (
                            <Pagination
                                page={page}
                                pages={data.mapa.totalPaginas}
                                total={data.mapa.totalItens}
                                count={data.mapa.itens?.length ?? 0}
                                size={100}
                                disabled={loading}
                                onPage={setPage}
                            />
                        ) : (
                            <p className="map-summary">
                                {quantity(data.mapa.totalItens)} endereços
                            </p>
                        )}
                    </>
                )}
            </section>
            {selected && (
                <PositionDetail
                    position={selected}
                    transport={transport}
                    onClose={() => setSelected(undefined)}
                    onAddress={onAddress}
                    clientSelected={!!context.clienteId}
                />
            )}
        </section>
    );
}

export function AddressMap({
    positions,
    onSelect,
    disabled = false,
}: {
    positions: VisaoOperacaoDto_Posicao[];
    onSelect: (p: VisaoOperacaoDto_Posicao) => void;
    disabled?: boolean;
}) {
    const groups = new Map<
        string,
        {
            name: string;
            streets: Map<string, Map<number, VisaoOperacaoDto_Posicao[]>>;
        }
    >();
    for (const p of positions) {
        if (!groups.has(p.armazemId))
            groups.set(p.armazemId, { name: p.armazem, streets: new Map() });
        const streets = groups.get(p.armazemId)!.streets;
        if (!streets.has(p.rua)) streets.set(p.rua, new Map());
        const levels = streets.get(p.rua)!;
        if (!levels.has(p.nivel)) levels.set(p.nivel, []);
        levels.get(p.nivel)!.push(p);
    }
    return (
        <div className="map-warehouses">
            {[...groups].map(([id, group]) => (
                <section
                    key={id}
                    className="map-warehouse"
                    aria-label={group.name}
                >
                    <h3 className={groups.size === 1 ? "sr-only" : undefined}>
                        <Icon name="armazens" />
                        {group.name}
                    </h3>
                    <div className="map-streets">
                        {[...group.streets].map(([street, levels]) => (
                            <MapStreet
                                key={street}
                                street={street}
                                levels={levels}
                                onSelect={onSelect}
                                disabled={disabled}
                            />
                        ))}
                    </div>
                </section>
            ))}
        </div>
    );
}

function MapStreet({
    street,
    levels,
    onSelect,
    disabled,
}: {
    street: string;
    levels: Map<number, VisaoOperacaoDto_Posicao[]>;
    onSelect: (p: VisaoOperacaoDto_Posicao) => void;
    disabled: boolean;
}) {
    const columns = [
        ...new Set([...levels.values()].flat().map((p) => p.posicao)),
    ].sort((a, b) => a.localeCompare(b, "pt-BR", { numeric: true }));
    const [capacity, setCapacity] = useState(0);
    const [start, setStart] = useState(0);
    const windowRef = useRef<HTMLDivElement>(null);
    const viewportId = useId();
    useEffect(() => {
        const el = windowRef.current;
        if (!el) return;
        const update = () => {
            if (!el.clientWidth) return;
            const style = getComputedStyle(el);
            const viewport = el.querySelector(".map-window-viewport")!;
            const viewportStyle = getComputedStyle(viewport);
            const level = el.querySelector(".map-level")!;
            const label = el.querySelector(".map-level-label")!;
            const gap = parseFloat(
                getComputedStyle(el.querySelector(".map-slots")!).columnGap,
            );
            const columnWidth = parseFloat(
                style.getPropertyValue("--map-slot-width"),
            );
            const available =
                el.clientWidth -
                label.getBoundingClientRect().width -
                parseFloat(getComputedStyle(level).columnGap) -
                parseFloat(viewportStyle.paddingLeft) -
                parseFloat(viewportStyle.paddingRight);
            setCapacity(
                Math.max(
                    1,
                    Math.floor((available + gap) / (columnWidth + gap)),
                ),
            );
        };
        update();
        if (typeof ResizeObserver === "undefined") return;
        const observer = new ResizeObserver(update);
        observer.observe(el);
        return () => observer.disconnect();
    }, [columns.length]);
    const count = Math.min(capacity || columns.length, columns.length);
    const maxStart = Math.max(0, columns.length - count);
    const offset = Math.min(start, maxStart);
    const visible = columns.slice(offset, offset + count);
    const paged = count < columns.length;
    return (
        <section className="map-street" aria-label={`Rua ${street}`}>
            <div className="map-street-heading">
                <h4>Rua {street}</h4>
                {paged && (
                    <div className="map-street-navigation">
                        <p className="map-window-note" aria-live="polite">
                            Colunas {offset + 1}–{offset + visible.length} de{" "}
                            {columns.length}
                        </p>
                        <button
                            type="button"
                            className="map-window-arrow"
                            aria-label={`Posições anteriores da rua ${street}`}
                            aria-controls={viewportId}
                            disabled={disabled || offset === 0}
                            onClick={() =>
                                setStart(Math.max(0, offset - count))
                            }
                        >
                            <Icon name="chevron-left" />
                        </button>
                        <button
                            type="button"
                            className="map-window-arrow"
                            aria-label={`Próximas posições da rua ${street}`}
                            aria-controls={viewportId}
                            disabled={disabled || offset >= maxStart}
                            onClick={() =>
                                setStart(Math.min(maxStart, offset + count))
                            }
                        >
                            <Icon name="chevron-right" />
                        </button>
                    </div>
                )}
            </div>
            <div>
                <div ref={windowRef} className="map-window">
                    <div
                        id={viewportId}
                        className="map-window-viewport"
                        role="group"
                        aria-label={`Posições da rua ${street}`}
                    >
                        <div className="map-levels">
                            {[...levels]
                                .sort(([a], [b]) => b - a)
                                .map(([level, slots]) => (
                                    <div className="map-level" key={level}>
                                        <span className="map-level-label">
                                            <span className="sr-only">
                                                Nível{" "}
                                            </span>
                                            {level}
                                        </span>
                                        <div
                                            className="map-slots"
                                            style={{
                                                gridTemplateColumns: `repeat(${visible.length}, var(--map-slot-width))`,
                                            }}
                                        >
                                            {slots
                                                .slice()
                                                .sort((a, b) =>
                                                    a.posicao.localeCompare(
                                                        b.posicao,
                                                        "pt-BR",
                                                        { numeric: true },
                                                    ),
                                                )
                                                .map((p) => {
                                                    const column =
                                                        visible.indexOf(
                                                            p.posicao,
                                                        );
                                                    return (
                                                        <button
                                                            type="button"
                                                            key={p.id}
                                                            hidden={column < 0}
                                                            style={{
                                                                gridRow: 1,
                                                                gridColumn:
                                                                    column >= 0
                                                                        ? column +
                                                                          1
                                                                        : undefined,
                                                            }}
                                                            disabled={disabled}
                                                            className={`map-position map-state--${p.ocupada ? "occupied" : p.disponivel ? "free" : "other"}`}
                                                            onClick={() =>
                                                                onSelect(p)
                                                            }
                                                            title={`${p.codigo} · ${positionState(p)}${p.bloqueada ? " · Bloqueado" : ""}${p.reservada ? " · Reservado" : ""}`}
                                                            aria-label={`${p.codigo} · ${positionState(p)}${p.bloqueada ? " · Bloqueado" : ""}${p.reservada ? " · Reservado" : ""}`}
                                                        >
                                                            <strong>
                                                                {p.codigo}
                                                            </strong>
                                                            {(p.bloqueada ||
                                                                p.reservada) && (
                                                                <span
                                                                    className="map-position-flags"
                                                                    aria-hidden="true"
                                                                >
                                                                    {p.bloqueada && (
                                                                        <Icon name="senha" />
                                                                    )}
                                                                    {p.reservada && (
                                                                        <Icon name="bookmark" />
                                                                    )}
                                                                </span>
                                                            )}
                                                        </button>
                                                    );
                                                })}
                                        </div>
                                    </div>
                                ))}
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
}
function PositionDetail({
    position,
    transport,
    onClose,
    onAddress,
    clientSelected,
}: {
    position: VisaoOperacaoDto_Posicao;
    transport: Transport;
    onClose: () => void;
    onAddress: (p: VisaoOperacaoDto_Posicao) => void;
    clientSelected: boolean;
}) {
    const [revision, setRevision] = useState(0),
        [result, setResult] = useState<{
            data?: VisaoOperacaoDto_Detalhe;
            error?: string;
        }>({});
    useEffect(() => {
        let active = true;
        const abort = new AbortController();
        void call(
            transport,
            "VisaoOperacaoController.detalhe",
            undefined,
            { id: position.id },
            {},
            abort.signal,
        )
            .then((data) => {
                if (active) setResult({ data });
            })
            .catch(() => {
                if (active)
                    setResult({
                        error: "Não foi possível consultar este endereço.",
                    });
            });
        return () => {
            active = false;
            abort.abort();
        };
    }, [transport, position.id, revision]);
    const p = result.data?.endereco;
    return (
        <Dialog title={position.codigo} icon="armazens" onClose={onClose} wide>
            {!result.data && !result.error && (
                <p role="status">Consultando o endereço…</p>
            )}
            {result.error && (
                <>
                    <p role="alert" className="error">
                        {result.error}
                    </p>
                    <button
                        onClick={() => {
                            setResult({});
                            setRevision((r) => r + 1);
                        }}
                    >
                        Tentar novamente
                    </button>
                </>
            )}
            {p && (
                <>
                    <dl className="position-facts">
                        {[
                            ["Armazém", p.armazem],
                            ["Rua", p.rua],
                            ["Nível", String(p.nivel)],
                            ["Posição", p.posicao],
                            ["Estado", positionState(p)],
                            ["Área", p.tipo.toLowerCase()],
                            [
                                "Capacidade de peso",
                                p.capacidadePesoKg == null
                                    ? "Não configurada"
                                    : `${quantity(p.capacidadePesoKg)} kg`,
                            ],
                            [
                                "Ocupação",
                                p.ocupada
                                    ? "Posição ocupada"
                                    : "Posição desocupada",
                            ],
                            [
                                "Altura",
                                result.data?.alturaMetros == null
                                    ? "Não configurada"
                                    : `${quantity(result.data.alturaMetros)} m`,
                            ],
                            [
                                "Largura",
                                result.data?.larguraMetros == null
                                    ? "Não configurada"
                                    : `${quantity(result.data.larguraMetros)} m`,
                            ],
                            [
                                "Profundidade",
                                result.data?.profundidadeMetros == null
                                    ? "Não configurada"
                                    : `${quantity(result.data.profundidadeMetros)} m`,
                            ],
                            [
                                "Empilhamento máximo",
                                result.data?.empilhamentoMaximo == null
                                    ? "Não configurado"
                                    : String(result.data.empilhamentoMaximo),
                            ],
                            [
                                "Tipo de unidade permitido",
                                result.data?.tipoUnidadePermitido ??
                                    "Não configurado",
                            ],
                        ].map(([label, value]) => (
                            <div key={label}>
                                <dt>{label}</dt>
                                <dd>{value}</dd>
                            </div>
                        ))}
                    </dl>
                    <section className="position-content">
                        <h3>Conteúdo do endereço</h3>
                        {result.data!.conteudoRestrito ? (
                            <p>
                                Posição ocupada. O conteúdo pertence a um
                                cliente fora do seu acesso.
                            </p>
                        ) : result.data!.unidades.length === 0 ? (
                            <p>Nenhuma unidade neste endereço.</p>
                        ) : (
                            result.data!.unidades.map((u) => (
                                <article key={u.id}>
                                    <h4>{u.produto}</h4>
                                    <p>
                                        {u.sku} · {quantity(u.quantidade)}{" "}
                                        {u.unidadeMedida}
                                    </p>
                                    <dl className="position-facts">
                                        <div>
                                            <dt>Etiqueta da unidade</dt>
                                            <dd>{u.codigo}</dd>
                                        </div>
                                        <div>
                                            <dt>Lote</dt>
                                            <dd>{u.lote ?? "Não informado"}</dd>
                                        </div>
                                        <div>
                                            <dt>Pedido de entrada</dt>
                                            <dd>#{u.pedidoEntradaId}</dd>
                                        </div>
                                        <div>
                                            <dt>
                                                Última movimentação registrada
                                            </dt>
                                            <dd>
                                                {u.ultimaMovimentacao
                                                    ? new Date(
                                                          u.ultimaMovimentacao,
                                                      ).toLocaleString("pt-BR")
                                                    : "Sem movimento registrado"}
                                            </dd>
                                        </div>
                                    </dl>
                                    <p>
                                        {u.bloqueada
                                            ? "Bloqueada"
                                            : "Sem bloqueio"}{" "}
                                        ·{" "}
                                        {u.reservada
                                            ? "Reserva ativa"
                                            : "Sem reserva ativa"}
                                        {u.quarentena ? " · Em quarentena" : ""}
                                    </p>
                                </article>
                            ))
                        )}
                    </section>
                    <footer className="position-actions">
                        {!p.disponivel ? (
                            <p>
                                Este endereço não está disponível para uma nova
                                alocação.
                            </p>
                        ) : !clientSelected ? (
                            <p>
                                Selecione o cliente no topo antes de endereçar
                                uma unidade.
                            </p>
                        ) : (
                            <p>
                                A unidade e a capacidade serão verificadas na
                                confirmação.
                            </p>
                        )}
                        <button type="button" onClick={onClose}>
                            Fechar
                        </button>
                        <button
                            type="button"
                            className="primary"
                            disabled={!p.disponivel || !clientSelected}
                            onClick={() => onAddress(p)}
                        >
                            Endereçar
                        </button>
                    </footer>
                </>
            )}
        </Dialog>
    );
}
