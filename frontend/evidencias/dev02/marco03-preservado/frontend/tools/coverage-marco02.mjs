import { readFileSync, writeFileSync } from "node:fs";
import { createHash } from "node:crypto";

// Índice de provas: não executa regras, testes nem modifica o parecer de Vigia.
const source =
    "../orchestracao/.runtime/frontend-vigia-parecer-lucas-20261008.json";
const raw = readFileSync(source);
const review = JSON.parse(raw.toString("utf8")).suficienciaJornadasLocais;
const files = (names) => names.map((name) => `tests/${name}.test.tsx`);
const definitions = {
    "FE04-01": [
        files(["registry-journey", "registry-frontiers"]),
        [],
        "Cliente1/revisão3→alteração4→consulta; produto/embalagem/endereço mantêm chaves imutáveis e Long9007199254740993; conflito não antecipa sucesso.",
    ],
    "FE04-02": [
        files(["registry-journey", "registry-frontiers"]),
        [],
        "Arquivo .xlsx fictício/multipart/armazém1→prévia2101/revisão3/hash retornado→consulta→confirmar; COM_ERROS e409 não concluem importação.",
    ],
    "FE04-03": [
        files(["registry-journey", "billing-journey"]),
        [],
        "Impedimentos→snapshot/encerramento/inativação→histórico; serviço2501/tabela2601/vínculo2701/vigência consultados, preço2.500000 e percentual wire null.",
    ],
    "FE05-01": [
        files(["receiving-journey", "receiving-frontiers", "damage-journey"]),
        ["journeys"],
        "Pedido101100previsto→chegadas50/48→diferença2→Supervisor/motivo/aceitarDivergencias→entrada20198triagem/0quarentena/indisponível;95boa5avaria em cenário próprio e Operação não efetiva.",
    ],
    "FE05-02": [
        files(["receiving-journey"]),
        ["journeys"],
        "XML→pedido101/importação sem chegada→consulta itens/revisão→conferência; não confunde importação fiscal e física. XML/replay/contexto compartilham testes de fronteira.",
    ],
    "FE06-01": [
        files(["receiving-journey"]),
        ["journeys"],
        "Entrada1000→duas unidades500→seleção explícita502→etiqueta por UUID; divisão/reagrupamento conservam revisões e origem sem cálculo local.",
    ],
    "FE06-02": [
        files(["receiving-journey"]),
        ["expedition"],
        "Reimpressão duas GET do mesmo UUID; divisão/reagrupamento atualizam etiqueta11/12; retirada parcial do pallet recebe490/revisão11, consultada por UUID da reserva.",
    ],
    "FE08-01": [
        files(["stock-queries"]),
        [],
        "SKU11/contexto1→unidade502→entrada202/nota302/localização→histórico page1/size2; categorias sobrepostas não somadas; recusa não vira zero.",
    ],
    "FE08-02": [
        files(["stock-queries", "indicator-frontiers"]),
        ["journeys"],
        "null/0/9007199254740993.123456, KG/UN e paginação sem total extrapolado; Operação não pede valor=true; Supervisor/Gestor o pedem conforme contrato.",
    ],
    "FE09-01": [
        files(["dispatch-operational", "marco02-selection"]),
        ["journeys", "expedition"],
        "Pedido integral100 sobre pallet500, reservas701/702 quantidades30/70 selecionadas por identidade, UUID/etiqueta/destino81 e versões próprias; SEPARADO recebido, remanescente recebido em FE10.",
    ],
    "FE09-02": [
        files(["dispatch-operational", "dispatch-profile-contract"]),
        [],
        "Justificativa FIFO retornada vinculada ao pedido/revisão; Operação não autoriza exceção;409avaria preserva reserva→reversão motivada/revisão2→consulta3 sem estoque liberado localmente.",
    ],
    "FE10-01": [
        files(["expedition-frontiers"]),
        ["expedition"],
        "Pedido601/reserva701/nota301→documento1601/revisão3→retirada4/XML/remanescente81→consulta baixa1701/fato1801/retiradaEm/revisão5; documento AUTORIZADO sozinho não é retirada.",
    ],
    "FE10-02": [
        [],
        ["expedition"],
        "Cenário próprio não retirado→documento simbólico1601→cancelamento4/CANCELADO sem movimento→retorno interno5/reserva701/endereço81→consulta RETORNO_INTERNO.",
    ],
    "FE10-03": [
        [],
        ["expedition"],
        "Após retirada, baixa1701→devolução2.000000/revisão5→pedidoEntrada102/entrada202→consulta EFETIVADO/nota302/item402/chegada13:00/efetivação13:01/FIFOoriginal; sem copiar IDs do DTO bruto.",
    ],
    "FE10-04": [
        files(["expedition-frontiers", "damage-journey"]),
        [],
        "Duas notas/coberturas por reserva/SKU/origem; simbólico sem movimento; cobertura incompleta/avaria recusadas; avaria3101→reconhecimentoGestor→reparo→consulta→liberação explicitamente recebida.",
    ],
    "FE11-01": [
        files(["billing-journey", "billing-frontiers"]),
        [],
        "Configuração/vigência compartilhadaFE04→sugestão com serviço2501→fato2801/quantidade2.123456→consultaVALIDO→cálculo801. Gestor seleciona compromisso901; Supervisor vê pendência sem resolução.",
    ],
    "FE11-02": [
        files([
            "billing-journey",
            "billing-frontiers",
            "billing-version-stage",
        ]),
        ["journeys"],
        "Memória dias/vigências/pico/avaria/mínimo/GRIS/null/zero/crédito exatos; ciclo901 e versão1001→decisão integral/recusa409→reabertura1002/número2/hash e próprio demonstrativo; aprovação/entrega2 conservam documentos1 históricos e PREPARADA sem decisão genérica.",
    ],
    "FE11-03": [
        files(["billing-journey"]),
        [],
        "Selecionar Cálculos801/Fechamentos901 por tabela semântica→origem1001/destino901/cálculo801→ajuste2901/motivo/prova→consulta histórico; DTO não pede delta, FE não o calcula.",
    ],
    "FE12-01": [
        [
            "tests/fe12-regularization.test.ts",
            "tests/fe12-presentation.test.tsx",
        ],
        ["fe12"],
        "Unidade→contagem sem Aplicar→consulta diferença/revisão/origens→Supervisor/Gestor ajuste motivado→consulta; reserva/impedimento recusados, Operação sem ajuste.",
    ],
    "FE12-02": [
        [
            "tests/fe12-regularization.test.ts",
            "tests/fe12-presentation.test.tsx",
        ],
        ["fe12"],
        "Carga fictícia com ausências→consulta pendência→revisar entrada201→preparar revisão/hash→confirmar leitura/etiquetas→consulta; criação/preparação não são confirmação.",
    ],
    "FE12-03": [
        [
            "tests/fe12-regularization.test.ts",
            "tests/fe12-presentation.test.tsx",
        ],
        ["fe12"],
        "CHEGADA manual tipada Long9007199254740993/decimal8.000001→consulta→VINCULAR prova/versão/hash→consulta; duplicado/replay/divergência/dependência não causam sync automático.",
    ],
    "FE12-04": [
        files([
            "stock-queries",
            "billing-journey",
            "receiving-journey",
            "dispatch-operational",
        ]),
        [],
        "Consultas compartilhadas por família/contexto/filtro/identidade; Gestor consulta auditoria tipoCLIENTE/registro1/página/tamanho após selecionar cliente1; Operação sem auditoria.",
    ],
};
const rows = review.etapas.flatMap((stage) =>
    stage.essenciais.map((criterion) => {
        const key = criterion.id.replace("REV-FE-JOR-", "");
        const [unit, browser, observed] = definitions[key] ?? [];
        if (!unit) throw Error("Critério sem prova: " + key);
        const testFiles = [
            ...unit,
            ...browser.map((name) => `tests/browser/${name}.spec.ts`),
        ];
        const witnesses = testFiles.map((path) => {
            const bytes = readFileSync(path);
            const lines = bytes.toString("utf8").split("\n");
            return {
                path: "frontend/" + path,
                sha256: createHash("sha256").update(bytes).digest("hex"),
                cases: lines.flatMap((line, i) =>
                    /^\s*(it|test)(\(|\.each)/.test(line)
                        ? [{ line: i + 1, declaration: line.trim() }]
                        : [],
                ),
            };
        });
        return {
            id: criterion.id,
            title: criterion.titulo,
            observed,
            witnesses,
            essentialSteps: criterion.passos,
            acceptableProof: criterion.provaAceitavel,
            proofLevel: browser.length
                ? "React/Transport e/ou Chromium local"
                : "React/jsdom + Transport fictício",
            limits: "Respostas preparadas, não prova de regra/backend/SQL/dispositivo/fiscal. Resultado final por caso/log no recibo MARCO02; existência deste índice não concede aceite.",
        };
    }),
);
if (rows.length !== 22)
    throw Error("Denominador esperado22, recebido " + rows.length);
const result = {
    demand: "FIM_FRONTEND_LUCAS_20261008",
    criteria: 22,
    reviewOrigin: {
        sourcePath: source.replace("../", ""),
        sha256: createHash("sha256").update(raw).digest("hex"),
        id: review.id,
        emittedUtc: review.emitidoUtc,
        role: "delimitação essencial revisão8; adendas posteriores preservadas",
    },
    execution:
        "Consultar execuções/resultados atuais no recibo frontend-lume-entrega-lucas-20261008.json. Provas compartilhadas não são somadas como novas jornadas.",
    rows,
};
writeFileSync(
    "docs/provas-jornadas-marco02.json",
    JSON.stringify(result, null, 2) + "\n",
);
writeFileSync(
    "docs/provas-jornadas-marco02.md",
    [
        "# Percursos essenciais e suas provas locais",
        "",
        "22 critérios da delimitação de Vigia, conservados individualmente. Este índice liga etapas, controles, identidade/revisões e arquivos de prova; os comandos/resultados executados constam no recibo MARCO02. React com Transport fictício exercita os componentes operacionais reais. Chromium é usado nos três ramos FE10, nas jornadas integradas/coletor e na fatia FE12. Nenhuma resposta preparada valida regra comercial/backend.",
        "",
        "| Critério | Percurso e fronteira observáveis | Arquivos de prova |",
        "| --- | --- | --- |",
        ...rows.map(
            (r) =>
                `| ${r.id} · ${r.title} | ${r.observed} | ${r.witnesses.map((w) => `[${w.path.replace("frontend/", "")}](../${w.path.replace("frontend/", "")})`).join("; ")} |`,
        ),
        "",
        "[Índice JSON com hashes, casos/linhas e passos requeridos](provas-jornadas-marco02.json). Não se exige nem se declara uma jornada E2E por rota. Variantes contratuais continuam oferecidas nas telas e têm focais de schema/perfil/referência; integrações e equipamentos permanecem critérios externos próprios.",
        "",
        "FE12-04 reutiliza FE08/FE11 e consultas de entrada/saída. Auditoria Gestor está em tests/stock-queries.test.tsx, caso FE12-04, sem relatório ou filtro inventado. A revisão independente R14 seis casos é histórica compartilhada, não uma nova execução Cedro.",
        "",
        "Replay, falhas/null/5xx, cancel versus interrupt, ausência NON_NULL, números exatos, XML diferido, resposta antiga de leitura/contexto, duplo envio, perfil condicional e teclado são provas comuns dos testes de fronteira e navegador. Os logs vermelhos anteriores permanecem próprios; o recibo final qualifica suas correções.",
        "",
    ].join("\n"),
);
console.log(
    "22 critérios ligados a " +
        new Set(rows.flatMap((r) => r.witnesses.map((w) => w.path))).size +
        " arquivos de prova compartilhados",
);
