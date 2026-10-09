# D30 Cedro — auditoria de suítes e efeitos

Inventário individual de 60 fontes de testes/helpers: 14 contextos Boot locais (12 H2 em memória e ApiHttp/header sem datasource), casos unitários seguros e SQLIT real separado. Todos os sources podem compilar. SQLIT permanece excluído da execução; os testes cujo nome inclui SQLServer mas usa apenas mocks/metadados continuam classificados pelo efeito concreto.

HTTP local com H2 efêmero está autorizado. Antes de qualquer fixture, a guarda exige perfil test único, URL de memória específica por classe, driver H2, flags sem Flyway/Liquibase/scripts e decoder fictício local, sem SqlServerConfig. Antes dos casos comprova DataSource/JPA e URL real por metadata. ApiHttp exige zero DataSource/JPA; outros dez H2 ainda aguardam contexto efetivo no final. As escrituras e CHECKs de falha dos casos existentes ficam restritos à memória H2 do child; não são migrations/DDL em servidor.

O child usa JDK/Maven descobertos localmente, offline, settings próprios vazios e ambiente por whitelist. Os cinco perfis Maven descobertos no pom ficam desativados. Não herda configuração Spring/WMS, credenciais ou opções JVM externas. Outputs e snapshots vão a build novo próprio; sem clean, restart, kill ou alteração de histórico. A03 mantém o caso e grava só arquivo novo CREATE_NEW, com SHA D20 preservado.

Este inventário autoriza o conjunto local condicionado às guardas; não certifica todos os contextos por fonte e não substitui a proveniência da execução final, ainda pendente após os últimos achados.
