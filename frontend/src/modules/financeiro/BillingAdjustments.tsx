import { ReferenceValue } from "../../components/context/ReferencePresentation";
import type { Values } from "../../contracts/runtime";
import { information } from "../../content/information";
import { display } from "../../domain/labels";
import { formatDate } from "../../domain/referencePresentation";
import { StatusBadge } from "../../design-system/StatusBadge";
import "./billingApproval.css";

export const billingValue = (value: unknown) =>
    value === "" || value == null ? "Não informado" : display(value);
const states: Record<string, string> = {
    APLICADO: "Aplicado",
    APLICADA: "Aplicada",
    PENDENTE: "Pendente",
    REVERTIDO: "Revertido",
    CANCELADO: "Cancelado",
};

export function BillingAdjustments({
    rows,
    onSelect,
    pending = false,
}: {
    rows: Values[];
    onSelect?: (row: Values, type: string) => void;
    pending?: boolean;
}) {
    const type = "FechamentoCobrancaDto.Ajuste";
    return (
        <div className="billing-adjustments" aria-busy={pending}>
            {rows.length > 0 && (
                <p className="hint">
                    {information.billingAdjustments.description}
                </p>
            )}
            {!rows.length && <p className="hint">Nenhum ajuste informado.</p>}
            {rows.map((row, index) => {
                const reference = (field: string) => (
                    <ReferenceValue
                        field={field}
                        value={row[field]}
                        row={row}
                        type={type}
                    />
                );
                const field = (title: string, value: unknown) => (
                    <div>
                        <dt>{title}</dt>
                        <dd>{billingValue(value)}</dd>
                    </div>
                );
                return (
                    <article
                        className="billing-adjustment"
                        key={index}
                        aria-label={`Ajuste ${index + 1}`}
                    >
                        <header>
                            <h4>Ajuste {index + 1}</h4>
                            <StatusBadge
                                value={row.situacao}
                                label={
                                    states[String(row.situacao)] ??
                                    billingValue(row.situacao)
                                }
                            />
                        </header>
                        <dl className="billing-detail-grid">
                            <div>
                                <dt>Versão de origem</dt>
                                <dd>{reference("origemVersaoId")}</dd>
                            </div>
                            <div>
                                <dt>Ciclo de destino</dt>
                                <dd>{reference("destinoFechamentoId")}</dd>
                            </div>
                            <div>
                                <dt>Cálculo base</dt>
                                <dd>{reference("calculoBaseId")}</dd>
                            </div>
                            <div>
                                <dt>Cálculo corrigido</dt>
                                <dd>{reference("calculoCorrigidoId")}</dd>
                            </div>
                        </dl>
                        <dl className="billing-amounts">
                            {field("Valor base", row.valorBase)}
                            {field("Valor corrigido", row.valorCorrigido)}
                            {field("Diferença informada", row.diferenca)}
                        </dl>
                        <div className="billing-long-text">
                            <h5>Motivo / justificativa</h5>
                            <p>{billingValue(row.motivo)}</p>
                        </div>
                        <div className="billing-long-text">
                            <h5>Evidência</h5>
                            <p>{billingValue(row.evidencia)}</p>
                        </div>
                        <dl className="billing-detail-grid">
                            <div>
                                <dt>Aplicado na versão</dt>
                                <dd>{reference("aplicadoVersaoId")}</dd>
                            </div>
                            {field(
                                "Aplicado em (Brasília)",
                                row.aplicadoEm
                                    ? formatDate(row.aplicadoEm)
                                    : null,
                            )}
                        </dl>
                        <details className="billing-technical">
                            <summary>
                                Identificadores e hash do ajuste {index + 1}
                            </summary>
                            <dl className="billing-detail-grid">
                                {field("ID do ajuste", row.id)}
                                {field(
                                    "ID da versão de origem",
                                    row.origemVersaoId,
                                )}
                                {field(
                                    "ID do ciclo de destino",
                                    row.destinoFechamentoId,
                                )}
                                {field("ID do cálculo base", row.calculoBaseId)}
                                {field(
                                    "ID do cálculo corrigido",
                                    row.calculoCorrigidoId,
                                )}
                                {field(
                                    "ID da versão de aplicação",
                                    row.aplicadoVersaoId,
                                )}
                                {field("Usuário", row.usuario)}
                                {field(
                                    "Registrado em (original)",
                                    row.registradoEm,
                                )}
                                {field(
                                    "Aplicado em (original)",
                                    row.aplicadoEm,
                                )}
                                {"tipo" in row &&
                                    field("Tipo do ajuste", row.tipo)}
                                {"tratativaOrigemId" in row &&
                                    field(
                                        "ID da tratativa de origem",
                                        row.tratativaOrigemId,
                                    )}
                                <div className="billing-detail-wide">
                                    <dt>Hash da correção</dt>
                                    <dd>
                                        <code>
                                            {billingValue(row.hashCorrecao)}
                                        </code>
                                    </dd>
                                </div>
                            </dl>
                        </details>
                        {onSelect && row.id !== undefined && (
                            <div className="billing-detail-actions">
                                <button
                                    type="button"
                                    className="action-select"
                                    disabled={pending}
                                    onClick={() => onSelect(row, type)}
                                    aria-label={`Selecionar ajuste ${index + 1}`}
                                >
                                    Selecionar ajuste
                                </button>
                            </div>
                        )}
                    </article>
                );
            })}
        </div>
    );
}
