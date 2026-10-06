package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ContextoCobrancaService {
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final AcessoService acesso;

    public ContextoCobrancaService(
            ClienteRepository clientes, ArmazemRepository armazens, AcessoService acesso) {
        this.clientes = clientes;
        this.armazens = armazens;
        this.acesso = acesso;
    }

    public record Contexto(Cliente cliente, Armazem armazem) {}

    public void autorizar(Long cliente, Long armazem) {
        acesso.exigirSupervisor();
        acesso.cliente(cliente);
        acesso.armazem(armazem);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Contexto bloquear(Long cliente, Long armazem) {
        autorizar(cliente, armazem);
        var c =
                clientes.buscarParaAtualizar(cliente)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var a =
                armazens.buscarParaAtualizar(armazem)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        return new Contexto(c, a);
    }
}
