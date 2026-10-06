# Ferramentas MCP deste projeto

## Integração de ferramentas MCP — 05/10/2026

- ai-memory acessível pelo MCP no escopo exclusivo workspace=rodogarcia, project=wms-rodogarcia; declaração em .ai-memory.toml. Históricos existentes preservados e espaços novos registrados vazios pelo mecanismo nativo, sem sessões ou páginas artificiais.
- Graphify consultável pelo MCP com project_path deste repositório: 1629 nós e 5861 relações na conferência. JSON, relatório e HTML presentes; verificação do grafo exportado encontrou zero relações com endpoint ausente. Consultas reais de saúde e conteúdo executadas pelos dois MCPs.
- Instruções oficiais e sete skills locais preparados, com escopo explícito em AGENTS.md; hooks Git de commit e troca de branch instalados. A captura de sessões usa os hooks globais já instalados no Codex.
- Preparador comum e guia em ../.runtime/mcp-tools/README.md; integração automática para novos repositórios Git diretamente em projetos, com processo próprio no login do usuário. Recibos e logs ficam na pasta da ferramenta. Não modifica runtime das aplicações, banco ou equipe Maestri.
- Limites: Conteúdo de 42 documentos/PDFs incluído; imagens incorporadas do Word não reavaliadas. Diagnóstico bruto: 778 referências sem nó definido e 285 relações paralelas agrupadas; grafo exportado com zero endpoints ausentes. Auditoria original preservada em graphify-out/extraction-audit.json e integrity-audit.json. Tokens dos agentes não medidos.

Este registro trata somente das ferramentas dos agentes. Critérios e pendências de implementação deste projeto permanecem nas seções próprias acima.
