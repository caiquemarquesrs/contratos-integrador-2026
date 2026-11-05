package br.com.plataforma.contratos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** PR 3c: GET /api/contratos/contratos/{id}/historico e o registro de mudanças. */
class HistoricoContratoTest extends BaseContratos {

    private static final UUID EMPRESA = UUID.randomUUID();
    private static final UUID RESPONSAVEL = UUID.randomUUID();

    /** PUT com os mesmos valores do contrato criado por {@code corpoNovo}, mudando só o valor mensal. */
    private static String edicao(String valorMensal, String servico) {
        return """
                {"servicosContratados":[%s],"valorMensal":%s,"mrr":1500.00,
                 "dataInicio":"2026-01-15","vigenciaMeses":12,"renovacao":"automatica",
                 "reajusteAnualPercentual":5.5,"indiceReajuste":"IPCA","avisoPrevioDias":30,
                 "responsavelId":"%s"}"""
                .formatted(servico, valorMensal, RESPONSAVEL);
    }

    /**
     * Cria o contrato com a mesma empresa e o mesmo responsável que o {@code edicao(...)} usa. Nos testes
     * de edição isso importa: com um responsável diferente, o PUT mudaria o responsável de verdade e o
     * histórico registraria uma linha a mais (corretamente).
     */
    private UUID criarComo(UUID tenant, UUID usuarioId) throws Exception {
        String resposta = mvc.perform(post(BASE)
                        .with(usuarioComId(tenant, usuarioId, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoNovo(EMPRESA, RESPONSAVEL)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(resposta, "$.data.id"));
    }

    // ---------------------------------------------------------------- segurança

    @Test
    void semTokenResponde401() throws Exception {
        mvc.perform(get(BASE + "/" + UUID.randomUUID() + "/historico"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void semPermissaoResponde403() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);

        mvc.perform(get(BASE + "/" + id + "/historico").with(usuario(tenant, CRIAR, EDITAR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void contratoDeOutraEmpresaOuInexistenteResponde404() throws Exception {
        UUID id = criarContrato(UUID.randomUUID());

        mvc.perform(get(BASE + "/" + id + "/historico").with(usuario(UUID.randomUUID(), VER)))
                .andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/" + UUID.randomUUID() + "/historico").with(usuario(UUID.randomUUID(), VER)))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- o que é registrado

    @Test
    void criarRegistraOStatusRascunhoComOUsuarioDoToken() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID criador = UUID.randomUUID();
        UUID id = criarComo(tenant, criador);

        mvc.perform(get(BASE + "/" + id + "/historico").with(usuario(tenant, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.itens.length()").value(1))
                .andExpect(jsonPath("$.data.itens[0].id").exists())
                .andExpect(jsonPath("$.data.itens[0].contratoId").value(id.toString()))
                .andExpect(jsonPath("$.data.itens[0].campo").value("status"))
                .andExpect(jsonPath("$.data.itens[0].valorAnterior").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.itens[0].valorNovo").value("RASCUNHO"))
                .andExpect(jsonPath("$.data.itens[0].usuarioId").value(criador.toString()))
                .andExpect(jsonPath("$.data.itens[0].ocorridoEm").exists());
    }

    @Test
    void aprovarEAssinarRegistramUmaLinhaCadaUmaNaOrdemDoMaisRecente() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID aprovador = UUID.randomUUID();
        UUID usuarioDaAssinatura = UUID.randomUUID();
        UUID id = criarContrato(tenant);
        mudarStatus(id, "EM_APROVACAO");

        mvc.perform(post(BASE + "/" + id + "/aprovar").with(usuarioComId(tenant, aprovador, APROVAR)))
                .andExpect(status().isOk());
        mvc.perform(post(BASE + "/" + id + "/assinar")
                        .with(usuarioComId(tenant, usuarioDaAssinatura, ASSINAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assinadoPor\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(get(BASE + "/" + id + "/historico").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(3))
                // do mais recente para o mais antigo
                .andExpect(jsonPath("$.data.itens[0].campo").value("status"))
                .andExpect(jsonPath("$.data.itens[0].valorAnterior").value("AGUARDANDO_ASSINATURA"))
                .andExpect(jsonPath("$.data.itens[0].valorNovo").value("ATIVO"))
                .andExpect(jsonPath("$.data.itens[0].usuarioId").value(usuarioDaAssinatura.toString()))
                .andExpect(jsonPath("$.data.itens[1].valorAnterior").value("EM_APROVACAO"))
                .andExpect(jsonPath("$.data.itens[1].valorNovo").value("AGUARDANDO_ASSINATURA"))
                .andExpect(jsonPath("$.data.itens[1].usuarioId").value(aprovador.toString()))
                .andExpect(jsonPath("$.data.itens[2].valorNovo").value("RASCUNHO"));
    }

    @Test
    void conflitoNaoRegistraNada() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarContrato(tenant);   // RASCUNHO: aprovar dá 409

        mvc.perform(post(BASE + "/" + id + "/aprovar").with(usuario(tenant, APROVAR)))
                .andExpect(status().isConflict());

        mvc.perform(get(BASE + "/" + id + "/historico").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(1));   // só a criação
    }

    @Test
    void editarMudandoOValorMensalRegistraExatamenteUmaLinhaComAnteriorENovo() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID editor = UUID.randomUUID();
        UUID id = criarComo(tenant, UUID.randomUUID());

        mvc.perform(put(BASE + "/" + id)
                        .with(usuarioComId(tenant, editor, EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("1600.00", SERVICO)))
                .andExpect(status().isOk());

        mvc.perform(get(BASE + "/" + id + "/historico").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(2))   // a criação e esta edição
                .andExpect(jsonPath("$.data.itens[0].campo").value("valorMensal"))
                .andExpect(jsonPath("$.data.itens[0].valorAnterior").value("1500.00"))
                .andExpect(jsonPath("$.data.itens[0].valorNovo").value("1600.00"))
                .andExpect(jsonPath("$.data.itens[0].usuarioId").value(editor.toString()));
    }

    @Test
    void editarMandandoOsMesmosValoresNaoRegistraNada() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarComo(tenant, UUID.randomUUID());

        // 1500.0 (e não 1500.00) e o mesmo serviço: nada mudou de verdade
        mvc.perform(put(BASE + "/" + id)
                        .with(usuario(tenant, EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("1500.0", SERVICO)))
                .andExpect(status().isOk());

        mvc.perform(get(BASE + "/" + id + "/historico").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void mudarAListaDeServicosRegistraUmaLinhaSoComResumo() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarComo(tenant, UUID.randomUUID());
        String outroServico = "{\"descricao\":\"Hospedagem\",\"quantidade\":3,"
                + "\"valorUnitario\":150.00,\"valorTotal\":450.00}";

        mvc.perform(put(BASE + "/" + id)
                        .with(usuario(tenant, EDITAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("1500.00", outroServico)))
                .andExpect(status().isOk());

        mvc.perform(get(BASE + "/" + id + "/historico").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.itens[0].campo").value("servicosContratados"))
                .andExpect(jsonPath("$.data.itens[0].valorAnterior").value("1 serviço(s), total 300.00"))
                .andExpect(jsonPath("$.data.itens[0].valorNovo").value("1 serviço(s), total 450.00"));
    }

    // ---------------------------------------------------------------- paginação

    @Test
    void paginaOHistoricoETamanhoMaximoE100() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = criarComo(tenant, UUID.randomUUID());
        // 4 edições, cada uma mudando o valor mensal: com a criação, são 5 linhas
        for (String valor : new String[] {"1600.00", "1700.00", "1800.00", "1900.00"}) {
            mvc.perform(put(BASE + "/" + id)
                            .with(usuario(tenant, EDITAR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(edicao(valor, SERVICO)))
                    .andExpect(status().isOk());
        }

        mvc.perform(get(BASE + "/" + id + "/historico").param("tamanho", "2").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.tamanho").value(2))
                .andExpect(jsonPath("$.data.itens.length()").value(2))
                .andExpect(jsonPath("$.data.itens[0].valorNovo").value("1900.00"));
        mvc.perform(get(BASE + "/" + id + "/historico")
                        .param("pagina", "2").param("tamanho", "2").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.itens.length()").value(1))
                .andExpect(jsonPath("$.data.itens[0].valorNovo").value("RASCUNHO"));
        mvc.perform(get(BASE + "/" + id + "/historico").param("tamanho", "500").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.tamanho").value(100));
        mvc.perform(get(BASE + "/" + id + "/historico").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.tamanho").value(20));
    }
}
