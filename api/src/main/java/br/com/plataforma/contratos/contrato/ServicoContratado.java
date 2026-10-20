package br.com.plataforma.contratos.contrato;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Linha de serviço de um contrato. Exclusão lógica, como toda tabela de negócio (Contrato §7.2). */
@Entity
@Table(name = "servicos_contratados")
@SQLDelete(sql = "UPDATE contratos.servicos_contratados SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class ServicoContratado {

    /** Gerado pelo Hibernate: assim ele sabe sozinho que a linha é nova (sem SELECT antes do INSERT). */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false, updatable = false)
    private Contrato contrato;

    @Column(name = "item_catalogo_id")
    private UUID itemCatalogoId;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

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

    protected ServicoContratado() {
    }

    static ServicoContratado novo(Contrato contrato, UUID itemCatalogoId, String descricao, BigDecimal quantidade,
                                  BigDecimal valorUnitario, BigDecimal valorTotal, UUID usuario) {
        ServicoContratado servico = new ServicoContratado();
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        servico.contrato = contrato;
        servico.itemCatalogoId = itemCatalogoId;
        servico.descricao = descricao;
        servico.quantidade = quantidade;
        servico.valorUnitario = valorUnitario;
        servico.valorTotal = valorTotal;
        servico.criadoEm = agora;
        servico.atualizadoEm = agora;
        servico.criadoPor = usuario;
        servico.atualizadoPor = usuario;
        return servico;
    }

    public UUID getId() {
        return id;
    }

    public UUID getItemCatalogoId() {
        return itemCatalogoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }
}
