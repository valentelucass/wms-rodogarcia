package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.models.CadastroBase;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** AC12: permite resolver compromisso identificado sem reabrir cadastros. */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ResolucaoCadastroService {
    private final AcessoService acesso;

    public ResolucaoCadastroService(AcessoService acesso) {
        this.acesso = acesso;
    }

    public void conferir(boolean resolverPendentes, CadastroBase... cadastros) {
        if (resolverPendentes) acesso.exigirGestor();
        for (var c : cadastros)
            if (c.getSituacao() != SituacaoCadastro.ATIVO
                    && !(resolverPendentes
                            && c.getSituacao() == SituacaoCadastro.ENCERRAMENTO_PENDENTE))
                CadastroSupport.ativo(c);
    }
}
