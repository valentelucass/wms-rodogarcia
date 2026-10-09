import { test, expect } from "./support";
test("FE-PRU-SKIP01 teclado e mouse preservam rota titulo e formulario",async({page})=>{
    await page.goto("/#entrada");await page.getByRole("button",{name:"Novo registro",exact:true}).click();
    await page.getByLabel("Referência *",{exact:true}).fill("EDICAO-PRESERVADA-SKIP");
    const url=page.url();const skip=page.getByRole("link",{name:"Ir para o conteúdo"});
    for(let i=0;i<40 && !(await skip.evaluate(el=>el===document.activeElement));i++) await page.keyboard.press("Shift+Tab");
    await page.keyboard.press("Shift+Tab");await page.keyboard.press("Tab");await expect(skip).toBeFocused();await page.keyboard.press("Enter");
    await expect(page.locator("#conteudo")).toBeFocused();expect(page.url()).toBe(url);await expect(page.getByRole("heading",{name:"Entrada e conferência",exact:true})).toBeVisible();await expect(page.getByLabel("Referência *",{exact:true})).toHaveValue("EDICAO-PRESERVADA-SKIP");
    await skip.focus();await skip.click();await expect(page.locator("#conteudo")).toBeFocused();expect(page.url()).toBe(url);await expect(page.getByLabel("Referência *",{exact:true})).toHaveValue("EDICAO-PRESERVADA-SKIP");
});
