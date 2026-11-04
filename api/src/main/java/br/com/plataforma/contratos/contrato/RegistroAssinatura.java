package br.com.plataforma.contratos.contrato;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Corpo do POST /api/contratos/contratos/{id}/assinar (schema RegistroAssinatura do contratos.yaml). */
public record RegistroAssinatura(
        @NotNull(message = "Informe quem assinou.")
        UUID assinadoPor,

        UUID documentoAssinadoId) {
}
