import { fireEvent, render, screen, within } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { OperationResults } from "../src/components/operation/OperationResults";
import { Result } from "../src/components/Result";
import { FormReferences } from "../src/components/FormReferences";
import {
    billingApprovalData as data,
    billingApprovalReferences as refs,
    billingApprovalAdjustment as adjustment,
} from "./support/billingApproval";
import type { Receipt } from "../src/api/client";

const receipt = (replay = false): Receipt => ({
    data,
    raw: "",
    ficticio: true,
    replay,
    requestId: "APROVACAO-QA",
});
const type = "FechamentoCobrancaDto.ConfirmacaoComando";

it("preserva valores/hash/textos e seleciona os objetos originais do ciclo, versão e ajuste", () => {
    const onSelect = vi.fn(),
        onContinue = vi.fn();
    render(
        <FormReferences.Provider value={refs}>
            <OperationResults
                id="FechamentoCobrancaController.aprovar"
                receipt={receipt()}
                type={type}
                onSelect={onSelect}
                onContinue={onContinue}
            />
        </FormReferences.Provider>,
    );
    const card = within(screen.getByRole("article", { name: "Ajuste 1" }));
    expect(card.getByText("0.000000", { exact: true })).toBeVisible();
    expect(card.getByText("Versão 3", { exact: true })).toBeVisible();
    expect(
        card.getAllByText("9007199254740993.123456", { exact: true }),
    ).toHaveLength(2);
    expect(
        card.getByText(adjustment.evidencia, { normalizer: (value) => value }),
    ).toBeVisible();
    expect(
        card.getByText(adjustment.motivo, { normalizer: (value) => value }),
    ).toBeVisible();
    fireEvent.click(card.getByText("Identificadores e hash do ajuste 1"));
    expect(
        card.getByText(adjustment.hashCorrecao, { exact: true }),
    ).toBeVisible();
    expect(
        card.getByText(adjustment.registradoEm, { exact: true }),
    ).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Selecionar ciclo" }));
    expect(onSelect.mock.calls.at(-1)?.[0]).toBe(data.fechamento);
    expect(onSelect.mock.calls.at(-1)?.[1]).toBe(
        "FechamentoCobrancaDto.Fechamento",
    );
    fireEvent.click(screen.getByRole("button", { name: "Selecionar versão" }));
    expect(onSelect.mock.calls.at(-1)?.[0]).toBe(data.versao);
    expect(onSelect.mock.calls.at(-1)?.[1]).toBe(
        "FechamentoCobrancaDto.Versao",
    );
    fireEvent.click(card.getByRole("button", { name: "Selecionar ajuste 1" }));
    expect(onSelect.mock.calls.at(-1)?.[0]).toBe(adjustment);
    expect(onSelect.mock.calls.at(-1)?.[1]).toBe(
        "FechamentoCobrancaDto.Ajuste",
    );
    expect(onContinue).not.toHaveBeenCalled();
    fireEvent.click(
        screen.getByRole("button", {
            name: "Registrar entrega manual desta versão",
        }),
    );
    expect(onContinue).toHaveBeenCalledWith({
        page: "fechamento",
        action: "FechamentoCobrancaController.entregar",
        title: "Registrar entrega manual desta versão",
    });
});

it("bloqueia seleção/continuação durante atualização e conserva a regra de repetição histórica", () => {
    const onSelect = vi.fn(),
        onContinue = vi.fn();
    const view = (pending: boolean, replay = false) => (
        <OperationResults
            id="FechamentoCobrancaController.aprovar"
            receipt={receipt(replay)}
            type={type}
            pending={pending}
            onSelect={onSelect}
            onContinue={onContinue}
        />
    );
    const mounted = render(view(true));
    for (const name of [
        "Selecionar ciclo",
        "Selecionar versão",
        "Selecionar ajuste 1",
        "Registrar entrega manual desta versão",
    ])
        expect(screen.getByRole("button", { name })).toBeDisabled();
    mounted.rerender(view(false, true));
    expect(
        screen.queryByRole("button", {
            name: "Registrar entrega manual desta versão",
        }),
    ).toBeNull();
    expect(
        screen.getByText(/Confirmação histórica: consulte o detalhe atual/),
    ).toBeVisible();
    expect(onContinue).not.toHaveBeenCalled();
    expect(onSelect).not.toHaveBeenCalled();
});

it("distingue ausências/vazio de zero, mantém crédito e dados completos/históricos sem nova consulta", () => {
    const alternative = {
        ...adjustment,
        origemVersaoId: "999",
        valorBase: null,
        valorCorrigido: "0.000000",
        diferenca: "-3.123456",
        tratativaOrigemId: "9007199254741031",
    };
    const mounted = render(
        <Result
            data={[alternative]}
            type="List<FechamentoCobrancaDto.Ajuste>"
        />,
    );
    expect(screen.getByText("-3.123456", { exact: true })).toBeVisible();
    expect(screen.getByText("0.000000", { exact: true })).toBeVisible();
    expect(
        screen.getAllByText("Referência indisponível").length,
    ).toBeGreaterThan(0);
    fireEvent.click(screen.getByText("Identificadores e hash do ajuste 1"));
    expect(screen.getByText("999", { exact: true })).toBeVisible();
    expect(screen.getByText("9007199254741031", { exact: true })).toBeVisible();
    mounted.rerender(
        <Result data={[]} type="List<FechamentoCobrancaDto.Ajuste>" />,
    );
    expect(screen.getByText("Nenhum ajuste informado.")).toBeVisible();
    mounted.rerender(
        <Result data={null} type="List<FechamentoCobrancaDto.Ajuste>" />,
    );
    expect(screen.getByText("Não informado pelo servidor.")).toBeVisible();
    mounted.rerender(
        <OperationResults
            id="FechamentoCobrancaController.aprovar"
            receipt={receipt()}
            type={type}
            onSelect={vi.fn()}
        />,
    );
    fireEvent.click(
        screen.getByText(
            "Identificadores e dados completos do ciclo e da versão",
        ),
    );
    expect(
        screen.getByText(data.versao.conteudoHash, { exact: true }),
    ).toBeVisible();
    expect(
        screen.getByText(data.fechamento.versao, { exact: true }),
    ).toBeVisible();
    fireEvent.click(
        screen.getByText("Entregas, confirmações e documentos da versão"),
    );
    expect(
        within(
            screen.getByRole("region", { name: "NFS-e da versão" }),
        ).getByText("Nenhum registro encontrado."),
    ).toBeVisible();
    expect(
        within(
            screen.getByRole("region", {
                name: "Resolução financeira da versão",
            }),
        ).getByText("Não informado pelo servidor."),
    ).toBeVisible();
});
