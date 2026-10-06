# Cenários de validação do WMS Rodogarcia

Este documento descreve verificações futuras em linguagem de negócio. Não contém testes executados. Os resultados baseados no PDF, nas respostas de 05/10 e nas interpretações/propostas estão identificados. Os cenários abaixo permitem validar resultados concretos, sem nova rodada extensa de perguntas.

## Regras do documento e interpretações do fluxo

| ID | Situação | Resultado esperado | Base |
| --- | --- | --- | --- |
| V01 | Importar uma nota antes da chegada da mercadoria. | Pedido preenchido; sem reconhecer recebimento físico apenas pela importação. | I01 |
| V02 | Conferir mil caixas e organizar dois pallets de quinhentas. | Duas unidades identificadas, mil caixas vinculadas ao recebimento. | RN15 e RN16 |
| V03 | Tentar misturar dois SKUs na mesma unidade logística. | Organização recusada na primeira versão. | RN15 |
| V04 | Somar quantidades unitizadas diferentes da quantidade conferida. | Diferença explicitada e unitização não concluída como correta. | RN16 |
| V05 | Consultar FIFO com unidades antigas em quarentena. | Unidades bloqueadas não são sugeridas nem reservadas. | RN17 e RN23 |
| V06 | Apenas visualizar uma sugestão FIFO. | A visualização não cria reserva. | RN24 |
| V07 | Confirmar reserva de três das dez bobinas fisicamente presentes. | Dez presentes e três reservadas; sete disponíveis se não houver outros impedimentos. | RN24 e I02 |
| V08 | Mover uma unidade para outro endereço. | Identidade e etiqueta preservadas; localização e histórico atualizados. | RN10, RN11 e RN19 |
| V09 | Reimprimir uma etiqueta. | Mesma identidade e data de entrada; sem criar saldo adicional. | I03 |
| V10 | Receber rejeição fiscal durante a saída. | Nenhuma baixa definitiva concluída por essa emissão. | RN26 |
| V11 | Ter nota autorizada e mercadoria ainda aguardando retirada. | Mercadoria permanece fisicamente presente até a confirmação da retirada. | I06 e proposta AC01 |
| V12 | Fechar o mês com unidades que já saíram durante o período. | Histórico preserva os fatos necessários à cobrança e sua explicação. | RN27 e I07 |

## Proteções propostas para a arquitetura

| ID | Situação | Resultado a validar | Proposta |
| --- | --- | --- | --- |
| V13 | Dois operadores confirmam reserva da mesma unidade ao mesmo tempo. | Uma reserva válida; a outra tentativa recebe conflito claro. | PR01 |
| V14 | Repetir a confirmação após uma resposta perdida. | Mesmo fato não gera segunda reserva, baixa ou cobrança. | PR02 |
| V15 | Falhar a gravação durante uma movimentação ou reserva. | Nenhum conjunto incompleto de alterações fica confirmado. | P04 |
| V16 | Tentar atuar sobre mercadoria de outro cliente ou armazém sem autorização. | Operação impedida no backend, independentemente da tela. | PR06 |
| V17 | Detectar avaria depois de reservar uma unidade. | Expedição bloqueada e pedido sinalizado; sem disponibilidade indevida. | PR11 |
| V18 | Receber atraso ou perda de resposta do emissor fiscal. | Pendência identificável e resultado consultado antes de repetir emissão equivalente. | PR10 |
| V19 | Processamento de cobrança ficar pendente e executar novamente. | Período recuperado com a regra aplicável, sem duplicar o mesmo fato cobrável. | PR02 e PR09 |
| V20 | Alterar uma tabela com início de nova vigência. | Histórico de cálculo preservado e nova vigência aplicada segundo o contrato confirmado. | RN08 e PR09 |
| V21 | Corrigir uma quantidade registrada incorretamente. | Autor, motivo e vínculo com o registro corrigido ficam rastreáveis. | PR04 |
| V22 | Restaurar uma cópia de segurança em ambiente de validação. | Dados e continuidade recuperáveis conforme metas que ainda serão definidas. | P07 |
| V23 | Ler uma unidade ou endereço inválido no coletor. | Mensagem compreensível e nenhuma movimentação indevida. | RN18 e PR07 |

## Resultados consolidados para validação

| ID | Exemplo | Resultado esperado | Base |
| --- | --- | --- | --- |
| V24 | Retirar 100 de um pallet de 500. | Pedido de 100 concluído; restante 400, origem preservada e etiqueta atualizada. | Q02/Q07; proposta AC10 |
| V25 | Reagrupar mesmo SKU com lote, data ou nota diferentes. | Lote/data diferentes impedem; mesma nota também exigida na proteção proposta. | Q02/Q21; proposta AC10 |
| V26 | Receber uma nota em dois dias. | Um pedido, chegadas registradas e efetivação única; FIFO da primeira chegada. | Q03/Q21; AC03 |
| V27 | Receber 98 ou 102 frente a 100 previstas. | Carga bloqueada; supervisor registra solução com cliente, quantidade real e diferença. | Q04; proposta AC03 |
| V28 | Cinco avariadas em carga de 100. | Toda carga em quarentena até solução, sem liberação automática das 95. | Q04; AC03 |
| V29 | Reservar unidade ainda na triagem. | Recusar até liberação e endereçamento. | Q05 e RN21 |
| V30 | Unidade grande ocupar duas posições; outra tentar usar uma delas. | As duas posições são ocupadas juntas; impedir sobreposição. Duas posições cobráveis propostas. | Q06; proposta AC05 |
| V31 | Atender oito das dez unidades pedidas. | Não concluir parcialmente; cancelar e criar pedido correto com efeitos fiscais tratados. | Q07/Q22 |
| V32 | Pedido urgente disputar reserva antiga. | Reserva não vence nem muda automaticamente; cancelar o original e criar novo atendimento. | Q08/Q09; AC11 |
| V33 | Cancelar após separar ou receber mercadoria devolvida. | Retorno interno à posição sem nova conferência; retorno externo por nova entrada, FIFO original. | Q10/Q21 |
| V34 | Registrar XML já emitido no NOTAZZ. | Vincular documento existente sem emitir novamente; conferir cobertura integral do pedido. | Q13; proposta AC02 |
| V35 | Entrar/sair no mesmo dia, trocar endereço ou usar duas posições. | Aplicar casos documentais de AC04/AC05 e conferir demonstrativo antes de uso real. | Q14-Q16; proposta AC04/AC05 |
| V36 | Corrigir cobrança antes ou depois da NFS-e. | Antes: reabrir e aprovar novamente; depois: ajuste no próximo ciclo, com origem. | Q17; proposta AC07 |
| V37 | Retirar parte ou identificar avaria. | Valor proporcional à quantidade remanescente; avaria excluída do indicador, preservada fisicamente. | Q18; proposta AC08 |
| V38 | Conferir depois da chegada ou receber nota em partes. | FIFO usa primeira chegada; início da cobrança usa endereçamento, datas distintas. | Q21/RN21 |
| V39 | Emitir segunda e retirar terça. | Manter presença/reserva até terça; supervisor/gestor confirma com XML. | Conciliação proposta AC01 |
| V40 | Executar serviço sem saída criada. | Registrar agora com origem e entrada; vincular futura saída sem duplicar cobrança. | Conciliação proposta AC09 |
| V41 | Pedir inativação com saldo existente. | Encerramento pendente; concluir compromissos e então inativar definitivamente. | Conciliação proposta AC12 |
| V42 | Operar piloto em Osasco. | Caio acompanha; verificar quatro simultâneos estimados e carga de teste acordada após levantamento. | Q25; AC16 |
| V43 | Falhar coletor, etiqueta ou rede. | Reimprimir etiqueta; falha isolada aguarda coletor; falha geral aciona planilha coordenada. | Q26/Q27; AC14 |
| V44 | Recuperar após indisponibilidade. | Conciliar planilha sem duplicidade e testar restauração; 36h não equivalem a perda permitida. | Q27; AC14/AC15 |
| V45 | Período de corte do dia 24 ao próximo dia 24. | Primeiro 24 incluído, próximo excluído; segundo ciclo inicia nesse dia, sem repetição. | Convenção proposta AC07 |
| V46 | Pallet com 20% avariado por responsabilidade da Rodogarcia. | Suspender 20% da equivalência cobrável desde ocorrência reconhecida, mantendo ocupação física. | Proposta AC08 |
| V47 | Serviço sugerido automaticamente e lançado manualmente. | Um único registro do serviço; não cobrar duas vezes o mesmo fato. | Proposta AC09 |
| V48 | Mudar tarifa durante permanência. | Aplicar tarifa antiga aos dias anteriores e nova desde vigência, preservando memória. | Q15.f; AC04/AC06 |

## Como registrar a execução futura

Para cada cenário, anotar ambiente, data, executor, dados de exemplo, resultado observado e evidência. Uma simulação documental não equivale a um teste no sistema. Nos cenários classificados como proposta, registrar a aceitação ou o ajuste do resultado com os responsáveis antes do uso real. Um exemplo documental não é homologação comercial nem teste executado.
