package br.com.plataforma.contratos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração: PostgreSQL real em Testcontainers, sem depender de nenhum
 * outro módulo no ar (Contrato §14.5). O RabbitMQ é substituído por um mock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.flyway.user=teste",
        "spring.flyway.password=teste",
        "spring.flyway.create-schemas=true",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
public abstract class BaseIntegracao {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("plataforma")
            .withUsername("teste")
            .withPassword("teste");

    static {
        POSTGRES.start();
    }

    @MockitoBean
    protected RabbitTemplate rabbit;

    @Autowired
    protected MockMvc mvc;

    /** Token de usuário do tenant informado, com as permissões informadas. */
    protected static RequestPostProcessor usuario(UUID tenant, String... permissoes) {
        return jwt()
                .jwt(token -> token
                        .subject(UUID.randomUUID().toString())
                        .claim("tenant_id", tenant.toString())
                        .claim("perms", List.of(permissoes)))
                .authorities(autoridades(permissoes));
    }

    /** Token de serviço: sub = svc:{modulo}, sem tenant_id. */
    protected static RequestPostProcessor servico(String modulo, String... permissoes) {
        return jwt()
                .jwt(token -> token
                        .subject("svc:" + modulo)
                        .claim("perms", List.of(permissoes)))
                .authorities(autoridades(permissoes));
    }

    private static GrantedAuthority[] autoridades(String... permissoes) {
        return Arrays.stream(permissoes).map(SimpleGrantedAuthority::new).toArray(GrantedAuthority[]::new);
    }
}
