# Etapa 11 — exportação XML e uma bobina por item

FE05-EXPORTACAO01, concluída no DEV em 13/10/2026. Complementa a [etapa 10](etapa-10.md) e preserva os componentes, formulários, temas e referências automáticas existentes.

## Fonte e comportamento

Decisão encaminhada por Lucas: na operação Bracell, cada item da nota corresponde a uma bobina, posteriormente endereçada em sua posição. NFs 32332 e 32485 fornecidas com XML/PDF reais. Não universalizar essa regra para outros clientes ou produtos nem inferir bobinas pelo peso total ou número de volumes.

O backend reconhece especificamente a exportação com versão/Id/nItem em elementos. Confere namespace, campos únicos, chave/dígito e identificação coerentes, protocolo, números decimais, itens e soma de produtos; mantém limites de tamanho/profundidade e bloqueio de DTD/entidades externas. A adaptação existe somente no DOM de leitura; o XML original é armazenado intacto. Um XML convencional inválido não recebe aprovação por fallback. O modo operacional é explicitamente identificado e não declara validação pelo esquema fiscal completo, assinatura ou consulta de situação na SEFAZ.

Regra de bobina por item em WMS_XML_BOBINA_POR_ITEM, pares emitente:SKU separados por vírgula. Padrão confirmado: 53943098000187:000000000000600366, somente unidades de massa suportadas. O perfil sqlserver-dev adiciona exclusivamente 00000000090601:TESTE-BOBINA-EXPORT para regressão fictícia; esse par não pertence ao padrão de produção. Configuração vazia desativa a sugestão. Produtos existentes não são alterados.

A prévia mostra uma bobina e uma posição previstas por linha, preservando linhas do mesmo SKU. A chegada física sugere a quantidade comercial convertida e uma bobina apenas para item ainda não recebido. Operador confirma peso, condição e contagem; importação não movimenta estoque. Uma única bobina recebe o peso exato conferido na distribuição; múltiplas bobinas continuam exigindo pesos individuais. Soma/contagem e capacidade/ocupação são validadas pelos serviços existentes. Não há divisão de peso por média, correção de escala ou lotes inferidos de texto livre.

## Amostras reais

| Amostra | Resultado da leitura |
| --- | --- |
| NF 32332 | Uma linha, 1,587 TO comerciais = 1.587 kg; 1,548 TO tributáveis = 1.548 kg. Diferença de 39 kg visível; valores originais preservados. |
| NF 32485 | Oito linhas independentes do mesmo SKU; 1.848, 1.826, 1.837, 1.782, 1.783, 2.026, 1.550 e 1.520 kg, total 14.172 kg. Peso líquido literal 14.172 no XML equivale a 14,172 kg; alerta de diferença sem multiplicação automática. |

Ambas passaram pelo parser Java offline e pela prévia autenticada. Nenhuma das notas reais foi confirmada como entrada. Não falta outro documento do gestor para suportar o formato entregue; divergências são tratadas na conferência física, sem inventar sua resolução.

## Verificação executada

- 584 testes frontend em 57 arquivos, tipagem, lint e build aprovados. 91 testes backend comprovadamente puros, sem H2, com build e formatação aprovados. Testes cobrem formato limitado, repetição de SKU, chave/protocolo/totais inconsistentes, decimais inválidos, XXE e escopo da regra, além das sugestões no frontend.
- Guarda atual WMS_DEV/WMSDEV/TLS: 26 verificações aprovadas. Uma execução de iniciar-dev.bat preparou a fonte atual e reiniciou somente os dois processos DEV de posse atestada. Run 6bb28488-9e38-4438-a07c-5f4fac740a43; backend PID 47028/25580, frontend PID 6332/25581. Comparação posterior dos 323 fingerprints e integridade do JAR aprovadas.
- 31 verificações da jornada e ambiente integrados aprovadas. Chrome autenticado: prévias reais, criação fictícia de oito itens, recebimento e unitização de uma bobina pelo navegador, IDs técnicos automáticos, desktop/mobile 390 px, temas claro/escuro, confirmação de descarte por Escape. Screenshots inspecionadas.
- APIs autenticadas: oito chegadas individualizadas, efetivação, oito unidades/etiquetas/pesos, repetição simultânea sem duplicidade, XML original preservado, recusas de peso negativo/divergente, nova confirmação da nota existente, endereço incompleto e posição ocupada. Nove unidades endereçadas separadamente; consulta posterior de saldo retorna 15.759 kg disponíveis, zero pendência/triagem/quarentena/reserva.

Fixtures preservadas: cliente 78, armazém 44, produto 82, embalagem 57; pedidos 87/88, notas 78/79, entradas 103–111, unidades 1347–1355, endereços 142–150. Marcador TESTE-EXPORT-1791920301864. Toda escrita por API legítima; sem SQL direto de fixtures, DDL/migrations, reset, H2, produção ou importação de NF real. Ajustes de payload/seletores do executor estão registrados separadamente de defeitos do produto. Não houve falha do produto durante a jornada final.

[Resultado](../../orchestracao/.runtime/fe05-exportacao/resultado.json), [jornada e IDs](../../orchestracao/.runtime/fe05-exportacao/jornada.json), [leitura das amostras reais](../../orchestracao/.runtime/fe05-exportacao/amostras-reais.json). Logs locais: .tools/fe05-export-frontend-final.log e .tools/fe05-export-backend-final2.log. Aviso de bundle frontend acima de 500 kB permanece, sem falha do build.

Este aceite cobre o recorte implementado e sua regressão disponível. Não substitui impressora/coletor físico, SEFAZ, produção ou nova execução integral dos 879 casos históricos. As provas anteriores de Excel, códigos automáticos e demais telas permanecem identificadas na [etapa 9](etapa-09.md), sem serem contabilizadas como testes novos.
