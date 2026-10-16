import { expect, it } from "vitest";
import {
    absorb,
    emptyWorkflow,
    operationContext,
} from "../src/domain/workflow";
import { initialValue, encode } from "../src/contracts/codec";
import { endpoint, type Values } from "../src/contracts/runtime";
import { prepareCommand } from "../src/api/prepareCommand";

it.each([
    {
        chaveFato: "ENTRADA:UNIDADE:1359",
        unidadeId: "1359",
        pedidoSaidaId: null,
        produtoId: "26",
    },
    {
        chaveFato: "SAIDA:RETIRADA:9007199254740997:SKU:26",
        unidadeId: null,
        pedidoSaidaId: "9007199254740997",
        produtoId: "26",
    },
])(
    "prepara a sugestão $chaveFato sem copiá-la para a referência manual",
    (origin) => {
        const suggestion = {
            ...origin,
            servicoId: "9007199254740995",
            executadoEm: "2026-10-16T02:46:36.454Z",
            quantidade: "1.000000",
            categoria: "",
        };
        const state = absorb(
            emptyWorkflow(),
            "FatoServicoDto.Sugestao",
            suggestion,
            true,
        );
        const context = operationContext(
            "FatoServicoController.registrar",
            {
                clienteId: "83",
                armazemId: "46",
                referenciaExecucao: "MANUAL-ANTERIOR",
            },
            state,
        );
        expect(context.referenciaExecucao).toBeNull();
        const body = initialValue("FatoServicoDto.Registrar", {
            ...context,
            operacaoId: "00000000-0000-4000-8000-000000000001",
            criterioRateio: "Origem consultada e conferida",
            motivo: "Execução fictícia conferida para este registro",
        }) as Values;
        const request = prepareCommand({
            endpoint: endpoint("FatoServicoController.registrar"),
            params: {},
            query: {},
            body,
            perfil: "SUPERVISOR",
        });
        expect(request.body).toMatchObject({
            clienteId: "83",
            armazemId: "46",
            servicoId: suggestion.servicoId,
            origem: "SUGESTAO",
            unidadeId: origin.unidadeId,
            pedidoSaidaId: origin.pedidoSaidaId,
            produtoId: origin.produtoId,
            executadoEm: suggestion.executadoEm,
            quantidade: suggestion.quantidade,
            referenciaExecucao: null,
        });
        expect(encode("FatoServicoDto.Registrar", request.body)).toContain(
            '"referenciaExecucao":null',
        );
        expect(state.selected["FatoServicoDto.Sugestao"]).toBe(suggestion);
        expect(suggestion.chaveFato).toBe(origin.chaveFato);
    },
);

it("conserva a referência informada em uma execução manual sem sugestão", () => {
    const state = absorb(
        emptyWorkflow(),
        "ConfiguracaoCobrancaDto.Servico",
        { id: "9007199254740995" },
        true,
    );
    expect(
        operationContext(
            "FatoServicoController.registrar",
            { origem: "MANUAL", referenciaExecucao: "EXEC-MANUAL-01" },
            state,
        ),
    ).toMatchObject({ origem: "MANUAL", referenciaExecucao: "EXEC-MANUAL-01" });
});
