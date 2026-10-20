package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.FatoServicoDto;
import br.com.rodogarcia.wms.models.BaixaSaida;
import br.com.rodogarcia.wms.models.RetiradaSaida;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.RetiradaSaidaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fontes físicas da prévia; confirmar a consulta nunca registra um fato financeiro. */
@Service
@Transactional(
        readOnly = true,
        noRollbackFor = br.com.rodogarcia.wms.exceptions.RegraNegocioException.class)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FontesPrevisaoServicoService {
    private final RetiradaSaidaRepository retiradas;
    private final BaixaSaidaRepository baixas;
    private final FatoServicoService fatos;

    public FontesPrevisaoServicoService(
            RetiradaSaidaRepository retiradas,
            BaixaSaidaRepository baixas,
            FatoServicoService fatos) {
        this.retiradas = retiradas;
        this.baixas = baixas;
        this.fatos = fatos;
    }

    record Evento(
            String tipo,
            Instant instante,
            UnidadeLogistica unidade,
            RetiradaSaida retirada,
            List<BaixaSaida> baixas) {
        List<Long> produtos(ServicoCobranca servico) {
            if (unidade != null || !"QUANTIDADE_PRODUTO".equals(servico.getUnidade()))
                return java.util.Collections.singletonList(null);
            return baixas.stream()
                    .map(b -> b.getReserva().getUnidade().getProduto().getId())
                    .distinct()
                    .sorted()
                    .toList();
        }

        String chave(Long produto) {
            return unidade != null
                    ? "ENTRADA:UNIDADE:" + unidade.getId()
                    : "SAIDA:RETIRADA:"
                            + retirada.getId()
                            + (produto == null ? "" : ":SKU:" + produto);
        }

        Set<String> categorias(Long produto) {
            return unidade != null
                    ? Set.of(unidade.getTipo().name())
                    : baixas.stream()
                            .filter(
                                    b ->
                                            produto == null
                                                    || b.getReserva()
                                                            .getUnidade()
                                                            .getProduto()
                                                            .getId()
                                                            .equals(produto))
                            .map(b -> b.getReserva().getUnidade().getTipo().name())
                            .collect(Collectors.toSet());
        }
    }

    List<Evento> eventos(
            Long cliente,
            Long armazem,
            LocalDate inicio,
            LocalDate fim,
            ZoneId zona,
            Instant referencia,
            List<UnidadeLogistica> unidades) {
        Instant desde = inicio.atStartOfDay(zona).toInstant();
        Instant ate = fim.atStartOfDay(zona).toInstant();
        var eventos = new ArrayList<Evento>();
        for (var unidade : unidades) {
            var instante = unidade.getPrimeiroEnderecamentoEm();
            if (instante != null
                    && !instante.isBefore(desde)
                    && instante.isBefore(ate)
                    && !instante.isAfter(referencia))
                eventos.add(new Evento("ENTRADA", instante, unidade, null, List.of()));
        }
        for (var retirada : retiradas.paraPrevisao(cliente, armazem, desde, ate, referencia))
            eventos.add(
                    new Evento(
                            "SAIDA",
                            retirada.getRetiradaEm(),
                            null,
                            retirada,
                            baixas.buscarDoPedido(retirada.getPedido().getId())));
        return List.copyOf(eventos);
    }

    FatoServicoService.Execucao comprovar(
            Long cliente, Long armazem, Evento evento, ServicoCobranca servico, Long produto) {
        return fatos.projetarExecucao(
                servico,
                new FatoServicoDto.Registrar(
                        null,
                        cliente,
                        armazem,
                        servico.getId(),
                        "SUGESTAO",
                        evento.unidade() == null ? null : evento.unidade().getId(),
                        null,
                        evento.retirada() == null ? null : evento.retirada().getPedido().getId(),
                        produto,
                        null,
                        null,
                        null,
                        "",
                        null,
                        List.of(),
                        "Rateio das origens físicas comprovadas",
                        "Prévia sem persistência",
                        null));
    }
}
