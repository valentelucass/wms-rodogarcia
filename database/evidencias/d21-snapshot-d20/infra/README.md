# Ambientes do WMS Rodogarcia

**D20 corrente:** [prontidão e bloqueio preciso](d20-prontidao.md), [procedimentos locais seguros](../database/docs/d20-procedimentos.md) e [relatório/freeze](../database/evidencias/d20-relatorio-reorganizacao.md). TCP127.0.0.1:1433/sa/master confirmado; falta credencial protegida para CREATE real. Arquivos locais preparados, nenhum SQL ou alteração global. Registros D19 abaixo são históricos.

[Correção documental da identidade360/367 — revisão456](evidencias/d19-correcao-identidade-ciclos-2026-10-06.md): histórico restituído por cópia profunda da origem preservada;367 separado, outputs e V1–V9 intactos. Guarda68 relações de ciclo e fixtures18 recusam mistura manifesto/fonte/JAR/resultados; diagnóstico306/306 substitui a cobertura insuficiente238 para esta fronteira. Manifesto456 e contrato incorreto preservados, novo manifesto separado. Nenhuma comparação JPA repetida ou execução externa; revisão/aceite com Farol/Vigia.

[Preparo FINAL p2-locks367](evidencias/d19-preparacao-p2-locks367-2026-10-06.md): diagnóstico238/238 em arquivos, original56 preservado, manifesto289/fonte31 exatos, JAR78526951 bytes/SHA352211… e17 XML367/0/0/0 conferidos sem execução. Diretório target-be14 explícito;360 e snapshot/outputs antigos históricos. Configuração/JWT/contas/privilegios/backup/restauração/donos preparados; nenhum ambiente/rede/SQL/JVM/build. Aceite com Farol/Vigia, homologação externa pendente.

[Artefato360 histórico e retenção BE14](evidencias/d19-be14-360-historico-2026-10-06.md): JAR target-be14/78.524.597 bytes/SHA829A… conferido em leitura,17 XML360/0/0/0, snapshot separado. Leitor histórico52/52, sem executar JAR/Java/build/SQL/rede/env. Diagnóstico integrado com diretório opcional somente em procedimento futuro após novo freeze p2-locks; pacote194/307 anterior preservado. Não é aceite/homologação.

## Atualização local do pareamento complementar V9 — 06/10

Fonte31 copiada SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C, anterior ao Java. [Parecer/outputs novos](../database/evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar.md): leitor424/424, somente arquivos, oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs de tabela/dez índices comuns/um único filtrado; seis CHECKs cumulativos substituídos e um novo de pares. ENTRADA_CONTINGENCIA acrescentado só em operacao_administrativa.tipo VARCHAR32, total38; auditoria permanece PEDIDO_ENTRADA/ENTRADA_EFETIVADA. AJUSTE mantém APLICACAO_CONTAGEM/AJUSTE_ESTOQUE, sem ação adicional. Serviço mantém VARCHAR24 e domínio ATIVO/ENCERRAMENTO_PENDENTE/INATIVO; V7 já tinha esses literais, V9 reafirma por DROP/ADD WITH CHECK formal autorizado, sem coluna/tabela/enum novo.

V1–V8 byte a byte preservadas. V9 preparada foi atualizada com cópia/hash anteriores; não afirmar imutabilidade integral V1–V9. Fonte1D/413, manifesto250, drafts e diagnósticos191/194 e194/194 anteriores são históricos preservados. Os registros abaixo de fonte corrente/contagens descrevem suas etapas anteriores. Nenhum grant/dado/estado/JSON alterado no banco; nenhuma execução SQL, JVM/JPA/H2/build/rede/ambiente. Mutabilidade, efeitos temporais, guardas, SQL emitido e JPA BE14 só após freeze/tarefa separados; não é aceite/homologação.


Preparação local de BE03/BE04/BE15 em D19, somente em arquivos. Nenhum provedor, contêiner, servidor, conta real ou ferramenta de implantação foi escolhido ou configurado.

## Preparação disponível

Consolidação BE03/BE04/BE15 em06/10: [pacote integrado V1–V9](preparacao-tecnica-final.md), [diagnóstico final somente arquivos](verificar-preparacao-final-local.ps1), [baseline/contrato técnico](contratos/preparacao-final-local.json) e [novo relatório/evidências](evidencias/d19-preparacao-tecnica-final-2026-10-06.md). Diagnóstico56/output intocados, documentos copiados antes de editar conteúdo de manifestos. Artefato final em construção e JPA BE14 em tarefa posterior ao freeze; nenhum ambiente/segredo/rede/SQL/JVM/H2/build/Flyway acessado ou executado.

- [Configuração externa](configuracao-externa.md): perfis e entradas sem valores reais, contrato JWT e diagnóstico seguro.
- [Recuperação e ensaio](recuperacao-e-ensaio.md): permissões/recuperação a validar, backup/restauração, contingência e donos externos.
- [Diagnóstico em arquivos](verificar-preparacao-local.ps1): sem leitura de ambiente/segredos, conexão ou build.
- [Permissões mínimas SQL Server](../database/docs/permissoes-minimas.md) e [procedimento de migrations](../database/migrations/README.md).
- [Evidência local de Prumo](../docs/33-preparacao-tecnica-local-backend.md): resultados estáticos e dependências dos schemas seguintes.

Preparar esses arquivos não autoriza SQL Server, inclusive Info/Validate, execução de backup/restauração, publicação, rotina, frontend ou alteração de perfis Hermes/ETL.

## Levantamento técnico pendente

- Local de execução do frontend e do backend.
- Acesso da aplicação ao SQL Server existente.
- Autenticação e gestão dos usuários.
- Modelos de coletor, impressora, etiqueta e cobertura de rede.
- Separação entre desenvolvimento, validação e produção.
- Proteção das conexões e armazenamento de segredos fora dos arquivos versionados.
- Registro de falhas, alertas e suporte à operação.
- Cópias de segurança e teste de recuperação.
- Quantidade de usuários simultâneos, unidades armazenadas e movimentações diárias.

As definições serão documentadas em [decisões e pendências](../docs/06-decisoes-e-pendencias.md). Não incluir credenciais ou executar alterações em ambientes existentes durante o planejamento.
