import { journeys } from "../../domain/journeys";
import type { Navigate } from "../../hooks/useNavigation";
import { Icon } from "../../design-system/Icon";
import { useEffect, useRef } from "react";
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
    const navigation = useRef<HTMLElement>(null);
    useEffect(() => {
        const menu = navigation.current;
        const selected = menu?.querySelector('[aria-current="page"]');
        if (!menu || !selected) return;
        const viewport = menu.getBoundingClientRect();
        const item = selected.getBoundingClientRect();
        if (item.top < viewport.top) menu.scrollTop += item.top - viewport.top;
        else if (item.bottom > viewport.bottom)
            menu.scrollTop += item.bottom - viewport.bottom;
    }, [page]);
    return (
        <nav ref={navigation} className="main-nav" aria-label="Módulos">
            {links.map((link) => (
                <button
                    type="button"
                    key={link.id}
                    title={link.title}
                    aria-label={link.title}
                    onClick={() => navigate(link.id)}
                    aria-current={page === link.id ? "page" : undefined}
                >
                    <Icon name={link.id} />
                    <span className="nav-label">{link.title}</span>
                </button>
            ))}
        </nav>
    );
}
