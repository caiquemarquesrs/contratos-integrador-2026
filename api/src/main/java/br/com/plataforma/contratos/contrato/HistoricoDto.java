package br.com.plataforma.contratos.contrato;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Item do GET /api/contratos/contratos/{id}/historico (schema HistoricoContrato do contratos.yaml). */
public record HistoricoDto(
        UUID id,
        UUID contratoId,
        String campo,
        String valorAnterior,
        String valorNovo,
        UUID usuarioId,
        OffsetDateTime ocorridoEm) {

    static HistoricoDto de(HistoricoContrato h) {
        return new HistoricoDto(h.getId(), h.getContratoId(), h.getCampo(), h.getValorAnterior(),
                h.getValorNovo(), h.getUsuarioId(), h.getOcorridoEm());
    }
}
