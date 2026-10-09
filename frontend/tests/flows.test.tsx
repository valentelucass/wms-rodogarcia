import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "../src/App";
import { Operation } from "../src/components/Operation";
import { endpoint } from "../src/contracts/runtime";
import { FictitiousTransport } from "../src/api/fictitious";
import { demoCode } from "../src/api/mock/constants";
import { ApiError, type Transport } from "../src/api/client";
describe("Jornadas e segurança de apresentação", () => {
    beforeEach(() => vi.stubEnv("VITE_DATA_MODE", "ficticio"));
    afterEach(() => vi.unstubAllEnvs());
    it("FE04 cadastra cliente com confirmação explícita e impede duplo envio", async () => {
        const t = new FictitiousTransport();
        const user = userEvent.setup();
        render(
            <Operation
                id="ClienteController.criar"
                transport={t}
                context={{}}
                perfil="GESTOR"
                onSelect={vi.fn()}
            />,
        );
        await user.type(screen.getByLabelText("Código *"), "DEMO");
        await user.type(screen.getByLabelText("Nome *"), "Cliente fictício");
        await user.type(
            screen.getByLabelText("CPF / CNPJ *"),
            "00000000000000",
        );
        await user.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        expect(t.requests).toHaveLength(0);
        const button = screen.getByRole("button", { name: "Confirmar agora" });
        fireEvent.click(button);
        fireEvent.click(button);
        await screen.findByText(/Resposta FICTÍCIA/);
        expect(t.requests).toHaveLength(1);
        expect(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        ).toBeDisabled();
    });
    it("FE09 conflito não vira sucesso/vazio; correlação visível", async () => {
        const t = new FictitiousTransport();
        t.scenario = "conflito";
        render(
            <Operation
                id="PedidoSaidaController.sugerir"
                transport={t}
                context={{ id: "1" }}
                perfil="OPERACAO"
                onSelect={vi.fn()}
            />,
        );
        await userEvent.click(
            screen.getByRole("button", { name: "Consultar" }),
        );
        await screen.findByText(/revisão mudou/);
        expect(screen.queryByText(/Resposta FICTÍCIA/)).not.toBeInTheDocument();
        expect(screen.getByText(/demo-409/)).toBeInTheDocument();
    });
    it("FE02 resultado desconhecido bloqueia edição e replay usa payload original", async () => {
        const t = new FictitiousTransport();
        t.scenario = "perdida";
        const user = userEvent.setup();
        render(
            <Operation
                id="EstoqueController.bloquear"
                transport={t}
                context={{ codigo: demoCode, versaoUnidade: "0" }}
                perfil="OPERACAO"
                onSelect={vi.fn()}
            />,
        );
        await user.type(
            screen.getByLabelText("Motivo / justificativa *"),
            "Bloqueio de exercício",
        );
        await user.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        await user.click(
            screen.getByRole("button", { name: "Confirmar agora" }),
        );
        await screen.findByText(/Resultado desconhecido/);
        expect(
            screen.getByLabelText("Motivo / justificativa *"),
        ).toBeDisabled();
        await user.click(
            screen.getByRole("button", {
                name: "Repetir exatamente a mesma operação",
            }),
        );
        await screen.findByText(/Repetição: confirmação original/);
        expect(t.requests[0].body).toEqual(t.requests[1].body);
    });
    it("FE03 Operação não recebe cadastro crítico nem confirmação física/fiscal", async () => {
        location.hash = "#cadastros";
        render(<App />);
        const user = userEvent.setup();
        await user.selectOptions(
            screen.getByLabelText("Perfil de apresentação fictício"),
            "OPERACAO",
        );
        expect(
            screen.queryByRole("button", { name: "Novo registro" }),
        ).not.toBeInTheDocument();
        await user.click(
            screen.getByRole("button", { name: "Fiscal e retirada" }),
        );
        expect(
            screen.queryByRole("button", {
                name: "Registrar documento existente",
            }),
        ).not.toBeInTheDocument();
        await user.click(
            screen.getByRole("button", { name: "2. Retirada física integral" }),
        );
        expect(
            screen.queryByRole("button", { name: "Confirmar retirada física" }),
        ).not.toBeInTheDocument();
    });
    it("FE03 troca de contexto aborta e remove resposta anterior", async () => {
        location.hash = "#entrada";
        render(<App />);
        const user = userEvent.setup();
        await user.selectOptions(
            screen.getByLabelText("Resposta do exercício fictício"),
            "lento",
        );
        await user.click(screen.getByRole("button", { name: "Consultar" }));
        await user.clear(screen.getByLabelText("Cliente fictício (ID)"));
        await user.type(screen.getByLabelText("Cliente fictício (ID)"), "2");
        await user.click(
            screen.getByRole("button", { name: "Aplicar contexto" }),
        );
        expect(screen.queryByText(/Carregando/)).not.toBeInTheDocument();
        await waitFor(() =>
            expect(screen.getByLabelText("Cliente (ID) *")).toHaveValue("2"),
        );
    });
    it("FE05 teclado submete consulta e distingue vazio de erro", async () => {
        const t = new FictitiousTransport();
        t.scenario = "vazio";
        const user = userEvent.setup();
        render(
            <Operation
                id="PedidoEntradaController.listar"
                transport={t}
                context={{ clienteId: "1", armazemId: "1" }}
                perfil="OPERACAO"
                onSelect={vi.fn()}
            />,
        );
        screen.getByLabelText("Cliente (ID) *").focus();
        await user.keyboard("{Enter}");
        await screen.findByText("Nenhum registro encontrado.");
        expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    });
    it("FE03 sessão expirada fica explícita e não guarda token", async () => {
        const t: Transport = {
            send: vi
                .fn()
                .mockRejectedValue(
                    new ApiError("Sessão expirada.", 401, "NAO_AUTENTICADO"),
                ),
        };
        render(
            <Operation
                id="ClienteController.listar"
                transport={t}
                context={{}}
                perfil="GESTOR"
                onSelect={vi.fn()}
            />,
        );
        await userEvent.click(
            screen.getByRole("button", { name: "Consultar" }),
        );
        await screen.findByText(/Retorne à seleção fictícia/);
        expect(localStorage.length).toBe(0);
    });
    it("FE06/FE11 ações de reimpressão e demonstrativo são GET", () => {
        expect(endpoint("UnidadeLogisticaController.etiqueta").method).toBe(
            "GET",
        );
        expect(
            endpoint("FechamentoCobrancaController.demonstrativo").method,
        ).toBe("GET");
    });
});
