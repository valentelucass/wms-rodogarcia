import { isObject, type Values } from "../../contracts/runtime";
import { ApiError, type Request } from "../../api/client";
import { fixture } from "../../api/mock/fixtures";
// Pre-authored exercise states and values, never commercial calculation.
export class ExampleBilling {
    private closure: Values | undefined;
    private closureVersion: Values | undefined;
    private documentSequence = 0;
    private treatments: Values[] = [];
    private previousVersions: Values[] = [];
    private adjustments: Values[] = [];
    respond(r: Request, body: Values, data: unknown): unknown {
        if (
            r.endpoint.id.startsWith("FechamentoCobrancaController.") &&
            isObject(data) &&
            isObject(data.fechamento) &&
            isObject(data.versao)
        ) {
            const op = r.endpoint.id.split(".")[1];
            // O fixture do comando não substitui a identidade/hash da versão
            // ativa. Documentos externos pertencem à versão em que chegaram.
            const version = { ...data.versao, ...this.closureVersion };
            const documents: Values[] = Array.isArray(version.nfse)
                ? structuredClone(version.nfse as Values[])
                : [];
            const status =
                op === "aprovar"
                    ? "APROVADO"
                    : op === "rejeitar"
                      ? "REJEITADO"
                      : op === "registrarNfse" && documents.length > 0
                        ? "CONFLITO_EXTERNO"
                        : op === "tratar"
                          ? "APROVADO"
                          : (this.closure?.situacao ?? "PREPARADO");
            this.closure = {
                ...data.fechamento,
                ...this.closure,
                situacao: status,
                versao: String(
                    BigInt(String(this.closure?.versao ?? "-1")) + 1n,
                ),
            };
            this.closureVersion = {
                ...version,
                situacao:
                    op === "aprovar"
                        ? "APROVADA"
                        : op === "rejeitar"
                          ? "REJEITADA"
                          : (this.closureVersion?.situacao ?? "PREPARADA"),
                estadoExterno:
                    op === "entregar"
                        ? "ENTREGUE"
                        : op === "registrarNfse"
                          ? documents.length > 0
                              ? "CONFLITO_EXTERNO"
                              : "EMITIDO"
                          : op === "tratar"
                            ? "EMITIDO"
                            : (this.closureVersion?.estadoExterno ??
                              "NAO_ENTREGUE"),
            };
            if (op === "registrarNfse")
                documents.push({
                    ...(fixture("FechamentoCobrancaDto.Documento") as Values),
                    id: String(1101 + this.documentSequence++),
                    referenciaExterna: body.referenciaExterna,
                    numero: body.numeroDocumento,
                    emissorDocumento: body.emissorDocumento,
                    emitidaEm: body.emitidaEm,
                });
            if (op === "tratar")
                this.treatments.push({
                    ...(fixture("FechamentoCobrancaDto.Tratativa") as Values),
                    id: "1201",
                    fechamentoId: "901",
                    versaoResultadoId: this.closureVersion.id,
                    resultado: body.resultado,
                    referencias: body.referencias,
                    fonte: body.fonte,
                    conferidaPor: body.conferidaPor,
                    conferidaEm: body.conferidaEm,
                    motivo: body.motivo,
                });
            this.closureVersion.nfse = documents;
            if (op === "aprovar" || op === "rejeitar") {
                this.closureVersion.decididaEm = "2026-10-08T12:45:00.123456Z";
                this.closureVersion.decisor = "GESTOR-FICTICIO";
                this.closureVersion.motivoDecisao =
                    body.motivo ?? "Decisão integral fictícia";
            }
            if (op === "entregar")
                this.closureVersion.entregas = [
                    ...(Array.isArray(version.entregas)
                        ? version.entregas
                        : []),
                    {
                        ...(fixture("FechamentoCobrancaDto.Entrega") as Values),
                        id: "1301",
                        arquivoHash: body.arquivoHash,
                        destinoReferencia: body.destinoReferencia,
                        entregueEm: body.entregueEm,
                    },
                ];
            if (op === "reabrir") {
                if (this.closureVersion)
                    this.previousVersions.push(
                        structuredClone(this.closureVersion),
                    );
                this.closureVersion = {
                    ...this.closureVersion,
                    id: "1002",
                    numero: 2,
                    estadoExterno: "NAO_ENTREGUE",
                    situacao: "PREPARADA",
                    conteudoHash: "e".repeat(64),
                    nfse: [],
                    entregas: [],
                    confirmacoes: [],
                    decididaEm: null,
                    decisor: null,
                    motivoDecisao: null,
                    resolucaoFinanceira: null,
                };
                this.closure = {
                    ...this.closure,
                    situacao: "PREPARADO",
                    versaoAtual: 2,
                };
            }
            data.fechamento = this.closure;
            data.versao = this.closureVersion;
        }
        if (
            r.endpoint.id === "FechamentoCobrancaController.consultar" &&
            this.closure
        )
            data = this.closure;
        if (
            r.endpoint.id === "FechamentoCobrancaController.versao" &&
            this.closureVersion
        )
            data = structuredClone(this.version(String(r.params.numero)));
        if (
            r.endpoint.id === "FechamentoCobrancaController.demonstrativo" &&
            isObject(data)
        ) {
            const selected = this.closureVersion
                ? this.version(String(r.params.numero))
                : (fixture("FechamentoCobrancaDto.Versao") as Values);
            if (String(selected.numero) !== String(r.params.numero))
                throw new ApiError(
                    "Exercício: versão não preparada",
                    404,
                    "VERSAO_NAO_ENCONTRADA",
                );
            data = {
                ...data,
                fechamentoId: selected.fechamentoId,
                numero: selected.numero,
                saldo: selected.saldo,
                natureza: selected.natureza,
                ajustes: selected.ajustes,
            };
            if (isObject(data) && isObject(data.calculo))
                data.calculo = { ...data.calculo, id: selected.calculoId };
        }
        if (
            r.endpoint.id === "FechamentoCobrancaController.versoes" &&
            this.closureVersion
        )
            data = [...this.previousVersions, this.closureVersion];
        if (
            r.endpoint.id === "AjusteFechamentoController.ajustar" &&
            isObject(data)
        ) {
            const adjustment: Values = {
                ...data,
                id: "2901",
                origemVersaoId: body.origemVersaoId,
                destinoFechamentoId: body.destinoFechamentoId,
                calculoCorrigidoId: body.calculoCorrigidoId,
                motivo: body.motivo,
                evidencia: body.evidencia,
            };
            this.adjustments.push(adjustment);
            data = adjustment;
        }
        if (r.endpoint.id === "AjusteFechamentoController.listar")
            data = structuredClone(this.adjustments);
        if (r.endpoint.id === "FechamentoCobrancaController.tratativas")
            data = structuredClone(this.treatments);
        return data;
    }
    private version(number: string): Values {
        const version = [this.closureVersion, ...this.previousVersions].find(
            (candidate) => candidate && String(candidate.numero) === number,
        );
        if (!version)
            throw new ApiError(
                "Exercício: versão não preparada",
                404,
                "VERSAO_NAO_ENCONTRADA",
            );
        return version;
    }
}
