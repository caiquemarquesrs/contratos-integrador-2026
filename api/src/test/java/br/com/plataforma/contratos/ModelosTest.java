package br.com.plataforma.contratos;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

/** Tarefa 6: segurança, validação e isolamento por empresa dos endpoints de modelos. */
class ModelosTest extends BaseIntegracao {

    private static final String VER = "contratos.modelo.ver";
    private static final String ADMINISTRAR = "contratos.modelo.administrar";

    @Test
    void semTokenResponde401NoEnvelope() throws Exception {
        mvc.perform(get("/api/contratos/modelos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listarSemPermissaoResponde403() throws Exception {
        mvc.perform(get("/api/contratos/modelos").with(usuario(UUID.randomUUID(), "contratos.acessar")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void criarSoComPermissaoDeVerResponde403() throws Exception {
        mvc.perform(post("/api/contratos/modelos")
                        .with(usuario(UUID.randomUUID(), VER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Modelo\",\"conteudo\":\"Texto\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void criaEDevolveOModeloNoFormatoDoContrato() throws Exception {
        UUID empresa = UUID.randomUUID();
        String id = criarModelo(empresa, "Prestação de serviços");

        mvc.perform(get("/api/contratos/modelos/" + id).with(usuario(empresa, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.nome").value("Prestação de serviços"))
                .andExpect(jsonPath("$.data.conteudo").value("Contrato com {{cliente.razaoSocial}}"));

        mvc.perform(get("/api/contratos/modelos").with(usuario(empresa, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.itens[0].id").value(id));
    }

    @Test
    void modeloDeOutraEmpresaResponde404() throws Exception {
        UUID empresaA = UUID.randomUUID();
        UUID empresaB = UUID.randomUUID();
        String id = criarModelo(empresaA, "Modelo da empresa A");

        mvc.perform(get("/api/contratos/modelos/" + id).with(usuario(empresaB, VER)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/contratos/modelos").with(usuario(empresaB, VER)))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void tenantEnviadoNoCorpoEIgnorado() throws Exception {
        UUID empresaDoToken = UUID.randomUUID();
        UUID empresaDoCorpo = UUID.randomUUID();

        mvc.perform(post("/api/contratos/modelos")
                        .with(usuario(empresaDoToken, ADMINISTRAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Tentativa\",\"conteudo\":\"Texto\",\"tenantId\":\"" + empresaDoCorpo + "\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/contratos/modelos").with(usuario(empresaDoCorpo, VER)))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/contratos/modelos").with(usuario(empresaDoToken, VER)))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void nomeEmBrancoResponde400ComOCampo() throws Exception {
        mvc.perform(post("/api/contratos/modelos")
                        .with(usuario(UUID.randomUUID(), ADMINISTRAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  \",\"conteudo\":\"Texto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("nome"));
    }

    @Test
    void conteudoAusenteResponde400ComOCampo() throws Exception {
        mvc.perform(post("/api/contratos/modelos")
                        .with(usuario(UUID.randomUUID(), ADMINISTRAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Modelo sem texto\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("conteudo"));
    }

    private String criarModelo(UUID tenant, String nome) throws Exception {
        String corpo = mvc.perform(post("/api/contratos/modelos")
                        .with(usuario(tenant, ADMINISTRAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"conteudo\":\"Contrato com {{cliente.razaoSocial}}\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/contratos/modelos/")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.data.id");
    }
}
