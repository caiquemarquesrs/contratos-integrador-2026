/**
 * Utilitários compartilhados pelos serviços mock:
 * - latência artificial
 * - simulação de falhas (flag global em tempo de execução)
 * - IDs
 */
import type { ApiError } from "@/types/contratos";

export const MOCK_LATENCY = 350; // ms

let forceErrors = false;
export function setForceErrors(value: boolean): void {
  forceErrors = value;
}
export function getForceErrors(): boolean {
  return forceErrors;
}

export async function delay<T>(value: T, ms = MOCK_LATENCY): Promise<T> {
  await new Promise((r) => setTimeout(r, ms));
  if (forceErrors) {
    const err: ApiError = {
      code: "SIMULATED_FAILURE",
      message: "Falha simulada para teste de estados de erro.",
    };
    throw err;
  }
  return value;
}

export function generateId(prefix: string): string {
  return `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 7)}`;
}

export function nowISO(): string {
  return new Date().toISOString();
}
