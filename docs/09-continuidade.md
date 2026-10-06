# Continuidade do projeto WMS Rodogarcia

Estado atualizado em06/10/2026. D19 concluída no escopo local BE01–BE16: build final367/0/0/0, revisão Vigia favorável, V9/JPA904/904+129/129 em arquivos, artefato e Graphify conferidos. Aceites240/292/333 e freeze360 com P2 retido permanecem históricos. Evidência atual32, matriz34 e modelo35. SQL Server, identidade real, equipamentos, fiscal/comercial, frontend e homologação continuam separados.

## Situação atual

- Projeto em `C:\Users\suporte\Documents\projetos\wms-rodogarcia`, com backend, frontend, docs, database e infra.
- Base definida pelo responsável: React/TypeScript, Java/Spring, MVC convencional com pastas por camada e SQL Server existente.
- PDF original preservado e questionário Word recebido em 05/10/2026 copiado sem alteração para referências.
- Os 110 subitens do roteiro receberam respostas. Foram lidos texto e imagens de referência.
- O responsável orientou resolver as pontas pelo raciocínio com o material existente, sem remeter cada detalhe ao gestor.
- Regras explícitas em `10-respostas-recebidas-2026-10-05.md`; interpretações e propostas em `11-alinhamentos-apos-respostas.md`.
- AC01 a AC16 são tratamentos com solução proposta, não dezesseis perguntas abertas. Não reenviar as 110 perguntas como pendentes.
- `states.md` mantém16 IDs backend e13 frontend: BE01/BE02/BE16 concluídas localmente; BE03–BE15 em validação externa com escopo local aceito. Frontend planejado, IDs preservados.
- Spring MVC com cinco cadastros, recebimento manual/XML, unidades/etiquetas, capacidade, conjuntos de posições, movimentos, bloqueios, saldo/histórico e pedido/FIFO/reserva; permissões por perfil/cliente/armazém, repetição e auditoria atômicas. Documento 24 define contratos FE09 e complemento FE08; 25 registra build/197 testes. Documentos 12 a 23 preservam as entregas anteriores.
- Usar `.properties` em resources. Documento 16 registra validação nas entradas dos serviços, formatação/imports, JDK/Maven conferidos no build e testes das fronteiras MVC. Executar `spotless:apply` após editar Java e `clean verify` para validar código.
- V1–V9 preparadas em arquivos: V7/JPA518/518+67/67, V8/JPA523/523+163/163 e V9 final904/904+129/129. Preparo final306/306, identidade18/18, fecho77/77 e488 hashes; Vigia favorável08:34 após correção que separa histórico360 e atual367. Nenhuma conexão/migration SQL Server real. Comparação textual não executa JPA/SQL; H2 não comprova dialeto/locks/permissões. Tokens de teste RSA efêmeros, XML sem homologação fiscal, etiquetas sem impressão/leitura física validadas; provedor real/frontend/homologação/publicação externos.
- Unitização fecha quantidades BOA/AVARIADA por entrada; não somar entradas e unidades como se fossem estoques diferentes. Divisão preserva ID remanescente; reagrupamento encerra origens consumidas sem apagá-las. Mesmo pedido/nota/SKU/lote/validade/FIFO/instante de chegada/DUN/tipo/condição são exigidos no recorte. Transformações BE07 exigem unidades ainda não endereçadas e sem bloqueio. Resposta idempotente é a confirmação original; consultar GET para estado atual.
- BE08 calcula disponibilidade no backend; triagem/quarentena/avaria não atendem saída. Quarentena deixa bloqueio persistente até liberação expressa. Medidas iniciais e equivalência ficam preservadas; mover não reinicia armazenagem nem muda etiqueta. D18 integra reserva: físico unitizado = disponível + reservado + bloqueado; total físico inclui entradas ainda não unitizadas. Remanescente do pallet parcialmente reservado fica protegido conforme proposta AC10. Reserva não vence nem é liberada por dano; GET/revalidação indicam o impedimento. Detalhes nos documentos 22/24.

## Equipe preparada no Maestri

D14 preparou uma equipe própria no programa aberto: Hermes WMS (entrada com perfil, memória e sessões próprios), WMS - Farol (supervisão), Cedro (backend), Lume (frontend), Prumo (banco/infra) e Vigia (revisão). Ela fica ao lado da equipe existente, no mesmo canvas Projetos, com pasta e papéis WMS. Hermes WMS se liga ao Farol; os especialistas se ligam ao Farol como supervisor. A nota WMS pertence somente aos seis terminais WMS. Nenhuma ligação de agente ou nota une os projetos. Os terminais antigos, suas posições, papéis e conexões foram preservados nos campos conferidos.

Consultar o [documento 17](17-orquestracao-maestri-hermes.md), [instruções da equipe](../orchestracao/README.md) e [perfil Hermes](../orchestracao/hermes/README.md). OR01 registra a preparação, quando havia escolhas de privacidade/confiança pendentes, e a evidência atual. D18 comprovou entregas/revisão; D19 comprovou leitura dos quatro especialistas e parecer de contratos de Lume, sem frontend. Prumo/Vigia relataram falta do CLI/ambiente Maestri próprio; Farol recebeu e encaminhou pelo `ask`/`check`. OR01 ainda precisa do retorno autônomo de todos os workers e conferência completa do caminho Hermes WMS; não bloqueia o backend autorizado. Não responder confirmações de confiança/privacidade via raw. Preparação D14, entrega D18 e autorização contínua D19 são registros distintos. Telegram WMS não configurado; gateway, ponte, controles e perfil padrão do Hermes antigo não foram reconfigurados. Não usar o Hermes vizinho como alternativa.

## Leitura para retomar

1. [Orientações](../AGENTS.md) e [states.md](../states.md): resumo, próximo passo e etapas pertinentes.
2. [Respostas recebidas](10-respostas-recebidas-2026-10-05.md) e [regras consolidadas](11-alinhamentos-apos-respostas.md), incluindo exemplos de cobrança e dados reais de implantação.
3. [Decisões e pendências](06-decisoes-e-pendencias.md), com D01 a D19 e situação Q01 a Q27; [execução contínua](26-execucao-continua-backend.md).
4. [Arquitetura](02-arquitetura.md), [fluxos](04-fluxos-operacionais.md) e [cenários](08-cenarios-de-validacao.md).
5. Conforme a etapa: [recebimento/conferência](18-recebimento-e-conferencia.md), [unidades e etiquetas](20-unidades-logisticas-e-etiquetas.md), [endereçamento e estoque](22-enderecamento-movimentacao-e-estoque.md), [pedido/FIFO/reserva](24-pedido-saida-fifo-e-reserva.md), [validação atual](25-validacao-pedido-saida-e-reserva.md), [padrões de engenharia](16-padroes-de-engenharia-backend.md) e [execução](../backend/README.md). Banco em [procedimento de migrations](../database/migrations/README.md); cadastros no documento 14.

## Regras que já orientam o desenho

- Parcial de pallet permitido; parcial de pedido de saída proibido. Cancelar e recriar para mudar quantidade.
- Uma nota pode ter várias chegadas, mas é efetivada uma vez e pertence a um pedido. Divergência mantém carga em quarentena.
- Triagem não atende saída. Reserva não vence e continua na pendência fiscal.
- FIFO usa chegada física; nota em partes usa primeira chegada. Devolução preserva referência original.
- Operador justifica exceção; supervisor/gestor autoriza. Essa conciliação não exige segunda pessoa obrigatória.
- Uma unidade por posição; duas posições para unidade grande são a interpretação proposta para conciliar Q06 e Q14.
- Emissão fiscal e confirmação física permanecem distintas; proposta de confirmação pelo supervisor/gestor com XML em AC01.
- Diárias partem do início de armazenagem, separado do primeiro endereçamento em triagem/quarentena, conforme22/29. Incluir data inicial e excluir saída, com pico cobrável, é a convenção proposta em AC04; validar os exemplos antes de cobrar.
- Cobrança persiste em separação e quarentena posterior; avaria atribuída à Rodogarcia suspende a parte afetada conforme proposta AC08.
- NOTAZZ emite nota de mercadoria; ESL emite NFS-e dos serviços. Procedimento manual intermediário é proposto para o piloto.
- Transferências entre armazéns fora da primeira versão. Retorno físico por nova entrada.
- Piloto Osasco, depois Castro/Tigre; Caio valida. Data desejada 13/10/2026, ainda sem compromisso técnico de entrega.
- Dez usuários/quatro simultâneos estimados. Mickael cuida dos equipamentos; Lucas de TI; Natalina de fiscal.
- Falha isolada do coletor aguarda equipamento; queda geral usa planilha. Até 36h de parada informadas, sem autorização para perder 36h de dados.

## Próximo trabalho

**Complemento da mesma D20:** nomes exclusivos `WMS_DEV` e `WMS_PROD`; DEV primeiro, sem migrations/cargas em PROD nesta rodada. Criação somente após alvo/acesso reais inequivocamente confirmados, sem sobrescrever existentes. Jamais alterar SQL Server global/instalação/versão/serviços/instância/collation do servidor, compatibilidade/configurações de bancos existentes ou outros bancos/acessos/rotinas. AGENTS conserva a regra durável. “System admin” não identifica host/instância; Farol solicitou somente endereço exato, sem senha. Preparação local continua, sem conexão adivinhada ou emprestada de outro projeto.

**D20 autorizada em06/10/2026 por Lucas, em andamento:** iniciar o macrobloco local database BE03/BE15 após D19 concluída. Auditar V1–V9/modelos/permissões e implementar melhorias concretamente justificadas, automações/testes isolados fictícios e procedimentos. Prumo escreve database/infra; Cedro somente integração backend necessária; Vigia revisão independente; Farol registros centrais/mapa. [Documento36](36-database-local-engenharia-e-validacao.md) mantém execução e evidências. Não recriar modelo nem reabrir questionário. Limites completos na D20 do06; resultado no ask e em `orchestracao/.runtime/`, sem callback Hermes. Ensaio SQL Server da empresa permanece separado e não autorizado.

Escopo local D19 concluído e matriz BE01–BE16 atualizada no34. Não há próxima implementação backend independente identificada na auditoria de Cedro e na revisão vigente; abrir código somente diante de achado concreto. Consultar32 para comandos/resultados/artefato e33 para configuração/recuperação/ensaio. Frontend não iniciou; sua trilha requer pedido próprio.

Lucas/TI e DBA definem alvo SQL Server, versão/collation, identidades/TLS/permissões e autorizam aplicação/evolução/locks/restauração. Lucas/equipe técnica define provedor/contas/login/renovação/revogação/rotação/contenção. Caio/gestor confirma operação, capacidade, estoque inicial se existir e propostas AC01–AC16; gestor/comercial fornece preços/mínimo/GRIS/cortes; Natalina/Controladoria valida fiscal/NOTAZZ/ESL; Mickael/TI valida etiqueta/impressão/leitura/cobertura. Ausências continuam explícitas, sem inventar valores, histórico ou prazo de piloto.

Os arquivos de migrations/procedimentos não são aplicação SQL, e H2 não comprova dialeto, índices/CHECKs, isolamento/deadlock/permissões/recuperação SQL Server. Artefato construído não é versão publicada/em operação. Preservar MVC/properties, históricos/aceites/IDs e dados originais. Mantidas as proibições de SQL Server real, emissão fiscal/NFS-e, cobrança real, publicação, commit/push, rotinas, perfis Hermes e ETL. Confiança/privacidade nunca são respondidas por raw. Farol conserva registros centrais/mapa; equipe somente WMS, sem identidade/ambiente de outro terminal.
