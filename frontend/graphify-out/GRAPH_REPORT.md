# Graph Report - frontend  (2026-10-09)

## Corpus Check
- 192 files · ~122,151 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 11 file(s) not represented in the graph (top: .css 7, (none) 2, .example 1)

## Summary
- 952 nodes · 2736 edges · 26 communities (25 shown, 1 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 38 edges (avg confidence: 0.88)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `eaeadb54`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- types.ts
- runtime.ts
- Values
- workflow.ts
- api/client.ts
- marco02-selection.test.tsx
- journeys.spec.ts
- useExerciseSession.ts
- journeys.ts
- OperationResults.tsx
- marco.mjs
- package.json
- README.md
- devDependencies
- snapshot-lume.mjs
- contratos.py
- mapeamento.mjs
- compilerOptions
- vite.dev.config.ts
- ready-chain.ts
- ref_node_path
- scripts
- login-browser.mjs
- closure-fronteiras.mjs
- dependencies

## God Nodes (most connected - your core abstractions)
1. `Values` - 98 edges
2. `isObject()` - 82 edges
3. `Request` - 56 edges
4. `fixture()` - 43 edges
5. `vitest` - 42 edges
6. `endpoint` - 31 edges
7. `react` - 30 edges
8. `@testing-library/react` - 27 edges
9. `AuthClient` - 26 edges
10. `Operation()` - 26 edges

## Surprising Connections (you probably didn't know these)
- `Responsabilidades da fonte D31` --references--> `call()`  [INFERRED]
  docs/organizacao.md → src/api/client.ts
- `Contratos e limites de transporte` --references--> `required()`  [INFERRED]
  docs/contratos-e-transporte.md → src/auth/contracts.ts
- `Responsabilidades da fonte D31` --references--> `Workflow`  [INFERRED]
  docs/organizacao.md → src/domain/workflow.ts
- `Responsabilidades da fonte D31` --references--> `ExampleDamage`  [INFERRED]
  docs/organizacao.md → src/modules/estoque/ExampleDamage.ts
- `Responsabilidades da fonte D31` --references--> `ExampleBilling`  [INFERRED]
  docs/organizacao.md → src/modules/financeiro/ExampleBilling.ts

## Import Cycles
- None detected.

## Communities (26 total, 1 thin omitted)

### Community 0 - "types.ts"
Cohesion: 0.01
Nodes (208): ApiContracts, ArmazemDto_Alterar, ArmazemDto_Criar, ArmazemDto_Resposta, AuditoriaResponse, AvariaDto_Confirmacao, AvariaDto_Ocorrencia, AvariaDto_Reconhecer (+200 more)

### Community 1 - "runtime.ts"
Cohesion: 0.08
Nodes (48): react, CommandDraft, prepareCommand(), FieldControl(), Fields(), Props, prefixLabel(), ScalarField() (+40 more)

### Community 2 - "Values"
Cohesion: 0.09
Nodes (36): Responsabilidades da fonte D31, ApiError, Request, snapshotFingerprint(), demoCode, examples, exampleValues(), baseValues (+28 more)

### Community 3 - "workflow.ts"
Cohesion: 0.08
Nodes (42): AuthApp(), Workspace(), LoginPage(), PasswordPage(), submit(), UsersPage(), ReferenceOption, JourneyPage() (+34 more)

### Community 4 - "api/client.ts"
Cohesion: 0.17
Nodes (33): @testing-library/react, @testing-library/user-event, vitest, call(), realTransport(), Transport, FictitiousTransport, Operation() (+25 more)

### Community 5 - "marco02-selection.test.tsx"
Cohesion: 0.10
Nodes (32): AuthClient, decodeAuth(), decodeCsrf(), decodeTokens(), decodeUser(), decodeUsersPage(), encodeRevisionBody(), long() (+24 more)

### Community 6 - "journeys.spec.ts"
Cohesion: 0.06
Nodes (35): lossless-json, @playwright/test, csrf(), json(), page(), tokens(), user, user (+27 more)

### Community 7 - "useExerciseSession.ts"
Cohesion: 0.12
Nodes (26): react-dom, Scenario, App(), Collector(), AppShell(), narrow(), subscribeWidth(), ExerciseContext() (+18 more)

### Community 8 - "journeys.ts"
Cohesion: 0.15
Nodes (22): OperationConfirmation(), actionLabel(), verbs, cadastro(), Journey, NextAction, s(), Step (+14 more)

### Community 9 - "OperationResults.tsx"
Cohesion: 0.15
Nodes (18): qrcode, downloadReceipt(), Receipt, route(), LabelPreview(), OperationResults(), ReferenceLookup(), Result() (+10 more)

### Community 10 - "marco.mjs"
Cohesion: 0.08
Nodes (20): baseline, browser, checks, contract, contractInputs, coverage, emittedUtc, excluded (+12 more)

### Community 11 - "package.json"
Cohesion: 0.10
Nodes (19): engines, node, name, private, type, version, eslint, @eslint/js (+11 more)

### Community 12 - "README.md"
Cohesion: 0.11
Nodes (12): Cobertura concreta e limites por etapa, Contratos e limites de transporte, Comando e contrato do launcher, Desenvolvimento frontend — D31-DEV02, Histórico e contrato D31 preservados, Preservação e provas, Sessão e cliente, Validação da cadeia (+4 more)

### Community 13 - "devDependencies"
Cohesion: 0.10
Nodes (20): devDependencies, eslint, @eslint/js, eslint-plugin-react-hooks, globals, jsdom, @playwright/test, prettier (+12 more)

### Community 14 - "snapshot-lume.mjs"
Cohesion: 0.11
Nodes (13): definitions, raw, result, rows, files, logs, receipt, cedro (+5 more)

### Community 15 - "contratos.py"
Cohesion: 0.15
Nodes (4): balanced(), constraints(), strip_annotations(), ts()

### Community 16 - "mapeamento.mjs"
Cohesion: 0.12
Nodes (12): typescript, bindings, cadastro, cadastroActions, endpoints, helperFile, missing, rows (+4 more)

### Community 17 - "compilerOptions"
Cohesion: 0.12
Nodes (15): compilerOptions, allowImportingTsExtensions, jsx, lib, module, moduleResolution, noEmit, noUnusedLocals (+7 more)

### Community 18 - "vite.dev.config.ts"
Cohesion: 0.25
Nodes (10): backendTarget(), dataMode, DevEnvironment, frontendPort(), PublicIdentity, ReadyDependencies, readyFiles, readyFixture() (+2 more)

### Community 19 - "ready-chain.ts"
Cohesion: 0.41
Nodes (13): requirePublicIdentity(), checkedBytes(), current(), digest(), equal(), hashed(), integer(), object() (+5 more)

### Community 20 - "ref_node_path"
Cohesion: 0.15
Nodes (7): inspectBackend(), BackendIdentity, checks, results, directory, report, reportPath

### Community 21 - "scripts"
Cohesion: 0.18
Nodes (11): scripts, build, build:ficticio, check, dev, dev:ficticio, lint, preview (+3 more)

### Community 22 - "login-browser.mjs"
Cohesion: 0.20
Nodes (6): vite, checks, output, evidence, port, startedAt

### Community 23 - "closure-fronteiras.mjs"
Cohesion: 0.20
Nodes (8): contractInputs, inputs, manifestTarget, output, proofLogs, receipt, snapshot, target

### Community 24 - "dependencies"
Cohesion: 0.40
Nodes (5): dependencies, lossless-json, qrcode, react, react-dom

## Knowledge Gaps
- **371 isolated node(s):** `PublicIdentity`, `ObjectValue`, `name`, `private`, `version` (+366 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 421 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **1 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `vitest` connect `api/client.ts` to `runtime.ts`, `Values`, `workflow.ts`, `marco02-selection.test.tsx`, `journeys.spec.ts`, `package.json`, `vite.dev.config.ts`?**
  _High betweenness centrality (0.108) - this node is a cross-community bridge._
- **Why does `Values` connect `Values` to `runtime.ts`, `workflow.ts`, `api/client.ts`, `marco02-selection.test.tsx`, `useExerciseSession.ts`, `journeys.ts`, `OperationResults.tsx`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Why does `react` connect `runtime.ts` to `workflow.ts`, `api/client.ts`, `marco02-selection.test.tsx`, `useExerciseSession.ts`, `OperationResults.tsx`, `package.json`?**
  _High betweenness centrality (0.047) - this node is a cross-community bridge._
- **Are the 5 inferred relationships involving `isObject()` (e.g. with `.respond()` and `billingReferences()`) actually correct?**
  _`isObject()` has 5 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PublicIdentity`, `ObjectValue`, `name` to the rest of the system?**
  _371 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `types.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.009569377990430622 - nodes in this community are weakly interconnected._
- **Should `runtime.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.0798442064264849 - nodes in this community are weakly interconnected._