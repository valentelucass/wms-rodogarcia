# Integração Notazz para retirada fiscal da mercadoria

A tela Fiscal e retirada reúne a conferência do pedido e a emissão de NF-e. O emitente é o estabelecimento do armazém da entrada e o destinatário é o proprietário da mercadoria. Os documentos fiscais e a movimentação física têm confirmações próprias. Esta implementação foi autorizada em 23/10/2026, nas etapas BE10-NOTAZZ-IMP01 e FE10-NOTAZZ-IMP01.

## Funcionamento da tela

A lista consulta os pedidos do cliente e armazém selecionados, com número, data, cliente, situação fiscal e números das notas. Os filtros e a paginação são aplicados pelo servidor. PENDENTE indica que a documentação da operação ainda não foi concluída; FINALIZADO indica a cobertura fiscal concluída, sem significar que a mercadoria já foi retirada.

Supervisor e Gestor podem conferir uma prévia, verificar as origens, valores, emitente e destinatário e confirmar a emissão. O sistema mostra pendências de cadastro e não envia uma nota incompleta. Operação pode consultar. Os campos fiscais continuam sendo mantidos pelo Gestor nos cadastros existentes.

O agrupamento é definido por cliente: CONSOLIDAR reúne origens distintas em uma nota; POR_NOTA separa a emissão conforme cada nota de entrada. Produtos iguais com valores de origem diferentes continuam em linhas distintas. Retiradas parciais usam o valor proporcional da entrada, com compensação dos centavos nas parcelas sucessivas. As observações incluem a referência do pedido de saída do WMS.

Uma confirmação cria primeiro uma solicitação durável. A consulta recupera sua mesma identidade quando a comunicação falha. A ausência de resposta não autoriza criar outra nota. Uma nota somente fica AUTORIZADA no WMS depois da consulta e conferência do XML: empresa, proprietário, ambiente, chave, protocolo, referências, operação, produtos, unidades, NCM, CFOP, quantidades e valores por origem. O número sozinho não comprova autorização.

## Cancelamento e retorno

O cancelamento exige confirmação do evento fiscal e não movimenta estoque. Retirada já registrada ou prazo superior a 24 horas encaminham a regularização por nova entrada. A resposta final do responsável determina nova nota de entrada quando a circulação impedir o cancelamento, inclusive dentro de 24 horas. A saída original permanece no histórico.

O fiscal emite a nova entrada vinculada ao documento de saída. Depois o operador processa o retorno: mercadoria que efetivamente saiu gera o recebimento de uma nova entrada; mercadoria ainda presente é reendereçada sem duplicar a quantidade. Quantidades e valores devem corresponder às parcelas da saída. A mesma nota de entrada não pode regularizar o estoque duas vezes.

Retorno simbólico pode ser exclusivamente fiscal. Sua autorização não libera uma retirada física sem a documentação e a confirmação próprias dessa retirada.

## Dados e configuração necessários

Cliente e armazém precisam de CNPJ/CPF conforme o cadastro, razão social, endereço, cidade, UF, CEP e código IBGE. O cliente também precisa do indicador de inscrição estadual, da IE quando contribuinte e do critério de agrupamento. Cada produto precisa de referência fiscal conferida para o armazém e operação, NCM, CFOP e tributação. A origem precisa de chave válida e valor da mercadoria. Campos ausentes bloqueiam a emissão; não recebem valores fiscais presumidos.

A configuração protegida por armazém vincula CNPJ, chave de API, ambiente e regras do estabelecimento conferidas pelo fiscal. A conta Notazz precisa estar preparada para homologação e a empresa emissora precisa corresponder ao armazém. A conferência deve incluir certificado, inscrição, regime e tratamento tributário efetivamente exigido. Chaves e configurações sensíveis não pertencem ao navegador, documentação ou repositório.

Exemplo de nomes de propriedades para um arquivo externo protegido, sem credenciais reais:

```properties
wms.notazz.enabled=false
wms.notazz.production-enabled=false
wms.notazz.timeout-seconds=12
wms.notazz.emitentes[46].cnpj=CNPJ_DO_ARMAZEM
wms.notazz.emitentes[46].api-key=CHAVE_CONFIGURADA_NO_CANAL_PROTEGIDO
wms.notazz.emitentes[46].api-key-sha256-conferida=FINGERPRINT_DA_CHAVE_CONFERIDA_PARA_ESTE_CNPJ
wms.notazz.emitentes[46].ambiente=2
wms.notazz.emitentes[46].configuracao-conferida=false
wms.notazz.emitentes[46].finalidade=FINALIDADE_CONFERIDA
wms.notazz.emitentes[46].natureza-operacao=NATUREZA_CONFERIDA
wms.notazz.emitentes[46].finalidade-entrada=FINALIDADE_DE_ENTRADA_CONFERIDA
wms.notazz.emitentes[46].natureza-entrada=NATUREZA_DE_ENTRADA_CONFERIDA
wms.notazz.emitentes[46].documento-configuracao=JSON_CONFERIDO_DO_ESTABELECIMENTO
```

Não habilitar emissão de produção como forma de testar. `ambiente=2` deve ser compatível com a empresa configurada na Notazz; a conferência do XML é uma proteção de conciliação, não uma garantia prévia sobre a configuração externa da conta.

**NOTAZZ-HOM01, 23/10/2026:** somente DALGA SPO / Osasco (04.547.874/0001-14) pode ser usada nos testes. A lista permitida de homologação inicia somente com esse CNPJ e o transporte exige correspondência com a impressão SHA-256 da chave previamente conferida. Castro e Agudos estão em produção; Castro também é a empresa padrão do portal. Não usar seleção implícita, suas chaves, configurações ou documentos nos testes. A leitura do portal e uma consulta sem documento existente confirmaram o acesso de Osasco, sem emissão/cancelamento. O cadastro geral tem parâmetros de VENDA/NF-e normal: os dados do retorno precisam ser enviados explicitamente e conferidos por operação/produto. Esses parâmetros gerais não constituem aprovação fiscal do retorno.

## Estrutura e ativação no desenvolvimento

V15 acrescenta os campos fiscais e quatro tabelas: solicitação, parcelas de origem, eventos e controle de chamadas por emitente/ambiente. O conteúdo confirmado e a identidade da solicitação são imutáveis para a aplicação. Não há exclusão de histórico. A migração não contém dados fictícios nem alteração de notas existentes.

`infra/notazz/contrato-banco.ps1` define o catálogo e os direitos mínimos esperados. `infra/notazz/ativar-dev.ps1` exige guarda atual aprovada, hash do recibo, histórico e direitos anteriores idênticos, certificado/processo SQL atuais e canal administrativo protegido. O modo Plan não abre o canal administrativo. Apply usa Flyway limitado à V15, confere o resultado e concede apenas SELECT/INSERT e UPDATE das colunas mutáveis necessárias, exclusivamente no WMS_DEV.

A guarda normal exige V15 depois da ativação. A opção `NotazzPreMigration` compara exatamente a estrutura V14 antes da aplicação; não serve para iniciar a nova aplicação. O BAT continua preparando e comprovando a fonte corrente antes de substituir somente os processos próprios do WMS DEV. A aplicação continua usando WMSDEV, TLS obrigatório, validação de esquema, sem inicialização ou migrações automáticas.

A conferência estrutural do preparador reconhece a V15 somente pelo hash revisado. No replay local, remove os delimitadores de transação e explicita `IDENTITY(1,1)`, preservando a semântica; o Flyway executa a fonte original sem conversão. O plano de permissões DEV reconhece a nova estrutura e preserva os direitos mínimos anteriores. `infra/notazz/testar-contrato.ps1` verifica o contrato e a compatibilidade estrutural em memória; isso não comprova execução SQL. A ativação de PROD permanece fora desta entrega.

O iniciador usa `infra/dev/compilar-backend-protegido.ps1` para executar somente a compilação no controlador compartilhado de memória. Servidores permanentes continuam no processo do iniciador, e não no batch temporário. O compilador integra o fingerprint obrigatório conferido também pelo frontend. O BAT limpa `PSModulePath` somente no seu ambiente local para o Windows PowerShell carregar seus módulos nativos, sem modificar o ambiente global ou o login. Diagnósticos de abertura registram códigos e tipos de falha, sem mensagem bruta ou credenciais.

## Validação e limites da entrega

As evidências desta implementação ficam em `orchestracao/.runtime/notazz-implementation-20261023/`. Compilação, testes puros, contratos, interface, integração SQL e emissão externa são verificações diferentes. Os testes de transporte utilizam cliente HTTP controlado e não emitem documentos reais. Nenhuma prova histórica dos levantamentos 46–49 vale como execução nova.

A emissão autenticada na Notazz, o XML real de autorização e cancelamento e a validação tributária final dependem da configuração protegida e da homologação da empresa. Uma solicitação rejeitada é preservada; a correção e retransmissão fiscal no emissor devem ser conciliadas pela mesma identidade. Não há exclusão remota, recriação automática de uma solicitação sem resposta nem recebimento de callbacks sem autenticação.

Referências: [mapeamento técnico](46-mapeamento-fiscal-notazz.md), [regras e decisões](47-regras-de-negocio-fiscal-notazz.md), [cenários de validação](48-cenarios-e-validacao-fiscal-notazz.md), [respostas e exemplos](49-alinhamento-fiscal-respostas-e-exemplos.md) e [documentação oficial da API](https://app.notazz.com/docs/api/#nfe-criar).
