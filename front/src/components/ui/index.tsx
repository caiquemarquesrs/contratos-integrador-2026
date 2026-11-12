import {
  forwardRef,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
  type ReactNode,
  useEffect,
} from "react";
import { Search, ChevronLeft, ChevronRight, X } from "lucide-react";

// ============================================================================
// Button
// ============================================================================
type ButtonVariant = "primary" | "secondary" | "outline" | "ghost" | "danger";
type ButtonSize = "sm" | "md" | "lg";

const BUTTON_VARIANTS: Record<ButtonVariant, string> = {
  primary:
    "bg-brand-800 text-white shadow-sm hover:bg-brand-700 focus-visible:outline-brand-700",
  secondary:
    "bg-slate-100 text-slate-700 shadow-sm hover:bg-slate-200 focus-visible:outline-slate-400",
  outline:
    "border border-slate-300 bg-white text-slate-700 shadow-sm hover:bg-slate-50 focus-visible:outline-slate-400",
  ghost: "text-slate-600 hover:bg-slate-100 hover:text-slate-900",
  danger:
    "bg-red-600 text-white shadow-sm hover:bg-red-700 focus-visible:outline-red-500",
};

const BUTTON_SIZES: Record<ButtonSize, string> = {
  sm: "h-8 px-3 text-xs gap-1.5",
  md: "h-9 px-4 text-sm gap-2",
  lg: "h-11 px-6 text-base gap-2",
};

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
}

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  className = "",
  children,
  disabled,
  ...props
}: ButtonProps) {
  return (
    <button
      className={`inline-flex items-center justify-center rounded-xl font-medium transition-all duration-200 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${BUTTON_VARIANTS[variant]} ${BUTTON_SIZES[size]} ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading && (
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
      )}
      {children}
    </button>
  );
}

// ============================================================================
// Input
// ============================================================================
export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className = "", ...props }, ref) => (
    <input
      ref={ref}
      className={`h-10 w-full rounded-xl border border-slate-300 bg-white px-3.5 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 transition-colors focus:border-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-100 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-500 ${className}`}
      {...props}
    />
  )
);
Input.displayName = "Input";

// ============================================================================
// Select
// ============================================================================
export const Select = forwardRef<
  HTMLSelectElement,
  SelectHTMLAttributes<HTMLSelectElement>
>(({ className = "", children, ...props }, ref) => (
  <select
    ref={ref}
    className={`h-10 w-full appearance-none rounded-xl border border-slate-300 bg-white bg-[url('data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2216%22%20height%3D%2216%22%20viewBox%3D%220%200%2024%2024%22%20fill%3D%22none%22%20stroke%3D%22%236b7280%22%20stroke-width%3D%222%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%3E%3Cpolyline%20points%3D%226%209%2012%2015%2018%209%22%2F%3E%3C%2Fsvg%3E')] bg-[length:16px] bg-[right_0.75rem_center] bg-no-repeat px-3.5 pr-9 text-sm text-slate-900 shadow-sm transition-colors focus:border-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-100 disabled:cursor-not-allowed disabled:bg-slate-50 ${className}`}
    {...props}
  >
    {children}
  </select>
));
Select.displayName = "Select";

// ============================================================================
// Textarea
// ============================================================================
export const Textarea = forwardRef<
  HTMLTextAreaElement,
  TextareaHTMLAttributes<HTMLTextAreaElement>
>(({ className = "", ...props }, ref) => (
  <textarea
    ref={ref}
    className={`w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm text-slate-900 shadow-sm placeholder:text-slate-400 transition-colors focus:border-brand-700 focus:outline-none focus:ring-2 focus:ring-brand-100 disabled:cursor-not-allowed disabled:bg-slate-50 ${className}`}
    {...props}
  />
));
Textarea.displayName = "Textarea";

// ============================================================================
// Badge
// ============================================================================
type BadgeVariant =
  | "green"
  | "red"
  | "blue"
  | "indigo"
  | "yellow"
  | "gray"
  | "purple"
  | "orange";

const BADGE_VARIANTS: Record<BadgeVariant, string> = {
  green: "bg-brand-100 text-brand-800",
  red: "bg-red-50 text-red-700",
  blue: "bg-blue-50 text-blue-700",
  indigo: "bg-indigo-50 text-indigo-700",
  yellow: "bg-yellow-50 text-yellow-800",
  gray: "bg-gray-100 text-gray-600",
  purple: "bg-purple-50 text-purple-700",
  orange: "bg-orange-50 text-orange-700",
};

export function Badge({
  variant = "gray",
  className = "",
  children,
}: {
  variant?: BadgeVariant;
  className?: string;
  children: ReactNode;
}) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-[11px] font-semibold ${BADGE_VARIANTS[variant]} ${className}`}
    >
      {children}
    </span>
  );
}

/**
 * Mapeamento automático de status para Badge (CRM).
 * Cada módulo pode estender este mapa.
 */
export function statusBadge(str: string): ReactNode {
  const s = str.toLowerCase();
  let variant: BadgeVariant = "gray";
  if (["ativo", "adimplente", "concluído", "concluido", "renovado"].includes(s))
    variant = "green";
  else if (["inativo", "cancelado", "inadimplente", "finalizado"].includes(s))
    variant = "red";
  else if (["prospect", "lead", "em andamento", "em_aprovacao", "em aprovação"].includes(s))
    variant = "blue";
  else if (["pendente", "aguardando assinatura", "aguardando_assinatura"].includes(s))
    variant = "indigo";
  else if (["rascunho", "em elaboração"].includes(s)) variant = "yellow";
  else if (["suspenso", "a vencer", "a_vencer"].includes(s)) variant = "orange";
  return <Badge variant={variant}>{str}</Badge>;
}

// ============================================================================
// Modal
// ============================================================================
type ModalSize = "sm" | "md" | "lg" | "xl" | "full";

const MODAL_SIZES: Record<ModalSize, string> = {
  sm: "max-w-md",
  md: "max-w-lg",
  lg: "max-w-2xl",
  xl: "max-w-4xl",
  full: "max-w-6xl",
};

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  subtitle?: string;
  size?: ModalSize;
  footer?: ReactNode;
  noPad?: boolean;
  children: ReactNode;
}

export function Modal({
  open,
  onClose,
  title,
  subtitle,
  size = "md",
  footer,
  noPad = false,
  children,
}: ModalProps) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = prev;
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
        aria-hidden
      />
      <div
        role="dialog"
        aria-modal="true"
        className={`relative z-10 flex max-h-[90vh] w-full ${MODAL_SIZES[size]} flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.18)]`}
      >
        {!noPad && (title || subtitle) && (
          <div className="flex items-start justify-between border-b border-slate-200 bg-slate-50/80 px-5 py-4">
            <div>
              {title && <h2 className="text-lg font-bold text-slate-900">{title}</h2>}
              {subtitle && (
                <p className="mt-0.5 text-sm text-slate-500">{subtitle}</p>
              )}
            </div>
            <button
              onClick={onClose}
              className="rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-200 hover:text-slate-700"
              aria-label="Fechar"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        )}
        <div className={`flex-1 overflow-y-auto ${noPad ? "" : "px-5 py-4"}`}>
          {children}
        </div>
        {footer && (
          <div className="flex justify-end gap-2 border-t border-gray-100 px-6 py-4">
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}

// ============================================================================
// FormSection + FormField
// ============================================================================
export function FormSection({
  number,
  title,
  children,
}: {
  number?: number;
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="mb-5 rounded-2xl border border-slate-200 bg-slate-50/60 p-4 shadow-sm">
      <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
        {number != null && (
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-800">
            {number}
          </span>
        )}
        {title}
      </h3>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">{children}</div>
    </section>
  );
}

export function FormField({
  label,
  required,
  colSpan,
  error,
  hint,
  children,
}: {
  label: string;
  required?: boolean;
  colSpan?: 1 | 2;
  error?: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <div className={colSpan === 2 ? "md:col-span-2" : ""}>
      <label className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
        {required && <span className="ml-0.5 text-red-500">*</span>}
      </label>
      {children}
      {hint && !error && <p className="mt-1 text-xs text-slate-400">{hint}</p>}
      {error && <p className="mt-1 text-xs text-red-600">{error}</p>}
    </div>
  );
}

// ============================================================================
// SearchBar
// ============================================================================
export function SearchBar({
  value,
  onChange,
  placeholder = "Buscar...",
  className = "",
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
}) {
  return (
    <div className={`relative ${className}`}>
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
      <Input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="pl-9"
      />
    </div>
  );
}

// ============================================================================
// ViewToggle
// ============================================================================
export function ViewToggle({
  view,
  onChange,
  options,
}: {
  view: string;
  onChange: (view: string) => void;
  options: { value: string; label: string }[];
}) {
  return (
    <div className="inline-flex rounded-lg border border-gray-200 bg-gray-50 p-0.5">
      {options.map((opt) => (
        <button
          key={opt.value}
          onClick={() => onChange(opt.value)}
          className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
            view === opt.value
              ? "bg-brand-950 text-white shadow-sm"
              : "text-gray-500 hover:bg-gray-100"
          }`}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}

// ============================================================================
// Pagination
// ============================================================================
export function Pagination({
  page,
  pageSize,
  total,
  onPageChange,
}: {
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number) => void;
}) {
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  if (total === 0) return null;
  return (
    <div className="flex items-center justify-between border-t border-slate-200 bg-white px-5 py-3">
      <p className="text-xs text-slate-500">
        Exibindo {Math.min((page - 1) * pageSize + 1, total)}–
        {Math.min(page * pageSize, total)} de {total}
      </p>
      <div className="flex items-center gap-1.5">
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 1}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
          aria-label="Página anterior"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <span className="rounded-lg border border-slate-200 bg-slate-50 px-2.5 py-1 text-sm font-medium text-slate-700">
          {page} / {totalPages}
        </span>
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-700 disabled:cursor-not-allowed disabled:opacity-40"
          aria-label="Próxima página"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

// ============================================================================
// FilterBar
// ============================================================================
export function FilterBar({ children, className = "" }: { children: ReactNode; className?: string }) {
  return (
    <div className={`flex flex-wrap items-center gap-3 border-b border-slate-200 bg-white px-5 py-3 ${className}`}>
      {children}
    </div>
  );
}

// ============================================================================
// PageHeader
// ============================================================================
export function PageHeader({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children?: ReactNode;
}) {
  return (
    <div className="flex shrink-0 items-center justify-between gap-4 bg-brand-950 px-5 py-4 text-white shadow-[inset_0_-1px_0_rgba(148,163,184,0.15)]">
      <div className="min-w-0">
        <h1 className="text-lg font-bold tracking-tight text-white">{title}</h1>
        {description && (
          <p className="mt-0.5 text-xs text-brand-200">{description}</p>
        )}
      </div>
      {children && <div className="flex shrink-0 items-center gap-2">{children}</div>}
    </div>
  );
}

// ============================================================================
// DetailField + DetailSection
// ============================================================================
export function DetailField({
  label,
  value,
  colSpan,
  children,
}: {
  label: string;
  value?: ReactNode;
  colSpan?: 1 | 2;
  children?: ReactNode;
}) {
  return (
    <div className={colSpan === 2 ? "md:col-span-2" : ""}>
      <dt className="text-[11px] font-semibold uppercase tracking-[0.08em] text-slate-500">
        {label}
      </dt>
      <dd className="mt-1.5 text-sm text-slate-800">{children ?? value ?? "—"}</dd>
    </div>
  );
}

export function DetailSection({
  number,
  title,
  children,
}: {
  number?: number;
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="mb-5 rounded-2xl border border-slate-200 bg-slate-50/60 p-4 shadow-sm">
      <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
        {number != null && (
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-800">
            {number}
          </span>
        )}
        {title}
      </h3>
      <dl className="grid grid-cols-1 gap-x-6 gap-y-4 md:grid-cols-2">
        {children}
      </dl>
    </section>
  );
}

// ============================================================================
// Stepper
// ============================================================================
export function Stepper({
  steps,
  current,
}: {
  steps: string[];
  current: number;
}) {
  return (
    <ol className="flex items-center gap-2">
      {steps.map((step, i) => (
        <li key={step} className="flex flex-1 items-center gap-2">
          <div className="flex items-center gap-2">
            <span
              className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold ${
                i < current
                  ? "bg-brand-700 text-white"
                  : i === current
                    ? "bg-brand-950 text-white"
                    : "bg-gray-200 text-gray-500"
              }`}
            >
              {i + 1}
            </span>
            <span
              className={`text-xs font-medium ${
                i <= current ? "text-gray-900" : "text-gray-400"
              }`}
            >
              {step}
            </span>
          </div>
          {i < steps.length - 1 && (
            <span
              className={`h-0.5 flex-1 ${
                i < current ? "bg-brand-700" : "bg-gray-200"
              }`}
            />
          )}
        </li>
      ))}
    </ol>
  );
}
