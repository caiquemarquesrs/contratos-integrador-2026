package br.com.plataforma.contratos.eventos;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica na exchange do próprio módulo, e só depois do commit: evento de algo desfeito por
 * rollback é pior que evento nenhum (Contrato §9.7).
 */
@Component
public class PublicadorDeEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorDeEventos.class);

    private final RabbitTemplate rabbit;
    private final String modulo;

    public PublicadorDeEventos(RabbitTemplate rabbit, @Value("${modulo.codigo}") String modulo) {
        this.rabbit = rabbit;
        this.modulo = modulo;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(FatoOcorrido fato) {
        Evento<Map<String, Object>> evento =
                Evento.novo(fato.tipo(), fato.tenantId(), modulo, fato.usuarioId(), fato.dados());
        try {
            rabbit.convertAndSend(modulo + ".eventos", fato.tipo(), evento);
        } catch (AmqpException e) {
            // Sem o padrão outbox, o evento se perde aqui. Risco aceito no semestre (Contrato §9.7).
            log.error("Evento {} ({}) não publicado: {}", evento.tipo(), evento.id(), e.getMessage());
        }
    }
}
