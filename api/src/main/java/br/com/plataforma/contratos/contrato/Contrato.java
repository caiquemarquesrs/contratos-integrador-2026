package br.com.plataforma.contratos.contrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.TenantId;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Contrato firmado com uma empresa-cliente (RF-01). Tem as sete colunas obrigatórias
 * (Contrato de Integração §7.2). Dinheiro é sempre BigDecimal (§8.7).
 */
@Entity
@Table(name = "contratos")
@SQLDelete(sql = "UPDATE contratos.contratos SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Contrato implements Persistable<UUID> {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 20, updatable = false)
    private String numero;

    @Column(name = "empresa_id", nullable = false, updatable = false)
    private UUID empresaId;

    /** Cópia do nome da empresa no cadastro (§9.5). Por enquanto fica nula: veja ContratoServico. */
    @Column(name = "empresa_nome", length = 255)
    private String empresaNome;

    @Column(name = "modelo_id")
    private UUID modeloId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusContrato status;

    @Column(name = "valor_mensal", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorMensal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal mrr;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "vigencia_meses")
    private Integer vigenciaMeses;

    /** Derivada: dataInicio + vigenciaMeses. Não aparece no JSON; serve para ordenar e para as próximas tarefas. */
    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @Column(nullable = false, length = 20)
    private String renovacao;

    @Column(name = "reajuste_anual_percentual", precision = 9, scale = 4)
    private BigDecimal reajusteAnualPercentual;

    @Column(name = "indice_reajuste", nullable = false, length = 10)
    private String indiceReajuste;

    @Column(name = "aviso_previo_dias", nullable = false)
    private int avisoPrevioDias;

    @Column(name = "responsavel_id", nullable = false)
    private UUID responsavelId;

    @Column(nullable = false, length = 100, updatable = false)
    private String origem;

    @Column(name = "origem_modulo_id", nullable = false, length = 255, updatable = false)
    private String origemModuloId;

    /** Preenchidas na Tarefa 3 (assinatura). */
    @Column(name = "assinado_em")
    private OffsetDateTime assinadoEm;

    @Column(name = "assinado_por")
    private UUID assinadoPor;

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

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<ServicoContratado> servicos = new ArrayList<>();

    /** Id gerado pela aplicação: sem isto, o Spring Data faria um SELECT antes de cada INSERT. */
    @Transient
    private boolean novo;

    protected Contrato() {
    }

    /** Dados que o cliente informa ao criar ou editar um serviço do contrato. */
    public record DadosServico(UUID itemCatalogoId, String descricao, BigDecimal quantidade,
                               BigDecimal valorUnitario, BigDecimal valorTotal) {
    }

    /** Dados editáveis do contrato (os mesmos na criação e na edição, fora empresa e origem). */
    public record Dados(UUID modeloId, BigDecimal valorMensal, BigDecimal mrr, LocalDate dataInicio,
                        Integer vigenciaMeses, String renovacao, BigDecimal reajusteAnualPercentual,
                        String indiceReajuste, int avisoPrevioDias, UUID responsavelId,
                        List<DadosServico> servicos) {
    }

    public static Contrato novo(String numero, UUID empresaId, String origem, String origemModuloId,
                                Dados dados, UUID usuario) {
        Contrato contrato = new Contrato();
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        contrato.id = UUID.randomUUID();
        contrato.numero = numero;
        contrato.empresaId = empresaId;
        contrato.origem = origem;
        contrato.origemModuloId = origemModuloId;
        contrato.status = StatusContrato.RASCUNHO;
        contrato.criadoEm = agora;
        contrato.criadoPor = usuario;
        contrato.novo = true;
        contrato.aplicar(dados, usuario, agora);
        return contrato;
    }

    /** Troca os dados editáveis e a lista de serviços (os antigos saem por exclusão lógica). */
    public void editar(Dados dados, UUID usuario) {
        aplicar(dados, usuario, OffsetDateTime.now(ZoneOffset.UTC));
    }

    private void aplicar(Dados dados, UUID usuario, OffsetDateTime agora) {
        this.modeloId = dados.modeloId();
        this.valorMensal = dados.valorMensal();
        this.mrr = dados.mrr();
        this.dataInicio = dados.dataInicio();
        this.vigenciaMeses = dados.vigenciaMeses();
        this.vigenciaFim = dados.vigenciaMeses() == null ? null : dados.dataInicio().plusMonths(dados.vigenciaMeses());
        this.renovacao = dados.renovacao();
        this.reajusteAnualPercentual = dados.reajusteAnualPercentual();
        this.indiceReajuste = dados.indiceReajuste() == null ? "nenhum" : dados.indiceReajuste();
        this.avisoPrevioDias = dados.avisoPrevioDias();
        this.responsavelId = dados.responsavelId();
        this.atualizadoEm = agora;
        this.atualizadoPor = usuario;

        this.servicos.clear();   // orphanRemoval: as linhas antigas recebem deleted_at
        for (DadosServico s : dados.servicos()) {
            this.servicos.add(ServicoContratado.novo(this, s.itemCatalogoId(), s.descricao().strip(),
                    s.quantidade(), s.valorUnitario(), s.valorTotal(), usuario));
        }
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

    public String getNumero() {
        return numero;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public String getEmpresaNome() {
        return empresaNome;
    }

    public UUID getModeloId() {
        return modeloId;
    }

    public StatusContrato getStatus() {
        return status;
    }

    public BigDecimal getValorMensal() {
        return valorMensal;
    }

    public BigDecimal getMrr() {
        return mrr;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public Integer getVigenciaMeses() {
        return vigenciaMeses;
    }

    public String getRenovacao() {
        return renovacao;
    }

    public BigDecimal getReajusteAnualPercentual() {
        return reajusteAnualPercentual;
    }

    public String getIndiceReajuste() {
        return indiceReajuste;
    }

    public int getAvisoPrevioDias() {
        return avisoPrevioDias;
    }

    public UUID getResponsavelId() {
        return responsavelId;
    }

    public String getOrigem() {
        return origem;
    }

    public String getOrigemModuloId() {
        return origemModuloId;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public OffsetDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public List<ServicoContratado> getServicos() {
        return servicos;
    }
}
