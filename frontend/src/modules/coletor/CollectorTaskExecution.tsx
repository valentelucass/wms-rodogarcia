import { useEffect, useRef, useState } from "react";
import type {
    EstoqueDto_Unidade,
    TarefaColetorDto_Detalhe,
    TarefaColetorDto_Unidade,
    TarefaColetorDto_Destinos,
} from "../../contracts/types";
import {
    inventoryQuantity,
    inventoryRead,
    useInventoryWrite,
} from "../regularizacao/inventoryApi";
import type { CollectorTaskProps } from "./CollectorTasks";

export function CollectorTaskExecution(
    props: CollectorTaskProps & {
        id: string;
        detail: TarefaColetorDto_Detalhe;
        writer: ReturnType<typeof useInventoryWrite>;
        onConfirmed: (data: unknown) => void;
    },
) {
    const task = props.detail.tarefa!,
        separating = task.tipo === "SEPARACAO";
    const separated = new Set(
        props.detail.separacoes
            ?.filter((s) => s.situacao === "SEPARADA")
            .map((s) => s.reservaId),
    );
    const reservations = (props.detail.pedido?.reservas ?? []).filter(
        (r) => r.situacao === "ATIVA" && !separated.has(r.id),
    );
    const [reservation, setReservation] = useState(reservations[0]?.id ?? "");
    const selected = reservations.find((r) => r.id === reservation),
        expected = separating ? selected?.codigoUnidade : task.codigoUnidade;
    const [unit, setUnit] = useState<TarefaColetorDto_Unidade>();
    const stock: EstoqueDto_Unidade | undefined | null = separating
        ? unit?.estoque
        : props.detail.unidade;
    const [code, setCode] = useState(""),
        [position, setPosition] = useState(""),
        [second, setSecond] = useState("");
    const [destinations, setDestinations] =
        useState<TarefaColetorDto_Destinos>();
    const [error, setError] = useState(""),
        [loading, setLoading] = useState(false);
    const [measures, setMeasures] = useState({
        pesoKg: "",
        alturaMetros: "",
        larguraMetros: "",
        profundidadeMetros: "",
        empilhamento: "1",
        posicoesNecessarias: "1",
    });
    const codeField = useRef<HTMLInputElement>(null),
        positionField = useRef<HTMLInputElement>(null);
    const generation = useRef(0),
        lookup = useRef<AbortController | null>(null);
    useEffect(
        () => () => {
            generation.current++;
            lookup.current?.abort();
        },
        [],
    );
    useEffect(() => {
        if (!separating || !expected) return;
        const abort = new AbortController();
        let current = true;
        setUnit(undefined);
        setLoading(true);
        setError("");
        void inventoryRead(
            props.transport,
            props.perfil,
            "TarefaColetorController.unidade",
            { id: props.id },
            { codigo: expected },
            abort.signal,
        )
            .then((r) => {
                if (!current) return;
                if (
                    r.data.estoque?.unidade?.codigo !== expected ||
                    String(r.data.estoque.unidade.armazemId) !==
                        String(task.armazemId)
                )
                    throw new Error(
                        "A mercadoria retornada não pertence a esta tarefa.",
                    );
                setUnit(r.data);
            })
            .catch((e) => {
                if (current)
                    setError(
                        e instanceof Error
                            ? e.message
                            : "Não foi possível consultar a mercadoria.",
                    );
            })
            .finally(() => {
                if (current) setLoading(false);
            });
        return () => {
            current = false;
            abort.abort();
        };
    }, [
        props.transport,
        props.perfil,
        props.id,
        separating,
        expected,
        task.armazemId,
    ]);
    useEffect(() => {
        codeField.current?.focus({ preventScroll: true });
    }, [expected]);
    const locked = props.writer.pending || props.writer.uncertain || loading;
    const two =
        String(
            stock?.medidas?.posicoesNecessarias ?? measures.posicoesNecessarias,
        ) === "2";
    function invalidate() {
        generation.current++;
        lookup.current?.abort();
        setDestinations(undefined);
        setError("");
        setLoading(false);
    }
    async function check(event: React.FormEvent) {
        event.preventDefault();
        invalidate();
        if (
            !code.trim() ||
            code.trim().toLowerCase() !== expected?.toLowerCase()
        ) {
            setError("Leia a etiqueta da mercadoria indicada nesta tarefa.");
            codeField.current?.focus();
            return;
        }
        const current = ++generation.current,
            abort = new AbortController();
        lookup.current = abort;
        setLoading(true);
        try {
            const r = await inventoryRead(
                props.transport,
                props.perfil,
                "TarefaColetorController.destinos",
                { id: props.id },
                {
                    codigo: position.trim(),
                    segundoCodigo: two ? second.trim() : undefined,
                },
                abort.signal,
            );
            if (current !== generation.current) return;
            if (
                !r.data.destinos?.length ||
                r.data.destinos.length !== (two ? 2 : 1)
            )
                throw new Error("Confira todas as posições necessárias.");
            setDestinations(r.data);
        } catch (e) {
            if (current === generation.current)
                setError(
                    e instanceof Error
                        ? e.message
                        : "Não foi possível conferir o destino.",
                );
        } finally {
            if (current === generation.current) setLoading(false);
        }
    }
    function confirm() {
        if (!destinations?.destinos || !stock?.unidade || locked) return;
        const op = crypto.randomUUID(),
            motivo = separating
                ? "Separação confirmada pela tarefa do coletor"
                : "Endereçamento confirmado pela tarefa do coletor";
        if (separating) {
            if (!selected || !props.detail.pedido || !unit) return;
            props.writer.run(
                "TarefaColetorController.separar",
                { id: props.id },
                {
                    operacaoId: op,
                    leitura: {
                        operacaoId: op,
                        versao: props.detail.pedido.versao,
                        reservaId: selected.id,
                        codigoLido: code.trim(),
                        revisaoConteudo: unit.revisaoConteudo,
                        motivo,
                        resolverPendentes: false,
                    },
                    destinacao: {
                        reservaId: selected.id,
                        conjuntoId: destinations.conjuntoId,
                        destinos: destinations.destinos,
                    },
                },
                props.onConfirmed,
            );
        } else {
            props.writer.run(
                "TarefaColetorController.enderecar",
                { id: props.id },
                {
                    operacaoId: op,
                    codigoLido: code.trim(),
                    posicionar: {
                        operacaoId: op,
                        versaoUnidade: stock.unidade.versao,
                        medidas: stock.medidas
                            ? null
                            : {
                                  ...measures,
                                  empilhamento: Number(measures.empilhamento),
                                  posicoesNecessarias: Number(
                                      measures.posicoesNecessarias,
                                  ),
                              },
                        conjuntoId: destinations.conjuntoId,
                        destinos: destinations.destinos,
                        motivo,
                    },
                },
                props.onConfirmed,
            );
        }
    }
    const item = props.detail.pedido?.itens?.find(
        (i) => i.id === selected?.itemId,
    );
    const measureLabels: Record<keyof typeof measures, string> = {
        pesoKg: "Peso (kg)",
        alturaMetros: "Altura (m)",
        larguraMetros: "Largura (m)",
        profundidadeMetros: "Profundidade (m)",
        empilhamento: "Empilhamento",
        posicoesNecessarias: "Posições necessárias",
    };
    return (
        <section className="collector-task-execution">
            {separating && (
                <label>
                    Mercadoria a separar
                    <select
                        value={reservation}
                        disabled={locked}
                        onChange={(e) => {
                            invalidate();
                            setReservation(e.target.value);
                            setCode("");
                            setPosition("");
                            setSecond("");
                        }}
                    >
                        {reservations.map((r, i) => (
                            <option key={r.id} value={r.id ?? ""}>
                                Mercadoria {i + 1} ·{" "}
                                {inventoryQuantity(r.quantidade)} ·{" "}
                                {
                                    props.detail.pedido?.itens?.find(
                                        (v) => v.id === r.itemId,
                                    )?.sku
                                }
                            </option>
                        ))}
                    </select>
                </label>
            )}
            {stock?.unidade && (
                <div className="collector-task-instruction">
                    <strong>
                        {stock.unidade.sku}
                        {item
                            ? ` · Separar ${inventoryQuantity(selected?.quantidade)} ${item.unidadeMedida ?? ""}`
                            : ""}
                    </strong>
                    <p>
                        {separating
                            ? `Buscar em: ${(stock.posicoes ?? []).map((p) => p.codigo).join(" + ") || "Posição indisponível"}`
                            : `Mercadoria: ${inventoryQuantity(stock.unidade.quantidade)} ${stock.unidade.unidadeMedida ?? ""}`}
                    </p>
                    <p>
                        Etiqueta:{" "}
                        <span className="collector-task-code">{expected}</span>
                    </p>
                </div>
            )}
            <form onSubmit={check}>
                <label>
                    1. Leia a etiqueta da mercadoria
                    <input
                        ref={codeField}
                        value={code}
                        disabled={locked}
                        autoComplete="off"
                        autoCapitalize="off"
                        placeholder="Bipe a etiqueta"
                        onChange={(e) => {
                            invalidate();
                            setCode(e.target.value.trim());
                        }}
                        onKeyDown={(e) => {
                            if (e.key === "Enter") {
                                e.preventDefault();
                                if (
                                    e.currentTarget.value
                                        .trim()
                                        .toLowerCase() ===
                                    expected?.toLowerCase()
                                )
                                    positionField.current?.focus();
                                else
                                    setError(
                                        "Leia a etiqueta da mercadoria indicada nesta tarefa.",
                                    );
                            }
                        }}
                    />
                </label>
                {!separating && !stock?.medidas && (
                    <fieldset
                        className="collector-task-measures"
                        disabled={locked}
                    >
                        <legend>Medidas da mercadoria</legend>
                        <p>Informe as medidas no primeiro endereçamento.</p>
                        {(
                            Object.keys(measures) as (keyof typeof measures)[]
                        ).map((key) => (
                            <label key={key}>
                                {measureLabels[key]}
                                {key === "posicoesNecessarias" ? (
                                    <select
                                        value={measures[key]}
                                        onChange={(e) => {
                                            invalidate();
                                            setMeasures((v) => ({
                                                ...v,
                                                [key]: e.target.value,
                                            }));
                                        }}
                                    >
                                        <option value="1">Uma posição</option>
                                        <option value="2">Duas posições</option>
                                    </select>
                                ) : (
                                    <input
                                        inputMode="decimal"
                                        value={measures[key]}
                                        required
                                        onChange={(e) => {
                                            invalidate();
                                            setMeasures((v) => ({
                                                ...v,
                                                [key]: e.target.value.replace(
                                                    ",",
                                                    ".",
                                                ),
                                            }));
                                        }}
                                    />
                                )}
                            </label>
                        ))}
                    </fieldset>
                )}
                <label>
                    2. Leia a posição de destino
                    <input
                        ref={positionField}
                        value={position}
                        disabled={locked}
                        required
                        autoComplete="off"
                        placeholder={
                            separating
                                ? "Posição da área de separação"
                                : "Código da posição"
                        }
                        onChange={(e) => {
                            invalidate();
                            setPosition(e.target.value.trim());
                        }}
                    />
                </label>
                {two && (
                    <label>
                        Leia a segunda posição
                        <input
                            value={second}
                            disabled={locked}
                            required
                            autoComplete="off"
                            placeholder="Segunda posição do conjunto"
                            onChange={(e) => {
                                invalidate();
                                setSecond(e.target.value.trim());
                            }}
                        />
                    </label>
                )}
                <button disabled={locked || !stock?.unidade} type="submit">
                    {loading ? "Conferindo…" : "Conferir destino"}
                </button>
            </form>
            {error && <p role="alert">{error}</p>}
            {destinations && (
                <div className="collector-task-confirm">
                    <h3>
                        3. Confirmar{" "}
                        {separating ? "separação" : "endereçamento"}
                    </h3>
                    <p>
                        Destino:{" "}
                        {destinations.destinos
                            ?.map((d) => d.codigoLido)
                            .join(" + ")}
                    </p>
                    <p>
                        O sistema confere capacidade e disponibilidade ao
                        confirmar.
                    </p>
                    <button
                        className="primary"
                        disabled={locked}
                        onClick={confirm}
                    >
                        {props.writer.pending
                            ? "Confirmando…"
                            : separating
                              ? "Confirmar separação"
                              : "Confirmar endereçamento"}
                    </button>
                </div>
            )}
        </section>
    );
}
