package br.com.rodogarcia.wms.models;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "dependencia_contingencia",
        schema = "wms",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_dependencia_contingencia",
                        columnNames = {"linha_id", "depende_id"}))
public class DependenciaContingencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linha_id", nullable = false, updatable = false)
    private LinhaContingencia linha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depende_id", nullable = false, updatable = false)
    private LinhaContingencia depende;

    protected DependenciaContingencia() {}

    public DependenciaContingencia(LinhaContingencia linha, LinhaContingencia depende) {
        if (linha.getId().equals(depende.getId()))
            throw new IllegalArgumentException("Dependência circular.");
        this.linha = linha;
        this.depende = depende;
    }
}
