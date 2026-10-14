package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Validação pura de capacidade, sem contexto, repositórios ou banco. */
class CapacidadeSupportTest {
    @Test
    void dimensoesSozinhasNaoBastamEInformaSomenteOTipoAusente() {
        var e = endereco(BigDecimal.TEN, 1);
        assertThatThrownBy(() -> CapacidadeSupport.configurado(e))
                .hasMessageContaining("A posição A-1 está sem: tipo de unidade permitido")
                .hasMessageContaining("Cadastros > Endereços > Editar")
                .hasMessageNotContaining("capacidade de peso")
                .hasMessageNotContaining("altura (m)");
        e.alterarTipoUnidadePermitido(TipoUnidadeLogistica.BOBINA, Instant.now());
        assertThatCode(() -> CapacidadeSupport.configurado(e)).doesNotThrowAnyException();
    }

    @Test
    void listaTodosOsLimitesAusentesSemLiberarCapacidade() {
        var e = endereco(null, null);
        e.alterarTipoUnidadePermitido(TipoUnidadeLogistica.BOBINA, Instant.now());
        assertThatThrownBy(() -> CapacidadeSupport.configurado(e))
                .hasMessageContaining(
                        "capacidade de peso (kg), altura (m), largura (m), profundidade (m), empilhamento máximo")
                .hasMessageNotContaining("está sem: tipo de unidade");
    }

    private Endereco endereco(BigDecimal limite, Integer empilhamento) {
        return new Endereco(
                null,
                "A-1",
                "A",
                0,
                "1",
                "Posição",
                TipoEndereco.ARMAZENAGEM,
                limite,
                limite,
                limite,
                limite,
                empilhamento,
                0,
                Instant.now());
    }
}
