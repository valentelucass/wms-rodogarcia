package br.com.rodogarcia.wms.models;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@MappedSuperclass
public abstract class CadastroBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SituacaoCadastro situacao = SituacaoCadastro.ATIVO;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    protected CadastroBase() {}

    protected CadastroBase(Instant instante) {
        criadoEm = instante;
        alteradoEm = instante;
    }

    public Long getId() {
        return id;
    }

    public long getVersao() {
        return versao;
    }

    public SituacaoCadastro getSituacao() {
        return situacao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAlteradoEm() {
        return alteradoEm;
    }

    public void registrarAlteracao(Instant instante) {
        alteradoEm = instante;
    }

    public void alterarSituacao(SituacaoCadastro nova, Instant instante) {
        situacao = nova;
        alteradoEm = instante;
    }
}
