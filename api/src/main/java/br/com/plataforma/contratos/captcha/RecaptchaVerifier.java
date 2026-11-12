package br.com.plataforma.contratos.captcha;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class RecaptchaVerifier {

    private static final String GOOGLE_VERIFY_URL =
            "https://www.google.com/recaptcha/api/siteverify";

    private final RestClient restClient;
    private final String secretKey;

    public RecaptchaVerifier(
            @Qualifier("recaptchaRestClient") RestClient restClient,
            @Value("${recaptcha.secret-key:}") String secretKey) {
        this.restClient = restClient;
        this.secretKey = secretKey;
    }

    public void validar(String token) {
        if (token == null || token.isBlank()) {
            throw RecaptchaException.invalido();
        }
        if (secretKey.isBlank()) {
            throw RecaptchaException.indisponivel();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);

        GoogleResponse resposta;
        try {
            resposta = restClient.post()
                    .uri(GOOGLE_VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleResponse.class);
        } catch (RestClientException e) {
            throw RecaptchaException.indisponivel();
        }

        if (resposta == null || !resposta.success()) {
            throw RecaptchaException.invalido();
        }
    }

    private record GoogleResponse(boolean success) {
    }
}
