import type { Values } from "../../src/contracts/runtime";

export const auditBefore = `{"id":9223372036854775807,"versao":0,"identidadeFato":"QA-PLANILHA-CONT-L1","clienteId":83,"armazemId":46,"tipo":"CONTAGEM","ocorridaEm":"2026-10-16T01:30:00.123456789Z","conteudoHash":"${"a".repeat(64)}","conteudo":{"dados":{"codigoUnidade":"00000000-0000-4000-8000-000000000003","contado":5.000001,"motivo":"QA fictícia — recontagem física sem ajuste de quantidade","observadoEm":"2026-10-16T01:30:00.123456789Z","versaoUnidade":15}},"dependencias":[],"efeitoRegistradoNoWms":false,"situacao":"PENDENTE","pendencia":"CONCILIACAO_PENDENTE","conciliadaEm":null,"resultado":null}`;
export const auditAfter = auditBefore
    .replace('"versao":0', '"versao":1')
    .replace('"situacao":"PENDENTE"', '"situacao":"CONCILIADA"')
    .replace('"pendencia":"CONCILIACAO_PENDENTE"', '"pendencia":null')
    .replace(
        '"resultado":null',
        '"resultado":{"efeito":{"id":9007199254740993,"versao":2,"unidadeId":1358,"codigoUnidade":"00000000-0000-4000-8000-000000000003","revisao":3,"esperado":5.000001,"contado":5.000001,"diferenca":0.000000,"reservado":0,"observadoEm":"2026-10-16T01:30:00.123456789Z","situacao":"RECONCILIADA","impedimento":false,"origens":[{"entradaId":114,"itemNotaId":100,"quantidade":5.000001}],"efeitoJson":null},"identidadeFato":"QA-PLANILHA-CONT-L1","modo":"EXECUTAR","ocorridaEm":"2026-10-16T01:30:00.123456789Z","operacaoEfeito":"00000000-0000-4000-8000-000000000004","prova":null}',
    );
export const auditDetailSample = {
    id: "9007199254740995",
    tipo: "CONTINGENCIA",
    registroId: "9223372036854775807",
    acao: "CONCILIACAO_CONTINGENCIA",
    usuario: "00000000-0000-4000-8000-000000000001",
    instante: "2026-10-16T01:32:36.083734Z",
    idOperacao: "00000000-0000-4000-8000-000000000002",
    motivo: "QA fictícia — executar somente a recontagem já conferida, sem ajuste de quantidade.",
    dadosAntes: auditBefore,
    dadosDepois: auditAfter,
    tipoFisico: "CONTINGENCIA",
} satisfies Values;
export const auditDetailReferences = {
    defaults: {},
    options: {},
    records: {
        "ContingenciaDto.Resultado": [
            {
                id: "9223372036854775807",
                identidadeFato: "QA-PLANILHA-CONT-L1",
            },
        ],
    },
};
