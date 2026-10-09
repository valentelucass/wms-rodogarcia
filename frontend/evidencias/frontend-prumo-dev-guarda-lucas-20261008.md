# Prumo — D31-DEV01: guarda mínima de desenvolvimento

**Bloqueado no pré-requisito de resolução segura do incidente D29. Guarda real não aprovada. Backend com SQL e conexão FE→API com SQL não liberados.**

A nova autorização expressa permite desenvolvimento integrado exclusivamente WMS_DEV/WMSDEV restrita e verificação mínima quando seguro; ela não comprova a recuperação. A leitura atual de states.md, continuidade, decisões, recibo de retomada e FINAL14 conserva o incidente sem resolução segura e sem nova guarda. Nenhuma referência atual de recuperação foi fornecida/localizada nas fontes direcionadas até 2026-10-08T21:29:07.6763700Z. Esta conclusão tem o alcance das fontes consultadas, não alega ausência universal de evidências.

A última tentativa do incidente consultada ocorreu em 07/10/2026 20:39:17Z: uma abertura falhou no prelogin em 5,12s (SQL_-2/native258), antes de confirmar banco/identidade/TLS. Zero SELECT. Preflight19:14Z é anterior à falha e permanece histórico. Serviço/listener ativo e baseline de certificado não comprovam recuperação nem handshake atual. Causa raiz permanece inconclusiva.

## Canal e configuração — sem segredo

- Fonte própria autorizada: database/scripts/d26-credencial-aplicacao.ps1, função Get-WmsDevCredentialMetadata. Arquivo %LOCALAPPDATA%/Rodogarcia/WMS/api-dev/wmsdev-app.clixml: existe=true, ACL/owner compatíveis=true. A biblioteca e seu auxiliar foram lidos antes do dot-source; apenas a função de metadados foi chamada.
- Nenhum conteúdo CLIXML, Import-Clixml, descriptografia DPAPI, senha, token, factory SQL ou perfil compartilhado executado/lido. Existência/ACL não certificam credencial, identidade ou permissão real.
- application.properties: perfil default local; ddl-auto=validate, generate-ddl=false, sql.init.mode=never, flyway.enabled=false. sqlserver-dev depende de entradas externas WMS de alvo/identidade/TLS/OIDC; valores externos não lidos. Isso confirma somente a configuração estática, não schema/checksum/histórico real nem ausência de migrations pendentes.

| Critério | Resultado |
| --- | --- |
| D31-DEV01-G01 — Resolucao segura atual D29 | BLOQUEADO_PROVA_NAO_LOCALIZADA_NAS_FONTES_CONSULTADAS |
| D31-DEV01-G02 — Canal proprio WMSDEV existencia e ACL | CONFIRMADO_SOMENTE_METADADO_LOCAL_SEM_SEGREDO |
| D31-DEV01-G03 — Nome real WMS_DEV e identidade real WMSDEV restrita | NAO_EXECUTADO_DEPENDE_G01 |
| D31-DEV01-G04 — TLS atual valido sem bypass | NAO_EXECUTADO_DEPENDE_G01 |
| D31-DEV01-G05 — Permissoes atuais restritas por objeto/coluna sem DDL/admin | NAO_EXECUTADO_DEPENDE_G01 |
| D31-DEV01-G06 — Schema/history/migrations sem pendentes e sem mutacao automatica | ESTATICO_SEM_MUTACAO_CONFIRMADO_REAL_NAO_EXECUTADO_DEPENDE_G01 |

## Evidências

| Fonte | SHA-256 consultado |
| --- | --- |
| orchestracao/.runtime/d29-incidente-prumo-diagnostico-20261007T203909936.json | 362950B5AEFF65716AB015B6E41D1E71BC73DE8D77A1DEC115F78D559FC6D74D |
| orchestracao/.runtime/d29-incidente-diagnostico-farol.md | 9F475CCC8C22A6E932EB511702375A39B00C2627EE740252F4FABACDF87FFDFE |
| orchestracao/.runtime/d29-incidente-suspensao-farol.md | 474E0712A0748605C72A0063633DCE8E8CB36AF123EE051B2EEC009C76B5345B |
| orchestracao/.runtime/d30-retomada-apos-reinicio-chats.json | 2E2919D92C5859D38F0F5232F003D96A2B41873C532AD88E4D905B12D44F455D |
| orchestracao/.runtime/d30-resultado-parcial-final14.json | A3DF3383FDBAAB68C6C4008BF1DC4C1FC632D00F529A294190CDA727CD2CBC5C |

Todos os hashes atuais de canônicos, configs e bibliotecas estão no [JSON](frontend-prumo-dev-guarda-lucas-20261008.json). Não houve alteração desses arquivos. A skill ai-memory-retrieval foi usada só neste projeto (workspace rodogarcia/project wms-rodogarcia), com resultados históricos sem prova atual de recuperação; nenhuma memória global consultada. A decisão se apoia nas fontes do repositório, não em autorização inferida da memória.

Comando próprio: powershell -NoProfile -File frontend/evidencias/prumo-dev-guarda-leitura.ps1. Exit0 da leitura e geração de recibo; não é sucesso de guarda SQL. Log: [prumo-dev-guarda-leitura.log](prumo-dev-guarda-leitura.log). Aberturas SQL=0, SELECT=0, HTTP=0, fixtures=0, backend iniciado/testado/construído=0. Sem admin/sa/PROD/DDL/migration/grants/alteração de acesso, servidor/runtime/restart/kill/publicação ou callback Hermes.

## Encaminhamento

Farol mantém Cedro sem iniciar backend com SQL e Lume sem conectar API com SQL. O responsável pelo SQL precisa fornecer evidência atual da resolução segura do incidente. Após conferir essa prova, avaliar a única guarda própria mínima autorizada (WMS_DEV real, WMSDEV restrita, TLS, permissões por objeto/coluna, catálogo e histórico), sem repetição/fallback. Recuperação que exige alterar servidor/acessos/sharedruntime precisa de decisão material; não executada neste recorte.

Desenvolvimento integrado real não foi iniciado e não há URL real verificada para esse uso. A documentação atual frontend/README.md fornece npm run dev em127.0.0.1:5178 para exercício fictício; isso não comprova um servidor ativo nem conexão real. Lume prepara forma documentada de integração no seu escopo após liberação baseada em provas.

CSS é uma entrega separada concluída: [recibo CSS](frontend-prumo-css-lucas-20261008.md), 30 capturas/computed DEV+BUILD próprios1440/768/390, fonte congelada e processos próprios5191 fechados. Foco visual aprovado na fatia; defeito do skip de navegação encaminhado a Lume via Farol. Fatia CSS para Vigia; integração/checks finais únicos com Lume.
