package br.com.plataforma.contratos.contrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * O que a API devolve (schema Contrato do contratos.yaml): nunca a entidade, que teria tenantId e
 * colunas de auditoria. Montado dentro do serviço, onde a lista de serviços (lazy) ainda carrega.
 */
public record ContratoDto(
        UUID id,
        String numero,
        UUID empresaId,
        String empresaNome,
        UUID modeloId,
        List<ServicoDto> servicosContratados,
        BigDecimal valorMensal,
        BigDecimal mrr,
        LocalDate dataInicio,
        Integer vigenciaMeses,
        String renovacao,
        BigDecimal reajusteAnualPercentual,
        String indiceReajuste,
        int avisoPrevioDias,
        UUID responsavelId,
        String origem,
        String origemModuloId,
        StatusContrato status,
        Object assinatura,   // sempre null até a Tarefa 3 (assinar)
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {

    public record ServicoDto(UUID itemCatalogoId, String descricao, BigDecimal quantidade,
                             BigDecimal valorUnitario, BigDecimal valorTotal) {

        static ServicoDto de(ServicoContratado s) {
            return new ServicoDto(s.getItemCatalogoId(), s.getDescricao(), s.getQuantidade(),
                    s.getValorUnitario(), s.getValorTotal());
        }
    }

    static ContratoDto de(Contrato c) {
        return new ContratoDto(c.getId(), c.getNumero(), c.getEmpresaId(), c.getEmpresaNome(), c.getModeloId(),
                c.getServicos().stream().map(ServicoDto::de).toList(),
                c.getValorMensal(), c.getMrr(), c.getDataInicio(), c.getVigenciaMeses(), c.getRenovacao(),
                c.getReajusteAnualPercentual(), c.getIndiceReajuste(), c.getAvisoPrevioDias(),
                c.getResponsavelId(), c.getOrigem(), c.getOrigemModuloId(), c.getStatus(),
                null, c.getCriadoEm(), c.getAtualizadoEm());
    }
}
