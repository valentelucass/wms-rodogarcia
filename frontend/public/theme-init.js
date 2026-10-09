// Executado antes do CSS, sem script inline ou acesso a dados de sessão.
(() => {
    const root = document.documentElement;
    const media = window.matchMedia("(prefers-color-scheme: dark)");
    const valid = (value) => ["light", "dark", "system"].includes(value);
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
    apply("system");
    media.addEventListener("change", () => {
        if (root.dataset.wmsThemePreference === "system") apply("system");
    });
    window.addEventListener("wms:theme-change", () => {
        const preference = root.dataset.wmsThemePreference;
        apply(valid(preference) ? preference : "system");
    });
})();
