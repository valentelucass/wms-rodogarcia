import { useState, type ReactNode } from "react";
import { AppFooter, developer } from "../components/shell/AppFooter";
import { Brand } from "../components/shell/Brand";
import { Icon } from "../design-system/Icon";
import { ThemeSelector } from "../design-system/ThemeSelector";

import { ShippingCartons } from "./ShippingCartons";

export function AuthLayout({
    children,
    shippingLabel = false,
}: {
    children: ReactNode;
    shippingLabel?: boolean;
}) {
    const [paused, setPaused] = useState(false);
    const motionAction = paused ? "Retomar animação" : "Pausar animação";
    return (
        <div
            data-motion={
                shippingLabel ? (paused ? "paused" : "running") : undefined
            }
            className={
                shippingLabel ? "auth-shell shipping-login" : "auth-shell"
            }
        >
            <header className="auth-header">
                <div className="auth-header-content">
                    {shippingLabel ? (
                        <div className="shipping-brand">
                            <span
                                role="img"
                                aria-label="Rodogarcia"
                                className="shipping-wordmark"
                            />
                            <strong>WMS</strong>
                        </div>
                    ) : (
                        <Brand />
                    )}
                    <div className="auth-header-actions">
                        {!shippingLabel && (
                            <span className="auth-access-label">
                                <Icon name="acesso" /> Acesso interno
                            </span>
                        )}
                        <a className="support-link" href={developer.support}>
                            <Icon name="support" />
                            <span>Precisa de ajuda?</span>
                        </a>
                        <ThemeSelector />
                    </div>
                </div>
            </header>
            <main
                className={`auth-main${shippingLabel ? " shipping-main" : ""}`}
            >
                {shippingLabel ? (
                    <>
                        <ShippingCartons />
                        <div className="shipping-ticket">
                            <button
                                type="button"
                                className="theme-button shipping-motion"
                                aria-label={motionAction}
                                title={motionAction}
                                onClick={() => setPaused((value) => !value)}
                            >
                                <span aria-hidden="true" />
                            </button>
                            {children}
                        </div>
                    </>
                ) : (
                    children
                )}
            </main>
            <AppFooter />
        </div>
    );
}
