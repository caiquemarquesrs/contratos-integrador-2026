package br.com.plataforma.contratos.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Arquivo acima do limite do servidor vira 400 no envelope (o contrato prevê 400, não 413). */
class TratadorDeErrosUploadTest {

    @Test
    void arquivoAcimaDoLimiteDoServidorResponde400ComCodigo() {
        ResponseEntity<Resposta<Void>> resposta =
                new TratadorDeErros().arquivoGrandeDemais(new MaxUploadSizeExceededException(5L * 1024 * 1024));

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertFalse(resposta.getBody().success());
        assertEquals("arquivo", resposta.getBody().errors().get(0).campo());
        assertEquals("ARQUIVO_GRANDE_DEMAIS", resposta.getBody().errors().get(0).codigo());
    }
}
