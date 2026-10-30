package br.com.plataforma.contratos;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
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

/** Tarefa 2: segurança, validação, isolamento por empresa e regras dos endpoints de contratos. */
class ContratosTest extends BaseIntegracao {

    private static final String BASE = "/api/contratos/contratos";
    private static final String VER = "contratos.contrato.ver";
    private static final String CRIAR = "contratos.contrato.criar";

    private static final String SERVICO = servico("Hospedagem", "2", "150.00", "300.00");

    // ---------------------------------------------------------------- segurança

    @Test
    void semTokenResponde401NoEnvelope() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(corpo(UUID.randomUUID(), "1500.00", SERVICO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(get(BASE + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void criarSemPermissaoResponde403() throws Exception {
        // corpo válido: a validação do corpo roda antes da checagem de permissão do método
        mvc.perform(post(BASE)
                        .with(usuario(UUID.randomUUID(), VER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(UUID.randomUUID(), "1500.00", SERVICO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void verSemPermissaoResponde403() throws Exception {
        mvc.perform(get(BASE + "/" + UUID.randomUUID()).with(usuario(UUID.randomUUID(), CRIAR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------------------------------------------------------------- criar e ver

    @Test
    void criaOContratoComoRascunhoNoFormatoDoContrato() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();

        String resposta = mvc.perform(post(BASE)
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(empresa, "1500.00", SERVICO)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith(BASE + "/")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.data.numero").value(matchesPattern("CT-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.data.empresaId").value(empresa.toString()))
                .andExpect(jsonPath("$.data.empresaNome").value(nullValue()))
                .andExpect(jsonPath("$.data.assinatura").value(nullValue()))
                .andExpect(jsonPath("$.data.valorMensal").value(1500.00))
                .andExpect(jsonPath("$.data.indiceReajuste").value("IPCA"))
                .andExpect(jsonPath("$.data.servicosContratados.length()").value(1))
                .andExpect(jsonPath("$.data.servicosContratados[0].descricao").value("Hospedagem"))
                // vigenciaFim e dados de auditoria não fazem parte do schema Contrato
                .andExpect(jsonPath("$.data.vigenciaFim").doesNotExist())
                .andExpect(jsonPath("$.data.tenantId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(resposta, "$.data.id");
        mvc.perform(get(BASE + "/" + id).with(usuario(tenant, VER))).andExpect(status().isOk());
    }

    @Test
    void doisContratosSeguidosTemNumerosDiferentes() throws Exception {
        UUID tenant = UUID.randomUUID();
        String primeiro = JsonPath.read(criar(tenant, "1500.00", SERVICO), "$.data.numero");
        String segundo = JsonPath.read(criar(tenant, "1500.00", SERVICO), "$.data.numero");

        org.junit.jupiter.api.Assertions.assertNotEquals(primeiro, segundo);
    }

    @Test
    void indiceDeReajusteAusenteViraNenhum() throws Exception {
        String corpo = corpo(UUID.randomUUID(), "1500.00", SERVICO).replace(",\"indiceReajuste\":\"IPCA\"", "");

        mvc.perform(post(BASE)
                        .with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.indiceReajuste").value("nenhum"));
    }

    @Test
    void verDevolveOsDadosGravados() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();
        String id = JsonPath.read(criar(tenant, empresa, "1500.00", SERVICO,
                servico("Armazenamento", "2.5000", "10.10", "25.25")), "$.data.id");

        mvc.perform(get(BASE + "/" + id).with(usuario(tenant, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.empresaId").value(empresa.toString()))
                .andExpect(jsonPath("$.data.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.data.valorMensal").value(1500.00))
                .andExpect(jsonPath("$.data.mrr").value(1500.00))
                .andExpect(jsonPath("$.data.dataInicio").value("2026-01-15"))
                .andExpect(jsonPath("$.data.vigenciaMeses").value(12))
                .andExpect(jsonPath("$.data.renovacao").value("automatica"))
                .andExpect(jsonPath("$.data.reajusteAnualPercentual").value(5.5))
                .andExpect(jsonPath("$.data.avisoPrevioDias").value(30))
                .andExpect(jsonPath("$.data.origem").value("crm"))
                .andExpect(jsonPath("$.data.origemModuloId").value("OPP-123"))
                .andExpect(jsonPath("$.data.servicosContratados.length()").value(2))
                .andExpect(jsonPath("$.data.servicosContratados[?(@.descricao == 'Armazenamento')].quantidade")
                        .value(2.5))
                .andExpect(jsonPath("$.data.criadoEm").exists())
                .andExpect(jsonPath("$.data.atualizadoEm").exists());
    }

    @Test
    void contratoDeOutraEmpresaResponde404() throws Exception {
        UUID empresaA = UUID.randomUUID();
        UUID empresaB = UUID.randomUUID();
        String id = JsonPath.read(criar(empresaA, "1500.00", SERVICO), "$.data.id");

        mvc.perform(get(BASE + "/" + id).with(usuario(empresaB, VER)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void contratoInexistenteResponde404() throws Exception {
        mvc.perform(get(BASE + "/" + UUID.randomUUID()).with(usuario(UUID.randomUUID(), VER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void tenantEnviadoNoCorpoEIgnorado() throws Exception {
        UUID tenantDoToken = UUID.randomUUID();
        UUID tenantDoCorpo = UUID.randomUUID();
        String corpo = corpo(UUID.randomUUID(), "1500.00", SERVICO)
                .replaceFirst("^\\{", "{\"tenantId\":\"" + tenantDoCorpo + "\",");

        String resposta = mvc.perform(post(BASE)
                        .with(usuario(tenantDoToken, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(resposta, "$.data.id");

        mvc.perform(get(BASE + "/" + id).with(usuario(tenantDoToken, VER))).andExpect(status().isOk());
        mvc.perform(get(BASE + "/" + id).with(usuario(tenantDoCorpo, VER))).andExpect(status().isNotFound());
    }

    @Test
    void modeloInexistenteResponde400ComOCampo() throws Exception {
        String corpo = corpo(UUID.randomUUID(), "1500.00", SERVICO)
                .replaceFirst("^\\{", "{\"modeloId\":\"" + UUID.randomUUID() + "\",");

        mvc.perform(post(BASE)
                        .with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("modeloId"));
    }

    @Test
    void modeloDaMesmaEmpresaEAceito() throws Exception {
        UUID tenant = UUID.randomUUID();
        String resposta = mvc.perform(post("/api/contratos/modelos")
                        .with(usuario(tenant, "contratos.modelo.administrar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Modelo\",\"conteudo\":\"Texto\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String modeloId = JsonPath.read(resposta, "$.data.id");

        mvc.perform(post(BASE)
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(UUID.randomUUID(), "1500.00", SERVICO)
                                .replaceFirst("^\\{", "{\"modeloId\":\"" + modeloId + "\",")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.modeloId").value(modeloId));
    }

    // ---------------------------------------------------------------- validação

    @Test
    void listaDeServicosVaziaResponde400() throws Exception {
        mvc.perform(post(BASE)
                        .with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(UUID.randomUUID(), "1500.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("servicosContratados"));
    }

    @Test
    void dinheiroComTresCasasResponde400ComPrecisaoExcedida() throws Exception {
        mvc.perform(post(BASE)
                        .with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(UUID.randomUUID(), "1500.005", SERVICO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("valorMensal"))
                .andExpect(jsonPath("$.errors[0].codigo").value("PRECISAO_EXCEDIDA"));
    }

    @Test
    void dinheiroComTresCasasDentroDoServicoTambemResponde400ComPrecisaoExcedida() throws Exception {
        mvc.perform(post(BASE)
                        .with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(UUID.randomUUID(), "1500.00", servico("Hospedagem", "2", "150.005", "300.00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("servicosContratados[0].valorUnitario"))
                .andExpect(jsonPath("$.errors[0].codigo").value("PRECISAO_EXCEDIDA"));
    }

    @Test
    void quantidadeComQuatroCasasEAceitaEComCincoERejeitada() throws Exception {
        UUID tenant = UUID.randomUUID();

        String resposta = mvc.perform(post(BASE)
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(UUID.randomUUID(), "1500.00", servico("Banda", "0.1234", "100.00", "12.34"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(0.1234,
                ((Number) JsonPath.read(resposta, "$.data.servicosContratados[0].quantidade")).doubleValue());

        mvc.perform(post(BASE)
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(UUID.randomUUID(), "1500.00", servico("Banda", "0.12345", "100.00", "12.34"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("servicosContratados[0].quantidade"));
    }

    @Test
    void renovacaoForaDaListaResponde400ComOCampo() throws Exception {
        String corpo = corpo(UUID.randomUUID(), "1500.00", SERVICO).replace("\"automatica\"", "\"talvez\"");

        mvc.perform(post(BASE)
                        .with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("renovacao"));
    }

    @Test
    void origemEmBrancoResponde400ComOCampo() throws Exception {
        String corpo = corpo(UUID.randomUUID(), "1500.00", SERVICO).replace("\"origem\":\"crm\"", "\"origem\":\" \"");

        mvc.perform(post(BASE)
                        .with(usuario(UUID.randomUUID(), CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("origem"))
                .andExpect(jsonPath("$.errors[0].codigo").value(not("PRECISAO_EXCEDIDA")));
    }

    // ---------------------------------------------------------------- apoio

    private String criar(UUID tenant, String valorMensal, String... servicos) throws Exception {
        return criar(tenant, UUID.randomUUID(), valorMensal, servicos);
    }

    private String criar(UUID tenant, UUID empresa, String valorMensal, String... servicos) throws Exception {
        return mvc.perform(post(BASE)
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(empresa, valorMensal, servicos)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static String servico(String descricao, String quantidade, String valorUnitario, String valorTotal) {
        return """
                {"descricao":"%s","quantidade":%s,"valorUnitario":%s,"valorTotal":%s}"""
                .formatted(descricao, quantidade, valorUnitario, valorTotal);
    }

    private static String corpo(UUID empresa, String valorMensal, String... servicos) {
        return """
                {"empresaId":"%s","servicosContratados":[%s],"valorMensal":%s,"mrr":1500.00,
                 "dataInicio":"2026-01-15","vigenciaMeses":12,"renovacao":"automatica",
                 "reajusteAnualPercentual":5.5,"indiceReajuste":"IPCA","avisoPrevioDias":30,
                 "responsavelId":"%s","origem":"crm","origemModuloId":"OPP-123"}"""
                .formatted(empresa, String.join(",", servicos), valorMensal, UUID.randomUUID());
    }
}
