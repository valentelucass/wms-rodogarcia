# Validação das unidades logísticas

Entrega D16 de 05/10/2026. Escopo e escolhas em [unidades logísticas e etiquetas](20-unidades-logisticas-e-etiquetas.md). Executado localmente em Windows, JDK 21.0.12.1 e Maven Wrapper 3.9.16; HTTP real, JPA/H2 isolado e JWT assinado com chave efêmera.

## Construção e testes

Na pasta `backend`, com `JAVA_HOME` no JDK 21:

```powershell
./mvnw.cmd -B -ntp spotless:apply
./mvnw.cmd -B -ntp clean verify
```

**BUILD SUCCESS: 122 testes, zero falhas, zero erros e zero ignorados.** Verificação final concluída em 05/10/2026 às 20:23:25 (-03), já com as colunas originais da entrada conferida marcadas sem atualização. Passaram Enforcer, Spotless, compilação, arquitetura, testes e empacotamento.

| Grupo | Quantidade | Evidência |
| --- | --- | --- |
| Base, cadastros, configuração, erros e arquitetura | 52 | Suíte anterior preservada; fronteiras MVC também verificam as novas classes |
| Recebimento e segurança XML | 34 | 28 cenários de recebimento e seis do extrator, descritos no documento 19 |
| [UnidadeLogisticaIntegrationTest](../backend/src/test/java/br/com/rodogarcia/wms/UnidadeLogisticaIntegrationTest.java) | 36 | Unitização, etiquetas, transformações, conservação, escopos, repetição, concorrência e rollback |

Resultados observados:

- Distribuir oito unidades boas e duas avariadas fecha exatamente com a entrada. DUN/quantidade por DUN continuam separados da quantidade atual e cada unidade recebe UUID distinto.
- Quantidade faltante, excedente, negativa, zero, fração incompatível e conversão de avaria em quantidade boa são recusadas sem identidades/composição parciais.
- Não é possível unitizar antes da efetivação, reutilizar a mesma entrada ou usar entrada/embalagem de origem incompatível. Campos adicionais não aceitos são rejeitados.
- Pedidos com duas entradas mostram progresso parcial após organizar somente uma; a conclusão exige todas. A condição `disponivelParaSaida` permanece falsa.
- Leitura e repetição da etiqueta mantêm identidade, data, quantidade, versão do pedido e número de auditorias. A etiqueta não contém endereço e expõe a quantidade atual.
- Dividir dez em seis e quatro preserva UUID/FIFO do remanescente e gera apenas um UUID novo. Produto medido aceita 0,125 na precisão três, recusa 0,0001 e conserva 1,250 sem arredondar.
- Reagrupar duas entradas compatíveis, depois dividir a unidade resultante, conserva as quantidades de cada entrada original. Identidades consumidas ficam com zero, consultáveis e sem nova etiqueta/divisão.
- Reagrupamento recusa produto, nota, lote, validade, chegada real, embalagem, tipo ou condição diferentes. Parcelas da mesma nota em dias diferentes são recusadas mesmo com FIFO igual.
- Origem repetida, destino usado como origem e unidade de outro pedido são recusados.
- Operação unitiza/consulta, mas não divide/reagrupa. Falta de alcance sobre cliente **ou** armazém bloqueia lista, progresso, leitura, etiqueta e repetição. Chamada direta ao serviço também valida dados/perfil.
- Embalagem em encerramento impede nova transformação, preservando consulta e dados da etiqueta existente.
- Repetição idêntica devolve a confirmação original mesmo após transformação posterior, sem criar outros fatos. Outra intenção com a mesma chave retorna conflito.
- Duas unitizações simultâneas com a mesma chave devolvem sucesso com um único efeito; chaves distintas com a mesma revisão produzem um sucesso e um conflito.
- Duas divisões com a mesma revisão não perdem quantidade. Dois reagrupamentos disputando a mesma origem consomem essa identidade uma única vez.
- Falha forçada na auditoria da criação reverte unidades, composição, marcador e revisão. Falha no registro de repetição da divisão também reverte a auditoria. Falha no reagrupamento mantém quantidades, vínculos, identidades e versões; repetir após retirar a falha de teste conclui.
- Uma confirmação com 100 unidades mantém evento de auditoria completo, sem truncamento. JSON da auditoria foi alinhado ao `nvarchar(max)` já preparado em V1.
- Paginação e leitura de códigos inválidos respeitam o contrato de erros.

Log local: `backend/target/unitizacao-verify.log`; relatórios: `backend/target/surefire-reports`; artefato: `backend/target/wms-backend-0.0.1-SNAPSHOT.jar`. São gerados e ignorados pelo versionamento. O aviso já conhecido de instrumentação automática Mockito/JDK 21 permanece sem falha.

A conferência adicional encontrou 99 arquivos Java, seis properties e 39 arquivos Markdown fora de diretórios gerados; UTF-8 e referências locais passaram. IDs preservados: 16 BE, 13 FE, um OR e D01 a D16, sem duplicação. Os fontes Java são anteriores aos relatórios finais, e `git diff --check` não apontou problemas. A preparação paralela D14/OR01 e os arquivos de orquestração foram preservados.

## Artefato em execução

O JAR final foi iniciado separadamente com perfil `local`, endereço `127.0.0.1` e porta aleatória. `GET /api/v1/status` respondeu **200**; o listener observado ficou somente em loopback. Não houve inicialização de DataSource/JPA. O processo foi encerrado após o teste, com encerramento gracioso registrado.

SHA-256: `70E5F2F4E31D15FCF63D4EE52C00D19B9307E1254CD780049BA934798EADE000`.

Inspeção do JAR confirmou controller/service de unidades e as configurações properties, sem H2, ArchUnit, identidade/configuração de testes ou YAML. Essa execução local verifica inicialização/empacotamento; as rotas persistentes foram exercitadas na suíte HTTP/H2, não no perfil local sem banco.

## Banco e verificações externas

V3 foi preparada, sem execução, para três tabelas, composição/rastreabilidade, unicidade de identidade/operação, marcador da entrada, restrições e ações de auditoria. V1/V2 foram preservadas. O [procedimento](../database/migrations/README.md) separa permissões de aplicação/migração e prevê UPDATE somente no marcador da entrada conferida.

**Nenhuma V1/V2/V3 foi executada no SQL Server ou em H2.** H2 gera estrutura pelo JPA; não comprova dialeto, locks, comportamento de concorrência, privilégios por coluna, índices, JSON ou evolução do SQL Server. Essas validações, com dados fictícios preexistentes e `ddl-auto=validate`, continuam em BE03/BE07.

Não houve conexão com banco empresarial, GRANT, impressão física, emissão fiscal, envio externo ou publicação. Não há implementação FE06, PDF/ZPL ou teste de simbologia/Tanca/coletor. SQL Server, provedor real, operação com Caio e dispositivos com Mickael mantêm validações próprias. BE07 fica **em validação**, e M02/piloto permanecem abertos.

Próximo código: BE08, endereçamento, capacidade, histórico de movimentação e disponibilidade. Reserva/saída e cobrança seguem etapas posteriores; quantidade conferida/unitizada ainda não representa saldo disponível para expedição.
