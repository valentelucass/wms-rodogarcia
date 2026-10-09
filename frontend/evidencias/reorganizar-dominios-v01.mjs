// Executed once on this delivery's draft; every destination stays in frontend/.
import ts from "typescript";
import {readFileSync,writeFileSync,mkdirSync} from "node:fs";
import {resolve,dirname} from "node:path";
const root=resolve(".");
const read=p=>readFileSync(p,"utf8");
const write=(p,s)=>{const target=resolve(p);if(!target.startsWith(root+"\\")&&!target.startsWith(root+"/"))throw Error("Fora do frontend");mkdirSync(dirname(target),{recursive:true});writeFileSync(target,s);console.log(p);};
const parse=p=>ts.createSourceFile(p,read(p),ts.ScriptTarget.Latest,true,p.endsWith("tsx")?ts.ScriptKind.TSX:ts.ScriptKind.TS);
const findVar=(sf,name)=>sf.statements.flatMap(x=>ts.isVariableStatement(x)?[...x.declarationList.declarations]:[]).find(x=>x.name.getText(sf)===name);
const groups={cadastros:["cadastros"],recebimento:["entrada","unidades"],estoque:["estoque"],saida:["saida","fiscal"],financeiro:["precos","cobranca","fechamento"],regularizacao:["contagem","contingencia"],relatorios:["relatorios"]};
const groupFor=id=>Object.keys(groups).find(g=>groups[g].includes(id));
const refNames={"PedidoEntradaDto.Resumo":"Pedido de entrada","RecebimentoDto.Entrada":"Entrada conferida","UnidadeLogisticaDto.Resumo":"Unidade logística","PedidoSaidaDto.Detalhe":"Pedido de saída","PedidoSaidaDto.Reserva":"Reserva","CalculoCobrancaDto.Resultado":"Cálculo","FechamentoCobrancaDto.Fechamento":"Fechamento","FechamentoCobrancaDto.Versao":"Versão financeira"};
const references={recebimento:Object.keys(refNames).slice(0,3),saida:Object.keys(refNames).slice(2,5),financeiro:Object.keys(refNames).slice(5),estoque:["UnidadeLogisticaDto.Resumo"],regularizacao:["UnidadeLogisticaDto.Resumo"],cadastros:[],relatorios:Object.keys(refNames)};
const lookups={entrada:[["Consultar produtos para a nota","ProdutoController.listar"]],unidades:[["Consultar embalagens deste SKU","EmbalagemController.listar"]],saida:[["Consultar posições para separação","EnderecoController.listar"],["Consultar etiqueta da unidade","UnidadeLogisticaController.etiqueta"]]};
const fields={cadastros:{},recebimento:{produtoId:"ProdutoDto.Resposta",embalagemId:"EmbalagemDto.Resposta",itemNotaId:"PedidoEntradaDto.ItemConferencia",entradaId:"RecebimentoDto.Entrada"},saida:{produtoId:"ProdutoDto.Resposta",enderecoId:"EnderecoDto.Resposta",reservaId:"PedidoSaidaDto.Reserva",unidadeId:"UnidadeLogisticaDto.Resumo"},financeiro:{calculoId:"CalculoCobrancaDto.Resultado"},estoque:{enderecoId:"EnderecoDto.Resposta",unidadeId:"UnidadeLogisticaDto.Resumo"},regularizacao:{unidadeId:"UnidadeLogisticaDto.Resumo"},relatorios:{}};
const j=parse("src/domain/journeys.ts"),w=parse("src/domain/workflow.ts");
const arr=findVar(j,"journeys").initializer.elements;
const next=findVar(w,"nextActions").initializer.properties;
const helper=j.statements.filter(x=>(ts.isVariableStatement(x)&&["s","cadastro"].includes(x.declarationList.declarations[0].name.getText(j)))).map(x=>x.getText(j)).join("\n").replace("const s =","export const s =").replace("const cadastro =","export const cadastro =");
write("src/domain/journeyTypes.ts",j.statements.filter(ts.isInterfaceDeclaration).map(x=>x.getText(j)).join("\n").replace("    steps: Step[];","    steps: Step[];\n    lookups?: [string,string][];\n    references: [string,string][];\n    referenceFields: Record<string,string>;")+"\nexport interface NextAction {page:string;action:string;title:string}\n"+helper);
const verb=findVar(j,"verbs"),label=j.statements.find(x=>ts.isVariableStatement(x)&&x.declarationList.declarations[0].name.getText(j)==="actionLabel");
write("src/domain/actionLabels.ts",`const verbs=${verb.initializer.getText(j)};\n${label.getText(j)}\n`);
for(const [group,ids] of Object.entries(groups)){
 const entries=arr.filter(e=>ids.includes(e.properties.find(p=>p.name.getText(j)==="id").initializer.text)).map(e=>{
  const id=e.properties.find(p=>p.name.getText(j)==="id").initializer.text;
  return `{...${e.getText(j)},lookups:${JSON.stringify(lookups[id]??[])},references:${JSON.stringify(references[group].map(t=>[refNames[t],t]))},referenceFields:${JSON.stringify(fields[group])}}`;
 });
 const props=next.filter(p=>{const name=p.name.text;return group==="recebimento"?/^(PedidoEntrada|UnidadeLogistica)Controller\./.test(name):group==="saida"?/^(PedidoSaida|Expedicao)Controller\./.test(name):group==="financeiro"?/^(CalculoCobranca|FechamentoCobranca)Controller\./.test(name):false;});
 const using=group==="cadastros"?"cadastro,s":group==="recebimento"||group==="saida"||group==="financeiro"||group==="estoque"||group==="regularizacao"||group==="relatorios"?"s":"s";
 write(`src/modules/${group}/definition.ts`,`import {${using},type Journey,type NextAction} from "../../domain/journeyTypes";\nexport const journeys:Journey[]=[${entries.join(",\n")}];\nexport const nextActions:Record<string,NextAction>={${props.map(p=>p.getText(w)).join(",\n")}};\n`);
}
write("src/domain/journeys.ts",Object.keys(groups).map((g,i)=>`import {journeys as j${i},nextActions as n${i}} from "../modules/${g}/definition";`).join("\n")+`\nexport const journeys=[${Object.keys(groups).map((_,i)=>`...j${i}`).join(",")}];\nexport const nextActions={${Object.keys(groups).map((_,i)=>`...n${i}`).join(",")}};\nexport type {Journey,Step,NextAction} from "./journeyTypes";\nexport {actionLabel} from "./actionLabels";\n`);
let ws=read("src/domain/workflow.ts");ws=ws.slice(0,ws.index("export interface NextAction"));
write("src/domain/workflow.ts",ws);
for(const p of ["src/components/JourneyPage.tsx","src/components/operation/OperationResults.tsx"]){let s=read(p);if(p.includes("OperationResults"))s=s.replace('"../../domain/workflow"','"../../domain/journeys"');else{s=s.replace("    nextActions,\n","").replace("    type NextAction,\n","");s=s.replace('import { type Journey, actionLabel } from "../domain/journeys";','import { type Journey, type NextAction, actionLabel, nextActions } from "../domain/journeys";');}write(p,s);}

// Presentation is explicit metadata for actual DTOs, rather than field-name policy in JSX.
const schemas=JSON.parse(read("src/contracts/schemas.json")).records;
const policies={};for(const [type,fs] of Object.entries(schemas)){
 const restricted=fs.filter(f=>["resolucao","resolverPendentes"].includes(f.name)).map(f=>[f.name,"GESTOR"]);
 if(restricted.length)policies[type]=Object.fromEntries(restricted);
}
policies["IndicadorEstoqueController.listar.query"]={valor:"SUPERVISOR"};policies["IndicadorEstoqueController.consultar.query"]={valor:"SUPERVISOR"};
write("src/domain/fieldPresentation.ts",`import {canPresent,type Perfil} from "../contracts/runtime";\n// Conditional historical resolution gates from current services; presentation only.\nconst policies:Record<string,Record<string,Perfil>>=${JSON.stringify(policies,null,2)};\nexport function isFieldDisabled(schema:string,field:string,perfil:Perfil){const minimum=policies[schema]?.[field];return minimum?!canPresent(perfil,minimum):false;}\n`);

// Fictitious transport owns latency/error/replay only; example state is separate.
let f=read("src/api/fictitious.ts");const fixturesStart=f.index("const baseValues"),fixturesEnd=f.index("const transitions");
let fixtureCode=f.slice(fixturesStart,fixturesEnd);
write("src/api/mock/constants.ts",'export const demoCode="00000000-0000-4000-8000-000000000001";\n');
write("src/api/mock/fixtures.ts",'import {records,enums,listType,type Values} from "../../contracts/runtime";\nimport {demoCode} from "./constants";\n'+fixtureCode);
const reflectStart=f.index("const transitions"),reflectEnd=f.index("export class FictitiousTransport");
write("src/api/mock/scriptedValues.ts",'import {isObject,type Values} from "../../contracts/runtime";\n'+f.slice(reflectStart,reflectEnd).replace("const transitions:","export const transitions:").replace("function reflect(","export function reflect("));
const warehouseStart=f.index('        if (r.endpoint.id.startsWith("PedidoEntradaController."))');
const financeStart=f.index('        if (\n            r.endpoint.id.startsWith("FechamentoCobrancaController.")');
const financeEnd=f.index('        if (this.scenario === "vazio"');
write("src/modules/recebimento/ExampleWarehouse.ts",'import {isObject,type Values} from "../../contracts/runtime";\nimport type {Request} from "../../api/client";\nimport {fixture} from "../../api/mock/fixtures";\nimport {transitions} from "../../api/mock/scriptedValues";\n// In-memory exercise receipts, not WMS availability/capacity rules.\nexport class ExampleWarehouse {\n private entry:Values|undefined;private outgoing:Values|undefined;private unit:Values|undefined;\n respond(r:Request,body:Values,data:unknown):unknown {\n'+f.slice(warehouseStart,financeStart)+'return data;\n}\n}\n');
write("src/modules/financeiro/ExampleBilling.ts",'import {isObject,type Values} from "../../contracts/runtime";\nimport type {Request} from "../../api/client";\nimport {fixture} from "../../api/mock/fixtures";\n// Pre-authored exercise states and values, never commercial calculation.\nexport class ExampleBilling {\n private closure:Values|undefined;private closureVersion:Values|undefined;private externalDocuments:Values[]=[];private treatments:Values[]=[];\n respond(r:Request,body:Values,data:unknown):unknown {\n'+f.slice(financeStart,financeEnd)+'return data;\n}\n}\n');
f=f.slice(0,fixturesStart)+f.slice(reflectEnd);
f=f.replace(/export const demoCode =[^;]+;\n/,"");
f=f.replace(/    private (entry|outgoing|closure|closureVersion|unit|externalDocuments|treatments):[^;]+;\n/g,"");
const a=f.index('        if (r.endpoint.id.startsWith("PedidoEntradaController."))'),b=f.index('        if (this.scenario === "vazio"');
f=f.slice(0,a)+'        data=this.warehouse.respond(r,body,data);data=this.billing.respond(r,body,data);\n'+f.slice(b);
f=f.replace("    enums,\n","").replace("    records,\n","").replace("    listType,\n","");
f=f.replace("    constructor(public perfil:","    private warehouse=new ExampleWarehouse();private billing=new ExampleBilling();\n    constructor(public perfil:");
f='import {fixture} from "./mock/fixtures";\nimport {reflect} from "./mock/scriptedValues";\nimport {ExampleWarehouse} from "../modules/recebimento/ExampleWarehouse";\nimport {ExampleBilling} from "../modules/financeiro/ExampleBilling";\n'+f;
write("src/api/fictitious.ts",f);
for(const p of ["tests/flows.test.tsx","tests/contracts.test.ts","tests/collector.test.tsx"]){let s=read(p);s=s.replace('import { FictitiousTransport, demoCode } from "../src/api/fictitious";','import { FictitiousTransport } from "../src/api/fictitious";\nimport {demoCode} from "../src/api/mock/constants";').replace('import { fixture } from "../src/api/fictitious";','import { fixture } from "../src/api/mock/fixtures";').replace('import { fixture, demoCode } from "../src/api/fictitious";','import {fixture} from "../src/api/mock/fixtures";\nimport {demoCode} from "../src/api/mock/constants";');write(p,s);}
