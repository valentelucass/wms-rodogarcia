# Prumo — delta de configuração DEV01, 08/10/2026

**Preparo local conferido; uso real continua bloqueado em G01.** Dois arquivos novos entregues: vite.dev.config.ts e docs/desenvolvimento-dev.md. Proxy preparado `/api` sem rewrite, mas ausente no servidor fictício. Modo real falha antes de criar servidor; flag booleana não libera integração.

O contrato enviado cedo ao Farol foi acolhido para Lume integrar `dev = vite --config vite.dev.config.ts`. Padrão frontend: 127.0.0.1:5178/strictPort/fictício. Preserve-se o processo e a configuração original; nenhum restart executado. Backend HTTP127.0.0.1:8080/WMS_PORT é contrato preparado do Cedro, sem processo observado. O destino opcional WMS_DEV_BACKEND_URL aceita só HTTP127.0.0.1:porta válida, sem caminho, credenciais, query ou fragmento. Same-origin conserva os caminhos existentes /api/v1; sem CORS wildcard ou mudança de CSP.

| Fonte nova | SHA-256 |
| --- | --- |
| vite.dev.config.ts | 2c8a77b396f3897a3a6d35098368f18e5db68d1841b34cfbf593348c7f8caf70 |
| docs/desenvolvimento-dev.md | 165fa7096472ba13fdd6fc3df9c4774ba70361c041d2412681f7dd256a3d1126 |

## Provas próprias

10 casos de configuração passaram com o loader Vite instalado: padrão fictício, WMS_PORT preparado, destino loopback explícito, real/G01 bloqueado, real com flag booleana sem liberação, host externo recusado, prefixo API no destino recusado, porta fora do intervalo recusada, WMS_PORT inválida e modo desconhecido. Nos casos fictícios, proxy ausente; contrato preparado conserva /api sem rewrite/changeOrigin. Middleware503 foi invocado apenas como função local, sem request HTTP/API/backend.

Bootstrap fictício passou no Chromium local em **http://127.0.0.1:5191/#coletor**, 390px, usando cópia consistente closure r08 + oito arquivos CSS/entrypoint congelados + os dois arquivos novos e o recibo bloqueado original. Consulta UUID respondeu em memória; zero API/externos/console/CSP/overflow, Arial/margin0 e seis folhas CSS carregadas. Processo próprio fechado após a prova; esta URL não está sendo entregue como servidor ativo. [Captura](prumo-dev-config-bootstrap-ficticio-390.png).

Composição/hashes efetivos estão no JSON e em `prumo-dev-config-20261008/snapshot.json`; manifesto r08 conferido SHA47214BF3. Fonte final da configuração permaneceu idêntica à executada. Nenhuma guarda fictícia foi criada: a cópia da guarda recebida conserva SHA9135BC92 e G01 bloqueado.

Comandos executados:

- Formatter instalado, somente vite.dev.config.ts e docs/desenvolvimento-dev.md (logs prumo-dev-config-formato.log/r02).
- PLAYWRIGHT_BROWSERS_PATH=frontend/.tools/ms-playwright node evidencias/prumo-dev-config-prova.mjs; exit0, 11 verificações (10 de config + bootstrap). Log prumo-dev-config-prova.log.
- node node_modules/typescript/bin/tsc --ignoreConfig --noEmit --target ES2022 --module ESNext --moduleResolution Bundler --types node --skipLibCheck vite.dev.config.ts; exit0. A primeira chamada sem --ignoreConfig recebeu TS5112 antes de avaliar a fonte; log preservado. Nenhuma suíte/tipagem global executada.

Config original vite.config.ts preservada SHA327C6FCD19B616DCE39B3B118A9C03B92DB51611E94F57120A44FB384BCE2489. Recibo CSS final SHA2453DC77 e guarda SHA9135BC92 preservados. Sem package/App/cliente/CSS/backend/canônicos/mapa alterados por este recorte; sem rebuild, SQL/DPAPI/HTTPAPI/proxy/backend real, publicação, ETL ou callback Hermes.

## Limite e encaminhamento

Não há autorização técnica inferida de um booleano. Mesmo guardaRealAprovada=true isolado não abre esse caminho; nova prova atual de resolução D29, guarda real inteira e liberação Farol precisam de revisão explícita antes de habilitar real/proxy. Guia Cedro foi usado como preparação, nunca como backend8080 em execução. Nenhum novo lançador, guarda futura, credencial ou token foi inventado.

Fonte estabilizada encaminhada ao Vigia via Farol. Lume mantém integração/package/App/sessão/cliente e único check final; ele inicia frontend atual próprio fictício e fornece endereço após consolidação. G01 bloqueado permanece independente desta entrega local. [Guia](../docs/desenvolvimento-dev.md) · [JSON completo](frontend-prumo-dev-config-lucas-20261008.json).
