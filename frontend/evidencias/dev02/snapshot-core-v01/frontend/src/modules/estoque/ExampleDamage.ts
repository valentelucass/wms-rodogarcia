import { fixture } from "../../api/mock/fixtures";
import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";

// Estágios autorais do exemplo. Sem disponibilidade, proporção ou cálculo local.
export class ExampleDamage {
    private stock: Values | undefined;
    private damage: Values | undefined;
    respond(r: Request, body: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        if (id === "EstoqueController.consultar" && isObject(data)) {
            const previous = this.stock?.unidade as Values | undefined;
            if (previous?.codigo !== r.params.codigo) this.damage = undefined;
            if (!this.damage) this.stock = structuredClone(data);
            return this.stock;
        }
        if (id === "AvariaController.listar")
            return this.damage ? [this.damage] : [];
        if (!isObject(data)) return data;
        if (id === "AvariaController.registrar") {
            this.damage = {
                ...(fixture("AvariaDto.Ocorrencia") as Values),
                id: "3101",
                versao: "0",
                unidadeId:
                    (this.stock?.unidade as Values | undefined)?.id ?? "501",
                quantidade: "5.000000",
                ocorridaEm: body.ocorridaEm,
                relato: body.motivo,
                responsabilidade: null,
                reconhecidaEm: null,
                validadaPor: null,
                tratativa: null,
                resolvidaEm: null,
                proporcaoSuspensa: "0.000000",
                inicioSuspensao: null,
            };
        }
        if (id === "AvariaController.reconhecer" && this.damage)
            this.damage = {
                ...this.damage,
                versao: "1",
                responsabilidade: body.responsabilidade,
                reconhecidaEm: "2026-10-08T13:30:00.123456Z",
                validadaPor: "gestor.ficticio",
                proporcaoSuspensa: "0.500000",
                inicioSuspensao: "2026-10-08T13:00:00.123456Z",
            };
        if (id === "AvariaController.reparar" && this.damage)
            this.damage = {
                ...this.damage,
                versao: "2",
                tratativa: body.motivo,
                resolvidaEm: "2026-10-08T14:00:00.123456Z",
            };
        if (id.startsWith("AvariaController.") && this.damage) {
            this.stock ??= fixture("EstoqueDto.Unidade") as Values;
            const unit = this.stock.unidade as Values;
            this.stock = {
                ...this.stock,
                unidade: {
                    ...unit,
                    versao: id.endsWith("registrar")
                        ? "8"
                        : id.endsWith("reparar")
                          ? "9"
                          : unit.versao,
                },
                bloqueada: true,
                avariaPosterior: !id.endsWith("reparar"),
            };
            return { ...data, avaria: this.damage, estoque: this.stock };
        }
        if (id === "EstoqueController.liberar" && this.stock) {
            this.stock = {
                ...this.stock,
                bloqueada: false,
                unidade: { ...(this.stock.unidade as Values), versao: "10" },
            };
            return { ...data, estoque: this.stock, avaria: null };
        }
        if (id === "EstoqueController.posicionar" && isObject(data.estoque))
            this.stock = structuredClone(data.estoque);
        return data;
    }
}
