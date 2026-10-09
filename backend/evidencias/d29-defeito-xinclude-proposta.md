# D29 — red XInclude e proposta antes de alterar produção

Registro de 07/10/2026. Red atual: rodada D29EDB36B64, família cliente55/armazém26, caso `D29 XML400 XInclude`, esperado400/obtido201, pedido65 criado. JAR4304D4283BB21A0F80F3D6AD034F9891D560DFED754B6511E6BD89CDE8900CE1. UUID, RequestId, XML/hash/IDs e GET atual estão na evidência HTTP e no contrato SELECT desta rodada. Preservar pedido65 e a evidência; nenhuma limpeza ou reaplicação sobre ele.

Fonte normativa: docs/24-pedido-saida-fifo-e-reserva.md:101 declara que o parser recusa XInclude; quantidade/XML incompatível recebe400 na linha103. docs/18-recebimento-e-conferencia.md:98 também desabilita XInclude. Não há prova de leitura de arquivo ou rede; a falha demonstrada é aceitar o elemento proibido no documento.

Causa proposta: NfeXmlService configura `setXIncludeAware(false)`, impedindo expansão, mas a extração de filhos NF-e ignora o elemento estranho. A configuração sozinha não recusa o documento que contém XInclude.

Arquivo proposto: backend/src/main/java/br/com/rodogarcia/wms/services/NfeXmlService.java. Correção mínima: depois do parse protegido, rejeitar elementos no namespace oficial XInclude, independentemente do prefixo e da posição, usando a mesma resposta genérica400. Manter todos os limites atuais, MVC, properties, entidades e schema.

Red/green planejado: teste público do parser com include/fallback/prefixos alternativos e controles NF-e válidos; build novo em target-d29-xinclude, fontes/classes/JAR arquivados antes do HTTP; novo comando e identidade fictícia na mesma família para provar400 e ausência de novo pedido. Não reexecutar o comando concluído do pedido65. SELECT Prumo e GETs correntes antes do hold. Aprovação comercial/fiscal, acesso externo e witnessSQL não decorrem deste caso.

Errata factual posterior, 18:10Z: Prumo confirmou que o pedido criado pelo XInclude é66, UUID87cd908d-4638-4562-b0f7-1269a7ab011e; 65 é o positivo científico. O texto anterior identifica erroneamente65. Ambos ficam preservados. O runner green deriva o ID da resposta HTTP do red, sem índice fixo, e captura GET66 antes do novo hold. A proposta técnica e a prova400/201 permanecem iguais.
