package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.DashboardDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.repositories.ConfiguracaoAvisoValidadeRepository;
import br.com.rodogarcia.wms.repositories.DashboardRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class DashboardService {
    private final DashboardRepository dashboard;
    private final ProdutoRepository produtos;
    private final ConfiguracaoAvisoValidadeRepository avisos;
    private final AcessoService acesso;
    private final Clock clock;

    public DashboardService(
            DashboardRepository dashboard,
            ProdutoRepository produtos,
            ConfiguracaoAvisoValidadeRepository avisos,
            AcessoService acesso,
            Clock clock) {
        this.dashboard = dashboard;
        this.produtos = produtos;
        this.avisos = avisos;
        this.acesso = acesso;
        this.clock = clock;
    }

    public DashboardDto.Resumo consultar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotBlank String fuso,
            int pagina,
            int tamanho) {
        acesso.cliente(clienteId);
        acesso.armazem(armazemId);
        var paginacao = CadastroSupport.pagina(pagina, tamanho);
        if (tamanho > 12) throw CadastroSupport.invalido("Consulte até 12 produtos por página.");
        ZoneId zona;
        try {
            zona = ZoneId.of(fuso);
        } catch (RuntimeException e) {
            throw CadastroSupport.invalido("Informe fuso civil válido.");
        }
        Instant agora = Instant.now(clock);
        boolean gestor = acesso.gestor();
        var ocupacao =
                dashboard.ocupacaoCliente(clienteId, armazemId).stream()
                        .collect(
                                Collectors.toMap(
                                        DashboardRepository.AreaCliente::getTipo,
                                        DashboardRepository.AreaCliente::getPosicoes));
        var capacidade =
                gestor
                        ? dashboard.capacidadeArmazem(armazemId).stream()
                                .collect(
                                        Collectors.toMap(
                                                DashboardRepository.AreaArmazem::getTipo,
                                                Function.identity()))
                        : Map.<TipoEndereco, DashboardRepository.AreaArmazem>of();
        var areas =
                Arrays.stream(TipoEndereco.values())
                        .map(
                                tipo -> {
                                    var c = capacidade.get(tipo);
                                    return new DashboardDto.Area(
                                            tipo,
                                            ocupacao.getOrDefault(tipo, 0L),
                                            gestor ? (c == null ? 0L : c.getCapacidade()) : null,
                                            gestor
                                                    ? (c == null
                                                            ? 0L
                                                            : c.getCapacidade() - c.getOcupadas())
                                                    : null);
                                })
                        .toList();
        var contagens =
                dashboard.fila(clienteId, armazemId).stream()
                        .collect(
                                Collectors.toMap(
                                        DashboardRepository.Fila::getSituacao,
                                        DashboardRepository.Fila::getPedidos));
        var fila =
                List.of(
                                SituacaoPedidoSaida.RASCUNHO,
                                SituacaoPedidoSaida.RESERVADO,
                                SituacaoPedidoSaida.EM_SEPARACAO,
                                SituacaoPedidoSaida.SEPARADO)
                        .stream()
                        .map(s -> new DashboardDto.Fila(s, contagens.getOrDefault(s, 0L)))
                        .toList();
        var config = avisos.findByClienteIdAndArmazemId(clienteId, armazemId).orElse(null);
        Integer antecedencia = config == null ? null : config.getDiasAntecedencia();
        Long quantidadeAvisos =
                antecedencia == null
                        ? null
                        : dashboard.unidadesComAviso(
                                clienteId,
                                armazemId,
                                agora.atZone(zona).toLocalDate().plusDays(antecedencia));
        var page =
                produtos.findByClienteId(
                        clienteId,
                        PageRequest.of(
                                paginacao.getPageNumber(),
                                paginacao.getPageSize(),
                                Sort.by("sku").and(Sort.by("id"))));
        var ids = page.getContent().stream().map(p -> p.getId()).toList();
        var fisico =
                quantidades(
                        ids.isEmpty() ? List.of() : dashboard.fisico(clienteId, armazemId, ids));
        var disponivel =
                quantidades(
                        ids.isEmpty()
                                ? List.of()
                                : dashboard.disponivel(clienteId, armazemId, ids));
        var reservado =
                quantidades(
                        ids.isEmpty() ? List.of() : dashboard.reservado(clienteId, armazemId, ids));
        var pendente =
                quantidades(
                        ids.isEmpty() ? List.of() : dashboard.pendente(clienteId, armazemId, ids));
        var saldo =
                PaginaResponse.de(
                        page,
                        p -> {
                            var f = fisico.getOrDefault(p.getId(), BigDecimal.ZERO);
                            var d = disponivel.getOrDefault(p.getId(), BigDecimal.ZERO);
                            var r = reservado.getOrDefault(p.getId(), BigDecimal.ZERO);
                            var n = pendente.getOrDefault(p.getId(), BigDecimal.ZERO);
                            return new DashboardDto.Produto(
                                    p.getId(),
                                    p.getSku(),
                                    p.getUnidadeMedida(),
                                    f.add(n),
                                    d,
                                    r,
                                    f.subtract(d).subtract(r),
                                    n);
                        });
        return new DashboardDto.Resumo(
                clienteId,
                armazemId,
                agora,
                zona.getId(),
                areas.stream().mapToLong(DashboardDto.Area::posicoesCliente).sum(),
                dashboard.unidadesDisponiveis(clienteId, armazemId),
                fila.stream().mapToLong(DashboardDto.Fila::pedidos).sum(),
                quantidadeAvisos,
                antecedencia,
                gestor,
                areas,
                fila,
                saldo);
    }

    private Map<Long, BigDecimal> quantidades(List<DashboardRepository.Quantidade> valores) {
        return valores.stream()
                .collect(
                        Collectors.toMap(
                                DashboardRepository.Quantidade::getProdutoId,
                                DashboardRepository.Quantidade::getQuantidade));
    }
}
