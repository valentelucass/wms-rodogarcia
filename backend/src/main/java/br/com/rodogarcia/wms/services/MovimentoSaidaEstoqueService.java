package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ConjuntoPosicoes;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.OcupacaoEndereco;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ConjuntoPosicoesRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Movimento físico de saída/retorno/avaria. Chamador já mantém CLIENTE e ARMAZEM bloqueados. */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class MovimentoSaidaEstoqueService {
    private final EnderecoRepository enderecos;
    private final ConjuntoPosicoesRepository conjuntos;
    private final OcupacaoEnderecoRepository ocupacoes;
    private final ResolucaoCadastroService resolucao;
    private final AcessoService acesso;

    public MovimentoSaidaEstoqueService(
            EnderecoRepository enderecos,
            ConjuntoPosicoesRepository conjuntos,
            OcupacaoEnderecoRepository ocupacoes,
            ResolucaoCadastroService resolucao,
            AcessoService acesso) {
        this.enderecos = enderecos;
        this.conjuntos = conjuntos;
        this.ocupacoes = ocupacoes;
        this.resolucao = resolucao;
        this.acesso = acesso;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void mover(
            UnidadeLogistica unidade,
            List<EstoqueDto.Destino> destinos,
            Long conjuntoId,
            TipoEndereco area,
            boolean resolverPendentes,
            Instant agora) {
        acesso.cliente(unidade.getPedido().getCliente().getId());
        acesso.armazem(unidade.getPedido().getArmazem().getId());
        var medidas = unidade.getMedidas();
        if (medidas == null || destinos.size() != medidas.getPosicoesNecessarias())
            throw CadastroSupport.invalido("Informe todas as posições físicas da unidade.");
        var ids = new HashSet<Long>();
        for (var d : destinos)
            if (!ids.add(d.enderecoId())) throw CadastroSupport.invalido("Destino repetido.");
        var origem = ocupacoes.buscarEnderecos(unidade.getId());
        var todos = new HashSet<>(origem);
        todos.addAll(ids);
        var posicoes = new HashMap<Long, Endereco>();
        for (Long id : todos.stream().sorted().toList())
            posicoes.put(
                    id,
                    enderecos
                            .buscarParaAtualizar(id)
                            .orElseThrow(RegraNegocioException::naoEncontrado));
        for (var d : destinos) {
            var e = posicoes.get(d.enderecoId());
            resolucao.conferir(resolverPendentes, e);
            CapacidadeSupport.configurado(e);
            if (!e.getArmazem().getId().equals(unidade.getPedido().getArmazem().getId())
                    || !e.getCodigo().equals(CadastroSupport.codigo(d.codigoLido()))
                    || e.getTipo() != area)
                throw CadastroSupport.invalido("Destino/código/área não corresponde à operação.");
            if (e.getTipoUnidadePermitido() != unidade.getTipo())
                throw RegraNegocioException.conflito(
                        "TIPO_INCOMPATIVEL", "Destino incompatível com a unidade.");
        }
        ConjuntoPosicoes conjunto = null;
        if (medidas.getPosicoesNecessarias() == 2) {
            if (conjuntoId == null)
                throw CadastroSupport.invalido("Selecione conjunto de duas posições.");
            conjunto =
                    conjuntos
                            .buscarParaAtualizar(conjuntoId)
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            resolucao.conferir(resolverPendentes, conjunto);
            if (!conjunto.getArmazem().getId().equals(unidade.getPedido().getArmazem().getId())
                    || !ids.equals(
                            Set.of(
                                    conjunto.getEnderecoA().getId(),
                                    conjunto.getEnderecoB().getId())))
                throw CadastroSupport.invalido("Posições não correspondem ao conjunto.");
            CapacidadeSupport.conferir(
                    medidas,
                    conjunto.getCapacidadePesoKg(),
                    conjunto.getAlturaMetros(),
                    conjunto.getLarguraMetros(),
                    conjunto.getProfundidadeMetros(),
                    conjunto.getEmpilhamentoMaximo());
        } else {
            if (conjuntoId != null)
                throw CadastroSupport.invalido("Uma posição não utiliza conjunto.");
            var e = posicoes.get(destinos.getFirst().enderecoId());
            CapacidadeSupport.conferir(
                    medidas,
                    e.getCapacidadePesoKg(),
                    e.getAlturaMetros(),
                    e.getLarguraMetros(),
                    e.getProfundidadeMetros(),
                    e.getEmpilhamentoMaximo());
        }
        var ocupadas = new HashMap<Long, OcupacaoEndereco>();
        for (Long id : todos.stream().sorted().toList()) {
            var o = ocupacoes.findByEnderecoId(id).orElse(null);
            if (ids.contains(id)
                    && o != null
                    && o.getUnidade() != null
                    && !o.getUnidade().getId().equals(unidade.getId()))
                throw RegraNegocioException.conflito(
                        "POSICAO_OCUPADA", "Destino ocupado por outra unidade.");
            ocupadas.put(id, o);
        }
        for (Long id : origem) if (!ids.contains(id)) ocupadas.get(id).atribuir(null);
        for (Long id : ids.stream().sorted().toList()) {
            var o = ocupadas.get(id);
            if (o == null) o = ocupacoes.save(new OcupacaoEndereco(posicoes.get(id)));
            o.atribuir(unidade);
        }
        unidade.posicionar(medidas, area, conjunto, agora);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void retirar(UnidadeLogistica unidade) {
        acesso.cliente(unidade.getPedido().getCliente().getId());
        acesso.armazem(unidade.getPedido().getArmazem().getId());
        for (Long id : ocupacoes.buscarEnderecos(unidade.getId())) {
            enderecos.buscarParaAtualizar(id).orElseThrow(RegraNegocioException::naoEncontrado);
            ocupacoes
                    .findByEnderecoId(id)
                    .orElseThrow(RegraNegocioException::naoEncontrado)
                    .atribuir(null);
        }
    }
}
