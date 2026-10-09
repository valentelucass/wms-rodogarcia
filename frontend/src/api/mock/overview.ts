import type { Request } from "../client";
import type { Perfil } from "../../contracts/runtime";
import type {
    VisaoOperacaoDto_Posicao,
    VisaoOperacaoDto_Resumo,
    VisaoOperacaoDto_Detalhe,
} from "../../contracts/types";
import { demoCode } from "./constants";

/** Dados exclusivos do exercício fictício. A aplicação real nunca importa este transporte. */
export const overviewPositions: VisaoOperacaoDto_Posicao[] = [
    "Centro fictício",
    "Anexo fictício",
].flatMap((armazem, w) =>
    Array.from({ length: w ? 24 : 80 }, (_, i) => ({
        id: String(1000 + w * 100 + i),
        armazemId: String(w + 1),
        armazem,
        codigo: `${w + 1}-${i < 40 ? "A" : "B"}-${Math.floor((i % 40) / 10) + 1}-${String((i % 10) + 1).padStart(2, "0")}`,
        rua: i < 40 ? "A" : "B",
        nivel: Math.floor((i % 40) / 10) + 1,
        posicao: String((i % 10) + 1).padStart(2, "0"),
        tipo: "ARMAZENAGEM",
        situacao: "ATIVO",
        estado: !w && i < 2 ? "OCUPADO" : "DISPONIVEL",
        disponivel: !!w || i >= 2,
        ocupada: !w && i < 2,
        bloqueada: false,
        reservada: !w && i < 2,
        quarentena: false,
        capacidadePesoKg: "1000.00",
    })),
);
export function overviewExample(
    r: Request,
    perfil: Perfil,
    empty: boolean,
): VisaoOperacaoDto_Resumo | VisaoOperacaoDto_Detalhe {
    if (r.endpoint.id === "VisaoOperacaoController.detalhe") {
        const p = overviewPositions.find((p) => p.id === String(r.params.id));
        if (!p) throw new Error("Endereço fictício inexistente.");
        return {
            endereco: p,
            unidadesVisiveis: p.ocupada ? "1" : "0",
            conteudoRestrito: false,
            alturaMetros: null,
            larguraMetros: null,
            profundidadeMetros: null,
            empilhamentoMaximo: null,
            tipoUnidadePermitido: null,
            unidades: p.ocupada
                ? [
                      {
                          id: "1",
                          clienteId: "1",
                          codigo: demoCode,
                          produto: "Mercadoria fictícia",
                          sku: "DEMO-CAIXA",
                          lote: "DEMO-01",
                          quantidade: "10.000000",
                          unidadeMedida: "UN",
                          pedidoEntradaId: "1",
                          bloqueada: false,
                          reservada: true,
                          quarentena: false,
                          ultimaMovimentacao: "2026-10-09T12:00:00Z",
                      },
                  ]
                : [],
        };
    }
    const all = empty
        ? []
        : overviewPositions.filter(
              (p) =>
                  !r.query.armazemId ||
                  p.armazemId === String(r.query.armazemId),
          );
    const occupied = all.filter((p) => p.ocupada).length;
    const financial = perfil !== "OPERACAO",
        own = !r.query.clienteId || String(r.query.clienteId) === "1";
    const filtered = all.filter(
        (p) =>
            (!r.query.codigo ||
                p.codigo
                    .toLowerCase()
                    .includes(String(r.query.codigo).toLowerCase())) &&
            (r.query.estado === "DISPONIVEL"
                ? p.disponivel
                : r.query.estado === "OCUPADO"
                  ? p.ocupada
                  : true),
    );
    const pagina = Number(r.query.pagina ?? 0),
        tamanho = Number(r.query.tamanho ?? 100);
    return {
        clienteId: r.query.clienteId ? String(r.query.clienteId) : null,
        armazemId: r.query.armazemId ? String(r.query.armazemId) : null,
        consultadoEm: new Date().toISOString(),
        fuso: String(r.query.fuso ?? "America/Sao_Paulo"),
        capacidade: String(all.length),
        posicoesOcupadas: String(occupied),
        posicoesLivres: String(all.length - occupied),
        ocupacao: String(
            all.length ? Math.round((100 * occupied) / all.length) : 0,
        ),
        unidadesArmazenadas: own && occupied ? "1" : "0",
        valorArmazenado: financial
            ? own && occupied
                ? "12500.50"
                : "0.00"
            : null,
        valorCompleto: financial,
        emQuarentena: "0",
        reservasAtivas: own && occupied ? "1" : "0",
        entradasAbertas: empty ? "0" : "2",
        saidasAbertas: own && occupied ? "1" : "0",
        faturamentoMes: financial ? (empty ? "0.00" : "2345.00") : null,
        faturamentoParcial: true,
        financeiroPermitido: financial,
        competencia: new Date().toISOString().slice(0, 7),
        mapa: {
            itens: filtered.slice(pagina * tamanho, (pagina + 1) * tamanho),
            pagina,
            tamanho,
            totalItens: String(filtered.length),
            totalPaginas: Math.ceil(filtered.length / tamanho),
        },
    };
}
