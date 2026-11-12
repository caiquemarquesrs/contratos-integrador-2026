import type { ClientesService } from "@/services/contracts";
import { db } from "@/services/mock/db";
import { delay } from "@/services/mock/helpers";

export const clientesMock: ClientesService = {
  async listar(filters) {
    let all = db().clientes;
    if (filters.search) {
      const q = filters.search.toLowerCase();
      all = all.filter(
        (c) =>
          c.nome.toLowerCase().includes(q) || c.documento.includes(q)
      );
    }
    return delay(all);
  },

  async obterPorId(id) {
    return delay(db().clientes.find((c) => c.id === id) ?? null);
  },
};
