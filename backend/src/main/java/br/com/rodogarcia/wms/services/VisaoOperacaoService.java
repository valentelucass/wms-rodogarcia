package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.VisaoOperacaoDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.CadastroBase;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.VisaoOperacaoRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class VisaoOperacaoService {
    private final VisaoOperacaoRepository repository;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final AcessoService acesso;
    private final IndicadorEstoqueService indicadores;
    private final Clock clock;

    public VisaoOperacaoService(
            VisaoOperacaoRepository repository,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            AcessoService acesso,
            IndicadorEstoqueService indicadores,
            Clock clock) {
        this.repository = repository;
        this.clientes = clientes;
        this.armazens = armazens;
        this.acesso = acesso;
        this.indicadores = indicadores;
        this.clock = clock;
    }

    private List<Long> clientes(Long id) {
        if (id != null) {
            acesso.cliente(id);
            if (!clientes.existsById(id)) throw RegraNegocioException.naoEncontrado();
            return List.of(id);
        }
        return acesso.gestor()
                ? clientes.findAll().stream().map(CadastroBase::getId).toList()
                : acesso.clientes();
    }

    private List<Long> armazens(Long id) {
        if (id != null) {
            acesso.armazem(id);
            if (!armazens.existsById(id)) throw RegraNegocioException.naoEncontrado();
            return List.of(id);
        }
        return acesso.gestor()
                ? armazens.findAll().stream().map(CadastroBase::getId).toList()
                : acesso.armazens();
    }

    private boolean financeiro() {
        return acesso.supervisor();
    }

    public VisaoOperacaoDto.Resumo consultar(
            @Positive Long clienteId,
            @Positive Long armazemId,
            @NotBlank String fuso,
            String codigo,
            String estado,
            int pagina,
            int tamanho) {
        var page = CadastroSupport.pagina(pagina, tamanho);

        codigo = codigo == null ? "" : codigo.trim();
        if (codigo.length() > 40 || !List.of("TODAS", "DISPONIVEL", "OCUPADO").contains(estado))
            throw CadastroSupport.invalido("Informe código de até 40 caracteres e estado válido.");
        ZoneId zona;
        try {
            zona = ZoneId.of(fuso);
        } catch (RuntimeException e) {
            throw CadastroSupport.invalido("Informe fuso civil válido.");
        }
        var cs = clientes(clienteId);
        var as = armazens(armazemId);
        boolean vazio = cs.isEmpty() || as.isEmpty();
        boolean financeiro = financeiro();
        Instant agora = Instant.now(clock);
        var competencia = YearMonth.from(agora.atZone(zona));
        long capacidade = as.isEmpty() ? 0 : repository.capacidade(as),
                ocupadas = as.isEmpty() ? 0 : repository.ocupadas(as),
                livres = as.isEmpty() ? 0 : repository.livres(as);
        BigDecimal ocupacao =
                capacidade == 0
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(ocupadas)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(capacidade), 0, RoundingMode.HALF_UP);
        BigDecimal valor = financeiro ? BigDecimal.ZERO : null;
        boolean completo = financeiro;
        if (financeiro && !vazio) {
            valor = indicadores.valorArmazenado(cs, as, agora, zona);
            completo = valor != null;
        }
        if (!completo) valor = null;
        BigDecimal faturamento = financeiro ? BigDecimal.ZERO : null;
        if (financeiro && !vazio) {
            var total =
                    repository.faturamento(
                            cs,
                            as,
                            competencia.atDay(1).atStartOfDay(zona).toInstant(),
                            competencia.plusMonths(1).atDay(1).atStartOfDay(zona).toInstant());
            if (total != null) faturamento = total;
            if (repository.faturamentoIncompleto(
                    cs,
                    as,
                    competencia.atDay(1).atStartOfDay(zona).toInstant(),
                    competencia.plusMonths(1).atDay(1).atStartOfDay(zona).toInstant()))
                faturamento = null;
        }
        long total = as.isEmpty() ? 0 : repository.totalMapa(as, codigo, estado);
        var celulas =
                as.isEmpty()
                        ? List.<VisaoOperacaoRepository.Celula>of()
                        : repository.mapa(
                                as, codigo, estado, Math.toIntExact(page.getOffset()), tamanho);
        var permitidos = Set.copyOf(cs);
        var idsVisiveis =
                celulas.stream()
                        .map(VisaoOperacaoRepository.Celula::unidade)
                        .filter(
                                u ->
                                        u != null
                                                && permitidos.contains(
                                                        u.getPedido().getCliente().getId()))
                        .map(UnidadeLogistica::getId)
                        .distinct()
                        .toList();
        var reservadas =
                idsVisiveis.isEmpty()
                        ? Set.<Long>of()
                        : Set.copyOf(repository.reservadas(idsVisiveis));
        var mapa =
                celulas.stream()
                        .map(
                                c ->
                                        posicao(
                                                c,
                                                permitidos,
                                                c.unidade() != null
                                                        && reservadas.contains(
                                                                c.unidade().getId())))
                        .toList();
        return new VisaoOperacaoDto.Resumo(
                clienteId,
                armazemId,
                agora,
                zona.getId(),
                capacidade,
                ocupadas,
                livres,
                ocupacao,
                vazio ? 0 : repository.unidades(cs, as),
                valor,
                completo,
                vazio ? 0 : repository.quarentena(cs, as),
                vazio ? 0 : repository.reservas(cs, as),
                vazio ? 0 : repository.entradas(cs, as),
                vazio ? 0 : repository.saidas(cs, as),
                faturamento,
                true,
                financeiro,
                competencia.toString(),
                new PaginaResponse<>(
                        mapa,
                        pagina,
                        tamanho,
                        total,
                        Math.toIntExact((total + tamanho - 1) / tamanho)));
    }

    private VisaoOperacaoDto.Posicao posicao(
            VisaoOperacaoRepository.Celula c, Set<Long> permitidos, boolean reservada) {
        var e = c.endereco();
        var u = c.unidade();
        boolean visivel = u != null && permitidos.contains(u.getPedido().getCliente().getId());
        boolean ocupada = u != null;
        boolean disponivel =
                !ocupada
                        && e.getSituacao() == SituacaoCadastro.ATIVO
                        && e.getArmazem().getSituacao() == SituacaoCadastro.ATIVO
                        && e.getTipo() == TipoEndereco.ARMAZENAGEM;
        String estado =
                ocupada
                        ? "OCUPADO"
                        : disponivel
                                ? "DISPONIVEL"
                                : e.getSituacao() != SituacaoCadastro.ATIVO
                                                || e.getArmazem().getSituacao()
                                                        != SituacaoCadastro.ATIVO
                                        ? "INATIVO"
                                        : e.getTipo().name();
        return new VisaoOperacaoDto.Posicao(
                e.getId(),
                e.getArmazem().getId(),
                e.getArmazem().getNome(),
                e.getCodigo(),
                e.getRua(),
                e.getNivel(),
                e.getPosicao(),
                e.getTipo(),
                e.getSituacao(),
                estado,
                disponivel,
                ocupada,
                visivel && u.isBloqueada(),
                visivel && reservada,
                e.getTipo() == TipoEndereco.QUARENTENA,
                e.getCapacidadePesoKg());
    }

    public VisaoOperacaoDto.Detalhe detalhe(@Positive Long id) {
        acesso.usuario();
        var c = repository.detalhe(id);
        if (c == null) throw RegraNegocioException.naoEncontrado();
        acesso.armazem(c.endereco().getArmazem().getId());
        var permitidos = Set.copyOf(clientes(null));
        var u = c.unidade();
        boolean visivel = u != null && permitidos.contains(u.getPedido().getCliente().getId());
        boolean reservada = visivel && repository.reservada(u.getId());
        return new VisaoOperacaoDto.Detalhe(
                posicao(c, permitidos, reservada),
                visivel ? List.of(unidade(u, reservada)) : List.of(),
                visivel ? 1 : 0,
                u != null && !visivel,
                c.endereco().getAlturaMetros(),
                c.endereco().getLarguraMetros(),
                c.endereco().getProfundidadeMetros(),
                c.endereco().getEmpilhamentoMaximo(),
                c.endereco().getTipoUnidadePermitido());
    }

    private VisaoOperacaoDto.Unidade unidade(UnidadeLogistica u, boolean reservada) {
        return new VisaoOperacaoDto.Unidade(
                u.getId(),
                u.getPedido().getCliente().getId(),
                u.getCodigo(),
                u.getProduto().getDescricao(),
                u.getProduto().getSku(),
                u.getLote(),
                u.getQuantidade(),
                u.getProduto().getUnidadeMedida(),
                u.getPedido().getId(),
                u.isBloqueada(),
                reservada,
                u.getTipoLocalizacao() == TipoEndereco.QUARENTENA,
                repository.ultimaMovimentacao(u.getId()));
    }
}
