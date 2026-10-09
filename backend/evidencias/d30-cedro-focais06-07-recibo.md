# D30 Cedro — focais06/07

Execuções separadas: focal06 9/0/4/0, exit1; focal07 4/0/0/0, exit0. Fonte/classes/XML/log de cada execução preservados; nenhuma soma é total final.

D30-L-CASO-PAGINA-OFFSET-001: quatro vetores green no focal06. Tamanho0 e offset2147483700 recusam400/DADOS_INVALIDOS; offsets2147483647 e2147483600 aceitos exatamente, sem consulta nem alocação proporcional. Não repetir no focal07.

D30-L-CASO-JWT-POSITIVOS-001: duração900s, sujeito200 e listas500 strings com LongMAX aceitos; AcessoService conserva9223372036854775807 nas listas e alcance. Vizinho901s recusado no focal06. Os três positivos desse focal passavam o validador, mas a fixture usava construtor não autenticado de JwtAuthenticationToken; fixture corrigida e os três passaram no focal07. Sem mudança de autorização produtiva, assinatura nesse caso unitário ou tolerância de relógio integral.

D30-L-CASO-BEARER-COOKIE-001: cadeia produtiva real em MockServletContext/MockMvc, perfiltest, zero DataSource/JPA, RSA efêmero de IdentidadeTesteConfig, sem servidor/socket/JWKS externo. Cookie access_token sozinho401/NAO_AUTENTICADO e zero serviço; mesmo token em Bearer200, uma consulta e sem Set-Cookie. Red anterior decorreu de mapper standalone sem customização Boot para ProblemDetail; importar JacksonAutoConfiguration real corrigiu a evidência, sem mudar handler produtivo.

A08: captura temporal tipada está compilada em todas as fontes, mas não foi executada nestes focais. Seus oráculos e qualificação de fotos antigas estão em d30-cedro-a08-plano-qualificacao-antes.json; primeira prova prevista na regressão FINAL após últimas alterações/importações. Focal04 não repetido.

Históricos D29/default20/página vazia qualificados em d30-cedro-focal05-historicos-adenda-v01.json; nenhum recibo histórico reescrito. Zero ações SQL Server; main/pom/migrations preservados. Fila integral e final continuam em curso.
