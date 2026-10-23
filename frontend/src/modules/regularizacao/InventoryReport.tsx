import { useState } from "react";
import type { InventarioDto_Relatorio } from "../../contracts/types";
import { Pagination } from "../../components/layout/Pagination";
import { inventoryDate, inventoryQuantity as quantity } from "./inventoryApi";

const reasons: Record<string, string> = {
    NAO_LIDA: "Não lida",
    QUANTIDADE: "Quantidade diferente",
    ENDERECO: "Posição diferente",
    FORA_BASE: "Mercadoria fora do estoque de referência",
    MOVIMENTACAO: "Movimentação durante a contagem",
};
const csvText = (value: unknown) =>
    '"' +
    String(value ?? "")
        .replace(/^[=+\-@\t\r]/, (c) => "'" + c)
        .replaceAll('"', '""') +
    '"';
export function InventoryReport({
    report,
}: {
    report: InventarioDto_Relatorio;
}) {
    const [view, setView] = useState("sku");
    const [page, setPage] = useState(0);
    const [onlyDifferences, setOnlyDifferences] = useState(false);
    const rows = (report.mercadorias ?? []).filter(
        (row) => !onlyDifferences || (row.ocorrencias ?? []).length > 0,
    );
    const grouped = report.porSku ?? [];
    const count = view === "sku" ? grouped.length : rows.length;
    function exportCsv() {
        const table = [
            [
                "Etiqueta",
                "SKU",
                "Produto",
                "Unidade",
                "Esperado",
                "Contado",
                "Diferença",
                "Posições esperadas",
                "Posição encontrada",
                "Ocorrências",
            ],
            ...(report.mercadorias ?? []).map((r) => [
                r.codigoUnidade,
                r.sku,
                r.descricao,
                r.unidadeMedida,
                r.esperado,
                r.contado,
                r.diferenca,
                r.posicoesEsperadas,
                r.posicaoEncontrada,
                (r.ocorrencias ?? []).map((x) => reasons[x] ?? x).join(" / "),
            ]),
        ];
        const url = URL.createObjectURL(
            new Blob(
                [
                    "\ufeff" +
                        table
                            .map((row) => row.map(csvText).join(";"))
                            .join("\r\n"),
                ],
                { type: "text/csv;charset=utf-8" },
            ),
        );
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = `inventario-${report.inventario?.id ?? "relatorio"}.csv`;
        anchor.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    }
    return (
        <div className="inventory-report">
            <div className="inventory-heading">
                <div>
                    <h3>Resultado da conferência</h3>
                    <p>
                        Estoque de referência em{" "}
                        {inventoryDate(report.referenciaEstoque)}. Encerrado em{" "}
                        {inventoryDate(report.inventario?.encerradoEm)}.
                    </p>
                </div>
                <button type="button" onClick={exportCsv}>
                    Baixar relatório
                </button>
            </div>
            <div className="inventory-summary-cards">
                <div>
                    <span>Mercadorias lidas</span>
                    <strong>{report.mercadoriasConferidas}</strong>
                </div>
                <div>
                    <span>Não lidas</span>
                    <strong>{report.mercadoriasNaoLidas}</strong>
                </div>
                <div>
                    <span>Diferenças de quantidade</span>
                    <strong>{report.divergenciasQuantidade}</strong>
                </div>
                <div>
                    <span>Diferenças de posição</span>
                    <strong>{report.divergenciasEndereco}</strong>
                </div>
            </div>
            {report.movimentadasDuranteContagem > 0 && (
                <p className="inventory-warning" role="status">
                    Houve movimentação em {report.movimentadasDuranteContagem}{" "}
                    mercadorias durante a contagem. Confira essas ocorrências e
                    faça uma nova contagem antes de solicitar ajustes.
                </p>
            )}
            <p className="hint">
                Este relatório registra a conferência. Ajustes de estoque e de
                endereço dependem da análise do responsável.
            </p>
            <div className="inventory-report-controls">
                <div className="button-row" aria-label="Visão do relatório">
                    <button
                        type="button"
                        aria-pressed={view === "sku"}
                        onClick={() => {
                            setView("sku");
                            setPage(0);
                        }}
                    >
                        Por SKU
                    </button>
                    <button
                        type="button"
                        aria-pressed={view === "items"}
                        onClick={() => {
                            setView("items");
                            setPage(0);
                        }}
                    >
                        Mercadorias e posições
                    </button>
                </div>
                {view === "items" && (
                    <label className="inventory-checkbox">
                        <input
                            type="checkbox"
                            checked={onlyDifferences}
                            onChange={(e) => {
                                setOnlyDifferences(e.target.checked);
                                setPage(0);
                            }}
                        />
                        Somente divergências
                    </label>
                )}
            </div>
            <div className="inventory-table-scroll">
                <table
                    className="inventory-table"
                    aria-label="Resultado do inventário"
                >
                    <thead>
                        {view === "sku" ? (
                            <tr>
                                <th>Produto / SKU</th>
                                <th>Unidade</th>
                                <th>Esperado</th>
                                <th>Contado</th>
                                <th>Diferença</th>
                                <th>Mercadorias com ocorrências</th>
                            </tr>
                        ) : (
                            <tr>
                                <th>Mercadoria / SKU</th>
                                <th>Esperado</th>
                                <th>Contado</th>
                                <th>Diferença</th>
                                <th>Posições esperadas</th>
                                <th>Posição encontrada</th>
                                <th>Ocorrências</th>
                            </tr>
                        )}
                    </thead>
                    <tbody>
                        {view === "sku"
                            ? grouped
                                  .slice(page * 50, page * 50 + 50)
                                  .map((r) => (
                                      <tr key={String(r.produtoId)}>
                                          <td>
                                              <strong>{r.descricao}</strong>
                                              <small>{r.sku}</small>
                                          </td>
                                          <td>{r.unidadeMedida}</td>
                                          <td>{quantity(r.esperado)}</td>
                                          <td>{quantity(r.contado)}</td>
                                          <td>{quantity(r.diferenca)}</td>
                                          <td>{r.mercadoriasDivergentes}</td>
                                      </tr>
                                  ))
                            : rows.slice(page * 50, page * 50 + 50).map((r) => (
                                  <tr key={r.codigoUnidade}>
                                      <td>
                                          <strong>
                                              {r.sku} · {r.descricao}
                                          </strong>
                                          <small>{r.codigoUnidade}</small>
                                      </td>
                                      <td>
                                          {quantity(r.esperado)}{" "}
                                          {r.unidadeMedida}
                                      </td>
                                      <td>
                                          {quantity(r.contado)}{" "}
                                          {r.unidadeMedida}
                                      </td>
                                      <td>{quantity(r.diferenca)}</td>
                                      <td>
                                          {r.posicoesEsperadas ||
                                              "Sem endereço"}
                                      </td>
                                      <td>
                                          {r.posicaoEncontrada ||
                                              "Sem endereço informado"}
                                      </td>
                                      <td>
                                          {(r.ocorrencias ?? []).length
                                              ? (r.ocorrencias ?? [])
                                                    .map((x) => reasons[x] ?? x)
                                                    .join(" · ")
                                              : "Conferida"}
                                      </td>
                                  </tr>
                              ))}
                    </tbody>
                </table>
            </div>
            <Pagination
                page={page}
                pages={Math.ceil(count / 50)}
                total={count}
                count={Math.min(50, Math.max(0, count - page * 50))}
                size={50}
                onPage={setPage}
            />
        </div>
    );
}
