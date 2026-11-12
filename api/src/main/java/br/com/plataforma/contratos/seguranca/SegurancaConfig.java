package br.com.plataforma.contratos.seguranca;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * O módulo só valida tokens; quem emite é o identity (Contrato §4). A chave pública vem do JWKS
 * configurado em spring.security.oauth2.resourceserver.jwt.jwk-set-uri.
 */
@Configuration
@EnableMethodSecurity
public class SegurancaConfig {

    @Bean
    SecurityFilterChain filtrosDeSeguranca(HttpSecurity http, ObjectMapper mapper) throws Exception {
        EscritorDeErro erros = new EscritorDeErro(mapper);
        AuthenticationEntryPoint naoAutenticado =
                (requisicao, resposta, excecao) -> erros.escrever(resposta, 401, "Autenticação necessária.");
        AccessDeniedHandler semPermissao =
                (requisicao, resposta, excecao) -> erros.escrever(resposta, 403, "Sem permissão para esta operação.");

        http
                .cors(Customizer.withDefaults())   // CorsFilter roda antes do filtro de autenticação: preflight não exige token
                .csrf(csrf -> csrf.disable())   // token no cabeçalho, sem cookie: CSRF não se aplica
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers("/api/contratos/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(permissoesDoToken()))
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(semPermissao))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(semPermissao))
                .addFilterAfter(new TenantFiltro(erros), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /** Cada item do claim "perms" vira uma authority, sem prefixo: hasAuthority('crm.oportunidade.ver'). */
    static JwtAuthenticationConverter permissoesDoToken() {
        JwtGrantedAuthoritiesConverter permissoes = new JwtGrantedAuthoritiesConverter();
        permissoes.setAuthoritiesClaimName("perms");
        permissoes.setAuthorityPrefix("");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(permissoes);
        return conversor;
    }

    /**
     * Origens autorizadas vêm de cors.origens-permitidas. X-Recaptcha-Token precisa estar liberado:
     * o cadastro envia o token do CAPTCHA nesse cabeçalho, que o preflight barra se não for permitido.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.origens-permitidas}") List<String> origensPermitidas) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Recaptcha-Token"));
        config.setExposedHeaders(List.of("Content-Disposition"));   // nome do arquivo no download
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }
}
