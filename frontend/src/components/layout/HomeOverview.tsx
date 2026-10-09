import { Icon } from "../../design-system/Icon";
import type { Navigate } from "../../hooks/useNavigation";

const shortcuts = [
    ["entrada", "Receber e conferir", "Pedidos, notas e conferência física."],
    [
        "coletor",
        "Ler e endereçar",
        "Unidades, posições e leitura de etiquetas.",
    ],
    ["saida", "Reservar e separar", "FIFO, reservas e preparação da saída."],
    [
        "fechamento",
        "Conferir fechamento",
        "Serviços, valores e demonstrativos.",
    ],
];
export function HomeOverview({ navigate }: { navigate: Navigate }) {
    return (
        <section
            className="home-shortcuts"
            aria-labelledby="home-shortcuts-title"
        >
            <h2 id="home-shortcuts-title">Acesso rápido</h2>
            <div className="shortcut-grid">
                {shortcuts.map(([page, title, description]) => (
                    <button
                        key={page}
                        type="button"
                        className="shortcut-card"
                        onClick={() => navigate(page)}
                    >
                        <span className="shortcut-icon">
                            <Icon name={page} />
                        </span>
                        <span>
                            <strong>{title}</strong>
                            <span>{description}</span>
                        </span>
                        <Icon name="arrow" />
                    </button>
                ))}
            </div>
        </section>
    );
}
