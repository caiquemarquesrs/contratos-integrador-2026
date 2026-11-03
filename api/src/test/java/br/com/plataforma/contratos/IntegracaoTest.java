package br.com.plataforma.contratos;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * Tarefa 5: busca, resumo e status contratual (todos GET). Cobre segurança (401, 403, token de
 * serviço), isolamento por empresa e as regras de cada um dos cinco endpoints. O contrato nasce
 * como RASCUNHO pelo POST; quando o teste precisa de outro estado, o JdbcTemplate ajusta a linha.
 */
class IntegracaoTest extends BaseIntegracao {

    private static final String BUSCA = "/api/contratos/busca";
    private static final String CONTRATOS = "/api/contratos/contratos";
    private static final String EMPRESAS = "/api/contratos/empresas";

    private static final String RESUMO = "contratos.contrato.ver_resumo";
    private static final String CRIAR = "contratos.contrato.criar";
    private static final String VER = "contratos.contrato.ver";

    @Autowired
    private JdbcTemplate jdbc;

    // ---------------------------------------------------------------- segurança (os cinco endpoints)

    @Test
    void semTokenResponde401NosCincoEndpoints() throws Exception {
        for (String rota : cincoRotas(UUID.randomUUID().toString(), UUID.randomUUID())) {
            mvc.perform(get(rota))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Test
    void semAPermissaoDeResumoResponde403NosCincoEndpoints() throws Exception {
        // tem ver e criar, mas não ver_resumo: a permissão de leitura de contrato não vale aqui
        for (String rota : cincoRotas(UUID.randomUUID().toString(), UUID.randomUUID())) {
            mvc.perform(get(rota).with(usuario(UUID.randomUUID(), VER, CRIAR)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Test
    void tokenDeServicoDoFinanceiroLeOsCincoEndpointsComOTenantDoCabecalho() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, "Centinela Digital Ltda");

        for (String rota : cincoRotas(criado.id(), criado.empresa())) {
            mvc.perform(get(rota)
                            .with(servico("financeiro", RESUMO))
                            .header("X-Tenant-Id", tenant.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
        // e enxerga o contrato da empresa do cabeçalho, não de outra
        mvc.perform(get(CONTRATOS + "/" + criado.id() + "/resumo")
                        .with(servico("financeiro", RESUMO))
                        .header("X-Tenant-Id", tenant.toString()))
                .andExpect(jsonPath("$.data.numero").value(criado.numero()));
        mvc.perform(get(CONTRATOS + "/" + criado.id() + "/resumo")
                        .with(servico("financeiro", RESUMO))
                        .header("X-Tenant-Id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void tokenDeServicoSemOCabecalhoDeTenantResponde400NosCincoEndpoints() throws Exception {
        for (String rota : cincoRotas(UUID.randomUUID().toString(), UUID.randomUUID())) {
            mvc.perform(get(rota).with(servico("financeiro", RESUMO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // ---------------------------------------------------------------- isolamento por empresa

    @Test
    void resumoDeContratoDeOutraEmpresaResponde404() throws Exception {
        Criado deA = criar(UUID.randomUUID(), "Empresa A");

        mvc.perform(get(CONTRATOS + "/" + deA.id() + "/resumo").with(usuario(UUID.randomUUID(), RESUMO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loteNaoDevolveContratoDeOutraEmpresa() throws Exception {
        Criado deA = criar(UUID.randomUUID(), "Empresa A");

        mvc.perform(get(CONTRATOS + "/resumo?ids=" + deA.id()).with(usuario(UUID.randomUUID(), RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
    }

    @Test
    void buscaNaoDevolveContratoDeOutraEmpresaNemComTenantNaQueryString() throws Exception {
        UUID tenantA = UUID.randomUUID();
        Criado deA = criar(tenantA, "Empresa Exclusiva da A");

        mvc.perform(get(BUSCA + "?q=" + deA.numero()).with(usuario(UUID.randomUUID(), RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
        mvc.perform(get(BUSCA + "?q=Exclusiva&tenantId=" + tenantA).with(usuario(UUID.randomUUID(), RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
    }

    @Test
    void statusContratualNaoEnxergaContratoDeOutraEmpresa() throws Exception {
        Criado deA = criar(UUID.randomUUID(), "Empresa A");
        definir(deA, "ATIVO", LocalDate.now().plusDays(365));
        UUID tenantB = UUID.randomUUID();

        // mesma empresaId, outro tenant: para B ela não tem contrato nenhum
        mvc.perform(get(EMPRESAS + "/" + deA.empresa() + "/status-contratual").with(usuario(tenantB, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.situacao").value("sem_contrato"));
        mvc.perform(get(EMPRESAS + "/status-contratual?ids=" + deA.empresa()).with(usuario(tenantB, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].situacao").value("sem_contrato"));
    }

    // ---------------------------------------------------------------- resumo (endpoint 2)

    @Test
    void resumoDevolveOFormatoDoContratoSemCamposDeMais() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, "Centinela Digital Ltda");

        mvc.perform(get(CONTRATOS + "/" + criado.id() + "/resumo").with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(criado.id()))
                .andExpect(jsonPath("$.data.numero").value(criado.numero()))
                .andExpect(jsonPath("$.data.empresaNome").value("Centinela Digital Ltda"))
                .andExpect(jsonPath("$.data.status").value("RASCUNHO"))
                .andExpect(jsonPath("$.data.valorMensal").value(1500.00))
                .andExpect(jsonPath("$.data.vigenciaFimPrevista").value("2027-01-15"))
                .andExpect(jsonPath("$.data.tenantId").doesNotExist())
                .andExpect(jsonPath("$.data.servicosContratados").doesNotExist())
                .andExpect(jsonPath("$.data.mrr").doesNotExist());
    }

    @Test
    void resumoSemNomeGravadoUsaOTextoDeReservaEnuncaDevolveNulo() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, null);   // o CRM não respondeu na criação

        mvc.perform(get(CONTRATOS + "/" + criado.id() + "/resumo").with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.empresaNome").value("Empresa indisponível"));
    }

    @Test
    void resumoInexistenteResponde404() throws Exception {
        mvc.perform(get(CONTRATOS + "/" + UUID.randomUUID() + "/resumo").with(usuario(UUID.randomUUID(), RESUMO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------------------------------------------------------------- resumo em lote (endpoint 3)

    @Test
    void loteDevolveSoOsQueExistemNaOrdemPedidaSemRepetir() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado primeiro = criar(tenant, "Primeira Ltda");
        Criado segundo = criar(tenant, "Segunda Ltda");
        UUID inexistente = UUID.randomUUID();

        mvc.perform(get(CONTRATOS + "/resumo?ids=" + segundo.id() + "," + inexistente + "," + primeiro.id()
                                + "," + segundo.id())
                        .with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].id").value(segundo.id()))
                .andExpect(jsonPath("$.data[0].empresaNome").value("Segunda Ltda"))
                .andExpect(jsonPath("$.data[1].id").value(primeiro.id()));
    }

    @Test
    void loteAceitaOParametroRepetidoAlemDaListaSeparadaPorVirgula() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado primeiro = criar(tenant, "Primeira Ltda");
        Criado segundo = criar(tenant, "Segunda Ltda");

        mvc.perform(get(CONTRATOS + "/resumo?ids=" + primeiro.id() + "&ids=" + segundo.id())
                        .with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void loteAceitaCemIdsERejeitaCentoEUm() throws Exception {
        UUID tenant = UUID.randomUUID();

        mvc.perform(get(CONTRATOS + "/resumo?ids=" + idsAleatorios(100)).with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk());

        mvc.perform(get(CONTRATOS + "/resumo?ids=" + idsAleatorios(101)).with(usuario(tenant, RESUMO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].campo").value("ids"));
    }

    @Test
    void loteSemOParametroIdsResponde400NoEnvelope() throws Exception {
        mvc.perform(get(CONTRATOS + "/resumo").with(usuario(UUID.randomUUID(), RESUMO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loteComIdQueNaoEUuidResponde400() throws Exception {
        mvc.perform(get(CONTRATOS + "/resumo?ids=abc").with(usuario(UUID.randomUUID(), RESUMO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void aRotaFixaDoLoteNaoCaiNoVerPorId() throws Exception {
        UUID tenant = UUID.randomUUID();

        // se /contratos/resumo caísse no GET /contratos/{id}, "resumo" não seria um UUID e viria 400,
        // e quem só tem ver_resumo levaria 403: aqui responde a lista (vazia), com a permissão fraca
        mvc.perform(get(CONTRATOS + "/resumo?ids=").with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", empty()));

        // e o GET /contratos/{id} de sempre continua no ar, com a permissão dele
        Criado criado = criar(tenant, "Centinela Digital Ltda");
        mvc.perform(get(CONTRATOS + "/" + criado.id()).with(usuario(tenant, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(criado.id()));
    }

    // ---------------------------------------------------------------- busca (endpoint 1)

    @Test
    void buscaAchaPeloNumeroSemDiferenciarMaiuscula() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, "Centinela Digital Ltda");

        for (String q : List.of(criado.numero(), criado.numero().toLowerCase())) {
            mvc.perform(get(BUSCA + "?q=" + q).with(usuario(tenant, RESUMO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id").value(criado.id()));
        }
    }

    @Test
    void buscaAchaPeloNomeDaEmpresaSemDiferenciarMaiuscula() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, "Centinela Digital Ltda");
        criar(tenant, "Outra Empresa SA");

        mvc.perform(get(BUSCA + "?q=cENTINELA").with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(criado.id()));
    }

    @Test
    void buscaComMenosDeDoisCaracteresResponde400ComOCampoQ() throws Exception {
        UUID tenant = UUID.randomUUID();

        for (String q : List.of("a", "", " a ")) {   // o espaço das pontas não conta
            buscar(tenant, q)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errors[0].campo").value("q"));
        }
        // sem o parâmetro q também é erro do campo q, não o erro genérico de parâmetro faltando
        mvc.perform(get(BUSCA).with(usuario(tenant, RESUMO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("q"));
        buscar(tenant, "ab").andExpect(status().isOk());
    }

    @Test
    void buscaDevolveNoMaximoCincoDoMaisRecenteParaOMaisAntigo() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado ultimo = null;
        for (int i = 0; i < 7; i++) {
            ultimo = criar(tenant, "Limite Comercial Ltda");
        }

        mvc.perform(get(BUSCA + "?q=Limite").with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].id").value(ultimo.id()));
    }

    @Test
    void buscaMontaTituloSubtituloERotaComEspacoComum() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, "Centinela Digital Ltda");
        definir(criado, "ATIVO", LocalDate.now().plusDays(365));
        jdbc.update("update contratos.contratos set valor_mensal = ? where id = ?",
                new BigDecimal("1890.00"), UUID.fromString(criado.id()));

        String resposta = mvc.perform(get(BUSCA + "?q=" + criado.numero()).with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(criado.id()))
                .andExpect(jsonPath("$.data[0].titulo").value(criado.numero() + " — Centinela Digital Ltda"))
                .andExpect(jsonPath("$.data[0].subtitulo").value("Ativo · R$ 1.890,00/mês"))
                .andExpect(jsonPath("$.data[0].rota", startsWith("/contratos/")))
                .andExpect(jsonPath("$.data[0].rota").value("/contratos/" + criado.id()))
                .andReturn().getResponse().getContentAsString();

        String subtitulo = JsonPath.read(resposta, "$.data[0].subtitulo");
        assertFalse(subtitulo.contains(" "), "o subtítulo não pode ter espaço não separável");
    }

    @Test
    void buscaSemNomeGravadoMostraSoONumeroNoTitulo() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, null);

        mvc.perform(get(BUSCA + "?q=" + criado.numero()).with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].titulo").value(criado.numero()));
    }

    @Test
    void buscaTrataPercentualEUnderlineComoTextoELiteral() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado promocao = criar(tenant, "Promo 50% Ltda");
        criar(tenant, "Promo Normal Ltda");

        buscar(tenant, "50%")
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(promocao.id()));
        // "%%" e "__" casariam com tudo se fossem curingas
        buscar(tenant, "%%").andExpect(jsonPath("$.data", empty()));
        buscar(tenant, "__").andExpect(jsonPath("$.data", empty()));
    }

    // ---------------------------------------------------------------- status contratual (endpoints 4 e 5)

    @Test
    void empresaSemContratoRespondeSempreComSemContrato() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();

        mvc.perform(get(EMPRESAS + "/" + empresa + "/status-contratual").with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.empresaId").value(empresa.toString()))
                .andExpect(jsonPath("$.data.situacao").value("sem_contrato"))
                .andExpect(jsonPath("$.data.contratoId").value(nullValue()))
                .andExpect(jsonPath("$.data.numero").value(nullValue()))
                .andExpect(jsonPath("$.data.diasParaVencer").value(nullValue()));
    }

    @Test
    void contratoAtivoLongeDoFimEOk() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, "Centinela Digital Ltda");
        definir(criado, "ATIVO", LocalDate.now().plusDays(365));

        mvc.perform(get(EMPRESAS + "/" + criado.empresa() + "/status-contratual").with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.situacao").value("ok"))
                .andExpect(jsonPath("$.data.contratoId").value(criado.id()))
                .andExpect(jsonPath("$.data.numero").value(criado.numero()))
                .andExpect(jsonPath("$.data.diasParaVencer").value(365));
    }

    @Test
    void contratoSuspensoECritico() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, "Centinela Digital Ltda");
        definir(criado, "SUSPENSO", LocalDate.now().plusDays(365));

        esperarSituacao(tenant, criado.empresa(), "critico");
    }

    @Test
    void contratoAVencerEAtencao() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, "Centinela Digital Ltda");
        definir(criado, "A_VENCER", LocalDate.now().plusDays(365));

        esperarSituacao(tenant, criado.empresa(), "atencao");
    }

    @Test
    void contratoDentroDaJanelaDeAvisoPrevioEAtencao() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado criado = criar(tenant, "Centinela Digital Ltda");
        definir(criado, "ATIVO", LocalDate.now().plusDays(10), "manual", 30);

        mvc.perform(get(EMPRESAS + "/" + criado.empresa() + "/status-contratual").with(usuario(tenant, RESUMO)))
                .andExpect(jsonPath("$.data.situacao").value("atencao"))
                .andExpect(jsonPath("$.data.diasParaVencer").value(10));
    }

    @Test
    void vencidoSemRenovacaoAutomaticaECriticoEComRenovacaoAutomaticaEAtencao() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado manual = criar(tenant, "Manual Ltda");
        Criado automatica = criar(tenant, "Automática Ltda");
        definir(manual, "ATIVO", LocalDate.now().minusDays(5), "manual", 30);
        definir(automatica, "ATIVO", LocalDate.now().minusDays(5), "automatica", 30);

        mvc.perform(get(EMPRESAS + "/" + manual.empresa() + "/status-contratual").with(usuario(tenant, RESUMO)))
                .andExpect(jsonPath("$.data.situacao").value("critico"))
                .andExpect(jsonPath("$.data.diasParaVencer").value(0));
        esperarSituacao(tenant, automatica.empresa(), "atencao");
    }

    @Test
    void contratoQueAindaNaoEVigenteNaoContaParaOStatus() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado rascunho = criar(tenant, "Rascunho Ltda");   // nasce RASCUNHO
        Criado cancelado = criar(tenant, "Cancelado Ltda");
        definir(cancelado, "CANCELADO", LocalDate.now().plusDays(365));

        esperarSituacao(tenant, rascunho.empresa(), "sem_contrato");
        esperarSituacao(tenant, cancelado.empresa(), "sem_contrato");
    }

    @Test
    void empresaComVariosContratosFicaComAPiorSituacao() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();
        Criado emDia = criar(tenant, empresa, "Centinela Digital Ltda");
        Criado suspenso = criar(tenant, empresa, "Centinela Digital Ltda");
        definir(emDia, "ATIVO", LocalDate.now().plusDays(365));
        definir(suspenso, "SUSPENSO", LocalDate.now().plusDays(200));

        mvc.perform(get(EMPRESAS + "/" + empresa + "/status-contratual").with(usuario(tenant, RESUMO)))
                .andExpect(jsonPath("$.data.situacao").value("critico"))
                .andExpect(jsonPath("$.data.contratoId").value(suspenso.id()))
                .andExpect(jsonPath("$.data.numero").value(suspenso.numero()))
                .andExpect(jsonPath("$.data.diasParaVencer").value(200));
    }

    @Test
    void loteDevolveUmItemParaCadaIdPedidoNaMesmaOrdemInclusiveSemContrato() throws Exception {
        UUID tenant = UUID.randomUUID();
        Criado emDia = criar(tenant, "Em Dia Ltda");
        Criado suspensa = criar(tenant, "Suspensa Ltda");
        definir(emDia, "ATIVO", LocalDate.now().plusDays(365));
        definir(suspensa, "SUSPENSO", LocalDate.now().plusDays(365));
        UUID semContrato = UUID.randomUUID();

        mvc.perform(get(EMPRESAS + "/status-contratual?ids=" + suspensa.empresa() + "," + semContrato + ","
                                + emDia.empresa())
                        .with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].empresaId").value(suspensa.empresa().toString()))
                .andExpect(jsonPath("$.data[0].situacao").value("critico"))
                .andExpect(jsonPath("$.data[1].empresaId").value(semContrato.toString()))
                .andExpect(jsonPath("$.data[1].situacao").value("sem_contrato"))
                .andExpect(jsonPath("$.data[2].empresaId").value(emDia.empresa().toString()))
                .andExpect(jsonPath("$.data[2].situacao").value("ok"));
    }

    @Test
    void loteDeStatusAceitaCemIdsERejeitaCentoEUmEIdsAusente() throws Exception {
        UUID tenant = UUID.randomUUID();

        mvc.perform(get(EMPRESAS + "/status-contratual?ids=" + idsAleatorios(100)).with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(100)));

        mvc.perform(get(EMPRESAS + "/status-contratual?ids=" + idsAleatorios(101)).with(usuario(tenant, RESUMO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("ids"));

        mvc.perform(get(EMPRESAS + "/status-contratual").with(usuario(tenant, RESUMO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void statusDeEmpresaComIdInvalidoResponde400() throws Exception {
        mvc.perform(get(EMPRESAS + "/abc/status-contratual").with(usuario(UUID.randomUUID(), RESUMO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ---------------------------------------------------------------- apoio

    /** O texto vai como parâmetro (e não colado na URL) para "%" e espaço chegarem exatamente como digitados. */
    private ResultActions buscar(UUID tenant, String q) throws Exception {
        return mvc.perform(get(BUSCA).param("q", q).with(usuario(tenant, RESUMO)));
    }

    private record Criado(String id, String numero, UUID empresa) {
    }

    private Criado criar(UUID tenant, String nomeDaEmpresa) throws Exception {
        return criar(tenant, UUID.randomUUID(), nomeDaEmpresa);
    }

    /** Cria pelo POST de verdade. O CRM é falso: devolve o nome informado, ou "não respondeu" se for nulo. */
    private Criado criar(UUID tenant, UUID empresa, String nomeDaEmpresa) throws Exception {
        when(crm.razaoSocial(eq(empresa), anyString())).thenReturn(Optional.ofNullable(nomeDaEmpresa));
        String resposta = mvc.perform(post(CONTRATOS)
                        .with(usuario(tenant, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(empresa)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Criado(JsonPath.read(resposta, "$.data.id"), JsonPath.read(resposta, "$.data.numero"), empresa);
    }

    /** Põe o contrato no estado desejado: a API ainda não tem as operações que mudam status e vigência. */
    private void definir(Criado contrato, String status, LocalDate vigenciaFim, String renovacao, int avisoPrevioDias) {
        jdbc.update("update contratos.contratos set status = ?, vigencia_fim = ?, renovacao = ?, "
                        + "aviso_previo_dias = ? where id = ?",
                status, vigenciaFim, renovacao, avisoPrevioDias, UUID.fromString(contrato.id()));
    }

    private void definir(Criado contrato, String status, LocalDate vigenciaFim) {
        definir(contrato, status, vigenciaFim, "automatica", 30);
    }

    private void esperarSituacao(UUID tenant, UUID empresa, String situacao) throws Exception {
        mvc.perform(get(EMPRESAS + "/" + empresa + "/status-contratual").with(usuario(tenant, RESUMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.situacao").value(situacao));
    }

    /** As cinco rotas do módulo de integração, todas com parâmetros válidos. */
    private static List<String> cincoRotas(String contratoId, UUID empresa) {
        return List.of(
                BUSCA + "?q=CT-",
                CONTRATOS + "/" + contratoId + "/resumo",
                CONTRATOS + "/resumo?ids=" + contratoId,
                EMPRESAS + "/" + empresa + "/status-contratual",
                EMPRESAS + "/status-contratual?ids=" + empresa);
    }

    private static String idsAleatorios(int quantidade) {
        return IntStream.range(0, quantidade).mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.joining(","));
    }

    private static String corpo(UUID empresa) {
        return """
                {"empresaId":"%s",
                 "servicosContratados":[{"descricao":"Suporte técnico mensal","quantidade":1,
                                         "valorUnitario":1500.00,"valorTotal":1500.00}],
                 "valorMensal":1500.00,"mrr":1500.00,
                 "dataInicio":"2026-01-15","vigenciaMeses":12,"renovacao":"automatica",
                 "reajusteAnualPercentual":5.5,"indiceReajuste":"IPCA","avisoPrevioDias":30,
                 "responsavelId":"%s","origem":"crm","origemModuloId":"OPP-123"}"""
                .formatted(empresa, UUID.randomUUID());
    }
}
