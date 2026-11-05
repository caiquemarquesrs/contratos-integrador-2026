package br.com.plataforma.contratos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** Tarefa 10: documentos de contrato (RF-08). */
class DocumentosTest extends BaseIntegracao {

    private static final String CONTRATOS = "/api/contratos/contratos";
    private static final String DOCUMENTOS = "/api/contratos/documentos";
    private static final String VER = "contratos.documento.ver";
    private static final String CRIAR = "contratos.documento.criar";
    private static final String EXCLUIR = "contratos.documento.excluir";
    private static final String ARQUIVO = "arquivo";

    @Autowired
    JdbcTemplate jdbcDoTeste;

    // ------------------------------------------------------------ segurança

    @Test
    void semTokenResponde401NosQuatroEndpoints() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get(CONTRATOS + "/" + id + "/documentos")).andExpect(status().isUnauthorized());
        mvc.perform(multipart(CONTRATOS + "/" + id + "/documentos").file(pdf("a.pdf", 10)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(DOCUMENTOS + "/" + id + "/download")).andExpect(status().isUnauthorized());
        mvc.perform(delete(DOCUMENTOS + "/" + id)).andExpect(status().isUnauthorized());
    }

    @Test
    void semPermissaoResponde403NosQuatroEndpoints() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        mvc.perform(get(CONTRATOS + "/" + id + "/documentos").with(usuario(tenant, "contratos.acessar")))
                .andExpect(status().isForbidden());
        mvc.perform(multipart(CONTRATOS + "/" + id + "/documentos").file(pdf("a.pdf", 10))
                        .with(usuario(tenant, VER, EXCLUIR)))
                .andExpect(status().isForbidden());
        mvc.perform(get(DOCUMENTOS + "/" + id + "/download").with(usuario(tenant, CRIAR)))
                .andExpect(status().isForbidden());
        mvc.perform(delete(DOCUMENTOS + "/" + id).with(usuario(tenant, VER, CRIAR)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ envio, lista e download

    @Test
    void enviaListaEBaixaOMesmoArquivo() throws Exception {
        UUID tenant = UUID.randomUUID();
        String contrato = criarContrato(tenant);
        byte[] bytes = new byte[2000];
        new Random(42).nextBytes(bytes);   // bytes quaisquer, que não são texto válido

        String resposta = mvc.perform(multipart(CONTRATOS + "/" + contrato + "/documentos")
                        .file(new MockMultipartFile(ARQUIVO, "scan.pdf", "application/pdf", bytes))
                        .param("nome", "Contrato assinado.pdf")
                        .with(usuario(tenant, CRIAR)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/contratos/documentos/")))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.contratoId").value(contrato))
                .andExpect(jsonPath("$.data.nome").value("Contrato assinado.pdf"))
                .andExpect(jsonPath("$.data.tipoArquivo").value("application/pdf"))
                .andExpect(jsonPath("$.data.tamanhoBytes").value(2000))
                .andExpect(jsonPath("$.data.urlDownload", startsWith("/api/contratos/documentos/")))
                .andExpect(jsonPath("$.data.urlDownload", endsWith("/download")))
                .andExpect(jsonPath("$.data.criadoEm").exists())
                .andExpect(jsonPath("$.data.criadoPor").exists())
                .andReturn().getResponse().getContentAsString();
        String documento = JsonPath.read(resposta, "$.data.id");

        mvc.perform(get(CONTRATOS + "/" + contrato + "/documentos").with(usuario(tenant, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.itens[0].id").value(documento));

        byte[] baixado = mvc.perform(get(DOCUMENTOS + "/" + documento + "/download").with(usuario(tenant, VER)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("Contrato")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(bytes, baixado);
    }

    @Test
    void semNomeUsaONomeDoArquivoSemAPasta() throws Exception {
        UUID tenant = UUID.randomUUID();
        String contrato = criarContrato(tenant);

        mvc.perform(multipart(CONTRATOS + "/" + contrato + "/documentos")
                        .file(new MockMultipartFile(ARQUIVO, "C:\\Users\\ana\\Documents\\proposta.pdf", "application/pdf", new byte[]{1, 2, 3}))
                        .with(usuario(tenant, CRIAR)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nome").value("proposta.pdf"));
    }

    @Test
    void tipoNaoInformadoViraBinarioGenerico() throws Exception {
        UUID tenant = UUID.randomUUID();
        String contrato = criarContrato(tenant);

        mvc.perform(multipart(CONTRATOS + "/" + contrato + "/documentos")
                        .file(new MockMultipartFile(ARQUIVO, "sem-tipo.bin", null, new byte[]{1, 2, 3}))
                        .with(usuario(tenant, CRIAR)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tipoArquivo").value("application/octet-stream"));
    }

    @Test
    void listaPaginadaDoContrato() throws Exception {
        UUID tenant = UUID.randomUUID();
        String contrato = criarContrato(tenant);
        for (int i = 1; i <= 3; i++) {
            enviar(tenant, contrato, pdf("doc" + i + ".pdf", 50)).andExpect(status().isCreated());
        }

        mvc.perform(get(CONTRATOS + "/" + contrato + "/documentos?tamanho=2").with(usuario(tenant, VER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.itens.length()").value(2));
    }

    // ------------------------------------------------------------ validação

    @Test
    void semOCampoArquivoResponde400ComOCampo() throws Exception {
        UUID tenant = UUID.randomUUID();
        String contrato = criarContrato(tenant);

        mvc.perform(multipart(CONTRATOS + "/" + contrato + "/documentos").param("nome", "so-o-nome")
                        .with(usuario(tenant, CRIAR)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value(ARQUIVO));
    }

    @Test
    void arquivoVazioResponde400() throws Exception {
        UUID tenant = UUID.randomUUID();
        String contrato = criarContrato(tenant);

        enviar(tenant, contrato, new MockMultipartFile(ARQUIVO, "vazio.pdf", "application/pdf", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value(ARQUIVO));
    }

    @Test
    void arquivoDeExatamente5MbPassaEUmByteAMaisNao() throws Exception {
        UUID tenant = UUID.randomUUID();
        String contrato = criarContrato(tenant);

        enviar(tenant, contrato, pdf("no-limite.pdf", 5 * 1024 * 1024)).andExpect(status().isCreated());
        enviar(tenant, contrato, pdf("passou.pdf", 5 * 1024 * 1024 + 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value(ARQUIVO))
                .andExpect(jsonPath("$.errors[0].codigo").value("ARQUIVO_GRANDE_DEMAIS"));
    }

    @Test
    void nomeComMaisDe255CaracteresResponde400() throws Exception {
        UUID tenant = UUID.randomUUID();
        String contrato = criarContrato(tenant);

        mvc.perform(multipart(CONTRATOS + "/" + contrato + "/documentos").file(pdf("a.pdf", 10))
                        .param("nome", "n".repeat(256))
                        .with(usuario(tenant, CRIAR)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].campo").value("nome"));
    }

    // ------------------------------------------------------------ 404 e isolamento por empresa

    @Test
    void contratoInexistenteResponde404() throws Exception {
        UUID tenant = UUID.randomUUID();
        UUID inexistente = UUID.randomUUID();

        mvc.perform(get(CONTRATOS + "/" + inexistente + "/documentos").with(usuario(tenant, VER)))
                .andExpect(status().isNotFound());
        enviar(tenant, inexistente.toString(), pdf("a.pdf", 10)).andExpect(status().isNotFound());
        mvc.perform(get(DOCUMENTOS + "/" + inexistente + "/download").with(usuario(tenant, VER)))
                .andExpect(status().isNotFound());
        mvc.perform(delete(DOCUMENTOS + "/" + inexistente).with(usuario(tenant, EXCLUIR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void outraEmpresaNaoVeNemBaixaNemExcluiNemEnvia() throws Exception {
        UUID empresaA = UUID.randomUUID();
        UUID empresaB = UUID.randomUUID();
        String contrato = criarContrato(empresaA);
        String documento = JsonPath.read(
                enviar(empresaA, contrato, pdf("segredo.pdf", 100)).andReturn().getResponse().getContentAsString(),
                "$.data.id");

        mvc.perform(get(CONTRATOS + "/" + contrato + "/documentos").with(usuario(empresaB, VER)))
                .andExpect(status().isNotFound());
        enviar(empresaB, contrato, pdf("invasor.pdf", 10)).andExpect(status().isNotFound());
        mvc.perform(get(DOCUMENTOS + "/" + documento + "/download").with(usuario(empresaB, VER)))
                .andExpect(status().isNotFound());
        mvc.perform(delete(DOCUMENTOS + "/" + documento).with(usuario(empresaB, EXCLUIR)))
                .andExpect(status().isNotFound());

        // e o da empresa A continua lá
        mvc.perform(get(DOCUMENTOS + "/" + documento + "/download").with(usuario(empresaA, VER)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------ exclusão

    @Test
    void excluirSomeDaListaDoDownloadELiberaOsBytes() throws Exception {
        UUID tenant = UUID.randomUUID();
        String contrato = criarContrato(tenant);
        String documento = JsonPath.read(
                enviar(tenant, contrato, pdf("apagar.pdf", 300)).andReturn().getResponse().getContentAsString(),
                "$.data.id");

        mvc.perform(delete(DOCUMENTOS + "/" + documento).with(usuario(tenant, EXCLUIR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mvc.perform(get(CONTRATOS + "/" + contrato + "/documentos").with(usuario(tenant, VER)))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get(DOCUMENTOS + "/" + documento + "/download").with(usuario(tenant, VER)))
                .andExpect(status().isNotFound());
        mvc.perform(delete(DOCUMENTOS + "/" + documento).with(usuario(tenant, EXCLUIR)))
                .andExpect(status().isNotFound());

        // exclusão lógica: a linha fica (com deleted_at), mas o arquivo foi liberado
        Integer linhas = jdbcDoTeste.queryForObject(
                "select count(*) from contratos.documentos where id = ? and deleted_at is not null and conteudo is null",
                Integer.class, UUID.fromString(documento));
        assertEquals(1, linhas);
    }

    // ------------------------------------------------------------ apoio

    private ResultActions enviar(UUID tenant, String contratoId, MockMultipartFile arquivo) throws Exception {
        return mvc.perform(multipart(CONTRATOS + "/" + contratoId + "/documentos").file(arquivo)
                .with(usuario(tenant, CRIAR)));
    }

    private static MockMultipartFile pdf(String nome, int tamanho) {
        byte[] bytes = new byte[tamanho];
        new Random(tamanho).nextBytes(bytes);
        return new MockMultipartFile(ARQUIVO, nome, "application/pdf", bytes);
    }

    /** Cria um contrato válido na empresa e devolve o id (o CRM é falso nos testes: o nome da empresa fica vazio). */
    private String criarContrato(UUID tenant) throws Exception {
        String resposta = mvc.perform(post(CONTRATOS).with(usuario(tenant, "contratos.contrato.criar"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """.formatted(UUID.randomUUID(), UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.data.id");
    }
}
