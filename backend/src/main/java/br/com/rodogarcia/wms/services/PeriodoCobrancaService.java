package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.models.ContratoCobranca;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public class PeriodoCobrancaService {
    private PeriodoCobrancaService() {}

    public record Ciclo(LocalDate inicio, LocalDate fim) {
        public long dias() {
            return ChronoUnit.DAYS.between(inicio, fim);
        }
    }

    public static Ciclo ciclo(ContratoCobranca c, LocalDate data) {
        if ("DIAS_CORRIDOS".equals(c.getModalidadeCiclo())) {
            long dias = ChronoUnit.DAYS.between(c.getVigenciaInicio(), data);
            long numero = Math.floorDiv(dias, c.getDuracaoDias());
            LocalDate inicio = c.getVigenciaInicio().plusDays(numero * c.getDuracaoDias());
            return new Ciclo(inicio, inicio.plusDays(c.getDuracaoDias()));
        }
        YearMonth mes = YearMonth.from(data);
        LocalDate corte = corte(mes, c.getDiaCorte());
        if (corte.isAfter(data)) mes = mes.minusMonths(1);
        return new Ciclo(corte(mes, c.getDiaCorte()), corte(mes.plusMonths(1), c.getDiaCorte()));
    }

    private static LocalDate corte(YearMonth mes, int dia) {
        return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
    }

    public static boolean vigente(LocalDate inicio, LocalDate fim, LocalDate data) {
        return !data.isBefore(inicio) && (fim == null || data.isBefore(fim));
    }

    public static boolean sobrepoe(LocalDate a, LocalDate b, LocalDate c, LocalDate d) {
        return (b == null || c.isBefore(b)) && (d == null || a.isBefore(d));
    }
}
