import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { ApiError, type Receipt, type Request, type Transport } from "../src/api/client";
import { FictitiousTransport } from "../src/api/fictitious";
import { AuthApp } from "../src/auth/AuthApp";
import { ReferenceCatalogProvider, useReferenceCatalog } from "../src/components/context/ReferenceCatalog";
import { endpoint, endpoints } from "../src/contracts/runtime";

afterEach(() => {
    vi.unstubAllGlobals();
    history.replaceState(null, "", "/");
});

function CatalogProbe() {
    const catalog = useReferenceCatalog();
    return <><output>{catalog?.loading ? "loading" : catalog?.error ? "error" : catalog?.clientes.map(c => c.nome).join(",")}</output><button onClick={catalog?.refresh}>Retry catalog</button></>;
}
const pageReceipt = (names: string[], pages = 1): Receipt => ({
    data: { itens: names.map((nome, i) => ({ id: String(i + 1), nome, codigo: "DEMO", situacao: "ATIVO" })), pagina: 0, tamanho: 100, totalItens: String(names.length), totalPaginas: pages },
    raw: "", requestId: "isolated", ficticio: true, replay: false,
});

async function workspace() {
    const requests: Request[] = [], responder = new FictitiousTransport("GESTOR");
    vi.stubGlobal("fetch", vi.fn<typeof fetch>(async (input, init) => {
        const url = new URL(String(input), location.origin);
        if (url.pathname === "/api/auth/csrf") return new Response(JSON.stringify({ token: "synthetic-csrf", header: "X-XSRF-TOKEN" }));
        if (url.pathname === "/api/auth/renovar") return new Response(JSON.stringify({ accessToken: "synthetic-memory-only", expiresIn: 300, usuario: { id: "synthetic", nome: "Pessoa fictícia", email: "perf@fixture.invalid", perfil: "GESTOR", administrador: true, principal: false, ativo: true, trocarSenha: false, clientes: [], armazens: [], versao: 0 } }));
        const e = endpoints.find(e => e.path === url.pathname && e.method === "GET");
        if (!e) throw new Error("Unexpected local request: " + url.pathname);
        const r: Request = { endpoint: e, params: {}, query: Object.fromEntries(url.searchParams), signal: init?.signal ?? new AbortController().signal };
        requests.push(r);
        const receipt = await responder.send(r);
        return new Response(receipt.raw, { headers: { "Content-Type": "application/json" } });
    }));
    render(<AuthApp />);
    await screen.findByText("DEMO-BOBINA");
    await waitFor(() => expect(screen.getByRole("combobox", { name: "Cliente" })).toBeEnabled());
    return requests;
}

it("PERF01 trocar contexto reconsulta visão e gráficos sem enumerar de novo catálogos do mesmo alcance", async () => {
    const requests = await workspace();
    const catalogs = () => requests.filter(r => ["ClienteController.listar", "ArmazemController.listar"].includes(r.endpoint.id));
    const initialCatalogCount = catalogs().length;
    expect(initialCatalogCount).toBe(2);
    for (const title of ["Cliente", "Armazém"]) {
        const input = screen.getByRole("combobox", { name: title });
        fireEvent.click(input);
        fireEvent.change(input, { target: { value: title === "Cliente" ? "Cliente fictício 1" : "Centro fictício" } });
        fireEvent.keyDown(input, { key: "Enter" });
    }
    fireEvent.click(screen.getByRole("button", { name: "Aplicar contexto" }));
    await waitFor(() => expect(requests.filter(r => r.endpoint.id === "DashboardController.consultar")).toHaveLength(2));
    await screen.findByText("DEMO-BOBINA");
    expect(requests.filter(r => r.endpoint.id === "VisaoOperacaoController.consultar")).toHaveLength(2);
    expect(requests.filter(r => ["VisaoOperacaoController.consultar", "DashboardController.consultar"].includes(r.endpoint.id)).slice(-2).every(r => r.query.clienteId === "1" && r.query.armazemId === "1")).toBe(true);
    expect(catalogs()).toHaveLength(initialCatalogCount);
});

it("PERF01 Atualizar visão ainda renova explicitamente os catálogos autorizados", async () => {
    const requests = await workspace();
    await waitFor(() => expect(screen.getByRole("button", { name: "Atualizar visão" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Atualizar visão" }));
    await waitFor(() => expect(requests.filter(r => ["ClienteController.listar", "ArmazemController.listar"].includes(r.endpoint.id))).toHaveLength(4));
    await waitFor(() => expect(screen.getByRole("button", { name: "Atualizar visão" })).toBeEnabled());
    expect(requests.filter(r => r.endpoint.id === "VisaoOperacaoController.consultar")).toHaveLength(2);
});

it.each([403, 503])("PERF01 falha %i de catálogo interrompe a paginação irmã e retry explícito recupera", async status => {
    let release!: (r: Receipt) => void;
    let failing = true;
    const requests: Request[] = [];
    const transport: Transport = { send: async r => {
        requests.push(r);
        if (!failing) return pageReceipt(["Autorizado atual"]);
        if (r.endpoint.id === "ClienteController.listar") throw new ApiError("Synthetic refusal", status, "DENIED");
        if (Number(r.query.pagina) === 0) return new Promise<Receipt>(resolve => { release = resolve; });
        return pageReceipt(["Outra página"]);
    } };
    render(<ReferenceCatalogProvider transport={transport}><CatalogProbe /></ReferenceCatalogProvider>);
    await screen.findByText("error");
    await act(async () => release(pageReceipt(["Página atrasada"], 2)));
    expect(requests.filter(r => r.endpoint.id === "ArmazemController.listar")).toHaveLength(1);
    expect(requests.find(r => r.endpoint.id === "ArmazemController.listar")?.signal.aborted).toBe(true);
    failing = false;
    fireEvent.click(screen.getByRole("button", { name: "Retry catalog" }));
    await screen.findByText("Autorizado atual");
});

it("PERF01 alcance substituído esconde nomes antigos e não segue páginas da consulta cancelada", async () => {
    let release!: (r: Receipt) => void;
    const requests: Request[] = [];
    const transport: Transport = { send: async r => {
        requests.push(r);
        if (requests.length === 1) return new Promise<Receipt>(resolve => { release = resolve; });
        return pageReceipt(["Nome atual"]);
    } };
    const view = render(<ReferenceCatalogProvider transport={transport} version="old"><CatalogProbe /></ReferenceCatalogProvider>);
    await waitFor(() => expect(requests).toHaveLength(2));
    const old = requests[0];
    view.rerender(<ReferenceCatalogProvider transport={transport} version="current"><CatalogProbe /></ReferenceCatalogProvider>);
    await screen.findByText("Nome atual");
    await act(async () => release(pageReceipt(["Nome antigo"], 2)));
    expect(screen.queryByText("Nome antigo")).not.toBeInTheDocument();
    expect(old.signal.aborted).toBe(true);
    expect(requests.filter(r => r.signal === old.signal)).toHaveLength(2);
});

it("PERF01 referência recusada não divulga nomes da versão anterior", async () => {
    let denied = false;
    const transport: Transport = { send: async () => { if (denied) throw new ApiError("Refusal", 403, "DENIED"); return pageReceipt(["Nome antes da recusa"]); } };
    const view = render(<ReferenceCatalogProvider transport={transport} version={0}><CatalogProbe /></ReferenceCatalogProvider>);
    await screen.findByText("Nome antes da recusa");
    denied = true;
    view.rerender(<ReferenceCatalogProvider transport={transport} version={1}><CatalogProbe /></ReferenceCatalogProvider>);
    expect(screen.queryByText("Nome antes da recusa")).not.toBeInTheDocument();
    await screen.findByText("error");
    expect(endpoint("ClienteController.listar").method).toBe("GET");
});
