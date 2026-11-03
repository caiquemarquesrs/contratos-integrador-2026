package br.com.plataforma.contratos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

/** O empresaNome do contrato é a razão social que o CRM devolveu no momento do cadastro (Contrato §9.5). */
class EmpresaNomeTest extends BaseIntegracao {

    private static final String BASE = "/api/contratos/contratos";
    private static final String CRIAR = "contratos.contrato.criar";
    private static final String VER = "contratos.contrato.ver";

    @Test
    void gravaARazaoSocialQueVeioDoCrm() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();
        when(crm.razaoSocial(eq(empresa), anyString())).thenReturn(Optional.of("Centinela Digital Ltda"));

        String resposta = mvc.perform(post(BASE).with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(empresa)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.empresaNome").value("Centinela Digital Ltda"))
                .andReturn().getResponse().getContentAsString();

        // foi gravado, não só devolvido na resposta do POST
        String id = JsonPath.read(resposta, "$.data.id");
        mvc.perform(get(BASE + "/" + id).with(usuario(tenant, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.empresaNome").value("Centinela Digital Ltda"));
    }

    @Test
    void crmForaDoArNaoDerrubaACriacao() throws Exception {
        UUID empresa = UUID.randomUUID();
        when(crm.razaoSocial(eq(empresa), anyString())).thenReturn(Optional.empty());

        mvc.perform(post(BASE).with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(empresa)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.numero").exists())
                .andExpect(jsonPath("$.data.empresaNome").doesNotExist());
    }

    @Test
    void repassaOTokenDoUsuarioAoCrm() throws Exception {
        UUID empresa = UUID.randomUUID();

        mvc.perform(post(BASE).with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo(empresa)))
                .andExpect(status().isCreated());

        verify(crm).razaoSocial(eq(empresa), argThat(token -> token != null && !token.isBlank()));
    }

    @Test
    void corpoInvalidoNaoConsultaOCrm() throws Exception {
        mvc.perform(post(BASE).with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empresaId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());

        verify(crm, never()).razaoSocial(any(), any());
    }

    private static String corpo(UUID empresaId) {
        return """
                {
                  "empresaId": "%s",
                  "servicosContratados": [
                    {"descricao": "Suporte técnico mensal", "quantidade": 1, "valorUnitario": 1890.00, "valorTotal": 1890.00}
                  ],
                  "valorMensal": 1890.00,
                  "mrr": 1890.00,
                  "dataInicio": "2026-11-01",
                  "renovacao": "automatica",
                  "avisoPrevioDias": 30,
                  "responsavelId": "%s",
                  "origem": "crm",
                  "origemModuloId": "oportunidade-abc123"
                }
                """.formatted(empresaId, UUID.randomUUID());
    }
}
