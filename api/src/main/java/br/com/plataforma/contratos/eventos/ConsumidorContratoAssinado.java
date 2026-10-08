package br.com.plataforma.contratos.eventos;

import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import br.com.plataforma.contratos.tenant.TenantContexto;

/**
 * Consome contratos.contrato.assinado. Mensagem não carrega token: o tenant vem do envelope,
 * e é definido antes de abrir a transação (Contrato §9.7).
 */
@Component
public class ConsumidorContratoAssinado {

    private final ProcessadorContratoAssinado processador;

    public ConsumidorContratoAssinado(ProcessadorContratoAssinado processador) {
        this.processador = processador;
    }

    @RabbitListener(queues = TopologiaMensageria.FILA_CONTRATO_ASSINADO)
    public void receber(Evento<Map<String, Object>> evento) {
        TenantContexto.executar(evento.tenantId(), () -> processador.processar(evento));
    }
}
