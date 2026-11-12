/**
 * The fake mode permits demo transitions; real mode follows the operations
 * currently exposed by the API.
 */
import { StatusContrato } from "@/types/contratos";

const TRANSICOES_REAIS: Partial<Record<StatusContrato, StatusContrato[]>> = {
  [StatusContrato.EM_APROVACAO]: [StatusContrato.AGUARDANDO_ASSINATURA],
  [StatusContrato.AGUARDANDO_ASSINATURA]: [StatusContrato.ATIVO],
};

/**
 * Returns whether a transition is available in the selected mode.
 */
export function canTransition(
  de: StatusContrato,
  para: StatusContrato,
  real = false,
): boolean {
  if (real) return TRANSICOES_REAIS[de]?.includes(para) ?? false;
  return true;
}

/**
 * Lista de status disponíveis para seleção ao alterar o status de um contrato.
 * Exclui o status atual (não faz sentido alterar para o mesmo).
 */
export function getAvailableStatuses(
  current: StatusContrato,
  real = false,
): StatusContrato[] {
  if (real) return TRANSICOES_REAIS[current] ?? [];
  return Object.values(StatusContrato).filter((s) => s !== current);
}
