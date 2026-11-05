package br.com.plataforma.contratos.documento;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

/**
 * Limite de upload do servidor. O padrão do Spring é 1 MB, pequeno demais para um contrato escaneado.
 * Passou disso, o TratadorDeErros responde 400 com ARQUIVO_GRANDE_DEMAIS (o contrato prevê 400, não 413).
 */
@Configuration
public class ConfiguracaoDeUpload {

    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory fabrica = new MultipartConfigFactory();
        fabrica.setMaxFileSize(DataSize.ofBytes(LimiteDeUpload.MAXIMO_EM_BYTES));
        // folga para os outros campos do formulário (o nome, por exemplo)
        fabrica.setMaxRequestSize(DataSize.ofBytes(LimiteDeUpload.MAXIMO_EM_BYTES + 64 * 1024));
        return fabrica.createMultipartConfig();
    }
}
