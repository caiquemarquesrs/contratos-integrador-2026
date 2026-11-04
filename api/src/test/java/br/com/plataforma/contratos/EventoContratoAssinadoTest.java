package br.com.plataforma.contratos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import br.com.plataforma.contratos.contrato.StatusContrato;
import br.com.plataforma.contratos.eventos.Evento;

/** PR 4: o evento contratos.contrato.assinado, que o Financeiro usa para começar a cobrança recorrente. */
class EventoContratoAssinadoTest extends BaseContratos {

    private static String corpo() {
        return "{\"assinadoPor\":\"" + UUID.randomUUID() + "\"}";
    }

    @Test
    void assinarComSucessoPublicaOEventoComOFormatoDoContrato() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID usuarioDoToken = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();
        UUID id = criarContratoDaEmpresa(tenant, empresa);
        mudarStatus(id, "AGUARDANDO_ASSINATURA");

        mvc.perform(post(BASE + "/" + id + "/assinar")
                        .with(usuarioComId(tenant, usuarioDoToken, ASSINAR))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo()))
                .andExpect(status().isOk());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).convertAndSend(eq("contratos.eventos"), eq("contratos.contrato.assinado"), captor.capture());

        Evento<?> evento = (Evento<?>) captor.getValue();
        assertThat(evento.tipo()).isEqualTo("contratos.contrato.assinado");
        assertThat(evento.versao()).isEqualTo(1);
        assertThat(evento.tenantId()).isEqualTo(tenant);
        assertThat(evento.moduloOrigem()).isEqualTo("contratos");
        assertThat(evento.usuarioId()).isEqualTo(usuarioDoToken);
        assertThat(evento.id()).isNotNull();
        assertThat(evento.ocorridoEm()).isNotNull();

        // "dados" tem exatamente estas três chaves, e mais nada
        @SuppressWarnings("unchecked")
        Map<String, Object> dados = (Map<String, Object>) evento.dados();
        assertThat(dados).containsOnlyKeys("contratoId", "empresaId", "valorMensal");
        assertThat(dados.get("contratoId")).isEqualTo(id);
        assertThat(dados.get("empresaId")).isEqualTo(empresa);
        assertThat((BigDecimal) dados.get("valorMensal")).isEqualByComparingTo("1500.00");
        assertThat(((BigDecimal) dados.get("valorMensal")).scale()).isEqualTo(2);
    }

    @Test
    void assinarEmEstadoErradoNaoPublicaNada() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);   // RASCUNHO: não pode ser assinado

        mvc.perform(post(BASE + "/" + id + "/assinar")
                        .with(usuario(tenant, ASSINAR))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo()))
                .andExpect(status().isConflict());

        verify(rabbit, never()).convertAndSend(eq("contratos.eventos"), eq("contratos.contrato.assinado"), any(Object.class));
    }

    @Test
    void assinarComCorpoInvalidoNaoPublicaNada() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);
        mudarStatus(id, StatusContrato.AGUARDANDO_ASSINATURA.name());

        mvc.perform(post(BASE + "/" + id + "/assinar")
                        .with(usuario(tenant, ASSINAR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verify(rabbit, never()).convertAndSend(eq("contratos.eventos"), eq("contratos.contrato.assinado"), any(Object.class));
    }

    @Test
    void aprovarNaoPublicaEvento() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);
        mudarStatus(id, "EM_APROVACAO");

        mvc.perform(post(BASE + "/" + id + "/aprovar").with(usuario(tenant, APROVAR)))
                .andExpect(status().isOk());

        verify(rabbit, never()).convertAndSend(eq("contratos.eventos"), eq("contratos.contrato.assinado"), any(Object.class));
    }

    private UUID criarContratoDaEmpresa(UUID tenant, UUID empresa) throws Exception {
        String resposta = mvc.perform(post(BASE)
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoNovo(empresa, UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(resposta, "$.data.id"));
    }
}
