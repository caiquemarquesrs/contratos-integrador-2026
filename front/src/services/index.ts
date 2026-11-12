/**
 * Ponto ÚNICO de seleção entre API mock e API real.
 * Nenhum componente deve importar implementação concreta diretamente.
 *
 * Modo controlado por VITE_API_MODE ("mock" | "real").
 * Padrão: "mock" quando a variável estiver ausente.
 */
import type { Services } from "@/services/contracts";

import { contratosMock } from "@/services/mock/contratos.mock";
import { documentosMock } from "@/services/mock/documentos.mock";
import { clientesMock } from "@/services/mock/clientes.mock";
import { historicoMock } from "@/services/mock/historico.mock";
import { modelosMock } from "@/services/mock/modelos.mock";

import { contratosReal } from "@/services/real/contratos.real";
import { documentosReal } from "@/services/real/documentos.real";
import { clientesReal } from "@/services/real/clientes.real";
import { historicoReal } from "@/services/real/historico.real";
import { modelosReal } from "@/services/real/modelos.real";

export const API_MODE: "mock" | "real" =
  import.meta.env.VITE_API_MODE === "real" ? "real" : "mock";

const services: Services =
  API_MODE === "real"
    ? {
        contratos: contratosReal,
        documentos: documentosReal,
        clientes: clientesReal,
        historico: historicoReal,
        modelos: modelosReal,
      }
    : {
        contratos: contratosMock,
        documentos: documentosMock,
        clientes: clientesMock,
        historico: historicoMock,
        modelos: modelosMock,
      };

export default services;

export const { contratos, documentos, clientes, historico, modelos } = services;
