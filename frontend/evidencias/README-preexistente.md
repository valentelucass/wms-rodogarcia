# Frontend do WMS Rodogarcia

Tecnologia definida pelo responsável: React com TypeScript. Esta pasta está reservada para a implementação futura; ainda não há aplicação ou dependências instaladas.

## Uso previsto

- Interface administrativa para cadastros, entrada, estoque, saída, faturamento e relatórios.
- Telas simples para coletores, priorizando a leitura da unidade e do endereço.
- Indicação clara de confirmação, pendência ou falha em cada operação importante.
- Visualização dos motivos de bloqueio e das próximas ações permitidas.

A escolha da ferramenta de construção, biblioteca visual, navegação, organização interna e suporte a dispositivos será feita na preparação técnica. O aplicativo inicial depende de conexão; a contingência recebida é por planilha com conciliação posterior, conforme AC14. Não pressupor sincronização automática sem rede.

As regras e permissões críticas serão verificadas no backend, conforme a [proposta de arquitetura](../docs/02-arquitetura.md). As validações da interface ajudam o operador e não substituem essa verificação.

O mapa visual de posições é uma melhoria posterior ao núcleo operacional, conforme a especificação.

A implementação será acompanhada na [trilha frontend do states.md](../states.md#trilha-frontend), de FE01 a FE13, com dependências do backend. Atualizar ali andamento, integração verificada, evidências e próximo passo. Uma tela com dados simulados não equivale a uma jornada integrada.
