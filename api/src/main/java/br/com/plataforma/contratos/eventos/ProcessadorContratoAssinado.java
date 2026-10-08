package br.com.plataforma.contratos.eventos;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.plataforma.contratos.item.Item;
import br.com.plataforma.contratos.item.ItemRepositorio;

@Service
public class ProcessadorContratoAssinado {

    private static final Logger log = LoggerFactory.getLogger(ProcessadorContratoAssinado.class);

    private final EventosProcessados eventosProcessados;
    private final ItemRepositorio itens;

    public ProcessadorContratoAssinado(EventosProcessados eventosProcessados, ItemRepositorio itens) {
        this.eventosProcessados = eventosProcessados;
        this.itens = itens;
    }

    /** Idempotente: a mesma mensagem entregue duas vezes produz um único item. */
    @Transactional
    public void processar(Evento<Map<String, Object>> evento) {
        if (!eventosProcessados.registrar(evento.id(), evento.tipo())) {
            log.info("Evento {} ({}) já processado; ignorado.", evento.tipo(), evento.id());
            return;
        }
        Object contrato = evento.dados().getOrDefault("contratoId", "sem número");
        itens.save(Item.novo("Contrato " + contrato + " assinado", "Criado pelo evento " + evento.id(), evento.usuarioId()));
    }
}
