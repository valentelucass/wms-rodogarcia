# Mesma D29 — proposta local PRIO06 antes da edição

Escopo autorizado: somente acrescentar testes em `backend/src/test/java/br/com/rodogarcia/wms/D29FinanceiroRetomadaTest.java`, conservando F33/F34, XML e o banco H2 exclusivo `wms-d29-retomada`. Nenhuma mudança de Java de negócio, SQL Server, API DEV, frontend ou arquivo ativo da sessão original.

Confronto: Lume PRIO06 aponta limites públicos ainda sem prova. O DTO `ConfiguracaoCobrancaDto.ConfigurarContrato` aceita `duracaoDias` 1..366 e `diaCorte` 1..31. O serviço exige somente o campo correspondente à modalidade. O controller de configuração retorna HTTP 200; DTO e `CadastroSupport.invalido` retornam 400 `DADOS_INVALIDOS`. Os testes de configuração/cobrança/fechamento consultados não demonstram 1/366/367 nem todas as recusas por exclusão/ausência de campo. MES31 já tem prova local de calendário, mas será o controle público positivo desta matriz, sem repetir jornada DEV.

| Modalidade | diaCorte | duracaoDias | Esperado independente |
| --- | ---: | ---: | --- |
| DIAS_CORRIDOS | ausente | 1 | HTTP 200, campos persistidos e consultáveis; replay sem novo efeito |
| DIAS_CORRIDOS | ausente | 366 | HTTP 200, campos persistidos e consultáveis; replay sem novo efeito |
| MES_DIA_FIXO | 31 | ausente | HTTP 200, campos persistidos e consultáveis; replay sem novo efeito |
| DIAS_CORRIDOS | ausente | 0 | HTTP 400 DADOS_INVALIDOS, campo duracaoDias/Min |
| DIAS_CORRIDOS | ausente | 367 | HTTP 400 DADOS_INVALIDOS, campo duracaoDias/Max |
| MES_DIA_FIXO | 31 | 1 | HTTP 400 DADOS_INVALIDOS, exclusão entre campos |
| DIAS_CORRIDOS | 31 | 1 | HTTP 400 DADOS_INVALIDOS, exclusão entre campos |
| MES_DIA_FIXO | ausente | ausente | HTTP 400 DADOS_INVALIDOS, diaCorte obrigatório nesta modalidade |
| DIAS_CORRIDOS | ausente | ausente | HTTP 400 DADOS_INVALIDOS, duracaoDias obrigatória nesta modalidade |

Cada invocação recebe contexto fictício exclusivo no mesmo H2. Antes de cada recusa haverá contrato válido existente e snapshot integral de contratos, serviços do mínimo, operações e auditorias. Logo após a resposta, antes de outra escrita, serão confrontadas as mesmas linhas e a ausência do UUID recusado. O controle válido evita atribuir o 400 a uma fixture inválida; o código/campo ou mensagem da recusa distingue os predicados públicos.

Validação prevista: formatar somente a classe própria, congelar as fontes atuais antes do build em evidência nova, e `clean verify` correspondente somente às duas classes Retomada no `target-d29-retomada` exclusivo. Os cinco testcases anteriores permanecerão; nove invocações PRIO06 serão acrescentadas. Preservar recibo/XML/JAR 5/0 e os 26 XMLs/JAR 486/0 originais, sem executar novamente a suíte original. Se houver red, classificar concretamente antes de qualquer correção; não alterar negócio nem flexibilizar o esperado para obter verde.

Após conferir fontes, resultados e recursos próprios, atualizar o checkpoint Cedro e encaminhar a Farol para revisão Vigia. Prova H2/HTTP local, sem equivalência automática com SQL Server, corte real 03Z, fiscalização, cobrança ou homologação comercial. Recibo geral continua somente Farol; sem callback Hermes/ETL.
