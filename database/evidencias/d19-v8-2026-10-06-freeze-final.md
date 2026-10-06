# V8 — reconciliação do freeze histórico251/328 — 06/10/2026

**Compatível estruturalmente em leitura com o freeze histórico251 e doc31 SHA03005CFBCCB039A057FCC8609ABE4AD8CBBB6AADDA51A5A39DC82ACBA30DA8F2:490/490 mais132/132. Aceite328 retido por Farol devido P2 de ajustes ORIGINADOS.** Não é aceite BE13/negócio ou homologação SQL Server.

[Leitor490 integral](d19-v8-2026-10-06-freeze-final-leitor.json) e [execução](d19-v8-2026-10-06-freeze-final-leitor-execucao.json):17 models/114 colunas (108 novas mais seis situações herdadas),11 tabelas/24 FKs/10 unicidades/22 CHECKs/15 índices mais seis CHECKs INATIVO/VARCHAR24. [Suplemento132](d19-v8-2026-10-06-freeze-final-suplementar-final.json) e [execução](d19-v8-2026-10-06-freeze-final-suplementar-final-execucao.json):82 atributos imutáveis,24 relações/optional/NULL e presença lexical de guardas de contexto/aplicação/INATIVO. Nenhum @Check nos onze models novos: estes CHECKs estão no SQL V8 e não são comprovados pelo schema H2.

Inventário:26 gravações administrativas/23 tipos em24 permitidos; dez pares BE13 auditados com FECHAMENTO_COBRANCA, incluindo TRATATIVA_EXTERNA. Oito ações passam por concluir(String tipo), cuja assinatura/cobertura/repasse sem transformação foram conferidos; duas gravações diretas. Dez unicidades têm nomes SQL locais diferentes dos JPA, explícitos em nomeJpa; quantidade/colunas/chaves iguais, sem renomear SQL. FK aplicado_versao_id opcional, múltiplas referências NFS-e sem UNIQUE por versão e mutabilidade do destino somente VALIDADO foram lidas. FKs isoladas não garantem contexto; guardas em trechos não provam comportamento financeiro.

[Primeiro output471/469](d19-v8-2026-10-06-freeze-final-leitor-inicial.json) conserva24 divergências (22 JPA e duas coberturas): parser anterior não seguia CadastroBase/inicialização de enum, repasse comum ou nomes locais de unicidades. [Leitor anterior](d19-v8-2026-10-06-freeze-final-leitor-antes.ps1) e [transcrição anterior](d19-v8-2026-10-06-freeze-final-transcricao-antes.json) preservados antes do fortalecimento. Suplemento132 anterior e script antes de corrigir só o texto de natureza para ASCII também preservados. Invocações finais PowerShell normais, LASTEXITCODE=null; não declarar saída nativa0.

[Antes](d19-v8-2026-10-06-freeze-final-antes.json) conferiu251 hashes iguais ao manifesto D249C81AC84EC02904842F15E3FE74D6CAA42B0668531FDBBD3F68115F36222A. Na conferência intermediária, após os leitores,251 ainda iguais. [Depois](d19-v8-2026-10-06-freeze-final-depois.json) observa245 iguais/seis alterados por Cedro/contrato durante o início da correção P2: DTO FechamentoCobrancaDto, models AjusteFechamento/TratativaExternaFechamento, AjusteFechamentoRepository, FechamentoCobrancaService e doc31. states também mudou por Farol. Essas alterações não foram produzidas por Prumo e os outputs490/132 não descrevem o código corrigido ainda sem freeze.101 evidências históricas da área preservadas; V1–V8 SQL intactas nesta reconciliação.

O novo P2 permite trocar a base financeira depois de deltas ORIGINADOS em outro ciclo; recebidos já possuem guardas. Farol reteve o aceite e Cedro formalizou complemento p2-origem antes do Java. Esta evidência permanece histórica; preparação do complemento terá arquivos/cópias/output separados e JPA final somente após novo freeze. Não usar490/132 para aceitar a correção posterior.

| Arquivo comparado | SHA-256 |
| --- | --- |
| V8 SQL histórica | 98240E965B726E9C65A55F63A21A5EDC561BCB4EAC2E8F5C7FC09D38E85A089F |
| Transcrição pareada | 6A945E6DBCC3DF098A26A057A882E4DF6C66056109F7881491DC0AB74DCA317E |
| Leitor pareado | 3CA0B7B0D6691AA64776F64A850F1235AA686BF827C3F9C3CA31FF278AD8C00A |
| Suplemento final | CBCC1D7F97ADE60210174906722B23E38596AD3E37E99DF018148CAF024BA7A1 |

[Metadados reais](d19-v8-2026-10-06-freeze-final-metadados.json), [fonte031 copiada](d19-v8-2026-10-06-freeze-final-fonte-doc31.md) e [comparação de fontes](d19-v8-2026-10-06-freeze-final-fonte-comparacao.json). Build328/15XML foi informado por Farol/entrega Cedro, não executado por Prumo. Leitura/anotações/regex não validam dialeto, SQL emitido, locks, concorrência, permissões reais, recuperação ou regra de negócio. Nenhuma conexão/Info/Validate/SQL/JVM/Hibernate/H2/build/git/fiscal real/ETL/frontend/Graphify.

CLI Maestri próprio indisponível; reportar neste ask/arquivos para check de WMS - Farol, sem identidade/env alheios ou raw. Aguardar novo freeze para reconferir a correção; BE14/V9 depende de schema formal.
