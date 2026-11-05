package br.com.plataforma.contratos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.plataforma.contratos.contrato.StatusContrato;

/** PR 3a: POST /api/contratos/contratos/{id}/aprovar. */
class AprovarContratoTest extends BaseContratos {

    @Test
    void semTokenResponde401() throws Exception {
        mvc.perform(post(BASE + "/" + UUID.randomUUID() + "/aprovar"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void semPermissaoResponde403() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);
        mudarStatus(id, "EM_APROVACAO");

        mvc.perform(post(BASE + "/" + id + "/aprovar").with(usuario(tenant, VER, CRIAR, EDITAR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
        assertEquals("EM_APROVACAO", statusNoBanco(id));
    }

    @Test
    void aprovaContratoEmAprovacaoEVaiParaAguardandoAssinatura() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID aprovador = UUID.randomUUID();
        UUID id = criarContrato(tenant);
        mudarStatus(id, "EM_APROVACAO");

        mvc.perform(post(BASE + "/" + id + "/aprovar").with(usuarioComId(tenant, aprovador, APROVAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.status").value("AGUARDANDO_ASSINATURA"))
                .andExpect(jsonPath("$.data.servicosContratados.length()").value(1));

        // o que ficou no banco é o que a resposta mostrou, e a auditoria é do usuário do token
        mvc.perform(get(BASE + "/" + id).with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.status").value("AGUARDANDO_ASSINATURA"));
        UUID atualizadoPor = jdbc.queryForObject(
                "select updated_by from contratos.contratos where id = ?", UUID.class, id);
        assertEquals(aprovador, atualizadoPor);
    }

    @Test
    void emQualquerOutroEstadoResponde409ENaoMudaOEstado() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);

        for (StatusContrato estado : StatusContrato.values()) {
            if (estado == StatusContrato.EM_APROVACAO) {
                continue;
            }
            mudarStatus(id, estado.name());

            mvc.perform(post(BASE + "/" + id + "/aprovar").with(usuario(tenant, APROVAR)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("O contrato precisa estar em EM_APROVACAO para ser aprovado."))
                    .andExpect(jsonPath("$.errors[0].campo").value("status"))
                    .andExpect(jsonPath("$.errors[0].codigo").value("TRANSICAO_INVALIDA"))
                    .andExpect(jsonPath("$.errors[0].detalhe").value("Estado atual: " + estado + "."));

            assertEquals(estado.name(), statusNoBanco(id), "o 409 não pode mudar o estado (" + estado + ")");
        }
    }

    @Test
    void contratoDeOutraEmpresaResponde404() throws Exception {
        UUID empresaA = UUID.randomUUID();
        UUID id = criarContrato(empresaA);
        mudarStatus(id, "EM_APROVACAO");

        mvc.perform(post(BASE + "/" + id + "/aprovar").with(usuario(UUID.randomUUID(), APROVAR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        assertEquals("EM_APROVACAO", statusNoBanco(id));
    }

    @Test
    void contratoInexistenteResponde404() throws Exception {
        mvc.perform(post(BASE + "/" + UUID.randomUUID() + "/aprovar").with(usuario(UUID.randomUUID(), APROVAR)))
                .andExpect(status().isNotFound());
    }
}
