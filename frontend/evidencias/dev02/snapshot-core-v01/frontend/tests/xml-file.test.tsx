import { it, expect, vi } from "vitest";
import { useState } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { Fields } from "../src/components/Fields";
import type { Values } from "../src/contracts/runtime";
function Form() {
    const [values, setValues] = useState<Values>({
        xml: "anterior",
        referencia: "referência inicial",
    });
    return (
        <>
            <Fields
                fields={[
                    { name: "xml", type: "String", required: true },
                    { name: "referencia", type: "String", required: false },
                ]}
                values={values}
                onChange={setValues}
                perfil="GESTOR"
            />
            <output data-testid="values">{JSON.stringify(values)}</output>
        </>
    );
}
function delayedFile(name: string) {
    let resolve!: (text: string) => void;
    let reject!: (error: Error) => void;
    const promise = new Promise<string>((ok, fail) => {
        resolve = ok;
        reject = fail;
    });
    const file = new File(["fictício"], name, { type: "application/xml" });
    Object.defineProperty(file, "text", { value: vi.fn(() => promise) });
    return { file, resolve, reject };
}
function select(file: File) {
    fireEvent.change(screen.getByLabelText(/Carregar arquivo XML/), {
        target: { files: [file] },
    });
}
it("XML B prevalece sobre A atrasado e conserva outros campos editados", async () => {
    render(<Form />);
    const a = delayedFile("A.xml"),
        b = delayedFile("B.xml");
    select(a.file);
    select(b.file);
    fireEvent.change(screen.getByLabelText("Referência"), {
        target: { value: "referência atual" },
    });
    await act(async () => {
        b.resolve("<B/>");
    });
    await act(async () => {
        a.resolve("<A/>");
    });
    expect(screen.getByLabelText("XML existente da NF-e *")).toHaveValue(
        "<B/>",
    );
    expect(screen.getByLabelText("Referência")).toHaveValue("referência atual");
    expect(screen.queryByText(/Lendo arquivo XML/)).not.toBeInTheDocument();
});
it("edição manual encerra intenção de arquivo pendente", async () => {
    render(<Form />);
    const a = delayedFile("A.xml");
    select(a.file);
    fireEvent.change(screen.getByLabelText("XML existente da NF-e *"), {
        target: { value: "<manual/>" },
    });
    await act(async () => {
        a.resolve("<A/>");
    });
    expect(screen.getByLabelText("XML existente da NF-e *")).toHaveValue(
        "<manual/>",
    );
});
it("rejeição é apresentada sem promessa rejeitada fora da UI", async () => {
    render(<Form />);
    const a = delayedFile("A.xml");
    select(a.file);
    await act(async () => {
        a.reject(new Error("arquivo indisponível"));
    });
    expect(screen.getByRole("alert")).toHaveTextContent(
        "Não foi possível ler o arquivo XML",
    );
    expect(screen.getByLabelText("XML existente da NF-e *")).toHaveValue("");
    fireEvent.change(screen.getByLabelText("XML existente da NF-e *"), {
        target: { value: "<manual/>" },
    });
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("desmontagem descarta publicação e rejeição atrasadas", async () => {
    const onChange = vi.fn();
    const { unmount } = render(
        <Fields
            fields={[{ name: "xml", type: "String", required: true }]}
            values={{}}
            onChange={onChange}
            perfil="GESTOR"
        />,
    );
    const a = delayedFile("A.xml");
    select(a.file);
    onChange.mockClear();
    unmount();
    await act(async () => {
        a.reject(new Error("fechado"));
    });
    expect(onChange).not.toHaveBeenCalled();
});
