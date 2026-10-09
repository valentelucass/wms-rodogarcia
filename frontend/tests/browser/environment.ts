// Parâmetros apenas do processo de prova local; nenhum destino API.
const port = process.env.WMS_FE_TEST_PORT ?? "5188";
export const proofLabel = process.env.WMS_FE_PROOF_LABEL ?? "marco02-local";
if (!/^[1-9]\d{0,4}$/.test(port) || Number(port) > 65535)
    throw new Error("Porta de prova frontend inválida");
if (!/^[a-z0-9-]+$/.test(proofLabel))
    throw new Error("Rótulo de prova frontend inválido");
export const origin = `http://127.0.0.1:${port}`;
