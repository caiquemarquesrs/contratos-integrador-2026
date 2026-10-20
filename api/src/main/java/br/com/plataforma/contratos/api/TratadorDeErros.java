package br.com.plataforma.contratos.api;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converte toda exceção no envelope padrão, com o código HTTP da §8.4. */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    /** Campos de dinheiro: com mais de 2 casas decimais o código é PRECISAO_EXCEDIDA, sem arredondar (§8.7). */
    private static final Set<String> CAMPOS_MONETARIOS = Set.of("valorMensal", "mrr", "valorUnitario", "valorTotal");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Resposta<Void>> dadosInvalidos(MethodArgumentNotValidException e) {
        List<ErroCampo> erros = e.getBindingResult().getFieldErrors().stream()
                .map(c -> new ErroCampo(c.getField(), codigoDe(c), c.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(Resposta.falha("Não foi possível salvar: confira os campos.", erros));
    }

    /** @Digits num campo monetário é excesso de precisão; qualquer outra violação é CAMPO_INVALIDO. */
    private static String codigoDe(FieldError erro) {
        // o caminho pode ser aninhado, como "servicosContratados[0].valorUnitario"
        String campo = erro.getField().substring(erro.getField().lastIndexOf('.') + 1);
        return "Digits".equals(erro.getCode()) && CAMPOS_MONETARIOS.contains(campo)
                ? "PRECISAO_EXCEDIDA"
                : "CAMPO_INVALIDO";
    }

    @ExceptionHandler(CampoInvalidoException.class)
    ResponseEntity<Resposta<Void>> campoInvalido(CampoInvalidoException e) {
        return ResponseEntity.badRequest()
                .body(Resposta.falha("Não foi possível salvar: confira os campos.", List.of(e.getErro())));
    }

    @ExceptionHandler(ConflitoDeEstadoException.class)
    ResponseEntity<Resposta<Void>> conflitoDeEstado(ConflitoDeEstadoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Resposta.falha(e.getMessage(), List.of(e.getErro())));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Resposta<Void>> requisicaoInvalida(Exception e) {
        return ResponseEntity.badRequest().body(Resposta.falha("Requisição inválida."));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    ResponseEntity<Resposta<Void>> naoEncontrado(NaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Resposta.falha(e.getMessage()));
    }

    /** Lançada pelo @PreAuthorize: autenticado, mas sem a permissão exigida. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Resposta<Void>> semPermissao(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Resposta.falha("Sem permissão para esta operação."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Resposta<Void>> inesperado(Exception e) {
        if (e instanceof ErrorResponse erroDoSpring) {
            // 404 de rota inexistente, 405 de método errado e afins: já trazem o código certo
            return ResponseEntity.status(erroDoSpring.getStatusCode())
                    .body(Resposta.falha("Requisição não atendida por este endpoint."));
        }
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Resposta.falha("Erro inesperado. Tente de novo em instantes."));
    }
}
