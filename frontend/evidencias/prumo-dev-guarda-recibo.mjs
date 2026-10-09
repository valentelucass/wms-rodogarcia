import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
const jsonPath=path.join(import.meta.dirname,'frontend-prumo-dev-guarda-lucas-20261008.json');
const report=JSON.parse(readFileSync(jsonPath,'utf8').replace(/^\uFEFF/,''));
const sha=data=>createHash('sha256').update(data).digest('hex');
const src=key=>report.fontes.find(s=>s.path===key);
const sources=['orchestracao/.runtime/d29-incidente-prumo-diagnostico-20261007T203909936.json','orchestracao/.runtime/d29-incidente-diagnostico-farol.md','orchestracao/.runtime/d29-incidente-suspensao-farol.md','orchestracao/.runtime/d30-retomada-apos-reinicio-chats.json','orchestracao/.runtime/d30-resultado-parcial-final14.json'];
const md=[
    '# Prumo — D31-DEV01: guarda mínima de desenvolvimento',
    '',
    '**Bloqueado no pré-requisito de resolução segura do incidente D29. Guarda real não aprovada. Backend com SQL e conexão FE→API com SQL não liberados.**',
    '',
    'A nova autorização expressa permite desenvolvimento integrado exclusivamente WMS_DEV/WMSDEV restrita e verificação mínima quando seguro; ela não comprova a recuperação. A leitura atual de states.md, continuidade, decisões, recibo de retomada e FINAL14 conserva o incidente sem resolução segura e sem nova guarda. Nenhuma referência atual de recuperação foi fornecida/localizada nas fontes direcionadas até '+report.observedAt+'. Esta conclusão tem o alcance das fontes consultadas, não alega ausência universal de evidências.',
    '',
    'A última tentativa do incidente consultada ocorreu em 07/10/2026 20:39:17Z: uma abertura falhou no prelogin em 5,12s (SQL_-2/native258), antes de confirmar banco/identidade/TLS. Zero SELECT. Preflight19:14Z é anterior à falha e permanece histórico. Serviço/listener ativo e baseline de certificado não comprovam recuperação nem handshake atual. Causa raiz permanece inconclusiva.',
    '',
    '## Canal e configuração — sem segredo',
    '',
    '- Fonte própria autorizada: database/scripts/d26-credencial-aplicacao.ps1, função Get-WmsDevCredentialMetadata. Arquivo %LOCALAPPDATA%/Rodogarcia/WMS/api-dev/wmsdev-app.clixml: existe='+report.canalProprioWMSDEV.metadata.existe+', ACL/owner compatíveis='+report.canalProprioWMSDEV.metadata.aclValida+'. A biblioteca e seu auxiliar foram lidos antes do dot-source; apenas a função de metadados foi chamada.',
    '- Nenhum conteúdo CLIXML, Import-Clixml, descriptografia DPAPI, senha, token, factory SQL ou perfil compartilhado executado/lido. Existência/ACL não certificam credencial, identidade ou permissão real.',
    '- application.properties: perfil default local; ddl-auto=validate, generate-ddl=false, sql.init.mode=never, flyway.enabled=false. sqlserver-dev depende de entradas externas WMS de alvo/identidade/TLS/OIDC; valores externos não lidos. Isso confirma somente a configuração estática, não schema/checksum/histórico real nem ausência de migrations pendentes.',
    '',
    '| Critério | Resultado |',
    '| --- | --- |',
    ...report.criterios.map(c=>'| '+c.id+' — '+c.criterio+' | '+c.estado+' |'),
    '',
    '## Evidências',
    '',
    '| Fonte | SHA-256 consultado |',
    '| --- | --- |',
    ...sources.map(s=>'| '+s+' | '+src(s).sha256+' |'),
    '',
    'Todos os hashes atuais de canônicos, configs e bibliotecas estão no [JSON](frontend-prumo-dev-guarda-lucas-20261008.json). Não houve alteração desses arquivos. A skill ai-memory-retrieval foi usada só neste projeto (workspace rodogarcia/project wms-rodogarcia), com resultados históricos sem prova atual de recuperação; nenhuma memória global consultada. A decisão se apoia nas fontes do repositório, não em autorização inferida da memória.',
    '',
    'Comando próprio: powershell -NoProfile -File frontend/evidencias/prumo-dev-guarda-leitura.ps1. Exit0 da leitura e geração de recibo; não é sucesso de guarda SQL. Log: [prumo-dev-guarda-leitura.log](prumo-dev-guarda-leitura.log). Aberturas SQL=0, SELECT=0, HTTP=0, fixtures=0, backend iniciado/testado/construído=0. Sem admin/sa/PROD/DDL/migration/grants/alteração de acesso, servidor/runtime/restart/kill/publicação ou callback Hermes.',
    '',
    '## Encaminhamento',
    '',
    'Farol mantém Cedro sem iniciar backend com SQL e Lume sem conectar API com SQL. O responsável pelo SQL precisa fornecer evidência atual da resolução segura do incidente. Após conferir essa prova, avaliar a única guarda própria mínima autorizada (WMS_DEV real, WMSDEV restrita, TLS, permissões por objeto/coluna, catálogo e histórico), sem repetição/fallback. Recuperação que exige alterar servidor/acessos/sharedruntime precisa de decisão material; não executada neste recorte.',
    '',
    'Desenvolvimento integrado real não foi iniciado e não há URL real verificada para esse uso. A documentação atual frontend/README.md fornece npm run dev em127.0.0.1:5178 para exercício fictício; isso não comprova um servidor ativo nem conexão real. Lume prepara forma documentada de integração no seu escopo após liberação baseada em provas.',
    '',
    'CSS é uma entrega separada concluída: [recibo CSS](frontend-prumo-css-lucas-20261008.md), 30 capturas/computed DEV+BUILD próprios1440/768/390, fonte congelada e processos próprios5191 fechados. Foco visual aprovado na fatia; defeito do skip de navegação encaminhado a Lume via Farol. Fatia CSS para Vigia; integração/checks finais únicos com Lume.',
];
const mdPath=path.join(import.meta.dirname,'frontend-prumo-dev-guarda-lucas-20261008.md');
writeFileSync(mdPath,md.join('\n')+'\n');
console.log(JSON.stringify({estado:report.estado,canalArquivoExiste:report.canalProprioWMSDEV.metadata.existe,acl:report.canalProprioWMSDEV.metadata.aclValida,jsonSha256:sha(readFileSync(jsonPath)),mdSha256:sha(readFileSync(mdPath))},null,2));
