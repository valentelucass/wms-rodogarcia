import { afterEach, expect, it, vi } from "vitest";
import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import QRCode from "qrcode";
import { LabelPreview } from "../src/components/LabelPreview";
import { OperationResults } from "../src/components/operation/OperationResults";
import { Operation } from "../src/components/Operation";
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../src/api/client";
import { exampleFixtures } from "../src/modules/recebimento/exampleFixtures";
import { nextActions } from "../src/domain/journeys";

vi.mock("qrcode", () => ({
    default: {
        toDataURL: vi.fn(
            async (code: string) => `data:image/png;base64,${btoa(code)}`,
        ),
    },
}));
afterEach(() => vi.restoreAllMocks());
const id = "UnidadeLogisticaController.etiqueta";
const label = exampleFixtures["UnidadeLogisticaDto.Etiqueta"];
const receipt: Receipt = {
    data: label,
    raw: "",
    ficticio: false,
    replay: false,
    requestId: "ETIQUETA-TESTE-01",
};

it("mostra o lote original da unidade na etiqueta sem substituir os códigos de leitura", async () => {
    render(
        <LabelPreview
            data={{ ...label, controlaLote: true, lote: "8CB2654A" }}
        />,
    );
    const preview = screen.getByRole("region", { name: "Prévia da etiqueta" });
    expect(within(preview).getByText("L: 8CB2654A")).toBeVisible();
    expect(
        within(preview).getByRole("img", {
            name: `Código de barras do UUID ${label.codigoLeitura}`,
        }),
    ).toBeInTheDocument();
    expect(
        await within(preview).findByRole("img", {
            name: `QR do UUID ${label.codigoLeitura}`,
        }),
    ).toBeInTheDocument();
    expect(
        within(preview).getByRole("button", { name: "Imprimir etiqueta" }),
    ).toBeEnabled();
});

it.each([
    { controlaLote: true, lote: null, texto: "L: Não informado" },
    { controlaLote: false, lote: null, texto: "L: Não controlado" },
    { controlaLote: true, lote: "0", texto: "L: 0" },
])("preserva a informação de lote: $texto", ({ controlaLote, lote, texto }) => {
    render(<LabelPreview data={{ ...label, controlaLote, lote }} />);
    expect(screen.getByText(texto)).toBeVisible();
});

it("imprime somente por ação explícita e mantém a navegação separada, com dados complementares recolhidos", async () => {
    const print = vi.spyOn(window, "print").mockImplementation(() => {}),
        onContinue = vi.fn(),
        onSelect = vi.fn();
    render(
        <OperationResults
            id={id}
            receipt={receipt}
            type="UnidadeLogisticaDto.Etiqueta"
            onSelect={onSelect}
            onContinue={onContinue}
        />,
    );
    const preview = screen.getByRole("region", { name: "Prévia da etiqueta" });
    const printButton = within(preview).getByRole("button", {
        name: "Imprimir etiqueta",
    });
    await waitFor(() => expect(printButton).toBeEnabled());
    const metadata = screen
        .getByText("Dados da unidade consultada")
        .closest("details");
    expect(metadata).not.toHaveAttribute("open");
    expect(
        screen.getByText("Como ler a etiqueta").closest("details"),
    ).not.toHaveAttribute("open");
    expect(print).not.toHaveBeenCalled();
    fireEvent.click(printButton);
    expect(print).toHaveBeenCalledTimes(1);
    expect(onContinue).not.toHaveBeenCalled();
    expect(onSelect).not.toHaveBeenCalled();
    fireEvent.click(
        screen.getByRole("button", {
            name: "Endereçar esta unidade no coletor",
        }),
    );
    expect(onContinue).toHaveBeenCalledExactlyOnceWith(nextActions[id]);
    expect(preview.querySelectorAll(".label-preview")).toHaveLength(1);
    expect(preview.querySelector(".label-uuid")).toHaveTextContent(
        String(label.codigoLeitura),
    );
    expect(preview.querySelector(".label-sequence")).toHaveTextContent("1/10");
    expect(
        within(metadata!).getByText("7", { exact: true }),
    ).toBeInTheDocument();
});

it("bloqueia impressão e continuação durante atualização e não oferece continuação de resposta histórica", async () => {
    const print = vi.spyOn(window, "print").mockImplementation(() => {}),
        onContinue = vi.fn();
    const view = render(
        <OperationResults
            id={id}
            receipt={receipt}
            type="UnidadeLogisticaDto.Etiqueta"
            onSelect={() => {}}
            onContinue={onContinue}
            pending
        />,
    );
    await screen.findByRole("img", {
        name: `QR do UUID ${label.codigoLeitura}`,
    });
    const button = screen.getByRole("button", { name: "Imprimir etiqueta" }),
        next = screen.getByRole("button", {
            name: "Endereçar esta unidade no coletor",
        });
    expect(button).toBeDisabled();
    expect(next).toBeDisabled();
    fireEvent.click(button);
    fireEvent.click(next);
    expect(print).not.toHaveBeenCalled();
    expect(onContinue).not.toHaveBeenCalled();
    view.rerender(
        <OperationResults
            id={id}
            receipt={{ ...receipt, replay: true }}
            type="UnidadeLogisticaDto.Etiqueta"
            onSelect={() => {}}
            onContinue={onContinue}
        />,
    );
    expect(
        screen.queryByRole("button", {
            name: "Endereçar esta unidade no coletor",
        }),
    ).not.toBeInTheDocument();
    expect(screen.getByText(/Confirmação histórica/)).toBeInTheDocument();
    expect(button).toBeEnabled();
});

it("não libera a impressão quando a geração do QR falha", async () => {
    vi.mocked(QRCode.toDataURL).mockRejectedValueOnce(
        new Error("QR indisponível"),
    );
    const print = vi.spyOn(window, "print").mockImplementation(() => {});
    render(<LabelPreview data={label} ficticio />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Não foi possível gerar o QR",
    );
    const button = screen.getByRole("button", {
        name: "Imprimir prévia fictícia",
    });
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(print).not.toHaveBeenCalled();
});

it("consulta o UUID exato, conserva a prévia anterior bloqueada na recarga e permite recuperar após erro", async () => {
    const requests: Request[] = [];
    let resolve!: (value: Receipt) => void, reject!: (error: Error) => void;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            return new Promise<Receipt>((done, fail) => {
                resolve = done;
                reject = fail;
            });
        },
    };
    render(
        <Operation
            id={id}
            transport={transport}
            perfil="OPERACAO"
            context={{ codigo: label.codigoLeitura }}
            onSelect={() => {}}
            onContinue={() => {}}
            inDialog
            hideHeading
        />,
    );
    const consult = screen.getByRole("button", {
        name: "Consultar",
    });
    fireEvent.click(consult);
    expect(requests).toHaveLength(1);
    expect(requests[0].endpoint.method).toBe("GET");
    expect(requests[0].params.codigo).toBe(label.codigoLeitura);
    await act(async () => resolve(receipt));
    await waitFor(() =>
        expect(
            screen.getByRole("button", { name: "Imprimir etiqueta" }),
        ).toBeEnabled(),
    );
    fireEvent.click(consult);
    expect(
        screen.getByRole("heading", {
            name: "Resultado anterior · aguardando atualização",
        }),
    ).toBeInTheDocument();
    expect(
        screen.getByRole("button", { name: "Imprimir etiqueta" }),
    ).toBeDisabled();
    expect(
        screen.getByRole("button", {
            name: "Endereçar esta unidade no coletor",
        }),
    ).toBeDisabled();
    await act(async () =>
        reject(
            new ApiError(
                "Etiqueta não encontrada.",
                404,
                "NAO_ENCONTRADA",
                "ETIQUETA-TESTE-02",
            ),
        ),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Etiqueta não encontrada.",
    );
    expect(
        screen.queryByRole("region", { name: "Prévia da etiqueta" }),
    ).not.toBeInTheDocument();
    fireEvent.click(consult);
    await act(async () => resolve({ ...receipt, ficticio: true }));
    await waitFor(() =>
        expect(
            screen.getByRole("button", { name: "Imprimir prévia fictícia" }),
        ).toBeEnabled(),
    );
    expect(requests).toHaveLength(3);
    expect(
        requests.every(
            (request) => request.params.codigo === label.codigoLeitura,
        ),
    ).toBe(true);
});
