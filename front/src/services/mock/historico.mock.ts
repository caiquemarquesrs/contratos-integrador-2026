import type { HistoricoService } from "@/services/contracts";
import { db } from "@/services/mock/db";
import { delay } from "@/services/mock/helpers";

export const historicoMock: HistoricoService = {
  async listarPorContrato(contratoId) {
    const all = db()
      .historico.filter((h) => h.contratoId === contratoId)
      .sort((a, b) => b.dataHora.localeCompare(a.dataHora));
    return delay(all);
  },
};
