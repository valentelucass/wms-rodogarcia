import { describe, it, expect, vi } from "vitest";
import {
    render,
    screen,
    within,
    fireEvent,
    waitFor,
} from "@testing-library/react";
import { RecordDialog } from "../src/components/records/RecordDialog";
import { emptyWorkflow } from "../src/domain/workflow";
import { journeys } from "../src/domain/journeys";
import { fixture } from "../src/api/mock/fixtures";
import type { Request, Transport } from "../src/api/client";
import type { Values } from "../src/contracts/runtime";

function setup(holdWrite = false) {
    const requests: Request[] = [];
    const client = {
        ...(fixture("ClienteDto.Resposta") as Values),
        id: "9007199254740993",
        nome: "Cliente Alfa",
        codigo: "CLI-ALFA",
        versao: "2",
    };
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            if (holdWrite && request.endpoint.method !== "GET")
                return new Promise(() => {});
            return {
                data: {
                    tipo: "CLIENTE",
                    id: "9007199254740993",
                    versao: "3",
                    situacao: "ATIVO",
                    impedimentos: [],
                },
                ficticio: true,
                raw: "",
                replay: false,
                requestId: "DIALOG-ORGANIZATION-LOCAL",
            };
        },
    };
    render(
        <RecordDialog
            opening={{ row: client, type: "ClienteDto.Resposta" }}
            page={{
                source: "ClienteController.listar",
                detail: "EncerramentoController.consultar",
                headers: [],
            }}
            actions={["solicitar", "inativar", "remanescente", "vigencia"].map(
                (command) => `EncerramentoController.${command}`,
            )}
            transport={transport}
            context={{ tipo: "CLIENTE" }}
            workflow={emptyWorkflow()}
            perfil="GESTOR"
            onClose={vi.fn()}
            onUnknownClose={vi.fn()}
            onReceipt={vi.fn()}
            onRecord={vi.fn()}
            onNavigate={vi.fn()}
            journey={journeys.find((journey) => journey.id === "cadastros")!}
        />,
    );
    return requests;
}
async function start() {
    await screen.findByRole("heading", { name: "Encerramento do cadastro" });
    fireEvent.click(screen.getByText("Operações deste registro"));
    await screen.findByRole("button", {
        name: "Solicitar encerramento definitivo",
    });
    fireEvent.click(
        screen.getByRole("button", {
            name: "Solicitar encerramento definitivo",
        }),
    );
    await screen.findByRole("heading", {
        name: "Solicitar encerramento definitivo",
    });
}

describe("Organização do diálogo por registro", () => {
    it("identifica a ação e o cadastro, retira a faixa vazia e separa a navegação", async () => {
        setup();
        await start();
        expect(
            screen.getByRole("region", { name: "Registro da operação" }),
        ).toHaveTextContent("Cliente Alfa");
        expect(
            screen.queryByRole("combobox", { name: "Tipo *" }),
        ).not.toBeInTheDocument();
        expect(screen.getByLabelText("Tipo *")).toHaveValue("CLIENTE");
        expect(screen.getByLabelText("Tipo *")).toBeDisabled();
        expect(
            screen.queryByRole("group", { name: "Registro selecionado" }),
        ).not.toBeInTheDocument();
        expect(
            screen.queryByRole("button", { name: "Confirmar inativação" }),
        ).not.toBeVisible();
        expect(
            document.querySelector(".record-other-operations"),
        ).not.toHaveAttribute("open");
        const footer = document.querySelector(
            ".record-dialog-footer",
        )! as HTMLElement;
        expect(
            within(footer)
                .getAllByRole("button")
                .map((button) => button.textContent),
        ).toEqual([
            "Voltar à lista",
            "Ver detalhes do registro",
            "Recarregar dados atuais",
        ]);
    });

    it("protege o texto ao trocar a operação pelo menu e permite a troca confirmada", async () => {
        setup();
        await start();
        const confirmation = vi.spyOn(window, "confirm").mockReturnValue(false);
        fireEvent.change(screen.getByLabelText("Motivo / justificativa *"), {
            target: { value: "Justificativa em edição" },
        });
        fireEvent.click(screen.getByText("Outras operações"));
        fireEvent.click(
            screen.getByRole("button", { name: "Confirmar inativação" }),
        );
        expect(
            screen.getByRole("heading", {
                name: "Solicitar encerramento definitivo",
            }),
        ).toBeInTheDocument();
        expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
            "Justificativa em edição",
        );
        confirmation.mockReturnValue(true);
        fireEvent.click(
            screen.getByRole("button", { name: "Confirmar inativação" }),
        );
        await screen.findByRole("heading", { name: "Confirmar inativação" });
        expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
            "",
        );
        confirmation.mockRestore();
    });

    it("preserva identidade, tipo e revisão atual na confirmação e bloqueia alternativas durante envio", async () => {
        const requests = setup(true);
        await start();
        fireEvent.change(screen.getByLabelText("Motivo / justificativa *"), {
            target: { value: "Encerramento conferido" },
        });
        fireEvent.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        expect(
            requests.filter((r) => r.endpoint.method !== "GET"),
        ).toHaveLength(0);
        fireEvent.click(
            screen.getByRole("button", { name: "Confirmar agora" }),
        );
        await screen.findByRole("button", { name: "Aguardando confirmação…" });
        const mutation = requests.find((r) => r.endpoint.method !== "GET")!;
        expect(mutation.params).toMatchObject({
            tipo: "CLIENTE",
            id: "9007199254740993",
        });
        expect(mutation.body).toMatchObject({
            versao: "3",
            motivo: "Encerramento conferido",
        });
        fireEvent.click(screen.getByText("Outras operações"));
        await waitFor(() =>
            expect(
                screen.getByRole("button", { name: "Confirmar inativação" }),
            ).toBeDisabled(),
        );
        expect(
            screen.getByRole("button", { name: "Voltar à lista" }),
        ).toBeDisabled();
        expect(
            screen.getByRole("button", { name: "Fechar diálogo" }),
        ).toBeDisabled();
    });
});
