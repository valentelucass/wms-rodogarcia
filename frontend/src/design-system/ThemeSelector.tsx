import { Icon } from "./Icon";
import { useTheme } from "./theme";
import { useId } from "react";

export function ThemeSelector() {
    const { preference, resolved, setPreference } = useTheme();
    const description = useId();
    const next = resolved === "dark" ? "light" : "dark";
    const action = next === "dark" ? "Ativar tema escuro" : "Ativar tema claro";
    return (
        <div className="theme-selector" role="group" aria-label="Tema">
            <button
                type="button"
                className="theme-button"
                aria-label={action}
                title={action}
                aria-describedby={description}
                onClick={() => setPreference(next)}
            >
                <Icon name={next === "dark" ? "moon" : "sun"} />
            </button>
            <span id={description} className="sr-only" aria-live="polite">
                Tema {resolved === "dark" ? "escuro" : "claro"}
                {preference === "system"
                    ? " conforme o sistema"
                    : " escolhido manualmente"}
                .
            </span>
        </div>
    );
}
