import type { ModelosService } from "@/services/contracts";
import { listarTodasAsPaginas } from "@/services/real/http";
import type { ModeloContrato } from "@/types/contratos";

interface ApiModelo {
  id: string;
  nome: string;
  descricao: string | null;
}

export const modelosReal: ModelosService = {
  async listar() {
    const templates = await listarTodasAsPaginas<ApiModelo>(
      "/contratos/modelos",
    );
    return templates.map(
      (template): ModeloContrato => ({
        id: template.id,
        nome: template.nome,
        descricao: template.descricao ?? undefined,
      }),
    );
  },
};
