# Execução contínua do backend — D19

Autorização expressa recebida de Hermes WMS em 05/10/2026: concluir todo o escopo **local** restante BE01–BE16, sem autorização por macrobloco e sem parar após BE10. Base D18: 197 testes aprovados, contrato 24 e evidência 25. Não renumerar IDs; [states.md](../states.md) continua sendo a trilha oficial.

## Ordem por dependências

| Sequência | Recorte | Dependência e resultado esperado | Documentos atribuídos |
| --- | --- | --- | --- |
| 1 | BE01/BE10/BE11 | BE09 disponível: leitura/separação, cobertura de documentos existentes, retirada física integral, cancelamento/retorno e avaria com responsabilidade; fatos para cálculo | Cedro contrato 27; Farol evidência 28 |
| 2 | BE01/BE05/BE12 | Fatos de permanência/retirada/avaria; completar importação Excel de endereços, complemento fiscal, serviços/tabelas/vigências e cálculos explicáveis | Cedro contrato 29; Farol evidência 30 |
| 3 | BE01/BE13 | Cálculo e lançamentos: ciclos sem duplicação, versões/ajustes, aprovação integral e demonstrativo manual ESL, sem emitir NFS-e | Cedro contrato 31, ampliável com BE14; Farol evidência 32 |
| 4 | BE01/BE14 e BE05 final | Contagem/carga inicial/contingência, indicadores, ajuste rastreável com reservas, inativação definitiva só sem compromissos | Cedro complementa contrato 31; Farol complementa evidência 32 |
| 5 | BE01/BE03/BE04/BE15/BE16 | Contratos integrados, cobertura de acesso, configuração/diagnóstico/recuperação preparada, revisão global, clean verify final e matriz BE01–BE16 | Prumo preparação 33; Farol matriz/evidência final 34 |

A ordem é técnica. O bloco seguinte já está autorizado. Revisão ou falha de teste exige correção, não nova autorização do usuário. SQL Server/provedor/preços reais não impedem código e procedimentos independentes; ficam identificados com dono e validação externa.

## Divisão de arquivos

- **Cedro:** escrita exclusiva em `backend/**`, contratos 27/29/31 e ajustes necessários nos contratos técnicos existentes que informar antes. Mantém MVC/properties/doc16; informa schema antes de implementar novas estruturas e solicita V6/V7/etc. a Prumo pelo Farol. Não escreve registros centrais, database, infra ou Graphify.
- **Prumo:** escrita exclusiva em `database/**`, `infra/**` e documento 33. Prepara migrations incrementais e procedimentos; não altera V1–V5 ou backend, não executa/conecta SQL Server. Alinha cada schema com Cedro/Farol antes de fechar a migration.
- **Vigia:** leitura apenas. Revisa cada bloco e o conjunto final, informando severidade, arquivo/linha, cenário e correção esperada; nenhuma edição ou build concorrente.
- **Lume:** apoio de contratos em leitura quando atribuído. Sem frontend, instalação ou build frontend.
- **Farol:** estados, decisões, continuidade, índice, plano 26, evidências 28/30/32, matriz 34, modelo integrado 35 e Graphify. Consolida somente resultados observados.

Durante revisão final de um bloco, Cedro mantém o código desse bloco estável até receber os achados. Leitura/planejamento do seguinte pode avançar. Escrita compartilhada só ocorre em sequência expressamente distribuída, nunca simultaneamente.

## Critérios locais

Serviços verificam perfil e escopos; confirmação revalida estado/saldo; operações têm chave idempotente e auditoria na mesma transação. Concorrência/rollback preservam reserva, unidade, origem, quantidade e histórico. Fiscal e físico permanecem fatos separados; pedido integral e parcial de pallet continuam coerentes. Cálculo registra quantidades, períodos, vigências, cortes, mínimo/GRIS configurados, responsabilidade de avaria e ajustes sem reescrever fechamento emitido. Contagem/contingência não duplicam efeitos nem editam silenciosamente notas.

Por bloco: testes relevantes com output fora de target, formatação e revisão em leitura, correção de achados e atualização de Graphify após Java. Ao final: clean verify completo, leitura dos XMLs/log, artefato e matriz de implementado/testado/pendente externo para cada BE01–BE16. Não declarar homologação, versão publicada ou ensaio externo pela aprovação em H2.

## Pontos de integração conferidos em leitura

Lume entregou parecer documental, sem arquivos ou testes. Regras/fontes permanecem nos documentos 10/11; os exemplos abaixo são critérios de desenho/teste fictícios, não aprovação comercial:

- BE08/BE10/BE11 precisam preservar fatos de início/fim de permanência e alterações da equivalência com instante. Separação libera posição física, mas mantém equivalência cobrável até retirada; duas permanências podem ser cobráveis na mesma data. Entrada/saída na mesma data é excluída do pico proposto em AC04.
- Vigência/corte usam intervalos sem sobreposição: uma posição em `[01/11,05/11)`, tarifa T1 até 03/11 e T2 desde 03/11, resulta em `2*T1 + 2*T2`. Configuração faltante é pendência, nunca zero ou preço Tigre. Mínimo complementa somente a diferença; GRIS exige base/percentual/periodicidade configurados.
- Avaria separa quantidade física, momento, responsabilidade e efeito financeiro aprovado. Vinte de cem unidades reconhecidas por responsabilidade Rodogarcia suspendem 20% da equivalência conforme AC08, não 20% da posição física. Período emitido só recebe ajuste identificado no próximo ciclo.
- Deduplicar serviço também pelo fato de negócio, não apenas UUID: sugestão e lançamento manual com chaves distintas não podem cobrar a mesma execução duas vezes. Discriminar parcelas por nota sem repetir o total.
- Aprovação, demonstrativo/entrega manual e referência NFS-e pertencem à versão correspondente do fechamento. Entrega ao ESL não comprova emissão; reabertura preserva versão e exige nova aprovação.
- Contagem abaixo da quantidade reservada exige bloqueio e conciliação explícita do compromisso, sem saldo negativo, atendimento parcial ou liberação silenciosa. Carga inicial incompleta preserva as ausências e fica impedida conforme o dado necessário; não inventar nota, FIFO ou valor para encaixá-la na entrada normal.
- Contingência deduplica a mesma linha/fato e preserva instante real e instante de conciliação. Não significa sincronização offline ou autorização para perder 36h de dados.
- AC12 requer uma operação identificada autorizada pelo gestor para resolver remanescente durante encerramento. A recusa geral de movimentos dos contratos anteriores deve ser conciliada por esse caminho explícito, sem desbloqueio geral. Inativação definitiva revalida saldo/pedidos/reservas/cobrança dentro da transação.

Fontes do parecer: AC04–AC15, respostas de cobrança/serviços/contagem, docs14/22/24 e cenários V35–V48. Apoio ligado a FE04/FE11/FE12 somente como contrato; frontend não iniciado.

## Limites e donos externos

| Item externo | Dono de referência | Trabalho local permitido |
| --- | --- | --- |
| Banco/alvo/credenciais/versão/collation, locks/permissões/restauração SQL Server | Lucas/TI e DBA | Migrations e plano de ensaio/recuperação em arquivos; H2 fictício |
| Provedor/contas/token/sessão e hospedagem | Equipe técnica com Lucas | Contratos, proteção JWT/configuração e procedimentos sem inventar fornecedor |
| Preços, mínimo, base/periodicidade GRIS e corte reais | Gestor/comercial | Parâmetros obrigatórios e testes fictícios; ausência gera pendência explícita |
| Documentos/enquadramento fiscal e procedimento NOTAZZ/ESL | Natalina/Controladoria | Registro/importação de documento existente e demonstrativo manual; sem emissão |
| Validação operacional, volumes e exemplos AC01–AC16 | Caio/operação e gestor | Jornadas/testes fictícios com propostas identificadas |
| Coletor, impressora, etiqueta e cobertura | Mickael/TI | Dados/contratos e ensaios futuros documentados |

Proibidos: conexão/aplicação SQL Server real, emissão fiscal/NFS-e, cobrança a clientes, publicação, commit/push, rotinas, alteração de perfis Hermes e qualquer uso do ETL. Usar somente identidade/ambiente Maestri do Farol. Se prompt estiver comprovadamente apenas colado, enviar somente Enter, sem duplicar tarefa. Segurança/privacidade nunca são aceitas via raw; bloqueio real deve ser informado ao usuário.

## Andamento observado

05/10/2026: AGENTS, states, continuidade, regras, doc16 e equipe conferidos; `maestri list` confirmou somente os destinos WMS esperados. Git `main` com zero commits e arquivos preexistentes não rastreados; baseline de 142 arquivos. `check` confirmou leitura real dos quatro especialistas. Pedidos apenas colados de Cedro/Prumo/Vigia receberam somente Enter; nenhuma confiança/privacidade foi aceita via raw. Lume concluiu o parecer em leitura; Cedro/Prumo/Vigia seguem no primeiro recorte. Sem novos testes declarados antes do output.


**Fecho local D19 em06/10/2026:** escopo BE01–BE16 entregue/testado/revisado após P2-locks:367/0/0/0,17 XMLs/JAR/289 hashes conferidos, Vigia favorável e V9/JPA904/904+129/129 em arquivos. Matriz [34](34-matriz-e-validacao-final-backend.md), evidência [32](32-validacao-fechamento-e-contingencia.md) e modelo [35](35-modelo-integrado-e-jornadas-backend.md) sincronizados; Graphify atualizado. Não é aprovação comercial/fiscal/homologação/piloto/publicação. AC01–AC16 e todos os limites/donos externos preservados. Sem nova autorização criada; conclui o trabalho local autorizado por D19.
