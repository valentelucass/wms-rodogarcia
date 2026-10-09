import { useSyncExternalStore } from "react";

export type ThemePreference = "light" | "dark" | "system";
function current() {
    const value = document.documentElement.dataset.wmsThemePreference;
    const preference = value === "light" || value === "dark" ? value : "system";
    const resolved =
        document.documentElement.dataset.ogTheme === "dark" ? "dark" : "light";
    return `${preference}:${resolved}`;
}
function subscribe(notify: () => void) {
    window.addEventListener("wms:theme-applied", notify);
    return () => window.removeEventListener("wms:theme-applied", notify);
}
export function useTheme() {
    const snapshot = useSyncExternalStore(subscribe, current);
    const [preference, resolved] = snapshot.split(":") as [
        ThemePreference,
        "light" | "dark",
    ];
    const setPreference = (value: "light" | "dark") => {
        document.documentElement.dataset.wmsThemePreference = value;
        window.dispatchEvent(new Event("wms:theme-change"));
    };
    return { preference, resolved, setPreference };
}
