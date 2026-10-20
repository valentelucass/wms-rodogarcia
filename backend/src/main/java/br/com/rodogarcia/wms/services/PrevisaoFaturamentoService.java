package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.VisaoOperacaoDto;
import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.VisaoOperacaoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Estimativa por ciclo contratado; não cria cobrança, fechamento ou documento fiscal. */
@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class PrevisaoFaturamentoService {
    private final ContratoCobrancaRepository contratos;
    private final VisaoOperacaoRepository fontes;
    private final CalculoCobrancaService calculos;
    private final ContextoCobrancaService contextos;
    private final AcessoService acesso;

    public PrevisaoFaturamentoService(
            ContratoCobrancaRepository contratos,
            VisaoOperacaoRepository fontes,
            CalculoCobrancaService calculos,
            ContextoCobrancaService contextos,
            AcessoService acesso) {
        this.contratos = contratos;
        this.fontes = fontes;
        this.calculos = calculos;
        this.contextos = contextos;
        this.acesso = acesso;
    }

    public VisaoOperacaoDto.PrevisaoFaturamento consultar(
            List<Long> clientes, List<Long> armazens, Instant referencia, ZoneId zonaPainel) {
        acesso.exigirSupervisor();
        clientes.forEach(acesso::cliente);
        armazens.forEach(acesso::armazem);
        var competencia = YearMonth.from(referencia.atZone(zonaPainel));
        var periodos = new ArrayList<VisaoOperacaoDto.PeriodoPrevisao>();
        var pendencias = new ArrayList<VisaoOperacaoDto.PendenciaPrevisao>();
        if (clientes.isEmpty() || armazens.isEmpty())
            return new VisaoOperacaoDto.PrevisaoFaturamento(
                    competencia.toString(),
                    referencia,
                    null,
                    false,
                    true,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    List.of(),
                    List.of());
        var configurados = contratos.paraPrevisao(clientes, armazens);
        Map<VisaoOperacaoRepository.ContextoFinanceiro, List<ContratoCobranca>> porContexto =
                new LinkedHashMap<>();
        for (var contrato : configurados) {
            var chave =
                    new VisaoOperacaoRepository.ContextoFinanceiro(
                            contrato.getCliente().getId(), contrato.getArmazem().getId());
            porContexto.computeIfAbsent(chave, k -> new ArrayList<>()).add(contrato);
        }
        var atividades =
                fontes.atividadesPrevisao(
                        clientes,
                        armazens,
                        competencia.atDay(1).atStartOfDay(zonaPainel).toInstant(),
                        referencia);
        var ativos =
                atividades.stream()
                        .map(VisaoOperacaoRepository.AtividadeFinanceira::contexto)
                        .distinct()
                        .toList();
        for (var contextoAmbiguo :
                atividades.stream()
                        .filter(VisaoOperacaoRepository.AtividadeFinanceira::historicoAmbiguo)
                        .map(VisaoOperacaoRepository.AtividadeFinanceira::contexto)
                        .distinct()
                        .toList())
            pendencias.add(
                    new VisaoOperacaoDto.PendenciaPrevisao(
                            contextoAmbiguo.clienteId(),
                            contextoAmbiguo.armazemId(),
                            "HISTORICO_OPERACIONAL_INSUFICIENTE",
                            null,
                            null,
                            null,
                            "Identidade física inativa sem fim cobrável datado comprovado. "
                                    + "Reconcilie o histórico antes de concluir a prévia."));
        for (var fonte : ativos) porContexto.computeIfAbsent(fonte, k -> new ArrayList<>());
        for (var contexto : porContexto.entrySet()) {
            var chave = contexto.getKey();
            // Revalidar também quando chamado por outro consumidor além do painel.
            contextos.autorizar(chave.clienteId(), chave.armazemId());
            if (contexto.getValue().isEmpty()) {
                if (atividades.stream()
                        .anyMatch(a -> a.contexto().equals(chave) && !a.historicoAmbiguo()))
                    pendencias.add(
                            new VisaoOperacaoDto.PendenciaPrevisao(
                                    chave.clienteId(),
                                    chave.armazemId(),
                                    "CONTRATO_AUSENTE",
                                    null,
                                    null,
                                    null,
                                    "Há atividade comprovada, mas contrato, fuso, mínimo e GRIS não estão configurados."));
                continue;
            }
            conferirCobertura(
                    chave,
                    contexto.getValue(),
                    atividades,
                    competencia,
                    referencia,
                    zonaPainel,
                    pendencias);
            for (var contrato : contexto.getValue())
                apurar(
                        contrato,
                        contexto.getValue(),
                        competencia,
                        referencia,
                        periodos,
                        pendencias);
        }
        var moedas =
                periodos.stream().map(VisaoOperacaoDto.PeriodoPrevisao::moeda).distinct().toList();
        if (moedas.size() > 1)
            pendencias.add(
                    new VisaoOperacaoDto.PendenciaPrevisao(
                            null,
                            null,
                            "MOEDAS_DIVERGENTES",
                            null,
                            null,
                            null,
                            "Os períodos usam moedas diferentes; não há soma ou conversão automática."));
        boolean completo =
                pendencias.isEmpty() && periodos.stream().allMatch(p -> p.total() != null);
        BigDecimal subtotal =
                periodos.stream()
                        .map(VisaoOperacaoDto.PeriodoPrevisao::subtotalConhecido)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total =
                completo
                        ? periodos.stream()
                                .map(VisaoOperacaoDto.PeriodoPrevisao::total)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                        : null;
        return new VisaoOperacaoDto.PrevisaoFaturamento(
                competencia.toString(),
                referencia,
                moedas.size() == 1 ? moedas.getFirst() : null,
                periodos.stream().anyMatch(VisaoOperacaoDto.PeriodoPrevisao::incluiDiaAtual),
                completo,
                moedas.size() > 1 ? null : completo ? total : subtotal,
                total,
                List.copyOf(periodos),
                List.copyOf(pendencias));
    }

    private void conferirCobertura(
            VisaoOperacaoRepository.ContextoFinanceiro contexto,
            List<ContratoCobranca> configurados,
            List<VisaoOperacaoRepository.AtividadeFinanceira> atividades,
            YearMonth competencia,
            Instant referencia,
            ZoneId zonaPainel,
            List<VisaoOperacaoDto.PendenciaPrevisao> pendencias) {
        var descobertos = new java.util.TreeSet<LocalDate>();
        var ordenados =
                configurados.stream()
                        .sorted(
                                java.util.Comparator.comparing(
                                        c ->
                                                c.getVigenciaInicio()
                                                        .atStartOfDay(ZoneId.of(c.getFuso()))
                                                        .toInstant()))
                        .toList();
        var inicioMes = competencia.atDay(1).atStartOfDay(zonaPainel).toInstant();
        for (var fonte : atividades) {
            if (!fonte.contexto().equals(contexto) || fonte.historicoAmbiguo()) continue;
            Instant cursor = fonte.inicio().isBefore(inicioMes) ? inicioMes : fonte.inicio();
            Instant fim =
                    fonte.fim() == null || fonte.fim().isAfter(referencia)
                            ? referencia
                            : fonte.fim();
            if (cursor.isAfter(fim)) continue;
            boolean ponto = fonte.inicio().equals(fonte.fim());
            boolean coberto = false;
            for (var c : ordenados) {
                var zonaContrato = ZoneId.of(c.getFuso());
                var inicioContrato = c.getVigenciaInicio().atStartOfDay(zonaContrato).toInstant();
                var fimContrato =
                        c.getVigenciaFim() == null
                                ? Instant.MAX
                                : c.getVigenciaFim().atStartOfDay(zonaContrato).toInstant();
                if (!fimContrato.isAfter(cursor)) continue;
                if (inicioContrato.isAfter(cursor)) {
                    descobertos.add(cursor.atZone(zonaPainel).toLocalDate());
                    var fimLacuna = inicioContrato.isBefore(fim) ? inicioContrato : fim;
                    descobertos.add(fimLacuna.minusNanos(1).atZone(zonaPainel).toLocalDate());
                    if (inicioContrato.isAfter(fim)) break;
                }
                if (!inicioContrato.isAfter(cursor) || !ponto) {
                    cursor = fimContrato;
                    if (!cursor.isBefore(fim)) {
                        coberto = true;
                        break;
                    }
                }
            }
            if (!coberto) descobertos.add(fim.atZone(zonaPainel).toLocalDate());
        }
        if (!descobertos.isEmpty())
            pendencias.add(
                    new VisaoOperacaoDto.PendenciaPrevisao(
                            contexto.clienteId(),
                            contexto.armazemId(),
                            "CONTRATO_NAO_COBRE_PERIODO",
                            descobertos.getFirst(),
                            null,
                            null,
                            "Há atividade sem contrato vigente entre "
                                    + descobertos.getFirst()
                                    + " e "
                                    + descobertos.getLast()
                                    + ". A prévia permanece incompleta."));
    }

    private void apurar(
            ContratoCobranca contrato,
            List<ContratoCobranca> configurados,
            YearMonth competencia,
            Instant referencia,
            List<VisaoOperacaoDto.PeriodoPrevisao> periodos,
            List<VisaoOperacaoDto.PendenciaPrevisao> pendencias) {
        var zona = ZoneId.of(contrato.getFuso());
        var hoje = referencia.atZone(zona).toLocalDate();
        LocalDate cursor = competencia.atDay(1).minusDays(366);
        if (cursor.isBefore(contrato.getVigenciaInicio())) cursor = contrato.getVigenciaInicio();
        var limite = competencia.plusMonths(1).atDay(1);
        while (cursor.isBefore(limite)
                && !cursor.isAfter(hoje)
                && (contrato.getVigenciaFim() == null
                        || cursor.isBefore(contrato.getVigenciaFim()))) {
            var ciclo = PeriodoCobrancaService.ciclo(contrato, cursor);
            cursor = ciclo.fim();
            if (!YearMonth.from(ciclo.fim().minusDays(1)).equals(competencia)) continue;
            var inicio =
                    ciclo.inicio().isBefore(contrato.getVigenciaInicio())
                            ? contrato.getVigenciaInicio()
                            : ciclo.inicio();
            var fim = ciclo.fim().isAfter(hoje.plusDays(1)) ? hoje.plusDays(1) : ciclo.fim();
            if (contrato.getVigenciaFim() != null && contrato.getVigenciaFim().isBefore(fim))
                fim = contrato.getVigenciaFim();
            if (!inicio.isBefore(fim)) continue;
            final LocalDate inicioApurado = inicio, fimApurado = fim;
            if (configurados.stream()
                    .anyMatch(
                            c ->
                                    c != contrato
                                            && PeriodoCobrancaService.sobrepoe(
                                                    c.getVigenciaInicio(),
                                                    c.getVigenciaFim(),
                                                    inicioApurado,
                                                    fimApurado))) {
                pendencias.add(
                        new VisaoOperacaoDto.PendenciaPrevisao(
                                contrato.getCliente().getId(),
                                contrato.getArmazem().getId(),
                                "CONTRATO_AMBIGUO",
                                inicio,
                                null,
                                null,
                                "Há contratos sobrepostos no período; a prévia não duplica suas cobranças."));
                continue;
            }
            var a =
                    calculos.prever(
                            contrato.getCliente().getId(),
                            contrato.getArmazem().getId(),
                            inicio,
                            fim,
                            referencia);
            periodos.add(
                    new VisaoOperacaoDto.PeriodoPrevisao(
                            contrato.getCliente().getId(),
                            contrato.getArmazem().getId(),
                            contrato.getId(),
                            contrato.getMoeda(),
                            contrato.getFuso(),
                            ciclo.inicio(),
                            ciclo.fim(),
                            inicio,
                            fim,
                            fim.equals(hoje.plusDays(1)),
                            a.subtotalConhecido(),
                            a.minimo(),
                            a.gris(),
                            a.total(),
                            a.pendencias()));
            for (var p : a.pendencias())
                pendencias.add(
                        new VisaoOperacaoDto.PendenciaPrevisao(
                                contrato.getCliente().getId(),
                                contrato.getArmazem().getId(),
                                p.codigo(),
                                p.data(),
                                p.unidadeId(),
                                p.fatoId(),
                                p.detalhe()));
        }
    }
}
