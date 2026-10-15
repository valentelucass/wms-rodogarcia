import { it, expect, vi } from "vitest";
import { render, screen, within, fireEvent } from "@testing-library/react";
import { RecordDialog } from "../src/components/records/RecordDialog";
import { emptyWorkflow } from "../src/domain/workflow";
import { journeys } from "../src/domain/journeys";
import type { Transport } from "../src/api/client";
import type { Values } from "../src/contracts/runtime";

const service: Values = {
    id: "9007199254740993",
    versao: "9007199254740995",
    codigo: "SRV-ARMAZENAGEM-ALFA",
    descricao: "Armazenagem fictícia",
    tipo: "ARMAZENAGEM",
    unidade: "POSICAO_DIA",
    situacao: "ATIVO",
};

function setup(row: Values = service) {
    const send = vi.fn<Transport["send"]>();
    const onRecord = vi.fn();
    const onClose = vi.fn();
    render(
        <RecordDialog
            opening={{ row, type: "ConfiguracaoCobrancaDto.Servico" }}
            page={{
                source: "ConfiguracaoCobrancaController.servicos",
                headers: [],
            }}
            actions={[]}
            transport={{ send }}
            context={{}}
            workflow={emptyWorkflow()}
            perfil="SUPERVISOR"
            onClose={onClose}
            onUnknownClose={vi.fn()}
            onReceipt={vi.fn()}
            onRecord={onRecord}
            onNavigate={vi.fn()}
            journey={journeys.find((journey) => journey.id === "precos")!}
        />,
    );
    return { send, onRecord, onClose };
}

it("seleciona o serviço original com ID/revisão exatos e fecha sem solicitação de negócio", () => {
    const { send, onRecord, onClose } = setup();
    const dialog = screen.getByRole("dialog", { name: "Detalhes do serviço" });
    expect(
        within(dialog).getAllByText(service.descricao as string),
    ).toHaveLength(1);
    expect(within(dialog).getByText("Posição por dia")).toBeVisible();
    expect(within(dialog).getByText("Ativo")).toBeVisible();
    const select = within(dialog).getByRole("button", {
        name: "Selecionar este registro",
    });
    expect(
        within(dialog).getAllByRole("button", {
            name: "Selecionar este registro",
        }),
    ).toHaveLength(1);
    fireEvent.click(select);
    expect(onRecord).toHaveBeenCalledExactlyOnceWith(
        service,
        "ConfiguracaoCobrancaDto.Servico",
    );
    expect(send).not.toHaveBeenCalled();
    expect(onClose).toHaveBeenCalledOnce();
});

it("mantém situações e unidades desconhecidas legíveis e seleciona sem transformar valores", () => {
    const row = {
        ...service,
        situacao: "REVISAO_EXTERNA",
        tipo: "NOVO_TIPO",
        unidade: "NOVA_UNIDADE",
    };
    const { onRecord, send } = setup(row);
    expect(screen.getByText("REVISAO_EXTERNA")).toBeVisible();
    expect(screen.getByText("NOVO_TIPO")).toBeVisible();
    expect(screen.getByText("NOVA_UNIDADE")).toBeVisible();
    fireEvent.click(
        screen.getByRole("button", { name: "Selecionar este registro" }),
    );
    expect(onRecord).toHaveBeenCalledExactlyOnceWith(
        row,
        "ConfiguracaoCobrancaDto.Servico",
    );
    expect(send).not.toHaveBeenCalled();
});

it("distingue inatividade e dados ausentes sem impedir a seleção já permitida", () => {
    const row = {
        ...service,
        descricao: null,
        tipo: null,
        unidade: null,
        situacao: "INATIVO",
    };
    const { onRecord } = setup(row);
    expect(screen.getByText("Inativo")).toBeVisible();
    expect(screen.getAllByText("Não informado")).toHaveLength(3);
    fireEvent.click(
        screen.getByRole("button", { name: "Selecionar este registro" }),
    );
    expect(onRecord).toHaveBeenCalledExactlyOnceWith(
        row,
        "ConfiguracaoCobrancaDto.Servico",
    );
});
