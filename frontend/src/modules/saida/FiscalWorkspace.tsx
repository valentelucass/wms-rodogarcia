import {
    useEffect,
    useEffectEvent,
    useState,
    type ComponentProps,
} from "react";
import type { JourneyPage } from "../../components/JourneyPage";
import type {
    ExpedicaoDto_Detalhe,
    FiscalNfeDto_Andamento,
    FiscalNfeDto_Linha,
    FiscalNfeDto_Previa,
    FiscalNfeDto_Solicitacao,
    FiscalNfeDto_RetornoOperacional,
    PaginaResponse,
} from "../../contracts/types";
import { isObject, type Values } from "../../contracts/runtime";
import { PageHeader } from "../../components/layout/PageHeader";
import { Pagination } from "../../components/layout/Pagination";
import { Dialog } from "../../components/layout/Dialog";
import { StatusBadge } from "../../design-system/StatusBadge";
import {
    RecordDialog,
    type RecordOpening,
} from "../../components/records/RecordDialog";
import { WorkflowReferences } from "../../components/context/WorkflowReferences";
import { recordPages } from "../../domain/recordPages";
import { canLeavePage } from "../../domain/pageLeave";
import {
    inventoryDate,
    inventoryQuantity,
} from "../regularizacao/inventoryApi";
import { invoiceMoney } from "../financeiro/invoicePresentation";
import { fiscalRead, useFiscalWrite } from "./fiscalApi";
import { information } from "../../content/information";
import { InformationHint } from "../../components/information/InformationPopup";
import { useRequestScope } from "../../hooks/useRequestScope";

type Props = ComponentProps<typeof JourneyPage>;
type FiscalPage = Omit<PaginaResponse<FiscalNfeDto_Linha>, "itens"> & {
    itens: FiscalNfeDto_Linha[];
};
type Detail = Omit<ExpedicaoDto_Detalhe, "pedido" | "documentos"> & {
    pedido: NonNullable<ExpedicaoDto_Detalhe["pedido"]> & {
        itens: NonNullable<
            NonNullable<ExpedicaoDto_Detalhe["pedido"]>["itens"]
        >;
    };
    documentos: NonNullable<ExpedicaoDto_Detalhe["documentos"]>;
};
export function FiscalWorkspace(props: Props) {
    const { transport, perfil, context, journey, workflow } = props;
    const clienteId = String(context.clienteId ?? ""),
        armazemId = String(context.armazemId ?? "");
    const [status, setStatus] = useState("TODOS"),
        [search, setSearch] = useState("");
    const [applied, setApplied] = useState({ status: "TODOS", search: "" });
    const [page, setPage] = useState(0),
        [revision, setRevision] = useState(0);
    const [list, setList] = useState<FiscalPage>();
    const [loading, setLoading] = useState(false),
        [error, setError] = useState("");
    const [selected, setSelected] = useState<FiscalNfeDto_Linha>();
    const [detail, setDetail] = useState<Detail>();
    const [tracking, setTracking] = useState<FiscalNfeDto_Andamento>();
    const [detailRevision, setDetailRevision] = useState(0);
    const [preview, setPreview] = useState<FiscalNfeDto_Previa>();
    const [reason, setReason] = useState(
        "Emissão fiscal conferida pelo responsável.",
    );
    const [confirmed, setConfirmed] = useState(false);
    const [nature, setNature] = useState("RETORNO_MERCADORIA");
    const [origin, setOrigin] = useState<string>();
    const [operationId, setOperationId] = useState("");
    const [cancel, setCancel] = useState<FiscalNfeDto_Solicitacao>();
    const [cancelReason, setCancelReason] = useState("");
    const [opening, setOpening] = useState<RecordOpening>();
    const [physicalContext, setPhysicalContext] = useState<Values>({});
    const write = useFiscalWrite(transport, perfil, props.onReceipt);
    const { begin: beginAuxiliary, cancel: cancelAuxiliary } =
        useRequestScope();
    const supervisor = perfil !== "OPERACAO";
    const locked = write.pending || write.uncertain;
    const poll = useEffectEvent((id: string) => {
        const s = tracking?.solicitacoes.find((s) => s.id === id);
        if (s && !locked) process(s);
    });
    const pendingId = tracking?.solicitacoes.find((s) =>
        ["PROCESSANDO", "ENVIANDO", "DESCONHECIDO", "CANCELANDO"].includes(
            s.situacao,
        ),
    )?.id;
    useEffect(() => {
        if (!pendingId || locked || !supervisor) return;
        const timer = window.setTimeout(() => poll(pendingId), 5000);
        return () => window.clearTimeout(timer);
    }, [pendingId, locked, supervisor, tracking]);
    useEffect(() => {
        if (!clienteId || !armazemId) return;
        const abort = new AbortController();
        let current = true;
        async function load() {
            setLoading(true);
            setError("");
            try {
                const r = await fiscalRead<PaginaResponse<FiscalNfeDto_Linha>>(
                    transport,
                    perfil,
                    "FiscalNfeController.listar",
                    {},
                    {
                        clienteId,
                        armazemId,
                        situacao: applied.status,
                        busca: applied.search,
                        pagina: page,
                        tamanho: 25,
                    },
                    abort.signal,
                );
                if (current) setList({ ...r.data, itens: r.data.itens ?? [] });
            } catch (e) {
                if (current)
                    setError(
                        e instanceof Error
                            ? e.message
                            : "Não foi possível consultar os pedidos.",
                    );
            } finally {
                if (current) setLoading(false);
            }
        }
        void load();
        return () => {
            current = false;
            abort.abort();
        };
    }, [transport, perfil, clienteId, armazemId, applied, page, revision]);
    const selectedId = selected?.id;
    useEffect(() => cancelAuxiliary, [selectedId, cancelAuxiliary]);
    useEffect(() => {
        if (!selectedId) return;
        const abort = new AbortController();
        let current = true;
        async function load() {
            setLoading(true);
            setError("");
            try {
                const [d, a] = await Promise.all([
                    fiscalRead<ExpedicaoDto_Detalhe>(
                        transport,
                        perfil,
                        "ExpedicaoController.consultar",
                        { id: selectedId },
                        {},
                        abort.signal,
                    ),
                    fiscalRead<FiscalNfeDto_Andamento>(
                        transport,
                        perfil,
                        "FiscalNfeController.andamento",
                        { id: selectedId },
                        {},
                        abort.signal,
                    ),
                ]);
                if (
                    !d.data.pedido ||
                    String(d.data.pedido.id) !== selectedId ||
                    String(a.data.pedidoId) !== selectedId
                )
                    throw new Error(
                        "O retorno não corresponde ao pedido consultado.",
                    );
                if (current) {
                    setDetail({
                        ...d.data,
                        pedido: {
                            ...d.data.pedido,
                            itens: d.data.pedido.itens ?? [],
                        },
                        documentos: d.data.documentos ?? [],
                    });
                    setTracking(a.data);
                }
            } catch (e) {
                if (current)
                    setError(
                        e instanceof Error
                            ? e.message
                            : "Não foi possível consultar a situação fiscal.",
                    );
            } finally {
                if (current) setLoading(false);
            }
        }
        void load();
        return () => {
            current = false;
            abort.abort();
        };
    }, [selectedId, detailRevision, transport, perfil]);
    function reload() {
        setRevision((v) => v + 1);
        setDetailRevision((v) => v + 1);
    }
    function received(data: unknown) {
        if (!isObject(data) || String(data.pedidoId) !== selectedId) {
            setError(
                "Resultado fiscal sem correspondência com o pedido selecionado.",
            );
            return;
        }
        setTracking(data as unknown as FiscalNfeDto_Andamento);
        setPreview(undefined);
        reload();
    }
    function prepare(nextNature = nature, document?: string) {
        if (!detail || !selected || locked) return;
        setNature(nextNature);
        setOrigin(document);
        setConfirmed(false);
        write.run(
            "FiscalNfeController.previa",
            { id: selected.id },
            {
                versao: detail.pedido.versao,
                natureza: nextNature,
                documentoOrigemId: document ?? null,
            },
            (data) => {
                if (!isObject(data) || String(data.pedidoId) !== selected.id) {
                    setError("Prévia de outro pedido recusada.");
                    return;
                }
                setPreview(data as unknown as FiscalNfeDto_Previa);
                setOperationId(crypto.randomUUID());
            },
        );
    }
    function finishPreview() {
        if (!selected || !preview || !confirmed) return;
        write.run(
            "FiscalNfeController.confirmar",
            { id: selected.id },
            {
                operacaoId: operationId,
                versao: preview.versao,
                hash: preview.hash,
                natureza: nature,
                documentoOrigemId: origin ?? null,
                motivo: reason,
            },
            received,
        );
    }
    function process(s: FiscalNfeDto_Solicitacao) {
        if (!selected) return;
        write.run(
            "FiscalNfeController.processar",
            { id: selected.id, solicitacaoId: s.id },
            {},
            received,
        );
    }
    async function download(s: FiscalNfeDto_Solicitacao) {
        if (!selected) return;
        const ticket = beginAuxiliary();
        try {
            const r = await fiscalRead<{ nome: string; conteudo: string }>(
                transport,
                perfil,
                "FiscalNfeController.arquivo",
                { id: selected.id, solicitacaoId: s.id },
                {},
                ticket.signal,
            );
            if (!ticket.isCurrent()) return;
            const blob = new Blob([r.data.conteudo], {
                type: "application/xml;charset=utf-8",
            });
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = r.data.nome;
            a.click();
            URL.revokeObjectURL(url);
        } catch (e) {
            if (ticket.isCurrent())
                setError(e instanceof Error ? e.message : "XML indisponível.");
        }
    }
    function legacy(action: string, preset: Values = {}) {
        if (!detail || !canLeavePage()) return;
        setPhysicalContext(preset);
        props.onRecord(detail as unknown as Values, "ExpedicaoDto.Detalhe");
        props.onRecord(
            detail.pedido as unknown as Values,
            "PedidoSaidaDto.Detalhe",
        );
        setOpening({
            action,
            row: detail as unknown as Values,
            type: "ExpedicaoDto.Detalhe",
        });
    }
    async function regularize(s: FiscalNfeDto_Solicitacao) {
        if (!selected || locked) return;
        const ticket = beginAuxiliary();
        try {
            const r = await fiscalRead<FiscalNfeDto_RetornoOperacional>(
                transport,
                perfil,
                "FiscalNfeController.retorno",
                { id: selected.id, solicitacaoId: s.id },
                {},
                ticket.signal,
            );
            if (!ticket.isCurrent()) return;
            if (r.data.pedidoId !== selected.id)
                throw new Error("A entrada fiscal não corresponde ao pedido.");
            legacy(
                r.data.houveRetirada
                    ? "ExpedicaoController.devolver"
                    : "ExpedicaoController.retornar",
                r.data.houveRetirada
                    ? {
                          solicitacaoEntradaId: r.data.solicitacaoEntradaId,
                          nota: r.data.nota,
                          itens: r.data.itens,
                      }
                    : { regularizacoesFiscais: r.data.regularizacoesFiscais },
            );
        } catch (e) {
            if (ticket.isCurrent())
                setError(
                    e instanceof Error
                        ? e.message
                        : "Não foi possível conferir a entrada fiscal.",
                );
        }
    }
    const feedback = (
        <>
            {write.pending && (
                <p role="status">Confirmando a operação fiscal…</p>
            )}
            {write.error && (
                <p role="alert" className="error">
                    {write.error}
                </p>
            )}
            {write.uncertain && (
                <div role="status">
                    <p>
                        A confirmação ficou sem resposta. Recupere a mesma
                        operação para evitar outra emissão.
                    </p>
                    <button
                        type="button"
                        disabled={write.pending}
                        onClick={write.retry}
                    >
                        Recuperar mesma confirmação
                    </button>
                </div>
            )}
        </>
    );
    return (
        <WorkflowReferences
            workflow={workflow}
            journey={journey}
            defaults={context}
        >
            <PageHeader
                title="Fiscal e retirada"
                icon="fiscal"
                description="Confira os pedidos, emita as notas e acompanhe a retirada da mercadoria."
            />
            <section
                className="fiscal-workspace"
                aria-label="Retirada fiscal da mercadoria"
            >
                <div className="fiscal-heading">
                    <div>
                        <h2>Retirada fiscal da mercadoria</h2>
                        <p>
                            Emissão da NF-e e confirmação da retirada no mesmo
                            pedido.
                        </p>
                    </div>
                    <InformationHint topic="fiscalWithdrawal" />
                </div>
                <form
                    className="fiscal-filters"
                    onSubmit={(e) => {
                        e.preventDefault();
                        if (locked) return;
                        setApplied({ status, search: search.trim() });
                        setPage(0);
                    }}
                >
                    <label>
                        Pedido
                        <input
                            value={search}
                            maxLength={40}
                            disabled={locked}
                            onChange={(e) => setSearch(e.target.value)}
                            placeholder="Buscar pelo número do pedido"
                        />
                    </label>
                    <label>
                        Situação da NF-e
                        <select
                            value={status}
                            disabled={locked}
                            onChange={(e) => setStatus(e.target.value)}
                        >
                            <option value="TODOS">Todas</option>
                            <option value="PENDENTE">PENDENTE</option>
                            <option value="FINALIZADO">FINALIZADO</option>
                        </select>
                    </label>
                    <button type="submit" disabled={locked || loading}>
                        Aplicar filtros
                    </button>
                    <button
                        type="button"
                        disabled={locked || loading}
                        onClick={reload}
                    >
                        Atualizar lista
                    </button>
                </form>
                {!clienteId || !armazemId ? (
                    <p>
                        Selecione cliente e armazém no cabeçalho para consultar
                        os pedidos.
                    </p>
                ) : null}
                {loading && (
                    <p role="status">
                        Consultando pedidos e documentos fiscais…
                    </p>
                )}
                {error && (
                    <p role="alert" className="error">
                        {error}
                    </p>
                )}
                {!preview && !cancel && feedback}
                {list && (
                    <>
                        <div className="fiscal-table-scroll">
                            <table
                                className="fiscal-table"
                                aria-label="Pedidos de saída e NF-e"
                            >
                                <thead>
                                    <tr>
                                        <th>Nº do pedido</th>
                                        <th>Data do pedido</th>
                                        <th>Cliente</th>
                                        <th>Status da NF-e</th>
                                        <th>Nº NF-e</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {list.itens.map((row) => (
                                        <tr key={row.id}>
                                            <td>
                                                <button
                                                    type="button"
                                                    className="fiscal-link"
                                                    disabled={locked}
                                                    onClick={() => {
                                                        setSelected(row);
                                                        setDetail(undefined);
                                                        setTracking(undefined);
                                                        setDetailRevision(
                                                            (v) => v + 1,
                                                        );
                                                        setError("");
                                                    }}
                                                >
                                                    {row.referencia}
                                                </button>
                                            </td>
                                            <td>
                                                {inventoryDate(row.criadoEm)}
                                            </td>
                                            <td>{row.cliente}</td>
                                            <td>
                                                <StatusBadge
                                                    value={row.situacaoFiscal}
                                                    label={row.situacaoFiscal}
                                                />
                                            </td>
                                            <td>
                                                {row.numerosNfe.length ? (
                                                    <span>
                                                        {row.numerosNfe.join(
                                                            ", ",
                                                        )}
                                                    </span>
                                                ) : supervisor &&
                                                  row.podeEmitir ? (
                                                    <button
                                                        type="button"
                                                        disabled={locked}
                                                        onClick={() => {
                                                            setSelected(row);
                                                            setDetail(
                                                                undefined,
                                                            );
                                                            setTracking(
                                                                undefined,
                                                            );
                                                            setDetailRevision(
                                                                (v) => v + 1,
                                                            );
                                                        }}
                                                    >
                                                        EMITIR NF-e
                                                    </button>
                                                ) : (
                                                    <span>
                                                        {row.situacaoOperacional ===
                                                        "SEPARADO"
                                                            ? "Pendente"
                                                            : "Aguardando preparação"}
                                                    </span>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        {!list.itens.length && !loading && (
                            <p>
                                Nenhum pedido encontrado para os filtros
                                selecionados.
                            </p>
                        )}
                        <Pagination
                            page={list.pagina}
                            pages={list.totalPaginas}
                            total={list.totalItens}
                            count={list.itens.length}
                            size={list.tamanho}
                            disabled={locked || loading}
                            onPage={setPage}
                        />
                    </>
                )}
                {selected && detail && (
                    <section
                        className="fiscal-detail"
                        aria-label={`Pedido ${selected.referencia}`}
                    >
                        <header className="fiscal-heading">
                            <div>
                                <h2>Pedido {selected.referencia}</h2>
                                <p>
                                    {selected.cliente} ·{" "}
                                    {inventoryDate(detail.pedido.criadoEm)} ·{" "}
                                    <StatusBadge
                                        value={detail.pedido.situacao}
                                    />
                                </p>
                            </div>
                            <button
                                type="button"
                                disabled={locked}
                                onClick={() => {
                                    setSelected(undefined);
                                    setDetail(undefined);
                                    setTracking(undefined);
                                }}
                            >
                                Fechar pedido
                            </button>
                        </header>
                        <div className="fiscal-actions">
                            {supervisor &&
                                detail.pedido.situacao === "SEPARADO" && (
                                    <>
                                        <button
                                            type="button"
                                            disabled={locked}
                                            onClick={() =>
                                                prepare("RETORNO_MERCADORIA")
                                            }
                                        >
                                            Conferir e emitir NF-e
                                        </button>
                                        <button
                                            type="button"
                                            disabled={locked}
                                            onClick={() =>
                                                prepare("RETORNO_SIMBOLICO")
                                            }
                                        >
                                            Conferir retorno somente fiscal
                                        </button>
                                        <button
                                            type="button"
                                            disabled={locked}
                                            onClick={() =>
                                                legacy(
                                                    "ExpedicaoController.retirar",
                                                )
                                            }
                                        >
                                            Confirmar retirada física
                                        </button>
                                    </>
                                )}
                            <button
                                type="button"
                                disabled={locked}
                                onClick={reload}
                            >
                                Atualizar pedido
                            </button>
                        </div>
                        <p>
                            {detail.retiradaEm
                                ? `Retirada registrada em ${inventoryDate(detail.retiradaEm)}.`
                                : "Retirada física ainda não confirmada."}
                        </p>
                        <div className="fiscal-table-scroll">
                            <table
                                className="fiscal-table"
                                aria-label="Itens do pedido"
                            >
                                <thead>
                                    <tr>
                                        <th>SKU</th>
                                        <th>Quantidade solicitada</th>
                                        <th>Situação</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {detail.pedido.itens.map((item) => (
                                        <tr key={item.id}>
                                            <td>{item.sku}</td>
                                            <td>
                                                {inventoryQuantity(
                                                    item.quantidade,
                                                )}
                                            </td>
                                            <td>
                                                <StatusBadge
                                                    value={
                                                        detail.pedido.situacao
                                                    }
                                                />
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                        <h3>Notas e acompanhamento</h3>
                        {!tracking?.solicitacoes.length && (
                            <p>
                                Nenhuma solicitação de emissão integrada neste
                                pedido. Os documentos já registrados aparecem
                                abaixo.
                            </p>
                        )}
                        {tracking?.solicitacoes.map((s) => (
                            <article className="fiscal-note" key={s.id}>
                                <div>
                                    <strong>
                                        {s.numero
                                            ? `NF-e ${s.numero} · série ${s.serie}`
                                            : "NF-e aguardando autorização"}
                                    </strong>
                                    <StatusBadge value={s.situacao} />
                                    <p>
                                        {s.natureza === "REGULARIZACAO_ENTRADA"
                                            ? "Nova entrada de regularização"
                                            : s.natureza === "RETORNO_SIMBOLICO"
                                              ? "Retorno somente fiscal"
                                              : "Retorno de mercadoria"}
                                    </p>
                                    <p>{s.mensagem}</p>
                                    {s.chaveAcesso && (
                                        <p className="fiscal-key">
                                            Chave: {s.chaveAcesso}
                                        </p>
                                    )}
                                </div>
                                <div className="fiscal-actions">
                                    {s.podeProcessar && (
                                        <button
                                            type="button"
                                            disabled={locked}
                                            onClick={() => process(s)}
                                        >
                                            {s.situacao === "PREPARADA"
                                                ? "Enviar à Notazz"
                                                : "Consultar resultado"}
                                        </button>
                                    )}
                                    {s.xmlDisponivel && (
                                        <button
                                            type="button"
                                            disabled={locked}
                                            onClick={() => void download(s)}
                                        >
                                            Baixar XML
                                        </button>
                                    )}
                                    {s.podeCancelar && (
                                        <button
                                            type="button"
                                            disabled={locked}
                                            onClick={() => {
                                                setCancel(s);
                                                setCancelReason("");
                                            }}
                                        >
                                            Solicitar cancelamento
                                        </button>
                                    )}
                                    {supervisor &&
                                        s.situacao === "AUTORIZADA" &&
                                        s.documentoId &&
                                        s.natureza === "RETORNO_MERCADORIA" && (
                                            <button
                                                type="button"
                                                disabled={locked}
                                                onClick={() =>
                                                    prepare(
                                                        "REGULARIZACAO_ENTRADA",
                                                        s.documentoId!,
                                                    )
                                                }
                                            >
                                                Emitir nova entrada
                                            </button>
                                        )}
                                </div>
                            </article>
                        ))}
                        <details>
                            <summary>Documentos já registrados</summary>
                            <ul>
                                {detail.documentos.map((d) => (
                                    <li key={d.id}>
                                        NF-e {d.nota?.numero} · série{" "}
                                        {d.nota?.serie} ·{" "}
                                        <StatusBadge value={d.situacao} />
                                        {supervisor &&
                                            d.id &&
                                            d.situacao === "AUTORIZADO" &&
                                            d.natureza ===
                                                "RETORNO_MERCADORIA" && (
                                                <button
                                                    type="button"
                                                    disabled={locked}
                                                    onClick={() =>
                                                        prepare(
                                                            "REGULARIZACAO_ENTRADA",
                                                            d.id!,
                                                        )
                                                    }
                                                >
                                                    Conferir nova entrada
                                                </button>
                                            )}
                                    </li>
                                ))}
                            </ul>
                            {supervisor && (
                                <button
                                    type="button"
                                    disabled={locked}
                                    onClick={() =>
                                        legacy("ExpedicaoController.documento")
                                    }
                                >
                                    Registrar nota existente
                                </button>
                            )}
                        </details>
                        {supervisor &&
                            tracking?.solicitacoes
                                .filter(
                                    (s) =>
                                        s.natureza ===
                                            "REGULARIZACAO_ENTRADA" &&
                                        s.situacao === "AUTORIZADA",
                                )
                                .map((s) => (
                                    <button
                                        key={s.id}
                                        type="button"
                                        disabled={locked}
                                        onClick={() => void regularize(s)}
                                    >
                                        Processar retorno da NF-e {s.numero}
                                    </button>
                                ))}
                        {supervisor && (
                            <details>
                                <summary>Retorno e devolução</summary>
                                <p>{information.fiscalReturn.description}</p>
                                <div className="fiscal-actions">
                                    <button
                                        type="button"
                                        disabled={locked}
                                        onClick={() =>
                                            legacy(
                                                "ExpedicaoController.retornar",
                                            )
                                        }
                                    >
                                        Reendereçar mercadoria separada
                                    </button>
                                    <button
                                        type="button"
                                        disabled={locked}
                                        onClick={() =>
                                            legacy(
                                                "ExpedicaoController.devolver",
                                            )
                                        }
                                    >
                                        Registrar chegada da devolução
                                    </button>
                                </div>
                            </details>
                        )}
                    </section>
                )}
            </section>
            {preview && (
                <Dialog
                    title={
                        nature === "REGULARIZACAO_ENTRADA"
                            ? "Conferir nova nota de entrada"
                            : "Conferir emissão da NF-e"
                    }
                    onClose={() => setPreview(undefined)}
                    locked={locked}
                    wide
                    icon="fiscal"
                >
                    {feedback}
                    {!preview.podeEmitir && (
                        <section role="alert">
                            <h3>Complete os dados antes de emitir</h3>
                            <ul>
                                {preview.pendencias.map((p) => (
                                    <li key={p}>{p}</li>
                                ))}
                            </ul>
                        </section>
                    )}
                    {preview.documentos.map((d, index) => (
                        <section key={d.grupo}>
                            <h3>
                                Nota {index + 1} de {preview.documentos.length}
                            </h3>
                            <dl className="fiscal-summary">
                                <div>
                                    <dt>Emitente</dt>
                                    <dd>{d.emitenteCnpj}</dd>
                                </div>
                                <div>
                                    <dt>Proprietário / destinatário</dt>
                                    <dd>
                                        {d.destinatarioNome} ·{" "}
                                        {d.destinatarioDocumento}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Operação</dt>
                                    <dd>{d.naturezaOperacao}</dd>
                                </div>
                                <div>
                                    <dt>Ambiente</dt>
                                    <dd>
                                        {d.ambiente === 2
                                            ? "Homologação"
                                            : "Produção"}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Valor da mercadoria</dt>
                                    <dd>{invoiceMoney(d.valorTotal)}</dd>
                                </div>
                                <div>
                                    <dt>Observações</dt>
                                    <dd>{d.observacao}</dd>
                                </div>
                            </dl>
                            <div className="fiscal-table-scroll">
                                <table
                                    className="fiscal-table"
                                    aria-label={`Itens da nota ${index + 1}`}
                                >
                                    <thead>
                                        <tr>
                                            <th>SKU / produto</th>
                                            <th>Quantidade</th>
                                            <th>Valor unitário</th>
                                            <th>Total</th>
                                            <th>NCM / CFOP</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {d.itens.map((i, n) => (
                                            <tr
                                                key={`${i.reservaId}-${i.entradaOrigemId}-${n}`}
                                            >
                                                <td>
                                                    {i.sku} · {i.descricao}
                                                </td>
                                                <td>
                                                    {inventoryQuantity(
                                                        i.quantidade,
                                                    )}{" "}
                                                    {i.unidade}
                                                </td>
                                                <td>
                                                    {inventoryQuantity(
                                                        i.valorUnitario,
                                                    )}
                                                </td>
                                                <td>
                                                    {invoiceMoney(i.valorTotal)}
                                                </td>
                                                <td>
                                                    {i.ncm} / {i.cfop}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                            <details>
                                <summary>
                                    Notas referenciadas (
                                    {d.chavesReferenciadas.length})
                                </summary>
                                <ul>
                                    {d.chavesReferenciadas.map((c) => (
                                        <li className="fiscal-key" key={c}>
                                            {c}
                                        </li>
                                    ))}
                                </ul>
                            </details>
                        </section>
                    ))}
                    <p>
                        {nature === "REGULARIZACAO_ENTRADA"
                            ? "O fiscal emite a nova entrada. Depois, o operador confirma a chegada e o reendereçamento; a saída original permanece registrada."
                            : "A autorização da nota não confirma a retirada física da mercadoria."}
                    </p>
                    <label>
                        Motivo da emissão
                        <textarea
                            value={reason}
                            minLength={5}
                            maxLength={500}
                            disabled={locked}
                            onChange={(e) => setReason(e.target.value)}
                        />
                    </label>
                    <label className="fiscal-confirm">
                        <input
                            type="checkbox"
                            checked={confirmed}
                            disabled={locked || !preview.podeEmitir}
                            onChange={(e) => setConfirmed(e.target.checked)}
                        />
                        Conferi os dados do pedido, das origens e das notas.
                    </label>
                    <div className="fiscal-actions">
                        <button
                            type="button"
                            disabled={
                                locked ||
                                !preview.podeEmitir ||
                                !confirmed ||
                                reason.trim().length < 5
                            }
                            onClick={finishPreview}
                        >
                            Confirmar emissão
                        </button>
                        <button
                            type="button"
                            disabled={locked}
                            onClick={() => setPreview(undefined)}
                        >
                            Voltar à conferência
                        </button>
                    </div>
                </Dialog>
            )}
            {cancel && selected && (
                <Dialog
                    title={`Cancelar NF-e ${cancel.numero}`}
                    onClose={() => setCancel(undefined)}
                    locked={locked}
                >
                    {feedback}
                    <p>
                        O cancelamento depende da confirmação fiscal. Se houve
                        circulação que o impeça, registre o retorno com uma nova
                        nota de entrada.
                    </p>
                    <label>
                        Motivo do cancelamento
                        <textarea
                            value={cancelReason}
                            minLength={30}
                            maxLength={500}
                            disabled={locked}
                            onChange={(e) => setCancelReason(e.target.value)}
                        />
                    </label>
                    <button
                        type="button"
                        disabled={locked || cancelReason.trim().length < 30}
                        onClick={() =>
                            write.run(
                                "FiscalNfeController.cancelar",
                                { id: selected.id, solicitacaoId: cancel.id },
                                {
                                    operacaoId: crypto.randomUUID(),
                                    motivo: cancelReason,
                                },
                                (data) => {
                                    received(data);
                                    setCancel(undefined);
                                },
                            )
                        }
                    >
                        Confirmar solicitação de cancelamento
                    </button>
                </Dialog>
            )}
            {opening && (
                <RecordDialog
                    opening={opening}
                    page={recordPages.fiscal[0]}
                    actions={journey.steps
                        .flatMap((s) => s.actions)
                        .filter((id) => id.startsWith("ExpedicaoController."))}
                    transport={transport}
                    context={{ ...context, ...physicalContext }}
                    workflow={workflow}
                    perfil={perfil}
                    journey={journey}
                    onClose={() => {
                        setOpening(undefined);
                        reload();
                    }}
                    onUnknownClose={() => {
                        setOpening(undefined);
                        reload();
                    }}
                    onReceipt={(r, type, id, request) => {
                        props.onReceipt(r, type, id, request);
                        if (request.endpoint.method !== "GET") reload();
                    }}
                    onRecord={props.onRecord}
                    onNavigate={props.onNavigate}
                />
            )}
        </WorkflowReferences>
    );
}
