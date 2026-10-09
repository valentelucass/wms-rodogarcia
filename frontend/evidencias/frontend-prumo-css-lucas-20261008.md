# Prumo — base CSS local de Lucas, 08/10/2026

**Resultado da fatia: PASS.** CSS e entrypoint aplicados em Chromium local, DEV e BUILD próprios, desktop 1440/768 e coletor 390. 30 capturas atuais, zero erro de console, zero violação CSP, zero overflow do documento, zero tentativa de API/rede externa. Não é aceite global do frontend nem integração real.

Manifesto de posse confirmado: `f49f86454f8852ddf334c71a04e4ebd624968b28892d62faaa70883711e2220b`. Organização de cinco estilos e classes existentes preservada. Sete arquivos alterados; `src/styles.css` ficou intacto.

## Causa e correção

Antes, em `http://127.0.0.1:5191/#coletor`, o DEV carregava styles.css como módulo JS e o Vite injetava `<style>` inline: a CSP `style-src self` recusava sua aplicação. Prova: Times New Roman, body margin 8px, cabeçalho sem fundo/padding, zero stylesheets e evento style-src-elem/inline. O BUILD inicial da mesma cópia já aplicava Arial, margin 0 e azul por asset externo. Não se conhece a URL original observada por Lucas.

`index.html` passa a carregar `<link rel="stylesheet" href="/src/styles.css">`; `main.tsx` deixa de importar o CSS como JS. Os cinco @import continuam no agregador. DEV e BUILD usam folhas externas, sem relaxar CSP e sem bypass. Base mínima: Arial/16px/preto/branco/azul124cab, espaçamento/alinhamento, botões/campos44px, coletor52px, foco azul3px, feedback/detalhes legíveis, navegação horizontal contida em telas<=800 e tabelas com rolagem contida.

## Provas e fonte

- URL sequencial própria: `http://127.0.0.1:5191/#cadastros`, `/#entrada`, `/#coletor`; processos próprios fechados após captura. Não é URL atualmente servida ao usuário.
- Cadastros: consulta com tabela/sucesso, erro e formulário em 1440/768. Entrada: navegação real por botão e feedback em 1440/768. Coletor390: input/foco, erro, consulta/avanço de foco, detalhes medidas/conjunto abertos por teclado, revisão, resumo confirmado com foco e detalhes da resposta.
- DOM/getComputedStyle registra fonte/tamanho/margem/padding/layout/gap/background/color/border/outline/min-height/overflow; documentWidth e mediaqueries800/480; URL/rules de cada folha. HTTPstatus/Content-Type/bytes/SHA de assets estão no JSON. Todas respostas CSS text/css e HTTP200.
- Asset CSS BUILD: `index-lEDF85Za.css`, SHA-256 `6352f457744254bd3e2fce1359b24bcf51bad8d2e33886e7050ccf85c2aed24d`, em `prumo-css-20261008/dist-verified/assets`. Nenhuma escrita em dist/evidências Lume.
- Fonte executada: cópia completa consistente `diagnostic-source-v03` + oito arquivos em posse para `verified-source-r02`; manifests de todos os arquivos e hashes em snapshot-diagnostic-v03.json/snapshot-verificado.json. O snapshot do handoff continha só recortes; a tentativa inicial incompleta foi preservada e substituída por cópia completa, com comparação antes/cópia/depois.
- Comandos: formatter local apenas nos oito arquivos; `PLAYWRIGHT_BROWSERS_PATH=frontend/.tools/ms-playwright node evidencias/prumo-css-diagnostico.mjs`; `node evidencias/prumo-css-prova.mjs`; `node evidencias/prumo-css-complemento.mjs`. Scripts chamam Vite DEV/build/preview em5191 com cache/saída próprios. Prova e complemento retornaram exit0; sem typecheck/lint/suíte geral repetida. Logs r01 de browser ausente, snapshot incompleto e falha skip preservados.

| Arquivo | SHA-256 atual |
| --- | --- |
| index.html | a46f3386778c68a07d187220333c5a20ffa4747a04288301360106b89160446c |
| src/main.tsx | 77a65c915e234dfd64b01f09dd7a2dde76b7eb0ea73f18982ec9ad2a3978c3fe |
| src/styles.css | 8acce5176644dd1b22fe8e45cc58a8984b9b2806fdb67061cf73d105700f7e0c |
| src/styles/base.css | ea18b51ff8e51034273c03e2135373a4098af5f33beb9ea52ab9d51f8bc16493 |
| src/styles/collector.css | e35a01c5e5ffd132321ee1645391e49146162b490fae4d491a5179b5394d1c6d |
| src/styles/forms.css | 17dbdfda862d882a8bdce916eec7430bebd3871196d402ade2502b68f003e903 |
| src/styles/results.css | 14ec77fba47d4d4aa78e349d80c51b003e709491acf2d72594a52393767afcff |
| src/styles/shell.css | 7365063d02c5b52b1ec3d3fe35cfe26db9e1c3d415aa76cf21a02f9770fd0334 |

| Estado DEV | Largura | Fonte | Main padding | Outline foco | Documento |
| --- | --- | --- | --- | --- | --- |

## Limites e encaminhamento

O link “Ir para o conteúdo” tem defeito na navegação da fonte copiada: href=#conteudo dispara useNavigation e troca a jornada por “Módulo não encontrado”. Foi reproduzido, registrado no JSON e comunicado ao Farol para Lume corrigir markup/hook, fora da posse Prumo. A prova de CSS prosseguiu recarregando a jornada após registrar o defeito. Não declarar teclado global aprovado com esse resultado.

Lume/Cedro trabalham nas demais fontes; esta cópia não certifica a futura integração global. Revisão da fatia foi encaminhada ao Vigia via Farol após estabilizar hashes. Lume integra e executa os checks finais. Nenhum componente/contrato/package/config/tools/backend/canônico/mapa foi editado por Prumo. Sem SQL, HTTPDEV, integração real, equipamento, publicação ou callback Hermes.

Recibo completo: [JSON](frontend-prumo-css-lucas-20261008.json). Capturas representativas: [desktop formulário](prumo-css-dev-cadastros-formulario-1440.png), [tablet tabela](prumo-css-build-cadastros-tabela-sucesso-768.png), [coletor](prumo-css-dev-coletor-consulta-390.png), [resumo confirmado](prumo-css-build-coletor-resumo-390.png).
