import type { HistoricoService } from "@/services/contracts";
import { listarTodasAsPaginas } from "@/services/real/http";
import type { HistoricoEvento } from "@/types/contratos";

interface ApiHistorico {
  id: string;
  contratoId: string;
  campo: string;
  valorAnterior: string | null;
  valorNovo: string | null;
  usuarioId: string;
  ocorridoEm: string;
}

export const historicoReal: HistoricoService = {
  async listarPorContrato(contratoId) {
    const events = await listarTodasAsPaginas<ApiHistorico>(
      `/contratos/contratos/${encodeURIComponent(contratoId)}/historico`,
    );
    return events.map(
      (event): HistoricoEvento => ({
        id: event.id,
        contratoId: event.contratoId,
        dataHora: event.ocorridoEm,
        usuario: "",
        usuarioId: event.usuarioId,
        acao: `Campo ${event.campo} alterado`,
        campoAlterado: event.campo,
        valorAnterior: event.valorAnterior ?? undefined,
        valorNovo: event.valorNovo ?? undefined,
        simulado: false,
      }),
    );
  },
};
