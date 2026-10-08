package br.com.plataforma.contratos.item;

import java.time.OffsetDateTime;
import java.util.UUID;

/** O que a API devolve — nunca a entidade, para não vazar tenant_id nem colunas de auditoria. */
public record ItemDto(UUID id, String nome, String descricao, OffsetDateTime criadoEm) {

    static ItemDto de(Item item) {
        return new ItemDto(item.getId(), item.getNome(), item.getDescricao(), item.getCriadoEm());
    }
}
