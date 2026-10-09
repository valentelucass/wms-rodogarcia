import { describe, expect, it } from "vitest";
import { productionConfiguration, productionCsp } from "../vite.prod.config";

describe("build de produção dedicado", () => {
    it("aceita somente production, sem modo fictício/dev", () => {
        for (const mode of ["ficticio", "real", "development", ""]) {
            expect(() => productionConfiguration(mode)).toThrow(
                "WMS_PROD_BUILD_MODE_INVALID",
            );
        }
    });
    it("real explícito, sem .env/VITE herdado, sourcemap/limpeza/Vite server", () => {
        const config = productionConfiguration("production");
        expect(config.envDir).toBe(false);
        expect(config.envPrefix).toEqual([]);
        expect(config.define?.["import.meta.env.VITE_DATA_MODE"]).toBe(
            '"real"',
        );
        expect(config.build?.sourcemap).toBe(false);
        expect(config.build?.emptyOutDir).toBe(false);
        expect(config.server).toBeUndefined();
        expect(productionCsp).not.toMatch(/unsafe-inline|ws:|\*/);
    });
});
