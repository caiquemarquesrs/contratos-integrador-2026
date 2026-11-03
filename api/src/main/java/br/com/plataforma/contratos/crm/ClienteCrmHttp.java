package br.com.plataforma.contratos.crm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Chama o CRM direto, pelo nome do serviço na rede interna, sem passar pelo gateway (Contrato §9.1).
 * O endereço vem da configuração (variável CRM_BASE_URL); o padrão é o nome do container do compose.
 * Timeout de 3 segundos e nenhuma exceção para fora (§9.6).
 */
@Component
public class ClienteCrmHttp implements ClienteCrm {

    private static final Logger log = LoggerFactory.getLogger(ClienteCrmHttp.class);
    private static final int TAMANHO_MAXIMO_DO_NOME = 255;   // coluna empresa_nome

    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public ClienteCrmHttp(@Value("${crm.base-url:http://crm:8082}") String baseUrl,
                          @Value("${crm.timeout-ms:3000}") long timeoutMs,
                          ObjectMapper mapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.mapper = mapper;
    }

    @Override
    public Optional<String> razaoSocial(UUID empresaId, String tokenDoUsuario) {
        try {
            HttpRequest pedido = HttpRequest.newBuilder(URI.create(baseUrl + "/api/crm/empresas/" + empresaId + "/resumo"))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + tokenDoUsuario)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != 200) {
                log.warn("CRM respondeu {} ao buscar a empresa {}; o contrato fica sem empresaNome.",
                        resposta.statusCode(), empresaId);
                return Optional.empty();
            }
            String nome = mapper.readTree(resposta.body()).path("data").path("razaoSocial").asText("").strip();
            if (nome.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(nome.length() > TAMANHO_MAXIMO_DO_NOME ? nome.substring(0, TAMANHO_MAXIMO_DO_NOME) : nome);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Consulta ao CRM interrompida para a empresa {}.", empresaId);
            return Optional.empty();
        } catch (Exception e) {
            // Sem o token no log, só o tipo do erro: o contrato é criado mesmo assim.
            log.warn("CRM indisponível ao buscar a empresa {}: {}", empresaId, e.toString());
            return Optional.empty();
        }
    }
}
