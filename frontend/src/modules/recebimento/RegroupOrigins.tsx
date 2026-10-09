import { useContext, type ReactNode } from "react";
import { FormReferences } from "../../components/FormReferences";
import { isObject, type Values } from "../../contracts/runtime";
export function RegroupOrigins({value,onChange,disabled,renderRow}:{value:unknown;onChange:(v:unknown)=>void;disabled:boolean;renderRow:(v:Values,i:number,change:(v:Values)=>void)=>ReactNode}) {
    const refs=useContext(FormReferences), rows=Array.isArray(value)?value.filter(isObject):[];
    const units=refs.records?.["UnidadeLogisticaDto.Resumo"] ?? [];
    return <fieldset disabled={disabled}><legend>Origens do reagrupamento com revisão consultada</legend><p>Selecione as unidades de origem. A unidade de destino e a soma serão conferidas pelo servidor.</p>
        {units.filter(u=>u.id!==refs.defaults.id).map(u=><button type="button" key={String(u.id)} disabled={rows.some(r=>r.unidadeId===u.id)} onClick={()=>onChange([...rows,{unidadeId:u.id,versao:u.versao}])}>Adicionar origem unidade {String(u.id)} · revisão {String(u.versao)}</button>)}
        {rows.map((row,i)=><div key={i}>{renderRow(row,i,v=>onChange(rows.map((old,j)=>j===i?v:old)))}<button type="button" onClick={()=>onChange(rows.filter((_,j)=>j!==i))}>Remover origem {i+1}</button></div>)}</fieldset>;
}
