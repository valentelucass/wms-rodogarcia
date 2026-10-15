import { it, expect, vi } from "vitest";
import {
    render,
    screen,
    fireEvent,
    waitFor,
    within,
} from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import {
    ApiError,
    type Request,
    type Transport,
    type Receipt,
} from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";
import type { Values } from "../src/contracts/runtime";

const first = "9007199254740993",
    second = "9007199254740995",
    commitment = "9007199254740997";
const draft: Values = {
    clienteId: "9007199254741001",
    armazemId: "9007199254741003",
    vigenciaInicio: "2026-10-01",
    vigenciaFim: null,
    fuso: "America/Sao_Paulo",
    moeda: "BRL",
    modalidadeCiclo: "MES_DIA_FIXO",
    diaCorte: 10,
    duracaoDias: null,
    minimoModo: "APLICAVEL",
    minimoValor: "9007199254740993.12",
    minimoProporcao: "PROPORCIONAL_DIAS",
    servicosMinimo: [],
    grisModo: "APLICAVEL",
    grisPercentual: "0.123456",
    grisBase: "VALOR_ESTOQUE_PICO",
    grisPeriodicidade: "POR_CICLO",
    grisProporcao: "INTEGRAL",
    motivo: "Configuração fictícia preservada para conferência",
};
function setup(patch: Values = {}, hold = false) {
    const requests: Request[] = [];
    let reject!: (error: Error) => void;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            if (hold)
                return new Promise<Receipt>((_resolve, fail) => {
                    reject = fail;
                });
            return {
                data: fixture("ConfiguracaoCobrancaDto.Contrato"),
                ficticio: true,
                raw: "",
                replay: false,
                requestId: "CONTRACT-UI-LOCAL",
            };
        },
    };
    render(
        <FormReferences.Provider
            value={{
                defaults: {},
                records: {},
                options: {
                    servicoId: [
                        { value: first, title: "Armazenagem Alfa" },
                        { value: second, title: "Serviço Beta" },
                    ],
                    compromissoId: [
                        { value: commitment, title: "Fechamento Alfa" },
                    ],
                },
            }}
        >
            <Operation
                id="ConfiguracaoCobrancaController.configurar"
                transport={transport}
                context={{ ...draft, ...patch }}
                perfil="GESTOR"
                onReceipt={vi.fn()}
                onSelect={vi.fn()}
            />
        </FormReferences.Provider>,
    );
    return {
        requests,
        fail: () =>
            reject(
                new ApiError(
                    "Contrato alterado; consulte novamente.",
                    409,
                    "CONFLITO",
                ),
            ),
    };
}
function add(value: string, index: number) {
    fireEvent.click(screen.getByRole("button", { name: "Adicionar serviço" }));
    fireEvent.change(
        within(
            screen.getByRole("group", { name: `Serviço ${index}` }),
        ).getByLabelText(`Serviço ${index} *`),
        { target: { value } },
    );
}
async function confirm() {
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(
        await screen.findByRole("button", { name: "Confirmar agora" }),
    );
}

it("inclui/remove serviços sem perder os parâmetros e confirma IDs/decimais/códigos exatos", async () => {
    const { requests } = setup();
    add(first, 1);
    add(second, 2);
    fireEvent.click(screen.getByRole("button", { name: "Remover serviço 1" }));
    expect(screen.getByLabelText("Serviço 1 *")).toHaveValue(second);
    expect(screen.getByLabelText("Valor mínimo")).toHaveValue(
        "9007199254740993.12",
    );
    expect(screen.getByLabelText("Percentual (%)")).toHaveValue("0.123456");
    expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
        String(draft.motivo),
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    await screen.findByRole("button", { name: "Confirmar agora" });
    expect(requests).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(requests).toHaveLength(1));
    expect(requests[0].endpoint.id).toBe(
        "ConfiguracaoCobrancaController.configurar",
    );
    expect(requests[0].body).toMatchObject({
        ...draft,
        servicosMinimo: [second],
    });
});

it("mantém ausência/não aplicação distintos de zero e permite informar/remover a resolução histórica", async () => {
    const absent: Values = {
        minimoModo: "NAO_APLICAVEL",
        minimoValor: null,
        minimoProporcao: null,
        grisModo: "NAO_INFORMADO",
        grisPercentual: null,
        grisBase: null,
        grisPeriodicidade: null,
        grisProporcao: null,
    };
    const { requests } = setup(absent);
    const toggle = () =>
        fireEvent.click(
            screen.getByRole("button", {
                name: "Informar Resolução financeira histórica (Gestor)",
            }),
        );
    toggle();
    fireEvent.click(
        screen.getByRole("button", {
            name: "Remover Resolução financeira histórica (Gestor)",
        }),
    );
    expect(screen.getByLabelText("Aplicação do mínimo *")).toHaveValue(
        "NAO_APLICAVEL",
    );
    expect(screen.getByLabelText("Aplicação do GRIS *")).toHaveValue(
        "NAO_INFORMADO",
    );
    expect(screen.getByLabelText("Valor mínimo")).toHaveValue("");
    toggle();
    fireEvent.change(
        screen.getByLabelText(/Resolução financeira histórica.*Tipo \*/),
        { target: { value: "FECHAMENTO" } },
    );
    fireEvent.change(
        screen.getByLabelText(/Resolução financeira histórica.*Compromisso/),
        { target: { value: commitment } },
    );
    await confirm();
    await waitFor(() => expect(requests).toHaveLength(1));
    expect(requests[0].body).toMatchObject({
        ...absent,
        resolucao: { tipo: "FECHAMENTO", compromissoId: commitment },
        servicosMinimo: [],
    });
});

it("bloqueia o formulário durante envio e preserva parâmetros/serviços após conflito", async () => {
    const { requests, fail } = setup({}, true);
    add(first, 1);
    await confirm();
    await waitFor(() => expect(requests).toHaveLength(1));
    expect(
        screen.getByRole("button", { name: "Adicionar serviço" }),
    ).toBeDisabled();
    expect(
        screen.getByRole("button", { name: "Remover serviço 1" }),
    ).toBeDisabled();
    fail();
    await screen.findByText("Contrato alterado; consulte novamente.");
    expect(screen.getByLabelText("Valor mínimo")).toHaveValue(
        "9007199254740993.12",
    );
    expect(screen.getByLabelText("Serviço 1 *")).toHaveValue(first);
    expect(screen.getByLabelText("Aplicação do GRIS *")).toHaveValue(
        "APLICAVEL",
    );
});
