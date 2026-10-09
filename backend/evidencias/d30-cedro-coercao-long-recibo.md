D30 Cedro — coerção de Long sem truncamento

Red material focal12 preservado:1.9→1 e0.9→0 no mapper Boot e POST de encerramento de cliente, HTTP200/uma chamada ao serviço por caso;2/2/0/0, exit1. Nenhum datasource, HTTP externo ou SQL Server.

Correção concreta: JsonInteirosModule valida números destinados a Long/long por conversão decimal inteira exata e conserva o deserializador original para os demais tokens e comportamentos contextuais. Não altera BigDecimal/Integer/saída nem impõe string ou novo teto. Há259 fontes main Java atuais;258 anteriores preservadas.

Green focal13 somente os dois negativos novos:2/0/0/0, exit0; mapper recusa ambos, HTTP400 seguro e zero chamadas ao negócio. Mapper/configuração, DTO/controller/advice/filter reais; serviço mock. Não aceita todos os atributos/rotas ou integração nativa.

JSON A2E2D69C12AA152F0374D36AFB4270D50CCFC0B2B6BD21CB03E20074307F1C7F; freeze ABDC5FF03276A3C2B1F591C0E6B21D028164DEF7C56074E0FF26B89B16AE6C52. D20 38AD8D204C8D947E2DC76DCE2D26473D05EAF76A423A2EE9FFF9EF1E94038801 preservado. Regressão integral e JAR final ainda pendentes após esgotar a fila; positivos Long/Decimal não foram repetidos neste focal. A08 tipado, A10 e ramosv09 ainda sem execução, programados para FINAL.
