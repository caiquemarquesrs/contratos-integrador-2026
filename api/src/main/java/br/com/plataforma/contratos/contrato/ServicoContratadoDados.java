package br.com.plataforma.contratos.contrato;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Um item de servicosContratados no corpo do POST e do PUT (schema ServicoContratado do contratos.yaml). */
public record ServicoContratadoDados(
        UUID itemCatalogoId,

        @NotBlank(message = "Informe a descrição do serviço.")
        @Size(max = 255, message = "A descrição pode ter até 255 caracteres.")
        String descricao,

        @NotNull(message = "Informe a quantidade.")
        @DecimalMin(value = "0.0001", message = "A quantidade deve ser maior que zero.")
        @Digits(integer = 11, fraction = 4, message = "A quantidade aceita no máximo 4 casas decimais.")
        BigDecimal quantidade,

        @NotNull(message = "Informe o valor unitário.")
        @DecimalMin(value = "0", message = "O valor unitário não pode ser negativo.")
        @Digits(integer = 13, fraction = 2, message = "Valor monetário aceita no máximo 2 casas decimais.")
        BigDecimal valorUnitario,

        @NotNull(message = "Informe o valor total.")
        @DecimalMin(value = "0", message = "O valor total não pode ser negativo.")
        @Digits(integer = 13, fraction = 2, message = "Valor monetário aceita no máximo 2 casas decimais.")
        BigDecimal valorTotal) {

    Contrato.DadosServico dados() {
        return new Contrato.DadosServico(itemCatalogoId, descricao, quantidade, valorUnitario, valorTotal);
    }
}
