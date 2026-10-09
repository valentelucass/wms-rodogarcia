import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Operation } from "../src/components/Operation";
import { FictitiousTransport } from "../src/api/fictitious";
import type { Values } from "../src/contracts/runtime";

const observed: Values = {
    codigoUnidade: "00000000-0000-4000-8000-000000000003",
    versaoUnidade: "0",
    contado: "8.000000",
    observadoEm: "2026-10-08T12:00:00.123456Z",
    motivo: "Contagem fictícia FE12",
};
describe("FE12 — fronteiras da apresentação", () => {
    it.each([
        "ContagemController.aplicar",
        "CargaInicialController.criar",
        "ContingenciaController.conciliar",
    ])("Operação não apresenta nem envia %s", (id) => {
        const transport = new FictitiousTransport("OPERACAO");
        render(
            <Operation
                id={id}
                transport={transport}
                context={{}}
                perfil="OPERACAO"
                onSelect={vi.fn()}
            />,
        );
        expect(screen.getByRole("status")).toHaveTextContent("SUPERVISOR");
        expect(
            screen.queryByRole("button", { name: "Conferir e confirmar" }),
        ).not.toBeInTheDocument();
        expect(transport.requests).toEqual([]);
    });
    it("Supervisor conta com confirmação, sem Aplicar automático e sem duplo envio", async () => {
        const transport = new FictitiousTransport("SUPERVISOR");
        const onReceipt = vi.fn();
        render(
            <Operation
                id="ContagemController.contar"
                transport={transport}
                context={observed}
                perfil="SUPERVISOR"
                onSelect={vi.fn()}
                onReceipt={onReceipt}
            />,
        );
        await userEvent.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        expect(transport.requests).toHaveLength(0);
        const confirm = screen.getByRole("button", { name: "Confirmar agora" });
        fireEvent.click(confirm);
        fireEvent.click(confirm);
        await screen.findByText(/Resposta FICTÍCIA/);
        expect(transport.requests.map((r) => r.endpoint.id)).toEqual([
            "ContagemController.contar",
        ]);
        expect(onReceipt.mock.calls[0][0].data).toMatchObject({
            contado: "8.000000",
            efeitoJson: null,
            situacao: "PENDENTE",
        });
        expect(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        ).toBeDisabled();
    });
    it("recusa recebida fica visível sem sucesso nem absorção de referências", async () => {
        const transport = new FictitiousTransport("SUPERVISOR");
        transport.scenario = "negada";
        const onReceipt = vi.fn();
        render(
            <Operation
                id="ContagemController.contar"
                transport={transport}
                context={observed}
                perfil="SUPERVISOR"
                onSelect={vi.fn()}
                onReceipt={onReceipt}
            />,
        );
        await userEvent.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        await userEvent.click(
            screen.getByRole("button", { name: "Confirmar agora" }),
        );
        await screen.findByText(/ação recusada ao perfil/);
        expect(screen.queryByText(/Resposta FICTÍCIA/)).not.toBeInTheDocument();
        expect(onReceipt).not.toHaveBeenCalled();
        expect(transport.requests.map((r) => r.endpoint.id)).toEqual([
            "ContagemController.contar",
        ]);
    });
});
