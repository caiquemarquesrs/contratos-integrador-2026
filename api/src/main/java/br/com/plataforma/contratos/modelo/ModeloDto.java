package br.com.plataforma.contratos.modelo;

import java.time.OffsetDateTime;
import java.util.UUID;

/** O que a API devolve (schema Modelo do contratos.yaml) — nunca a entidade. */
public record ModeloDto(UUID id, String nome, String descricao, String conteudo, OffsetDateTime criadoEm) {

    static ModeloDto de(Modelo modelo) {
        return new ModeloDto(modelo.getId(), modelo.getNome(), modelo.getDescricao(),
                modelo.getConteudo(), modelo.getCriadoEm());
    }
}
