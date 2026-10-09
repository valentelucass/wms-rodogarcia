// Executed once on this delivery's draft; every destination stays in frontend/.
import ts from "typescript";
import {readFileSync,writeFileSync,mkdirSync} from "node:fs";
import {resolve,dirname} from "node:path";
const root=resolve(".");
const read=p=>readFileSync(p,"utf8");
const write=(p,s)=>{const target=resolve(p);if(!target.startsWith(root+"\\")&&!target.startsWith(root+"/"))throw Error("Fora do frontend");mkdirSync(dirname(target),{recursive:true});writeFileSync(target,s);console.log(p);};
const parse=p=>ts.createSourceFile(p,read(p),ts.ScriptTarget.Latest,true,p.endsWith("tsx")?ts.ScriptKind.TSX:ts.ScriptKind.TS);
const findVar=(sf,name)=>sf.statements.flatMap(x=>ts.isVariableStatement(x)?[...x.declarationList.declarations]:[]).find(x=>x.name.getText(sf)===name);
let ws=read("src/domain/workflow.ts");ws=ws.slice(0,ws.indexOf("export interface NextAction"));
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

