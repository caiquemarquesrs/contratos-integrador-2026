import type { ContratosService } from "@/services/contracts";
import type {
  Contrato,
  ContratoFilters,
  HistoricoEvento,
  PaginatedResult,
  StatusContrato,
} from "@/types/contratos";
import { db, saveDB } from "@/services/mock/db";
import { delay, generateId, nowISO } from "@/services/mock/helpers";

function matchContrato(c: Contrato, f: ContratoFilters): boolean {
  if (f.search) {
    const q = f.search.toLowerCase();
    if (
      !c.numero.toLowerCase().includes(q) &&
      !(c.clienteNome ?? "").toLowerCase().includes(q)
    )
      return false;
  }
  if (f.clienteId && c.clienteId !== f.clienteId) return false;
  if (f.status && c.status !== f.status) return false;
  return true;
}

export const contratosMock: ContratosService = {
  async listar(filters, signal) {
    const all = db().contratos;
    const filtered = all.filter((c) => matchContrato(c, filters));
    // simular cancelamento por AbortSignal
    if (signal?.aborted) {
      throw { code: "ABORTED", message: "Requisição cancelada" };
    }
    const start = (filters.page - 1) * filters.pageSize;
    const items = filtered.slice(start, start + filters.pageSize);
    return delay<PaginatedResult<Contrato>>({
      items,
      page: filters.page,
      pageSize: filters.pageSize,
      total: filtered.length,
    });
  },

  async obterPorId(id) {
    const c = db().contratos.find((x) => x.id === id) ?? null;
    return delay(c);
  },

  async criar(input) {
    const store = db();
    const now = nowISO();
    const cliente = store.clientes.find((cl) => cl.id === input.clienteId);
    const contrato: Contrato = {
      ...input,
      id: generateId("ct"),
      clienteNome: cliente?.nome,
      criadoEm: now,
      atualizadoEm: now,
    };
    store.contratos.unshift(contrato);
    store.historico.unshift({
      id: generateId("h"),
      contratoId: contrato.id,
      dataHora: now,
      usuario: contrato.responsavel || "Usuário",
      acao: "Contrato criado",
      simulado: true,
    });
    saveDB();
    return delay(contrato);
  },

  async atualizar(id, input) {
    const store = db();
    const idx = store.contratos.findIndex((c) => c.id === id);
    if (idx === -1) {
      throw { code: "NOT_FOUND", message: "Contrato não encontrado." };
    }
    const before = store.contratos[idx];
    const cliente = input.clienteId
      ? store.clientes.find((cl) => cl.id === input.clienteId)
      : undefined;
    const updated: Contrato = {
      ...before,
      ...input,
      clienteNome: cliente?.nome ?? before.clienteNome,
      atualizadoEm: nowISO(),
    };
    store.contratos[idx] = updated;
    store.historico.unshift({
      id: generateId("h"),
      contratoId: id,
      dataHora: nowISO(),
      usuario: updated.responsavel || "Usuário",
      acao: "Contrato atualizado",
      simulado: true,
    });
    saveDB();
    return delay(updated);
  },

  async alterarStatus(id, input) {
    const store = db();
    const idx = store.contratos.findIndex((c) => c.id === id);
    if (idx === -1) {
      throw { code: "NOT_FOUND", message: "Contrato não encontrado." };
    }
    const before = store.contratos[idx];
    const updated: Contrato = {
      ...before,
      status: input.status as StatusContrato,
      atualizadoEm: nowISO(),
    };
    store.contratos[idx] = updated;
    store.historico.unshift({
      id: generateId("h"),
      contratoId: id,
      dataHora: nowISO(),
      usuario: updated.responsavel || "Usuário",
      acao: "Status alterado",
      campoAlterado: "status",
      valorAnterior: before.status,
      valorNovo: updated.status,
      simulado: true,
    });
    saveDB();
    return delay(updated);
  },
};

// helper para outros serviços registrarem histórico
export function registrarHistorico(evt: Omit<HistoricoEvento, "id" | "simulado">) {
  const store = db();
  store.historico.unshift({
    ...evt,
    id: generateId("h"),
    simulado: true,
  });
  saveDB();
}
