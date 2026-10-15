import { useState } from "react";
import { it, expect, vi } from "vitest";
import {
    render,
    screen,
    fireEvent,
    waitFor,
    within,
} from "@testing-library/react";
import { RecordDialog } from "../src/components/records/RecordDialog";
import { JourneyPage } from "../src/components/JourneyPage";
import { journeys } from "../src/domain/journeys";
import { recordPages, visibleRecordActions } from "../src/domain/recordPages";
import { emptyWorkflow, absorb } from "../src/domain/workflow";
import { fixture } from "../src/api/mock/fixtures";
import type { Request, Transport } from "../src/api/client";
import type { Values } from "../src/contracts/runtime";

const action = "ConfiguracaoCobrancaController.alterar";
const journey = journeys.find((j) => j.id === "precos")!;
const table = {
    id: "9007199254740993",
    versao: "7",
    codigo: "TAB-000003",
    descricao: "Tabela do cliente",
    armazemId: "1",
    clienteId: "1",
    tipo: "ESPECIFICA",
    situacao: "ATIVA",
    vigenciaInicio: "2026-10-01",
    vigenciaFim: "2026-11-01",
    itens: [
        {
            id: "50",
            servicoId: "2",
            categoria: "BOBINA",
            preco: "2.500000",
            percentual: null,
        },
        {
            id: "51",
            servicoId: "3",
            categoria: "",
            preco: "15.000000",
            percentual: null,
        },
    ],
};
function transport() {
    const requests: Request[] = [];
    const t: Transport = {
        send: vi.fn(async (r) => {
            requests.push(r);
            let data: unknown = table;
            if (r.endpoint.id === "ConfiguracaoCobrancaController.servicos")
                data = {
                    itens: [2, 3, 4].map((id) => ({
                        ...(fixture(
                            "ConfiguracaoCobrancaDto.Servico",
                        ) as Values),
                        id: String(id),
                        codigo: `SRV-${id}`,
                        descricao: `Serviço ${id}`,
                    })),
                    pagina: 0,
                    tamanho: 100,
                    totalItens: "3",
                    totalPaginas: 1,
                };
            if (
                r.endpoint.id === "ClienteController.listar" ||
                r.endpoint.id === "ArmazemController.listar"
            )
                data = {
                    itens: [
                        {
                            id: "1",
                            nome: "Cliente / armazém consultado",
                            codigo: "REF-1",
                        },
                    ],
                    totalItens: "1",
                    totalPaginas: 1,
                    pagina: 0,
                    tamanho: 100,
                };
            if (r.endpoint.id === "ConfiguracaoCobrancaController.tabelas")
                data = [table];
            if (r.endpoint.id === action)
                data = { ...table, ...r.body, versao: "8" };
            return {
                data,
                ficticio: true,
                raw: "",
                replay: false,
                requestId: "TAB-EDIT-FICTICIO",
            };
        }),
    };
    return { t, requests };
}

it("abre editor com valores exatos, permite trocar preço, remover/adicionar serviço e envia revisão", async () => {
    const { t, requests } = transport();
    render(
        <RecordDialog
            opening={{
                row: table,
                type: "ConfiguracaoCobrancaDto.Tabela",
                action,
            }}
            page={recordPages.precos[1].views![0]}
            actions={[action]}
            transport={t}
            context={{ clienteId: "9", armazemId: "9" }}
            workflow={emptyWorkflow()}
            perfil="GESTOR"
            onClose={vi.fn()}
            onUnknownClose={vi.fn()}
            onReceipt={vi.fn()}
            onRecord={vi.fn()}
            onNavigate={vi.fn()}
            journey={journey}
        />,
    );
    const dialog = within(await screen.findByRole("dialog"));
    await waitFor(() =>
        expect(dialog.getByLabelText("Itens / Item 1 / Preco")).toHaveValue(
            "2.500000",
        ),
    );
    expect(dialog.getByLabelText("Descricao *")).toHaveValue(
        "Tabela do cliente",
    );
    expect(dialog.getByLabelText("Armazém *")).not.toBeDisabled();
    expect(dialog.getByLabelText("Cliente")).not.toBeDisabled();
    expect(dialog.getByLabelText("Vigencia Inicio *")).toHaveValue(
        "2026-10-01",
    );
    fireEvent.change(dialog.getByLabelText("Itens / Item 1 / Preco"), {
        target: { value: "3.123456" },
    });
    const remove = dialog.getAllByRole("button", { name: /Remover/ });
    fireEvent.click(remove[1]);
    fireEvent.click(dialog.getByRole("button", { name: "Adicionar Itens" }));
    fireEvent.change(dialog.getByLabelText("Itens / Item 2 / Serviço *"), {
        target: { value: "4" },
    });
    fireEvent.change(dialog.getByLabelText("Itens / Item 2 / Preco"), {
        target: { value: "0.000000" },
    });
    fireEvent.change(dialog.getByLabelText("Motivo / justificativa *"), {
        target: { value: "Correção de serviços contratados" },
    });
    fireEvent.click(
        dialog.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(
        await screen.findByRole("button", { name: "Confirmar agora" }),
    );
    await waitFor(() =>
        expect(requests.some((r) => r.endpoint.id === action)).toBe(true),
    );
    const sent = requests.find((r) => r.endpoint.id === action)!;
    expect(sent.params.id).toBe(table.id);
    expect(sent.body).toMatchObject({
        versao: "7",
        clienteId: "1",
        armazemId: "1",
        itens: [
            { servicoId: "2", categoria: "BOBINA", preco: "3.123456" },
            { servicoId: "4", preco: "0.000000" },
        ],
    });
});

it("retira a etapa lateral duplicada e apresenta Editar na tabela para Gestor", async () => {
    const { t } = transport();
    function Harness() {
        const [workflow, setWorkflow] = useState(emptyWorkflow);
        return (
            <JourneyPage
                journey={journey}
                transport={t}
                context={{ clienteId: "1", armazemId: "1" }}
                perfil="GESTOR"
                workflow={workflow}
                startAction="ConfiguracaoCobrancaController.tabelas"
                onNavigate={vi.fn()}
                onRecord={(r, type) =>
                    setWorkflow((old) => absorb(old, type, r, true))
                }
                onReceipt={vi.fn()}
            />
        );
    }
    render(<Harness />);
    expect(journey.steps).toHaveLength(4);
    expect(
        screen.queryByRole("button", { name: /Vínculos por cliente/ }),
    ).toBeNull();
    expect(
        await screen.findByRole("button", { name: "Editar" }),
    ).toBeInTheDocument();
    expect(visibleRecordActions([action], "SUPERVISOR", table)).toEqual([]);
    expect(
        visibleRecordActions([action], "GESTOR", {
            ...table,
            situacao: "ENCERRADA",
        }),
    ).toEqual([]);
});
