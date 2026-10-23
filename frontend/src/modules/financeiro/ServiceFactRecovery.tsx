import type { ReactNode } from "react";
import { information } from "../../content/information";
import { Icon } from "../../design-system/Icon";

export function ServiceFactRecovery({
    verifying,
    error,
    hasVerification,
    onVerify,
    onExit,
    children,
}: {
    verifying: boolean;
    error: string;
    hasVerification: boolean;
    onVerify: () => void;
    onExit: () => void;
    children: ReactNode;
}) {
    return (
        <section
            className="service-fact-recovery"
            aria-label="Recuperação da operação"
        >
            <header className="service-fact-recovery-warning" role="status">
                <strong>{information.serviceFactRecovery.title}</strong>
                <p>{information.serviceFactRecovery.description}</p>
            </header>
            <div className="service-fact-recovery-toolbar">
                <div>
                    <h3>Consultar o fato</h3>
                    <p className="hint">
                        Consulta somente leitura. A repetição da alteração
                        mantém o conteúdo e o identificador originais.
                    </p>
                </div>
                <button
                    type="button"
                    className="action-refresh"
                    disabled={verifying}
                    onClick={onVerify}
                    aria-label="Consultar estado atual sem repetir"
                >
                    <Icon name="refresh" />
                    {verifying ? "Consultando…" : "Consultar estado atual"}
                </button>
            </div>
            {error && (
                <p className="service-fact-recovery-error" role="alert">
                    {error}
                </p>
            )}
            {hasVerification && (
                <section
                    className="service-fact-recovery-result"
                    aria-label="Consulta de conferência"
                    aria-busy={verifying}
                >
                    <header>
                        <h3>
                            {verifying
                                ? "Consulta anterior · atualizando"
                                : error
                                  ? "Última consulta recebida"
                                  : "Dados recebidos na consulta"}
                        </h3>
                        <p className="hint">
                            A situação consultada não confirma o resultado da
                            anulação.
                        </p>
                    </header>
                    {children}
                </section>
            )}
            {hasVerification && (
                <footer className="service-fact-recovery-exit">
                    <p>
                        Ao sair, o resultado da anulação permanece desconhecido.
                    </p>
                    <button
                        type="button"
                        className="action-dismiss"
                        onClick={onExit}
                        aria-label="Voltar à lista mantendo resultado desconhecido"
                    >
                        Sair sem confirmar resultado
                    </button>
                </footer>
            )}
        </section>
    );
}
