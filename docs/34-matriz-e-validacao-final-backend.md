# Matriz e validação integrada do backend — D19

**Escopo local D19 concluído.** Esta matriz reúne implementação, testes observados e pendências externas por bloco; não representa homologação ou liberação operacional. Ordem e arquivos no [documento 26](26-execucao-continua-backend.md). Base preservada no [documento 25](25-validacao-pedido-saida-e-reserva.md): 197 testes aprovados em D18. Status oficiais no [states.md](../states.md).

## Matriz BE01–BE16

| ID | Implementado/aceito localmente | Testado/verificado com evidência | Pendente externo e dono |
| --- | --- | --- | --- |
| BE01 | Modelo35 e contratos14–31 integrados: entidades/transições, quantidades/datas/precisão, erros/filtros/perfis e jornadas FE | Jornada HTTP13 própria; Lume encerrou replay legado de chegada; revisão integrada final | Caio/gestor/Natalina: conferência operacional, propostas e documentos reais |
| BE02 | Spring MVC/Maven Wrapper, camadas/DTOs/properties, ambientes seguros e API/erros | Build integral367/17 XMLs/JAR; base anterior preservada | Lucas/equipe técnica: hospedagem/ambiente operacional |
| BE03 | JPA, V1–V9 em arquivos, Flyway externo, validate/SQL-init never, alvo/TLS e procedimentos/permissões | V7 518+67; V8 523+163; V9 final904/904+129/129 em leitura, sem SQL | Lucas/TI e DBA: alvo/credenciais, versão/collation, aplicação/evolução, constraints/índices, locks/permissões/restauração |
| BE04 | Resource Server RS256/claims, três perfis/alcances nos serviços, replay e auditoria transacionais em todos os módulos | JWT RSA efêmero,403/alcance/replay/rollback e chamadas fora HTTP no367 | Lucas/equipe técnica: provedor/contas/login, renovação/revogação/rotação, contenção/TLS e indisponibilidade reais |
| BE05 | Cadastros/fiscal contextual, Excel integral, capacidade/conjuntos, serviços/tabelas/vigências e inativação segura dos sete tipos | Cadastros28, financeiro52, fechamento43 e jornada13; encerramento/novo compromisso nas duas ordens | Caio/gestor/Natalina: dados, capacidade, layout operacional e referências fiscais reais |
| BE06 | Recebimento manual/XML/partes, quarentena/estorno, efetivação única, devolução e entrada inicial preparada uma vez | Recebimento28/XML6 preservados; entrada temporal física02/09 versus registro05/09 e carga inicial na jornada | Caio/Natalina/Lucas: conferência física/documentos reais e persistência SQL Server |
| BE07 | Unidade/composição/identidade/etiqueta, divisão/reagrupamento, remanescente e leitura integral da carga inicial | Unidades36 preservados; seis fronteiras BE07, condição/origem/UUID e etiquetas de carga inicial | Mickael/TI/Caio: formato/impressão/leitura/coletores reais |
| BE08 | Capacidade/conjuntos/movimentos/bloqueios, físico/disponível/reservado, impedimento próprio de contagem e recomposição por origem | Estoque31 preservados; ajuste a zero/fim datado, não recuperar retirada, concorrência/rollback e P2 locks | Caio/Mickael/TI/Lucas: capacidade/equipamentos e isolamento/locks SQL Server |
| BE09 | Pedido integral por contexto, FIFO/desempate/exceção, reserva atômica sem vencimento e remanescente protegido | Pedido44 preservados; expedição43, avaria posterior, disputa de saldo e conciliação integral | Caio/Lucas: homologação dos exemplos e concorrência SQL Server |
| BE10 | Leitura/separação, cobertura de documentos existentes, retirada integral e contingência temporal comprovada | Expedição43 e jornada13; remanescente/origem/fiscal separados do físico, replay/rollback | Natalina/Caio: NOTAZZ/XML/documentos e percurso físico no armazém |
| BE11 | Cancelamentos/retornos, avaria/responsabilidade/reparo e fatos temporais por ciclo/origem | Expedição43; bases100/50, condições efetivas, novo ciclo sem dano antigo e integração física/financeira | Caio/cliente/gestor/Natalina: tratativa real, responsabilidade e documentos |
| BE12 | Fato único/rateio, vigências, diária/pico, mínimo/GRIS, marcos de avaria, encerramento e cálculo explicável sem reescrever snapshot | Financeiro52; R$0.01 conservado, SKU A/B, vigências/picos/avaria; jornada COMPLETO/memória122 fictícia | Gestor/comercial: preço/unidade/mínimo/GRIS/corte e validação AC04–AC09; Natalina: parâmetros fiscais |
| BE13 | Ciclos/âncora/versões/bytes/hash, decisão/entrega manual, referências/tratativa/ajustes e resolução histórica | Fechamento43; aceito333 e três P2 preservados; integração final sem reabrir recorte aceito | Natalina/gestor: ESL/NFS-e/procedimento e condições reais; Lucas/DBA: SQL Server |
| BE14 | Consultas/indicadores/validade, contagem/revisão/ajustes, carga PENDENTE/PREPARADA/REGULARIZADA e contingência global/dependências/efeitos temporais | Contingência19+jornada13; P2 aninhado nas duas ordens, divergência sem efeito, replay/perfis/rollback | Caio/cliente: estoque inicial se existir/origem/contagem/contingência física; Mickael/TI: equipamentos/cobertura |
| BE15 | Artefato local final, diagnósticos/configuração/privilégios e planos de backup/restauração/ensaio/contingência em arquivos | Build367/17 XMLs/JAR/hashes e parecer integrado; diagnóstico final e fecho técnico no33 | Lucas/DBA/Caio/Natalina/Mickael: SQL, recuperação medida/desempenho, provedor/fiscal/equipamentos/piloto; FE13 futura |
| BE16 | Properties/construtores/validação nos serviços, Spotless/Enforcer e regras das camadas | Spotless276 Java, ArchUnit5 importa toda produção pelo CodeSource; build integral final | Nenhum externo para padrões locais; homologação continua BE15 |

**Código local restante D19:** nenhum independente identificado na auditoria de Cedro e nas revisões finais. P2-locks encerrado; retomada de Java somente por achado concreto. Etapas que exigem ambiente/homologação real permanecem em validação externa no states.

## Evidência desta autorização

Histórico D19 — primeiro bloco aceito após P2: `spotless:apply` aprovado (157 Java) e `clean verify` aprovado às 00:35:36 de 06/10/2026, 2:35 min, **240 testes sem falhas/erros/ignorados** e JAR empacotado. Treze XMLs conferidos por Farol: 197 anteriores + 43 expedição. Vigia favorável no freeze p2-final, sem achados materiais; comparação V6 de 74 colunas e 58 checks sem divergências. A [evidência do bloco](28-validacao-separacao-retirada-retornos.md) preserva cada tentativa, hashes e limites. Naquela etapa seguiu BE05/BE12; o JAR240 foi sucedido pelo artefato final D19 abaixo. Não há migration aplicada, cálculo/cobrança real, publicação ou homologação declarados.

## Limites de aceite

Segundo recorte aceito localmente após P2 financeiro BE08: [evidência30](30-validacao-servicos-e-calculo.md), clean verify292/0/0/0 e spotless210 Java,14 XMLs/JAR/222 hashes conferidos, Vigia favorável e V7/JPA compatíveis em arquivos. Naquela etapa seguiram BE13/BE14/inativação definitiva/integrado, agora concluídos no recorte local. Não se trata do artefato final de todos os blocos.

Vigia conferiu preparação BE03/BE04/BE15 em leitura e concluiu às02:44 de06/10: **favorável ao preparo local, nenhum achado material no recorte**. Properties/config, separação Flyway/build, JWT/alcance e documentação de permissões/recuperação coerentes com seus limites. Não executou testes/build/consulta de ambiente/credenciais/SQL; não homologa provedor, revogação, permissões ou recuperação. BE13/BE14 e o conjunto final permanecem fora desse parecer.

Concluir o código local não conclui SQL Server, identidade real, equipamentos, fiscal/cobrança ou piloto. H2 cria estrutura pelos models, não comprova scripts SQL Server, índices/CHECKs específicos, locks/deadlocks, collation ou recuperação. Propostas AC01–AC16 continuam identificadas; preços e parâmetros reais são configurações externas, não valores assumidos.

Sem frontend, SQL Server real, emissão fiscal/NFS-e, cobrança a clientes, publicação, commit/push, rotinas, alteração de perfis Hermes ou ETL. Git observado: `main`, zero commits; baseline SHA-256 de 142 arquivos D18 registrada por Farol antes das alterações D19 para comparar a preservação.


## Fecho local final após P2-locks

[Entrega final](../backend/evidencias/d19-bloco4-entrega-p2-locks.txt), [evidência32](32-validacao-fechamento-e-contingencia.md) e [preparo33](33-preparacao-tecnica-local-backend.md). Clean verify **367/0/0/0**,17 XMLs/JAR,4:05min,07:49:48−03; spotless276 Java,2.111s,07:45:38. Fronteiras19/0/0/0;360 cenários preservados por nome mais7. Vigia favorável às07:58, sem achado material residual no recorte; Prumo V9/JPA904/904 e suplemento129/129 em arquivos. O parecer novo fecha somente as diferenças P2 e a integração atual, preservando os aceites240/292/333 e a revisão dos demais achados.

[Manifesto289](../backend/evidencias/d19-bloco4-freeze-p2-locks.sha256) SHA A257476BCC711BCB14A42A27FAE72C092E68F85460700D758B8BC9C70CE38233 conferido,286 iguais/3 diferenças frente360. Artefato [wms-backend-0.0.1-SNAPSHOT.jar](../backend/target-be14/wms-backend-0.0.1-SNAPSHOT.jar),78526951 bytes, SHA `352211B7A38EFE0E80A034656B41D40CEEDDE0D988FE5AF130E56BF2C1FA13E5`. Logs/XMLs/manifestações/tentativas anteriores e JAR360 preservados fora de target; contratos27/29 e V1–V9 intactos no P2. Graphify atualizado após código e documentos. Nenhum SQL, fiscal/envio/cobrança reais, frontend, publicação, commit/push, rotina, perfil Hermes ou ETL executado.

Próximo passo: Lucas/TI/DBA, Caio/gestor, Natalina/Controladoria e Mickael/TI definirem insumos e autorizações dos ensaios externos do33. H2 não comprova SQL Server, o registro/demonstrativo local não comprova emissão/entrega externa e build não comprova piloto/operação.


**Preparo final e proveniência:** Vigia favorável às08:34 após P2 de identidade encerrado. [Correção e outputs](../infra/evidencias/d19-correcao-identidade-ciclos-2026-10-06.md):18/18 fixtures,306/306 diagnóstico,77/77 fecho e [manifesto488](../infra/evidencias/d19-correcao-identidade-ciclos-2026-10-06-fecho.sha256) conferido sem divergências. Histórico360 e atual367 têm fontes/JARs/resultados separados. Correção somente em contratos/leitores/metadados, sem mudar Java ou V1–V9 nem repetir build367. Esse parecer complementa a revisão de código07:58 e fecha o preparo local BE03/BE04/BE15; homologação externa permanece pendente por dono na matriz.
