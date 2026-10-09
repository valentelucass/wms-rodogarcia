# D29 — complemento local Cedro

Mesmo recorte autorizado por Lucas: conferir 498 e 14/0, completar somente lacunas locais. Nenhuma aplicação, SQL Server, H2, HTTP ou credencial será usada pelos novos testes. Produção e fontes D29 anteriores permanecem intactas.

Antes de editar: manifesto/hash de fontes, evidências antigas e builds anteriores; snapshot ZIP das fontes. Depois da formatação e antes do build: novo snapshot e manifesto. Depois do build: confronto dos preexistentes e snapshot final.

| Lacuna | Fonte / oráculo independente | Prova mínima nova | Limite |
| --- | --- | --- | --- |
| Permutação de três notas | Doc29 §rateio: total único e resíduo por ordem de ID. 100 centavos, cotas 1/1/1: IDs 11/22/33 recebem 33/33/34 centavos | Seis ordens do comando real de registro; registro e cálculo reais, repositórios simulados. Ordem de gravação variável, consulta ordenada conforme contrato do repository; valores conferidos por ID e soma | Não prova ORDER BY/dialeto/persistência/rollback SQL Server. O mock respeita o método OrderByNotaIdAsc; retorno artificial fora desse contrato não será chamado defeito |
| Corte mensal 1/0/32 | Doc29:90 admite 1..31; modalidade mensal exclui duração. 1 aceita; 0 recusa Min; 32 recusa Max | Configuração real por proxy de validação de método, sem contexto Spring/HTTP/DataSource. Recusas antes de qualquer interação nos colaboradores | Não prova resposta HTTP, integração SQL ou catálogo |

Não repetir duração 1/366, duração 0/367, corte 31, modalidade/campos exclusivos, F33/F34 ou parser já incluídos em 498 e 14/0. Builds 486/498 e focal novo serão relatados como execuções distintas.

Somente um novo teste `D29FechoLocalFinanceiroTest.java`, evidências `d29-fecho-local-cedro-*` e auxiliares/recibo `orchestracao/.runtime/d29-fecho-local-cedro.*`. Nenhuma produção será modificada sem defeito concreto/red.
