D30 Cedro — escopos JPQL locais

Sete consultas foram traduzidas pelo HQL/SQM real do Hibernate 7.4.5.Final, com 64 entidades de metadados e ConnectionProvider que recusa conexão. Foram ligadas 34 ocorrências a raízes distintas de subqueries. Zero tentativas JDBC e zero consultas executadas.

ContagemEstoque.c.unidade.id aponta para contagem_estoque.unidade_id → unidade_logistica.id; c.impedimento para contagem_estoque.impedimento. ConjuntoPosicoes.c.situacao aponta para conjunto_posicoes.situacao. No filtrarEstoque, o outro c é CargaInicial: c.situacao → carga_inicial.situacao e c.entrada.id → carga_inicial.entrada_id → entrada_conferida.id. O oráculo literal foi registrado antes do retorno.

Recibo JSON: backend/evidencias/d30-cedro-jpql-escopos-recibo.json; SHA 50D82748757491D1C4F1CD84AC5D4F9ED7E7A9694EAFE4562BB314EEBB6A2FA8. Cada query conserva seus IDs317 afetados. Esta ligação não prova execução/materialização, filtros nativos, locks, atomicidade ou os requisitos pais.

Prumo final v4 e doc35 atual foram importados em backend/evidencias/d30-cedro-importacao-prumo-final-v4.json; SHA 5998D6865D80861EB1909DDE4CC5BD9EC1655E297B0B80AE78498F0648CF5C09. As 23 razões sem prova de regra local e os sete mecanismos aritméticos permanecem individualizados na mesma fila local, sem bloqueio SQL artificial. A09 conserva 189 entradas: 168 métodos repository e 21 getters de projeção.
