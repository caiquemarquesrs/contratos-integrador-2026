import { useEffect, useRef, useState } from "react";
import { Search, X } from "lucide-react";
import { Input } from "@/components/ui";
import { clientes } from "@/services";
import { formatCPF, formatCNPJ } from "@/lib/format";
import { getErrorMessage, type Cliente } from "@/types/contratos";

interface ClienteSelectorProps {
  value: Cliente | null;
  onChange: (cliente: Cliente | null) => void;
  required?: boolean;
}

/**
 * Seletor de cliente inline (combobox pesquisável).
 * Evita empilhamento de modais (o formulário já está em um modal).
 */
export function ClienteSelector({ value, onChange, required }: ClienteSelectorProps) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const [results, setResults] = useState<Cliente[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    let active = true;
    setLoading(true);
    setLoadError("");
    clientes.listar({ search }).then((r) => {
      if (active) {
        setResults(r);
      }
    }).catch((error: unknown) => {
      if (active) {
        setLoadError(getErrorMessage(error, "Erro ao buscar clientes."));
      }
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => {
      active = false;
    };
  }, [open, search]);

  // Fecha ao clicar fora
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open]);

  const docLabel = value
    ? value.tipo === "JURIDICA"
      ? formatCNPJ(value.documento)
      : formatCPF(value.documento)
    : "";

  return (
    <div ref={containerRef} className="relative">
      <div className="flex gap-2">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <Input
            value={open ? search : value?.nome ?? ""}
            onChange={(e) => {
              setSearch(e.target.value);
              setOpen(true);
            }}
            onFocus={() => setOpen(true)}
            placeholder="Buscar cliente por nome ou documento..."
            className="pl-9"
            aria-label="Selecionar cliente"
            required={required}
          />
        </div>
        {value && (
          <button
            type="button"
            onClick={() => {
              onChange(null);
              setSearch("");
            }}
            className="rounded-lg border border-gray-300 px-2 text-gray-500 hover:bg-gray-50"
            aria-label="Limpar cliente"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      {open && (
        <div className="absolute z-30 mt-1 max-h-64 w-full overflow-y-auto rounded-lg border border-gray-200 bg-white shadow-lg">
          {loading && (
            <p className="px-3 py-2 text-sm text-gray-400">Buscando...</p>
          )}
          {!loading && loadError && (
            <p className="px-3 py-2 text-sm text-red-600">{loadError}</p>
          )}
          {!loading && !loadError && results.length === 0 && (
            <p className="px-3 py-2 text-sm text-gray-400">Nenhum cliente encontrado.</p>
          )}
          {!loading &&
            !loadError &&
            results.map((c) => (
              <button
                key={c.id}
                type="button"
                onClick={() => {
                  onChange(c);
                  setSearch("");
                  setOpen(false);
                }}
                className="flex w-full items-center justify-between px-3 py-2 text-left hover:bg-brand-50"
              >
                <span className="text-sm font-medium text-gray-900">{c.nome}</span>
                <span className="text-xs text-gray-400">
                  {c.tipo === "JURIDICA" ? formatCNPJ(c.documento) : formatCPF(c.documento)}
                </span>
              </button>
            ))}
        </div>
      )}

      {value && (
        <p className="mt-1 text-xs text-gray-500">
          {docLabel}
          {value.cidade ? ` · ${value.cidade}/${value.uf}` : ""}
        </p>
      )}
    </div>
  );
}
