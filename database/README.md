# Banco de dados do WMS Rodogarcia

[V9 FINAL p2-locks367](evidencias/d19-v9-2026-10-06-p2-locks-final.md):904/904 JPA/contrato/DDL em arquivos e129/129 lexical, fonte31/manifesto exatos, sem schema adicional. V1–V9 preservadas;904/122/411 anteriores históricos intactos. Parecer estrutural compatível; aceite local Farol/Vigia e SQL Server externo independentes. Nenhum SQL/Flyway/JVM/build.

[Reconciliação V9 histórica360/289](evidencias/d19-v9-freeze-2026-10-06-final-historico-p2-locks.md):904/904 e122/122 somente arquivos, V1–V9 intactas. Aceite BE14 retido P2 locks por Farol/Vigia07:27; não cobre backend em correção. Nova tarefa/manifesto exato p2-locks antes da comparação final. Sem SQL/conexão/Flyway/JVM/H2/build.

## Atualização local do pareamento complementar V9 — 06/10

Fonte31 copiada SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C, anterior ao Java. [Parecer/outputs novos](evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar.md): leitor424/424, somente arquivos, oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs de tabela/dez índices comuns/um único filtrado; seis CHECKs cumulativos substituídos e um novo de pares. ENTRADA_CONTINGENCIA acrescentado só em operacao_administrativa.tipo VARCHAR32, total38; auditoria permanece PEDIDO_ENTRADA/ENTRADA_EFETIVADA. AJUSTE mantém APLICACAO_CONTAGEM/AJUSTE_ESTOQUE, sem ação adicional. Serviço mantém VARCHAR24 e domínio ATIVO/ENCERRAMENTO_PENDENTE/INATIVO; V7 já tinha esses literais, V9 reafirma por DROP/ADD WITH CHECK formal autorizado, sem coluna/tabela/enum novo.

V1–V8 byte a byte preservadas. V9 preparada foi atualizada com cópia/hash anteriores; não afirmar imutabilidade integral V1–V9. Fonte1D/413, manifesto250, drafts e diagnósticos191/194 e194/194 anteriores são históricos preservados. Os registros abaixo de fonte corrente/contagens descrevem suas etapas anteriores. Nenhum grant/dado/estado/JSON alterado no banco; nenhuma execução SQL, JVM/JPA/H2/build/rede/ambiente. Mutabilidade, efeitos temporais, guardas, SQL emitido e JPA BE14 só após freeze/tarefa separados; não é aceite/homologação.


O responsável definiu SQL Server. Esta pasta contém V1 dos cadastros/auditoria, V2 do recebimento, V3 das unidades logísticas, V4 de capacidade/localização/movimentos, V5 de saída/FIFO/reserva, V6 de separação/retirada/retornos/avaria, V7 de cadastros/serviços/cálculo e V8 preparada para fechamento/versões/registros externos conforme doc31. V1–V8 preservadas na reconferência final p2-origem333:523/523 e163/163 em leitura, compatíveis estruturalmente; aceite BE13 permanece com Farol/Vigia. Nenhum SQL Server foi acessado ou migration aplicada; testes JPA/H2 de Cedro criam estrutura própria e não executam estes scripts SQL Server.

Atualização BE14: Farol informou aceite LOCAL BE13/333/Vigia favorável. [V9](migrations/V9__contagem_carga_contingencia_e_encerramento.sql) preparada do rodapé31/complemento996750 formal anterior ao Java: oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs, dez índices comuns e um único filtrado, cinco CHECKs cumulativos substituídos e um novo de pares. [Transcrição/matriz](contratos/v9-matriz-cobertura.md), [leitor](verificar-schema-v9.ps1), [procedimento](procedimento-v9.md) e [parecer](evidencias/d19-v9-preparacao-2026-10-06.md). PREPARADA/PREPARACAO_CARGA/onze tipos de contingência e resolucao_remanescente.carga_id formais; vínculo histórico CANCELADA conservado, efeitoRegistradoNoWms no JSON existente. Draft383/complemento395/residuais preservados. Estrutura preparada, JPA/guardas finais BE14 aguardam freeze; V1–V8 congeladas, sem SQL real/homologação.

Leitura posterior52D607 do31 acrescentou encerramento de vigência para CONTRATO_COBRANCA, mantendo TABELA/vínculo; CHECK financeiro ampliado nos dois sentidos, sem literal/coluna extra. Output407/fonte996750 e cópias preservadas. [Matriz/procedimento](procedimento-v9.md) identificam a fonte corrente desta preparação52D607 e o limite da comparação anterior com fonte viva em alteração.

Fonte corrente final desta preparação1D6413AF: SOLICITACAO_ENCERRAMENTO dos sete cadastros, incluindo serviço, cumulativa auditoria/admin; sem coluna/tabela/enum novo. Leitor413/413,19 tipos/71 ações auditoria,37 tipos administrativos/nove movimentos. [Parecer](evidencias/d19-v9-preparacao-2026-10-06.md) e [matriz](contratos/v9-matriz-cobertura.md) registram versões/metadados e hashes preservados. JPA BE14 permanece pendente, sem SQL real.

| Pasta | Uso previsto |
| --- | --- |
| migrations | V1–V8 preservadas e V9 em preparo conforme rodapé/complementos BE14 do schema31 |
| contratos | Transcrição/matriz técnica dos schemas29/31 para comparação estática; fontes oficiais continuam Cedro/docs |
| seeds | Dados de referência necessários à aplicação, quando forem definidos |
| evidencias | Baseline de preservação dos scripts; não comprova aplicação Flyway |

Consultar [migrations/README.md](migrations/README.md) para ferramenta, alvo, permissões, aplicação e recuperação. `migrate.ps1` confere a identidade do destino antes de chamar Flyway; foi inspecionado, sem executar conexão. Não há seed de usuário, preço, cliente real ou credencial.

D19 acrescenta [plano de permissões mínimas](permissoes-minimas.md), [preparação técnica local](../docs/33-preparacao-tecnica-local-backend.md) e [ensaio/recuperação](../infra/recuperacao-e-ensaio.md). V1–V5 permanecem preservadas. Novas migrations dependem dos schemas de Cedro encaminhados por Farol; não inventar estruturas antes do contrato. Nenhuma conexão está autorizada nesta rodada, inclusive Info/Validate.

[verificar-schema-v6.ps1](verificar-schema-v6.ps1) compara somente arquivos dos oito models novos com colunas/tipos/nulos/FKs/chaves/índices da V6; não inicia JPA, SQL, Maven ou Flyway. A [reconferência p2-final](evidencias/d19-v6-freeze-2026-10-06-p2-final.md) registrou compatibilidade com doc27/models finais: 74 colunas mais uma extensão, 14 FKs/oito chaves, 58 checks locais e seis verificações suplementares, sem divergências. V1–V6 SQL e outputs antigos foram preservados, com hashes antes/depois. Farol informou depois o aceite local BE10/BE11; ensaio SQL Server permanece externo.

[V7](migrations/V7__cadastros_servicos_e_calculo.sql), [transcrição do schema29](contratos/v7-schema-doc29.json) e [leitor estático V7](verificar-schema-v7.ps1) estão preparados. [Parecer/output datados](evidencias/d19-v7-preparacao-2026-10-06.md) registram 15 tabelas/172 colunas, 26 extensões opcionais, 36 FKs, 13 chaves únicas, 25 índices e 494 verificações locais, sem divergências frente ao contrato lido. Inclui situação do serviço VARCHAR(24) formalizada por Farol/Cedro; naquela preparação, JPA final aguardava freeze. A [correção documental/CHECK no parser-final](evidencias/d19-v7-2026-10-06-freeze-parser-final-tipos-concretos.md) teve 518/518 verificações (494 anteriores mais 24 de cobertura administrativa), 198 colunas JPA em leitura e 67/67 checks suplementares. Preserva a falha 65/67 e a cópia V7 anterior. A [reconferência final p2-avaria-final](evidencias/d19-v7-2026-10-06-p2-avaria-final.md) repetiu esses resultados contra doc29 e manifesto final de 222 arquivos, sem divergência estrutural. V1–V7 e outputs históricos preservados; somente o hash da fonte na transcrição foi atualizado, sem novo schema. Essas verificações são leitura de arquivos, sem execução de migrations ou aceite financeiro/comercial. V8 segue em leitura/planejamento até refinamento formal do doc31 e aceite local por Farol.

## Pontos a definir antes da aplicação no ambiente real

Preparo integrado BE03/BE04/BE15 em06/10: [pacote V1–V9](../infra/preparacao-tecnica-final.md), [diagnóstico final](../infra/verificar-preparacao-final-local.ps1), [permissões64 tabelas](permissoes-minimas.md) e [ensaio](../infra/recuperacao-e-ensaio.md). V1–V8 e diagnóstico56/outputs/manifestos anteriores preservados; V9 atualizada pelo complemento autorizado, com cópia/hash anteriores. Documentos copiados antes de edição. [Relatório novo](../infra/evidencias/d19-preparacao-tecnica-final-2026-10-06.md) separa preparo, artefato final em construção e homologação externa. Sem valores de ambiente/segredos/rede/SQL/JVM/H2/build/Flyway; sem RPO/RTO/volumes/provedor/credenciais/preços inventados. JPA BE14 será tarefa separada após freeze.

[V8](migrations/V8__fechamento_e_versoes.sql), [transcrição31](contratos/v8-schema-doc31.json), [matriz](contratos/v8-matriz-cobertura.md) e [leitor](verificar-schema-v8.ps1):11 tabelas112 colunas,26 FKs/10 constraints UNIQUE/26 CHECKs/16 índices comuns mais1 único filtrado, seis CHECKs INATIVO. [Reconferência final333/251](evidencias/d19-v8-2026-10-06-p2-origem-final.md):523/523 e163/163,17 models/118 campos,86 imutáveis/26 relações, dois CHECKs JPA pareados. Índice JPA nullable tem mesma chave/nome local próprio, sem predicado expresso; V8 mantém filtro SQL Server, sem observar DDL Hibernate/dialeto real. [Preparação469/470](evidencias/d19-v8-p2-origem-2026-10-06-final.md), [histórico490/132](evidencias/d19-v8-2026-10-06-freeze-final.md), fontes/cópias/outputs438/30/355/394/falhas e V1–V7 preservados. Nenhum SQL real; BE14/V9 aguarda schema.

Farol informou aceite LOCAL BE05/BE12 p2-avaria292. V1–V7 continuam congeladas, sem aplicação SQL. V8 foi ampliada somente pelos complementos formais31, preservando drafts/outputs; o P2 ORIGINADOS reteve o aceite328 e recebeu novo freeze333. Reconferência estrutural final523/163 concluiu em leitura; aceite BE13 depende de Vigia/Farol, com homologação SQL Server separada.

- Versão e recursos disponíveis no SQL Server.
- Banco/host exclusivos do WMS; esquema técnico `wms` preparado, sem autorização para tabelas de outros sistemas.
- Ambientes separados para desenvolvimento, validação e produção.
- Aplicar e validar as migrations no ambiente real autorizado pelo procedimento Flyway preparado, incluindo evolução com dados existentes; V1 a V5 estão somente em arquivos.
- Usuários da aplicação e permissões mínimas necessárias.
- Rotina de cópia de segurança, restauração e responsáveis.
- Detalhamento físico de quantidades, medidas e capacidade a partir das respostas recebidas e das propostas AC04, AC05 e AC10, sem reabrir as Q01, Q02 e Q06 como se estivessem sem resposta.

Não armazenar credenciais, cópias de produção ou informações reais de clientes nesta pasta. O desenho conceitual e as proteções de consistência estão na [arquitetura](../docs/02-arquitetura.md).

Preparação e aplicação de estruturas serão acompanhadas na etapa BE03 da [trilha backend](../states.md#trilha-backend); recuperação e preparo do piloto ficam em BE15. Registrar separadamente script preparado e execução no banco real.
