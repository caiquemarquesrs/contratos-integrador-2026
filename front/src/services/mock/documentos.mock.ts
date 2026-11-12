import type { DocumentosService } from "@/services/contracts";
import type { Documento, DocumentoCreateInput } from "@/types/contratos";
import { db, saveDB } from "@/services/mock/db";
import { delay, generateId, nowISO } from "@/services/mock/helpers";

export const documentosMock: DocumentosService = {
  async listarPorContrato(contratoId) {
    const all = db().documentos.filter((d) => d.contratoId === contratoId);
    return delay(all);
  },

  async adicionar(input: DocumentoCreateInput) {
    const store = db();
    const now = nowISO();
    const { arquivo: _arquivo, ...metadados } = input;
    const doc: Documento = {
      ...metadados,
      id: generateId("doc"),
      enviadoEm: now,
      atualizadoEm: now,
    };
    store.documentos.push(doc);
    saveDB();
    return delay(doc);
  },

  async excluir(id) {
    const store = db();
    const idx = store.documentos.findIndex((d) => d.id === id);
    if (idx === -1) {
      throw { code: "NOT_FOUND", message: "Documento não encontrado." };
    }
    store.documentos.splice(idx, 1);
    saveDB();
    return delay(undefined);
  },

  async baixar() {
    throw {
      code: "MOCK_DOWNLOAD_UNAVAILABLE",
      message: "O modo simulado armazena apenas os metadados do documento.",
    };
  },
};
