# D27 — SELECT focal após JAR final

JAR final D3380684642FF521627C48F3B35E3F8000425738E65E7AE6F11A3DC2B67C687E, 418/0/0/0. Sem novas migrations/grants/IT vazio. Todas as mutações abaixo ocorreram por HTTP WMSDEV/DEV.

## Ajuste AC13 e XML RN22

Retomada real [D27F7D4404E](d27-D27F7D4404E-http.json), concluída. Fonte inicial [D2763484C74](d27-D2763484C74-http.json) preserva 409 de ajuste zero e seus IDs/UUID; a retomada usa o mesmo UUID da aplicação que sofreu rollback. Cliente19/armazém9/produto8; unidade15 terminou35 ativa, unidade16 terminou0 inativa. Contagem3 revisão2 APPLICADA e contagem4 revisão1 APPLICADA (a grafia do enum real é APLICADA); três ajustes efetivos totais, incluindo45 inicial, zero e35 após reversão explícita da reserva. Pedido23 reservado40 e depois revertido; GET saldo35 físico/disponível, reservado0. Não se apagou ocupação: unidade16 sem associação ativa, marco histórico de localização preservado.

Solicita-se SELECT das unidades/ocupações preservadas, contagens/revisões, movimentos/fatos AJUSTE_ESTOQUE, operações UUID/idempotência e auditorias, pedido23/reservas revertidas e saldo. Conferir atomicidade das recusas de reserva ativa/revisão superada/delta inválido/perfil/alcance. Cálculo25 PENDENTE/totalNULL e GET/memória, sem reclassificá-lo como financeiro completo. XML pedidos19/20/21: origem/metadados, quantidade agregada5 cada, CRIACAO+XML_VINCULADO e ausência de reserva automática; fonte inicial contém negativas sem pedido/efeito extra.

## Financeiro fictício não zero

Fonte [D275A055528](d27-D275A055528-http.json): cliente21/armazém10, serviço8/tabela5/vínculo5/contrato6. Cálculos22 previsão07→08,23 completo06→07 e24 recálculo do mesmo corte. Total50 composto por subtotal0 + mínimo complementar50 + GRIS0; sem estoque/fatos retroativos/NFS-e. Fechamento14 inicialmente aprovado versão1 e15 em revisão futura com aprovação recusada por período não encerrado.

Está em execução complemento `financeiro-validar` no JAR final: GET23/14, replay e alcance, reabertura de14 via cálculo24 e aprovação da versão2, preservando memória da versão1 e recusando versão1 superada. Conferir o JSON novo quando concluído antes do SELECT final; não supor versão1 ainda atual.

## Listas

Fonte [D270A1D076A](d27-D270A1D076A-http.json): cliente23/armazém11, produtos10/11/12 e endereços28/29/30, páginas de conteúdo/ordem/isolamento. Solicita-se conferência limitada a esses IDs, sem ponte genérica certificar cada filtro/API.

## Concorrência

O observador Prumo revisto foi lido: aceita pedido público antes do gate e descobre SPIDs por appPID real. Helper irá criar família nova e publicar `d27-concorrencia-pedido.json` em aguardando-readiness; nenhum gate antes de sinal real atestado da mesma rodada. Solicita-se operar a janela de leitura a partir desse pedido, sem SQL texto de terceiros, grants ou API sa. O par200/409 só será aprovado como concorrente com witness SQL das duas requests/locks; a fixture ficará preservada.
