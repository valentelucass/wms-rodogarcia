import { fireEvent, render, screen, within } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Result } from "../src/components/Result";
import { FormReferences } from "../src/components/FormReferences";
import { information } from "../src/content/information";
import {
    auditBefore,
    auditAfter,
    auditDetailSample,
    auditDetailReferences,
} from "./support/auditDetail";

it("separa a alteração e preserva números exatos, conteúdo original e seleção da auditoria", () => {
    const onSelect = vi.fn();
    const data = Object.freeze({ ...auditDetailSample });
    render(
        <FormReferences.Provider value={auditDetailReferences}>
            <Result data={data} type="AuditoriaResponse" onSelect={onSelect} />
        </FormReferences.Provider>,
    );
    expect(
        screen.getByRole("heading", { name: "Conciliação da contingência" }),
    ).toBeVisible();
    expect(
        screen.getByText("QA-PLANILHA-CONT-L1", { selector: "dd" }),
    ).toBeVisible();
    const before = screen.getByLabelText("Conteúdo: Antes da alteração");
    expect(before.textContent).toContain('"id": 9223372036854775807');
    expect(before.textContent).toContain('"contado": 5.000001');
    expect(before.textContent).toContain('"versao": 0');
    expect(before.textContent).toContain('"efeitoRegistradoNoWms": false');
    expect(before.textContent).toContain('"conciliadaEm": null');
    expect(before.textContent).toContain("2026-10-16T01:30:00.123456789Z");
    const after = screen.getByLabelText("Conteúdo: Depois da alteração");
    expect(after.textContent).toContain('"diferenca": 0.000000');
    expect(after.textContent).toContain('"id": 9007199254740993');
    const beforePanel = screen.getByRole("region", {
        name: "Antes da alteração",
    });
    fireEvent.click(within(beforePanel).getByText("Ver JSON original"));
    expect(
        screen.getByLabelText("JSON original: Antes da alteração"),
    ).toBeVisible();
    expect(
        screen.getByLabelText("JSON original: Antes da alteração").textContent,
    ).toBe(auditBefore);
    expect(
        screen.getByLabelText("JSON original: Depois da alteração").textContent,
    ).toBe(auditAfter);
    expect(screen.getAllByRole("button")).toHaveLength(1);
    expect(onSelect).not.toHaveBeenCalled();
    fireEvent.click(
        screen.getByRole("button", { name: "Selecionar este registro" }),
    );
    expect(onSelect).toHaveBeenCalledExactlyOnceWith(data, "AuditoriaResponse");
    expect(onSelect.mock.calls[0][0]).toBe(data);
});

it("mostra criação sem estado anterior e preserva texto inválido sem interpretar HTML", () => {
    const raw = '<img src=x onerror="alert(1)"><script>alert(1)</script>';
    const { container } = render(
        <Result
            data={{
                ...auditDetailSample,
                acao: "CRIACAO",
                dadosAntes: null,
                dadosDepois: raw,
            }}
            type="AuditoriaResponse"
        />,
    );
    expect(screen.getByRole("heading", { name: "Criação" })).toBeVisible();
    expect(screen.getByText("Sem dados anteriores informados.")).toBeVisible();
    expect(
        screen.getByText(information.auditSnapshotText.description),
    ).toBeVisible();
    expect(
        screen.getByLabelText("Conteúdo: Depois da alteração").textContent,
    ).toBe(raw);
    expect(container.querySelector("img, script")).toBeNull();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
});

it("mantém legado distinto, códigos desconhecidos e ausência de dados, com seleção bloqueada", () => {
    const onSelect = vi.fn();
    render(
        <Result
            data={{
                ...auditDetailSample,
                tipo: "LEGADO_NAO_ATRIBUIDO",
                tipoFisico: "CONTRATO_COBRANCA",
                acao: "ACAO_FUTURA",
                dadosAntes: null,
                dadosDepois: null,
                motivo: null,
            }}
            type="AuditoriaResponse"
            onSelect={onSelect}
            pending
        />,
    );
    expect(
        screen.getByText(information.auditHistoryLegacy.description),
    ).toBeVisible();
    expect(screen.getByRole("heading", { name: "ACAO_FUTURA" })).toBeVisible();
    expect(screen.getByText("Contrato", { selector: "dd" })).toBeVisible();
    expect(screen.getByText("Sem dados posteriores informados.")).toBeVisible();
    const select = screen.getByRole("button", {
        name: "Selecionar este registro",
    });
    expect(select).toBeDisabled();
    fireEvent.click(select);
    expect(onSelect).not.toHaveBeenCalled();
});

it.each(["0", "false", "null"])(
    "não confunde snapshot JSON %s com dado ausente",
    (raw) => {
        render(
            <Result
                data={{
                    ...auditDetailSample,
                    dadosAntes: raw,
                    dadosDepois: raw,
                }}
                type="AuditoriaResponse"
            />,
        );
        expect(
            screen.getByLabelText("Conteúdo: Antes da alteração").textContent,
        ).toBe(raw);
        expect(
            screen.getByLabelText("Conteúdo: Depois da alteração").textContent,
        ).toBe(raw);
        expect(
            screen.queryByText("Sem dados anteriores informados."),
        ).not.toBeInTheDocument();
    },
);
