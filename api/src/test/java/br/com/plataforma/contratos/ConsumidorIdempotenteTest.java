package br.com.plataforma.contratos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.plataforma.contratos.eventos.ConsumidorContratoAssinado;
import br.com.plataforma.contratos.eventos.Evento;
import br.com.plataforma.contratos.item.ItemRepositorio;
import br.com.plataforma.contratos.tenant.TenantContexto;

/** O caso mínimo de mensageria do checklist: mesma mensagem duas vezes, um efeito só. */
class ConsumidorIdempotenteTest extends BaseIntegracao {

    @Autowired
    ConsumidorContratoAssinado consumidor;

    @Autowired
    ItemRepositorio itens;

    @Test
    void mesmaMensagemDuasVezesGeraUmItemSo() {
        UUID empresa = UUID.randomUUID();
        Evento<Map<String, Object>> evento = new Evento<>(
                UUID.randomUUID(), "contratos.contrato.assinado", 1, empresa, "contratos",
                OffsetDateTime.now(ZoneOffset.UTC), null, null, Map.of("contratoId", "CT-0042"));

        consumidor.receber(evento);
        consumidor.receber(evento);

        assertThat(totalDeItens(empresa)).isEqualTo(1L);
    }

    @Test
    void efeitoFicaNoTenantDaMensagem() {
        UUID empresa = UUID.randomUUID();
        consumidor.receber(new Evento<>(
                UUID.randomUUID(), "contratos.contrato.assinado", 1, empresa, "contratos",
                OffsetDateTime.now(ZoneOffset.UTC), null, null, Map.<String, Object>of("contratoId", "CT-0099")));

        assertThat(totalDeItens(UUID.randomUUID())).isZero();
        assertThat(totalDeItens(empresa)).isEqualTo(1L);
    }

    private long totalDeItens(UUID tenant) {
        return TenantContexto.calcular(tenant, () -> itens.count());
    }
}
