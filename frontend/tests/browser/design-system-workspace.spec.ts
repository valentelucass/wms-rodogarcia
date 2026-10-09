import { test, expect } from "./support";
import { journeys as cadastros } from "../../src/modules/cadastros/definition";
import { journeys as recebimento } from "../../src/modules/recebimento/definition";
import { journeys as estoque } from "../../src/modules/estoque/definition";
import { journeys as saida } from "../../src/modules/saida/definition";
import { journeys as financeiro } from "../../src/modules/financeiro/definition";
import { journeys as regularizacao } from "../../src/modules/regularizacao/definition";
import { journeys as relatorios } from "../../src/modules/relatorios/definition";
import { chooseTheme } from "./theme-controls";
const journeys = [
    ...cadastros,
    ...recebimento,
    ...estoque,
    ...saida,
    ...financeiro,
    ...regularizacao,
    ...relatorios,
];
test("FE02-DS03 sidebar conserva expansão, minimização e tema após refresh", async ({
    page,
}) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto("/");
    await chooseTheme(page, "dark");
    await page
        .getByRole("button", { name: "Minimizar menu", exact: true })
        .click();
    await page.reload();
    await expect(
        page.getByRole("button", { name: "Expandir menu", exact: true }),
    ).toBeVisible();
    await expect(page.locator("html")).toHaveAttribute("data-og-theme", "dark");
    await page.setViewportSize({ width: 390, height: 900 });
    await expect(
        page.getByRole("button", { name: "Abrir menu", exact: true }),
    ).toBeVisible();
    await page.reload();
    await page.setViewportSize({ width: 1440, height: 900 });
    await expect(
        page.getByRole("button", { name: "Expandir menu", exact: true }),
    ).toBeVisible();
    await page
        .getByRole("button", { name: "Expandir menu", exact: true })
        .click();
    await page.reload();
    await expect(
        page.getByRole("button", { name: "Minimizar menu", exact: true }),
    ).toBeVisible();
});
test("FE02-DS03 sidebar funciona sem armazenamento disponível", async ({
    page,
}) => {
    await page.addInitScript(() => {
        Storage.prototype.getItem = () => {
            throw new Error("Armazenamento indisponível");
        };
        Storage.prototype.setItem = () => {
            throw new Error("Armazenamento indisponível");
        };
    });
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto("/");
    await page
        .getByRole("button", { name: "Minimizar menu", exact: true })
        .click();
    await expect(
        page.getByRole("button", { name: "Expandir menu", exact: true }),
    ).toBeVisible();
});

for (const width of [320, 390, 768, 1024, 1440]) {
    test(`FE02-DS03 páginas internas claro e escuro em ${width}px`, async ({
        page,
    }, info) => {
        await page.setViewportSize({ width, height: 900 });
        const errors: string[] = [];
        page.on("pageerror", (err) => errors.push(err.message));
        await page.goto("/");
        for (const theme of ["light", "dark"] as const) {
            await chooseTheme(page, theme);
            const modules = [
                { id: "inicio", title: "Início da operação" },
                ...journeys,
                { id: "coletor", title: "Coletor" },
            ];
            for (const module of modules) {
                await page.goto("/#" + module.id);
                await expect(
                    page
                        .getByRole("heading", {
                            name: module.title,
                            exact: true,
                        })
                        .first(),
                ).toBeVisible();
                const overflow = await page.evaluate(
                    () =>
                        document.documentElement.scrollWidth >
                        document.documentElement.clientWidth + 1,
                );
                expect(overflow, module.title + " / " + theme).toBe(false);
                if (module.id === "cadastros") {
                    if (width >= 1024) {
                        const form = await page
                            .locator(".stage-panel")
                            .boundingBox();
                        const nav = await page
                            .locator(".journey-navigation")
                            .boundingBox();
                        expect(form!.x).toBeGreaterThan(nav!.x + nav!.width);
                        expect(Math.abs(form!.y - nav!.y)).toBeLessThan(1);
                    }
                    await page.screenshot({
                        path: info.outputPath(`cadastros-${theme}.png`),
                        fullPage: true,
                    });
                }
                if (module.id === "inicio" || module.id === "coletor")
                    await page.screenshot({
                        path: info.outputPath(`${module.id}-${theme}.png`),
                        fullPage: true,
                    });
            }
        }
        expect(errors).toEqual([]);
    });
}
test("FE02-DS03 confirmação central, Escape e referência consultada sem deslocar o formulário", async ({
    page,
}, info) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto("/#cadastros");
    await page
        .getByRole("button", { name: "Novo registro", exact: true })
        .click();
    await page.getByLabel("Código *", { exact: true }).fill("DS03");
    await page
        .getByLabel("Nome *", { exact: true })
        .fill("Cliente de conferência visual");
    await page
        .getByLabel("CPF / CNPJ *", { exact: true })
        .fill("00000000000000");
    const trigger = page.getByRole("button", {
        name: "Conferir e confirmar",
        exact: true,
    });
    await trigger.click();
    const dialog = page.getByRole("dialog");
    await expect(dialog).toBeVisible();
    await expect(
        dialog.getByText("Cliente de conferência visual", { exact: true }),
    ).toBeVisible();
    const box = await dialog.boundingBox();
    expect(Math.abs(box!.x + box!.width / 2 - 720)).toBeLessThan(2);
    expect(Math.abs(box!.y + box!.height / 2 - 450)).toBeLessThan(2);
    for (let i = 0; i < 8; i++) {
        await page.keyboard.press("Tab");
        expect(
            await dialog.evaluate((el) => el.contains(document.activeElement)),
        ).toBe(true);
    }
    await page.screenshot({ path: info.outputPath("confirmacao.png") });
    await page.keyboard.press("Escape");
    await expect(dialog).toHaveCount(0);
    await expect(trigger).toBeFocused();
    await expect(page.getByLabel("Nome *", { exact: true })).toHaveValue(
        "Cliente de conferência visual",
    );
    await page.goto("/#entrada");
    const operation = page.locator(".stage-panel");
    const before = await operation.boundingBox();
    await page
        .getByRole("region", { name: "Consultar referências para a tarefa" })
        .getByRole("button")
        .first()
        .click();
    const refs = page.getByRole("dialog", { name: "Referências consultadas" });
    await expect(refs).toBeVisible();
    await page.screenshot({ path: info.outputPath("referencias.png") });
    await page.keyboard.press("Escape");
    await expect(refs).toHaveCount(0);
    const after = await operation.boundingBox();
    expect(Math.abs(after!.y - before!.y)).toBeLessThan(1);
});
