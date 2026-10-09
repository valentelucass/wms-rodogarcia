# Frontend do WMS Rodogarcia

Aplicação React + TypeScript da D31. O comando de desenvolvimento padrão é REAL; o exercício fictício tem comando separado, explícito e sem proxy. Telas administrativas e coletor preservam os contratos e jornadas locais do MARCO03. O README preexistente foi preservado em [evidencias/README-preexistente.md](evidencias/README-preexistente.md).

A integração DEV02 está impedida por duas verificações ATUAIS: autenticação não configurada e falha TLS da única abertura da guarda Prumo, antes de SELECT. Nenhum backend ou frontend integrado foi iniciado. Não há fallback para mocks. [Guia DEV02](docs/desenvolvimento-dev.md) explica os contratos, as provas locais e o encaminhamento necessário.

Com Node24 LTS >=24.13 e npm11, instale as dependências em frontend/ com npm ci. O launcher da raiz, sob posse Farol, é iniciar-dev.bat. Quando as fronteiras estiverem resolvidas, ele fornecerá em memória a porta própria, o destino backend confirmado e a cadeia atual para npm run dev. Executar esse comando sem a cadeia válida termina com erro; não abre um exercício.

Exercício isolado, na pasta frontend/, somente em porta própria livre:

```powershell
$env:WMS_FE_PORT = '<porta própria livre>'
npm run dev:ficticio
```

Abra a origem 127.0.0.1 na porta escolhida. Porta ocupada causa falha, sem troca automática de porta. Preserve os processos existentes em5178,5188 e5189. O modo fictício não usa token, banco ou backend; /api é recusada localmente, sem upstream. Recarregar descarta os dados do exercício. O visual/CSP e a configuração original vite.config.ts permanecem preservados.

npm run build compila a aplicação; o artefato não comprova servidor nem sessão. npm run build:ficticio compila explicitamente o exercício. Nenhum build autoriza integração real. Os snapshots e processos fictícios do MARCO03 conservam sua identidade histórica e não são anunciados como frontend integrado.

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

Integração real depende de provedorRS256/Bearer, ciclo de tokens, atribuições e ambiente seguro; o desenvolvimento usa proxy de mesma origem, sem abrir CORS. Backend não tem login/token/senha próprios; gestão de contas não foi inventada. API, SQL Server, impressora/coletor físicos, fiscal/ESL reais e piloto não foram validados. Não houve publicação, commit/push, ETL ou rotina. Farol controla registros centrais/mapa; Vigia revisa fonte estável de modo independente.
