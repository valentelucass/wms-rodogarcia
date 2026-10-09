import { downloadReceipt, type Receipt } from "../../api/client";
import { isObject, type Values } from "../../contracts/runtime";
import { ImportPreview } from "../../modules/cadastros/ImportPreview";
import {
    nextActions,
    dispatchContinuation,
    type NextAction,
} from "../../domain/journeys";
import { Result } from "../Result";
import { LabelPreview } from "../LabelPreview";
import { CollectorReceipt } from "../../modules/estoque/CollectorReceipt";
export function OperationResults({
    id,
    receipt,
    type,
    onSelect,
    onContinue,
    collector = false,
}: {
    id: string;
    receipt: Receipt;
    type: string;
    onSelect: (v: Values, type: string) => void;
    onContinue?: (next: NextAction) => void;
    collector?: boolean;
}) {
    const next = dispatchContinuation(id, receipt, nextActions[id]);
    return (
        <div className="results">
            {id.startsWith("ImportacaoEnderecoController.") && (
                <ImportPreview data={receipt.data} />
            )}
            {collector ? (
                <CollectorReceipt
                    data={receipt.data}
                    type={type}
                    onSelect={onSelect}
                />
            ) : (
                <Result data={receipt.data} type={type} onSelect={onSelect} />
            )}
            {id === "UnidadeLogisticaController.etiqueta" && (
                <LabelPreview data={receipt.data} />
            )}
            {id === "FechamentoCobrancaController.demonstrativo" && (
                <button type="button" onClick={() => downloadReceipt(receipt)}>
                    Baixar os bytes do demonstrativo fictício
                </button>
            )}
            {receipt.replay && (
                <p>
                    Confirmação histórica: consulte o detalhe atual antes de
                    continuar a jornada.
                </p>
            )}
            {onContinue &&
                next &&
                !receipt.replay &&
                !(
                    id === "ImportacaoEnderecoController.consultar" &&
                    isObject(receipt.data) &&
                    (receipt.data.situacao === "CONFIRMADA" ||
                        (Array.isArray(receipt.data.erros) &&
                            receipt.data.erros.length > 0))
                ) && (
                    <button
                        type="button"
                        className="primary"
                        onClick={() => onContinue(next)}
                    >
                        {next.title}
                    </button>
                )}
        </div>
    );
}
