import { Badge } from "@/components/ui";
import { StatusContrato, STATUS_LABELS } from "@/types/contratos";

/**
 * Mapeamento CENTRALIZADO status → variante de badge.
 * Usado em tabelas, detalhes e formulários. Não duplique esta lógica.
 */
const STATUS_VARIANT: Record<StatusContrato, "green" | "red" | "blue" | "indigo" | "yellow" | "gray" | "purple" | "orange"> = {
  [StatusContrato.RASCUNHO]: "yellow",
  [StatusContrato.EM_APROVACAO]: "blue",
  [StatusContrato.AGUARDANDO_ASSINATURA]: "indigo",
  [StatusContrato.ATIVO]: "green",
  [StatusContrato.A_VENCER]: "orange",
  [StatusContrato.RENOVADO]: "green",
  [StatusContrato.SUSPENSO]: "purple",
  [StatusContrato.CANCELADO]: "red",
  [StatusContrato.FINALIZADO]: "gray",
};

export function ContratoStatusBadge({ status }: { status: StatusContrato }) {
  return <Badge variant={STATUS_VARIANT[status]}>{STATUS_LABELS[status]}</Badge>;
}
