import {
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import {
    ReferencePresentationProvider,
    ReferenceValue,
} from "../src/components/context/ReferencePresentation";
import { ScalarField } from "../src/components/fields/ScalarField";
import { ServiceFactConfirmation } from "../src/modules/financeiro/ServiceFactConfirmation";
import type { Request, Transport, Receipt } from "../src/api/client";

const service = {
    id: "9007199254740995",
    codigo: "SRV-QA",
    descricao: "Entrada QA consultada",
    tipo: "ENTRADA",
    unidade: "UNIDADE_LOGISTICA",
    situacao: "ATIVO",
};
const other = {
    ...service,
    id: "9007199254740997",
    codigo: "SRV-BETA",
    descricao: "Serviço Beta consultado",
};
const sample = {
    clienteId: "83",
    armazemId: "46",
    servicoId: service.id,
    origem: "MANUAL",
    referenciaExecucao: "EXEC-QA",
    executadoEm: "2026-10-16T02:46:36.454123Z",
    quantidade: "12.123456",
    valorBase: "0.00",
    categoria: "PALLET",
    criterioRateio: "Critério exato por nota\nCom origem conferida.",
    motivo: "Justificativa fictícia completa\nCom segunda linha preservada.",
};
const receipt = (data: unknown) => ({
    data,
    raw: "",
    ficticio: true,
    replay: false,
    requestId: "FATO-CONF-LOCAL",
});
const refs = {
    defaults: {},
    options: {},
    records: {
        "ClienteDto.Resposta": [
            { id: "83", nome: "Cliente QA", codigo: "CLI-QA" },
        ],
        "ArmazemDto.Resposta": [
            { id: "46", nome: "Armazém QA", codigo: "ARM-QA" },
        ],
    },
};

it("conserva o nome escolhido no seletor ao confirmar, volta sem enviar e envia o ID exato uma vez", async () => {
    const requests: Request[] = [];
    const transport: Transport = {
        send: vi.fn(async (request) => {
            requests.push(request);
            if (request.endpoint.method !== "GET")
                return new Promise<Receipt>(() => {});
            return receipt({
                itens: [service, other],
                pagina: 0,
                tamanho: 100,
                totalItens: "2",
                totalPaginas: 1,
            });
        }),
    };
    render(
        <ReferencePresentationProvider transport={transport}>
            <FormReferences.Provider value={refs}>
                <Operation
                    id="FatoServicoController.registrar"
                    context={sample}
                    perfil="SUPERVISOR"
                    transport={transport}
                    onSelect={vi.fn()}
                />
            </FormReferences.Provider>
        </ReferencePresentationProvider>,
    );
    await screen.findByRole("option", {
        name: "Serviço Beta consultado · SRV-BETA",
    });
    fireEvent.change(screen.getByRole("combobox", { name: "Serviço *" }), {
        target: { value: other.id },
    });
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    const confirmation = within(
        screen.getByRole("dialog", { name: "Confirmar fato de serviço" }),
    );
    expect(
        await confirmation.findByText("Serviço Beta consultado · SRV-BETA"),
    ).toBeVisible();
    expect(
        requests.filter((request) => request.endpoint.method !== "GET"),
    ).toHaveLength(0);
    fireEvent.click(
        confirmation.getByRole("button", { name: "Voltar à conferência" }),
    );
    expect(screen.getByRole("combobox", { name: "Serviço *" })).toHaveValue(
        other.id,
    );
    expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
        sample.motivo,
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() =>
        expect(
            requests.filter((request) => request.endpoint.method !== "GET"),
        ).toHaveLength(1),
    );
    expect(requests.at(-1)?.body).toMatchObject({
        ...sample,
        servicoId: other.id,
    });
});

it("mantém o nome consultado após desmontar o seletor e descarta-o na troca de escopo", async () => {
    const transport: Transport = {
        send: vi.fn(async () => receipt({ itens: [service], totalPaginas: 1 })),
    };
    const view = (editing: boolean, scope = "A") => (
        <ReferencePresentationProvider transport={transport} scope={scope}>
            <FormReferences.Provider value={refs}>
                {editing ? (
                    <ScalarField
                        f={{ name: "servicoId", type: "Long", required: true }}
                        path=""
                        value={service.id}
                        onChange={vi.fn()}
                        disabled={false}
                    />
                ) : (
                    <ReferenceValue field="servicoId" value={service.id} />
                )}
            </FormReferences.Provider>
        </ReferencePresentationProvider>
    );
    const mounted = render(view(true));
    await screen.findByRole("option", {
        name: "Entrada QA consultada · SRV-QA",
    });
    mounted.rerender(view(false));
    expect(
        await screen.findByText("Entrada QA consultada · SRV-QA"),
    ).toBeVisible();
    mounted.rerender(view(false, "B"));
    expect(screen.queryByText("Entrada QA consultada · SRV-QA")).toBeNull();
    expect(screen.getByText("Referência indisponível")).toBeVisible();
    expect(transport.send).toHaveBeenCalledTimes(1);
});

it("distingue valor zero de ausência e conserva textos/instante exatos na conferência", () => {
    const content = (valorBase: string, servicoId = service.id) => (
        <FormReferences.Provider
            value={{
                ...refs,
                records: {
                    ...refs.records,
                    "ConfiguracaoCobrancaDto.Servico": [service],
                },
            }}
        >
            <ServiceFactConfirmation
                values={{
                    ...sample,
                    valorBase,
                    servicoId,
                    operacaoId: "00000000-0000-4000-8000-000000000001",
                    cotas: [],
                }}
            />
        </FormReferences.Provider>
    );
    const view = render(content("0.00"));
    const amounts = within(
        screen.getByRole("region", { name: "Quantidade e valor" }),
    );
    expect(amounts.getByText("0.00", { exact: true })).toBeVisible();
    expect(amounts.getByText(sample.quantidade, { exact: true })).toBeVisible();
    expect(
        screen
            .getByRole("region", { name: "Justificativa do fato" })
            .querySelector("p")?.textContent,
    ).toBe(sample.motivo);
    expect(
        screen
            .getByRole("region", { name: "Critério de rateio" })
            .querySelector("p")?.textContent,
    ).toBe(sample.criterioRateio);
    fireEvent.click(screen.getByText("Identificadores e instante original"));
    expect(screen.getByText(sample.executadoEm, { exact: true })).toBeVisible();
    view.rerender(content("", "9007199254740999"));
    expect(
        within(
            screen.getByRole("region", { name: "Quantidade e valor" }),
        ).getByText("Não informado", { exact: true }),
    ).toBeVisible();
    expect(
        within(
            screen.getByRole("region", { name: "Serviço e execução" }),
        ).getByText("Referência indisponível"),
    ).toBeVisible();
});

it("não reaproveita opções do seletor ainda montado depois da troca de escopo", async () => {
    const send = vi
        .fn<Transport["send"]>()
        .mockResolvedValueOnce(receipt({ itens: [service], totalPaginas: 1 }))
        .mockRejectedValue(new Error("403"));
    const transport = { send };
    const content = (scope: string, editing: boolean) => (
        <ReferencePresentationProvider transport={transport} scope={scope}>
            <FormReferences.Provider value={refs}>
                {editing ? (
                    <ScalarField
                        f={{ name: "servicoId", type: "Long", required: true }}
                        path=""
                        value={service.id}
                        onChange={vi.fn()}
                        disabled={false}
                    />
                ) : (
                    <ReferenceValue field="servicoId" value={service.id} />
                )}
            </FormReferences.Provider>
        </ReferencePresentationProvider>
    );
    const view = render(content("A", true));
    await screen.findByRole("option", {
        name: "Entrada QA consultada · SRV-QA",
    });
    view.rerender(content("B", true));
    expect(
        screen.queryByRole("option", {
            name: "Entrada QA consultada · SRV-QA",
        }),
    ).toBeNull();
    await waitFor(() => expect(send).toHaveBeenCalledTimes(2));
    view.rerender(content("B", false));
    expect(screen.getByText("Referência indisponível")).toBeVisible();
});
