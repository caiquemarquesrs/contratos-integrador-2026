package br.com.plataforma.contratos.eventos;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.slf4j.MDC;

import br.com.plataforma.contratos.observabilidade.CorrelacaoFiltro;

/**
 * Envelope de toda mensagem do RabbitMQ, igual em todos os módulos (Contrato §9.7).
 *
 * @param id           chave de idempotência: quem consome ignora um id já processado
 * @param tipo         modulo.entidade.acao, com o verbo no particípio
 * @param versao       sobe quando o formato de dados muda de forma incompatível
 * @param tenantId     a única fonte de tenant numa mensagem, que não carrega token
 * @param usuarioId    quem provocou o fato; nulo em rotina automática
 * @param correlacaoId X-Request-Id da requisição de origem
 */
public record Evento<T>(
        UUID id,
        String tipo,
        int versao,
        UUID tenantId,
        String moduloOrigem,
        OffsetDateTime ocorridoEm,
        UUID usuarioId,
        String correlacaoId,
        T dados) {

    public static <T> Evento<T> novo(String tipo, UUID tenantId, String moduloOrigem, UUID usuarioId, T dados) {
        return new Evento<>(
                UUID.randomUUID(), tipo, 1, tenantId, moduloOrigem,
                OffsetDateTime.now(ZoneOffset.UTC), usuarioId, MDC.get(CorrelacaoFiltro.CHAVE_MDC), dados);
    }
}
