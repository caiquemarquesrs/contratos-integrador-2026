package br.com.plataforma.contratos.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

/** Testa o cliente contra um servidorzinho local que faz o papel do CRM. Sem Spring e sem Docker. */
class ClienteCrmHttpTest {

    private static final UUID EMPRESA = UUID.fromString("3a7e91b0-1111-4222-8333-444455556666");

    private HttpServer servidor;
    private final AtomicReference<String> caminho = new AtomicReference<>();
    private final AtomicReference<String> autorizacao = new AtomicReference<>();

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.start();
    }

    @AfterEach
    void parar() {
        servidor.stop(0);
    }

    @Test
    void devolveARazaoSocialSemEspacosSobrando() {
        responder(200, "{\"success\":true,\"data\":{\"id\":\"" + EMPRESA
                + "\",\"razaoSocial\":\"  Centinela Soluções Digitais \"}}", 0);

        assertEquals(Optional.of("Centinela Soluções Digitais"), cliente(3000).razaoSocial(EMPRESA, "token-do-usuario"));
    }

    @Test
    void chamaOCaminhoCertoRepassandoOTokenDoUsuario() {
        responder(200, "{\"data\":{\"razaoSocial\":\"Acme\"}}", 0);

        cliente(3000).razaoSocial(EMPRESA, "token-do-usuario");

        assertEquals("/api/crm/empresas/" + EMPRESA + "/resumo", caminho.get());
        assertEquals("Bearer token-do-usuario", autorizacao.get());
    }

    @Test
    void empresaNaoEncontradaDevolveVazio() {
        responder(404, "{\"success\":false}", 0);

        assertEquals(Optional.empty(), cliente(3000).razaoSocial(EMPRESA, "t"));
    }

    @Test
    void erroDoCrmDevolveVazio() {
        responder(500, "{\"success\":false}", 0);

        assertEquals(Optional.empty(), cliente(3000).razaoSocial(EMPRESA, "t"));
    }

    @Test
    void corpoQueNaoEJsonDevolveVazio() {
        responder(200, "isto não é json", 0);

        assertEquals(Optional.empty(), cliente(3000).razaoSocial(EMPRESA, "t"));
    }

    @Test
    void razaoSocialEmBrancoDevolveVazio() {
        responder(200, "{\"data\":{\"razaoSocial\":\"   \"}}", 0);

        assertEquals(Optional.empty(), cliente(3000).razaoSocial(EMPRESA, "t"));
    }

    @Test
    void nomeMuitoLongoEhCortadoEm255() {
        responder(200, "{\"data\":{\"razaoSocial\":\"" + "A".repeat(400) + "\"}}", 0);

        assertEquals("A".repeat(255), cliente(3000).razaoSocial(EMPRESA, "t").orElseThrow());
    }

    @Test
    void demoraAlemDoTimeoutDevolveVazioSemEsperarOCrm() {
        responder(200, "{\"data\":{\"razaoSocial\":\"Acme\"}}", 1500);

        assertTimeout(Duration.ofSeconds(1), () ->
                assertEquals(Optional.empty(), cliente(200).razaoSocial(EMPRESA, "t")));
    }

    @Test
    void crmForaDoArDevolveVazio() throws IOException {
        int portaSemNinguem;
        try (ServerSocket livre = new ServerSocket(0)) {
            portaSemNinguem = livre.getLocalPort();
        }
        ClienteCrmHttp cliente = new ClienteCrmHttp("http://127.0.0.1:" + portaSemNinguem, 1000, new ObjectMapper());

        assertEquals(Optional.empty(), cliente.razaoSocial(EMPRESA, "t"));
    }

    private ClienteCrmHttp cliente(long timeoutMs) {
        return new ClienteCrmHttp("http://127.0.0.1:" + servidor.getAddress().getPort(), timeoutMs, new ObjectMapper());
    }

    private void responder(int status, String corpo, long atrasoMs) {
        servidor.createContext("/", troca -> {
            caminho.set(troca.getRequestURI().getPath());
            autorizacao.set(troca.getRequestHeaders().getFirst("Authorization"));
            try {
                if (atrasoMs > 0) {
                    Thread.sleep(atrasoMs);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            } catch (IOException e) {
                // o cliente já desistiu (teste de timeout): não é problema
            }
        });
    }
}
