package br.com.plataforma.contratos.integracao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.com.plataforma.contratos.contrato.StatusContrato;

/**
 * Schema ContratoResumo do contratos.yaml, lido por outros módulos (Financeiro, CRM). Nunca a
 * entidade: sem tenantId, sem auditoria e sem a lista de serviços.
 */
public record ContratoResumo(
        UUID id,
        String numero,
        String empresaNome,          // nunca nulo: sem nome gravado vai o texto de reserva (§9.6)
        StatusContrato status,
        BigDecimal valorMensal,
        LocalDate vigenciaFimPrevista) {   // pode ser nula (contrato sem prazo)
}
