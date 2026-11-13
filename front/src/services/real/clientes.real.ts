import type { ClientesService } from "@/services/contracts";
import { http } from "@/services/real/http";
import type { ApiError, Cliente } from "@/types/contratos";

/**
 * Integração com o CRM (contratos/crm.yaml no infra-integrador-2026), via gateway e com o
 * token do usuário logado. Permissão necessária: crm.empresa.ver_resumo (todo perfil operacional).
 *
 * - GET /api/crm/busca?q=texto            → razão social, nome fantasia ou CNPJ (2–100 chars, até 5)
 * - GET /api/crm/empresas/{id}/resumo     → razão social, CNPJ e cidade
 * - GET /api/crm/empresas                 → lista paginada (crm.empresa.ver: só comercial/gestão)
 */
interface CrmEmpresa {
  id: string;
  razaoSocial?: string | null;
  nomeFantasia?: string | null;
  cnpj?: string | null;
  cidade?: string | null;
  uf?: string | null;
  estado?: string | null;
}

const MIN_BUSCA = 2;
const MAX_BUSCA = 100;

function paraCliente(e: CrmEmpresa): Cliente {
  return {
    id: e.id,
    nome: e.razaoSocial || e.nomeFantasia || "(sem nome)",
    documento: (e.cnpj ?? "").replace(/\D/g, ""),
    tipo: "JURIDICA",
    cidade: e.cidade ?? undefined,
    uf: e.uf ?? e.estado ?? undefined,
  };
}

/** A API devolve array direto (busca/lote) ou página { itens } (lista completa). */
function extrairLista(data: CrmEmpresa[] | { itens: CrmEmpresa[] } | null): CrmEmpresa[] {
  if (!data) return [];
  return Array.isArray(data) ? data : (data.itens ?? []);
}

export const clientesReal: ClientesService = {
  async listar(filters) {
    const termo = (filters.search ?? "").trim().slice(0, MAX_BUSCA);

    if (termo.length >= MIN_BUSCA) {
      const data = await http.get<CrmEmpresa[] | { itens: CrmEmpresa[] }>(
        `/crm/busca?${new URLSearchParams({ q: termo })}`,
      );
      return extrairLista(data).map(paraCliente);
    }

    // Sem termo (ex.: filtro de clientes da listagem): tenta a lista completa. Ela exige
    // crm.empresa.ver; para perfis sem essa permissão (403) o filtro simplesmente fica vazio.
    try {
      const data = await http.get<{ itens: CrmEmpresa[] }>(
        "/crm/empresas?pagina=0&tamanho=100",
      );
      return extrairLista(data).map(paraCliente);
    } catch (error) {
      if ((error as ApiError).code === "HTTP_403") return [];
      throw error;
    }
  },

  async obterPorId(id) {
    try {
      const data = await http.get<CrmEmpresa>(
        `/crm/empresas/${encodeURIComponent(id)}/resumo`,
      );
      return data ? paraCliente(data) : null;
    } catch (error) {
      if ((error as ApiError).code === "HTTP_404") return null;
      throw error;
    }
  },
};
