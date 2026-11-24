package br.com.plataforma.contratos.captcha;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP da verificação do reCAPTCHA. O gateway corta a requisição em 3 s (Contrato §8.4, 504),
 * e a criação ainda consulta o CRM (até 1 s) e grava no banco depois disso. Por isso o Google tem no
 * máximo 1,2 s para responder; passou disso, a criação falha fechada com "indisponível".
 */
@Configuration(proxyBeanMethods = false)
public class RecaptchaConfiguracao {

    @Bean
    @Qualifier("recaptchaRestClient")
    RestClient recaptchaRestClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1000))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(1200));   // inclui a conexão
        return builder.requestFactory(requestFactory).build();
    }
}
