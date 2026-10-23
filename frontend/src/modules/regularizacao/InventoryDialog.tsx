import { useEffect, useRef, useState, type ComponentProps } from "react";
import type { JourneyPage } from "../../components/JourneyPage";
import { Dialog } from "../../components/layout/Dialog";
import { Fields } from "../../components/Fields";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { StatusBadge } from "../../design-system/StatusBadge";
import { records } from "../../contracts/runtime";
import type {
    InventarioDto_Detalhe,
    InventarioDto_Leitura,
    InventarioDto_Registro,
    InventarioDto_Relatorio,
    InventarioDto_Resumo,
    TipoInventario,
} from "../../contracts/types";
import { inventoryModes } from "../../content/information";
import {
    inventoryDate,
    inventoryQuantity,
    inventoryRead,
    useInventoryWrite,
} from "./inventoryApi";
import { InventoryReport } from "./InventoryReport";

type Props = Pick<
    ComponentProps<typeof JourneyPage>,
    "transport" | "perfil" | "context" | "onReceipt"
> & {
    initial?: InventarioDto_Resumo;
    presetProduct?: string;
    onClose: () => void;
    onChanged: () => void;
};
export function InventoryDialog(props: Props) {
    const { transport, perfil, context } = props;
    const [summary, setSummary] = useState(props.initial);
    const [readings, setReadings] = useState<InventarioDto_Leitura[]>([]);
    const [report, setReport] = useState<InventarioDto_Relatorio>();
    const [revision, setRevision] = useState(0);
    const [loading, setLoading] = useState(false);
    const [readError, setReadError] = useState("");
    const [mode, setMode] = useState<TipoInventario>(
        props.presetProduct ? "SKU" : "CEGAS",
    );
    const [selection, setSelection] = useState<Record<string, unknown>>({
        produtoId: props.presetProduct ?? "",
        enderecoId: "",
    });
    const [reason, setReason] = useState("Conferência física de inventário");
    const [code, setCode] = useState("");
    const [quantity, setQuantity] = useState("");
    const [position, setPosition] = useState<Record<string, unknown>>({
        enderecoId: "",
    });
    const [unaddressed, setUnaddressed] = useState(false);
    const [replace, setReplace] = useState(false);
    const [confirmMissing, setConfirmMissing] = useState(false);
    const [finishReason, setFinishReason] = useState(
        "Conferência das mercadorias encerrada",
    );
    const [lastReading, setLastReading] = useState("");
    const codeField = useRef<HTMLInputElement>(null);
    const callbacks = useRef(props);
    callbacks.current = props;
    const writer = useInventoryWrite(transport, perfil, props.onReceipt);
    const id = summary?.id;
    function validSummary(value: InventarioDto_Resumo) {
        if (
            String(value.clienteId) !== String(context.clienteId) ||
            String(value.armazemId) !== String(context.armazemId) ||
            (id && String(value.id) !== String(id))
        )
            throw new Error("O inventário recebido pertence a outro contexto.");
        return value;
    }
    const validation = useRef(validSummary);
    validation.current = validSummary;
    useEffect(() => {
        if (!id) return;
        const abort = new AbortController();
        let current = true;
        setLoading(true);
        setReadError("");
        void (async () => {
            try {
                const detail = await inventoryRead(
                    transport,
                    perfil,
                    "InventarioController.consultar",
                    { id },
                    {},
                    abort.signal,
                );
                if (!current) return;
                const data = detail.data as InventarioDto_Detalhe;
                if (!data.inventario)
                    throw new Error("A consulta não retornou o inventário.");
                validation.current(data.inventario);
                setSummary(data.inventario);
                setReadings(data.leituras ?? []);
                callbacks.current.onReceipt(
                    detail.receipt,
                    detail.request.endpoint.response,
                    detail.request.endpoint.id,
                    detail.request,
                );
                if (data.inventario.situacao === "CONCLUIDO") {
                    const result = await inventoryRead(
                        transport,
                        perfil,
                        "InventarioController.relatorio",
                        { id },
                        {},
                        abort.signal,
                    );
                    if (!current) return;
                    if (!result.data.inventario)
                        throw new Error(
                            "O relatório não identificou o inventário.",
                        );
                    validation.current(result.data.inventario);
                    setReport(result.data);
                    callbacks.current.onReceipt(
                        result.receipt,
                        result.request.endpoint.response,
                        result.request.endpoint.id,
                        result.request,
                    );
                }
            } catch (error) {
                if (current)
                    setReadError(
                        error instanceof Error
                            ? error.message
                            : "Não foi possível consultar o inventário.",
                    );
            } finally {
                if (current) setLoading(false);
            }
        })();
        return () => {
            current = false;
            abort.abort();
        };
    }, [id, revision, transport, perfil]);
    const locked = writer.pending || writer.uncertain || loading;
    const open = summary?.situacao === "EM_ANDAMENTO";
    useEffect(() => {
        if (open && !loading && !writer.pending && !writer.uncertain)
            codeField.current?.focus({ preventScroll: true });
    }, [id, open, loading, writer.pending, writer.uncertain]);
    const title = !summary
        ? "Realizar inventário"
        : summary.situacao === "CONCLUIDO"
          ? "Relatório do inventário"
          : "Contagem do inventário";
    function close() {
        if (writer.pending) return;
        if (
            writer.uncertain &&
            !window.confirm(
                "A confirmação ainda não chegou. Ao sair, consulte os inventários realizados antes de repetir a operação. Sair mesmo assim?",
            )
        )
            return;
        props.onClose();
    }
    function start(event: React.FormEvent) {
        event.preventDefault();
        if (mode === "SKU" && !selection.produtoId) {
            setReadError("Selecione o SKU a inventariar.");
            return;
        }
        if (mode === "POSICAO" && !selection.enderecoId) {
            setReadError("Selecione a posição a inventariar.");
            return;
        }
        writer.run(
            "InventarioController.iniciar",
            {},
            {
                clienteId: context.clienteId,
                armazemId: context.armazemId,
                tipo: mode,
                produtoId: mode === "SKU" ? selection.produtoId : null,
                enderecoId: mode === "POSICAO" ? selection.enderecoId : null,
                motivo: reason,
            },
            (value) => {
                const created = validSummary(value as InventarioDto_Resumo);
                setSummary(created);
                props.onChanged();
            },
        );
    }
    function read(event: React.FormEvent) {
        event.preventDefault();
        if (!summary) return;
        if (
            summary.tipo !== "POSICAO" &&
            !unaddressed &&
            !position.enderecoId
        ) {
            setReadError(
                "Informe a posição encontrada ou marque Mercadoria sem endereço.",
            );
            return;
        }
        setReadError("");
        writer.run(
            "InventarioController.ler",
            { id: summary.id },
            {
                versao: summary.versao,
                codigoUnidade: code.trim(),
                contado: quantity.trim().replace(",", "."),
                enderecoId:
                    summary.tipo === "POSICAO"
                        ? summary.enderecoId
                        : unaddressed
                          ? null
                          : position.enderecoId,
                substituir: replace,
            },
            (value) => {
                const result = value as InventarioDto_Registro;
                if (!result.inventario)
                    throw new Error("A leitura não identificou o inventário.");
                setSummary(validSummary(result.inventario));
                setLastReading(
                    `Leitura registrada: ${result.leitura?.sku ?? "mercadoria"}, quantidade ${inventoryQuantity(result.leitura?.contado)}.`,
                );
                setCode("");
                setQuantity("");
                setReplace(false);
                setRevision((v) => v + 1);
                props.onChanged();
                requestAnimationFrame(() => codeField.current?.focus());
            },
        );
    }
    function finish(event: React.FormEvent) {
        event.preventDefault();
        if (!summary) return;
        writer.run(
            "InventarioController.finalizar",
            { id: summary.id },
            {
                versao: summary.versao,
                confirmarNaoLidas: confirmMissing,
                motivo: finishReason,
            },
            (value) => {
                const result = value as InventarioDto_Relatorio;
                if (!result.inventario)
                    throw new Error(
                        "O relatório não identificou o inventário.",
                    );
                setSummary(validSummary(result.inventario));
                setReport(result);
                props.onChanged();
            },
        );
    }
    return (
        <Dialog
            title={title}
            icon="contagem"
            wide
            className="inventory-dialog"
            onClose={close}
            locked={writer.pending}
        >
            <div className="inventory-dialog-content">
                <div className="inventory-scope">
                    <span>
                        <strong>Cliente: </strong>
                        <ReferenceValue
                            field="clienteId"
                            value={context.clienteId}
                        />
                    </span>
                    <span>
                        <strong>Armazém: </strong>
                        <ReferenceValue
                            field="armazemId"
                            value={context.armazemId}
                        />
                    </span>
                </div>
                {(writer.error || readError) && (
                    <div className="inventory-warning" role="alert">
                        <p>{writer.error || readError}</p>
                        {writer.uncertain ? (
                            <>
                                <p>
                                    A confirmação pode ter sido registrada.
                                    Reenvie a mesma solicitação para conferir,
                                    sem duplicar a leitura.
                                </p>
                                <button
                                    type="button"
                                    disabled={writer.pending}
                                    onClick={writer.retry}
                                >
                                    Conferir a mesma solicitação
                                </button>
                            </>
                        ) : (
                            summary && (
                                <button
                                    type="button"
                                    disabled={locked}
                                    onClick={() => setRevision((v) => v + 1)}
                                >
                                    Atualizar inventário
                                </button>
                            )
                        )}
                    </div>
                )}
                {loading && <p role="status">Consultando inventário…</p>}
                {writer.pending && (
                    <p role="status">Confirmando registro do inventário…</p>
                )}
                {!summary && (
                    <form onSubmit={start}>
                        <fieldset disabled={locked}>
                            <legend>Como deseja realizar o inventário?</legend>
                            <div className="inventory-mode-options">
                                {inventoryModes.map((item) => (
                                    <label
                                        key={item.id}
                                        className={
                                            mode === item.id
                                                ? "inventory-mode selected"
                                                : "inventory-mode"
                                        }
                                    >
                                        <input
                                            type="radio"
                                            name="tipoInventario"
                                            value={item.id}
                                            checked={mode === item.id}
                                            onChange={() => {
                                                setMode(item.id);
                                                setSelection({
                                                    produtoId: "",
                                                    enderecoId: "",
                                                });
                                            }}
                                        />
                                        <span>
                                            <strong>{item.title}</strong>
                                            <span>{item.description}</span>
                                        </span>
                                    </label>
                                ))}
                            </div>
                            {mode !== "CEGAS" && (
                                <Fields
                                    fields={records["InventarioDto.Iniciar"]
                                        .filter(
                                            (f) =>
                                                f.name ===
                                                (mode === "SKU"
                                                    ? "produtoId"
                                                    : "enderecoId"),
                                        )
                                        .map((f) => ({ ...f, required: true }))}
                                    values={selection}
                                    onChange={setSelection}
                                    perfil={perfil}
                                />
                            )}
                            <label className="inventory-full">
                                Observação do inventário
                                <input
                                    value={reason}
                                    onChange={(e) => setReason(e.target.value)}
                                    required
                                    minLength={5}
                                    maxLength={500}
                                />
                            </label>
                            <p className="hint inventory-full">
                                Use as etiquetas das mercadorias. Cada etiqueta
                                é contada uma vez; uma recontagem substitui a
                                leitura anterior.
                            </p>
                            <div className="inventory-full button-row">
                                <button type="submit" className="primary">
                                    Iniciar inventário
                                </button>
                            </div>
                        </fieldset>
                    </form>
                )}
                {summary && (
                    <>
                        <div className="inventory-heading">
                            <div>
                                <h3>Inventário #{String(summary.id)}</h3>
                                <p>
                                    {
                                        inventoryModes.find(
                                            (x) => x.id === summary.tipo,
                                        )?.title
                                    }{" "}
                                    · iniciado em{" "}
                                    {inventoryDate(summary.iniciadoEm)}
                                </p>
                            </div>
                            <StatusBadge value={summary.situacao} />
                        </div>
                        {summary.produtoId && (
                            <p>
                                <strong>SKU selecionado: </strong>
                                <ReferenceValue
                                    field="produtoId"
                                    value={summary.produtoId}
                                />
                            </p>
                        )}
                        {summary.enderecoId && (
                            <p>
                                <strong>Posição selecionada: </strong>
                                <ReferenceValue
                                    field="enderecoId"
                                    value={summary.enderecoId}
                                />
                            </p>
                        )}
                        {open && (
                            <>
                                <div className="inventory-reading-notice">
                                    <strong>
                                        {String(summary.unidadesLidas)}{" "}
                                        etiquetas lidas
                                    </strong>
                                    <p>
                                        Confira todas as mercadorias do escopo
                                        escolhido. As divergências serão
                                        apresentadas ao encerrar.
                                    </p>
                                </div>
                                <form
                                    onSubmit={read}
                                    aria-label="Leitura de mercadoria"
                                >
                                    <fieldset disabled={locked}>
                                        <legend>Registrar mercadoria</legend>
                                        <label className="inventory-full">
                                            Etiqueta da mercadoria
                                            <input
                                                ref={codeField}
                                                value={code}
                                                onChange={(e) =>
                                                    setCode(e.target.value)
                                                }
                                                required
                                                autoComplete="off"
                                                placeholder="Bipe ou informe a etiqueta"
                                            />
                                        </label>
                                        <label>
                                            Quantidade contada
                                            <input
                                                type="text"
                                                inputMode="decimal"
                                                value={quantity}
                                                onChange={(e) =>
                                                    setQuantity(e.target.value)
                                                }
                                                required
                                                placeholder="Informe a quantidade encontrada"
                                            />
                                        </label>
                                        {summary.tipo !== "POSICAO" && (
                                            <>
                                                <label className="inventory-checkbox">
                                                    <input
                                                        type="checkbox"
                                                        checked={unaddressed}
                                                        onChange={(e) =>
                                                            setUnaddressed(
                                                                e.target
                                                                    .checked,
                                                            )
                                                        }
                                                    />
                                                    Mercadoria sem endereço
                                                </label>
                                                {!unaddressed && (
                                                    <Fields
                                                        fields={records[
                                                            "InventarioDto.Ler"
                                                        ]
                                                            .filter(
                                                                (f) =>
                                                                    f.name ===
                                                                    "enderecoId",
                                                            )
                                                            .map((f) => ({
                                                                ...f,
                                                                required: true,
                                                            }))}
                                                        values={position}
                                                        onChange={setPosition}
                                                        perfil={perfil}
                                                    />
                                                )}
                                            </>
                                        )}
                                        <label className="inventory-checkbox inventory-full">
                                            <input
                                                type="checkbox"
                                                checked={replace}
                                                onChange={(e) =>
                                                    setReplace(e.target.checked)
                                                }
                                            />
                                            Recontar: substituir a leitura
                                            anterior desta etiqueta
                                        </label>
                                        <div className="inventory-full button-row">
                                            <button
                                                type="submit"
                                                className="primary"
                                            >
                                                Registrar leitura
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() =>
                                                    setRevision((v) => v + 1)
                                                }
                                            >
                                                Atualizar leituras
                                            </button>
                                        </div>
                                    </fieldset>
                                </form>
                                {lastReading && (
                                    <p
                                        role="status"
                                        className="inventory-reading-success"
                                    >
                                        {lastReading}
                                    </p>
                                )}
                                <section aria-label="Últimas leituras">
                                    <h3>Últimas leituras</h3>
                                    <p className="hint">
                                        Até 100 leituras mais recentes. O
                                        relatório final considera todas as
                                        leituras registradas.
                                    </p>
                                    {readings.length === 0 ? (
                                        <p>
                                            Nenhuma etiqueta registrada ainda.
                                        </p>
                                    ) : (
                                        <div className="inventory-table-scroll">
                                            <table className="inventory-table">
                                                <thead>
                                                    <tr>
                                                        <th>Mercadoria</th>
                                                        <th>
                                                            Quantidade informada
                                                        </th>
                                                        <th>
                                                            Posição informada
                                                        </th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    {readings.map((r) => (
                                                        <tr
                                                            key={
                                                                r.codigoUnidade
                                                            }
                                                        >
                                                            <td>
                                                                <strong>
                                                                    {r.sku} ·{" "}
                                                                    {
                                                                        r.descricao
                                                                    }
                                                                </strong>
                                                                <small>
                                                                    {
                                                                        r.codigoUnidade
                                                                    }
                                                                </small>
                                                            </td>
                                                            <td>
                                                                {inventoryQuantity(
                                                                    r.contado,
                                                                )}{" "}
                                                                {
                                                                    r.unidadeMedida
                                                                }
                                                            </td>
                                                            <td>
                                                                {r.posicao ||
                                                                    "Sem endereço"}
                                                            </td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    )}
                                </section>
                                <form
                                    onSubmit={finish}
                                    className="inventory-finish"
                                >
                                    <h3>Concluir a conferência</h3>
                                    <fieldset disabled={locked}>
                                        <label className="inventory-full">
                                            Observação de encerramento
                                            <input
                                                value={finishReason}
                                                onChange={(e) =>
                                                    setFinishReason(
                                                        e.target.value,
                                                    )
                                                }
                                                minLength={5}
                                                maxLength={500}
                                                required
                                            />
                                        </label>
                                        <label className="inventory-checkbox inventory-full">
                                            <input
                                                type="checkbox"
                                                checked={confirmMissing}
                                                onChange={(e) =>
                                                    setConfirmMissing(
                                                        e.target.checked,
                                                    )
                                                }
                                                required
                                            />
                                            Concluí a conferência. Mercadorias
                                            não lidas serão consideradas
                                            faltantes no relatório.
                                        </label>
                                        <button
                                            type="submit"
                                            className="primary"
                                            disabled={!confirmMissing}
                                        >
                                            Encerrar e gerar relatório
                                        </button>
                                    </fieldset>
                                </form>
                                {perfil !== "OPERACAO" && (
                                    <details className="inventory-admin">
                                        <summary>
                                            Cancelar este inventário
                                        </summary>
                                        <p>
                                            As leituras ficam no histórico e
                                            nenhum ajuste de estoque é
                                            realizado.
                                        </p>
                                        <label>
                                            Motivo do cancelamento
                                            <input
                                                value={finishReason}
                                                onChange={(e) =>
                                                    setFinishReason(
                                                        e.target.value,
                                                    )
                                                }
                                                minLength={5}
                                                maxLength={500}
                                                disabled={locked}
                                            />
                                        </label>
                                        <button
                                            type="button"
                                            disabled={
                                                locked ||
                                                finishReason.trim().length < 5
                                            }
                                            onClick={() =>
                                                writer.run(
                                                    "InventarioController.cancelar",
                                                    { id: summary.id },
                                                    {
                                                        versao: summary.versao,
                                                        motivo: finishReason,
                                                        confirmarNaoLidas: false,
                                                    },
                                                    (value) => {
                                                        setSummary(
                                                            validSummary(
                                                                value as InventarioDto_Resumo,
                                                            ),
                                                        );
                                                        props.onChanged();
                                                    },
                                                )
                                            }
                                        >
                                            Confirmar cancelamento
                                        </button>
                                    </details>
                                )}
                            </>
                        )}
                        {!open && report && <InventoryReport report={report} />}
                        {summary.situacao === "CANCELADO" && (
                            <p>
                                Inventário cancelado. As leituras foram
                                preservadas no histórico.
                            </p>
                        )}
                    </>
                )}
            </div>
        </Dialog>
    );
}
