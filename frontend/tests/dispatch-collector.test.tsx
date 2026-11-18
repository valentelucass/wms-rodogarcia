import { it, expect, vi } from "vitest";
import {
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { ApiError, type Receipt, type Transport } from "../src/api/client";
import { DispatchTaskStatus } from "../src/modules/saida/DispatchTaskStatus";
import { CollectorTasks } from "../src/modules/coletor/CollectorTasks";
import { Collector } from "../src/components/Collector";
import { dispatchContinuation } from "../src/modules/saida/continuation";
import { journeys } from "../src/modules/saida/definition";
const result = (data: unknown): Receipt => ({
    data,
    raw: "",
    requestId: "teste",
    ficticio: true,
    replay: false,
});
const task = {
    id: "5",
    origemId: "91",
    tipo: "SEPARACAO",
    versao: "0",
    situacao: "AGUARDANDO_RESERVA",
    clienteId: "1",
    armazemId: "2",
    referencia: "PEDIDO-91",
    impedimento: "Aguarde a reserva.",
    minha: false,
    responsavel: null,
};
function transport(
    lookup: () => Promise<Receipt> = async () => result(task),
): Transport {
    return {
        send: vi.fn(async (r) => {
            switch (r.endpoint.id) {
                case "TarefaColetorController.porPedido":
                    return lookup();
                case "TarefaColetorController.disponibilidade":
                    return result({ disponivel: true });
                case "TarefaColetorController.totais":
                    return result({
                        enderecamento: "0",
                        separacao: "1",
                        inventario: "0",
                    });
                case "TarefaColetorController.listar":
                    return result({
                        itens: [task],
                        pagina: 0,
                        tamanho: 20,
                        totalItens: "1",
                        totalPaginas: 1,
                    });
                case "TarefaColetorController.consultar":
                    return result({ tarefa: task, separacoes: [] });
                default:
                    throw new Error(r.endpoint.id);
            }
        }),
    };
}
it("Saída mantém três etapas administrativas e reserva encaminha o pedido exato", () => {
    expect(journeys[0].steps.map((s) => s.title)).toEqual([
        "1. Pedido integral",
        "2. Sugestão FIFO e reserva",
        "3. Reversão e cancelamento",
    ]);
    expect(journeys[0].steps.flatMap((s) => s.actions)).not.toContain(
        "ExpedicaoController.ler",
    );
    expect(
        dispatchContinuation(
            "PedidoSaidaController.reservar",
            result({ pedido: { id: "91" } }),
            undefined,
        ),
    ).toMatchObject({ page: "coletor", action: "pedido:91" });
});
it("detalhe acompanha a tarefa e abre a identidade exata sem iniciar operação", async () => {
    const t = transport(),
        navigate = vi.fn();
    render(
        <DispatchTaskStatus
            order={{ id: "91", versao: "0" }}
            transport={t}
            perfil="OPERACAO"
            disabled={false}
            onNavigate={navigate}
        />,
    );
    await screen.findByText("Aguardando reserva");
    fireEvent.click(
        screen.getByRole("button", { name: "Acompanhar no Coletor" }),
    );
    expect(navigate).toHaveBeenCalledWith(
        expect.objectContaining({ page: "coletor", action: "tarefa:5" }),
    );
    expect(
        vi
            .mocked(t.send)
            .mock.calls.every(([r]) => r.endpoint.method === "GET"),
    ).toBe(true);
});
it("pedido histórico sem tarefa oferece operação manual e erro de servidor não", async () => {
    const t = transport(async () => {
        throw new ApiError("Não encontrado", 404, "NAO_ENCONTRADO");
    });
    const { rerender } = render(
        <DispatchTaskStatus
            order={{ id: "91" }}
            transport={t}
            perfil="OPERACAO"
            disabled={false}
            onNavigate={vi.fn()}
        />,
    );
    await screen.findByRole("button", {
        name: "Abrir separação manual no Coletor",
    });
    const failed = transport(async () => {
        throw new ApiError("Consulta indisponível", 503, "INDISPONIVEL");
    });
    rerender(
        <DispatchTaskStatus
            order={{ id: "91" }}
            transport={failed}
            perfil="OPERACAO"
            disabled={false}
            onNavigate={vi.fn()}
        />,
    );
    await screen.findByRole("alert");
    expect(
        screen.queryByRole("button", {
            name: "Abrir separação manual no Coletor",
        }),
    ).toBeNull();
});
it("encaminhamento após reserva busca por origem e abre a tarefa sem assumir o responsável", async () => {
    const t = transport();
    render(
        <CollectorTasks
            startAction="pedido:91"
            transport={t}
            perfil="OPERACAO"
            context={{ clienteId: "1", armazemId: "2" }}
        />,
    );
    const dialog = await screen.findByRole("dialog");
    await within(dialog).findByText("PEDIDO-91");
    expect(
        within(dialog).queryByRole("button", { name: "Iniciar tarefa" }),
    ).toBeNull();
    expect(
        vi
            .mocked(t.send)
            .mock.calls.find(
                ([r]) => r.endpoint.id === "TarefaColetorController.consultar",
            )?.[0].params,
    ).toEqual({ id: "5" });
    expect(
        vi
            .mocked(t.send)
            .mock.calls.every(([r]) => r.endpoint.method === "GET"),
    ).toBe(true);
});
it("resposta de outro pedido não abre uma tarefa incorreta", async () => {
    render(
        <CollectorTasks
            startAction="pedido:92"
            transport={transport()}
            perfil="OPERACAO"
            context={{}}
        />,
    );
    expect(await screen.findByRole("alert")).toHaveTextContent("outro pedido");
    expect(screen.queryByRole("dialog")).toBeNull();
});
it("atalho de histórico abre o modo manual de leitura sem alterar a fila", async () => {
    render(
        <Collector
            startAction="ExpedicaoController.ler"
            transport={transport()}
            perfil="OPERACAO"
            context={{}}
        />,
    );
    await waitFor(() =>
        expect(screen.getByLabelText("Tarefa")).toHaveValue("ler"),
    );
});
