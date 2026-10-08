package br.com.plataforma.contratos.eventos;

import java.util.Map;
import java.util.UUID;

/** Fato de domínio registrado durante a transação; vira mensagem só depois do commit. */
public record FatoOcorrido(String tipo, UUID tenantId, UUID usuarioId, Map<String, Object> dados) {
}
