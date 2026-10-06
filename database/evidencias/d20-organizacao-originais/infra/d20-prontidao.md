# D20 — prontidão local database/infra

**Entrega manual corrente:** [launcher BAT](../database/iniciar-bancos.bat), [guia curto](../database/README.md) e [21 fixtures](../database/evidencias/d20-launcher-final.json). Agente não executa SQL. Operador fornece senha oculta, vê identidade real por leitura TLS e confirma na interação. Existentes com schema/dados são preservados e conferidos; novos vazios. DEV existente permite PROD faltante. Nenhuma compatibilidade TLS real afirmada pelos testes offline; sem herdar perfil privado/pin de referência ou mudar confiança global.

Prumo, 06/10/2026, BE03/BE15. [Plano revisado](../database/d20-plano.md), [procedimentos](../database/d20-procedimentos.md), [relatório](../database/evidencias/d20-relatorio-final.md) e [manifesto](../database/evidencias/d20-manifesto-final.json). Nenhum serviço/servidor/global/default/banco existente/login/GRANT alterado. Escrita limitada a database/infra; registros centrais e backend pertencem a Farol/Cedro.

| Parte | Resultado / encaminhamento |
| --- | --- |
| Pacote local | Guardas, CREATE separado, matrizes efetivas, diagnóstico/ensaio fictício/recuperação e evidências em arquivos. V1–V9 intactas, nenhuma V10. Revisão final Vigia/Farol após freeze. |
| Alvo CREATE | Lucas confirmou TCP127.0.0.1:1433, SQL auth sa, master. WMS_DEV primeiro, WMS_PROD depois; apenas CREATE de nomes inexistentes, sem migrations/cargas PROD. |
| Acesso CREATE real | **BLOQUEADO: credencial sa indisponível por canal protegido nesta sessão.** Farol verificou somente presença de WMS_DB_PASSWORD/WMS_DB_MIGRATION_PASSWORD/WMS_DB_CREATION_PASSWORD em Process/User/Machine: ausentes. Não há conector/cofre administrativo WMS autorizado; não usar credenciais de outros projetos. |
| Evidência de rede/serviço | Farol informou TCP alcançável/MSSQLSERVER Running. Não é conexão SQL/autenticação/TLS/identidade confirmada. Prumo não abriu conexão nem leu guia/segredo compartilhado. |
| Entrada futura | Operador local autorizado usa Read-Host -AsSecureString e objeto SecureString/PSCredential; inspeção master somente leitura obtém ServerName e existência dos dois nomes. Sem senha literal/ambiente persistente/log. Comando preparado no procedimento, não executado. |
| Identidade/TLS/model/vazio | Ainda não observados. Executor exige TLS validado, ServerName e ORIGINAL_LOGIN na mesma sessão, autocommit, inexistência e metadados completos de model/banco novo. Falha conserva banco, sem DROP/fallback/GRANT. |
| Aplicação / migration | Contas distintas restritas a WMS, nunca sa. Cedro protege cada conexão da aplicação; wrapper gera initSql por conexão Flyway com SETs/servidor/banco/login e recusa roles amplas/conferência desconhecida. Permissões efetivas aguardam ensaio real, sem concessão neste pacote. |
| Versão/collation/compatibilidade/defaults | Registrar o ambiente existente na inspeção autorizada; não instalar/atualizar/alterar para adequá-lo. Ausência não bloqueia arquivos locais. |
| Recuperação | RPO/RTO/retenção/destino/encarregado são implantação externa. Restore real em destino isolado aprovado é prova necessária; VERIFYONLY/H2/estática não substituem. Não criar rotinas nem terceiro banco agora. |

Guia compartilhado `../.runtime/SQL-SERVER-EXISTENTE.md` pertence a Farol; Prumo não o altera. Trabalho deste pacote encerra no freeze local. Criação efetiva dos bancos e ensaios SQL permanecem não executados, separados do aceite dos arquivos.
