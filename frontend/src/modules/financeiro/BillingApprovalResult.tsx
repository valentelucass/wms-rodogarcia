import { Result } from "../../components/Result";
import { useContext } from "react";
import { FormReferences } from "../../components/FormReferences";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { isObject, type Values } from "../../contracts/runtime";
import { formatDate, indexRecords } from "../../domain/referencePresentation";
import { StatusBadge } from "../../design-system/StatusBadge";
import { information } from "../../content/information";
import { billingValue } from "./BillingAdjustments";

const states: Record<string, string> = {
    APROVADA: "Aprovada",
    APROVADO: "Aprovado",
    PREPARADA: "Preparada",
    EM_REVISAO: "Em revisão",
    SUPERADA: "Superada",
    REJEITADA: "Rejeitada",
};

export function BillingApprovalResult(
    props: Parameters<typeof BillingApprovalContent>[0],
) {
    const parent = useContext(FormReferences);
    const indexed = indexRecords(
        props.data,
        "FechamentoCobrancaDto.ConfirmacaoComando",
        Object.fromEntries(
            Object.entries(parent.records ?? {}).map(([type, rows]) => [
                type,
                [...rows],
            ]),
        ),
    );
    return (
        <FormReferences.Provider value={{ ...parent, records: indexed }}>
            <BillingApprovalContent {...props} />
        </FormReferences.Provider>
    );
}

function BillingApprovalContent({
    data,
    onSelect,
    pending,
}: {
    data: Values;
    onSelect?: (row: Values, type: string) => void;
    pending: boolean;
}) {
    const cycle = isObject(data.fechamento) ? data.fechamento : {},
        version = isObject(data.versao) ? data.versao : {};
    const reference = (field: string, row: Values, type: string) => (
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
    const badge = (value: unknown) => (
        <StatusBadge
            value={value}
            label={states[String(value)] ?? billingValue(value)}
        />
    );
    const date = (value: unknown) =>
        value ? formatDate(value) : "Não informado";
    const cycleType = "FechamentoCobrancaDto.Fechamento",
        versionType = "FechamentoCobrancaDto.Versao";
    return (
        <article
            className="billing-approval-result"
            aria-label="Ciclo retornado pela aprovação"
            aria-busy={pending}
        >
            <p className="hint">
                {information.billingApprovalResult.description}
            </p>
            <header className="billing-approval-header">
                <div>
                    <span className="hint">Período do ciclo</span>
                    <h3>
                        {date(cycle.periodoInicio)} · {date(cycle.periodoFim)}
                    </h3>
                </div>
                <div>
                    <span className="hint">Situação do ciclo</span>
                    {badge(cycle.situacao)}
                </div>
            </header>
            <dl className="billing-context">
                <div>
                    <dt>Cliente</dt>
                    <dd>{reference("clienteId", cycle, cycleType)}</dd>
                </div>
                <div>
                    <dt>Armazém</dt>
                    <dd>{reference("armazemId", cycle, cycleType)}</dd>
                </div>
            </dl>
            <section
                className="billing-approval-section"
                aria-label="Versão retornada"
            >
                <header>
                    <h3>Versão {billingValue(version.numero)}</h3>
                    {badge(version.situacao)}
                </header>
                <dl className="billing-detail-grid">
                    <div>
                        <dt>Cálculo de referência</dt>
                        <dd>{reference("calculoId", version, versionType)}</dd>
                    </div>
                    {field("Situação externa", version.estadoExterno)}
                    {field("Criada em (Brasília)", date(version.criadaEm))}
                    {field("Decidida em (Brasília)", date(version.decididaEm))}
                </dl>
                <dl className="billing-amounts billing-amounts--pair">
                    {field("Saldo informado", version.saldo)}
                    {field("Natureza", version.natureza)}
                </dl>
                <div className="billing-long-text">
                    <h4>Motivo da decisão</h4>
                    <p>{billingValue(version.motivoDecisao)}</p>
                </div>
            </section>
            <section
                className="billing-approval-section"
                aria-label="Ajustes desta versão"
            >
                <h3>
                    Ajustes desta versão
                    {Array.isArray(version.ajustes)
                        ? ` (${version.ajustes.length})`
                        : ""}
                </h3>
                <Result
                    data={version.ajustes}
                    type="List<FechamentoCobrancaDto.Ajuste>"
                    onSelect={onSelect}
                    pending={pending}
                />
            </section>
            <details className="billing-history">
                <summary>Entregas, confirmações e documentos da versão</summary>
                <div>
                    <section aria-label="Entregas da versão">
                        <h4>Entregas</h4>
                        <Result
                            data={version.entregas}
                            type="List<FechamentoCobrancaDto.Entrega>"
                            onSelect={onSelect}
                            pending={pending}
                        />
                    </section>
                    <section aria-label="Confirmações da versão">
                        <h4>Confirmações</h4>
                        <Result
                            data={version.confirmacoes}
                            type="List<FechamentoCobrancaDto.Confirmacao>"
                            onSelect={onSelect}
                            pending={pending}
                        />
                    </section>
                    <section aria-label="NFS-e da versão">
                        <h4>NFS-e</h4>
                        <Result
                            data={version.nfse}
                            type="List<FechamentoCobrancaDto.Documento>"
                            onSelect={onSelect}
                            pending={pending}
                        />
                    </section>
                    <section aria-label="Resolução financeira da versão">
                        <h4>Resolução financeira</h4>
                        <Result
                            data={version.resolucaoFinanceira}
                            type="FechamentoCobrancaDto.ResolucaoSaldo"
                            onSelect={onSelect}
                            pending={pending}
                        />
                    </section>
                </div>
            </details>
            <details className="billing-technical">
                <summary>
                    Identificadores e dados completos do ciclo e da versão
                </summary>
                <dl className="billing-detail-grid">
                    {field("ID do ciclo", cycle.id)}
                    {field("Revisão do ciclo", cycle.versao)}
                    {field("Versão atual do ciclo", cycle.versaoAtual)}
                    {field("ID do contrato", cycle.contratoId)}
                    {field("ID do cliente", cycle.clienteId)}
                    {field("ID do armazém", cycle.armazemId)}
                    {field("Início do período (original)", cycle.periodoInicio)}
                    {field(
                        "Fim exclusivo do período (original)",
                        cycle.periodoFim,
                    )}
                    {field("Ciclo criado em (original)", cycle.criadoEm)}
                    {field("Ciclo alterado em (original)", cycle.alteradoEm)}
                    {field("ID da versão", version.id)}
                    {field("ID do ciclo da versão", version.fechamentoId)}
                    {field("ID do cálculo", version.calculoId)}
                    {field("Decisor", version.decisor)}
                    {field("Versão criada em (original)", version.criadaEm)}
                    {field("Versão decidida em (original)", version.decididaEm)}
                    <div className="billing-detail-wide">
                        <dt>Hash do conteúdo da versão</dt>
                        <dd>
                            <code>{billingValue(version.conteudoHash)}</code>
                        </dd>
                    </div>
                </dl>
            </details>
            {onSelect && (
                <div
                    className="billing-detail-actions"
                    role="group"
                    aria-label="Usar dados retornados"
                >
                    {cycle.id !== undefined && (
                        <button
                            type="button"
                            className="action-select"
                            disabled={pending}
                            onClick={() => onSelect(cycle, cycleType)}
                        >
                            Selecionar ciclo
                        </button>
                    )}
                    {version.id !== undefined && (
                        <button
                            type="button"
                            className="action-select"
                            disabled={pending}
                            onClick={() => onSelect(version, versionType)}
                        >
                            Selecionar versão
                        </button>
                    )}
                </div>
            )}
        </article>
    );
}
