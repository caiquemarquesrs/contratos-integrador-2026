package br.com.plataforma.contratos.seguranca;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

/** Leitura do usuário a partir do token. */
public final class Usuarios {

    private Usuarios() {
    }

    /** Token emitido para rotina sem usuário (Contrato §9.2). */
    public static boolean ehServico(Jwt jwt) {
        String sub = jwt.getSubject();
        return sub != null && sub.startsWith("svc:");
    }

    /** Id do usuário, para created_by e updated_by; nulo quando quem chama é um serviço. */
    public static UUID idDe(Jwt jwt) {
        return ehServico(jwt) ? null : UUID.fromString(jwt.getSubject());
    }
}
