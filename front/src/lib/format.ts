/**
 * Utilitários de formatação centralizados.
 * Usam Intl — sem bibliotecas novas.
 */

const BRL_FORMATTER = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  minimumFractionDigits: 2,
});

const DATE_FORMATTER = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

const DATETIME_FORMATTER = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
  hour: "2-digit",
  minute: "2-digit",
});

export function formatBRL(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return "—";
  return BRL_FORMATTER.format(value);
}

export function formatDate(value: string | Date | null | undefined): string {
  if (!value) return "—";
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return DATE_FORMATTER.format(date);
}

export function formatDateTime(value: string | Date | null | undefined): string {
  if (!value) return "—";
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  return DATETIME_FORMATTER.format(date);
}

/** Remove tudo que não é dígito. */
function onlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

/** Máscara de CPF: 000.000.000-00 */
export function maskCPF(value: string): string {
  const digits = onlyDigits(value).slice(0, 11);
  return digits
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}

/** Máscara de CNPJ: 00.000.000/0000-00 */
export function maskCNPJ(value: string): string {
  const digits = onlyDigits(value).slice(0, 14);
  return digits
    .replace(/(\d{2})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1/$2")
    .replace(/(\d{4})(\d{1,2})$/, "$1-$2");
}

/** Detecta se é CPF (11 dígitos) ou CNPJ (14) e aplica a máscara correta. */
export function maskDocumento(value: string): string {
  const digits = onlyDigits(value);
  return digits.length <= 11 ? maskCPF(digits) : maskCNPJ(digits);
}

export function formatCPF(value: string | null | undefined): string {
  if (!value) return "—";
  return maskCPF(value);
}

export function formatCNPJ(value: string | null | undefined): string {
  if (!value) return "—";
  return maskCNPJ(value);
}
