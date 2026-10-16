import {
    render,
    screen,
    fireEvent,
    within,
    waitFor,
} from "@testing-library/react";
import { it, expect, vi } from "vitest";
import {
    ApiError,
    type Request,
    type Receipt,
    type Transport,
} from "../src/api/client";
import { RecordDialog } from "../src/components/records/RecordDialog";
import { Result } from "../src/components/Result";
import { FormReferences } from "../src/components/FormReferences";
import { journeys } from "../src/domain/journeys";
import { absorb, emptyWorkflow } from "../src/domain/workflow";
import { recordPages } from "../src/domain/recordPages";
import {
    serviceFactRecoveryData as fact,
    serviceFactRecoveryRecords as refs,
} from "./support/serviceFactRecovery";

const receipt = (data: unknown): Receipt => ({
    data,
    raw: "",
    ficticio: true,
    replay: false,
    requestId: "REC-FICTICIO",
});
const click = (name: string) =>
    fireEvent.click(screen.getByRole("button", { name }));
const uncertainty = () =>
    new ApiError(
        "Resposta perdida após envio fictício",
        0,
        "RESPOSTA_DESCONHECIDA",
        "REC-FICTICIO",
        true,
    );

it("apresenta cotas e valores exatos sem criar ação sobre a cota e conserva a seleção do fato", () => {
    const select = vi.fn();
    const view = (pending = false) => (
        <FormReferences.Provider
            value={{ defaults: {}, options: {}, records: refs }}
        >
            <Result
                data={{ ...fact, quantidade: "0.000000", valorBase: "0.00" }}
                type="FatoServicoDto.Fato"
                onSelect={select}
                pending={pending}
            />
        </FormReferences.Provider>
    );
    const mounted = render(view());
    const panel = within(
        screen.getByRole("article", { name: "Fato de serviço consultado" }),
    );
    expect(panel.getByText("10.000000", { exact: true })).toBeVisible();
    expect(panel.getByText("0.000000", { exact: true })).toBeVisible();
    expect(panel.getByText("0.00", { exact: true })).toBeVisible();
    expect(
        panel.getByText("NF 15102026 · série 0", { exact: true }),
    ).toBeVisible();
    expect(panel.queryByRole("button")).toBeNull();
    expect(
        panel.getByText(fact.criterioRateio, { normalizer: (value) => value }),
    ).toBeVisible();
    click("Selecionar este registro");
    expect(select).toHaveBeenCalledWith(
        { ...fact, quantidade: "0.000000", valorBase: "0.00" },
        "FatoServicoDto.Fato",
    );
    mounted.rerender(view(true));
    expect(
        screen.getByRole("button", { name: "Selecionar este registro" }),
    ).toBeDisabled();
});

it("distingue cotas ausentes de vazias e mantém identificadores e instante originais", () => {
    const mounted = render(
        <Result data={{ ...fact, cotas: null }} type="FatoServicoDto.Fato" />,
    );
    expect(
        screen.getByText("Cotas não informadas pelo servidor."),
    ).toBeVisible();
    fireEvent.click(screen.getByText("Identificadores e instante original"));
    expect(screen.getByText(fact.id, { exact: true })).toBeVisible();
    expect(screen.getByText(fact.versao, { exact: true })).toBeVisible();
    expect(screen.getByText(fact.executadoEm, { exact: true })).toBeVisible();
    mounted.rerender(
        <Result data={{ ...fact, cotas: [] }} type="FatoServicoDto.Fato" />,
    );
    expect(screen.getByText("Nenhuma cota informada.")).toBeVisible();
});

async function openUnknown() {
    const requests: Request[] = [];
    let read: () => Promise<Receipt> = async () => receipt(fact);
    const onUnknownClose = vi.fn(),
        onClose = vi.fn();
    const transport: Transport = {
        send: vi.fn(async (request) => {
            requests.push(request);
            if (request.endpoint.method !== "GET") throw uncertainty();
            if (request.endpoint.id !== "FatoServicoController.consultar")
                throw new Error("Consulta inesperada");
            return read();
        }),
    };
    let workflow = emptyWorkflow();
    for (const [type, rows] of Object.entries(refs))
        for (const row of rows) workflow = absorb(workflow, type, row, false);
    workflow = absorb(
        workflow,
        "PedidoEntradaDto.Detalhe",
        {
            pedido: refs["PedidoEntradaDto.Resumo"][0],
            notas: refs["PedidoEntradaDto.Nota"],
            itens: [],
        },
        true,
    );
    render(
        <RecordDialog
            opening={{
                row: fact,
                type: "FatoServicoDto.Fato",
                action: "FatoServicoController.anular",
            }}
            page={recordPages.cobranca[0]}
            actions={["FatoServicoController.anular"]}
            transport={transport}
            context={{ clienteId: "83", armazemId: "46" }}
            workflow={workflow}
            perfil="GESTOR"
            journey={journeys.find((j) => j.id === "cobranca")!}
            onClose={onClose}
            onUnknownClose={onUnknownClose}
            onReceipt={vi.fn()}
            onRecord={vi.fn()}
            onNavigate={vi.fn()}
        />,
    );
    const reason = await screen.findByLabelText("Motivo / justificativa *", {
        exact: true,
    });
    fireEvent.change(reason, {
        target: { value: "Anulação fictícia identificada para conferência." },
    });
    click("Conferir e confirmar");
    click("Confirmar agora");
    await screen.findByText("Resultado da anulação ainda não confirmado");
    return {
        requests,
        onUnknownClose,
        onClose,
        setRead: (callback: typeof read) => {
            read = callback;
        },
    };
}

it("consulta após resposta perdida sem repetir a anulação, conserva incerteza e exige saída explícita", async () => {
    const session = await openUnknown();
    session.setRead(async () =>
        receipt({ ...fact, situacao: "ANULADO", versao: "9007199254740996" }),
    );
    click("Consultar estado atual sem repetir");
    await screen.findByText("Anulado", { exact: true });
    expect(
        screen.getByText("NF 15102026 · série 0", { exact: true }),
    ).toBeVisible();
    expect(
        screen.getByText("Resultado da anulação ainda não confirmado"),
    ).toBeVisible();
    expect(
        screen.getByRole("button", { name: "Fechar diálogo" }),
    ).toBeDisabled();
    expect(screen.getByLabelText("Motivo / justificativa *")).toBeDisabled();
    expect(
        session.requests.filter((r) => r.endpoint.method !== "GET"),
    ).toHaveLength(1);
    expect(
        session.requests
            .filter((r) => r.endpoint.method === "GET")
            .map((r) => r.params.id),
    ).toEqual([fact.id, fact.id]);
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    click("Voltar à lista mantendo resultado desconhecido");
    expect(session.onUnknownClose).not.toHaveBeenCalled();
    confirm.mockReturnValue(true);
    click("Voltar à lista mantendo resultado desconhecido");
    expect(session.onUnknownClose).toHaveBeenCalledOnce();
    expect(session.onClose).not.toHaveBeenCalled();
});

it("identifica a consulta anterior ao atualizar/falhar e repete somente o comando original", async () => {
    const session = await openUnknown();
    click("Consultar estado atual sem repetir");
    await screen.findByText("Dados recebidos na consulta");
    let rejectRead!: (error: Error) => void;
    session.setRead(
        () =>
            new Promise((_resolve, reject) => {
                rejectRead = reject;
            }),
    );
    click("Consultar estado atual sem repetir");
    await screen.findByText("Consulta anterior · atualizando");
    expect(
        screen.getByRole("button", {
            name: "Consultar estado atual sem repetir",
        }),
    ).toBeDisabled();
    rejectRead(new Error("Consulta fictícia indisponível"));
    await screen.findByText("Última consulta recebida");
    expect(screen.getByText("Consulta fictícia indisponível")).toBeVisible();
    expect(
        screen.getByRole("article", { name: "Fato de serviço consultado" }),
    ).toBeVisible();
    expect(
        session.requests.filter((r) => r.endpoint.method !== "GET"),
    ).toHaveLength(1);
    click("Repetir exatamente a mesma operação");
    await waitFor(() =>
        expect(
            session.requests.filter((r) => r.endpoint.method !== "GET"),
        ).toHaveLength(2),
    );
    const mutations = session.requests.filter(
        (r) => r.endpoint.method !== "GET",
    );
    expect(mutations[1].body).toEqual(mutations[0].body);
    expect(mutations[1].params).toEqual(mutations[0].params);
    expect(mutations[0].params.id).toBe(fact.id);
});
