package br.com.plataforma.contratos.modelo;

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
 * Modelo de contrato (RF-03): um texto-base, com variáveis entre chaves, que reduz a criação manual
 * de contratos. Tem as sete colunas obrigatórias (Contrato de Integração §7.2).
 */
@Entity
@Table(name = "modelos")
@SQLDelete(sql = "UPDATE contratos.modelos SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Modelo implements Persistable<UUID> {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, columnDefinition = "text")
    private String conteudo;

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

    protected Modelo() {
    }

    public static Modelo novo(String nome, String descricao, String conteudo, UUID usuario) {
        Modelo modelo = new Modelo();
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        modelo.id = UUID.randomUUID();
        modelo.nome = nome;
        modelo.descricao = descricao;
        modelo.conteudo = conteudo;
        modelo.criadoEm = agora;
        modelo.atualizadoEm = agora;
        modelo.criadoPor = usuario;
        modelo.atualizadoPor = usuario;
        modelo.novo = true;
        return modelo;
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

    public UUID getTenantId() {
        return tenantId;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getConteudo() {
        return conteudo;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
