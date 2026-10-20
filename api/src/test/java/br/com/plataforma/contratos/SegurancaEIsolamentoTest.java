package br.com.plataforma.contratos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Itens do checklist de conformidade que dependem só deste módulo (Contrato §15): saúde sem token,
 * 401 no envelope e token de serviço. Os testes de 403, isolamento por empresa, tenant no corpo e
 * validação de cada endpoint ficam junto do próprio endpoint (por exemplo, ModelosTest).
 * Usa os endpoints de modelos só como alvo; quando houver outros, ajuste as rotas aqui.
 */
class SegurancaEIsolamentoTest extends BaseIntegracao {

    private static final String VER = "contratos.modelo.ver";
    private static final String ADMINISTRAR = "contratos.modelo.administrar";

    @Test
    void saudeRespondeSemToken() throws Exception {
        mvc.perform(get("/api/contratos/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void semTokenResponde401NoEnvelope() throws Exception {
        mvc.perform(get("/api/contratos/modelos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Autenticação necessária."));
    }

    @Test
    void tokenDeServicoSemCabecalhoDeTenantResponde400() throws Exception {
        mvc.perform(get("/api/contratos/modelos").with(servico("financeiro", VER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void tokenDeServicoUsaOTenantDoCabecalho() throws Exception {
        UUID empresa = UUID.randomUUID();
        mvc.perform(post("/api/contratos/modelos")
                        .with(servico("landing", ADMINISTRAR))
                        .header("X-Tenant-Id", empresa.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Criado por rotina\",\"conteudo\":\"Texto\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/contratos/modelos").with(usuario(empresa, VER)))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
