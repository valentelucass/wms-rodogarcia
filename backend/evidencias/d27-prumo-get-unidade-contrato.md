# D27 — unidade GET é envelope, não id direto

SELECT final interrompido `D27_AJUSTES_UNIDADE_GET_AUSENTE`, checks0: o leitor procura resposta.id/pedidoId na fonte D27CC95FDEB. O contrato real GET `/unidades-logisticas/{codigo}` é Detalhe com `resposta.unidade` e `resposta.origens`; quantidade/ativa/versao/id/pedidoId estão em unidade. Isso não demonstra ausência de GET ou defeito de persistência.

Fonte final [D27CC95FDEB](d27-D27CC95FDEB-http.json): selecionar **último GET200 da rota canônica por código** e `resposta.unidade.id15/16`, cliente19/armazém9/pedido16, preservando origens fora da unidade. Não selecionar paginação vazia ou payload de ASSERT como HTTP. D27F7D4404E contém snapshots intermediários45; a fotografia final deve confrontar35/0 da fonte listas-estoque posterior. Preservar leitura interrompida sem refazer HTTP/mutações. Prumo corrige apenas seu leitor/oráculo.
