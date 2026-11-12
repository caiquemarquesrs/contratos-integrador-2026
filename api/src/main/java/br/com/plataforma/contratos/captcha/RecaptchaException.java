package br.com.plataforma.contratos.captcha;

import org.springframework.http.HttpStatus;

public final class RecaptchaException extends RuntimeException {

    private final String codigo;
    private final HttpStatus status;

    private RecaptchaException(String codigo, String mensagem, HttpStatus status) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
    }

    public static RecaptchaException invalido() {
        return new RecaptchaException(
                "RECAPTCHA_INVALIDO",
                "A verificação CAPTCHA é inválida ou expirou. Resolva o desafio novamente.",
                HttpStatus.BAD_REQUEST);
    }

    public static RecaptchaException indisponivel() {
        return new RecaptchaException(
                "RECAPTCHA_INDISPONIVEL",
                "Não foi possível validar o CAPTCHA. Tente novamente.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    public String codigo() {
        return codigo;
    }

    public HttpStatus status() {
        return status;
    }
}
