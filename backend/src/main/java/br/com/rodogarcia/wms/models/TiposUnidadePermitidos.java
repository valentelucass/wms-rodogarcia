package br.com.rodogarcia.wms.models;

import java.util.EnumSet;
import java.util.Set;

/** Perfil da posição; os tipos das unidades físicas continuam PALLET, BOBINA e VOLUME. */
public enum TiposUnidadePermitidos {
    PALLET,
    BOBINA,
    VOLUME,
    PALLET_BOBINA,
    PALLET_VOLUME,
    BOBINA_VOLUME,
    TODAS;

    public boolean permite(TipoUnidadeLogistica tipo) {
        return tipo != null && tipos().contains(tipo);
    }

    public Set<TipoUnidadeLogistica> tipos() {
        return switch (this) {
            case PALLET -> EnumSet.of(TipoUnidadeLogistica.PALLET);
            case BOBINA -> EnumSet.of(TipoUnidadeLogistica.BOBINA);
            case VOLUME -> EnumSet.of(TipoUnidadeLogistica.VOLUME);
            case PALLET_BOBINA ->
                    EnumSet.of(TipoUnidadeLogistica.PALLET, TipoUnidadeLogistica.BOBINA);
            case PALLET_VOLUME ->
                    EnumSet.of(TipoUnidadeLogistica.PALLET, TipoUnidadeLogistica.VOLUME);
            case BOBINA_VOLUME ->
                    EnumSet.of(TipoUnidadeLogistica.BOBINA, TipoUnidadeLogistica.VOLUME);
            case TODAS -> EnumSet.allOf(TipoUnidadeLogistica.class);
        };
    }

    /** Conjunto só permite os tipos aceitos por ambas as posições. */
    public TiposUnidadePermitidos intersecao(TiposUnidadePermitidos outro) {
        if (outro == null) return null;
        var comuns = tipos();
        comuns.retainAll(outro.tipos());
        for (var perfil : values()) if (perfil.tipos().equals(comuns)) return perfil;
        return null;
    }
}
