import type { DocumentosService } from "@/services/contracts";
import { http, listarTodasAsPaginas } from "@/services/real/http";
import type { Documento } from "@/types/contratos";

interface ApiDocumento {
  id: string;
  contratoId: string;
  nome: string;
  tipoArquivo: string;
  tamanhoBytes: number;
  urlDownload: string;
  criadoEm: string;
  criadoPor: string;
}

function fromApi(document: ApiDocumento): Documento {
  return {
    id: document.id,
    contratoId: document.contratoId,
    nome: document.nome,
    tipo: document.tipoArquivo,
    arquivoTamanho: document.tamanhoBytes,
    arquivoTipo: document.tipoArquivo,
    enviadoEm: document.criadoEm,
    responsavel: document.criadoPor,
  };
}

export const documentosReal: DocumentosService = {
  async listarPorContrato(contratoId) {
    const documents = await listarTodasAsPaginas<ApiDocumento>(
      `/contratos/contratos/${encodeURIComponent(contratoId)}/documentos`,
    );
    return documents.map(fromApi);
  },
  async adicionar(input) {
    if (!input.arquivo) {
      throw {
        code: "ARQUIVO_OBRIGATORIO",
        message: "Selecione um arquivo para enviar à API real.",
      };
    }
    if (input.arquivo.size > 5 * 1024 * 1024) {
      throw {
        code: "ARQUIVO_GRANDE_DEMAIS",
        message: "O arquivo passa do tamanho máximo de 5 MB aceito pela API.",
        fieldErrors: { arquivo: "O tamanho máximo é 5 MB." },
      };
    }

    const formData = new FormData();
    formData.append("arquivo", input.arquivo, input.arquivo.name);
    if (input.nome.trim()) formData.append("nome", input.nome.trim());
    const document = await http.upload<ApiDocumento>(
      `/contratos/contratos/${encodeURIComponent(input.contratoId)}/documentos`,
      formData,
    );
    return fromApi(document);
  },
  excluir(id) {
    return http.delete<void>(
      `/contratos/documentos/${encodeURIComponent(id)}`,
    );
  },
  async baixar(id) {
    const { blob, filename } = await http.baixar(
      `/contratos/documentos/${encodeURIComponent(id)}/download`,
    );
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename ?? "documento";
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  },
};
