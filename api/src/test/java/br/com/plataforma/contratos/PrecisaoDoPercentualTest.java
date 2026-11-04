package br.com.plataforma.contratos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Contrato §8.7: valor com mais casas do que o tipo permite responde 400 com PRECISAO_EXCEDIDA, sem arredondar. */
class PrecisaoDoPercentualTest extends BaseIntegracao {

    private static final String CONTRATOS = "/api/contratos/contratos";

    @Test
    void percentualComTresCasasResponde400ComPrecisaoExcedida() throws Exception {
        mvc.perform(post(CONTRATOS).with(usuario(UUID.randomUUID(), "contratos.contrato.criar"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("12.345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("reajusteAnualPercentual"))
                .andExpect(jsonPath("$.errors[0].codigo").value("PRECISAO_EXCEDIDA"));
    }

    @Test
    void percentualComDuasCasasEAceito() throws Exception {
        mvc.perform(post(CONTRATOS).with(usuario(UUID.randomUUID(), "contratos.contrato.criar"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("12.50")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reajusteAnualPercentual").value(12.5));
    }

    private static String corpo(String percentual) {
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
                  "reajusteAnualPercentual": %s,
                  "indiceReajuste": "IPCA",
                  "avisoPrevioDias": 30,
                  "responsavelId": "%s",
                  "origem": "crm",
                  "origemModuloId": "oportunidade-abc123"
                }
                """.formatted(UUID.randomUUID(), percentual, UUID.randomUUID());
    }
}
