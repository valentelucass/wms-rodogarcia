# Esquema local da importação de NF-e

Pacote congelado `PL_010b_NT2025_002_v1.30`, obtido em 10/10/2026 na [fonte oficial SVRS](https://dfe-portal.svrs.rs.gov.br/NFe/Documentos). [ZIP original](https://dfe-portal.svrs.rs.gov.br/NFE/DownloadArquivoEstatico/?sistema=NFE&tipoArquivo=2&nomeArquivo=PL_010b_NT2025_002_v1.30.zip), SHA-256 `2deaa8d430d0acb47deae06b9b6d1202dbb436baf3d2d3aa834a534a3d6fa1b8`.

Os cinco arquivos oficiais foram preservados byte a byte. `wms_procNFe_v4.00.xsd` é somente o ponto de entrada local de `nfeProc`, com os tipos do pacote. O mesmo conjunto aceita `NFe`. Inclui `IBSCBS` e `IBSCBSTot`; suporte à estrutura não calcula tributos nem valida regras fiscais externas.

O resolvedor Java só permite os seis nomes desta pasta. Nenhum download de esquema, DTD, entidade externa ou XInclude durante a leitura. Limites: 1.000.000 bytes UTF-8, profundidade 64 e 200 itens por nota. Layouts novos exigem atualizar deliberadamente o pacote e seus testes; nunca remover a validação para aceitar um arquivo.

O XSD verifica estrutura e tipos; não verifica criptografia da assinatura, validade atual, cancelamentos ou autenticidade perante SEFAZ. O protocolo é informação do arquivo, não prova de chegada física. A configuração `wms.recebimento.xml.ambiente` aceita `1` (padrão, produção fiscal) ou `2` (homologação fiscal), independentemente do banco DEV/PROD.
