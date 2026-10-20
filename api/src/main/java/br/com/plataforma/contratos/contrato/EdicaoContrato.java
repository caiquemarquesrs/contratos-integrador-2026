package br.com.plataforma.contratos.contrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Corpo do PUT /api/contratos/contratos/{id} (schema EdicaoContrato do contratos.yaml).
 * Não tem empresaId, origem nem origemModuloId: esses três não mudam depois de criado.
 */
public record EdicaoContrato(
        UUID modeloId,

        @NotEmpty(message = "Informe ao menos um serviço contratado.")
        List<@NotNull(message = "Serviço inválido.") @Valid ServicoContratadoDados> servicosContratados,

        @NotNull(message = "Informe o valor mensal.")
        @DecimalMin(value = "0", message = "O valor mensal não pode ser negativo.")
        @Digits(integer = 13, fraction = 2, message = "Valor monetário aceita no máximo 2 casas decimais.")
        BigDecimal valorMensal,

        @NotNull(message = "Informe o MRR.")
        @DecimalMin(value = "0", message = "O MRR não pode ser negativo.")
        @Digits(integer = 13, fraction = 2, message = "Valor monetário aceita no máximo 2 casas decimais.")
        BigDecimal mrr,

        @NotNull(message = "Informe a data de início.")
        LocalDate dataInicio,

        @Min(value = 1, message = "A vigência deve ser de no mínimo 1 mês.")
        Integer vigenciaMeses,

        @NotBlank(message = "Informe o tipo de renovação.")
        @Pattern(regexp = "automatica|manual|sem_renovacao",
                message = "Renovação deve ser automatica, manual ou sem_renovacao.")
        String renovacao,

        @DecimalMin(value = "0", message = "O reajuste não pode ser negativo.")
        @DecimalMax(value = "100", message = "O reajuste não pode passar de 100%.")
        @Digits(integer = 3, fraction = 2, message = "O reajuste aceita no máximo 2 casas decimais.")
        BigDecimal reajusteAnualPercentual,

        @Pattern(regexp = "IPCA|IGPM|INPC|nenhum", message = "Índice deve ser IPCA, IGPM, INPC ou nenhum.")
        String indiceReajuste,

        @NotNull(message = "Informe o aviso prévio em dias.")
        @Min(value = 0, message = "O aviso prévio não pode ser negativo.")
        Integer avisoPrevioDias,

        @NotNull(message = "Informe o responsável.")
        UUID responsavelId) {

    Contrato.Dados dados() {
        return new Contrato.Dados(modeloId, valorMensal, mrr, dataInicio, vigenciaMeses, renovacao,
                reajusteAnualPercentual, indiceReajuste, avisoPrevioDias, responsavelId,
                servicosContratados.stream().map(ServicoContratadoDados::dados).toList());
    }
}
