package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.CadastroBase;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

/** Validações compartilhadas dos cadastros, sem implementar CRUD genérico. */
public final class CadastroSupport {
    private CadastroSupport() {}

    public static String codigo(String valor) {
        return valor.strip().toUpperCase(Locale.ROOT);
    }

    /** Categoria livre, salvo a whitelist de armazenagem aplicada pelo serviço. */
    public static String categoriaCobranca(String valor) {
        String categoria = valor == null ? null : codigo(valor);
        if (categoria == null || categoria.length() > 40) {
            throw new RegraNegocioException(
                    HttpStatus.BAD_REQUEST,
                    "CATEGORIA_INVALIDA",
                    "itens[].categoria: informe até 40 caracteres após a normalização.");
        }
        return categoria;
    }

    public static String documento(String valor) {
        return codigo(valor).replaceAll("[./ -]", "");
    }

    public static String texto(String valor) {
        return valor.strip();
    }

    public static String motivo(String valor) {
        String motivo = texto(valor);
        if (motivo.length() < 5 || motivo.length() > 500) {
            throw invalido("Informe um motivo entre 5 e 500 caracteres.");
        }
        return motivo;
    }

    public static PageRequest pagina(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw invalido("Página deve ser não negativa e tamanho deve estar entre 1 e 100.");
        }
        if ((long) pagina * tamanho > Integer.MAX_VALUE) {
            throw invalido("Página fora do limite de consulta.");
        }
        return PageRequest.of(pagina, tamanho, Sort.by("id").ascending());
    }

    public static void ativo(CadastroBase cadastro) {
        if (cadastro.getSituacao() != SituacaoCadastro.ATIVO) {
            throw RegraNegocioException.conflito(
                    "CADASTRO_EM_ENCERRAMENTO",
                    "Cadastro em encerramento não permite novos vínculos.");
        }
    }

    public static void versao(CadastroBase cadastro, long versao) {
        if (cadastro.getVersao() != versao) {
            throw RegraNegocioException.conflito(
                    "VERSAO_DESATUALIZADA",
                    "Cadastro alterado por outra operação. Consulte novamente antes de confirmar.");
        }
    }

    public static void transicao(CadastroBase cadastro, SituacaoCadastro destino) {
        if (cadastro.getSituacao() == SituacaoCadastro.INATIVO
                || destino == SituacaoCadastro.INATIVO) {
            throw RegraNegocioException.conflito(
                    "CADASTRO_INATIVO",
                    "Inativação definitiva e reativação de INATIVO não são permitidas por transição comum.");
        }
        if (cadastro.getSituacao() == destino) {
            throw RegraNegocioException.conflito(
                    "TRANSICAO_INVALIDA", "O cadastro já está na situação solicitada.");
        }
    }

    public static void quantidade(Produto produto, BigDecimal quantidade) {
        if (quantidade.signum() <= 0
                || quantidade.stripTrailingZeros().scale() > produto.getPrecisaoQuantidade()) {
            throw invalido("Quantidade incompatível com a precisão do produto.");
        }
    }

    public static RegraNegocioException invalido(String mensagem) {
        return new RegraNegocioException(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", mensagem);
    }
}
