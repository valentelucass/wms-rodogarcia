export const dispatchOrderSample = {
    clienteId: "9007199254740993",
    armazemId: "9007199254740995",
    itens: [{ produtoId: "9007199254740997", quantidade: "1.125000" }],
    motivo: "Pedido fictício integral.\nPreservar a justificativa completa.",
};
const clients = [
    {
        id: dispatchOrderSample.clienteId,
        nome: "Cliente fictício de demonstração",
        codigo: "CLI-000001",
        situacao: "ATIVO",
    },
];
const warehouses = [
    {
        id: dispatchOrderSample.armazemId,
        nome: "Armazém fictício de demonstração",
        codigo: "ARM-000001",
        situacao: "ATIVO",
    },
];
const products = [
    {
        id: "9007199254740997",
        clienteId: dispatchOrderSample.clienteId,
        sku: "0000123",
        descricao: "Produto fictício com identificação preservada",
        unidadeMedida: "KG",
        situacao: "ATIVO",
    },
    {
        id: "9007199254740999",
        clienteId: dispatchOrderSample.clienteId,
        sku: "0000456",
        descricao: "Segundo produto fictício de demonstração",
        unidadeMedida: "KG",
        situacao: "ATIVO",
    },
];
export const dispatchOrderReferences = {
    defaults: { produtoId: products[0].id },
    options: {
        clienteId: clients.map((row) => ({ value: row.id, title: row.nome })),
        armazemId: warehouses.map((row) => ({
            value: row.id,
            title: row.nome,
        })),
        produtoId: products.map((row) => ({
            value: row.id,
            title: `${row.sku} · ${row.descricao}`,
        })),
    },
    records: {
        "ClienteDto.Resposta": clients,
        "ArmazemDto.Resposta": warehouses,
        "ProdutoDto.Resposta": products,
    },
};
