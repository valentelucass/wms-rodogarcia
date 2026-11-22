import { readFileSync } from "node:fs";
import { join } from "node:path";
import type { Page } from "@playwright/test";

export function journeyWorkspace(page: Page, id: string) {
    if (id === "fiscal")
        return page.getByRole("region", { name: "Retirada fiscal da mercadoria" });
    if (id === "fechamento" || id === "cobranca")
        return page.getByRole("region", { name: "Últimos fechamentos" });
    if (id === "contagem")
        return page.getByRole("region", { name: "Visão geral do inventário" });
    return page.locator(".record-workspace");
}

export function offlineRelease(build: string) {
    const { runId, builtUtc } = JSON.parse(
        readFileSync(join(build, "build-manifest.json"), "utf8"),
    );
    return { runId, activatedUtc: builtUtc };
}
