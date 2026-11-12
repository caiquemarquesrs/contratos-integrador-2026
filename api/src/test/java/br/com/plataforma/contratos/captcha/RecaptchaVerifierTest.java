package br.com.plataforma.contratos.captcha;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RecaptchaVerifierTest {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final String SECRET = "test-secret";

    private MockRestServiceServer server;
    private RecaptchaVerifier verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new RecaptchaVerifier(builder.build(), SECRET);
    }

    @Test
    void aceitaSomenteRespostaDeSucessoDoGoogle() {
        server.expect(requestTo(VERIFY_URL))
                .andExpect(method(POST))
                .andExpect(content().formDataContains(Map.of(
                        "secret", SECRET,
                        "response", "token-valido")))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> verifier.validar("token-valido"));
        server.verify();
    }

    @Test
    void rejeitaTokenExpiradoOuJaUtilizado() {
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}",
                        MediaType.APPLICATION_JSON));

        RecaptchaException erro = assertThrows(
                RecaptchaException.class, () -> verifier.validar("token-expirado-ou-reutilizado"));

        assertEquals("RECAPTCHA_INVALIDO", erro.codigo());
        server.verify();
    }

    @Test
    void rejeitaSegundaValidacaoDoMesmoTokenConformeRespostaDoGoogle() {
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"success\":true}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}",
                        MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> verifier.validar("token-de-uso-unico"));
        RecaptchaException erro = assertThrows(
                RecaptchaException.class, () -> verifier.validar("token-de-uso-unico"));

        assertEquals("RECAPTCHA_INVALIDO", erro.codigo());
        server.verify();
    }

    @Test
    void rejeitaTokenVazioSemConsultarGoogle() {
        RecaptchaException erro = assertThrows(RecaptchaException.class, () -> verifier.validar(" "));

        assertEquals("RECAPTCHA_INVALIDO", erro.codigo());
        server.verify();
    }

    @Test
    void falhaFechadoQuandoChaveAusenteOuGoogleIndisponivel() {
        RecaptchaVerifier semChave = new RecaptchaVerifier(RestClient.builder().build(), "");
        RecaptchaException semChaveErro = assertThrows(
                RecaptchaException.class, () -> semChave.validar("token"));
        assertEquals("RECAPTCHA_INDISPONIVEL", semChaveErro.codigo());
        server.verify();

        server.expect(requestTo(VERIFY_URL)).andRespond(withServerError());
        RecaptchaException indisponivel = assertThrows(
                RecaptchaException.class, () -> verifier.validar("token"));
        assertEquals("RECAPTCHA_INDISPONIVEL", indisponivel.codigo());
        server.verify();
    }
}
