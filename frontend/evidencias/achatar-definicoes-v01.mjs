import { readFileSync, writeFileSync } from 'node:fs';
const groups=['cadastros','recebimento','estoque','saida','financeiro','regularizacao','relatorios'];
for (const group of groups) {
 const file=`src/modules/${group}/definition.ts`;
 const old=readFileSync(file,'utf8');
 const flat=old.replace(/        \.\.\.\{\r?\n([\s\S]*?)        \},\r?\n/g, (_all, inner)=>inner.replace(/^    /gm,''));
 writeFileSync(file,flat);
 console.log(group + ': objetos de jornada sem spread intermediário.');
}
