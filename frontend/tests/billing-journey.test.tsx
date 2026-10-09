import { it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { FictitiousTransport } from "../src/api/fictitious";
import { Result } from "../src/components/Result";
import {
    Harness,
    lastMutation,
    click,
    fill,
    confirm,
    consult,
    selected,
    body,
    saveProof,
    obj,
    selectResultRow,
    wire,
} from "./support/operationalHarness";
it("FE04 FE11 serviço tabela vigência vínculo sugestão fato e cálculo com referências consultadas", async () => {
    const t = new FictitiousTransport();
    render(
        <Harness
            transport={t}
            page="precos"
            action="ConfiguracaoCobrancaController.criarServico"
        />,
    );
    fill("Código *", "FICT-SERVICO");
    fill("Descricao *", "Serviço fictício configurado");
    fill("Tipo *", "ADICIONAL");
    fill("Unidade *", "QUANTIDADE_PRODUTO");
    fill(
        "Motivo / justificativa *",
        "Configuração fictícia autorizada no exercício",
    );
    await confirm();
    await click("Configurar tabela com este serviço fictício");
    fill("Código *", "FICT-TABELA");
    fill("Descricao *", "Tabela fictícia de exercício");
    fill("Tipo *", "ESPECIFICA");
    fill("Vigencia Inicio *", "2026-10-01");
    await click("Adicionar Itens");
    selected("Itens / Item 1 / Serviço (ID) *", "2501");
    fill("Itens / Item 1 / Categoria *", "");
    fill("Itens / Item 1 / Preco", "2.500000");
    selected("Itens / Item 1 / Percentual", "");
    fill("Motivo / justificativa *", "Preço fictício informado sem percentual");
    await confirm();
    expect(
        (
            body(lastMutation(t.requests)).itens as {
                preco: string;
                percentual: null;
            }[]
        )[0],
    ).toEqual({
        servicoId: "2501",
        categoria: "",
        preco: "2.500000",
        percentual: "",
    });
    expect(wire(lastMutation(t.requests))).toContain('"percentual":null');
    await click("Consultar tabela e vigência recebidas");
    selected("Identificador *", "2601");
    await consult();
    await click("Vincular esta tabela ao cliente do contexto");
    selected("Tabela Id *", "2601");
    fill("Vigencia Inicio *", "2026-10-01");
    fill("Motivo / justificativa *", "Vínculo fictício do cliente1 armazém1");
    await confirm();
    await click("Consultar vínculo de tabela confirmado");
    await consult();
    expect(screen.getByRole("cell", { name: "2601" })).toBeInTheDocument();
    await click("Serviços e cálculo");
    await click("Consultar sugestões de serviço");
    await consult();
    await click("Selecionar registro 1");
    await click("Registrar fato");
    selected("Serviço (ID) *", "2501");
    selected("Unidade logística (ID)", "501");
    selected("Pedido de saída (ID)", "601");
    selected("Quantidade", "2.123456");
    fill(
        "Criterio Rateio *",
        "Execução fictícia vinculada à unidade e ao pedido consultados",
    );
    fill(
        "Motivo / justificativa *",
        "Execução fictícia identificada sem criar saída",
    );
    await confirm();
    expect(
        t.requests.some((r) => r.endpoint.id === "PedidoSaidaController.criar"),
    ).toBe(false);
    await click("Consultar situação e origem deste fato");
    selected("Identificador *", "2801");
    await consult();
    expect(screen.getByText("VALIDO", { exact: true })).toBeInTheDocument();
    await click("Conferir solicitação de cálculo após consulta do fato");
    fill("Início do período *", "2026-10-01");
    fill("Fim exclusivo do período *", "2026-11-01");
    fill("Motivo / justificativa *", "Cálculo fictício solicitado após o fato");
    await confirm();
    expect(
        screen.getAllByText("10.000000", { exact: true })[0],
    ).toBeInTheDocument();
    saveProof("FE04-03-FE11-01", t.requests);
}, 22000);
it("FE11 memória recebida preserva dias vigências pico avaria mínimo GRIS null zero crédito e exatidão", () => {
    const memory = obj("CalculoCobrancaDto.Memoria", {
        diarias: [
            obj("CalculoCobrancaDto.Diaria", {
                data: "2026-10-01",
                tabelaId: "2601",
                picoCobravel: "2.000000",
                equivalenciaSuspensa: "0.500000",
                valorEstoque: null,
                tarifa: "0.000000",
                valor: "-3.123456",
            }),
        ],
        servicos: [],
        ajustes: obj("CalculoCobrancaDto.Ajustes", {
            diasIncluidos: "31",
            diasNominais: "31",
            minimoAplicavel: null,
            minimoComplemento: "0.000000",
            baseGris: "9007199254740993.123456",
            valorGris: null,
        }),
    });
    render(
        <Result
            data={memory}
            type="CalculoCobrancaDto.Memoria"
            onSelect={vi.fn()}
        />,
    );
    expect(screen.getByRole("cell", { name: "2.000000" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "0.500000" })).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "-3.123456" })).toBeInTheDocument();
    expect(
        screen.getByText("9007199254740993.123456", { exact: true }),
    ).toBeInTheDocument();
    expect(
        screen.getAllByText("Não informado", { exact: true }).length,
    ).toBeGreaterThan(1);
});
it("FE11 reabertura nova versão ajuste conserva origem histórica e consulta identificada", async () => {
    const t = new FictitiousTransport();
    render(
        <Harness
            transport={t}
            page="cobranca"
            action="CalculoCobrancaController.listar"
        />,
    );
    await consult();
    await selectResultRow("Cálculos", "801");
    await click("Fechamentos e ESL");
    await consult();
    await selectResultRow("Fechamentos", "901");
    await click("1. Ciclo e versões");
    await click("Consultar versão e hash");
    await consult();
    await click("Guardar versão consultada como origem do ajuste");
    await click("2. Decisão integral do Gestor");
    await click("Reabrir ciclo");
    selected("Identificador *", "901");
    selected("Calculo Id *", "801");
    fill(
        "Motivo / justificativa *",
        "Reabertura fictícia para correção identificada",
    );
    await confirm();
    await click("Consultar a nova versão após reabertura");
    selected("Numero *", "2");
    await consult();
    expect(
        screen
            .getAllByRole("region", { name: "Origem histórica do ajuste" })
            .at(-1),
    ).toHaveTextContent("identificador 1001");
    await click("Ajustes de ciclo");
    await click("Registrar ajuste");
    selected("Origem Versao Id *", "1001");
    selected("Destino Fechamento Id *", "901");
    selected("Calculo Corrigido Id *", "801");
    fill(
        "Motivo / justificativa *",
        "Ajuste fictício identificado de versão histórica",
    );
    fill("Evidencia *", "Documento fictício e memória corrigida recebida");
    await confirm();
    const cmd = body(lastMutation(t.requests));
    expect(cmd.origemVersaoId).toBe("1001");
    expect(cmd.destinoFechamentoId).toBe("901");
    expect(cmd.calculoCorrigidoId).toBe("801");
    expect(cmd).not.toHaveProperty("delta");
    await click("Consultar ajustes identificados do fechamento");
    await consult();
    expect(screen.getByRole("cell", { name: "2901" })).toBeInTheDocument();
    saveProof("FE11-02-03", t.requests);
}, 20000);
