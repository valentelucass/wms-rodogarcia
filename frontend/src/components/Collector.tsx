import type { Request } from "../api/client";
import { useCollectorReading } from "../hooks/useCollectorReading";
import { isObject, type Values, type Perfil } from "../contracts/runtime";
import type { Receipt, Transport } from "../api/client";
import { Result } from "./Result";
import { Operation } from "./Operation";
import { useLayoutEffect } from "react";
import { PageHeader } from "./layout/PageHeader";
import { Icon } from "../design-system/Icon";
export function Collector({
    transport,
    context,
    perfil,
    onReceipt,
    taskContexts = {},
}: {
    transport: Transport;
    context: Values;
    perfil: Perfil;
    onReceipt?: (
        r: Receipt,
        type: string,
        id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => void;
    taskContexts?: Record<string, Values>;
}) {
    const {
        code,
        setCode,
        position,
        setPosition,
        unit,
        addresses,
        target,
        setTarget,
        error,
        busy,
        page,
        setPage,
        mode,
        setMode,
        posRef,
        captured,
        invalidate,
        load,
        match,
    } = useCollectorReading(transport, context, perfil);
    useLayoutEffect(() => {
        if (unit && captured.current === code && mode === "posicionar")
            posRef.current?.focus();
    }, [unit, code, mode, captured, posRef]);
    const selected =
        unit && isObject(unit.unidade)
            ? {
                  codigo: unit.unidade.codigo,
                  unidadeId: unit.unidade.id,
                  versaoUnidade: unit.unidade.versao,
              }
            : {};
    return (
        <div className="collector">
            <PageHeader
                title="Coletor"
                icon="coletor"
                description="Leitura por teclado/scanner web. Confirmações aguardam resposta; equipamento real ainda não validado. Interromper espera não comprova desfazer uma operação."
            />
            <div className="collector-workspace">
                <section
                    className="workspace-panel collector-task"
                    aria-label="Tarefa e leitura da unidade"
                >
                    <div className="collector-task-picker">
                        <span
                            className="collector-task-icon"
                            aria-hidden="true"
                        >
                            <Icon name="coletor" />
                        </span>
                        <h2>Operação</h2>
                        <label>
                            Tarefa
                            <select
                                value={mode}
                                onChange={(e) => {
                                    invalidate();
                                    setMode(e.target.value);
                                }}
                            >
                                <option value="posicionar">
                                    Endereçar / movimentar
                                </option>
                                <option value="ler">
                                    Conferir reserva para separação
                                </option>
                                <option value="separar">Separar reserva</option>
                                <option value="contar">Contar unidade</option>
                            </select>
                        </label>
                        <p>A leitura começa pela etiqueta da unidade.</p>
                    </div>
                    <div className="collector-reading">
                        <h2>Identificar unidade</h2>
                        {Boolean(context.enderecoCodigo) && (
                            <p>
                                Destino selecionado:{" "}
                                <strong>
                                    {String(context.enderecoCodigo)}
                                </strong>
                            </p>
                        )}
                        <form
                            onSubmit={(e) => {
                                e.preventDefault();
                                void load();
                            }}
                        >
                            <label>
                                1. Leia o UUID da unidade
                                <input
                                    placeholder="UUID da etiqueta"
                                    value={code}
                                    onChange={(e) => {
                                        invalidate();
                                        setCode(e.target.value.trim());
                                    }}
                                    autoComplete="off"
                                    autoCapitalize="off"
                                />
                            </label>
                            <label>
                                Página das posições
                                <input
                                    type="number"
                                    min="0"
                                    value={page}
                                    onChange={(e) => {
                                        invalidate();
                                        setPage(Number(e.target.value));
                                    }}
                                />
                            </label>
                            <button className="primary" disabled={busy}>
                                {busy
                                    ? "Consultando…"
                                    : "Consultar unidade e posições"}
                            </button>
                        </form>
                        {error && <p role="alert">{error}</p>}
                    </div>
                </section>
                {unit && captured.current === code && (
                    <>
                        <details className="collector-unit workspace-panel">
                            <summary>
                                Identidade, condição e localização consultadas
                            </summary>
                            <Result data={unit} type="EstoqueDto.Unidade" />
                        </details>
                        {mode === "posicionar" ? (
                            <>
                                <form
                                    className="workspace-panel collector-destination"
                                    onSubmit={(e) => {
                                        e.preventDefault();
                                        match();
                                    }}
                                >
                                    <label>
                                        2. Leia o código da posição
                                        <input
                                            ref={posRef}
                                            value={position}
                                            onChange={(e) => {
                                                setPosition(
                                                    e.target.value.trim(),
                                                );
                                                setTarget(undefined);
                                            }}
                                            autoComplete="off"
                                        />
                                    </label>
                                    <button>Conferir destino</button>
                                </form>
                                <p>
                                    Posições na página:{" "}
                                    {addresses
                                        .map((a) => String(a.codigo))
                                        .join(", ")}
                                    . Capacidades e ocupação serão revalidadas
                                    no backend.
                                </p>
                                {target && (
                                    <>
                                        <p>
                                            Destino lido:{" "}
                                            {String(target.codigo)} ·{" "}
                                            {String(target.tipo)}. Para duas
                                            posições, informe também o conjunto
                                            e o segundo destino no comando.
                                        </p>
                                        <div className="workspace-panel collector-command">
                                            <Operation
                                                key={code + position}
                                                id="EstoqueController.posicionar"
                                                collector
                                                transport={transport}
                                                context={{
                                                    ...context,
                                                    ...selected,
                                                    codigo: code,
                                                    destinos: [
                                                        {
                                                            enderecoId:
                                                                target.id,
                                                            codigoLido:
                                                                position,
                                                        },
                                                    ],
                                                    medidas: unit.medidas,
                                                }}
                                                perfil={perfil}
                                                onSelect={() => {
                                                    invalidate();
                                                    setCode("");
                                                    setPosition("");
                                                }}
                                                onReceipt={onReceipt}
                                            />
                                        </div>
                                    </>
                                )}
                            </>
                        ) : (
                            <div className="workspace-panel collector-command">
                                <Operation
                                    key={mode + code}
                                    collector
                                    id={
                                        mode === "ler"
                                            ? "ExpedicaoController.ler"
                                            : mode === "separar"
                                              ? "ExpedicaoController.separar"
                                              : "ContagemController.contar"
                                    }
                                    transport={transport}
                                    context={{
                                        ...context,
                                        ...taskContexts[mode],
                                        ...selected,
                                        codigoLido: code,
                                        codigoUnidade: code,
                                    }}
                                    perfil={perfil}
                                    onSelect={() => {
                                        invalidate();
                                        setCode("");
                                    }}
                                    onReceipt={onReceipt}
                                />
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}
