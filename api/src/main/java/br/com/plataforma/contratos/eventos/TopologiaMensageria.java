package br.com.plataforma.contratos.eventos;

import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exchange do módulo. Regra das permissões do broker: o módulo declara só o que começa com o
 * próprio código. Este módulo só PUBLICA eventos (contratos.contrato.assinado, para o Financeiro);
 * quem consome cria a própria fila e a liga a esta exchange, então aqui não há fila nem consumidor.
 */
@Configuration
public class TopologiaMensageria {

    static final String EXCHANGE_PROPRIA = "contratos.eventos";

    @Bean
    TopicExchange exchangePropria() {
        return new TopicExchange(EXCHANGE_PROPRIA, true, false);
    }

    /**
     * Toda mensagem sai com a propriedade user_id = usuário da conexão, mq_contratos (Contrato §9.7).
     * O RabbitMQ confere que é mesmo quem está conectado, e o identity recusa, direto para a .dlq,
     * pedido em identity.entrada sem ela ou com moduloOrigem de outro módulo.
     */
    @Bean
    RabbitTemplateCustomizer remetenteEmTodaMensagem(@Value("${spring.rabbitmq.username}") String usuario) {
        return template -> template.addBeforePublishPostProcessors(comRemetente(usuario));
    }

    static MessagePostProcessor comRemetente(String usuario) {
        return mensagem -> {
            mensagem.getMessageProperties().setUserId(usuario);
            return mensagem;
        };
    }

    /** JSON nos dois sentidos. O tipo vem do parâmetro do listener, não de cabeçalho Java de quem publicou. */
    @Bean
    MessageConverter conversorJson(ObjectMapper mapper) {
        Jackson2JsonMessageConverter conversor = new Jackson2JsonMessageConverter(mapper);
        conversor.setAlwaysConvertToInferredType(true);
        return conversor;
    }
}
