# D21 — entrega local manual

[Launcher](../iniciar-bancos.bat) e [guia curto vigente](../README.md): bootstrap/upgrade administrativo de **WMS_DEV e WMS_PROD**, DEV completo primeiro. Ausentes usam primitives CREATE protegidas; existentes Check preserva schema/dados antes do upgrade. Flyway filesystem aplica somente pendentes; validate pré ignora somente pending, migrate/pós/info usam ignoreMigrationPatterns explicitamente vazio. Checksum/missing/future/failed/queda/falha de catálogo em DEV impede qualquer PROD. Sem clean/repair/baseline/DROP/grants.

[Contrato estável/hashes](d21-contrato-estavel.json), [contrato público Prumo](../docs/d21-contrato-flyway.md) e [Cedro](../../backend/evidencias/d21-contrato-flyway.md). Guardas/contrato ficaram estáveis para fecho Cedro. Perfil bootstrap-local distinto do runtime; args fixos, initSQL por conexão confere 7 SETs, servidor/banco/sa e recusa NULL. Senha oculta só memória/env do filho efêmero; parent/User/Machine/argumentos/arquivos não recebem senha. stdout/stderr brutos descartados; BSTR/cópias/filho/ambiente limpos em finally. Settings vazios, Maven -o/cache existente, sem instalar/baixar distribuição. TLS strict em SqlClient e JDBC; confiança existente de ambos necessária.

Catálogo real confrontado com expectativa derivada da fonte canônica: tabelas, colunas/tipos/nulos/identity/seed/incremento, PK/UNIQUE/FK e ações, CHECK/default, índices/chaves/ordem/include/filtro. Metadados incompletos, restrições disabled/untrusted, diferença estrutural ou definição alterada recusam conclusão. AST normaliza formas conhecidas IN/NOT IN/BETWEEN e preserva agrupamento AND/OR. Parser não fixa latest9 nem seleciona pendentes/checksums; sintaxe não suportada falha antes da conexão. Não prova drift universal, permissões/trigger/propriedades físicas/collation. SQL preparado não executado; dialect/provider/handshake reais só no operador.

| Conferência executada pelo agente | Resultado |
| --- | --- |
| [Fixtures finais](d21-fixtures-final.json) / [comando e exit](d21-fixtures-final-execucao.json) | 41/41, exit 0; ausente/existente/pendentes/checksum/queda/recusa/TLS/catálogo/ordem/dispose |
| Queda e retomada na mesma situação persistida | DEV parcial + PROD intocado; DEV completo/PROD parcial; nova SecureString, sem recriar existentes, aplica só restantes |
| V10 somente em filesystem de fixture | 10 fontes, expectativa inclui nova coluna; mock Flyway aplica só V10 em ambos; nenhuma V10 criada na pasta canônica |
| [Pacote/links/sintaxe/preservação](d21-pacote-final.json) / [exit](d21-pacote-final-execucao.json) | 672/672, exit 0; resultado real nestes arquivos, sem suíte/build D20 ou Maven Prumo |
| cmd.exe real [offline](d21-bat-offline.json) / [outro cwd](d21-bat-outro-cwd.json) | exit 0/0, sem segredo/SQL |
| cmd.exe [argumento inválido](d21-bat-invalido.json) / [extra](d21-bat-argumento-extra.json) | exit 2/2; pausa/mensagem presentes |
| [Snapshot D20](d21-snapshot-d20.json) | 45 cópias SHA conferidas antes das alterações, incluindo ambos manifestos; históricos/evidências D20 intocados |
| [Auditoria de alvo SQL frozen](d21-migrations-alvos.json) | 9 usam placeholder wmsDatabase; nenhum hardcode DEV/PROD; SHA V1–V9 originais mantidos |

[Tentativas/falhas de implementação](d21-falhas-implementacao.json), [leituras iniciais](d21-falhas-leitura-inicial.json), [39 fixtures anteriores](d21-fixtures-01.json) e check inicial01/02 preservados. O pré-check inicial recusou NOT IN; suporte corrigido, sem SQL. Farol informou CMD independente 3/3 aprovado, sem SQL; evidência de terceiros não substitui os resultados Prumo acima.

[Manifesto D21](d21-manifesto-final.json) corrente; D20 permanece histórico, sem regravar os manifestos. Raiz database somente README/BAT, auxiliares em scripts/docs/config, SQL nas pastas próprias. Supersedidos apenas os limites D20 de PROD vazio/migration DEV separada no uso manual administrativo. Backend continua identidade distinta/restrita. Nenhum SQL, Flyway Maven, build backend ou callback pelo agente. Estado real: **EXECUCAO_MANUAL_PELO_OPERADOR_NAO_EXECUTADA_PELO_AGENTE**. Pronto para Farol/Vigia, encerrar no pacote local.