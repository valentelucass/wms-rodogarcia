import type { Workflow } from "../../domain/workflow";
import type { Values } from "../../contracts/runtime";
export function AdjustmentOrigin({workflow,onPin,disabled}:{workflow:Workflow;onPin:(v:Values,type:string)=>void;disabled:boolean}) {
    const version=workflow.selected["FechamentoCobrancaDto.Versao"],origin=workflow.adjustmentOrigin;
    if(!version) return null;
    return <section aria-label="Origem histórica do ajuste"><button type="button" disabled={disabled} onClick={()=>onPin(version,"AjusteFechamento.origem")}>Guardar versão consultada como origem do ajuste</button>{origin && <p>Origem conservada: fechamento {String(origin.fechamentoId)} · versão {String(origin.numero)} · identificador {String(origin.id)}</p>}</section>;
}
