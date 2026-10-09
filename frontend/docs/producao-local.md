# Frontend de produção local — PROD-LAUNCH01

O launcher WMS da raiz coordena `iniciar-prod.bat` e `iniciar-prod.bat --preparar`. O segundo comando prepara builds reais isolados, sem SQL/backend. A inicialização operacional depende do canal próprio PROD, guarda atual e backend confirmados por Farol. Preparação e harness sintético não comprovam produção funcionando.

## Interface de build

Na raiz do WMS, com Node/dependências já presentes:

```powershell
node frontend/tools/build-prod.mjs --release <caminho-absoluto-candidate/frontend> --run-id <uuid>
```

O pai `candidate` deve existir dentro de `orchestracao/.runtime/launcher-producao`. São criados `frontend/source` e `frontend/dist` novos; um candidato existente é recusado. Não há instalação, limpeza de `dist` vivo ou escrita da aplicação. O snapshot copia as fontes atuais necessárias e usa dependências instaladas por junction. Nenhum `.env` é carregado. Tipagem é executada no snapshot; Vite faz build `production` com `VITE_DATA_MODE=real`, sem publicar variáveis `VITE_*` herdadas, sourcemap ou servidor DEV. A CSP do HTML gerado elimina o WebSocket de desenvolvimento; o `index.html` vivo permanece igual.

O script compara universo/hashes da fonte antes da captura, depois da captura e após o build. Mudança concorrente recusa sucesso/promoção, preservando o candidato para inspeção. Build/tipagem falhos não publicam manifesto pronto nem substituem versão em execução. O stdout final `WMS_PROD_FRONTEND_BUILT` ocorre somente após as verificações.

Saídas:

- `frontend/dist/build-manifest.json`: `natureza=WMS_PROD_FRONTEND_BUILD`, `runId`, `sourceHash`, `buildmode=real`, `builtUtc`, `artifacts[{path,bytes,sha256}]` e entradas `inputs[{path,sourcePath,usedPath,bytes,sha256}]`.
- `sourceHash`: SHA256 UTF-8 da concatenação ordenada `path + NUL + sha256 + LF` das entradas capturadas. É a identidade da fatia frontend, não do JAR/backend.
- `usedPath` aponta os bytes efetivamente usados no snapshot; `sourcePath` indica a origem viva à captura. O launcher liga este manifesto ao JAR/guarda/run próprios, sem presumir equivalência entre builds independentes.

## Interface de servidor

```powershell
node frontend/tools/prod-server.mjs --dist <candidate/frontend/dist> --manifest <candidate/frontend/dist/build-manifest.json> --port 25591 --backend http://127.0.0.1:25590 --public-origin https://wms.rodogarcia.com.br
```

Portas25590 backend/25591 frontend foram escolhidas pelo inventário Prumo/Farol; precisam estar livres no instante de inicializar. Bind exclusivamente `127.0.0.1`, sem fallback de porta, kill ou restart. A CLI recusa destino externo, credencial/path/query no destino, mesma porta FE/BE, argumento duplicado/ausente e origem divergente.

Todos os assets são conferidos por tamanho/SHA antes de `listen`; manifesto fictício, arquivo não listado, raiz inválida, traversal ou link/junction para fora são recusados. O servidor conserva em memória os bytes verificados dessa versão. Alterar arquivos depois não altera silenciosamente o que ele serve; atualização exige candidato validado e processo próprio novo coordenado, sem interromper instância existente. Assets em `assets/` têm cache imutável; HTML/public têm revalidação. Fontes e `.env` não são servidos; asset ausente não recebe HTML de SPA.

`GET /__wms/health` retorna `STATIC_READY`, PID/porta, `runId`, `sourceHash`, SHA do manifesto e `backendReadiness=NOT_PROBED`. Não chama API/banco. Readiness FE estático não implica readiness backend, login ou banco. `POST` em rotas estáticas é recusado. Não há CORS aberto nem relaxamento da CSP.

## Relay e autenticação atual

O cliente D32 usa `/api/auth` same-origin para sessão/CSRF e `/api/v1` com Bearer. `/api` é encaminhado integralmente ao backend loopback confirmado: método, caminho/query, bytes de request/response, status, correlação e `Set-Cookie` são preservados. Não há parse JSON, arredondamento, rewrite, redirect seguido, replay ou retry. Erro de transporte produz502 `WMS_PROD_RELAY_UNCONFIRMED`; escrita pode ter resultado incerto e não é reenviada pelo servidor.

Host é limitado ao domínio frontend exato ou à autoridade loopback técnica. `Origin` recebido permanece intacto; não é convertido em HTTPS ou substituído por origem aprovada. `Forwarded`, `X-Forwarded-*`, `CF-*` e cabeçalhos hop-by-hop fornecidos pelo cliente são retirados. Este relay não emite cabeçalho de topologia fingindo HTTPS. A verificação de origem/CSRF e a autorização continuam no backend.

Contrato backend acordado: origem fixa `https://wms.rodogarcia.com.br`, `secure-cookie=true`, `proxy-origin` vazio. `LoginConfig` permite o alias/detecção dinâmica DEV07 somente em `sqlserver-dev/test`, loopback; isso não é autorização PROD. Refresh/CSRF mantêm Secure/HttpOnly/SameSite=Strict e Path `/api/auth`. JWT fica apenas na memória do navegador, conforme cliente existente; este servidor não cria conta, token ou provider nem recebe segredo de banco/autenticação em argumentos.

HTTP loopback é superfície técnica para assets/readiness. Não certifica login HTTPS operacional com cookies Secure. Nesta fase não se altera modo DEV/tunnel nem se contorna origem/CSRF para testar login local.

## Informações para a etapa futura Cloudflare

| Entrada futura                      | Origem local planejada   | Uso                                                                   |
| ----------------------------------- | ------------------------ | --------------------------------------------------------------------- |
| `https://wms.rodogarcia.com.br`     | `http://127.0.0.1:25591` | Assets e `/api` same-origin via relay.                                |
| `https://wms-api.rodogarcia.com.br` | `http://127.0.0.1:25590` | Backend público futuro, com contrato/restrição de exposição próprios. |

O frontend não passa a chamar o segundo domínio diretamente: manter `/api` relativo evita introduzir fluxo cross-origin/cookies/CORS. A futura entrada HTTPS deve preservar Host/Origin coerentes; o backend autoriza a origem frontend exata. Túnel, DNS, CNAME, ID de túnel, credenciais e TLS público não foram criados ou inventados. Portas planejadas não são listeners operacionais comprovados por este documento.

## Verificações proporcionais

```powershell
node --test frontend/tests/prod-launcher-server.test.mjs
```

O harness cria somente HTTP estático/upstream sintético próprios em portas efêmeras e fecha os listeners que criou. Abrange bytes exatos, cookies/correlação,5xx/redirect sem replay, recusa de configuração/host/traversal, hashes, arquivos extras, links, colisão e cancelamento. O teste `tests/prod-launcher-config.test.ts` verifica configuração de build; nenhum desses casos usa API/banco PROD. Build real e tipagem do snapshot têm comandos/logs/manifesto no recibo Lume em `orchestracao/.runtime/launcher-producao/lume`. Não repetir suites FINAL14/VALID001–007 como parte deste preparo.
