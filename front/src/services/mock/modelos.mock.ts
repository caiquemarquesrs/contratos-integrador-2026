import type { ModelosService } from "@/services/contracts";
import { db } from "@/services/mock/db";
import { delay } from "@/services/mock/helpers";

export const modelosMock: ModelosService = {
  async listar() {
    return delay(db().modelos);
  },
};
