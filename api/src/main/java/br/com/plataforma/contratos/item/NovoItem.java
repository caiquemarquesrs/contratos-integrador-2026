package br.com.plataforma.contratos.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do POST. Um "tenantId" enviado aqui é simplesmente ignorado. */
public record NovoItem(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 200, message = "O nome pode ter até 200 caracteres.")
        String nome,

        @Size(max = 2000, message = "A descrição pode ter até 2000 caracteres.")
        String descricao) {
}
