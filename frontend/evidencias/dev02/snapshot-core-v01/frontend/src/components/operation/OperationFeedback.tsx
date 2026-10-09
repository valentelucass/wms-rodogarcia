import { ApiError } from "../../api/client";
import { InputError } from "../../contracts/codec";
import type { OperationState } from "../../hooks/useOperation";
import { useLayoutEffect } from "react";
export function OperationFeedback({ state: s }: { state: OperationState }) {
    useLayoutEffect(() => {
        if (!s.pending && (s.error || s.receipt)) s.status.current?.focus();
    }, [s.pending, s.error, s.receipt, s.status]);
    return (
        <div
            ref={s.status}
            tabIndex={-1}
            role={s.error ? "alert" : "status"}
            aria-live="polite"
            className="feedback"
        >
            {s.pending && <p>Carregando. Aguarde a resposta.</p>}
            {s.error && (
                <>
                    <p>{s.error.message}</p>
                    {s.error instanceof ApiError && (
                        <p>
                            Código: {s.error.code} · Correlação:{" "}
                            {s.error.requestId || "não informada"}
                        </p>
                    )}
                    {s.error instanceof ApiError && s.error.status === 401 && (
                        <p>
                            A sessão expirou. Retorne à seleção fictícia de
                            perfil. Integração com provedor real pendente.
                        </p>
                    )}
                    {s.uncertain && (
                        <p>
                            Resultado desconhecido. Campos conservados. Consulte
                            o estado atual ou repita somente o mesmo conteúdo e
                            identificador; nenhuma nova chave foi criada.
                        </p>
                    )}
                    {s.error instanceof InputError && (
                        <p>Campo: {s.error.field}</p>
                    )}
                </>
            )}
            {s.receipt && (
                <p>
                    {s.receipt.ficticio
                        ? "Resposta FICTÍCIA de exercício recebida."
                        : "Resposta do servidor recebida."}{" "}
                    {s.receipt.replay
                        ? "Repetição: confirmação original; consulte o estado atual para acompanhar alterações."
                        : ""}{" "}
                    Correlação: {s.receipt.requestId}
                </p>
            )}
        </div>
    );
}
