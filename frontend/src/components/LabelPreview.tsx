import { useEffect, useMemo, useState, type ReactNode } from "react";
import QRCode from "qrcode";
import JsBarcode from "jsbarcode";
import { isObject, type Values } from "../contracts/runtime";
import { display } from "../domain/labels";
import { formatProductQuantity } from "../domain/quantityPresentation";
import { information } from "../content/information";
import { Icon } from "../design-system/Icon";

export function LabelPreview({
    data,
    ficticio = false,
    pending = false,
    actions,
    details,
}: {
    data: unknown;
    ficticio?: boolean;
    pending?: boolean;
    actions?: ReactNode;
    details?: ReactNode;
}) {
    if (!isObject(data)) return null;
    return (
        <UnitLabel
            key={String(data.codigoLeitura)}
            data={data}
            ficticio={ficticio}
            pending={pending}
            actions={actions}
            details={details}
        />
    );
}

function UnitLabel({
    data,
    ficticio,
    pending,
    actions,
    details,
}: {
    data: Values;
    ficticio: boolean;
    pending: boolean;
    actions?: ReactNode;
    details?: ReactNode;
}) {
    const [qr, setQr] = useState("");
    const [qrError, setQrError] = useState(false);
    const code = String(data.codigoLeitura ?? "");
    const validCode =
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
            code,
        );
    const bars = useMemo(() => {
        if (!validCode) return "";
        const encoded: { encodings?: { data: string }[] } = {};
        JsBarcode(encoded, code, { format: "CODE128", displayValue: false });
        return encoded.encodings?.map((part) => part.data).join("") ?? "";
    }, [code, validCode]);
    useEffect(() => {
        let active = true;
        if (validCode)
            void QRCode.toDataURL(code, {
                width: 360,
                margin: 4,
                errorCorrectionLevel: "M",
            })
                .then((url) => {
                    if (active) setQr(url);
                })
                .catch(() => {
                    if (active) setQrError(true);
                });
        return () => {
            active = false;
        };
    }, [code, validCode]);
    const date = data.chegadaReal ? new Date(String(data.chegadaReal)) : null;
    const entryDate =
        date && !Number.isNaN(date.getTime())
            ? new Intl.DateTimeFormat("pt-BR", {
                  timeZone: "America/Sao_Paulo",
              }).format(date)
            : "Não informada";
    const notes = Array.isArray(data.notas) ? data.notas.filter(isObject) : [];
    const ready = validCode && Boolean(qr) && Boolean(bars);
    return (
        <section
            className="label-preview-container"
            aria-label={
                ficticio ? "Prévia de etiqueta fictícia" : "Prévia da etiqueta"
            }
        >
            <div className="label-preview-layout">
                <div className="label-preview-stage">
                    <div className="no-print label-preview-caption">
                        <strong>Prévia de impressão</strong>
                        <span>
                            {ficticio
                                ? "Exercício fictício"
                                : "Etiqueta da unidade"}
                        </span>
                    </div>
                    <div className="label-preview">
                        {ficticio && (
                            <div className="label-exercise">
                                EXERCÍCIO FICTÍCIO
                            </div>
                        )}
                        <div className="label-product">
                            <strong className="label-sku">
                                {display(data.sku)}
                            </strong>
                            <strong className="label-description">
                                {display(data.descricaoProduto)}
                            </strong>
                            <div className="label-client">
                                {display(data.nomeCliente)}
                            </div>
                            <div className="label-notes">
                                Notas fiscais:{" "}
                                {notes.length
                                    ? notes
                                          .map(
                                              (n) =>
                                                  `${display(n.numero)} (série ${display(n.serie)})`,
                                          )
                                          .join(" · ")
                                    : "Não informadas"}
                            </div>
                            <div>
                                Quantidade nas notas:{" "}
                                {formatProductQuantity(
                                    data.quantidadeProdutoNasNotas,
                                    data.unidadeMedida,
                                )}{" "}
                                {display(data.unidadeMedida)}
                            </div>
                        </div>
                        <div className="label-entry">
                            <strong className="label-date">{entryDate}</strong>
                            <strong>
                                CONFERENTE:{" "}
                                {data.nomeConferente
                                    ? String(data.nomeConferente)
                                    : "Não identificado no histórico"}
                            </strong>
                        </div>
                        <div className="label-codes">
                            {bars && (
                                <svg
                                    className="label-barcode"
                                    viewBox={`0 0 ${bars.length + 24} 85`}
                                    role="img"
                                    aria-label={`Código de barras do UUID ${code}`}
                                    preserveAspectRatio="none"
                                >
                                    <rect
                                        width={bars.length + 24}
                                        height={85}
                                        fill="white"
                                    />
                                    {Array.from(bars, (bit, i) =>
                                        bit === "1" ? (
                                            <rect
                                                key={i}
                                                x={i + 12}
                                                y={5}
                                                width={1}
                                                height={75}
                                                fill="black"
                                            />
                                        ) : null,
                                    )}
                                </svg>
                            )}
                            <div className="label-uuid">{code}</div>
                            <div className="label-dun">
                                <span>DUN: {display(data.codigoDun)}</span>
                                <span>
                                    Produtos por DUN:{" "}
                                    {formatProductQuantity(
                                        data.quantidadeProdutoPorDun,
                                        data.unidadeMedida,
                                    )}
                                </span>
                            </div>
                        </div>
                        <div className="label-bottom">
                            {qr ? (
                                <img
                                    className="label-qr"
                                    src={qr}
                                    alt={`QR do UUID ${code}`}
                                    width={128}
                                    height={128}
                                />
                            ) : (
                                <span className="label-qr">
                                    {qrError
                                        ? "Falha ao gerar QR"
                                        : "Preparando QR"}
                                </span>
                            )}
                            <strong
                                className="label-sequence"
                                aria-label="Unidade / total do pedido"
                            >
                                {display(data.numeroUnidade)}/
                                {display(data.totalUnidadesPedido)}
                            </strong>
                            <strong className="label-kind">
                                {data.tipo === "BOBINA"
                                    ? "BOBINAS"
                                    : data.tipo === "PALLET"
                                      ? "PALETES"
                                      : display(data.tipo)}
                            </strong>
                        </div>
                    </div>
                </div>
                <aside
                    className="no-print label-print-actions"
                    aria-label="Impressão da etiqueta"
                >
                    <div className="label-print-heading">
                        <span className="label-print-icon">
                            <Icon name="print" />
                        </span>
                        <h4>Impressão</h4>
                    </div>
                    <p>{information.unitLabelPrint.description}</p>
                    {!validCode && (
                        <p role="alert">
                            UUID inválido. Consulte novamente a unidade antes de
                            imprimir.
                        </p>
                    )}
                    {validCode && qrError && (
                        <p role="alert">
                            Não foi possível gerar o QR. Consulte novamente a
                            etiqueta antes de imprimir.
                        </p>
                    )}
                    {validCode && !qr && !qrError && (
                        <p role="status">Preparando os códigos da etiqueta…</p>
                    )}
                    <button
                        type="button"
                        className="action-details"
                        disabled={!ready || pending}
                        onClick={() => window.print()}
                    >
                        <Icon name="print" />
                        {ficticio
                            ? "Imprimir prévia fictícia"
                            : "Imprimir etiqueta"}
                    </button>
                    <details className="label-reading-help">
                        <summary>Como ler a etiqueta</summary>
                        <p>{information.unitLabel.description}</p>
                    </details>
                    {actions && (
                        <div className="label-next-step">
                            <h4>Próximo passo</h4>
                            {actions}
                        </div>
                    )}
                </aside>
            </div>
            {details && (
                <details className="no-print label-query-data">
                    <summary>Dados da unidade consultada</summary>
                    {details}
                </details>
            )}
        </section>
    );
}
