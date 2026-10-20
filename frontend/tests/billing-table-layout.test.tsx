import { it, expect, vi } from "vitest";
import {
    render,
    screen,
    fireEvent,
    within,
    waitFor,
} from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import {
    ApiError,
    type Transport,
    type Request,
    type Receipt,
} from "../src/api/client";
import { encode } from "../src/contracts/codec";
import { fixture } from "../src/api/mock/fixtures";
import type { Values } from "../src/contracts/runtime";

const serviceId = "9007199254740993",
    secondId = "9007199254740995";
const draft: Values = {
    armazemId: "9007199254741001",
    clienteId: "9007199254741003",
    descricao: "Tabela fictícia local",
    tipo: "ESPECIFICA",
    vigenciaInicio: "2026-10-01",
    vigenciaFim: null,
    motivo: "Valores contratados fictícios para conferir o formulário",
};
const row = (index: number) =>
    within(screen.getByRole("group", { name: `Serviço ${index}` }));
function setup(context: Values = {}, hold = false) {
    const calls: Request[] = [];
    let reject!: (error: Error) => void;
    const transport: Transport = {
        send: vi.fn(async (request) => {
            calls.push(request);
            if (hold)
                return new Promise<Receipt>((_resolve, fail) => {
                    reject = fail;
                });
            return {
                data: fixture("ConfiguracaoCobrancaDto.Tabela"),
                raw: "",
                ficticio: true,
                replay: false,
                requestId: "TABLE-LAYOUT-LOCAL",
            };
        }),
    };
    render(
        <FormReferences.Provider
            value={{
                defaults: { servicoId: serviceId },
                options: {
                    servicoId: [
                        { value: serviceId, title: "Armazenagem Alfa" },
                        { value: secondId, title: "Percentual Beta" },
                    ],
                    compromissoId: [
                        { value: secondId, title: "Fechamento Beta" },
                    ],
                },
            }}
        >
            <Operation
                id="ConfiguracaoCobrancaController.criarTabela"
                transport={transport}
                context={{ ...draft, ...context }}
                perfil="GESTOR"
                inDialog
                hideHeading
                onSelect={vi.fn()}
            />
        </FormReferences.Provider>,
    );
    return {
        calls,
        fail: () =>
            reject(
                new ApiError(
                    "Tabela alterada; confira os dados.",
                    409,
                    "CONFLITO",
                ),
            ),
    };
}
function add(price: string) {
    fireEvent.click(screen.getByRole("button", { name: "Adicionar serviço" }));
    const last = screen.getAllByRole("group", { name: /^Serviço \d+$/ }).length;
    fireEvent.change(row(last).getByLabelText("Preço"), {
        target: { value: price },
    });
}
async function review() {
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    await screen.findByRole("button", { name: "Confirmar agora" });
}

it("exige ao menos um serviço e não envia o cadastro vazio", async () => {
    const { calls } = setup();
    expect(screen.getByLabelText("Descrição *").tagName).toBe("INPUT");
    expect(screen.getByLabelText("Descrição *")).toHaveAttribute(
        "maxlength",
        "160",
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(/itens.*1.*500/);
    expect(calls).toHaveLength(0);
});

it("adiciona/remove serviços, volta da conferência e conserva IDs, casas decimais e ausências no envio", async () => {
    const { calls } = setup();
    add("1.123456");
    add("9999999999999.123456");
    fireEvent.change(row(2).getByLabelText("Serviço *"), {
        target: { value: secondId },
    });
    fireEvent.click(screen.getByRole("button", { name: "Remover serviço 1" }));
    expect(row(1).getByLabelText("Serviço *")).toHaveValue(secondId);
    await review();
    expect(calls).toHaveLength(0);
    fireEvent.click(
        screen.getByRole("button", { name: "Voltar à conferência" }),
    );
    expect(row(1).getByLabelText("Preço")).toHaveValue("9999999999999.123456");
    expect(screen.getByLabelText("Descrição *")).toHaveValue(
        String(draft.descricao),
    );
    await review();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(calls).toHaveLength(1));
    expect(calls[0].body).toMatchObject({
        ...draft,
        itens: [
            {
                servicoId: secondId,
                categoria: "",
                preco: "9999999999999.123456",
                percentual: "",
            },
        ],
    });
    const wire = encode(calls[0].endpoint.request!, calls[0].body);
    expect(wire).toContain('"preco":9999999999999.123456');
    expect(wire).toContain('"servicoId":9007199254740995');
    expect(wire).toContain('"percentual":null');
});

it("conserva preço zero e percentual sem converter nem limpar automaticamente os valores", async () => {
    const { calls } = setup();
    add("0.000000");
    add("");
    fireEvent.change(row(2).getByLabelText("Serviço *"), {
        target: { value: secondId },
    });
    fireEvent.change(row(2).getByLabelText("Percentual (%)"), {
        target: { value: "0.123456" },
    });
    fireEvent.change(row(2).getByLabelText("Categoria *"), {
        target: { value: "AVULSO" },
    });
    await review();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(calls).toHaveLength(1));
    expect(calls[0].body).toMatchObject({
        itens: [
            { preco: "0.000000", percentual: "" },
            {
                servicoId: secondId,
                categoria: "AVULSO",
                preco: "",
                percentual: "0.123456",
            },
        ],
    });
});

it("preserva resolução ao recolher/voltar e permite removê-la explicitamente", async () => {
    const { calls } = setup();
    add("2.500000");
    const summary = screen.getByText("Resolução financeira histórica", {
        selector: "summary",
    });
    expect(summary.closest("details")).not.toHaveAttribute("open");
    fireEvent.click(summary);
    fireEvent.click(
        screen.getByRole("button", { name: "Informar resolução financeira" }),
    );
    fireEvent.change(screen.getByLabelText("Tipo de compromisso *"), {
        target: { value: "FECHAMENTO" },
    });
    fireEvent.change(screen.getByLabelText("Compromisso *"), {
        target: { value: secondId },
    });
    fireEvent.click(summary);
    await review();
    fireEvent.click(
        screen.getByRole("button", { name: "Voltar à conferência" }),
    );
    expect(screen.getByText("Informada")).toBeInTheDocument();
    fireEvent.click(
        screen.getByText("Resolução financeira histórica", {
            selector: "summary",
        }),
    );
    expect(screen.getByLabelText("Compromisso *")).toHaveValue(secondId);
    fireEvent.click(
        screen.getByRole("button", { name: "Remover resolução financeira" }),
    );
    expect(screen.queryByText("Informada")).toBeNull();
    await review();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(calls).toHaveLength(1));
    expect((calls[0].body as Values).resolucao).toBeNull();
});

it("bloqueia os controles durante o envio e conserva rascunho e operação após conflito", async () => {
    const { calls, fail } = setup({}, true);
    add("2.123456");
    await review();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(calls).toHaveLength(1));
    expect(
        screen.getByRole("button", { name: "Adicionar serviço" }),
    ).toBeDisabled();
    expect(
        screen.getByRole("button", { name: "Remover serviço 1" }),
    ).toBeDisabled();
    expect(row(1).getByLabelText("Preço")).toBeDisabled();
    const operation = (calls[0].body as Values).operacaoId;
    fail();
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Tabela alterada; confira os dados.",
    );
    expect(row(1).getByLabelText("Preço")).toHaveValue("2.123456");
    await review();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(calls).toHaveLength(2));
    expect((calls[1].body as Values).operacaoId).toBe(operation);
});

it("envia a resolução histórica informada mesmo recolhida", async () => {
    const resolution = { tipo: "FECHAMENTO", compromissoId: secondId };
    const { calls } = setup({ resolucao: resolution });
    add("2.500000");
    expect(screen.getByText("Informada")).toBeInTheDocument();
    expect(
        screen
            .getByText("Resolução financeira histórica", {
                selector: "summary",
            })
            .closest("details"),
    ).not.toHaveAttribute("open");
    await review();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(calls).toHaveLength(1));
    expect((calls[0].body as Values).resolucao).toEqual(resolution);
});
