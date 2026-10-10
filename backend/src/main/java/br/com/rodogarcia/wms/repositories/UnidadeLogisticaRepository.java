package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.UnidadeLogistica;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UnidadeLogisticaRepository extends JpaRepository<UnidadeLogistica, Long> {
    @Query(
            """
        select u from UnidadeLogistica u where u.pedido.cliente.id=:clienteId and u.pedido.armazem.id=:armazemId and u.produto.id in :produtosIds
        and u.ativa=true and u.bloqueada=false and u.avariaPosterior=false and u.reservaSaida is null
        and (u.condicao=br.com.rodogarcia.wms.models.CondicaoMercadoria.BOA or u.avariaInicialReparada=true)
        and u.tipoLocalizacao=br.com.rodogarcia.wms.models.TipoEndereco.ARMAZENAGEM
        and u.pedido.situacao=br.com.rodogarcia.wms.models.SituacaoPedidoEntrada.EFETIVADO
        and u.pedido.cliente.situacao in :situacoes and u.pedido.armazem.situacao in :situacoes and u.produto.situacao in :situacoes
        and u.medidas.posicoesNecessarias in (1,2)
        and (select count(o) from OcupacaoEndereco o where o.unidade.id=u.id)=u.medidas.posicoesNecessarias
        and not exists(select o.id from OcupacaoEndereco o where o.unidade.id=u.id and (o.endereco.situacao not in :situacoes or o.endereco.tipo<>br.com.rodogarcia.wms.models.TipoEndereco.ARMAZENAGEM or o.endereco.armazem.id<>u.pedido.armazem.id))
        and (u.medidas.posicoesNecessarias=1 or exists(select conjunto.id from ConjuntoPosicoes conjunto where conjunto.id=u.conjuntoAtual.id and conjunto.situacao in :situacoes))
        and not exists(select c.id from ContagemEstoque c where c.unidade.id=u.id and c.impedimento=true)
        and not exists(select ci.id from CargaInicial ci, ConteudoUnidade cu where cu.unidade.id=u.id and cu.entrada.id=ci.entrada.id and ci.situacao<>br.com.rodogarcia.wms.models.SituacaoCargaInicial.REGULARIZADA and (:cargaId is null or ci.id<>:cargaId))
        order by u.id
        """)
    List<UnidadeLogistica> candidatasResolucao(
            Long clienteId,
            Long armazemId,
            List<Long> produtosIds,
            Long cargaId,
            List<br.com.rodogarcia.wms.models.SituacaoCadastro> situacoes);

    // Condição física compartilhada também pela revalidação do pedido já reservado.
    String ELEGIVEL =
            """
        u.ativa = true and u.bloqueada = false and u.avariaPosterior = false
        and not exists (select c.id from ContagemEstoque c where c.unidade.id=u.id and c.impedimento=true)
        and not exists (select ci.id from CargaInicial ci, ConteudoUnidade cu where cu.unidade.id=u.id and cu.entrada.id=ci.entrada.id and ci.situacao<>br.com.rodogarcia.wms.models.SituacaoCargaInicial.REGULARIZADA)
        and (u.condicao = br.com.rodogarcia.wms.models.CondicaoMercadoria.BOA or u.avariaInicialReparada = true)
        and u.tipoLocalizacao is not null
        and u.tipoLocalizacao = br.com.rodogarcia.wms.models.TipoEndereco.ARMAZENAGEM
        and u.pedido.situacao = br.com.rodogarcia.wms.models.SituacaoPedidoEntrada.EFETIVADO
        and u.pedido.cliente.situacao = br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO
        and u.pedido.armazem.situacao = br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO
        and u.produto.situacao = br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO
        and u.medidas.posicoesNecessarias in (1,2)
        and (select count(o) from OcupacaoEndereco o where o.unidade.id = u.id) = u.medidas.posicoesNecessarias
        and not exists (select o.id from OcupacaoEndereco o where o.unidade.id = u.id and
            (o.endereco.situacao <> br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO
             or o.endereco.tipo <> br.com.rodogarcia.wms.models.TipoEndereco.ARMAZENAGEM
             or o.endereco.armazem.id <> u.pedido.armazem.id))
        and (u.medidas.posicoesNecessarias = 1 or exists
            (select c.id from ConjuntoPosicoes c where c.id = u.conjuntoAtual.id
             and c.situacao = br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO))
        """;

    String DISPONIVEL = ELEGIVEL + " and u.reservaSaida is null";

    String ELEGIVEL_SAIDA =
            """
        u.ativa = true and u.bloqueada = false and u.avariaPosterior = false
        and not exists (select c.id from ContagemEstoque c where c.unidade.id=u.id and c.impedimento=true)
        and not exists (select ci.id from CargaInicial ci, ConteudoUnidade cu where cu.unidade.id=u.id and cu.entrada.id=ci.entrada.id and ci.situacao<>br.com.rodogarcia.wms.models.SituacaoCargaInicial.REGULARIZADA and not exists (select rr.id from ResolucaoRemanescente rr where rr.pedidoSaida.id=u.reservaSaida.id and rr.carga.id=ci.id))
        and (u.condicao = br.com.rodogarcia.wms.models.CondicaoMercadoria.BOA or u.avariaInicialReparada = true)
        and u.tipoLocalizacao is not null
        and u.tipoLocalizacao = :tipo
        and u.pedido.situacao = br.com.rodogarcia.wms.models.SituacaoPedidoEntrada.EFETIVADO
        and u.pedido.cliente.situacao in :situacoes
        and u.pedido.armazem.situacao in :situacoes
        and u.produto.situacao in :situacoes
        and u.medidas.posicoesNecessarias in (1,2)
        and (select count(o) from OcupacaoEndereco o where o.unidade.id = u.id) = u.medidas.posicoesNecessarias
        and not exists (select o.id from OcupacaoEndereco o where o.unidade.id = u.id and
            (o.endereco.situacao not in :situacoes
             or o.endereco.tipo <> :tipo
             or o.endereco.armazem.id <> u.pedido.armazem.id))
        and (u.medidas.posicoesNecessarias = 1 or exists
            (select c.id from ConjuntoPosicoes c where c.id = u.conjuntoAtual.id
             and c.situacao in :situacoes))
        """;

    @Query(
            "select u.id from UnidadeLogistica u where u.id in :ids and u.reservaSaida.id=:pedidoId and ("
                    + ELEGIVEL_SAIDA
                    + ")")
    List<Long> buscarElegiveisSaida(
            List<Long> ids,
            Long pedidoId,
            br.com.rodogarcia.wms.models.TipoEndereco tipo,
            List<br.com.rodogarcia.wms.models.SituacaoCadastro> situacoes);

    interface EscopoUnidade {
        Long getClienteId();

        Long getArmazemId();
    }

    @Query(
            "select u.pedido.cliente.id as clienteId,u.pedido.armazem.id as armazemId from UnidadeLogistica u where u.codigo=:codigo")
    Optional<EscopoUnidade> buscarEscopoPorCodigo(String codigo);

    @Query("select u.id from UnidadeLogistica u where u.id in :ids and (" + ELEGIVEL + ")")
    List<Long> buscarElegiveis(List<Long> ids);

    @Query(
            "select u from UnidadeLogistica u where u.pedido.cliente.id=:clienteId and u.pedido.armazem.id=:armazemId and u.produto.id in :produtosIds and ("
                    + DISPONIVEL
                    + ") order by u.id")
    List<UnidadeLogistica> buscarCandidatas(Long clienteId, Long armazemId, List<Long> produtosIds);

    @Query("select u.id from UnidadeLogistica u where u.id in :ids and (" + DISPONIVEL + ")")
    List<Long> buscarDisponiveis(List<Long> ids);

    @EntityGraph(attributePaths = {"pedido", "produto", "nota", "embalagem", "conjuntoAtual"})
    @Query(
            "select u from UnidadeLogistica u where u.ativa = true and u.pedido.cliente.id = :clienteId and u.pedido.armazem.id = :armazemId and (:produtoId is null or u.produto.id = :produtoId) and (:disponivel is null or (:disponivel = true and ("
                    + DISPONIVEL
                    + ")) or (:disponivel = false and not ("
                    + DISPONIVEL
                    + ")))")
    Page<UnidadeLogistica> consultarEstoque(
            Long clienteId, Long armazemId, Long produtoId, Boolean disponivel, Pageable pageable);

    @EntityGraph(attributePaths = {"pedido", "produto", "nota", "embalagem", "conjuntoAtual"})
    @Query(
            "select u from UnidadeLogistica u where u.ativa=true and u.pedido.cliente.id=:clienteId and u.pedido.armazem.id=:armazemId and (:produtoId is null or u.produto.id=:produtoId) and (:codigo is null or u.codigo=:codigo) and (:enderecoId is null or exists(select o.id from OcupacaoEndereco o where o.unidade.id=u.id and o.endereco.id=:enderecoId)) and (:disponivel is null or (:disponivel=true and ("
                    + DISPONIVEL
                    + ")) or (:disponivel=false and not ("
                    + DISPONIVEL
                    + "))) and (:situacao is null or (:situacao='DISPONIVEL' and ("
                    + DISPONIVEL
                    + ")) or (:situacao='RESERVADO' and u.reservaSaida is not null) or (:situacao='BLOQUEADO' and (u.bloqueada=true or u.avariaPosterior=true or (u.condicao=br.com.rodogarcia.wms.models.CondicaoMercadoria.AVARIADA and u.avariaInicialReparada=false))) or (:situacao='CONTAGEM_PENDENTE' and exists(select c.id from ContagemEstoque c where c.unidade.id=u.id and c.impedimento=true)) or (:situacao='CONFERENCIA_PENDENTE' and exists(select c.id from CargaInicial c, ConteudoUnidade cu where cu.unidade.id=u.id and cu.entrada.id=c.entrada.id and c.situacao=br.com.rodogarcia.wms.models.SituacaoCargaInicial.PREPARADA)) or (:situacao='ARMAZENAGEM' and u.tipoLocalizacao=br.com.rodogarcia.wms.models.TipoEndereco.ARMAZENAGEM) or (:situacao='TRIAGEM' and u.tipoLocalizacao=br.com.rodogarcia.wms.models.TipoEndereco.TRIAGEM) or (:situacao='QUARENTENA' and u.tipoLocalizacao=br.com.rodogarcia.wms.models.TipoEndereco.QUARENTENA) or (:situacao='SEPARACAO' and u.tipoLocalizacao=br.com.rodogarcia.wms.models.TipoEndereco.SEPARACAO))")
    Page<UnidadeLogistica> filtrarEstoque(
            Long clienteId,
            Long armazemId,
            Long produtoId,
            String codigo,
            Long enderecoId,
            Boolean disponivel,
            String situacao,
            Pageable pagina);

    interface TotaisEstoque {
        BigDecimal getFisico();

        BigDecimal getNaoEnderecado();

        BigDecimal getTriagem();

        BigDecimal getQuarentena();

        BigDecimal getArmazenagem();

        BigDecimal getAvariado();
    }

    @Query(
            "select sum(u.quantidade) as fisico, sum(case when u.tipoLocalizacao is null then u.quantidade else 0 end) as naoEnderecado, sum(case when u.tipoLocalizacao = br.com.rodogarcia.wms.models.TipoEndereco.TRIAGEM then u.quantidade else 0 end) as triagem, sum(case when u.tipoLocalizacao = br.com.rodogarcia.wms.models.TipoEndereco.QUARENTENA then u.quantidade else 0 end) as quarentena, sum(case when u.tipoLocalizacao = br.com.rodogarcia.wms.models.TipoEndereco.ARMAZENAGEM then u.quantidade else 0 end) as armazenagem, sum(case when ((u.condicao = br.com.rodogarcia.wms.models.CondicaoMercadoria.AVARIADA and u.avariaInicialReparada = false) or u.avariaPosterior = true) then u.quantidade else 0 end) as avariado from UnidadeLogistica u where u.ativa = true and u.pedido.cliente.id = :clienteId and u.pedido.armazem.id = :armazemId and u.produto.id = :produtoId")
    TotaisEstoque resumirEstoque(Long clienteId, Long armazemId, Long produtoId);

    @Query(
            "select sum(u.quantidade) from UnidadeLogistica u where u.pedido.cliente.id = :clienteId and u.pedido.armazem.id = :armazemId and u.produto.id = :produtoId and ("
                    + DISPONIVEL
                    + ")")
    BigDecimal somarDisponivel(Long clienteId, Long armazemId, Long produtoId);

    boolean existsByConjuntoAtualId(Long conjuntoId);

    @EntityGraph(attributePaths = {"pedido", "produto", "nota", "embalagem"})
    Page<UnidadeLogistica> findByPedidoId(Long pedidoId, Pageable pageable);

    Optional<UnidadeLogistica> findByIdAndPedidoId(Long id, Long pedidoId);

    @Query("select u.pedido.id from UnidadeLogistica u where u.codigo = :codigo")
    Optional<Long> buscarPedidoPorCodigo(String codigo);

    Optional<UnidadeLogistica> findByCodigo(String codigo);

    @Query(
            "select distinct u.embalagem.id from UnidadeLogistica u where u.pedido.id = :pedidoId and u.id in :ids order by u.embalagem.id")
    List<Long> buscarEmbalagens(Long pedidoId, List<Long> ids);

    long countByPedidoId(Long pedidoId);

    @Query(
            "select u from UnidadeLogistica u where u.pedido.cliente.id=:clienteId and u.pedido.armazem.id=:armazemId order by u.id")
    List<UnidadeLogistica> historicoCobranca(Long clienteId, Long armazemId);

    @Query(
            "select u from UnidadeLogistica u join fetch u.pedido p where u.ativa=true and p.cliente.id in :clientes and p.armazem.id in :armazens and u.produto.cliente.id=p.cliente.id order by u.id")
    List<UnidadeLogistica> ativasParaIndicador(List<Long> clientes, List<Long> armazens);

    @Query(
            "select u from UnidadeLogistica u where u.pedido.cliente.id=:clienteId and u.pedido.armazem.id=:armazemId and u.primeiroEnderecamentoEm is not null and not exists(select f.id from FatoServico f where f.servico.id=:servicoId and f.unidade.id=u.id and f.servico.tipo='ENTRADA')")
    Page<UnidadeLogistica> candidatasServicoEntrada(
            Long clienteId, Long armazemId, Long servicoId, Pageable pagina);
}
