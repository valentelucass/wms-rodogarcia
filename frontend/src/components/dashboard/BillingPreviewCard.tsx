import { useEffect, useState } from "react";
import type { VisaoOperacaoDto_PrevisaoFaturamento } from "../../contracts/types";
import { ApiError, call, type Transport } from "../../api/client";
import type { Perfil, Values } from "../../contracts/runtime";
import { information } from "../../content/information";
import { useReferenceCatalog } from "../context/ReferenceCatalog";
import { InformationHint } from "../information/InformationPopup";
import { Dialog } from "../layout/Dialog";
import { overviewMoney } from "./overviewFormatting";

type Preview = VisaoOperacaoDto_PrevisaoFaturamento;

function previewMoney(value: string | null, currency: string | null) {
    if (value == null) return "—";
    const formatted = overviewMoney(value);
    return currency === "BRL"
        ? formatted
        : `${currency ? currency + " " : ""}${formatted.slice(3)}`;
}

function date(value: string | null) {
    if (value == null) return "—";
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
    return match ? `${match[3]}/${match[2]}/${match[1]}` : value;
}

function moment(value: string, timeZone: string | null) {
    if (!timeZone) return value;
    try {
        return new Date(value).toLocaleString("pt-BR", { timeZone });
    } catch {
        return value;
    }
}

export function BillingPreviewCard({
    transport,
    context,
    perfil,
    revision,
}: {
    transport: Transport;
    context: Values;
    perfil: Perfil;
    revision: number;
}) {
    const [detailsOpen, setDetailsOpen] = useState(false);
    const [result, setResult] = useState<{
        key: string;
        preview?: Preview | null;
        error?: string;
    }>({ key: "" });
    const permitted = perfil === "SUPERVISOR" || perfil === "GESTOR";
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    const key = `${revision}/${context.clienteId ?? ""}/${context.armazemId ?? ""}/${perfil}/${timeZone}`;
    const loading = permitted && result.key !== key;
    const error = result.key === key ? result.error : undefined;
    useEffect(() => {
        if (!permitted) return;
        const abort = new AbortController();
        let active = true;
        setDetailsOpen(false);
        void call(
            transport,
            "VisaoOperacaoController.preverFaturamento",
            undefined,
            {},
            {
                clienteId: context.clienteId,
                armazemId: context.armazemId,
                fuso: timeZone,
            },
            abort.signal,
        )
            .then((preview) => {
                if (active) setResult({ key, preview });
            })
            .catch((error: unknown) => {
                if (active)
                    setResult({
                        key,
                        error:
                            error instanceof ApiError && error.status === 403
                                ? "Seu acesso não permite consultar a previsão deste contexto."
                                : "Não foi possível consultar a previsão de faturamento. Use Atualizar visão para tentar novamente.",
                    });
            });
        return () => {
            active = false;
            abort.abort();
        };
    }, [
        transport,
        permitted,
        context.clienteId,
        context.armazemId,
        timeZone,
        key,
    ]);
    const preview = permitted && !loading && !error ? result.preview : null;
    const pending = preview?.pendencias?.length ?? 0;
    const note = loading
        ? "Consultando prévia…"
        : error
          ? "Prévia indisponível"
          : !permitted
            ? "Disponível para Supervisor e Gestor"
            : !preview
              ? "Prévia indisponível"
              : preview.completo
                ? `Prévia automática · ${preview.competencia}`
                : `Prévia incompleta · ${pending ? `${pending} ${pending === 1 ? "pendência" : "pendências"}` : "consulte os períodos"}`;
    return (
        <>
            <article
                className="overview-metric overview-metric--9 overview-billing"
                aria-label={information.billing.title}
                aria-busy={loading}
            >
                <div>
                    <span>{information.billing.title}</span>
                    <InformationHint topic="billing" />
                </div>
                <strong>
                    {preview?.completo
                        ? previewMoney(preview.total, preview.moeda)
                        : "—"}
                </strong>
                <small>{note}</small>
                {error && <small role="alert">{error}</small>}
                {preview && !preview.completo && (
                    <>
                        <small>
                            Competência dos ciclos: {preview.competencia}
                        </small>
                        {preview.conhecido != null && (
                            <small>
                                Valor conhecido:{" "}
                                {previewMoney(preview.conhecido, preview.moeda)}
                            </small>
                        )}
                    </>
                )}
                {preview && (
                    <button type="button" onClick={() => setDetailsOpen(true)}>
                        Ver períodos e pendências
                    </button>
                )}
            </article>
            {detailsOpen && preview && (
                <Dialog
                    title="Previsão de faturamento"
                    onClose={() => setDetailsOpen(false)}
                    icon="fechamento"
                    className="billing-preview-dialog"
                    wide
                >
                    <PreviewDetails preview={preview} timeZone={timeZone} />
                </Dialog>
            )}
        </>
    );
}

function PreviewDetails({
    preview,
    timeZone,
}: {
    preview: Preview;
    timeZone: string;
}) {
    const catalog = useReferenceCatalog();
    const client = (id: string | null) =>
        id == null
            ? "Cliente não informado"
            : (catalog?.clientes.find((value) => value.id === id)?.nome ??
              `Cliente ${id}`);
    const warehouse = (id: string | null) =>
        id == null
            ? "Armazém não informado"
            : (catalog?.armazens.find((value) => value.id === id)?.nome ??
              `Armazém ${id}`);
    return (
        <div className="billing-preview-details">
            <p className="muted">{information.billing.footer}</p>
            <dl className="record">
                <div>
                    <dt>Competência dos ciclos</dt>
                    <dd>{preview.competencia}</dd>
                </div>
                <div>
                    <dt>Referência da prévia</dt>
                    <dd>
                        {moment(preview.referenciaEm, timeZone)} · {timeZone}
                    </dd>
                </div>
                <div>
                    <dt>Situação</dt>
                    <dd>
                        {preview.completo
                            ? "Prévia completa até a referência"
                            : "Prévia incompleta"}
                    </dd>
                </div>
                <div>
                    <dt>Total previsto</dt>
                    <dd>
                        {previewMoney(
                            preview.completo ? preview.total : null,
                            preview.moeda,
                        )}
                    </dd>
                </div>
                {!preview.completo && (
                    <div>
                        <dt>Valor conhecido (parcial)</dt>
                        <dd>
                            {previewMoney(preview.conhecido, preview.moeda)}
                        </dd>
                    </div>
                )}
                {!preview.moeda && (
                    <div>
                        <dt>Moeda do agregado</dt>
                        <dd>{preview.moeda ?? "Sem moeda única informada"}</dd>
                    </div>
                )}
            </dl>
            <p className="muted">
                A competência segue o último dia de cada ciclo contratado.
            </p>
            <section aria-label="Períodos da previsão">
                <h3>Períodos</h3>
                {preview.periodos?.length ? (
                    preview.periodos.map((period, index) => (
                        <section
                            className="result-section"
                            key={`${period.clienteId}/${period.armazemId}/${period.contratoId}/${period.cicloInicio}/${index}`}
                        >
                            <h4>Período {index + 1}</h4>
                            <p>
                                {client(period.clienteId)} ·{" "}
                                {warehouse(period.armazemId)}
                            </p>
                            <dl className="record">
                                <div>
                                    <dt>Ciclo contratado</dt>
                                    <dd>
                                        {date(period.cicloInicio)} a{" "}
                                        {date(period.cicloFimExclusivo)}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Período apurado</dt>
                                    <dd>
                                        {date(period.apuradoInicio)} a{" "}
                                        {date(period.apuradoFimExclusivo)}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Referência no fuso do contrato</dt>
                                    <dd>
                                        {moment(
                                            preview.referenciaEm,
                                            period.fuso,
                                        )}{" "}
                                        · {period.fuso ?? "Fuso não informado"}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Subtotal conhecido</dt>
                                    <dd>
                                        {previewMoney(
                                            period.subtotalConhecido,
                                            period.moeda,
                                        )}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Mínimo calculado</dt>
                                    <dd>
                                        {previewMoney(
                                            period.minimoCalculado,
                                            period.moeda,
                                        )}
                                    </dd>
                                </div>
                                <div>
                                    <dt>GRIS calculado</dt>
                                    <dd>
                                        {previewMoney(
                                            period.grisCalculado,
                                            period.moeda,
                                        )}
                                    </dd>
                                </div>
                                <div>
                                    <dt>Total previsto do período</dt>
                                    <dd>
                                        {previewMoney(
                                            period.total,
                                            period.moeda,
                                        )}
                                    </dd>
                                </div>
                            </dl>
                            <p className="muted">
                                Dia final não incluído nos intervalos acima.{" "}
                                {period.incluiDiaAtual
                                    ? "Inclui as operações de hoje até a referência."
                                    : "Operações do dia da referência não incluídas."}
                            </p>
                            {!!period.pendencias?.length && (
                                <p>
                                    {period.pendencias.length}{" "}
                                    {period.pendencias.length === 1
                                        ? "pendência neste período"
                                        : "pendências neste período"}
                                    .
                                </p>
                            )}
                            <details>
                                <summary>Referências do período</summary>
                                <dl className="record">
                                    <div>
                                        <dt>Cliente</dt>
                                        <dd>{period.clienteId}</dd>
                                    </div>
                                    <div>
                                        <dt>Armazém</dt>
                                        <dd>{period.armazemId}</dd>
                                    </div>
                                    <div>
                                        <dt>Contrato</dt>
                                        <dd>{period.contratoId}</dd>
                                    </div>
                                </dl>
                            </details>
                        </section>
                    ))
                ) : (
                    <p className="muted">
                        Nenhum período informado na consulta.
                    </p>
                )}
            </section>
            <section aria-label="Pendências da previsão">
                <h3>Pendências</h3>
                {preview.pendencias?.length ? (
                    <ol className="billing-preview-pendencies">
                        {preview.pendencias.map((pending, index) => (
                            <li
                                key={`${pending.clienteId}/${pending.armazemId}/${pending.codigo}/${index}`}
                            >
                                <p>
                                    {pending.detalhe ??
                                        "Pendência sem descrição na consulta."}
                                </p>
                                <p className="muted">
                                    {client(pending.clienteId)} ·{" "}
                                    {warehouse(pending.armazemId)}
                                    {pending.data
                                        ? ` · ${date(pending.data)}`
                                        : ""}
                                </p>
                                <details>
                                    <summary>Referências da pendência</summary>
                                    <dl className="record">
                                        <div>
                                            <dt>Código</dt>
                                            <dd>{pending.codigo}</dd>
                                        </div>
                                        <div>
                                            <dt>Cliente</dt>
                                            <dd>{pending.clienteId}</dd>
                                        </div>
                                        <div>
                                            <dt>Armazém</dt>
                                            <dd>{pending.armazemId}</dd>
                                        </div>
                                        {pending.unidadeId && (
                                            <div>
                                                <dt>Unidade</dt>
                                                <dd>{pending.unidadeId}</dd>
                                            </div>
                                        )}
                                        {pending.fatoId && (
                                            <div>
                                                <dt>Fato de serviço</dt>
                                                <dd>{pending.fatoId}</dd>
                                            </div>
                                        )}
                                    </dl>
                                </details>
                            </li>
                        ))}
                    </ol>
                ) : (
                    <p className="muted">
                        Nenhuma pendência informada até a referência.
                    </p>
                )}
            </section>
        </div>
    );
}
