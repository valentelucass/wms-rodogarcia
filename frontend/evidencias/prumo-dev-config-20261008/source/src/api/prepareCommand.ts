import { validateField, validateValue, InputError } from "../contracts/codec";
import type { Endpoint, Values, Perfil } from "../contracts/runtime";
import type { Request } from "./client";
import { validateContingencyCommand } from "../modules/regularizacao/command";

export interface CommandDraft {
    endpoint: Endpoint;
    params: Values;
    query: Values;
    body: Values;
    file?: File;
    perfil: Perfil;
}
export function prepareCommand({
    endpoint: e,
    params,
    query,
    body,
    file,
    perfil,
}: CommandDraft): Request {
    e.params.forEach((f) =>
        validateField(
            { ...f, positive: ["Long", "long"].includes(f.type) },
            params[f.name],
        ),
    );
    e.query.forEach((f) => validateField(f, query[f.name]));
    if (e.request) validateValue(e.request, body);
    if (
        query.tamanho &&
        (BigInt(String(query.tamanho)) < 1n ||
            BigInt(String(query.tamanho)) > 100n)
    )
        throw new InputError(
            "Itens por página deve estar entre 1 e 100.",
            "tamanho",
        );
    if (e.multipart && (!file || !file.name.endsWith(".xlsx")))
        throw new InputError(
            "Selecione uma planilha .xlsx para conferir a prévia.",
        );
    if (
        perfil !== "GESTOR" &&
        (body.resolverPendentes === true || body.resolucao)
    )
        throw new InputError(
            "Resolução de pendências exige apresentação como Gestor.",
        );
    if (e.id === "ContingenciaController.registrar")
        validateContingencyCommand(body);
    return {
        endpoint: e,
        params: structuredClone(params),
        query: structuredClone(query),
        body: structuredClone(body),
        file,
        signal: new AbortController().signal,
    };
}
