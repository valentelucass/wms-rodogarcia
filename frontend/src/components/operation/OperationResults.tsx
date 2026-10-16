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
import { BillingApprovalResult } from "../../modules/financeiro/BillingApprovalResult";
export function OperationResults({
    id,
    receipt,
    type,
    onSelect,
    onContinue,
    collector = false,
    pending = false,
    onPage,
}: {
    id: string;
    receipt: Receipt;
    type: string;
    onSelect: (v: Values, type: string) => void;
    onContinue?: (next: NextAction) => void;
    collector?: boolean;
    pending?: boolean;
    onPage?: (page: number) => void;
}) {
    const next = dispatchContinuation(id, receipt, nextActions[id]);
    const labelPreview =
        id === "UnidadeLogisticaController.etiqueta" && isObject(receipt.data);
    const billingApproval =
        id === "FechamentoCobrancaController.aprovar" &&
        type === "FechamentoCobrancaDto.ConfirmacaoComando" &&
        isObject(receipt.data) &&
        isObject(receipt.data.fechamento) &&
        isObject(receipt.data.versao);
    const continuation =
        onContinue &&
        next &&
        !receipt.replay &&
        !(
            id === "ImportacaoEnderecoController.consultar" &&
            isObject(receipt.data) &&
            (receipt.data.situacao === "CONFIRMADA" ||
                (Array.isArray(receipt.data.erros) &&
                    receipt.data.erros.length > 0))
        ) ? (
            <button
                type="button"
                className="primary operation-continue"
                disabled={(labelPreview || billingApproval) && pending}
                onClick={() => onContinue(next)}
            >
                {next.title}
            </button>
        ) : null;
    const result = billingApproval ? (
        <BillingApprovalResult
            data={receipt.data as Values}
            onSelect={onSelect}
            pending={pending}
        />
    ) : (
        <Result
            data={receipt.data}
            type={type}
            onSelect={onSelect}
            onPage={onPage}
            pending={pending}
        />
    );
    return (
        <div className="results">
            <h4 className="results-heading">
                {pending
                    ? "Resultado anterior · aguardando atualização"
                    : id === "CargaInicialController.consultar"
                      ? "Carga consultada"
                      : labelPreview
                        ? "Etiqueta consultada"
                        : "Resultado da operação"}
            </h4>
            {id.startsWith("ImportacaoEnderecoController.") && (
                <ImportPreview data={receipt.data} />
            )}
            {labelPreview ? (
                <LabelPreview
                    data={receipt.data}
                    ficticio={receipt.ficticio}
                    pending={pending}
                    actions={continuation}
                    details={result}
                />
            ) : collector ? (
                <CollectorReceipt
                    data={receipt.data}
                    type={type}
                    onSelect={onSelect}
                />
            ) : (
                result
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
            {!labelPreview &&
                (billingApproval && continuation ? (
                    <section
                        className="billing-approval-next"
                        aria-label="Próximo passo após aprovação"
                    >
                        <h4>Próximo passo</h4>
                        {continuation}
                    </section>
                ) : (
                    continuation
                ))}
        </div>
    );
}
