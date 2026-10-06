# Preparação técnica integrada — BE03/BE04/BE15, V1–V9

Preparação local D19 em06/10/2026, por Prumo. Escrita somente database/infra/doc33; leitura de fontes locais. Farol recebeu V9/fonte413,18 checks e250 hashes; JPA BE14 depende de freeze e tarefa separada. O artefato final do backend ainda está em construção por Cedro: nenhum JAR atual, presença de target ou teste anterior é tratado aqui como entrega final. Estados/aceites permanecem com Farol.

| Nível de evidência | O que este preparo entrega | O que não comprova |
| --- | --- | --- |
| Preparo local | Migrations V1–V9 preservadas, inventário64 tabelas, controles declarados em fontes/properties, plano de acessos/identidade/ensaio/recuperação, diagnóstico estático e hashes | Código BE14/JPA final, execução de SQL, permissões/locks reais, conta/provedor ou equipamento |
| Artefato final | Procedimento de receber manifesto, logs/resultado de Cedro e revisão Vigia/Farol | Ainda não recebido/conferido nesta tarefa; não executar build paralelo ou repetir333 |
| Homologação externa | Casos, donos, registros e critérios de encaminhamento preparados | Alvo SQL Server/provedor/contas/TLS/rede/restauração/RPO/RTO/fiscal/comercial/operacional reais |

## Diagnóstico reproduzível sem ambiente ou rede

[Leitor final](verificar-preparacao-final-local.ps1) e [contrato técnico](contratos/preparacao-final-local.json) usam caminhos fixos deste repositório. O leitor preserva/reutiliza o [diagnóstico56 original](verificar-preparacao-local.ps1) como leitura de fontes, compara nove hashes/guardas e inventário de migrations, cobertura documental das64 tabelas, planos e estabilidade das fontes técnicas ao ler. Não avalia variáveis WMS_DB/WMS_OIDC, não consulta URLs/JWK/TLS/status, não abre SQL ou JVM/Flyway/Maven/H2 e não escreve arquivo. Bloqueio de leitura/formato retorna código seguro, sem raw/stack/conteúdo ou segredo. DTD/entidades XML externas são recusadas antes da leitura original do POM.

Comando autorizado nesta rodada, na raiz WMS:

```powershell
./infra/verificar-preparacao-final-local.ps1
```

Quem coleta a evidência escolhe sufixo novo, verifica que destino não existe e guarda output integral/AST/comando/início/fim/hashes e resultado PowerShell/LASTEXITCODE imediatamente após invocação. LASTEXITCODE nulo não prova saída0 nativa. Não alterar política/confiança para executar script bloqueado; informar o bloqueio seguro e encaminhar a TI/Farol. Não usar raw, outro terminal/env/identidade ou leitura de segredo para diagnóstico. Fontes técnicas podem mudar enquanto Cedro implementa: registrar diferença/hash, sem presumir freeze ou alterar backend.

[Configuração externa](configuracao-externa.md), [permissões](../database/permissoes-minimas.md), [migrations](../database/migrations/README.md), [ensaio/recuperação](recuperacao-e-ensaio.md) e [doc33](../docs/33-preparacao-tecnica-local-backend.md) são os procedimentos detalhados. Nenhum valor sensível deverá ser colocado neste pacote/evidência.

## BE03 — alvo e mínimo privilégio

Perfis/properties atuais separam local sem DataSource, test isolado e sqlserver-dev que efetivamente conecta. Default local/127.0.0.1, validate sem geração automática, SQL init never/Flyway desabilitado no startup e proteção de logs são declarações conferidas em arquivos, sem aplicação iniciada. Database.migrate.ps1 lê ambiente e conecta também em Info/Validate; foi apenas lido e não é diagnóstico permitido nesta rodada.

Lucas/TI com DBA a identificar nominalmente definem banco/schema WMS exclusivo, instância/versão/collation/TLS, topologia/ambientes, credenciais externas, quem autoriza cada conexão/aplicação e janela de manutenção. Não usar banco de outro projeto nem inferir alvo de variável encontrada em outro terminal. O backend atual valida formato/confirmed-target antes do DataSource; o wrapper consulta identidade real do servidor/banco por TLS. Uma validação não prova a outra; requer ensaio externo do caminho completo.

V1–V9 formam sequência incremental em arquivos,64 CREATE TABLE distintos; sem DML de dados históricos. V1 exige alvo compatível com esquema novo; não usar baseline/repair/clean para passar por conflito. Identidade da aplicação separada de migration e DBA/backup/consulta técnica, sem db_owner/sysadmin/db_datawriter. Aplicação só SELECT/INSERT/UPDATE necessários por objeto/coluna; fatos, operações, auditoria, revisões/snapshots imutáveis sem UPDATE/DELETE. Conferir SQL realmente emitido/colunas @Version no ensaio: anotações/GRANT por coluna não comprovam condição de linha, estado, escopo ou rollback.

DBA verifica permissões efetivas/herdadas, acesso a metadados/histórico Flyway e sessão de criação/DML dos índices filtrados. Conferir várias linhas NULL permitidas, chave preenchida única, ISJSON/checks/FKs confiáveis, contexto controlado em serviço e preservação de dados na evolução. Não presumir collation/case/normalização da identidade global; confirmar com regra canônica e dados fictícios no alvo autorizado. Nenhum grant/alvo foi criado.

## BE04 — contas e ciclo do JWT externo

Contrato existente: Resource Server JWT RS256, assinatura/emissor/audiência/sujeito/prazo/perfil/alcances; somente Bearer, sem login/emissão/refresh/sessão local. Prazo nominal entre emissão/expiração até15 minutos; fonte também guarda emissão futura além de60 segundos. Tolerância de relógio/cache e indisponibilidade reais ainda exigem ensaio, sem promessa de revogação imediata.

| Processo | Dono e decisão externa | Evidência futura sanitizada |
| --- | --- | --- |
| Provedor/login/fluxo de renovação | Equipe técnica com Lucas, sem fornecedor escolhido neste preparo | Conta fictícia de ensaio, issuer/audience/claims conformes; registrar caso/código/instante, nunca token |
| Conceder/mudar/desligar conta e alcance | TI administra identidade; Gestor aprova função/contextos com Caio/Supervisor; responsáveis nominais a confirmar por Lucas | Aprovação/mudança rastreável, novo token com alcance correto e tentativa recusada fora do alcance |
| Renovar/recuperar sessão | Equipe de identidade define armazenamento protegido/expiração/recuperação no provedor e cliente futuros | Token novo/expirado e falha de refresh; WMS não passa a emitir tokens por esse plano |
| Revogar conta/alcance/token | TI/identidade com Gestor define prazo e contenção exigidos | Comparar token já emitido e token novo após bloqueio; registrar janela residual e tolerâncias; sem afirmar invalidação instantânea |
| Rotacionar chaves/TLS e tratar indisponibilidade | TI/identidade com Lucas | Chave antiga/nova/desconhecida, JWKS inacessível/certificado rejeitado; erro seguro e sem abrir operação não autenticada |

JWT já emitido pode conservar autorização antiga até validade/tolerâncias aplicáveis. Desligar conta no provedor não comprova rejeição imediata no Resource Server. Se o prazo requerido exigir mecanismo adicional, TI/Cedro/Farol registram lacuna e contrato antes de implementar; não inventar introspecção, blacklist, Redis, fornecedor ou novo campo. Política de contenção e suporte externa ainda deve ser definida por TI. Lista vazia não concede alcance a perfil limitado; papel técnico SQL não equivale a função do operador. Guarda de serviço permanece obrigatória, inclusive replay e confirmação.

Segredos por mecanismo protegido fora do repositório/log/argumentos, com responsáveis, acesso mínimo e rotação. WMS usa chaves públicas, não recebe chave privada do emissor. Em diagnóstico usar IDs/correlação técnica/código/caso/instante e métricas autorizadas; sem header Bearer, claims brutos, XML/JSON/provas/nota/valor/driver/connection string. Erro seguro não autoriza raw ou confiança/certificado bypass.

## BE15 — ensaio, backup/restauração e contingência

| Etapa futura | Critério de prova | Responsável de referência |
| --- | --- | --- |
| Definir RPO/RTO/volumes/retenção/frequência | Decisão explícita considerando operação, capacidade e perda tolerável; não derivar RPO das36h informadas | Lucas/TI/DBA com Caio e responsável |
| Preparar backup/cadeia/chaves e destino isolado | Modelo de recuperação/compatibilidade/cadeia protegidos e restauráveis; retenção/criptografia/chaves externas definidas | DBA a identificar por Lucas/TI |
| Restaurar e conferir integridade | Restauração real em destino autorizado, tempo/instante recuperável/perda medidos; VERIFYONLY isolado não prova restauração | DBA/TI; Caio/Supervisor conciliam operação |
| Aplicar/evoluir V1–V9 | Hash/histórico/constraints/indexes/permissões/sessão/dados preservados, rollback/falha/retomada medidos; sem repair/clean automático | DBA/TI, com contratos/artefato estáveis de Cedro/Farol |
| Concorrência/privacidade | Reserva×contagem×retirada×avaria, preparo/replay, compromisso×inativação e auditoria tardia, sem efeito parcial/duplicado ou log sensível | Cedro/Vigia com Caio/DBA no ensaio autorizado |
| Recuperar fatos na contingência | Identidade global/conteúdo/hash/instante/dependências/prova, efeitos já WMS vinculados, ausências pendentes; saldo/reserva/história reconciliados | Caio/Supervisor/Gestor, com TI/DBA |

Não executar ensaio nesta rodada. Conservar evidências antes/depois; operação parcialmente confirmada ou timeout exige consulta idempotente/resultado original, não novo UUID para duplicar efeito. Contingência identifica fatos físicos ocorridos e se há efeito registrado WMS; não inventa nota/FIFO/origem/instante para fechar histórico. Restauração não exclui reserva/movimento/auditoria/revisão/documento/cobrança e não autoriza fiscal/billing real. Regularização de documentos existentes com Natalina/Controladoria fica separada do físico e do procedimento DBA.

Alvo ou backup sem prova impede aquela etapa externa, não desfaz trabalho local independente. Falha de migration exige conservar logs sanitizados/histórico/estado e diagnosticar DDL efetivamente confirmado antes de repetir; rollback transacional ou restauração não são pressupostos. Escolher correção incremental ou restauração autorizada conforme estado/impacto, com comparação de saldos/UUIDs/operações após retomada. Não configurar rotinas nem automação de restauração.

## Donos e próximos encaminhamentos

Lucas/TI responde pela definição do ambiente/identidade/hospedagem e nomeação do DBA. DBA executa privilégios/SQL/backup/restauração somente com alvo/autorização próprios. Caio e Supervisor/Gestor conciliam fluxo físico e compromissos; Gestor/comercial fornece preços/mínimo/GRIS/corte reais, sem valores nesta entrega. Natalina/Controladoria define regularização fiscal/documentos existentes; Mickael/TI valida coletor/impressora/cobertura. Nenhum desses nomes foi usado como identidade de execução pelo agente.

Cedro continua código/testes/artefato final; Vigia revisão; Farol mantém estados/contratos centrais/aceite/mapa e encaminha freeze BE14 para tarefa JPA separada. Prumo entrega somente preparação/diagnóstico/evidência local nesta rodada. FE03/login, equipamentos, implantação, comercial/fiscal e SQL real são validações externas/futuras, sem frontend ou publicação. [Relatório desta entrega](evidencias/d19-preparacao-tecnica-final-2026-10-06.md) identifica comandos, resultados, hashes, preservação e limites reais.
