# D20 — relatório corrente após organização

Entrega manual local, somente database/infra. Launcher [iniciar-bancos.bat](../iniciar-bancos.bat), [guia curto](../README.md), [procedimentos](../docs/d20-procedimentos.md). Senha oculta em memória, leitura master/TLS e confirmação local, DEV antes PROD. Existentes preservam schema/dados; corrida vira Check antes de DDL/model. Novo comprova vazio. PROD não recebe migration/carga; entrada migration DEV separada em scripts/migrate.ps1. Agente não executou SQL/build/backend.

[Organização/de-para](../docs/d20-organizacao.md): 34 arquivos movidos byte a byte e ajustes relativos posteriores. [Inventário anterior](d20-organizacao-inventario-antes.json), [hashes/de-para](d20-organizacao-de-para.json), [preservação](d20-organizacao-preservacao.json) e [referências externas para Farol](d20-organizacao-referencias-externas.json). Todos os 489 arquivos anteriores permanecem atuais ou como original exato; manifestos e evidências históricos intocados. Raiz somente README e BAT; scripts/docs/config separados. Históricos inline e snapshots não são reescritos como execução atual. Farol informou que ajustou centrais/shared; esses arquivos não foram editados por Prumo.

Resultados após movimentos em [guardas](d20-guardas-reorg.json), [launcher/fixtures](d20-launcher-reorg.json), [parser](d20-parser-reorg.json), [pacote/links](d20-pacote-reorg.json) e [execuções/códigos](d20-reorganizacao-execucoes.json). CMD real: [Offline](d20-bat-offline-final-reorg.json), [outro cwd](d20-bat-outro-cwd-reorg.json), [inválido](d20-bat-invalido-reorg.json), [argumento extra](d20-bat-argumento-extra-reorg.json), logs correspondentes. Apenas Offline/fixtures, sem prompt real, segredo ou conexão.

| Verificação real local | Resultado |
| --- | --- |
| Guardas puras, permissões/alvo/SETs/credencial | 80/80, exit 0 |
| Orquestração com mocks/fixtures | 21/21, exit 0; existentes com schema preservados, retomada DEV, corrida Create→Check sem DDL, falhas TLS/identidade/metadados, recusa, ordem e descarte SecureString |
| Parser de migrations | 13/13, exit 0 |
| Pacote, sintaxe PS1, defaults offline e links | 336/336, exit 0; 271 links locais |
| cmd.exe BAT offline / outro cwd | exit 0 / 0; nenhuma leitura de senha ou conexão |
| cmd.exe argumento inválido / extra | exit 2 / 2; mensagem de pausa presente |
| Inventário e movimentos | 489/489 originais disponíveis, 0 perdidos; 34 movimentos com SHA idêntico; V1–V9 intactas |

BAT exibe **TLS com validacao obrigatoria**, inclusive offline: política exigida, não resultado de handshake. Nenhum ensaio SQL Server ocorreu. Tentativas anteriores permanecem preservadas; a repetição do check de pacote após os últimos ajustes conserva também a saída anterior.

[Manifesto corrente](d20-manifesto-corrente-reorganizacao.json), criado após checks: criacaoReal=EXECUCAO_MANUAL_PELO_OPERADOR_NAO_EXECUTADA_PELO_AGENTE. [Manifesto anterior](d20-manifesto-final.json) preservado integralmente como histórico, inclusive seu estado antigo de acesso. V1–V9 permanecem nomes/bytes/SHA exatos; nenhum V10. [Auditoria64 tabelas/687 colunas/34 UPDATEs](d20-auditoria-final.json) permanece evidência estática, não SQL observado. [Relatório anterior](d20-relatorio-final.md) e tentativas/falhas anteriores intactos.

Compatibilidade TLS real somente na execução manual, com confiança já existente no SqlClient/Windows; não herdar pin/perfil privado sqlcmd das referências. Sem bypass/instalação/config global/grants/outros bancos. Farol/Vigia revisam arquivos; centrais/shared/backend não editados por Prumo. Sem callback Hermes/ETL. Trabalho encerra no pacote local organizado/testado.
