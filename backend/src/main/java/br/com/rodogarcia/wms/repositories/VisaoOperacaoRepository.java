package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** Leituras do painel. Ocupação física nunca depende do filtro visual do mapa. */
@Repository
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class VisaoOperacaoRepository {
    private final EntityManager em;

    public VisaoOperacaoRepository(EntityManager em) {
        this.em = em;
    }

    public record Celula(Endereco endereco, UnidadeLogistica unidade) {}

    public Instant ultimaMovimentacao(Long unidadeId) {
        return em.createQuery(
                        "select max(m.instante) from MovimentoEstoque m where m.unidade.id=:id",
                        Instant.class)
                .setParameter("id", unidadeId)
                .getSingleResult();
    }

    private static final String MAPA =
            " from Endereco e left join OcupacaoEndereco o on o.endereco.id=e.id left join fetch e.armazem a left join o.unidade u";
    private static final String BASE =
            " from Endereco e left join OcupacaoEndereco o on o.endereco.id=e.id where e.armazem.id in :armazens";
    private static final String CAPACIDADE =
            " and e.situacao=br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO and e.armazem.situacao=br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO and e.tipo=br.com.rodogarcia.wms.models.TipoEndereco.ARMAZENAGEM";

    public long capacidade(List<Long> armazens) {
        return count("select count(e.id)" + BASE + CAPACIDADE, null, armazens);
    }

    public long ocupadas(List<Long> armazens) {
        return count(
                "select count(e.id)" + BASE + CAPACIDADE + " and o.unidade.id is not null",
                null,
                armazens);
    }

    public long livres(List<Long> armazens) {
        return count(
                "select count(e.id)" + BASE + CAPACIDADE + " and o.unidade.id is null",
                null,
                armazens);
    }

    private String filtroMapa(String estado) {
        return switch (estado) {
            case "DISPONIVEL" -> CAPACIDADE + " and o.unidade.id is null";
            case "OCUPADO" -> " and o.unidade.id is not null";
            case "TODAS" -> "";
            default -> throw new IllegalArgumentException("Estado inválido.");
        };
    }

    public long totalMapa(List<Long> armazens, String codigo, String estado) {
        var q =
                em.createQuery(
                        "select count(e.id)"
                                + BASE
                                + filtroMapa(estado)
                                + " and lower(e.codigo) like :codigo",
                        Long.class);
        return q.setParameter("armazens", armazens)
                .setParameter("codigo", "%" + codigo.toLowerCase(java.util.Locale.ROOT) + "%")
                .getSingleResult();
    }

    public List<Celula> mapa(
            List<Long> armazens, String codigo, String estado, int inicio, int tamanho) {
        var q =
                em.createQuery(
                        "select e,u"
                                + MAPA
                                + " where e.armazem.id in :armazens"
                                + filtroMapa(estado)
                                + " and lower(e.codigo) like :codigo order by a.nome,e.rua,e.nivel desc,e.sequenciaColeta,e.codigo,e.id",
                        Object[].class);
        return q
                .setParameter("armazens", armazens)
                .setParameter("codigo", "%" + codigo.toLowerCase(java.util.Locale.ROOT) + "%")
                .setFirstResult(inicio)
                .setMaxResults(tamanho)
                .getResultList()
                .stream()
                .map(v -> new Celula((Endereco) v[0], (UnidadeLogistica) v[1]))
                .toList();
    }

    public Celula detalhe(Long id) {
        return em.createQuery("select e,u" + MAPA + " where e.id=:id", Object[].class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .map(v -> new Celula((Endereco) v[0], (UnidadeLogistica) v[1]))
                .orElse(null);
    }

    private static final String ESCOPO_U =
            " from UnidadeLogistica u where u.ativa=true and u.pedido.cliente.id in :clientes and u.pedido.armazem.id in :armazens";

    public long unidades(List<Long> clientes, List<Long> armazens) {
        return count("select count(u.id)" + ESCOPO_U, clientes, armazens);
    }

    public long quarentena(List<Long> clientes, List<Long> armazens) {
        return count(
                "select count(u.id)"
                        + ESCOPO_U
                        + " and u.tipoLocalizacao=br.com.rodogarcia.wms.models.TipoEndereco.QUARENTENA",
                clientes,
                armazens);
    }

    public long reservas(List<Long> clientes, List<Long> armazens) {
        return count(
                "select count(r.id) from ReservaSaida r where r.situacao=br.com.rodogarcia.wms.models.SituacaoReservaSaida.ATIVA and r.pedido.cliente.id in :clientes and r.pedido.armazem.id in :armazens",
                clientes,
                armazens);
    }

    public boolean reservada(Long unidadeId) {
        return em.createQuery(
                                "select count(r.id) from ReservaSaida r where r.unidade.id=:id and r.situacao=br.com.rodogarcia.wms.models.SituacaoReservaSaida.ATIVA",
                                Long.class)
                        .setParameter("id", unidadeId)
                        .getSingleResult()
                > 0;
    }

    public long entradas(List<Long> clientes, List<Long> armazens) {
        return count(
                "select count(p.id) from PedidoEntrada p where p.cliente.id in :clientes and p.armazem.id in :armazens and p.situacao not in (br.com.rodogarcia.wms.models.SituacaoPedidoEntrada.EFETIVADO,br.com.rodogarcia.wms.models.SituacaoPedidoEntrada.CANCELADO)",
                clientes,
                armazens);
    }

    public long saidas(List<Long> clientes, List<Long> armazens) {
        return count(
                "select count(p.id) from PedidoSaida p where p.cliente.id in :clientes and p.armazem.id in :armazens and p.situacao in (br.com.rodogarcia.wms.models.SituacaoPedidoSaida.RASCUNHO,br.com.rodogarcia.wms.models.SituacaoPedidoSaida.RESERVADO,br.com.rodogarcia.wms.models.SituacaoPedidoSaida.EM_SEPARACAO,br.com.rodogarcia.wms.models.SituacaoPedidoSaida.SEPARADO)",
                clientes,
                armazens);
    }

    public boolean faturamentoIncompleto(
            List<Long> clientes, List<Long> armazens, Instant inicio, Instant fim) {
        return scoped(
                                em.createQuery(
                                        "select count(v.id) from VersaoFechamento v where v.fechamento.cliente.id in :clientes and v.fechamento.armazem.id in :armazens and v.saldo is null and v.estadoExterno='EMITIDO' and exists (select n.id from ReferenciaNfse n where n.versao.id=v.id and n.emitidaEm>=:inicio and n.emitidaEm<:fim)",
                                        Long.class),
                                clientes,
                                armazens)
                        .setParameter("inicio", inicio)
                        .setParameter("fim", fim)
                        .getSingleResult()
                > 0;
    }

    public BigDecimal faturamento(
            List<Long> clientes, List<Long> armazens, Instant inicio, Instant fim) {
        // Uma versão é contada uma vez mesmo se houver várias referências externas.
        return scoped(
                        em.createQuery(
                                "select sum(v.saldo) from VersaoFechamento v where v.fechamento.cliente.id in :clientes and v.fechamento.armazem.id in :armazens and v.saldo>0 and v.estadoExterno='EMITIDO' and exists (select n.id from ReferenciaNfse n where n.versao.id=v.id and n.emitidaEm>=:inicio and n.emitidaEm<:fim)",
                                BigDecimal.class),
                        clientes,
                        armazens)
                .setParameter("inicio", inicio)
                .setParameter("fim", fim)
                .getSingleResult();
    }

    private long count(String jpql, List<Long> clientes, List<Long> armazens) {
        return scoped(em.createQuery(jpql, Long.class), clientes, armazens).getSingleResult();
    }

    private <T> TypedQuery<T> scoped(
            TypedQuery<T> query, List<Long> clientes, List<Long> armazens) {
        query.setParameter("armazens", armazens);
        if (clientes != null) query.setParameter("clientes", clientes);
        return query;
    }
}
