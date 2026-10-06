package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ContagemEstoque;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContagemEstoqueRepository extends JpaRepository<ContagemEstoque, Long> {
    Optional<ContagemEstoque> findByUnidadeId(Long unidadeId);

    @org.springframework.data.jpa.repository.Query(
            "select c from ContagemEstoque c where c.unidade.pedido.cliente.id=:clienteId and c.unidade.pedido.armazem.id=:armazemId and (:produtoId is null or c.unidade.produto.id=:produtoId) and (:codigo is null or c.unidade.codigo=:codigo) and (:enderecoId is null or exists(select o.id from OcupacaoEndereco o where o.unidade.id=c.unidade.id and o.endereco.id=:enderecoId)) and (:impedimento is null or c.impedimento=:impedimento) and (:situacao is null or exists(select r.id from RevisaoContagem r where r.contagem.id=c.id and r.numero=c.revisaoAtual and r.situacao=:situacao))")
    org.springframework.data.domain.Page<ContagemEstoque> consultar(
            Long clienteId,
            Long armazemId,
            Long produtoId,
            String codigo,
            Long enderecoId,
            br.com.rodogarcia.wms.models.SituacaoRevisaoContagem situacao,
            Boolean impedimento,
            org.springframework.data.domain.Pageable pagina);
}
