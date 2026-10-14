import { useEffect, useState } from "react";
import QRCode from "qrcode";
import { isObject } from "../contracts/runtime";
import { display } from "../domain/labels";
import { ReferenceValue } from "./context/ReferencePresentation";
import { formatDate } from "../domain/referencePresentation";
import { formatProductQuantity } from "../domain/quantityPresentation";
export function LabelPreview({ data }: { data: unknown }) {
    const [qr, setQr] = useState("");
    const code = isObject(data) ? String(data.codigoLeitura ?? "") : "";
    useEffect(() => {
        let active = true;
        if (code)
            void QRCode.toDataURL(code, {
                width: 220,
                margin: 2,
                errorCorrectionLevel: "M",
            }).then((url) => {
                if (active) setQr(url);
            });
        return () => {
            active = false;
        };
    }, [code]);
    if (!isObject(data)) return null;
    return (
        <section
            className="label-preview"
            aria-label="Prévia de etiqueta fictícia"
        >
            <h3>ETIQUETA DE EXERCÍCIO FICTÍCIO</h3>
            <p>
                SKU: {display(data.sku)} · Código da unidade: {code}
            </p>
            <p>
                Entrada / FIFO: {formatDate(data.dataEntrada)} · Chegada:{" "}
                {formatDate(data.chegadaReal)}
            </p>
            <p>
                Nota:{" "}
                <ReferenceValue field="notaId" value={data.notaId} row={data} />{" "}
                · Quantidade:{" "}
                {formatProductQuantity(
                    data.quantidadeProduto,
                    data.unidadeMedida,
                )}{" "}
                {display(data.unidadeMedida)}
            </p>
            {qr && (
                <img
                    src={qr}
                    alt={"QR do UUID " + code}
                    width={220}
                    height={220}
                />
            )}
            <p>
                DUN: {display(data.codigoDun)} · Quantidade por DUN:{" "}
                {display(data.quantidadeProdutoPorDun)}
            </p>
            <button
                className="no-print"
                type="button"
                onClick={() => window.print()}
            >
                Imprimir prévia fictícia
            </button>
            <p className="no-print">
                Reimpressão consulta o UUID existente. Tanca, tamanho, leitura e
                substituição física ainda precisam de validação no equipamento.
            </p>
        </section>
    );
}
