package br.com.plataforma.contratos;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jayway.jsonpath.JsonPath;

/** Tarefa 2: segurança, validação, isolamento por empresa e regras dos endpoints de contratos. */
class ContratosTest extends BaseIntegracao {

    private static final String BASE = "/api/contratos/contratos";
    private static final String VER = "contratos.contrato.ver";
    private static final String CRIAR = "contratos.contrato.criar";
    private static final String EDITAR = "contratos.contrato.editar";

    @Autowired
    private JdbcTemplate jdbc;

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
        mvc.perform(get(BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(put(BASE + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("1500.00", SERVICO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listarSemPermissaoResponde403() throws Exception {
        mvc.perform(get(BASE).with(usuario(UUID.randomUUID(), CRIAR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void editarSemPermissaoResponde403() throws Exception {
        UUID tenant = UUID.randomUUID();
        String id = JsonPath.read(criar(tenant, "1500.00", SERVICO), "$.data.id");

        mvc.perform(put(BASE + "/" + id)
                        .with(usuario(tenant, VER, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("1600.00", SERVICO)))
                .andExpect(status().isForbidden())
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

    // ---------------------------------------------------------------- editar

    @Test
    void editaRascunhoTrocaOsServicosEMantemEmpresaEOrigem() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();
        String id = JsonPath.read(criar(tenant, empresa, "1500.00", SERVICO,
                servico("Suporte", "1", "10.00", "10.00")), "$.data.id");

        // empresaId e origem no corpo do PUT são ignorados: o EdicaoContrato não os tem
        String corpo = edicao("2000.00", servico("Backup", "3", "100.00", "300.00"))
                .replaceFirst("^\\{", "{\"empresaId\":\"" + UUID.randomUUID() + "\",\"origem\":\"outra\",");

        mvc.perform(put(BASE + "/" + id)
                        .with(usuario(tenant, EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.valorMensal").value(2000.00))
                .andExpect(jsonPath("$.data.empresaId").value(empresa.toString()))
                .andExpect(jsonPath("$.data.origem").value("crm"))
                .andExpect(jsonPath("$.data.origemModuloId").value("OPP-123"))
                .andExpect(jsonPath("$.data.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.data.servicosContratados.length()").value(1))
                .andExpect(jsonPath("$.data.servicosContratados[0].descricao").value("Backup"));

        // o que foi gravado é igual ao que a resposta mostrou, e os serviços antigos saíram (exclusão lógica)
        mvc.perform(get(BASE + "/" + id).with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.valorMensal").value(2000.00))
                .andExpect(jsonPath("$.data.servicosContratados.length()").value(1))
                .andExpect(jsonPath("$.data.servicosContratados[0].descricao").value("Backup"));
        Integer apagados = jdbc.queryForObject(
                "select count(*) from contratos.servicos_contratados where contrato_id = ? and deleted_at is not null",
                Integer.class, UUID.fromString(id));
        org.junit.jupiter.api.Assertions.assertEquals(2, apagados);
    }

    @Test
    void editaContratoEmAprovacao() throws Exception {
        UUID tenant = UUID.randomUUID();
        String id = JsonPath.read(criar(tenant, "1500.00", SERVICO), "$.data.id");
        mudarStatus(id, "EM_APROVACAO");

        mvc.perform(put(BASE + "/" + id)
                        .with(usuario(tenant, EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("1800.00", SERVICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EM_APROVACAO"))
                .andExpect(jsonPath("$.data.valorMensal").value(1800.00));
    }

    @Test
    void editarEmOutroEstadoResponde409() throws Exception {
        UUID tenant = UUID.randomUUID();
        String id = JsonPath.read(criar(tenant, "1500.00", SERVICO), "$.data.id");
        mudarStatus(id, "ATIVO");

        mvc.perform(put(BASE + "/" + id)
                        .with(usuario(tenant, EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("1800.00", SERVICO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].campo").value("status"))
                .andExpect(jsonPath("$.errors[0].codigo").value("STATUS_NAO_PERMITE_EDICAO"))
                .andExpect(jsonPath("$.errors[0].detalhe").value("Estado atual: ATIVO."));

        mvc.perform(get(BASE + "/" + id).with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.valorMensal").value(1500.00));
    }

    @Test
    void editarContratoDeOutraEmpresaResponde404() throws Exception {
        UUID empresaA = UUID.randomUUID();
        String id = JsonPath.read(criar(empresaA, "1500.00", SERVICO), "$.data.id");

        mvc.perform(put(BASE + "/" + id)
                        .with(usuario(UUID.randomUUID(), EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("1800.00", SERVICO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void editarComDinheiroDeTresCasasResponde400ComPrecisaoExcedida() throws Exception {
        UUID tenant = UUID.randomUUID();
        String id = JsonPath.read(criar(tenant, "1500.00", SERVICO), "$.data.id");

        mvc.perform(put(BASE + "/" + id)
                        .with(usuario(tenant, EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("1800.001", SERVICO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].codigo").value("PRECISAO_EXCEDIDA"));
    }

    // ---------------------------------------------------------------- listar

    @Test
    void listaComPaginacaoETamanhoMaximoDe100() throws Exception {
        UUID tenant = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            criar(tenant, "1500.00", SERVICO);
        }

        mvc.perform(get(BASE).param("pagina", "0").param("tamanho", "2").with(usuario(tenant, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.pagina").value(0))
                .andExpect(jsonPath("$.data.tamanho").value(2))
                .andExpect(jsonPath("$.data.itens.length()").value(2))
                .andExpect(jsonPath("$.data.itens[0].servicosContratados.length()").value(1));
        mvc.perform(get(BASE).param("pagina", "1").param("tamanho", "2").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.itens.length()").value(1));
        mvc.perform(get(BASE).param("tamanho", "500").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.tamanho").value(100));
        mvc.perform(get(BASE).with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.tamanho").value(20));
    }

    @Test
    void listaSoOsContratosDaPropriaEmpresa() throws Exception {
        UUID empresaA = UUID.randomUUID();
        criar(empresaA, "1500.00", SERVICO);

        mvc.perform(get(BASE).with(usuario(UUID.randomUUID(), VER)))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void filtraPorStatusEEmpresaId() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID cliente = UUID.randomUUID();
        String ativo = JsonPath.read(criar(tenant, cliente, "1500.00", SERVICO), "$.data.id");
        criar(tenant, "1500.00", SERVICO);
        mudarStatus(ativo, "ATIVO");

        mvc.perform(get(BASE).param("status", "ATIVO").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.itens[0].id").value(ativo));
        mvc.perform(get(BASE).param("status", "RASCUNHO").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.itens[0].id").value(not(ativo)));
        mvc.perform(get(BASE).param("empresaId", cliente.toString()).with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.itens[0].id").value(ativo));
    }

    @Test
    void buscaPorNumeroOuNomeDaEmpresaSemDiferenciarMaiusculas() throws Exception {
        UUID tenant = UUID.randomUUID();
        String resposta = criar(tenant, "1500.00", SERVICO);
        String id = JsonPath.read(resposta, "$.data.id");
        String numero = JsonPath.read(resposta, "$.data.numero");
        criar(tenant, "1500.00", SERVICO);
        jdbc.update("update contratos.contratos set empresa_nome = ? where id = ?", "Acme Ltda", UUID.fromString(id));

        mvc.perform(get(BASE).param("q", numero.toLowerCase()).with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.itens[0].id").value(id));
        mvc.perform(get(BASE).param("q", "ACME").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.itens[0].id").value(id));
        mvc.perform(get(BASE).param("q", "acme").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get(BASE).param("q", "nao-existe").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(0));
        // % e _ são texto comum na busca, não curinga
        mvc.perform(get(BASE).param("q", "%%").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void buscaComUmCaractereResponde400() throws Exception {
        mvc.perform(get(BASE).param("q", "a").with(usuario(UUID.randomUUID(), VER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("q"));
    }

    @Test
    void statusInvalidoResponde400() throws Exception {
        mvc.perform(get(BASE).param("status", "QUALQUER").with(usuario(UUID.randomUUID(), VER)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ordenaPelasQuatroFormasPermitidas() throws Exception {
        UUID tenant = UUID.randomUUID();
        String a = JsonPath.read(criarCom(tenant, "100.00", "24"), "$.data.id");
        String b = JsonPath.read(criarCom(tenant, "300.00", "null"), "$.data.id");
        String c = JsonPath.read(criarCom(tenant, "200.00", "6"), "$.data.id");

        // padrão e criadoEm,desc: o mais novo primeiro
        mvc.perform(get(BASE).with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.itens[0].id").value(c))
                .andExpect(jsonPath("$.data.itens[2].id").value(a));
        mvc.perform(get(BASE).param("ordenar", "criadoEm,desc").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.itens[0].id").value(c));
        mvc.perform(get(BASE).param("ordenar", "criadoEm,asc").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.itens[0].id").value(a))
                .andExpect(jsonPath("$.data.itens[2].id").value(c));
        // vigência que termina antes primeiro, sem vigência por último
        mvc.perform(get(BASE).param("ordenar", "vigenciaFim,asc").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.itens[0].id").value(c))
                .andExpect(jsonPath("$.data.itens[1].id").value(a))
                .andExpect(jsonPath("$.data.itens[2].id").value(b));
        mvc.perform(get(BASE).param("ordenar", "valorMensal,desc").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.itens[0].id").value(b))
                .andExpect(jsonPath("$.data.itens[1].id").value(c))
                .andExpect(jsonPath("$.data.itens[2].id").value(a));
    }

    @Test
    void ordenarInvalidoResponde400() throws Exception {
        mvc.perform(get(BASE).param("ordenar", "numero,asc").with(usuario(UUID.randomUUID(), VER)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("ordenar"));
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

    /** Cria um contrato com valorMensal e vigenciaMeses (número ou "null") escolhidos. */
    private String criarCom(UUID tenant, String valorMensal, String vigenciaMeses) throws Exception {
        return mvc.perform(post(BASE)
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(UUID.randomUUID(), valorMensal, SERVICO)
                                .replace("\"vigenciaMeses\":12", "\"vigenciaMeses\":" + vigenciaMeses)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    /** Coloca o contrato em outro estado, já que ainda não há endpoint para isso (Tarefa 3). */
    private void mudarStatus(String id, String status) {
        jdbc.update("update contratos.contratos set status = ? where id = ?", status, UUID.fromString(id));
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

    private static String edicao(String valorMensal, String... servicos) {
        return """
                {"servicosContratados":[%s],"valorMensal":%s,"mrr":1500.00,
                 "dataInicio":"2026-02-01","vigenciaMeses":24,"renovacao":"manual",
                 "reajusteAnualPercentual":4.25,"indiceReajuste":"IGPM","avisoPrevioDias":60,
                 "responsavelId":"%s"}"""
                .formatted(String.join(",", servicos), valorMensal, UUID.randomUUID());
    }
}
