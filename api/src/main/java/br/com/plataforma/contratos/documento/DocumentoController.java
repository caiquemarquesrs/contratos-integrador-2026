package br.com.plataforma.contratos.documento;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.plataforma.contratos.api.CampoInvalidoException;
import br.com.plataforma.contratos.api.ErroCampo;
import br.com.plataforma.contratos.api.Pagina;
import br.com.plataforma.contratos.api.Resposta;
import br.com.plataforma.contratos.seguranca.Usuarios;

/** Documentos de contrato (RF-08). Cada operação confere a permissão no back-end (Contrato §5.2). */
@RestController
@RequestMapping("/api/contratos")
public class DocumentoController {

    private final DocumentoServico servico;

    public DocumentoController(DocumentoServico servico) {
        this.servico = servico;
    }

    @GetMapping("/contratos/{id}/documentos")
    @PreAuthorize("hasAuthority('contratos.documento.ver')")
    public Resposta<Pagina<DocumentoDto>> listar(@PathVariable UUID id,
                                                 @RequestParam(defaultValue = "0") int pagina,
                                                 @RequestParam(defaultValue = "20") int tamanho) {
        return Resposta.ok(servico.listar(id, Math.max(pagina, 0), Math.clamp(tamanho, 1, 100)));
    }

    /** multipart/form-data: o campo "arquivo" (obrigatório) e o "nome" de exibição (opcional). */
    @PostMapping(path = "/contratos/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('contratos.documento.criar')")
    public ResponseEntity<Resposta<DocumentoDto>> associar(@PathVariable UUID id,
                                                           @RequestParam(name = "arquivo", required = false) MultipartFile arquivo,
                                                           @RequestParam(name = "nome", required = false) String nome,
                                                           @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (arquivo == null) {
            throw new CampoInvalidoException("arquivo", "CAMPO_INVALIDO", "Envie o arquivo no campo \"arquivo\".");
        }
        DocumentoDto documento = servico.associar(id, nome, arquivo.getOriginalFilename(), arquivo.getContentType(),
                arquivo.getBytes(), Usuarios.idDe(jwt));
        return ResponseEntity.created(URI.create(documento.urlDownload())).body(Resposta.ok(documento));
    }

    /**
     * Única resposta fora do envelope (Contrato §8.2): devolve o arquivo. Sempre como anexo, e com
     * nosniff, para um HTML enviado por alguém nunca ser exibido como página.
     */
    @GetMapping("/documentos/{documentoId}/download")
    @PreAuthorize("hasAuthority('contratos.documento.ver')")
    public ResponseEntity<byte[]> baixar(@PathVariable UUID documentoId) {
        DocumentoServico.Arquivo arquivo = servico.baixar(documentoId);
        return ResponseEntity.ok()
                .contentType(tipoDeMidia(arquivo.tipo()))
                .contentLength(arquivo.conteudo().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nome(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(arquivo.conteudo());
    }

    @DeleteMapping("/documentos/{documentoId}")
    @PreAuthorize("hasAuthority('contratos.documento.excluir')")
    public Resposta<Void> excluir(@PathVariable UUID documentoId) {
        servico.excluir(documentoId);
        return new Resposta<>(true, null, "Documento excluído.", List.<ErroCampo>of());
    }

    private static MediaType tipoDeMidia(String tipo) {
        try {
            return MediaType.parseMediaType(tipo);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
