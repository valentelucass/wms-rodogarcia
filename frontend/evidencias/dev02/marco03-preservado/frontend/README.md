# Frontend do WMS Rodogarcia

Aplicação React + TypeScript da D31, em exercício fictício explícito. Telas administrativas e coletor usam contratos atuais; o aplicativo executa respostas em memória e não chama API real. Visual básico preto/branco/azul. O README anterior, único arquivo preexistente observado, foi preservado em [evidencias/README-preexistente.md](evidencias/README-preexistente.md).

Na pasta `frontend/`, com Node24 LTS >=24.13 e npm11:

```powershell
npm ci
npm run dev
```

Abra `http://127.0.0.1:5178`. Porta ocupada causa falha; não encerre processos existentes. Para outra porta própria, use `npm run dev -- --port 5188`. `dev` usa `vite.dev.config.ts`; a configuração original `vite.config.ts` foi preservada. O padrão é fictício, sem proxy API. Não é necessário token, banco ou backend. Recarregar encerra os dados do exercício. G01 continua bloqueada: modo real é recusado antes de abrir servidor, inclusive com flag isolada. [Guia DEV01](docs/desenvolvimento-dev.md) delimita preparação e execução real pendente.

Para usar o artefato construído:

```powershell
npm run build
npm run preview
```

Abra `http://127.0.0.1:5188`. O preview registra PID/URL em `evidencias/marco02-local-browser-processo.json`. Ctrl+C encerra somente esse processo próprio. A URL fictícia mantida para Lucas e seu PID/snapshot constam no [recibo atual](evidencias/frontend-lume-entrega-lucas-20261008.json); não presume a identidade do processo antigo em5178.

Use perfil fictício Gestor, cliente1/armazém1, para percorrer a jornada inteira. As consultas e as respostas são identificadas como exercício. Etapas, seletores e botões após a resposta conservam referências por domínio:

- Entrada: criar pedido → consultar produtos e informar nota → iniciar conferência → registrar chegada → efetivar → entradas conferidas → unitizar com embalagem consultada → etiqueta → coletor, ler unidade e posiçãoA101, conferir destino e confirmar.
- Saída: consultar produto → pedido integral → sugestãoFIFO → reserva → etiqueta → leitura da reserva → destino consultado → separação. Pedido, unidade, reserva e revisão de etiqueta têm identidades distintas.
- Fiscal e físico: pedido separado → documento/coberturas consultadas → retirada com comprovantes/remanescentes → baixas e etiqueta atual; cenário próprio sem retirada → cancelar documento → retorno interno; após retirada → devolver por baixa → consultar nova entrada. Documento ou cancelamento fiscal não representam movimento físico.
- Financeiro: cálculo fictício → fechamento → versão/memória → demonstrativo → aprovação → entrega manual → referências NFS-e → tratativa de conflito. Valores/hashes são exemplos; interface não calcula cobrança nem envia ao ESL.

Cadastros, complemento fiscal, capacidades/importaçãoExcel, estoque/saldo/histórico, avaria/retorno/devolução, tabelas/serviços/vigências, ajustes, carga inicial, contagem e contingência têm consultas/formulários das rotas existentes. Havendo vários registros, selecione a identidade desejada. [Mapa atual de telas/contratos](docs/mapeamento-telas-contratos.md) delimita campos, rotas, DTOs, perfil e BE/FE; associação a rota não comprova execução de todas as variantes.

Operação não recebe ações de Supervisor/Gestor. Seleção controla apresentação; backend autoriza papel/alcance reais. Resoluções históricas são Gestor e exceçãoFIFO exige Supervisor/Gestor. Vazio, carregamento, conflito, recusa e resultado desconhecido são distintos. O seletor de respostas fictícias permite exercitá-los. Escrita incerta conserva comando/UUID para repetição explícita; interrupção não promete rollback.

Verificações somente frontend:

```powershell
npm run typecheck
npm run lint
npm test
npm run build
$env:PLAYWRIGHT_BROWSERS_PATH = Join-Path (Get-Location) '.tools/ms-playwright'
npm exec playwright -- install chromium
npm run test:browser
```

Playwright inicia/encerra seu preview próprio, não reutiliza processo existente e restringe requests ao frontend127.0.0.1:5188. Para ensaio em snapshot próprio, `WMS_FE_TEST_PORT=5192`, `FE12_BASE_URL=http://127.0.0.1:5192` e `WMS_FE_PROOF_LABEL` identificam porta/artefatos sem alterar bytes da fonte. `node tools/checks-marco02.mjs rotulo-novo` registra comandos/horários/exit/log e recusa sobrescrever rodada. Respondedor em memória não usa HTTP. Logs vermelhos anteriores foram preservados. Logs finais/hashes ficam no [marco Lume](evidencias/frontend-lume-entrega-lucas-20261008.md).

Derivação estática dos contratos, quando autorizada e fora do freeze de revisão:

```powershell
uv run --no-project --python 3.12 tools/contratos.py
node tools/mapeamento.mjs
```

MARCO01 permanece preservado com seus arquivos/hashes em `evidencias/marco01-preservado/`. MARCO02 registra as correções após os focais independentes, com logs próprios e nova fonte para revisão.

As saídas geradas são identificadas e não recebem edição manual. Consulte [organização](docs/organizacao.md), [transporte exato](docs/contratos-e-transporte.md), [versões](docs/versoes.md), [cobertura/limites porFE](docs/cobertura-e-limites.md) e [22 critérios com provas compartilhadas](docs/provas-jornadas-marco02.md). A contagem de contratos não substitui prova operacional.

Integração real depende de provedorRS256/Bearer, ciclo de tokens, atribuições, CORS e autorização segura de ambiente. Backend não tem login/token/senha próprios; gestão de contas não foi inventada. API, SQL Server, impressora/coletor físicos, fiscal/ESL reais e piloto não foram validados. Não houve publicação, commit/push, ETL ou rotina. Farol controla registros centrais/mapa; Vigia revisa fonte estável de modo independente.
