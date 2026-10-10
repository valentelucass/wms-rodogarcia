import { journeys } from "../../domain/journeys";
import type { Navigate } from "../../hooks/useNavigation";
import { Icon } from "../../design-system/Icon";
import { useEffect, useId, useRef, useState } from "react";
export function navigationItems(exercise: boolean, administrator: boolean) {
    return [
        { id: "inicio", title: "Início" },
        ...journeys,
        { id: "coletor", title: "Coletor" },
        ...(exercise ? [{ id: "acesso", title: "Acesso e limites" }] : []),
        ...(administrator
            ? [{ id: "usuarios", title: "Usuários e acessos" }]
            : []),
    ];
}
export function Navigation({
    page,
    navigate,
    exercise = true,
    administrator = false,
}: {
    page: string;
    navigate: Navigate;
    exercise?: boolean;
    administrator?: boolean;
}) {
    const links = navigationItems(exercise, administrator);
    const id = useId();
    const container = useRef<HTMLDivElement>(null);
    const content = useRef<HTMLDivElement>(null);
    const navigation = useRef<HTMLElement>(null);
    const [edges, setEdges] = useState({
        overflow: false,
        up: false,
        down: false,
    });
    const reveal = (item: Element) => {
        const menu = navigation.current;
        if (!menu) return;
        const viewport = menu.getBoundingClientRect();
        const bounds = item.getBoundingClientRect();
        if (bounds.top < viewport.top + 20)
            menu.scrollTop += bounds.top - viewport.top - 20;
        else if (bounds.bottom > viewport.bottom - 20)
            menu.scrollTop += bounds.bottom - viewport.bottom + 20;
    };
    useEffect(() => {
        const menu = navigation.current;
        const frame = container.current;
        const items = content.current;
        if (!menu || !frame || !items) return;
        const update = () => {
            const next = {
                overflow:
                    frame.clientHeight > 0 &&
                    items.scrollHeight > frame.clientHeight + 1,
                up: menu.scrollTop > 1,
                down:
                    menu.scrollTop + menu.clientHeight < menu.scrollHeight - 1,
            };
            setEdges((previous) =>
                previous.overflow === next.overflow &&
                previous.up === next.up &&
                previous.down === next.down
                    ? previous
                    : next,
            );
        };
        const resize = () => {
            const selected = menu.contains(document.activeElement)
                ? document.activeElement
                : menu.querySelector('[aria-current="page"]');
            if (selected) reveal(selected);
            update();
        };
        resize();
        const observer =
            typeof ResizeObserver === "undefined"
                ? undefined
                : new ResizeObserver(resize);
        observer?.observe(frame);
        observer?.observe(items);
        observer?.observe(menu);
        menu.addEventListener("scroll", update, { passive: true });
        return () => {
            observer?.disconnect();
            menu.removeEventListener("scroll", update);
        };
    }, [page, exercise, administrator]);
    const move = (direction: number) => {
        const menu = navigation.current;
        if (!menu) return;
        menu.scrollBy({
            top: direction * Math.max(44, menu.clientHeight - 44),
            behavior: window.matchMedia?.("(prefers-reduced-motion: reduce)")
                .matches
                ? "instant"
                : "smooth",
        });
    };
    return (
        <div ref={container} className="nav-scroll">
            {edges.overflow && (
                <button
                    type="button"
                    className="nav-scroll-arrow nav-scroll-arrow--up"
                    aria-label="Mostrar módulos anteriores"
                    aria-controls={id}
                    disabled={!edges.up}
                    onClick={() => move(-1)}
                >
                    <Icon name="chevron-down" />
                </button>
            )}
            <div
                className="nav-scroll-window"
                data-up={edges.overflow && edges.up}
                data-down={edges.overflow && edges.down}
            >
                <nav
                    id={id}
                    ref={navigation}
                    className="main-nav"
                    aria-label="Módulos"
                    onFocusCapture={(event) => reveal(event.target)}
                >
                    <div ref={content} className="main-nav-items">
                        {links.map((link) => (
                            <button
                                type="button"
                                key={link.id}
                                title={link.title}
                                aria-label={link.title}
                                onClick={() => navigate(link.id)}
                                aria-current={
                                    page === link.id ? "page" : undefined
                                }
                            >
                                <Icon name={link.id} />
                                <span className="nav-label">{link.title}</span>
                            </button>
                        ))}
                    </div>
                </nav>
            </div>
            {edges.overflow && (
                <button
                    type="button"
                    className="nav-scroll-arrow"
                    aria-label="Mostrar próximos módulos"
                    aria-controls={id}
                    disabled={!edges.down}
                    onClick={() => move(1)}
                >
                    <Icon name="chevron-down" />
                </button>
            )}
        </div>
    );
}
