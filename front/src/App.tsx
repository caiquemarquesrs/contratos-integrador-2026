import { useState, type ReactNode } from "react";
import { FileText, ChevronLeft, ChevronRight } from "lucide-react";
import { ContratosDocumentosPage } from "@/features/contratos-documentos/ContratosDocumentosPage";

type Page = "contratos";

interface NavItem {
  id: Page;
  label: string;
  icon: ReactNode;
  group: string;
}

const NAV: NavItem[] = [
  { id: "contratos", label: "Contratos e Documentos", icon: <FileText className="h-4 w-4 shrink-0" />, group: "Financeiro" },
];

const pageMap: Record<Page, ReactNode> = {
  contratos: <ContratosDocumentosPage />,
};

const PAGE_BREADCRUMB: Record<Page, string> = {
  contratos: "Financeiro / Contratos e Documentos",
};

export default function App() {
  const [page, setPage] = useState<Page>("contratos");
  const [collapsed, setCollapsed] = useState(false);

  // Agrupa itens por grupo
  const groups = NAV.reduce<Record<string, NavItem[]>>((acc, item) => {
    (acc[item.group] ??= []).push(item);
    return acc;
  }, {});

  return (
    <div className="flex h-full w-full overflow-hidden bg-gray-50">
      {/* Sidebar */}
      <aside
        className={`flex flex-col border-r border-brand-900 bg-brand-950 text-white transition-all duration-200 ${
          collapsed ? "w-16" : "w-60"
        }`}
      >
        <div className="flex h-14 items-center gap-2 px-4">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-700 font-bold">
            C
          </div>
          {!collapsed && (
            <div className="min-w-0">
              <p className="truncate text-sm font-bold">Centinela</p>
              <p className="truncate text-[10px] text-brand-300">Soluções</p>
            </div>
          )}
        </div>

        <nav className="flex-1 overflow-y-auto px-2 py-2">
          {Object.entries(groups).map(([group, items]) => (
            <div key={group} className="mb-4">
              {!collapsed && (
                <p className="px-3 pb-1 text-[10px] font-semibold uppercase tracking-wider text-brand-400">
                  {group}
                </p>
              )}
              {items.map((item) => {
                const active = page === item.id;
                return (
                  <button
                    key={item.id}
                    onClick={() => setPage(item.id)}
                    title={collapsed ? item.label : undefined}
                    className={`mb-0.5 flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm transition-all duration-200 ${
                      active
                        ? "bg-brand-700 text-white shadow-[inset_0_0_0_1px_rgba(255,255,255,0.08)]"
                        : "text-slate-300 hover:bg-brand-800 hover:text-white"
                    }`}
                  >
                    {item.icon}
                    {!collapsed && <span className="truncate">{item.label}</span>}
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        <button
          onClick={() => setCollapsed((c) => !c)}
          className="flex items-center gap-2 border-t border-brand-900 px-4 py-3 text-brand-300 hover:bg-brand-900 hover:text-white"
        >
          {collapsed ? (
            <ChevronRight className="h-4 w-4" />
          ) : (
            <>
              <ChevronLeft className="h-4 w-4" />
              <span className="text-xs">Recolher</span>
            </>
          )}
        </button>
      </aside>

      {/* Conteúdo principal */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Header global */}
        <header className="flex h-14 shrink-0 items-center border-b border-brand-900 bg-brand-950 px-5 text-white">
          <div className="min-w-0">
            <p className="truncate text-xs text-brand-300">{PAGE_BREADCRUMB[page]}</p>
          </div>
        </header>

        {/* Página */}
        <main className="min-h-0 flex-1 overflow-hidden">{pageMap[page]}</main>
      </div>
    </div>
  );
}
