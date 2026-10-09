import { test as base, expect, type Page } from "@playwright/test";
import { writeFileSync } from "node:fs";
import { parse, isLosslessNumber } from "lossless-json";
import { origin, proofLabel } from "./environment";
export interface Recorded {
    kind: string;
    id: string;
    params?: Record<string, string>;
    query?: Record<string, unknown>;
    wire?: string;
    raw?: string;
    file?: { name: string; size: number };
}
export const test = base.extend<{ isolated: void }>({
    isolated: [
        async ({ page }, use, info) => {
            const requests: string[] = [];
            await page.addInitScript(() => {
                const traces: unknown[] = [];
                Object.defineProperty(window, "__wmsOperationalTrace", {
                    value: traces,
                });
                for (const kind of ["request", "receipt"])
                    window.addEventListener("wms:fictitious-" + kind, (event) =>
                        traces.push({ kind, ...(event as CustomEvent).detail }),
                    );
            });
            await page.route("**/*", (route) => {
                const url = route.request().url();
                requests.push(url);
                return new URL(url).origin === origin &&
                    !new URL(url).pathname.startsWith("/api/")
                    ? route.continue()
                    : route.abort("blockedbyclient");
            });
            await use();
            const trace = await page.evaluate(
                () =>
                    (window as unknown as { __wmsOperationalTrace: Recorded[] })
                        .__wmsOperationalTrace ?? [],
            );
            writeFileSync(
                "evidencias/" +
                    proofLabel +
                    "-ui-" +
                    info.title.replace(/[^a-zA-Z0-9]/g, "_") +
                    ".json",
                JSON.stringify(
                    {
                        test: info.title,
                        status: info.status,
                        trace,
                        requests,
                        externalRequests: requests.filter(
                            (x) => new URL(x).origin !== origin,
                        ),
                        backendCalled: requests.some((x) =>
                            new URL(x).pathname.startsWith("/api/"),
                        ),
                    },
                    null,
                    2,
                ),
            );
        },
        { auto: true },
    ],
});
export { expect };
export async function recorded(
    page: Page,
    id: string,
    kind = "request",
): Promise<Recorded> {
    const found = await page.evaluate(
        ({ id, kind }) =>
            (
                window as unknown as { __wmsOperationalTrace: Recorded[] }
            ).__wmsOperationalTrace
                .filter((x) => x.id === id && x.kind === kind)
                .at(-1),
        { id, kind },
    );
    expect(found).toBeDefined();
    return found!;
}
export const command = (r: Pick<Recorded, "wire">) =>
    parse(r.wire ?? "null") as Record<string, unknown>;
export const exact = (v: unknown) => (isLosslessNumber(v) ? v.value : v);
export async function confirm(page: Page) {
    await page
        .getByRole("button", { name: "Conferir e confirmar", exact: true })
        .click();
    await expect(
        page.getByRole("region", { name: "Confirmação da ação" }),
    ).toBeVisible();
    await page
        .getByRole("button", { name: "Confirmar agora", exact: true })
        .click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
}
export async function consult(page: Page) {
    await page
        .getByRole("button", { name: "Consultar", exact: true })
        .last()
        .click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/).last(),
    ).toBeVisible();
}
