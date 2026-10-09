# Desenvolvimento frontend — D31-DEV01

**Estado observado em 08/10/2026: G01 bloqueado.** A [guarda Prumo](../evidencias/frontend-prumo-dev-guarda-lucas-20261008.md) nao comprova resolucao segura atual do incidente D29. Nenhum backend com SQL, proxy ativo ou FE→API real foi iniciado por esta preparacao. CSS aplicado/provado permanece separado. O processo existente em5178 nao foi reiniciado; este guia nao certifica que essa URL esteja respondendo agora.

## Contrato para Lume integrar

Em `package.json`, sob posse Lume, o script proposto e `"dev": "vite --config vite.dev.config.ts"`. A configuracao separada mantem host127.0.0.1, porta5178 e strictPort; preserva `vite.config.ts` e os checks existentes. Depois de integrar o script, `npm run dev` inicia **somente frontend ficticio**. Enquanto nao integrado, a mesma preparacao pode ser escolhida explicitamente com `npm exec vite -- --config vite.dev.config.ts`; nenhum desses comandos autoriza encerrar ou reutilizar processo existente.

O modo padrao e `ficticio`. A configuracao fixa o modo apresentado ao aplicativo como ficticio e deixa `server.proxy` ausente. Um middleware recusa `/api` localmente com503; nao encaminha requests ao backend. `VITE_DATA_MODE=real` falha antes de iniciar o servidor: recibo ausente/inconclusivo ou G01 bloqueado produzem erro claro. Trocar um booleano no recibo ou definir uma flag de ambiente nao libera o modo real. Prova atual, guarda completa e liberacao Farol exigem revisao explicita desse contrato; nao ha ativacao automatica por arquivo, listener ou preflight antigo.

O contrato de proxy esta preparado como `proxyApiDev`: prefixo `/api`, `changeOrigin=false`, sem `rewrite`. Quando autorizado e integrado apos a revisao, preservara `/api/v1/...` e o frontend usara a mesma origem; Lume mantem App/sessao/cliente/autenticacao e o script. Nao alterar CORS fechado nem CSP `self`; nenhum wildcard, senha, token ou emissor novo.

Destino backend HTTP conforme [guia Cedro](../../backend/docs/frontend-dev.md): `http://127.0.0.1:8080`. A configuracao usa `WMS_PORT` informado pelo contrato do processo backend (padrao8080), sem descobrir listeners nem sondar servicos. `WMS_DEV_BACKEND_URL` pode informar outra porta **somente** como `http://127.0.0.1:porta`, sem caminho, credenciais, query ou fragmento. Porta deve estar entre1 e65535. Essa URL nao informa nem presume host SQL. Entradas sao nao sensiveis do processo, nunca segredo de banco ou JWT; Prumo nao leu `.env` ou DPAPI.

## Guarda requerida e execucao futura

O responsavel pelo SQL precisa fornecer prova atual de resolucao segura de D29. Prumo avalia e, somente se seguro no escopo autorizado, confere nova guarda minima: WMS_DEV real, WMSDEV restrita, TLS sem bypass, permissoes por objeto/coluna sem administracao/DDL, schema/historico/checksums e nenhuma migration pendente ou mutacao automatica. Farol libera a execucao por evidencias, nao por flag. Ausencia, erro de transporte ou inconclusao mantem bloqueio, sem retries/fallback/PROD/sa/migration/alteracao de servidor/acessos/sharedruntime.

Depois desses requisitos e da revisao do caminho real desta configuracao: Cedro inicia somente backend proprio conforme seu guia/perfil complementar; Lume integra modo real, Bearer em memoria e base de mesma origem, e inicia frontend proprio com `npm run dev`. Porta ocupada exige outra porta propria coordenada, sem restart/kill existentes. A prova real permanece separada das respostas ficticias. No estado atual, nao ha endereco integrado real verificado para Lucas.

Esta entrega prova apenas erros de configuracao e frontend ficticio em copia consistente/porta propria5191, sem request API/proxy/backend, build ou suite geral. [Recibo delta Prumo](../evidencias/frontend-prumo-dev-config-lucas-20261008.md). Integracao/checks finais pertencem a Lume; Vigia revisa a fonte estabilizada.
