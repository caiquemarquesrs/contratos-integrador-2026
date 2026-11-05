package br.com.plataforma.contratos.contrato;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.TenantId;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/** Uma linha do histórico: um campo do contrato que mudou, quem mudou e quando (RF-01). */
@Entity
@Table(name = "historico_contrato")
@SQLDelete(sql = "UPDATE contratos.historico_contrato SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class HistoricoContrato implements Persistable<UUID> {

    /** Tamanho das colunas valor_anterior e valor_novo. */
    static final int LIMITE_DO_VALOR = 500;

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "contrato_id", nullable = false, updatable = false)
    private UUID contratoId;

    @Column(nullable = false, length = 100, updatable = false)
    private String campo;

    @Column(name = "valor_anterior", length = LIMITE_DO_VALOR, updatable = false)
    private String valorAnterior;

    @Column(name = "valor_novo", length = LIMITE_DO_VALOR, updatable = false)
    private String valorNovo;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private OffsetDateTime ocorridoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Column(name = "deleted_at")
    private OffsetDateTime excluidoEm;

    @Column(name = "created_by", updatable = false)
    private UUID criadoPor;

    @Column(name = "updated_by")
    private UUID atualizadoPor;

    /** Id gerado pela aplicação: sem isto, o Spring Data faria um SELECT antes de cada INSERT. */
    @Transient
    private boolean novo;

    protected HistoricoContrato() {
    }

    static HistoricoContrato novo(UUID contratoId, String campo, String valorAnterior, String valorNovo,
                                  UUID usuario) {
        HistoricoContrato registro = new HistoricoContrato();
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        registro.id = UUID.randomUUID();
        registro.contratoId = contratoId;
        registro.campo = campo;
        registro.valorAnterior = cortar(valorAnterior);
        registro.valorNovo = cortar(valorNovo);
        registro.usuarioId = usuario;
        registro.ocorridoEm = agora;
        registro.criadoEm = agora;
        registro.atualizadoEm = agora;
        registro.criadoPor = usuario;
        registro.atualizadoPor = usuario;
        registro.novo = true;
        return registro;
    }

    /** Os valores cabem em 500 caracteres: o que passar disso é cortado. */
    static String cortar(String valor) {
        return valor == null || valor.length() <= LIMITE_DO_VALOR ? valor : valor.substring(0, LIMITE_DO_VALOR);
    }

    @PostLoad
    @PostPersist
    void marcarComoPersistido() {
        novo = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public String getCampo() {
        return campo;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNovo() {
        return valorNovo;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public OffsetDateTime getOcorridoEm() {
        return ocorridoEm;
    }
}
