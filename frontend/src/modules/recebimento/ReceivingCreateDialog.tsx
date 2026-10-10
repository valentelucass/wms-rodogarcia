import { useEffect, useRef, useState } from "react";
import {
    ApiError,
    call,
    type Receipt,
    type Request,
    type Transport,
} from "../../api/client";
import { endpoint } from "../../contracts/runtime";
import type {
    NfeImportacaoDto_Confirmacao,
    NfeImportacaoDto_Confirmar,
    NfeImportacaoDto_Previa,
    ProdutoDto_Resposta,
} from "../../contracts/types";
import { Dialog } from "../../components/layout/Dialog";
import { ReferenceSelect } from "../../components/context/ReferenceSelect";
import { registerPageLeave } from "../../domain/pageLeave";
import { NfeDocument, downloadXml } from "./NfeDocument";

export function ReceivingCreateDialog({
    transport,
    onClose,
    onManual,
    onConfirmed,
    onExisting,
}: {
    transport: Transport;
    onClose: () => void;
    onManual: () => void;
    onConfirmed: (
        receipt: Receipt,
        request: Request,
        data: NfeImportacaoDto_Confirmacao,
    ) => void;
    onExisting: (id: string) => void;
}) {
    const [mode, setMode] = useState<"choose" | "xml">("choose");
    const [xml, setXml] = useState(""),
        [fileName, setFileName] = useState("");
    const [cliente, setCliente] = useState(""),
        [armazem, setArmazem] = useState("");
    const [referencia, setReferencia] = useState("");
    const [associacoes, setAssociacoes] = useState<Record<number, string>>({});
    const [previa, setPrevia] = useState<NfeImportacaoDto_Previa>();
    const [products, setProducts] = useState<ProdutoDto_Resposta[]>([]),
        [catalogError, setCatalogError] = useState("");
    const [pending, setPending] = useState(""),
        [error, setError] = useState("");
    const [unknown, setUnknown] = useState(false);
    const command = useRef<NfeImportacaoDto_Confirmar | undefined>(undefined);
    const lifetime = useRef(new AbortController());
    const lock = useRef(false);
    const fileInput = useRef<HTMLInputElement>(null);
    useEffect(() => {
        lifetime.current = new AbortController();
        return () => lifetime.current.abort();
    }, []);
    const close = () => {
        if (pending || unknown) return;
        if (
            !xml ||
            window.confirm("Descartar a prévia e o arquivo selecionado?")
        )
            onClose();
    };
    useEffect(
        () =>
            registerPageLeave(() => {
                if (pending || unknown) return false;
                return (
                    !xml ||
                    window.confirm(
                        "Descartar a prévia e o arquivo selecionado?",
                    )
                );
            }),
        [pending, unknown, xml],
    );
    useEffect(() => {
        const controller = new AbortController();
        setProducts([]);
        setCatalogError("");
        if (cliente)
            void (async () => {
                const all: ProdutoDto_Resposta[] = [];
                for (let pagina = 0; ; pagina++) {
                    const page = await call(
                        transport,
                        "ProdutoController.listar",
                        undefined,
                        {},
                        { clienteId: cliente, pagina, tamanho: 100 },
                        controller.signal,
                    );
                    all.push(...(page.itens ?? []));
                    if (pagina + 1 >= Number(page.totalPaginas)) break;
                }
                if (!controller.signal.aborted) setProducts(all);
            })().catch((e) => {
                if (!controller.signal.aborted)
                    setCatalogError(
                        e instanceof Error
                            ? e.message
                            : "Não foi possível consultar produtos.",
                    );
            });
        return () => controller.abort();
    }, [cliente, transport]);
    const invalidate = () => {
        setPrevia((old) =>
            old ? { ...old, podeConfirmar: false, revisaoPrevia: null } : old,
        );
        setError("");
        command.current = undefined;
    };
    const clear = () => {
        setXml("");
        setFileName("");
        setPrevia(undefined);
        setAssociacoes({});
        setReferencia("");
        setError("");
        setCliente("");
        setArmazem("");
        command.current = undefined;
    };
    const read = async (file?: File) => {
        if (!file || lock.current || unknown) return;
        lock.current = true;
        clear();
        setPending("Lendo arquivo…");
        try {
            if (!file.name.toLowerCase().endsWith(".xml"))
                throw new Error("Selecione um arquivo .xml da NF-e.");
            if (file.size > 1000000)
                throw new Error("O XML deve ter no máximo 1 MB.");
            const text = new TextDecoder("utf-8", {
                fatal: true,
                ignoreBOM: true,
            }).decode(await file.arrayBuffer());
            if (!text.trim()) throw new Error("O arquivo está vazio.");
            if (!lifetime.current.signal.aborted) {
                setXml(text);
                setFileName(file.name);
            }
        } catch (e) {
            if (!lifetime.current.signal.aborted)
                setError(
                    e instanceof Error
                        ? e.message
                        : "Não foi possível ler o arquivo UTF-8.",
                );
        } finally {
            lock.current = false;
            if (!lifetime.current.signal.aborted) setPending("");
        }
    };
    const mappings = () =>
        Object.entries(associacoes)
            .filter(([, id]) => id)
            .map(([numero, produtoId]) => ({
                numeroItem: Number(numero),
                produtoId,
            }));
    const preview = async () => {
        if (lock.current || unknown) return;
        lock.current = true;
        setPending("Conferindo o XML…");
        setError("");
        try {
            const data = await call(
                transport,
                "PedidoEntradaXmlController.previa",
                {
                    xml,
                    clienteId: cliente || null,
                    armazemId: armazem || null,
                    associacoes: mappings(),
                },
                {},
                {},
                lifetime.current.signal,
            );
            if (lifetime.current.signal.aborted) return;
            setPrevia(data);
            setCliente(data.clienteId ?? "");
            setArmazem(data.armazemId ?? "");
            setAssociacoes(
                Object.fromEntries(
                    data.itens?.map((i) => [i.numeroItem, i.produtoId ?? ""]) ??
                        [],
                ),
            );
            if (!referencia && data.documento)
                setReferencia(
                    `NFE-${data.documento.numero}-${data.documento.serie}`,
                );
        } catch (e) {
            if (!lifetime.current.signal.aborted) {
                setError(
                    e instanceof Error
                        ? e.message
                        : "Não foi possível conferir o XML.",
                );
                setPrevia((old) =>
                    old
                        ? { ...old, podeConfirmar: false, revisaoPrevia: null }
                        : old,
                );
            }
        } finally {
            lock.current = false;
            if (!lifetime.current.signal.aborted) setPending("");
        }
    };
    const confirm = async (repeat = false) => {
        if (lock.current) return;
        const data = repeat
            ? command.current
            : previa?.podeConfirmar && previa.revisaoPrevia
              ? {
                    operacaoId: crypto.randomUUID(),
                    clienteId: cliente,
                    armazemId: armazem,
                    referencia,
                    xml,
                    revisaoPrevia: previa.revisaoPrevia,
                    associacoes: mappings(),
                }
              : undefined;
        if (!data) return;
        command.current = data;
        lock.current = true;
        setPending("Confirmando pedido…");
        setError("");
        const request: Request = {
            endpoint: endpoint("PedidoEntradaXmlController.confirmar"),
            params: {},
            query: {},
            body: data,
            signal: lifetime.current.signal,
        };
        try {
            const receipt = await transport.send(request);
            if (!lifetime.current.signal.aborted)
                onConfirmed(
                    receipt,
                    request,
                    receipt.data as NfeImportacaoDto_Confirmacao,
                );
        } catch (e) {
            if (!lifetime.current.signal.aborted) {
                const uncertain =
                    !(e instanceof ApiError) || e.uncertain || e.status >= 500;
                if (!uncertain) invalidate();
                setUnknown(uncertain);
                setError(
                    uncertain
                        ? "O resultado da confirmação é desconhecido. Consulte a confirmação antes de iniciar outro pedido."
                        : e.message,
                );
            }
        } finally {
            lock.current = false;
            if (!lifetime.current.signal.aborted) setPending("");
        }
    };
    const recover = async () => {
        if (lock.current || !command.current) return;
        lock.current = true;
        setPending("Consultando confirmação…");
        setError("");
        const request: Request = {
            endpoint: endpoint("PedidoEntradaXmlController.resultado"),
            params: { operacaoId: command.current.operacaoId },
            query: {},
            signal: lifetime.current.signal,
        };
        try {
            const receipt = await transport.send(request);
            if (!lifetime.current.signal.aborted)
                onConfirmed(
                    receipt,
                    request,
                    receipt.data as NfeImportacaoDto_Confirmacao,
                );
        } catch (e) {
            if (!lifetime.current.signal.aborted)
                setError(
                    e instanceof ApiError && e.status === 404
                        ? "Confirmação ainda não encontrada. Consulte novamente ou repita a mesma confirmação; seu identificador será preservado."
                        : e instanceof Error
                          ? e.message
                          : "Não foi possível consultar a confirmação.",
                );
        } finally {
            lock.current = false;
            if (!lifetime.current.signal.aborted) setPending("");
        }
    };
    const blocked = Boolean(pending) || unknown;
    return (
        <Dialog
            title={mode === "choose" ? "Novo pedido" : "Importar XML da NF-e"}
            onClose={close}
            locked={blocked}
            wide={mode === "xml"}
        >
            {mode === "choose" ? (
                <div className="nfe-create-choices">
                    <p>Como deseja criar o pedido de entrada?</p>
                    <button
                        type="button"
                        className="primary"
                        data-dialog-autofocus="true"
                        onClick={onManual}
                    >
                        Criar manualmente
                    </button>
                    <button type="button" onClick={() => setMode("xml")}>
                        Importar XML da NF-e
                    </button>
                </div>
            ) : (
                <div className="nfe-create">
                    <p>
                        Selecione a nota, confira a prévia e associe os
                        produtos. O pedido só será gravado ao confirmar.
                    </p>
                    <label>
                        Arquivo XML da NF-e
                        <input
                            ref={fileInput}
                            type="file"
                            accept=".xml,application/xml,text/xml"
                            disabled={blocked}
                            onChange={(e) => {
                                void read(e.target.files?.[0]);
                            }}
                        />
                    </label>
                    {fileName && (
                        <div className="record-header-actions">
                            <span>{fileName}</span>
                            <button
                                type="button"
                                disabled={blocked}
                                onClick={() => {
                                    clear();
                                    if (fileInput.current)
                                        fileInput.current.value = "";
                                }}
                            >
                                Remover arquivo
                            </button>
                            <button
                                type="button"
                                disabled={blocked || !xml}
                                onClick={() => void preview()}
                            >
                                {previa ? "Atualizar prévia" : "Ler XML"}
                            </button>
                        </div>
                    )}
                    {pending && <p role="status">{pending}</p>}
                    {error && <p role="alert">{error}</p>}
                    {unknown && (
                        <div className="record-feedback record-feedback--unknown">
                            <p>Identificador: {command.current?.operacaoId}</p>
                            <button
                                type="button"
                                disabled={Boolean(pending)}
                                onClick={() => void recover()}
                            >
                                Consultar confirmação
                            </button>
                            <button
                                type="button"
                                disabled={Boolean(pending)}
                                onClick={() => void confirm(true)}
                            >
                                Repetir a mesma confirmação
                            </button>
                        </div>
                    )}
                    {previa?.documento && (
                        <>
                            <NfeDocument documento={previa.documento} />
                            <button
                                type="button"
                                onClick={() =>
                                    downloadXml(
                                        xml,
                                        previa.documento?.chaveAcesso ?? null,
                                    )
                                }
                            >
                                Baixar XML selecionado
                            </button>
                            <h3>Destino no WMS</h3>
                            <div className="nfe-context">
                                <ReferenceSelect
                                    kind="clientes"
                                    title="Cliente proprietário"
                                    value={cliente}
                                    disabled={blocked}
                                    required
                                    onChange={(id) => {
                                        setCliente(id);
                                        setAssociacoes({});
                                        invalidate();
                                    }}
                                />
                                <ReferenceSelect
                                    kind="armazens"
                                    title="Armazém de recebimento"
                                    value={armazem}
                                    disabled={blocked}
                                    required
                                    onChange={(id) => {
                                        setArmazem(id);
                                        invalidate();
                                    }}
                                />
                                <label>
                                    Referência do pedido
                                    <input
                                        value={referencia}
                                        maxLength={40}
                                        pattern="[A-Za-z0-9][A-Za-z0-9._/\-]*"
                                        disabled={blocked}
                                        onChange={(e) =>
                                            setReferencia(e.target.value)
                                        }
                                    />
                                </label>
                            </div>
                            <h3>Produtos do cliente proprietário</h3>
                            {catalogError && <p role="alert">{catalogError}</p>}
                            <div className="nfe-associations">
                                {previa.documento.itens?.map((i) => {
                                    const mapped = previa.itens?.find(
                                        (m) => m.numeroItem === i.numeroItem,
                                    );
                                    return (
                                        <div key={i.numeroItem}>
                                            <label>
                                                Produto do item {i.numeroItem} ·{" "}
                                                {i.codigo}
                                                <select
                                                    value={
                                                        associacoes[
                                                            i.numeroItem
                                                        ] ?? ""
                                                    }
                                                    disabled={
                                                        blocked || !cliente
                                                    }
                                                    onChange={(e) => {
                                                        setAssociacoes(
                                                            (old) => ({
                                                                ...old,
                                                                [i.numeroItem]:
                                                                    e.target
                                                                        .value,
                                                            }),
                                                        );
                                                        invalidate();
                                                    }}
                                                >
                                                    <option value="">
                                                        Selecionar produto
                                                    </option>
                                                    {products.map((p) => (
                                                        <option
                                                            key={p.id}
                                                            value={p.id ?? ""}
                                                            disabled={
                                                                p.situacao !==
                                                                "ATIVO"
                                                            }
                                                        >
                                                            {p.sku} ·{" "}
                                                            {p.descricao} ·{" "}
                                                            {p.unidadeMedida}
                                                        </option>
                                                    ))}
                                                </select>
                                            </label>
                                            {mapped?.quantidadeEstoque && (
                                                <p>
                                                    Previsto no estoque:{" "}
                                                    {mapped.quantidadeEstoque}{" "}
                                                    {mapped.unidadeEstoque} ·
                                                    fator{" "}
                                                    {mapped.fatorConversao}
                                                </p>
                                            )}
                                            {mapped?.pendencia && (
                                                <p>{mapped.pendencia}</p>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                            {previa.avisos?.map((m, i) => (
                                <p key={i}>{m}</p>
                            ))}
                            {!!previa.pendencias?.length && (
                                <div role="status">
                                    <strong>Resolva antes de confirmar</strong>
                                    <ul>
                                        {previa.pendencias.map((p, i) => (
                                            <li key={i}>{p}</li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                            {previa.pedidoExistenteId && (
                                <button
                                    type="button"
                                    disabled={blocked}
                                    onClick={() =>
                                        onExisting(previa.pedidoExistenteId!)
                                    }
                                >
                                    Ver pedido existente
                                </button>
                            )}
                            {!previa.revisaoPrevia && (
                                <p role="status">
                                    As escolhas mudaram. Atualize a prévia para
                                    conferir novamente.
                                </p>
                            )}
                            <div className="record-header-actions">
                                <button
                                    type="button"
                                    disabled={blocked}
                                    onClick={() => void preview()}
                                >
                                    Atualizar prévia
                                </button>
                                <button
                                    type="button"
                                    className="primary"
                                    disabled={
                                        blocked ||
                                        !previa.podeConfirmar ||
                                        !referencia ||
                                        !/^[A-Za-z0-9][A-Za-z0-9._/-]*$/.test(
                                            referencia,
                                        )
                                    }
                                    onClick={() => void confirm()}
                                >
                                    Confirmar pedido de entrada
                                </button>
                            </div>
                        </>
                    )}
                </div>
            )}
        </Dialog>
    );
}
