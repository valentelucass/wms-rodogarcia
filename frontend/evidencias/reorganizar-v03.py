"""Registro executável da divisão do draft próprio. Não é rotina da aplicação.
Recusa reaplicação: os marcadores exigem a fonte anterior desta entrega.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def read(p):return (root/p).read_text(encoding='utf-8-sig')
def write(p,s):
 target=(root/p).resolve();assert target.is_relative_to(root)
 target.parent.mkdir(parents=True,exist_ok=True);target.write_text(s,encoding='utf-8');print(p)

# Scalar controls remain reusable. Typed contingency stays with its module.
s=read('src/components/Fields.tsx')
a=s.index('    const choices =');b=s.index('function prefixLabel');c=s.index('function ContingencyData')
scalar=s[a:b]+s[b:c]
write('src/components/fields/ScalarField.tsx','''import {useId,useContext} from "react";
import {enums,type Field} from "../../contracts/runtime";
import {label} from "../../domain/labels";
import {FormReferences} from "../FormReferences";
export function ScalarField({f,value,path,onChange,disabled}:{f:Field;value:unknown;path:string;onChange:(v:unknown)=>void;disabled:boolean}){
 const id=useId(),refs=useContext(FormReferences);const title=label(f.name)+(f.required?" *":"");
'''+scalar)
cont=s[c:].replace('function ContingencyData','export function ContingencyData')
write('src/modules/regularizacao/ContingencyFields.tsx','''import {records,isObject,type Values,type Perfil} from "../../contracts/runtime";
import {contingencyTypes} from "../../contracts/contingency";
import type {Fields as FieldsView} from "../../components/Fields";
'''+cont.replace('    kind,','    kind,\n    Fields,').replace('    kind: string;','    kind: string;\n    Fields: typeof FieldsView;'))
s=s[:a]+'    return <ScalarField f={f} value={value} path={path} onChange={onChange} disabled={disabled}/>;\n}\n'
s=s.replace('useId, ','').replace('    enums,\n','').replace('import { contingencyTypes } from "../contracts/contingency";','import {ContingencyData} from "../modules/regularizacao/ContingencyFields";\nimport {ScalarField} from "./fields/ScalarField";\nimport {isFieldDisabled} from "../domain/fieldPresentation";')
s=s.replace('    const id = useId();\n','').replace('<ContingencyData\n','<ContingencyData\n                Fields={Fields}\n')
old='''disabled={
                        disabled ||
                        (perfil !== "GESTOR" &&
                            ["resolverPendentes", "resolucao"].includes(f.name)) ||
                        (perfil === "OPERACAO" && f.name === "valor")
                    }'''
i=s.index('                    disabled={');j=s.index('                    perfil={perfil}',i)
s=s[:i]+'                    disabled={disabled || isFieldDisabled(schema,f.name,perfil)}\n'+s[j:]
s=s.replace('    root?: Values;','    root?: Values;\n    schema?: string;').replace('    root = values,','    root = values,\n    schema = "",')
s=s.replace('    root,\n}: {','    root,\n}: {')
s=s.replace('                            root={root}\n                        />','                            root={root}\n                            schema={f.type}\n                        />')
write('src/components/Fields.tsx',s)

# Label is an actual domain result, not a branch of the recursive record renderer.
s=read('src/components/Result.tsx');a=s.index('export function LabelPreview')
write('src/components/LabelPreview.tsx','''import {useEffect,useState} from "react";
import QRCode from "qrcode";
import {isObject} from "../contracts/runtime";
import {display} from "../domain/labels";
'''+s[a:])
write('src/components/Result.tsx',s[:a].replace('import { useEffect, useState } from "react";\n','').replace('import QRCode from "qrcode";\n',''))

# Collector reading lifecycle owns the same asynchronous scope as other requests.
s=read('src/components/Collector.tsx');a=s.index('    const [code');b=s.index('    const selected =');jsx=s[b:]
state=s[a:b]
start=state.index('    const alive =');end=state.index('    const invalidate =')
state=state[:start]+'''    const scope=useRequestScope();
    const posRef=useRef<HTMLInputElement>(null),captured=useRef("");
    const contextKey=JSON.stringify([context.clienteId,context.armazemId,perfil]);
    const {cancel}=scope;
    useEffect(()=>{cancel();setBusy(false);setUnit(undefined);setTarget(undefined);setAddresses([]);setError("");},[contextKey,cancel]);
'''+state[end:]
state=state.replace('        generation.current++;\n        controller.current?.abort();','        scope.cancel();')
state=state.replace('        controller.current?.abort();\n        const g = ++generation.current;','        const ticket=scope.begin();')
state=state.replace('        controller.current = new AbortController();\n','')
state=state.replace('controller.current.signal','ticket.signal').replace('!alive.current || g !== generation.current','!ticket.isCurrent()').replace('alive.current && g === generation.current','ticket.isCurrent()')
state=state.replace('setTimeout(() => posRef.current?.focus(), 0);','setTimeout(() => {if(ticket.isCurrent())posRef.current?.focus();}, 0);')
write('src/hooks/useCollectorReading.ts','''import {useEffect,useRef,useState} from "react";
import {endpoint,isObject,type Values,type Perfil} from "../contracts/runtime";
import type {Transport} from "../api/client";
import {useRequestScope} from "./useRequestScope";
export function useCollectorReading(transport:Transport,context:Values,perfil:Perfil){
'''+state+''' return {code,setCode,position,setPosition,unit,addresses,target,setTarget,error,busy,page,setPage,mode,setMode,posRef,captured,invalidate,load,match};
}
''')
s=s[:a]+'''    const {code,setCode,position,setPosition,unit,addresses,target,setTarget,error,busy,page,setPage,mode,setMode,posRef,captured,invalidate,load,match}=useCollectorReading(transport,context,perfil);
'''+jsx
s=s.replace('import { useEffect, useRef, useState } from "react";','import {useCollectorReading} from "../hooks/useCollectorReading";').replace('    endpoint,\n','')
write('src/components/Collector.tsx',s)

# Lookup UI and hook have a separate purpose, with the shared cancellation policy.
s=read('src/components/JourneyPage.tsx');a=s.index('function LookupPanel')
look=s[a:];c=look.index('    const [result');d=look.index('    if (!lookups[journey])')
load=look[c:d];start=load.index('    const controller');end=load.index('    const lookups:')
load=load[:start]+'''    const scope=useRequestScope();
    const {cancel}=scope;
    const contextKey=JSON.stringify([context.clienteId,context.armazemId,context.codigoLido,context.produtoId]);
    useEffect(()=>{cancel();setPending(false);setResult(undefined);setError("");},[contextKey,cancel]);
'''+load[end:]
ld=load.index('    const lookups:');le=load.index('    const load =')
lookupdefs=load[ld:le];load=load[:ld]+load[le:]
load=load.replace('        controller.current = new AbortController();','        const ticket=scope.begin();').replace('controller.current.signal','ticket.signal').replace('!alive.current','!ticket.isCurrent()').replace('alive.current','ticket.isCurrent()')
write('src/hooks/useReferenceLookup.ts','''import {useEffect,useState} from "react";
import {endpoint,type Values} from "../contracts/runtime";
import type {Transport,Receipt} from "../api/client";
import {useRequestScope} from "./useRequestScope";
export function useReferenceLookup({context,transport,onReceipt}:{context:Values;transport:Transport;onReceipt:(r:Receipt,type:string,id:string)=>void}){
'''+load+' return {result,error,pending,load};\n}\n')
look=look[:c]+'    const {result,error,pending,load}=useReferenceLookup({context,transport,onReceipt});\n'+lookupdefs+look[d:]
write('src/components/ReferenceLookup.tsx','''import {type Values} from "../contracts/runtime";
import type {Receipt,Transport} from "../api/client";
import {useReferenceLookup} from "../hooks/useReferenceLookup";
import {Result} from "./Result";
'''+look.replace('function LookupPanel','export function ReferenceLookup'))
s=s[:a].replace('useState, useEffect, useRef','useState').replace('<LookupPanel','<ReferenceLookup').replace('import { Result } from "./Result";','import {ReferenceLookup} from "./ReferenceLookup";')
write('src/components/JourneyPage.tsx',s)
print('Separação aplicada somente ao draft frontend próprio.')
