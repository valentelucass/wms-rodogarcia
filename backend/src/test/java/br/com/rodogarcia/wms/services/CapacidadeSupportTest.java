package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.TiposUnidadePermitidos;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        e.alterarTipoUnidadePermitido(TiposUnidadePermitidos.BOBINA, Instant.now());
        assertThatCode(() -> CapacidadeSupport.configurado(e)).doesNotThrowAnyException();
    }

    @Test
    void listaTodosOsLimitesAusentesSemLiberarCapacidade() {
        var e = endereco(null, null);
        e.alterarTipoUnidadePermitido(TiposUnidadePermitidos.BOBINA, Instant.now());
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

    @ParameterizedTest
    @CsvSource({
        "PALLET,PALLET,true", "PALLET,BOBINA,false", "PALLET,VOLUME,false",
        "BOBINA,PALLET,false", "BOBINA,BOBINA,true", "BOBINA,VOLUME,false",
        "VOLUME,PALLET,false", "VOLUME,BOBINA,false", "VOLUME,VOLUME,true",
        "PALLET_BOBINA,PALLET,true", "PALLET_BOBINA,BOBINA,true", "PALLET_BOBINA,VOLUME,false",
        "PALLET_VOLUME,PALLET,true", "PALLET_VOLUME,BOBINA,false", "PALLET_VOLUME,VOLUME,true",
        "BOBINA_VOLUME,PALLET,false", "BOBINA_VOLUME,BOBINA,true", "BOBINA_VOLUME,VOLUME,true",
        "TODAS,PALLET,true", "TODAS,BOBINA,true", "TODAS,VOLUME,true"
    })
    void movimentoAceitaSomenteTiposSelecionados(
            TiposUnidadePermitidos perfil, TipoUnidadeLogistica tipo, boolean permitido) {
        var endereco = enderecoComPerfil(perfil, BigDecimal.TEN);
        if (permitido)
            assertThatCode(() -> CapacidadeSupport.compativel(endereco, tipo))
                    .doesNotThrowAnyException();
        else
            assertThatThrownBy(() -> CapacidadeSupport.compativel(endereco, tipo))
                    .isInstanceOfSatisfying(
                            RegraNegocioException.class,
                            ex -> assertThat(ex.getCodigo()).isEqualTo("TIPO_INCOMPATIVEL"));
    }

    @ParameterizedTest
    @CsvSource({
        "TODAS,PALLET_BOBINA,PALLET_BOBINA",
        "PALLET_BOBINA,BOBINA_VOLUME,BOBINA",
        "PALLET_VOLUME,BOBINA_VOLUME,VOLUME",
        "TODAS,TODAS,TODAS",
        "BOBINA,BOBINA,BOBINA"
    })
    void conjuntoAceitaApenasTiposComuns(
            TiposUnidadePermitidos a, TiposUnidadePermitidos b, TiposUnidadePermitidos esperado) {
        assertThat(a.intersecao(b)).isEqualTo(esperado);
        assertThat(b.intersecao(a)).isEqualTo(esperado);
    }

    @Test
    void perfisSemTipoComumNaoFormamConjunto() {
        assertThat(TiposUnidadePermitidos.PALLET.intersecao(TiposUnidadePermitidos.BOBINA_VOLUME))
                .isNull();
    }

    @Test
    void todasNaoSubstituiLimitesNemTipoFisico() {
        assertThatThrownBy(
                        () ->
                                CapacidadeSupport.compativel(
                                        enderecoComPerfil(TiposUnidadePermitidos.TODAS, null),
                                        TipoUnidadeLogistica.BOBINA))
                .isInstanceOfSatisfying(
                        RegraNegocioException.class,
                        ex -> assertThat(ex.getCodigo()).isEqualTo("CAPACIDADE_NAO_CONFIGURADA"));
        assertThat(TipoUnidadeLogistica.values())
                .containsExactly(
                        TipoUnidadeLogistica.PALLET,
                        TipoUnidadeLogistica.BOBINA,
                        TipoUnidadeLogistica.VOLUME);
        assertThatThrownBy(() -> TipoUnidadeLogistica.valueOf("TODAS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(TiposUnidadePermitidos.TODAS.permite(null)).isFalse();
    }

    private Endereco enderecoComPerfil(TiposUnidadePermitidos perfil, BigDecimal limite) {
        var endereco =
                new Endereco(
                        null,
                        "A-1",
                        "A",
                        0,
                        "1",
                        "Posição fictícia",
                        TipoEndereco.ARMAZENAGEM,
                        limite,
                        limite,
                        limite,
                        limite,
                        1,
                        0,
                        Instant.now());
        endereco.alterarTipoUnidadePermitido(perfil, Instant.now());
        return endereco;
    }
}
