import { useEffect, useState } from "react";

export const developer = {
    name: "Lucas Andrade",
    profile: "https://www.linkedin.com/in/dev-lucasandrade/",
    support: "mailto:lucasmac.dev@gmail.com",
    email: "lucasmac.dev@gmail.com",
};

export function AppFooter() {
    const runId = import.meta.env.PROD
        ? (import.meta.env.VITE_WMS_PRODUCTION_RUN_ID as string | undefined)
        : undefined;
    const [release, setRelease] = useState<{
        runId: string;
        activatedUtc: string;
    } | null>(null);
    const [unavailable, setUnavailable] = useState(false);
    useEffect(() => {
        if (!runId) return;
        let disposed = false;
        let pending = false;
        const controller = new AbortController();
        const refresh = async () => {
            if (pending) return;
            pending = true;
            try {
                const response = await fetch("/__wms/release", {
                    cache: "no-store",
                    signal: AbortSignal.any([
                        controller.signal,
                        AbortSignal.timeout(10000),
                    ]),
                });
                if (!response.ok) throw new Error("RELEASE_UNAVAILABLE");
                const data = await response.json();
                if (
                    typeof data.runId !== "string" ||
                    typeof data.activatedUtc !== "string" ||
                    !Number.isFinite(Date.parse(data.activatedUtc))
                )
                    throw new Error("RELEASE_INVALID");
                if (!disposed) {
                    setRelease(data);
                    setUnavailable(false);
                }
            } catch {
                if (!disposed) setUnavailable(true);
            } finally {
                pending = false;
            }
        };
        void refresh();
        window.addEventListener("focus", refresh);
        const interval = window.setInterval(() => {
            void refresh();
        }, 60000);
        return () => {
            disposed = true;
            controller.abort();
            window.clearInterval(interval);
            window.removeEventListener("focus", refresh);
        };
    }, [runId]);
    return (
        <footer className="app-footer">
            <div className="app-footer-content">
                <div className="app-footer-product">
                    <strong>
                        Rodogarcia <span aria-hidden="true">·</span> WMS
                    </strong>
                    <div className="app-footer-legal">
                        <span>
                            © {new Date().getFullYear()} Rodogarcia. Todos os
                            direitos reservados.
                        </span>
                        {runId && (
                            <span
                                className="app-footer-update"
                                aria-live="polite"
                            >
                                {unavailable ? (
                                    "Atualização indisponível"
                                ) : !release ? (
                                    "Conferindo atualização…"
                                ) : release.runId !== runId ? (
                                    <a href={window.location.href}>
                                        Nova versão disponível. Atualize a
                                        página.
                                    </a>
                                ) : (
                                    <>
                                        Atualizado em{" "}
                                        <time dateTime={release.activatedUtc}>
                                            {new Intl.DateTimeFormat("pt-BR", {
                                                timeZone: "America/Sao_Paulo",
                                                dateStyle: "short",
                                                timeStyle: "medium",
                                            }).format(
                                                new Date(release.activatedUtc),
                                            )}
                                        </time>{" "}
                                        (Brasília)
                                    </>
                                )}
                            </span>
                        )}
                    </div>
                </div>
                <div className="app-footer-credit">
                    <span>
                        Desenvolvido por{" "}
                        <a
                            href={developer.profile}
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            {developer.name}
                            <span className="sr-only"> (abre em nova aba)</span>
                        </a>
                    </span>
                    <span>
                        Suporte:{" "}
                        <a href={developer.support}>{developer.email}</a>
                    </span>
                </div>
            </div>
        </footer>
    );
}
