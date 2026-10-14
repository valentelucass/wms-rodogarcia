import { useState } from "react";
import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { UnitTypesField } from "../src/components/fields/UnitTypesField";
import {
    selectedUnitTypes,
    permittedUnitTypesLabel,
} from "../src/domain/unitTypes";
import { scalarText } from "../src/domain/referencePresentation";
import { records, enums } from "../src/contracts/runtime";
import { validateValue } from "../src/contracts/codec";
import { fixture } from "../src/api/mock/fixtures";
import { FictitiousTransport } from "../src/api/fictitious";
import {
    Harness,
    click,
    consult,
    fill,
    confirm,
    body,
    lastMutation,
} from "./support/operationalHarness";

function Editor({
    initial = null,
    disabled = false,
}: {
    initial?: unknown;
    disabled?: boolean;
}) {
    const [value, setValue] = useState(initial);
    return (
        <>
            <UnitTypesField
                name="Tipos de unidade permitidos"
                value={value}
                onChange={setValue}
                disabled={disabled}
                required={false}
            />
            <output aria-label="Perfil enviado">{String(value)}</output>
        </>
    );
}

describe("Tipos de unidade permitidos na posição", () => {
    it.each([
        "PALLET",
        "BOBINA",
        "VOLUME",
        "PALLET_BOBINA",
        "PALLET_VOLUME",
        "BOBINA_VOLUME",
        "TODAS",
    ])("reabre %s com os tipos cadastrados", (profile) => {
        render(<Editor initial={profile} />);
        const selected = selectedUnitTypes(profile);
        for (const [type, name] of [
            ["PALLET", "Pallet"],
            ["BOBINA", "Bobina"],
            ["VOLUME", "Volume"],
        ])
            expect(screen.getByRole("checkbox", { name })).toHaveProperty(
                "checked",
                selected.includes(type as "PALLET" | "BOBINA" | "VOLUME"),
            );
        expect(
            screen.getByRole("checkbox", {
                name: "Todas as opções",
            }),
        ).toHaveProperty("checked", profile === "TODAS");
    });
    it("seleciona combinações, sincroniza todas e retira um tipo", () => {
        render(<Editor />);
        const choose = (name: string) =>
            fireEvent.click(screen.getByRole("checkbox", { name }));
        choose("Pallet");
        choose("Bobina");
        expect(screen.getByLabelText("Perfil enviado")).toHaveTextContent(
            "PALLET_BOBINA",
        );
        choose("Volume");
        expect(
            screen.getByRole("checkbox", { name: "Todas as opções" }),
        ).toBeChecked();
        expect(screen.getByLabelText("Perfil enviado")).toHaveTextContent(
            "TODAS",
        );
        choose("Bobina");
        expect(screen.getByLabelText("Perfil enviado")).toHaveTextContent(
            "PALLET_VOLUME",
        );
        expect(
            screen.getByRole("checkbox", { name: "Todas as opções" }),
        ).not.toBeChecked();
        choose("Todas as opções");
        choose("Todas as opções");
        expect(
            screen.getByRole("checkbox", { name: "Pallet" }),
        ).not.toBeChecked();
        expect(screen.getByLabelText("Perfil enviado")).toHaveTextContent(
            "null",
        );
    });
    it("mantém a seleção bloqueada durante envio", () => {
        const onChange = vi.fn();
        render(
            <UnitTypesField
                name="Tipos de unidade permitidos"
                value="BOBINA"
                onChange={onChange}
                disabled
                required
            />,
        );
        expect(screen.getByRole("group")).toBeDisabled();
        for (const control of screen.getAllByRole("checkbox"))
            expect(control).toBeDisabled();
    });
    it.each([
        "EnderecoDto.Criar",
        "EnderecoDto.Alterar",
        "CapacidadeDto.ConfigurarEndereco",
    ])("contrato %s aceita todas e combinações", (schema) => {
        const limits = {
            pesoKg: "2000.000",
            alturaMetros: "2.000",
            larguraMetros: "2.000",
            profundidadeMetros: "2.000",
            empilhamentoMaximo: 1,
        };
        const candidate = {
            ...(fixture(schema) as object),
            capacidadePesoKg: "2000.000",
            alturaMetros: "2.000",
            larguraMetros: "2.000",
            profundidadeMetros: "2.000",
            limites: limits,
        };
        const data = Object.fromEntries(
            records[schema].map((field) => [
                field.name,
                candidate[field.name as keyof typeof candidate],
            ]),
        );
        expect(
            records[schema].find((f) => f.name === "tipoUnidadePermitido")
                ?.type,
        ).toBe("TiposUnidadePermitidos");
        for (const profile of ["TODAS", "PALLET_BOBINA", "BOBINA_VOLUME"]) {
            expect(() =>
                validateValue(
                    schema,
                    {
                        ...data,
                        tipoUnidadePermitido: profile,
                    },
                    "posição",
                ),
            ).not.toThrow();
        }
        expect(() =>
            validateValue(
                schema,
                {
                    ...data,
                    tipoUnidadePermitido: "DESCONHECIDO",
                },
                "posição",
            ),
        ).toThrow();
    });
    it("exige tipo na configuração e preserva tipos das unidades físicas", () => {
        expect(() =>
            validateValue(
                "CapacidadeDto.ConfigurarEndereco",
                {
                    ...(fixture("CapacidadeDto.ConfigurarEndereco") as object),
                    tipoUnidadePermitido: null,
                },
                "posição",
            ),
        ).toThrow();
        expect(enums.TipoUnidadeLogistica).toEqual([
            "PALLET",
            "BOBINA",
            "VOLUME",
        ]);
    });
    it("exibe nomes legíveis nos detalhes e conferência", () => {
        expect(permittedUnitTypesLabel("TODAS")).toBe("Todas as opções");
        expect(
            scalarText(
                "tipoUnidadePermitido",
                "PALLET_BOBINA",
                {},
                "EnderecoDto.Resposta",
            ),
        ).toBe("Pallet, Bobina");
        expect(permittedUnitTypesLabel(null)).toBe("Não informado");
    });
    it("envia a combinação pelo editor oficial com motivo e revisão", async () => {
        const transport = new FictitiousTransport();
        render(
            <Harness
                transport={transport}
                page="cadastros"
                action="EnderecoController.listar"
            />,
        );
        await consult();
        await click("Selecionar registro 1");
        await click("Editar");
        fireEvent.click(screen.getByRole("checkbox", { name: "Bobina" }));
        fill(
            "Motivo / justificativa *",
            "Permitir pallet e bobina nesta posição",
        );
        await confirm();
        expect(body(lastMutation(transport.requests))).toMatchObject({
            tipoUnidadePermitido: "PALLET_BOBINA",
            versao: "0",
            motivo: "Permitir pallet e bobina nesta posição",
        });
    });
});
