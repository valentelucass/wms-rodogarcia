import { expect, type Page } from "@playwright/test";

export async function chooseTheme(page: Page, theme: "light" | "dark") {
    if ((await page.locator("html").getAttribute("data-og-theme")) !== theme) {
        await page
            .getByRole("button", {
                name: `Ativar tema ${theme === "light" ? "claro" : "escuro"}`,
                exact: true,
            })
            .click();
    }
    await expect(page.locator("html")).toHaveAttribute("data-og-theme", theme);
}
