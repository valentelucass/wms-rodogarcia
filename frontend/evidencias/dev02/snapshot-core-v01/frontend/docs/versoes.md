# Versões verificadas e compatibilidade

Consulta oficial em08/10/2026 e instalação real com lock. React19.3 está na [lista oficial](https://react.dev/versions). [Vite](https://vite.dev/guide/) documenta requisitos de Node. [TypeScript](https://www.typescriptlang.org/download/) apresenta7.0 como atual; nesta combinação escolheu-se6.0.3 estável porque o peer typescript-eslint8.71.1 exige>=4.8.4 e<6.1.0. A tentativa7.0 foi recusada pelo npm e preservada. Guias oficiais [Vitest](https://vitest.dev/guide/) e [Playwright](https://playwright.dev/docs/intro) orientam testes locais.

Instalados: React/react-dom19.3.0, Vite8.3.4/plugin-react6.1.2, TypeScript6.0.3, Vitest5.0.3, ESLint10.12.0/@eslint/js10.0.1/hooks7.1.1, typescript-eslint8.71.1, Playwright1.64.0, lossless-json4.3.1, qrcode1.5.4 e Prettier3.9.9. Node24.21.0 e npm11.19.0 reais. package.json exige Node24>=24.13, atendido pelo host. Lock fixa também tipos e bibliotecas de teste.

evidencias/versoes-instaladas.json lê engines/peers dos pacotes efetivamente instalados. plugin-react6 requerVite8; ReactDOM19.3 requerReact19.3; pluginsESLint atuais aceitamESLint10; TS6.0.3 atende parser. Npm resolveu sem --force/--legacy-peer-deps. Tentativas incompatíveis anteriores estão nos logs. Auditoria do lock retornou zero vulnerabilidades naquele instante, sem certificação futura.

Chromium próprio156.0.8078.4/v1248 foi instalado sob .tools/ms-playwright e exercitado no Playwright. Provas não homologam outros navegadores/coletor físico. Python3.12/PyMuPDF próprios foram usados para extrair/renderizar PDF, sem mudar originais. Instalação/ensaio não publicaram a aplicação.
