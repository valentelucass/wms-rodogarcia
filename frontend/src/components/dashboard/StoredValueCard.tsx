import type { VisaoOperacaoDto_Resumo } from "../../contracts/types";
import type { Perfil } from "../../contracts/runtime";
import {
    information,
    metricInformation,
    storedValueReasons,
    unknownStoredValueReason,
} from "../../content/information";
import { Icon } from "../../design-system/Icon";
import { InformationCard } from "../information/InformationPopup";
import { formatQuantity } from "./OperationDashboard";
import { overviewMoney } from "./overviewFormatting";

export function StoredValueCard({
    data,
    perfil = "OPERACAO",
}: {
    data?: VisaoOperacaoDto_Resumo;
    perfil?: Perfil;
}) {
    const allowed =
        (perfil === "SUPERVISOR" || perfil === "GESTOR") &&
        data?.financeiroPermitido === true;
    const valuation = allowed ? data.valoracao : null;
    const hasKnown =
        valuation?.conhecido != null &&
        [valuation.unidadesValoradas, valuation.entradasValoradas].some(
            (count) => BigInt(count) > 0n,
        );
    const partial = allowed && !data.valorCompleto && hasKnown;
    const tone = !allowed
        ? "neutral"
        : data.valorCompleto && data.valorArmazenado != null
          ? "success"
          : "warning";
    const value = overviewMoney(
        allowed
            ? data.valorCompleto
                ? data.valorArmazenado
                : partial
                  ? valuation!.conhecido
                  : null
            : null,
    );
    const note = !data
        ? "Aguardando a consulta"
        : !allowed
          ? "Disponível para Supervisor e Gestor"
          : data.valorCompleto
            ? "Mercadoria pela nota de origem"
            : partial
              ? "Parcial — valoração incompleta"
              : "Valoração pendente de informações";
    const content = metricInformation("storedValue", value, note);
    if (valuation) {
        content.facts!.push(
            {
                label: "Unidades valoradas",
                value: formatQuantity(valuation.unidadesValoradas),
            },
            {
                label: "Unidades sem valoração",
                value: formatQuantity(valuation.unidadesPendentes),
            },
            {
                label: "Itens de entrada não unitizados valorados",
                value: formatQuantity(valuation.entradasValoradas),
            },
            {
                label: "Itens de entrada não unitizados sem valoração",
                value: formatQuantity(valuation.entradasPendentes),
            },
            ...valuation.pendencias.map((pending) => {
                const reason =
                    storedValueReasons[pending.codigo] ??
                    unknownStoredValueReason;
                return {
                    label: `${reason.label} · ${formatQuantity(pending.ocorrencias)}`,
                    value: reason.description,
                };
            }),
        );
    }
    return (
        <InformationCard
            content={content}
            className={`overview-metric overview-metric--4 overview-metric--${tone} overview-stored-value${partial ? " overview-stored-value--partial" : ""}`}
        >
            <div className="overview-metric-heading">
                <span>{information.storedValue.title}</span>
                <Icon name="servicos" />
            </div>
            <strong>{value}</strong>
            <small className="stored-value-status">{note}</small>
            {valuation && !data?.valorCompleto && (
                <small>
                    Sem valoração: {formatQuantity(valuation.unidadesPendentes)}{" "}
                    {valuation.unidadesPendentes === "1"
                        ? "unidade"
                        : "unidades"}{" "}
                    · {formatQuantity(valuation.entradasPendentes)}{" "}
                    {valuation.entradasPendentes === "1"
                        ? "item de entrada"
                        : "itens de entrada"}
                </small>
            )}
        </InformationCard>
    );
}
