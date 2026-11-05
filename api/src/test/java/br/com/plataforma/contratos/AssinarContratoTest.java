package br.com.plataforma.contratos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import br.com.plataforma.contratos.contrato.StatusContrato;

/** PR 3b: POST /api/contratos/contratos/{id}/assinar e o campo assinatura do contrato. */
class AssinarContratoTest extends BaseContratos {

    private static String corpo(UUID assinadoPor) {
        return "{\"assinadoPor\":\"" + assinadoPor + "\"}";
    }

    @Test
    void semTokenResponde401() throws Exception {
        mvc.perform(post(BASE + "/" + UUID.randomUUID() + "/assinar")
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void semPermissaoResponde403() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);
        mudarStatus(id, "AGUARDANDO_ASSINATURA");

        // corpo válido: a validação do corpo roda antes da checagem de permissão do método
        mvc.perform(post(BASE + "/" + id + "/assinar")
                        .with(usuario(tenant, VER, CRIAR, EDITAR, APROVAR))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
        assertEquals("AGUARDANDO_ASSINATURA", statusNoBanco(id));
    }

    @Test
    void contratoAindaNaoAssinadoTemAssinaturaNula() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);

        mvc.perform(get(BASE + "/" + id).with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.assinatura").value(Matchers.nullValue()));
    }

    @Test
    void assinaEVaiParaAtivoComAAssinaturaDoCorpo() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID usuarioDoToken = UUID.randomUUID();
        UUID quemAssinou = UUID.randomUUID();
        UUID id = criarContrato(tenant);
        mudarStatus(id, "AGUARDANDO_ASSINATURA");

        String resposta = mvc.perform(post(BASE + "/" + id + "/assinar")
                        .with(usuarioComId(tenant, usuarioDoToken, ASSINAR))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(quemAssinou)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ATIVO"))
                .andExpect(jsonPath("$.data.assinatura.assinadoPor").value(quemAssinou.toString()))
                .andExpect(jsonPath("$.data.assinatura.assinadoEm").exists())
                .andReturn().getResponse().getContentAsString();

        // assinado_por é o do corpo; updated_by é o usuário do token
        UUID assinadoPorNoBanco = jdbc.queryForObject(
                "select assinado_por from contratos.contratos where id = ?", UUID.class, id);
        UUID atualizadoPor = jdbc.queryForObject(
                "select updated_by from contratos.contratos where id = ?", UUID.class, id);
        assertEquals(quemAssinou, assinadoPorNoBanco);
        assertEquals(usuarioDoToken, atualizadoPor);

        // um GET depois devolve a mesma assinatura
        String leitura = mvc.perform(get(BASE + "/" + id).with(usuario(tenant, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ATIVO"))
                .andExpect(jsonPath("$.data.assinatura.assinadoPor").value(quemAssinou.toString()))
                .andReturn().getResponse().getContentAsString();
        // o mesmo instante, mesmo que o fuso do texto seja diferente
        OffsetDateTime naAssinatura = OffsetDateTime.parse(JsonPath.read(resposta, "$.data.assinatura.assinadoEm"));
        OffsetDateTime naLeitura = OffsetDateTime.parse(JsonPath.read(leitura, "$.data.assinatura.assinadoEm"));
        assertNotNull(naAssinatura);
        assertEquals(naAssinatura.toInstant(), naLeitura.toInstant());
    }

    @Test
    void documentoAssinadoIdEAceitoMasNaoEGravado() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);
        mudarStatus(id, "AGUARDANDO_ASSINATURA");

        mvc.perform(post(BASE + "/" + id + "/assinar")
                        .with(usuario(tenant, ASSINAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assinadoPor\":\"" + UUID.randomUUID() + "\",\"documentoAssinadoId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ATIVO"))
                .andExpect(jsonPath("$.data.documentoAssinadoId").doesNotExist());
    }

    @Test
    void semAssinadoPorResponde400ENaoMudaOEstado() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);
        mudarStatus(id, "AGUARDANDO_ASSINATURA");

        mvc.perform(post(BASE + "/" + id + "/assinar")
                        .with(usuario(tenant, ASSINAR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("assinadoPor"));
        assertEquals("AGUARDANDO_ASSINATURA", statusNoBanco(id));
    }

    @Test
    void emQualquerOutroEstadoResponde409ENaoMudaOEstado() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);

        for (StatusContrato estado : StatusContrato.values()) {
            if (estado == StatusContrato.AGUARDANDO_ASSINATURA) {
                continue;
            }
            mudarStatus(id, estado.name());

            mvc.perform(post(BASE + "/" + id + "/assinar")
                            .with(usuario(tenant, ASSINAR))
                            .contentType(MediaType.APPLICATION_JSON).content(corpo(UUID.randomUUID())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("O contrato precisa estar em AGUARDANDO_ASSINATURA para ser assinado."))
                    .andExpect(jsonPath("$.errors[0].campo").value("status"))
                    .andExpect(jsonPath("$.errors[0].codigo").value("TRANSICAO_INVALIDA"))
                    .andExpect(jsonPath("$.errors[0].detalhe").value("Estado atual: " + estado + "."));

            assertEquals(estado.name(), statusNoBanco(id), "o 409 não pode mudar o estado (" + estado + ")");
        }
    }

    @Test
    void contratoDeOutraEmpresaResponde404() throws Exception {
        UUID id = criarContrato(UUID.randomUUID());
        mudarStatus(id, "AGUARDANDO_ASSINATURA");

        mvc.perform(post(BASE + "/" + id + "/assinar")
                        .with(usuario(UUID.randomUUID(), ASSINAR))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(UUID.randomUUID())))
                .andExpect(status().isNotFound());
        assertEquals("AGUARDANDO_ASSINATURA", statusNoBanco(id));
    }
}
