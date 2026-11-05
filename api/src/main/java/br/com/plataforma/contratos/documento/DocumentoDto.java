package br.com.plataforma.contratos.documento;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Schema Documento do contratos.yaml. Nunca devolve a entidade, e nunca o conteúdo do arquivo. */
public record DocumentoDto(
        UUID id,
        UUID contratoId,
        String nome,
        String tipoArquivo,
        long tamanhoBytes,
        String urlDownload,
        OffsetDateTime criadoEm,
        UUID criadoPor) {

    static DocumentoDto de(Documento d) {
        return new DocumentoDto(d.getId(), d.getContratoId(), d.getNome(), d.getTipoArquivo(), d.getTamanhoBytes(),
                "/api/contratos/documentos/" + d.getId() + "/download", d.getCriadoEm(), d.getCriadoPor());
    }
}
