import type { ReactNode } from "react";
import { AppFooter, developer } from "../components/shell/AppFooter";
import { Brand } from "../components/shell/Brand";
import { Icon } from "../design-system/Icon";
import { ThemeSelector } from "../design-system/ThemeSelector";

export function AuthLayout({
    children,
    wide = false,
}: {
    children: ReactNode;
    wide?: boolean;
}) {
    return (
        <div className="auth-shell">
            <header className="auth-header">
                <div className="auth-header-content">
                    <Brand />
                    <div className="auth-header-actions">
                        <span className="auth-access-label">
                            <Icon name="acesso" /> Acesso interno
                        </span>
                        <a className="support-link" href={developer.support}>
                            <Icon name="support" />
                            <span>Precisa de ajuda?</span>
                        </a>
                        <ThemeSelector />
                    </div>
                </div>
            </header>
            <main className={`auth-main${wide ? " auth-main--wide" : ""}`}>
                {children}
            </main>
            <AppFooter />
        </div>
    );
}
