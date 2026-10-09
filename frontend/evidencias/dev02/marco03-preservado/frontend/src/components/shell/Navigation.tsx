import { journeys } from "../../domain/journeys";
import type { Navigate } from "../../hooks/useNavigation";
export function Navigation({
    page,
    navigate,
}: {
    page: string;
    navigate: Navigate;
}) {
    const links = [
        { id: "inicio", title: "Início" },
        ...journeys,
        { id: "coletor", title: "Coletor" },
        { id: "acesso", title: "Acesso e limites" },
    ];
    return (
        <nav className="main-nav" aria-label="Módulos">
            {links.map((link) => (
                <button
                    key={link.id}
                    onClick={() => navigate(link.id)}
                    aria-current={page === link.id ? "page" : undefined}
                >
                    {link.title}
                </button>
            ))}
        </nav>
    );
}
