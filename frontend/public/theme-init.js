// Executado antes do CSS, sem script inline ou acesso a dados de sessão.
(() => {
    const root = document.documentElement;
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const storageKey = "wms.theme";
    const valid = (value) => ["light", "dark", "system"].includes(value);
    const storedPreference = () => {
        try {
            const value = window.localStorage.getItem(storageKey);
            return value === "light" || value === "dark" ? value : "system";
        } catch {
            return "system";
        }
    };
    const apply = (preference) => {
        root.dataset.wmsThemePreference = preference;
        root.dataset.ogTheme =
            preference === "system"
                ? media.matches
                    ? "dark"
                    : "light"
                : preference;
        window.dispatchEvent(new Event("wms:theme-applied"));
    };
    apply(storedPreference());
    media.addEventListener("change", () => {
        if (root.dataset.wmsThemePreference === "system") apply("system");
    });
    window.addEventListener("wms:theme-change", () => {
        const preference = root.dataset.wmsThemePreference;
        apply(valid(preference) ? preference : "system");
        if (preference === "light" || preference === "dark") {
            try {
                window.localStorage.setItem(storageKey, preference);
            } catch {
                // A escolha ainda vale nesta página quando o navegador bloqueia armazenamento.
            }
        }
    });
    window.addEventListener("storage", (event) => {
        if (event.key === storageKey || event.key === null)
            apply(storedPreference());
    });
})();
