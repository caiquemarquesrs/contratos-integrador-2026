package br.com.plataforma.contratos.item;

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
 * Entidade de negócio com as sete colunas obrigatórias (Contrato §7.2).
 *
 * {@code @TenantId}: o Hibernate grava o tenant do contexto e filtra as consultas por ele.
 * {@code @SQLDelete} + {@code @SQLRestriction}: excluir marca deleted_at, e registros excluídos
 * somem das consultas (soft delete).
 */
@Entity
@Table(name = "itens")
@SQLDelete(sql = "UPDATE contratos.itens SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Item implements Persistable<UUID> {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(columnDefinition = "text")
    private String descricao;

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

    protected Item() {
    }

    public static Item novo(String nome, String descricao, UUID usuario) {
        Item item = new Item();
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        item.id = UUID.randomUUID();
        item.nome = nome;
        item.descricao = descricao;
        item.criadoEm = agora;
        item.atualizadoEm = agora;
        item.criadoPor = usuario;
        item.atualizadoPor = usuario;
        item.novo = true;
        return item;
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

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }
}
