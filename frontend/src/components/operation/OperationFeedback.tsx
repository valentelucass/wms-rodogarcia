import { ApiError } from "../../api/client";
import { InputError } from "../../contracts/codec";
import type { OperationState } from "../../hooks/useOperation";
import { useLayoutEffect } from "react";
import { Icon } from "../../design-system/Icon";
import { information } from "../../content/information";
export function OperationFeedback({ state: s }: { state: OperationState }) {
    const initialLoadQuery = s.e.id === "CargaInicialController.consultar";
    const labelQuery = s.e.id === "UnidadeLogisticaController.etiqueta";
    useLayoutEffect(() => {
        if (!s.pending && (s.error || s.receipt)) s.status.current?.focus();
    }, [s.pending, s.error, s.receipt, s.status]);
    return (
        <div
            ref={s.status}
            tabIndex={-1}
            role={s.error ? "alert" : "status"}
            aria-live="polite"
            className={
                (initialLoadQuery
                    ? "feedback feedback--initial-load-query"
                    : labelQuery
                      ? "feedback feedback--label-query"
                      : "feedback") +
                (s.error ? " feedback--error" : "") +
                (s.error && s.uncertain ? " feedback--uncertain" : "")
            }
        >
            {s.pending && <p>Carregando. Aguarde a resposta.</p>}
            {s.error && (
                <div className="operation-error">
                    <span className="operation-error-icon">
                        <Icon name="contingencia" />
                    </span>
                    <div className="operation-error-content">
                        <p className="operation-error-message">
                            {s.error.message}
                        </p>
                        {s.error instanceof InputError && (
                            <p>Campo: {s.error.field}</p>
                        )}
                        {s.error instanceof ApiError &&
                            s.error.status === 401 && (
                                <p>
                                    A sessão expirou. Entre novamente para
                                    continuar.
                                </p>
                            )}
                        {s.uncertain && (
                            <p>
                                Resultado desconhecido. Campos conservados.
                                Consulte o estado atual ou repita somente o
                                mesmo conteúdo e identificador; nenhuma nova
                                chave foi criada.
                            </p>
                        )}
                        {s.error instanceof ApiError && (
                            <details className="operation-error-details">
                                <summary>
                                    {information.operationErrorDetails.title}
                                </summary>
                                <p className="hint">
                                    {
                                        information.operationErrorDetails
                                            .description
                                    }
                                </p>
                                <dl>
                                    <div>
                                        <dt>Código</dt>
                                        <dd>
                                            <code>{s.error.code}</code>
                                        </dd>
                                    </div>
                                    <div>
                                        <dt>Correlação</dt>
                                        <dd>
                                            <code>
                                                {s.error.requestId ||
                                                    "não informada"}
                                            </code>
                                        </dd>
                                    </div>
                                </dl>
                            </details>
                        )}
                    </div>
                </div>
            )}
            {s.receipt &&
                !s.pending &&
                (initialLoadQuery || labelQuery ? (
                    <div
                        className={
                            initialLoadQuery
                                ? "initial-load-query-feedback"
                                : "label-query-feedback"
                        }
                    >
                        <p>
                            {s.receipt.ficticio
                                ? "Resposta FICTÍCIA de exercício recebida."
                                : "Consulta concluída."}
                        </p>
                        <details>
                            <summary>Dados da consulta</summary>
                            <dl>
                                <dt>Código de correlação</dt>
                                <dd>
                                    {s.receipt.requestId || "Não informado"}
                                </dd>
                            </dl>
                        </details>
                    </div>
                ) : (
                    <p>
                        {s.receipt.ficticio
                            ? "Resposta FICTÍCIA de exercício recebida."
                            : "Resposta do servidor recebida."}{" "}
                        {s.receipt.replay
                            ? "Repetição: confirmação original; consulte o estado atual para acompanhar alterações."
                            : ""}{" "}
                        Correlação: {s.receipt.requestId}
                    </p>
                ))}
        </div>
    );
}
