import type { ClientesService } from "@/services/contracts";

export const clientesReal: ClientesService = {
  async listar() {
    throw {
      code: "CLIENTS_ENDPOINT_UNAVAILABLE",
      message:
        "A API de contratos não disponibiliza endpoint de consulta de clientes/empresas; essa integração depende do CRM.",
    };
  },
  async obterPorId() {
    throw {
      code: "CLIENTS_ENDPOINT_UNAVAILABLE",
      message:
        "A API de contratos não disponibiliza consulta de cliente/empresa por ID.",
    };
  },
};
