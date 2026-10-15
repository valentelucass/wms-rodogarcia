# Produção no PM2

Pedido de Lucas em 13/10/2026: gerenciar frontend e backend separadamente no PM2, com arquivo na raiz e nome contendo a porta. O exemplo Satélite é referência de organização; Java, flags, credenciais e tarefas daquele projeto não pertencem ao WMS.

**Ativação PREPARO-AUTO01 concluída em 15/10/2026:** V13 e direitos por coluna atuais em DEV/PROD, guardas aprovadas, produção no run `33cef6964be2430baf9f05201e5c9939`, API/UI online, PM2 salvo e HTTPS/versão ativa conferidos. O primeiro erro DEV era de aplicação da migration após histórico validado; V13 ainda pendente corrigida com GO antes de aplicá-la, sem repair. Recibo `orchestracao/.runtime/preparo-20261015/producao-conferida.json`.

## Uso

Na raiz, executar como administrador, no mesmo usuário Windows que possui os canais DPAPI de produção e o PM2 principal:

```bat
pm2-wms.bat atualizar
pm2-wms.bat status
pm2-wms.bat logs
pm2-wms.bat parar
pm2-wms.bat iniciar
pm2 ls
pm2 save
```

`atualizar` (também ação padrão sem argumento) prepara a fonte atual pelo launcher de produção, confere fingerprints/artefatos e guarda antes de interromper a versão ativa. Seleciona a versão preparada explicitamente, nunca por data de arquivo. Inicia API e UI, espera readiness de ambas e salva a lista do PM2 principal, mantendo as entradas dos demais projetos. Somente IDs WMS com nome, diretório e script atestados recebem start/stop/delete. Falha no preparo ou guarda preserva os processos ativos. A transição inicial do BAT para PM2 atesta recibos, perfil, comando, PID/início e portas; dono desconhecido impede encerramento.

**PREPARO-AUTO01, pedido de Lucas em 15/10/2026:** `atualizar` e `iniciar` preparam a fonte atual e depois executam migrations/permissões, antes da guarda final e de parar os processos. Usam o bootstrap Flyway protegido existente, DEV completo antes de PROD, somente bancos WMS já existentes. Não é mais necessário executar o BAT de bancos à parte. O ajuste reconhece as 17 colunas cadastrais e seis adicionais da edição de tabelas, sem ampliar direitos fora do contrato. Direitos já presentes são preservados.

O preparador valida contratos revisados, hashes e catálogo local antes de SQL. Flyway valida histórico, aplica somente pendentes e confere catálogo real; os helpers adequam permissões com guardas antes/depois. TLS privado deve corresponder ao processo SQL atual, sem recaptura ou bypass. DEV e PROD compartilham um mutex com o BAT de bancos: uma execução pode aguardar até180s pela liberação antes de qualquer SQL. Plan local não adquire essa trava. Se o limite vencer, o recibo informa tempo e PID observado quando disponível; nenhum processo é encerrado por isso. A espera local não repete conexão/migration e não altera a interrupção imediata após falha SQL. Falha interrompe sem alternativa e preserva processos ativos. Migrations/permissões concluídas permanecem registradas mesmo se uma etapa posterior falhar. Recibos em `orchestracao/.runtime/preparo-ambiente/`.

`iniciar` também prepara a fonte atual após parar ou reiniciar a máquina. A supervisão de um processo que cai usa o pacote atestado selecionado; não compila nem migra no reinício automático do PM2. `status`, `logs`, `parar` e `migrar` não preparam banco. Operações simultâneas são recusadas por `operation.lock`; após interrupção forçada, confira o PID antes de remover um arquivo residual.

| Aplicativo | Origem interna | Processo |
| --- | --- | --- |
| `wms-api-25590` | `http://127.0.0.1:25590` | Supervisor próprio → PowerShell → Java 21/Spring |
| `wms-ui-25591` | `http://127.0.0.1:25591` | Supervisor próprio → PowerShell → servidor estático Node |

Configuração: [ecosystem.config.js](../ecosystem.config.js). Acesso do operador: **https://wms.rodogarcia.com.br**. O HTTP nas origens loopback é a ligação interna do tunnel; não é o endereço de login. HTTPS/DNS/tunnel existentes são preservados.

Pode fechar o terminal depois da confirmação: os dois supervisores permanecem no PM2. Não foi instalado serviço nem tarefa de inicialização do Windows; após reiniciar a máquina, executar `pm2-wms.bat iniciar`. O BAT standalone `iniciar-prod.bat` continua disponível, mas recusa portas ocupadas; não deve competir com o PM2. Para atualizar uma instância gerenciada, usar o comando PM2 acima.

## Isolamento, segurança e supervisão

Por correção expressa de Lucas, o controlador usa agora o PM2 principal (`%USERPROFILE%/.pm2`), onde `pm2 ls` lista também os dois WMS. Nenhum reinício do Windows ou daemon é necessário. `PROD_PM2_MAIN_ACCESS_REQUIRED` indica que a sessão não conseguiu acessar o daemon existente; abrir o terminal como administrador. O controlador verifica acesso ao pipe antes de chamar o PM2, sem tentar criar outro daemon quando há acesso negado.

`pm2-wms.bat migrar` é a transferência única da instância isolada anterior: exige ausência de entradas WMS no principal, guarda atual, remove apenas as duas entradas atestadas do legado, salva sua lista vazia, inicia o pacote selecionado no principal e executa o equivalente a `pm2 save`. O preload `pm2-isolation.cjs` e o argumento interno `--isolado` permanecem apenas para retirar esse legado; não alteram arquivos instalados nem pipes do principal. Recibo em `orchestracao/.runtime/launcher-producao/pm2/main-migration.json`.

O helper principal atesta o PID servidor do pipe com `GetNamedPipeServerProcessId` e exige vínculo de pai com um processo vivo devolvido pela consulta PM2. Não pressupõe que um daemon iniciado por serviço contenha `Daemon.js` em sua linha de comando. A recusa informa a fase; falha anterior à guarda/parada preserva os WMS ativos.

API abre somente WMS_PROD com WMSPROD, TLS Mandatory e validação de certificado. Cada início exige guarda atual; configuração/guarda recusada sai com código 20, sem autorestart nem repetição SQL automática. Credenciais/chaves DPAPI são lidas pelo helper existente e passam somente na memória do Java. Não ficam em ecosystem, argumentos, dump ou logs PM2. Não há migrations, grants ou testes de negócio em produção.

A UI aguarda o recibo e o processo API da mesma versão. Cada componente tem supervisor independente; queda após readiness permite reinício limitado, com nova guarda da API. Parada Windows usa mensagem `shutdown`, arquivo de controle e encerramento apenas dos handles filhos criados pelo supervisor. Se o wrapper desaparecer, o PowerShell observa a morte do pai e encerra seus filhos; nunca usa kill genérico por porta. PM2 tem uma instância por componente, sem cluster/watch ou repetição ilimitada de falhas iniciais.

O rodapé continua mostrando a ativação bem-sucedida de frontend + backend. Reiniciar a mesma versão preserva o horário; publicar novo pacote cria novo horário. O identificador compilado permite avisar abas antigas. Recibos em `orchestracao/.runtime/launcher-producao/pm2/`, logs em `orchestracao/.runtime/pm2/logs/`.

Protocolo Windows e readiness seguem a [documentação oficial do PM2](https://pm2.keymetrics.io/docs/usage/signals-clean-restart/); recusa de reinício por código usa [stop_exit_codes](https://pm2.keymetrics.io/docs/usage/restart-strategies/). O isolamento de pipes foi conferido no código local da versão instalada.

## Verificação

Transferência posterior concluída pelo operador em 13/10/2026, 20:04 (Brasília): `main-migration.json` registra `saved=true`, IDs principais 19/20 online, supervisores 43604/44940. Java 71156/25590 e Node 17688/25591 consomem o mesmo run 4573ed7613a6431c84839dd473e3d213; horário do rodapé preservado porque não houve pacote novo. Não repetir `migrar` após este sucesso; usar `atualizar` para fonte posterior.

Ativação concluída em 13/10/2026: run `4573ed7613a6431c84839dd473e3d213`, API/UI online após saída do controlador; Java 33516/25590 e Node 68932/25591, supervisores 68112/54624. Rodapé HTTPS mostra `13/10/2026, 19:26:34 (Brasília)`. UI, API pública, relay e metadados responderam HTTP 200, certificado válido. Guarda nova 26/26, build BE/FE e 32 testes locais aprovados (2 wrapper, 6 seleção e 24 launcher). DEV 5904/35416 preservado. Sem login autenticado, testes de negócio ou fixtures PROD nesta publicação.

Testes locais sem banco: protocolo IPC de ready/parada do wrapper; recusa antes de readiness com código sem autorestart; seleção exata do pacote e recusa de hash alterado, alvo DEV, manifesto fora do candidato ou configuração modificada. Regressão do launcher e parsing PowerShell separados da ativação real. Evidências da publicação: `orchestracao/.runtime/pm2-prod-20261013/resultado.json`.
