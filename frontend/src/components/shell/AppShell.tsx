import {
    useLayoutEffect,
    useRef,
    useState,
    useSyncExternalStore,
    type ReactNode,
} from "react";
import { Icon } from "../../design-system/Icon";
import { ThemeSelector } from "../../design-system/ThemeSelector";
import type { Navigate } from "../../hooks/useNavigation";
import { Navigation, navigationItems } from "./Navigation";
import { AppFooter, developer } from "./AppFooter";
import { Brand } from "./Brand";

const narrow = () =>
    window.matchMedia?.("(max-width: 1023px)").matches ?? false;
function subscribeWidth(notify: () => void) {
    const media = window.matchMedia?.("(max-width: 1023px)");
    media?.addEventListener("change", notify);
    return () => media?.removeEventListener("change", notify);
}
const sidebarPreferenceKey = "wms.sidebar.collapsed";
function readSidebarPreference() {
    try {
        return localStorage.getItem(sidebarPreferenceKey) === "true";
    } catch {
        return false;
    }
}
export function AppShell({
    page,
    navigate,
    contentKey,
    context,
    children,
    footer,
    exercise = false,
    administrator = false,
    userName,
    userRole,
    onLogout,
    onPassword,
}: {
    page: string;
    navigate: Navigate;
    contentKey: string;
    context: ReactNode;
    children: ReactNode;
    footer?: ReactNode;
    exercise?: boolean;
    administrator?: boolean;
    userName?: string;
    userRole?: string;
    onLogout?: () => void;
    onPassword?: () => void;
}) {
    const mobile = useSyncExternalStore(subscribeWidth, narrow);
    const [collapsed, setCollapsed] = useState(readSidebarPreference);
    const [drawerOpen, setDrawerOpen] = useState(false);
    const drawer = useRef<HTMLDialogElement>(null);
    const previousContent = useRef(contentKey);
    useLayoutEffect(() => {
        setDrawerOpen(false);
    }, [mobile]);
    useLayoutEffect(() => {
        if (previousContent.current !== contentKey) {
            drawer.current?.close();
            document.getElementById("conteudo")?.focus({ preventScroll: true });
            previousContent.current = contentKey;
        }
    }, [contentKey]);
    const title =
        page === "senha"
            ? "Minha senha"
            : (navigationItems(exercise, administrator).find(
                  (item) => item.id === page,
              )?.title ?? "Início");
    const sidebar = (
        <>
            <div className="sidebar-heading">
                <Brand onHome={() => navigate("inicio")} />
                {mobile && (
                    <button
                        type="button"
                        className="icon-button"
                        aria-label="Fechar menu"
                        onClick={() => drawer.current?.close()}
                    >
                        <Icon name="close" />
                    </button>
                )}
            </div>
            <p className="nav-section-label">Navegação</p>
            <Navigation
                page={page}
                navigate={navigate}
                exercise={exercise}
                administrator={administrator}
            />
            {!mobile && (
                <button
                    type="button"
                    className="sidebar-toggle"
                    aria-label={collapsed ? "Expandir menu" : "Minimizar menu"}
                    title={collapsed ? "Expandir menu" : "Minimizar menu"}
                    aria-controls="shell-navigation"
                    aria-expanded={!collapsed}
                    onClick={() => {
                        const value = !collapsed;
                        setCollapsed(value);
                        try {
                            localStorage.setItem(
                                sidebarPreferenceKey,
                                String(value),
                            );
                        } catch {
                            /* Preference remains usable for this visit. */
                        }
                    }}
                >
                    <Icon name={collapsed ? "chevron-right" : "chevron-left"} />
                    <span className="sidebar-toggle-label">Minimizar menu</span>
                </button>
            )}
            <div className="sidebar-footer">
                <Icon name={exercise ? "acesso" : "usuarios"} />
                <div className="sidebar-identity">
                    <strong>{userName ?? "Exercício fictício"}</strong>
                    <span>{userRole ?? "Ambiente de demonstração"}</span>
                </div>
            </div>
        </>
    );
    return (
        <div
            className={`app-shell${!mobile && collapsed ? " app-shell--collapsed" : ""}`}
        >
            <a
                className="skip"
                href="#conteudo"
                onClick={(event) => {
                    event.preventDefault();
                    document.getElementById("conteudo")?.focus();
                }}
            >
                Ir para o conteúdo
            </a>
            {mobile ? (
                <dialog
                    id="shell-navigation"
                    className="nav-drawer"
                    ref={drawer}
                    aria-label="Menu de navegação"
                    onClose={() => setDrawerOpen(false)}
                    onKeyDown={(event) => {
                        if (event.key !== "Tab") return;
                        const controls = Array.from(
                            event.currentTarget.querySelectorAll<HTMLElement>(
                                'button:not(:disabled), a[href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])',
                            ),
                        ).filter((item) => item.getClientRects().length > 0);
                        const first = controls[0],
                            last = controls.at(-1);
                        if (
                            (!event.shiftKey &&
                                document.activeElement === last) ||
                            (event.shiftKey && document.activeElement === first)
                        ) {
                            event.preventDefault();
                            (event.shiftKey ? last : first)?.focus();
                        }
                    }}
                    onClick={(event) => {
                        if (event.target === event.currentTarget)
                            drawer.current?.close();
                    }}
                >
                    {sidebar}
                </dialog>
            ) : (
                <aside id="shell-navigation" className="shell-sidebar">
                    {sidebar}
                </aside>
            )}
            <div className="shell-workspace">
                <header
                    className={"topbar" + (!exercise ? " topbar--context" : "")}
                >
                    <div className="topbar-location">
                        {mobile && (
                            <button
                                type="button"
                                className="icon-button"
                                aria-label="Abrir menu"
                                aria-controls="shell-navigation"
                                aria-expanded={drawerOpen}
                                onClick={() => {
                                    drawer.current?.showModal();
                                    setDrawerOpen(true);
                                }}
                            >
                                <Icon name="menu" />
                            </button>
                        )}
                        <div className="topbar-title">
                            <span>WMS Rodogarcia</span>
                            <strong>{title}</strong>
                        </div>
                        {exercise && (
                            <span className="environment-badge">
                                Exercício fictício
                            </span>
                        )}
                    </div>
                    {!exercise && (
                        <div className="topbar-context">{context}</div>
                    )}
                    <div className="topbar-actions">
                        <a
                            className="icon-button topbar-support"
                            href={developer.support}
                            aria-label="Falar com o suporte"
                            title="Falar com o suporte"
                        >
                            <Icon name="support" />
                        </a>
                        <ThemeSelector />
                        {onLogout && (
                            <>
                                <button
                                    type="button"
                                    className="account-action"
                                    aria-haspopup="dialog"
                                    onClick={
                                        onPassword ?? (() => navigate("senha"))
                                    }
                                >
                                    <Icon name="senha" />
                                    <span>Minha senha</span>
                                </button>
                                <button
                                    type="button"
                                    className="account-action"
                                    onClick={onLogout}
                                >
                                    <Icon name="logout" />
                                    <span>Sair</span>
                                </button>
                            </>
                        )}
                    </div>
                </header>
                {exercise && (
                    <p className="exercise-banner">
                        EXERCÍCIO FICTÍCIO · sem backend · sem SQL
                    </p>
                )}
                {exercise && <div className="shell-context">{context}</div>}
                <main id="conteudo" tabIndex={-1} key={contentKey}>
                    {children}
                </main>
                {footer}
                <AppFooter />
            </div>
        </div>
    );
}
