package br.com.plataforma.contratos.modelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do POST /api/contratos/modelos. Um "tenantId" enviado aqui é simplesmente ignorado. */
public record NovoModelo(
        @NotBlank(message = "Informe o nome.")
        @Size(max = 255, message = "O nome pode ter até 255 caracteres.")
        String nome,

        @Size(max = 500, message = "A descrição pode ter até 500 caracteres.")
        String descricao,

        @NotBlank(message = "Informe o conteúdo do modelo.")
        String conteudo) {
}
