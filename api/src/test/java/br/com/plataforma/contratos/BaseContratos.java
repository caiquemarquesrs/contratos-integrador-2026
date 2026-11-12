package br.com.plataforma.contratos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jayway.jsonpath.JsonPath;

/**
 * Apoio dos testes do ciclo de vida do contrato (aprovar, assinar, histórico). O contrato não tem rota
 * que leve RASCUNHO a EM_APROVACAO (decisão do grupo: não alterar a infra), então o teste coloca o
 * contrato no estado de que precisa direto no banco.
 */
public abstract class BaseContratos extends BaseIntegracao {

    protected static final String BASE = "/api/contratos/contratos";
    protected static final String VER = "contratos.contrato.ver";
    protected static final String CRIAR = "contratos.contrato.criar";
    protected static final String EDITAR = "contratos.contrato.editar";
    protected static final String APROVAR = "contratos.contrato.aprovar";
    protected static final String ASSINAR = "contratos.contrato.assinar";

    /** Serviço único usado nos contratos de teste: 2 x 150,00 = 300,00. */
    protected static final String SERVICO =
            "{\"descricao\":\"Hospedagem\",\"quantidade\":2,\"valorUnitario\":150.00,\"valorTotal\":300.00}";

    @Autowired
    protected JdbcTemplate jdbc;

    /** Como {@code usuario(...)}, mas com o id do usuário escolhido, para conferir auditoria, evento e histórico. */
    protected static RequestPostProcessor usuarioComId(UUID tenant, UUID usuarioId, String... permissoes) {
        GrantedAuthority[] autoridades = Arrays.stream(permissoes)
                .map(SimpleGrantedAuthority::new)
                .toArray(GrantedAuthority[]::new);
        return comRecaptchaValido(jwt()
                .jwt(token -> token
                        .subject(usuarioId.toString())
                        .claim("tenant_id", tenant.toString())
                        .claim("perms", List.of(permissoes)))
                .authorities(autoridades));
    }

    /** JSON de criação com valores conhecidos, para os testes poderem repeti-los num PUT. */
    protected static String corpoNovo(UUID empresa, UUID responsavel) {
        return """
                {"empresaId":"%s","servicosContratados":[%s],"valorMensal":1500.00,"mrr":1500.00,
                 "dataInicio":"2026-01-15","vigenciaMeses":12,"renovacao":"automatica",
                 "reajusteAnualPercentual":5.5,"indiceReajuste":"IPCA","avisoPrevioDias":30,
                 "responsavelId":"%s","origem":"crm","origemModuloId":"OPP-123"}"""
                .formatted(empresa, SERVICO, responsavel);
    }

    /** Cria um contrato (nasce RASCUNHO) e devolve o id. */
    protected UUID criarContrato(UUID tenant) throws Exception {
        return criarContratoComo(tenant, UUID.randomUUID());
    }

    protected UUID criarContratoComo(UUID tenant, UUID usuarioId) throws Exception {
        String resposta = mvc.perform(post(BASE)
                        .with(usuarioComId(tenant, usuarioId, CRIAR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoNovo(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(resposta, "$.data.id"));
    }

    protected void mudarStatus(UUID id, String status) {
        jdbc.update("update contratos.contratos set status = ? where id = ?", status, id);
    }

    protected String statusNoBanco(UUID id) {
        return jdbc.queryForObject("select status from contratos.contratos where id = ?", String.class, id);
    }
}
