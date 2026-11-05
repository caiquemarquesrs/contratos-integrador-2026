package br.com.plataforma.contratos.documento;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

/**
 * Metadados de um documento de contrato (RF-08). O conteúdo do arquivo NÃO é mapeado aqui: só a
 * classe ArmazenamentoNoBanco lê e grava a coluna "conteudo", então listar documentos nunca
 * carrega os bytes.
 */
@Entity
@Table(name = "documentos")
@SQLDelete(sql = "UPDATE contratos.documentos SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Documento implements Persistable<UUID> {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "contrato_id", nullable = false, updatable = false)
    private UUID contratoId;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(name = "tipo_arquivo", nullable = false, length = 150)
    private String tipoArquivo;

    @Column(name = "tamanho_bytes", nullable = false)
    private long tamanhoBytes;

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

    protected Documento() {
    }

    public static Documento novo(UUID contratoId, String nome, String tipoArquivo, long tamanhoBytes, UUID usuario) {
        Documento documento = new Documento();
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        documento.id = UUID.randomUUID();
        documento.contratoId = contratoId;
        documento.nome = nome;
        documento.tipoArquivo = tipoArquivo;
        documento.tamanhoBytes = tamanhoBytes;
        documento.criadoEm = agora;
        documento.atualizadoEm = agora;
        documento.criadoPor = usuario;
        documento.atualizadoPor = usuario;
        documento.novo = true;
        return documento;
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

    public String getNome() {
        return nome;
    }

    public String getTipoArquivo() {
        return tipoArquivo;
    }

    public long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public UUID getCriadoPor() {
        return criadoPor;
    }
}
