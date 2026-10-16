import {
    fireEvent,
    render,
    screen,
    within,
    waitFor,
} from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { ReceivingDetail } from "../src/modules/recebimento/ReceivingDetail";
import {
    NfeDocument,
    SavedNfeDocument,
} from "../src/modules/recebimento/NfeDocument";
import { emptyWorkflow } from "../src/domain/workflow";
import type { Request } from "../src/api/client";
import { nfePreview } from "./nfe-example";

const saved = {
    documento: nfePreview.documento,
    xmlOriginal: "<nfe>original fictício</nfe>",
    xmlHash: "a".repeat(64),
};
const receipt = (data: unknown) => ({
    data,
    raw: "",
    requestId: "note-detail-local",
    ficticio: true,
    replay: false,
});

it("seleção conserva os objetos e o XML só é consultado ao abrir, sem duplicar a leitura ao reabrir", async () => {
    const item = {
        id: "9007199254740997",
        produtoId: "9007199254740999",
        numeroItem: 1,
        sku: "0000123",
        unidadeMedida: "KG",
        prevista: "1.125000",
        recebidaBoa: "0",
        recebidaAvariada: "0",
        diferenca: "-1.125000",
    };
    const note = {
        id: "9007199254740995",
        serie: "0",
        numero: "0000123",
        emissao: "2026-10-15",
        xmlVinculado: true,
        itens: [item],
    };
    const select = vi.fn(),
        send = vi.fn(async (request: Request) => {
            expect(request.endpoint.id).toBe(
                "PedidoEntradaXmlController.documento",
            );
            return receipt(saved);
        });
    render(
        <ReceivingDetail
            record={{
                pedido: {
                    id: "9007199254740993",
                    referencia: "ENT-000013",
                    situacao: "QUARENTENA",
                    versao: "5",
                },
                notas: [note],
                divergente: false,
            }}
            section="Notas e itens"
            onSection={vi.fn()}
            onSelect={select}
            locked={false}
            workflow={emptyWorkflow()}
            transport={{ send }}
        />,
    );
    expect(send).not.toHaveBeenCalled();
    const items = screen.getByRole("region", {
        name: "Itens da nota 0000123",
    });
    const toggle = screen.getByRole("button", {
        name: "Ver documento original",
    });
    expect(
        items.compareDocumentPosition(toggle) &
            Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    fireEvent.click(
        within(items).getByRole("button", {
            name: "Selecionar item 9007199254740997 da nota 0000123",
        }),
    );
    expect(select.mock.calls[0]).toEqual([note, "PedidoEntradaDto.Nota"]);
    expect(select.mock.calls[0][0]).toBe(note);
    expect(select.mock.calls[1][0]).toBe(item);
    fireEvent.click(toggle);
    await waitFor(() =>
        expect(
            screen.getByRole("button", { name: "Baixar XML original" }),
        ).toBeEnabled(),
    );
    expect(send.mock.calls[0][0].endpoint.id).toBe(
        "PedidoEntradaXmlController.documento",
    );
    expect(send.mock.calls[0][0].params).toEqual({
        pedidoId: "9007199254740993",
        notaId: "9007199254740995",
    });
    const additional = screen
        .getByText("Transporte, protocolo e informações adicionais")
        .closest("details");
    expect(additional).not.toHaveAttribute("open");
    fireEvent.click(
        screen.getByRole("button", { name: "Ocultar documento original" }),
    );
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(toggle);
    await screen.findByRole("button", { name: "Baixar XML original" });
    expect(send).toHaveBeenCalledTimes(1);
});

it("recusa mantém erro legível, nova leitura exige ação e bloqueio desabilita consulta e download", async () => {
    const send = vi
        .fn()
        .mockRejectedValueOnce(
            new Error("Documento indisponível nesta consulta"),
        )
        .mockResolvedValue(receipt(saved));
    const { rerender } = render(
        <SavedNfeDocument
            transport={{ send }}
            pedidoId="91"
            notaId="92"
            disabled={false}
        />,
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Ver documento original" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Documento indisponível",
    );
    expect(send).toHaveBeenCalledTimes(1);
    fireEvent.click(
        screen.getByRole("button", { name: "Ocultar documento original" }),
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Ver documento original" }),
    );
    await screen.findByRole("button", { name: "Baixar XML original" });
    expect(send).toHaveBeenCalledTimes(2);
    rerender(
        <SavedNfeDocument
            transport={{ send }}
            pedidoId="91"
            notaId="92"
            disabled
        />,
    );
    await waitFor(() =>
        expect(
            screen.getByRole("button", { name: "Baixar XML original" }),
        ).toBeDisabled(),
    );
    expect(
        screen.getByRole("button", { name: "Ocultar documento original" }),
    ).toBeDisabled();
});

it("campos ausentes não viram ambiente ou operação fiscal conhecidos e a prévia mantém os adicionais abertos", () => {
    const documento = {
        ...nfePreview.documento!,
        ambiente: null,
        tipoOperacao: null,
        modelo: null,
        versao: null,
        serie: 0,
        valorTotal: "9007199254740993.1250",
    };
    render(<NfeDocument documento={documento} />);
    const document = screen.getByRole("region", {
        name: "Documento original da NF-e",
    });
    expect(document).toHaveTextContent("série 0");
    expect(document).toHaveTextContent("9007199254740993.1250");
    expect(
        within(document).queryByText("Homologação", { exact: true }),
    ).not.toBeInTheDocument();
    expect(
        within(document).queryByText("Entrada", { exact: true }),
    ).not.toBeInTheDocument();
    expect(within(document).getByText("— / —")).toBeVisible();
    expect(
        screen
            .getByText("Transporte, protocolo e informações adicionais")
            .closest("details"),
    ).toHaveAttribute("open");
    expect(screen.getByText("24")).toBeVisible();
});
