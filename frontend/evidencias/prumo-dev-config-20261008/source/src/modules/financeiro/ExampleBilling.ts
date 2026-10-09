import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { fixture } from "../../api/mock/fixtures";
// Pre-authored exercise states and values, never commercial calculation.
export class ExampleBilling {
    private closure: Values | undefined;
    private closureVersion: Values | undefined;
    private externalDocuments: Values[] = [];
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
            const status =
                op === "aprovar"
                    ? "APROVADO"
                    : op === "registrarNfse" &&
                        this.externalDocuments.length > 0
                      ? "CONFLITO_EXTERNO"
                      : op === "tratar"
                        ? "APROVADO"
                        : (this.closure?.situacao ?? "PREPARADO");
            this.closure = {
                ...data.fechamento,
                situacao: status,
                versao: String(
                    BigInt(String(this.closure?.versao ?? "-1")) + 1n,
                ),
            };
            this.closureVersion = {
                ...data.versao,
                situacao:
                    op === "aprovar"
                        ? "APROVADA"
                        : (this.closureVersion?.situacao ?? "PREPARADA"),
                estadoExterno:
                    op === "entregar"
                        ? "ENTREGUE"
                        : op === "registrarNfse"
                          ? this.externalDocuments.length > 0
                              ? "CONFLITO_EXTERNO"
                              : "EMITIDO"
                          : op === "tratar"
                            ? "EMITIDO"
                            : (this.closureVersion?.estadoExterno ??
                              "NAO_ENTREGUE"),
            };
            if (op === "registrarNfse")
                this.externalDocuments.push({
                    ...(fixture("FechamentoCobrancaDto.Documento") as Values),
                    id: String(1101 + this.externalDocuments.length),
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
                    versaoResultadoId: "1001",
                    resultado: body.resultado,
                    referencias: body.referencias,
                    fonte: body.fonte,
                    conferidaPor: body.conferidaPor,
                    conferidaEm: body.conferidaEm,
                    motivo: body.motivo,
                });
            this.closureVersion.nfse = structuredClone(this.externalDocuments);
            if(op === "reabrir") {
                if(this.closureVersion) this.previousVersions.push(structuredClone(this.closureVersion));
                this.closureVersion={...this.closureVersion,id:"1002",numero:2,estadoExterno:"NAO_ENTREGUE",situacao:"PREPARADA",conteudoHash:"e".repeat(64),nfse:[],entregas:[],confirmacoes:[]};
                this.closure={...this.closure,situacao:"PREPARADO",versaoAtual:2};
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
            data = r.params.numero === "1" && this.previousVersions.length ? this.previousVersions[0] : this.closureVersion;
        if (
            r.endpoint.id === "FechamentoCobrancaController.versoes" &&
            this.closureVersion
        )
            data = [...this.previousVersions,this.closureVersion];
        if(r.endpoint.id === "AjusteFechamentoController.ajustar" && isObject(data)) {
            const adjustment:Values={...data,id:"2901",origemVersaoId:body.origemVersaoId,destinoFechamentoId:body.destinoFechamentoId,calculoCorrigidoId:body.calculoCorrigidoId,motivo:body.motivo,evidencia:body.evidencia};this.adjustments.push(adjustment);data=adjustment;
        }
        if(r.endpoint.id === "AjusteFechamentoController.listar") data=structuredClone(this.adjustments);
        if (r.endpoint.id === "FechamentoCobrancaController.tratativas")
            data = structuredClone(this.treatments);
        return data;
    }
}
