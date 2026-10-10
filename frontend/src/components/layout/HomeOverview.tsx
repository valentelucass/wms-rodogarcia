import { Icon } from "../../design-system/Icon";
import type { Navigate } from "../../hooks/useNavigation";
import { information, shortcutInformation } from "../../content/information";
import { InformationCard } from "../information/InformationPopup";

const shortcuts = ["entrada", "coletor", "saida", "fechamento"] as const;
export function HomeOverview({ navigate }: { navigate: Navigate }) {
    return (
        <section
            className="home-shortcuts"
            aria-labelledby="home-shortcuts-title"
        >
            <h2 id="home-shortcuts-title" className="sr-only">
                Acesso rápido
            </h2>
            <div className="shortcut-grid">
                {shortcuts.map((page) => (
                    <InformationCard
                        as="button"
                        content={information[shortcutInformation[page]]}
                        key={page}
                        className={`shortcut-card shortcut-card--${page}`}
                        onClick={() => navigate(page)}
                    >
                        <span className="shortcut-icon">
                            <Icon name={page} />
                        </span>
                        <strong>
                            {information[shortcutInformation[page]].title}
                        </strong>
                        <span className="sr-only">
                            {information[shortcutInformation[page]].description}
                        </span>
                    </InformationCard>
                ))}
            </div>
        </section>
    );
}
