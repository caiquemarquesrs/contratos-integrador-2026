package br.com.plataforma.contratos;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

/** Os itens do checklist de conformidade que dependem só deste módulo (Contrato §15). */
class SegurancaEIsolamentoTest extends BaseIntegracao {

    private static final String VER = "contratos.item.ver";
    private static final String CRIAR = "contratos.item.criar";
    private static final String EXCLUIR = "contratos.item.excluir";

    @Test
    void saudeRespondeSemToken() throws Exception {
        mvc.perform(get("/api/contratos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void semTokenResponde401NoEnvelope() throws Exception {
        mvc.perform(get("/api/contratos/itens"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Autenticação necessária."));
    }

    @Test
    void semPermissaoResponde403() throws Exception {
        mvc.perform(get("/api/contratos/itens").with(usuario(UUID.randomUUID(), "contratos.acessar")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registroDeOutroTenantResponde404() throws Exception {
        UUID empresaA = UUID.randomUUID();
        UUID empresaB = UUID.randomUUID();
        String id = criarItem(empresaA, "Item da empresa A");

        mvc.perform(get("/api/contratos/itens/" + id).with(usuario(empresaB, VER)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/contratos/itens").with(usuario(empresaB, VER)))
                .andExpect(jsonPath("$.data.total").value(0));

        mvc.perform(get("/api/contratos/itens/" + id).with(usuario(empresaA, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value("Item da empresa A"));
    }

    @Test
    void tenantEnviadoNoCorpoEIgnorado() throws Exception {
        UUID empresaDoToken = UUID.randomUUID();
        UUID empresaDoCorpo = UUID.randomUUID();

        mvc.perform(post("/api/contratos/itens")
                        .with(usuario(empresaDoToken, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Tentativa\",\"tenantId\":\"" + empresaDoCorpo + "\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/contratos/itens").with(usuario(empresaDoCorpo, VER)))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/contratos/itens").with(usuario(empresaDoToken, VER)))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void tokenDeServicoSemCabecalhoDeTenantResponde400() throws Exception {
        mvc.perform(get("/api/contratos/itens").with(servico("financeiro", VER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void tokenDeServicoUsaOTenantDoCabecalho() throws Exception {
        UUID empresa = UUID.randomUUID();
        mvc.perform(post("/api/contratos/itens")
                        .with(servico("landing", CRIAR))
                        .header("X-Tenant-Id", empresa.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Criado por rotina\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/contratos/itens").with(usuario(empresa, VER)))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void dadoInvalidoResponde400ComOCampo() throws Exception {
        mvc.perform(post("/api/contratos/itens")
                        .with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("nome"));
    }

    @Test
    void criarPublicaEventoNaExchangeDoModulo() throws Exception {
        criarItem(UUID.randomUUID(), "Item que gera evento");
        verify(rabbit).convertAndSend(eq("contratos.eventos"), eq("contratos.item.criado"), any(Object.class));
    }

    @Test
    void excluirEscondeORegistro() throws Exception {
        UUID empresa = UUID.randomUUID();
        String id = criarItem(empresa, "Item a excluir");

        mvc.perform(delete("/api/contratos/itens/" + id).with(usuario(empresa, EXCLUIR)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/contratos/itens/" + id).with(usuario(empresa, VER)))
                .andExpect(status().isNotFound());
    }

    private String criarItem(UUID tenant, String nome) throws Exception {
        String corpo = mvc.perform(post("/api/contratos/itens")
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/contratos/itens/")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.data.id");
    }
}
