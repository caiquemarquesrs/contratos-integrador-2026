package br.com.plataforma.contratos.documento;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.plataforma.contratos.api.CampoInvalidoException;
import br.com.plataforma.contratos.api.NaoEncontradoException;
import br.com.plataforma.contratos.api.Pagina;
import br.com.plataforma.contratos.contrato.ContratoRepositorio;
import br.com.plataforma.contratos.tenant.TenantContexto;

@Service
public class DocumentoServico {

    private static final int MAXIMO_DO_NOME = 255;
    private static final int MAXIMO_DO_TIPO = 150;
    private static final String TIPO_PADRAO = "application/octet-stream";

    /** O arquivo pronto para o download: nome, tipo e bytes. */
    public record Arquivo(String nome, String tipo, byte[] conteudo) {
    }

    private final DocumentoRepositorio documentos;
    private final ContratoRepositorio contratos;
    private final ArmazenamentoDeArquivos armazenamento;

    public DocumentoServico(DocumentoRepositorio documentos, ContratoRepositorio contratos,
                            ArmazenamentoDeArquivos armazenamento) {
        this.documentos = documentos;
        this.contratos = contratos;
        this.armazenamento = armazenamento;
    }

    @Transactional(readOnly = true)
    public Pagina<DocumentoDto> listar(UUID contratoId, int pagina, int tamanho) {
        exigirContrato(contratoId);
        PageRequest pedido = PageRequest.of(pagina, tamanho,
                Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.asc("id")));
        return Pagina.de(documentos.doContrato(contratoId, pedido), DocumentoDto::de);
    }

    @Transactional
    public DocumentoDto associar(UUID contratoId, String nomeInformado, String nomeOriginal, String tipo,
                                 byte[] conteudo, UUID usuario) {
        TenantContexto.exigir();   // nunca gravar sem tenant definido
        exigirContrato(contratoId);
        if (conteudo == null || conteudo.length == 0) {
            throw new CampoInvalidoException("arquivo", "CAMPO_INVALIDO", "O arquivo está vazio.");
        }
        if (conteudo.length > LimiteDeUpload.MAXIMO_EM_BYTES) {
            throw new CampoInvalidoException("arquivo", "ARQUIVO_GRANDE_DEMAIS",
                    "O arquivo passa do tamanho máximo de 5 MB.");
        }
        Documento documento = documentos.saveAndFlush(
                Documento.novo(contratoId, escolherNome(nomeInformado, nomeOriginal), tipoSeguro(tipo),
                        conteudo.length, usuario));
        armazenamento.salvar(documento.getId(), conteudo);
        return DocumentoDto.de(documento);
    }

    @Transactional(readOnly = true)
    public Arquivo baixar(UUID documentoId) {
        Documento documento = obter(documentoId);
        return new Arquivo(documento.getNome(), documento.getTipoArquivo(), armazenamento.ler(documento.getId()));
    }

    @Transactional
    public void excluir(UUID documentoId) {
        Documento documento = obter(documentoId);
        armazenamento.excluir(documento.getId());   // libera os bytes; o registro fica marcado como excluído
        documentos.delete(documento);
    }

    // ------------------------------------------------------------------ apoio

    private Documento obter(UUID documentoId) {
        return documentos.buscarPorId(documentoId)
                .orElseThrow(() -> new NaoEncontradoException("Documento não encontrado."));
    }

    /** Contrato inexistente ou de outra empresa: 404 (o filtro de tenant esconde o de outra empresa). */
    private void exigirContrato(UUID contratoId) {
        contratos.buscarPorId(contratoId)
                .orElseThrow(() -> new NaoEncontradoException("Contrato não encontrado."));
    }

    /** Nome de exibição: o informado; se não veio, o do próprio arquivo, sem pasta. */
    private static String escolherNome(String nomeInformado, String nomeOriginal) {
        if (nomeInformado != null && !nomeInformado.isBlank()) {
            String nome = limpar(nomeInformado);
            if (nome.length() > MAXIMO_DO_NOME) {
                throw new CampoInvalidoException("nome", "CAMPO_INVALIDO", "O nome pode ter até 255 caracteres.");
            }
            return nome.isEmpty() ? "documento" : nome;
        }
        String original = limpar(soONome(nomeOriginal));
        if (original.isEmpty()) {
            return "documento";
        }
        return original.length() > MAXIMO_DO_NOME ? original.substring(0, MAXIMO_DO_NOME) : original;
    }

    /** Alguns navegadores mandam o caminho completo ("C:\pasta\arquivo.pdf"): fica só o nome. */
    private static String soONome(String caminho) {
        if (caminho == null) {
            return "";
        }
        int corte = Math.max(caminho.lastIndexOf('/'), caminho.lastIndexOf('\\'));
        return caminho.substring(corte + 1);
    }

    /** Tira caracteres de controle (quebra de linha em nome de arquivo não serve a ninguém) e espaços nas pontas. */
    private static String limpar(String texto) {
        return texto.replaceAll("\\p{Cntrl}", "").strip();
    }

    /** Tipo informado pelo cliente: só vale se for um tipo de mídia válido; senão, binário genérico. */
    private static String tipoSeguro(String tipo) {
        if (tipo == null || tipo.isBlank() || tipo.length() > MAXIMO_DO_TIPO) {
            return TIPO_PADRAO;
        }
        try {
            MediaType.parseMediaType(tipo);
            return tipo.strip();
        } catch (InvalidMediaTypeException e) {
            return TIPO_PADRAO;
        }
    }
}
