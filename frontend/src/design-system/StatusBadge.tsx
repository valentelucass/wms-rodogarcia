import { display } from "../domain/labels";

const states: Record<string, Record<string, string>> = {
    success: {
        ATIVO: "Ativo",
        BOA: "Boa",
        NORMAL: "Normal",
        DISPONIVEL: "Disponível",
        EFETIVADO: "Efetivado",
        CONFERIDO: "Conferido",
        CONCLUIDO: "Concluído",
        CONCILIADO: "Conciliado",
        CONCILIADA: "Conciliada",
        RECONCILIADA: "Reconciliada",
        REGULARIZADA: "Regularizada",
        APLICADA: "Aplicada",
        EMITIDA: "Emitida",
        FECHADO: "Fechado",
        SEPARADO: "Separado",
        RETIRADO: "Retirado",
        RETIRADA: "Retirada",
    },
    warning: {
        ENCERRAMENTO_PENDENTE: "Encerramento pendente",
        PENDENTE: "Pendente",
        PENDENTE_RESERVA: "Pendente de reserva",
        QUARENTENA: "Quarentena",
        TRIAGEM: "Triagem",
        CONFERENCIA_PENDENTE: "Conferência pendente",
        CONFERIDO_COM_DIVERGENCIA: "Conferido com divergência",
    },
    danger: {
        DIVERGENTE: "Divergente",
        BLOQUEADO: "Bloqueado",
        BLOQUEADA: "Bloqueada",
        AVARIADO: "Avariado",
        AVARIADA: "Avariada",
        REJEITADO: "Rejeitado",
        RECUSADO: "Recusado",
        FALHOU: "Falhou",
    },
    info: {
        RASCUNHO: "Rascunho",
        EM_CONFERENCIA: "Em conferência",
        EM_PROCESSAMENTO: "Em processamento",
        RESERVADO: "Reservado",
        RESERVADA: "Reservada",
        ATIVA: "Ativa",
        EM_SEPARACAO: "Em separação",
        PREPARADA: "Preparada",
        ABERTO: "Aberto",
        EM_ANDAMENTO: "Em andamento",
    },
    neutral: {
        INATIVO: "Inativo",
        CANCELADO: "Cancelado",
        CANCELADA: "Cancelada",
        ESTORNADO: "Estornado",
        ESTORNADA: "Estornada",
        REVERTIDA: "Revertida",
        SUBSTITUIDA: "Substituída",
    },
};

function presentation(value: unknown) {
    const key = String(value ?? "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim()
        .toLocaleUpperCase("pt-BR")
        .replace(/\s+/g, "_");
    const match = Object.entries(states).find(([, labels]) =>
        Object.hasOwn(labels, key),
    );
    return {
        tone: match?.[0] ?? "neutral",
        label: match?.[1][key] ?? display(value),
    };
}

export const statusLabel = (value: unknown) => presentation(value).label;

export function StatusBadge({
    value,
    label,
}: {
    value: unknown;
    label?: string;
}) {
    const state = presentation(value);
    return (
        <span className="record-status" data-tone={state.tone}>
            <span className="record-status-dot" aria-hidden="true" />
            {label ?? state.label}
        </span>
    );
}
