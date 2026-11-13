import { useEffect, useState, useSyncExternalStore, type FormEvent } from "react";
import { AlertCircle, FileText } from "lucide-react";
import { ContratosDocumentosPage } from "@/features/contratos-documentos/ContratosDocumentosPage";
import { Button } from "@/components/ui";
import { API_MODE } from "@/services";
import { entrarComUsuarioDeTeste } from "@/plataforma/api";
import { aoMudarSessao, estaEmbutido, sessaoAtual } from "@/plataforma/sessao";
import { navegar, ROTA_INICIAL, useRota } from "@/plataforma/rotas";

/** Tempo até avisar que a sessão da casca não chegou (a espera continua). */
const AVISO_SEM_SESSAO_MS = 10_000;

/**
 * Raiz do módulo. Não desenha login, menu, barra superior nem sidebar da plataforma:
 * isso é da casca (Contrato §3 e §4.1). Aqui só se decide se já há sessão e qual tela mostrar.
 */
export default function App() {
  // Lê a sessão do objeto externo (plataforma/sessao.ts): nada se perde se ela chegar
  // antes deste componente montar (Contrato §12.2).
  const sessao = useSyncExternalStore(aoMudarSessao, sessaoAtual);

  if (estaEmbutido()) return sessao ? <Rotas /> : <AguardandoSessao />;
  // Modo direto (desenvolvimento). Com a API simulada não há token envolvido.
  if (API_MODE === "mock" || sessao) return <Rotas />;
  return <EntradaDeTeste />;
}

function Rotas() {
  const rota = useRota();
  const partes = rota.split("/").filter(Boolean);

  if (partes[0] === "contratos" && partes.length <= 2) {
    return <ContratosDocumentosPage contratoId={partes[1] ? decodeURIComponent(partes[1]) : undefined} />;
  }
  if (partes.length === 1 && (partes[0] === "modelos" || partes[0] === "documentos")) {
    return (
      <Aviso
        titulo={partes[0] === "modelos" ? "Modelos de Contrato" : "Documentos"}
        texto="Esta seção ainda não tem tela própria neste módulo. Os documentos de cada contrato ficam nos detalhes do contrato."
      />
    );
  }
  return <Aviso titulo="Página não encontrada" texto={`A rota ${rota} não existe no módulo Contratos.`} />;
}

function Aviso({ titulo, texto }: { titulo: string; texto: string }) {
  return (
    <main className="flex flex-col items-center px-6 py-16 text-center">
      <FileText className="h-10 w-10 text-slate-300" />
      <h1 className="mt-3 text-lg font-bold text-slate-900">{titulo}</h1>
      <p className="mt-1 max-w-md text-sm text-slate-500">{texto}</p>
      <Button variant="outline" size="sm" className="mt-4" onClick={() => navegar(ROTA_INICIAL)}>
        Ir para Contratos
      </Button>
    </main>
  );
}

function AguardandoSessao() {
  const [demorou, setDemorou] = useState(false);
  useEffect(() => {
    const limite = setTimeout(() => setDemorou(true), AVISO_SEM_SESSAO_MS);
    return () => clearTimeout(limite);
  }, []);

  return (
    <main className="flex flex-col items-center px-6 py-16 text-center text-sm text-slate-500" aria-live="polite">
      {demorou ? (
        <>
          <AlertCircle className="h-8 w-8 text-amber-500" />
          <p className="mt-2">A sessão da plataforma ainda não chegou. Recarregue a página para tentar de novo.</p>
        </>
      ) : (
        <p>Aguardando a sessão da plataforma…</p>
      )}
    </main>
  );
}

/**
 * Modo direto, só para desenvolvimento: login com um usuário de teste do identity de
 * desenvolvimento (infra-integrador-2026/docs/usuarios-de-teste.md). Nunca aparece no iframe.
 */
function EntradaDeTeste() {
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function entrar(evento: FormEvent) {
    evento.preventDefault();
    setEnviando(true);
    setErro(null);
    try {
      await entrarComUsuarioDeTeste(email, senha);
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Não foi possível entrar.");
    } finally {
      setEnviando(false);
    }
  }

  const campo =
    "h-9 rounded-lg border border-slate-300 bg-white px-3 text-sm outline-none focus:border-brand-700 focus:ring-2 focus:ring-brand-700/20";

  return (
    <main className="mx-auto max-w-sm p-6">
      <p className="font-mono text-xs uppercase tracking-widest text-slate-500">Modo direto · desenvolvimento</p>
      <h1 className="mt-2 text-xl font-bold text-slate-900">Entrar com usuário de teste</h1>
      <p className="mt-1 text-sm text-slate-500">
        Dentro da plataforma este passo não existe: a casca entrega a sessão.
      </p>
      <form onSubmit={entrar} className="mt-6 grid gap-3">
        <label className="grid gap-1 text-sm">
          E-mail
          <input type="email" required autoComplete="username" value={email}
            onChange={(e) => setEmail(e.target.value)} className={campo} />
        </label>
        <label className="grid gap-1 text-sm">
          Senha
          <input type="password" required autoComplete="current-password" value={senha}
            onChange={(e) => setSenha(e.target.value)} className={campo} />
        </label>
        {erro && <p role="alert" className="text-sm text-red-600">{erro}</p>}
        <Button type="submit" disabled={enviando}>
          {enviando ? "Entrando…" : "Entrar"}
        </Button>
      </form>
    </main>
  );
}
