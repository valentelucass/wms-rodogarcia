import { parse, stringify } from "lossless-json";
import type { Values } from "../../contracts/runtime";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { formatDate } from "../../domain/referencePresentation";
import { information } from "../../content/information";
import { auditTargets } from "./auditTargets";

const actions: Record<string, string> = {
    CRIACAO: "Criação",
    ALTERACAO: "Alteração",
    INATIVACAO: "Inativação",
    REATIVACAO: "Reativação",
    CONCILIACAO_CONTINGENCIA: "Conciliação da contingência",
};
const shown = (value: unknown) =>
    value == null || value === "" ? "Não informado" : String(value);
const subject = (value: unknown) =>
    auditTargets.find((target) => target.tipo === value)?.label ?? shown(value);

function Snapshot({ value, after }: { value: unknown; after?: boolean }) {
    const raw = typeof value === "string" ? value : null;
    let text = raw,
        formatted = false;
    if (raw !== null) {
        try {
            text = stringify(parse(raw), null, 2) ?? raw;
            formatted = true;
        } catch {
            // Historical text remains readable even when it is not valid JSON.
        }
    }
    const title = after ? "Depois da alteração" : "Antes da alteração";
    return (
        <section
            className={
                "audit-snapshot" + (after ? " audit-snapshot--after" : "")
            }
            aria-label={title}
        >
            <h4>{title}</h4>
            {raw === null ? (
                <p className="audit-snapshot-empty">
                    {after
                        ? "Sem dados posteriores informados."
                        : "Sem dados anteriores informados."}
                </p>
            ) : (
                <>
                    {!formatted && (
                        <p className="audit-snapshot-note">
                            {information.auditSnapshotText.description}
                        </p>
                    )}
                    <pre tabIndex={0} aria-label={`Conteúdo: ${title}`}>
                        <code>{text}</code>
                    </pre>
                    {formatted && (
                        <details className="audit-snapshot-original">
                            <summary>Ver JSON original</summary>
                            <pre
                                tabIndex={0}
                                aria-label={`JSON original: ${title}`}
                            >
                                <code>{raw}</code>
                            </pre>
                        </details>
                    )}
                </>
            )}
        </section>
    );
}

export function AuditDetail({ data }: { data: Values }) {
    const legacy = data.tipo === "LEGADO_NAO_ATRIBUIDO";
    return (
        <div className="audit-detail">
            <header className="audit-detail-heading">
                <span className="hint">Alteração registrada</span>
                <h3>{actions[String(data.acao)] ?? shown(data.acao)}</h3>
            </header>
            {legacy && (
                <aside
                    className="audit-detail-legacy"
                    aria-label={information.auditHistoryLegacy.title}
                >
                    <strong>{information.auditHistoryLegacy.title}</strong>
                    <p>{information.auditHistoryLegacy.description}</p>
                </aside>
            )}
            <dl className="audit-detail-metadata">
                <div>
                    <dt>Registro</dt>
                    <dd>
                        <ReferenceValue
                            field="registroId"
                            value={data.registroId}
                            row={data}
                            type="AuditoriaResponse"
                        />
                    </dd>
                </div>
                <div>
                    <dt>Data e hora</dt>
                    <dd>{formatDate(data.instante)}</dd>
                </div>
                <div>
                    <dt>Tipo do registro</dt>
                    <dd>
                        {legacy ? "Legado não atribuído" : subject(data.tipo)}
                    </dd>
                </div>
                <div>
                    <dt>Usuário (identificador)</dt>
                    <dd className="audit-detail-identifier">
                        {shown(data.usuario)}
                    </dd>
                </div>
                <div className="audit-detail-wide">
                    <dt>Origem física da auditoria</dt>
                    <dd>{subject(data.tipoFisico)}</dd>
                </div>
            </dl>
            <section
                className="audit-detail-reason"
                aria-label="Motivo da alteração"
            >
                <h3>Motivo / justificativa</h3>
                <p>{shown(data.motivo)}</p>
            </section>
            <section
                className="audit-detail-comparison"
                aria-label="Comparação da alteração"
            >
                <header>
                    <h3>{information.auditSnapshots.title}</h3>
                    <p>{information.auditSnapshots.description}</p>
                </header>
                <div className="audit-snapshot-grid">
                    <Snapshot value={data.dadosAntes} />
                    <Snapshot value={data.dadosDepois} after />
                </div>
            </section>
            <details className="audit-detail-technical">
                <summary>Identificadores da auditoria</summary>
                <dl className="audit-detail-metadata">
                    {[
                        ["Identificador da alteração", data.id],
                        ["Identificador da operação", data.idOperacao],
                        ["Identificador do registro", data.registroId],
                        ["Instante original", data.instante],
                        ["Código da ação", data.acao],
                        ["Código do tipo", data.tipo],
                        ["Código da origem física", data.tipoFisico],
                    ].map(([title, value]) => (
                        <div key={String(title)}>
                            <dt>{String(title)}</dt>
                            <dd className="audit-detail-identifier">
                                {shown(value)}
                            </dd>
                        </div>
                    ))}
                </dl>
            </details>
        </div>
    );
}
