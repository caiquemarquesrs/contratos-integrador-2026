/**
 * Conversa com a casca por postMessage (Contrato de Integração §12.1 e §12.2).
 *
 * Este arquivo é o ÚNICO lugar que sabe em qual modo o front está:
 *  - embutido no iframe da casca: a sessão chega por `plataforma:sessao` e o token vive só em memória;
 *  - aberto direto no navegador (desenvolvimento): a sessão vem do login com usuário de teste do
 *    identity de desenvolvimento (Contrato §12 e §14.3), também só em memória.
 *
 * A sessão fica num objeto de módulo, fora dos componentes React: a casca responde ao
 * `modulo:pronto` na hora e a mensagem pode chegar antes do primeiro render (Contrato §12.2).
 * Nada daqui é gravado em localStorage, sessionStorage ou cookie.
 */

export type Tema = 'claro' | 'escuro'

export interface Usuario {
  id: string
  nome: string
  email?: string
  tenantId?: string
}

export interface Sessao {
  token: string
  tenantId: string
  usuario: Usuario
  tema: Tema
}

type NivelNotificacao = 'sucesso' | 'erro' | 'info'

type MensagemParaCasca =
  | { tipo: 'modulo:pronto' }
  | { tipo: 'modulo:altura'; altura: number }
  | { tipo: 'modulo:navegar'; rota: string }
  | { tipo: 'modulo:token-expirado' }
  | { tipo: 'modulo:notificar'; nivel: NivelNotificacao; texto: string }

/** Tempo máximo que uma requisição espera a casca reenviar o token após um 401. */
const ESPERA_TOKEN_MS = 10_000

const embutido = detectarEmbutido()
/** Origem da plataforma: casca, fronts e APIs saem do mesmo gateway (Contrato §12.8). */
const ORIGEM_PLATAFORMA = window.location.origin

let sessao: Sessao | null = null
let iniciado = false
let aguardandoToken: Promise<boolean> | null = null
const ouvintes = new Set<() => void>()

function detectarEmbutido(): boolean {
  try {
    return window.self !== window.top
  } catch {
    // Acesso a window.top bloqueado: só acontece quando há uma janela pai de outra origem.
    return true
  }
}

// ---------------------------------------------------------------- validação das mensagens

function ehTexto(valor: unknown): valor is string {
  return typeof valor === 'string' && valor.trim().length > 0
}

function ehTema(valor: unknown): valor is Tema {
  return valor === 'claro' || valor === 'escuro'
}

function ehRegistro(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null
}

function lerUsuario(valor: unknown): Usuario | null {
  if (!ehRegistro(valor) || !ehTexto(valor.id) || typeof valor.nome !== 'string') return null
  return {
    id: valor.id,
    nome: valor.nome,
    ...(typeof valor.email === 'string' ? { email: valor.email } : {}),
    ...(typeof valor.tenantId === 'string' ? { tenantId: valor.tenantId } : {}),
  }
}

// ---------------------------------------------------------------- estado

function aplicarTema(tema: Tema) {
  document.documentElement.dataset.tema = tema
}

function atualizar(nova: Sessao | null) {
  sessao = nova
  if (nova) aplicarTema(nova.tema)
  ouvintes.forEach((ouvinte) => ouvinte())
}

function paraCasca(mensagem: MensagemParaCasca) {
  // Modo direto: não há casca, e nada é enviado para a própria janela.
  // targetOrigin explícito, nunca '*': só a plataforma (mesma origem) recebe.
  if (embutido) window.parent.postMessage(mensagem, ORIGEM_PLATAFORMA)
}

function tratarMensagem(evento: MessageEvent) {
  // Só aceita mensagem da própria plataforma, vinda da janela que embute o módulo (Contrato §12.2)
  if (evento.origin !== ORIGEM_PLATAFORMA || evento.source !== window.parent) return
  const mensagem: unknown = evento.data
  if (!ehRegistro(mensagem)) return

  switch (mensagem.tipo) {
    case 'plataforma:sessao': {
      const usuario = lerUsuario(mensagem.usuario)
      if (!ehTexto(mensagem.token) || !ehTexto(mensagem.tenantId) || !usuario) {
        console.warn('[contratos] plataforma:sessao ignorada: formato inválido.')
        return
      }
      atualizar({
        token: mensagem.token,
        tenantId: mensagem.tenantId,
        usuario,
        tema: ehTema(mensagem.tema) ? mensagem.tema : 'claro',
      })
      break
    }
    case 'plataforma:token':
      if (!ehTexto(mensagem.token)) return
      // Sem sessão ainda, o token sozinho não basta (faltam tenant e usuário): espera a sessão.
      if (sessao) atualizar({ ...sessao, token: mensagem.token })
      break
    case 'plataforma:tema':
      if (!ehTema(mensagem.tema)) return
      if (sessao) atualizar({ ...sessao, tema: mensagem.tema })
      else aplicarTema(mensagem.tema)
      break
    default:
      // Mensagem desconhecida da própria plataforma: ignorada sem efeito colateral.
      break
  }
}

// ---------------------------------------------------------------- altura do iframe

function observarAltura() {
  let ultima = -1
  let agendado = false

  const medir = () => {
    agendado = false
    // Altura do conteúdo (body), não da janela: documentElement.scrollHeight nunca fica menor
    // que o próprio iframe, e a altura não conseguiria diminuir quando o conteúdo encolhe.
    const altura = Math.ceil(document.body.scrollHeight)
    if (!Number.isFinite(altura) || altura <= 0 || altura === ultima) return
    ultima = altura
    paraCasca({ tipo: 'modulo:altura', altura })
  }
  const agendar = () => {
    if (agendado) return
    agendado = true
    requestAnimationFrame(medir)
  }

  const observador = new ResizeObserver(agendar)
  observador.observe(document.body)
  const raiz = document.getElementById('root')
  if (raiz) observador.observe(raiz)
  window.addEventListener('load', agendar)
}

// ---------------------------------------------------------------- API pública

/**
 * Deve ser chamada antes do primeiro render (main.tsx). Ordem obrigatória do Contrato §12.2:
 * 1) registra o ouvinte de `message`; 2) só então envia `modulo:pronto`.
 */
export function iniciarSessao() {
  if (iniciado || !embutido) return
  iniciado = true

  window.addEventListener('message', tratarMensagem)
  observarAltura()
  paraCasca({ tipo: 'modulo:pronto' })
}

export const estaEmbutido = () => embutido
export const sessaoAtual = () => sessao

/** Assinatura no formato de useSyncExternalStore. */
export function aoMudarSessao(ouvinte: () => void) {
  ouvintes.add(ouvinte)
  return () => {
    ouvintes.delete(ouvinte)
  }
}

/** Modo direto: guarda a sessão obtida no login de desenvolvimento. Nunca em localStorage. */
export function definirSessaoDireta(nova: Sessao | null) {
  if (embutido) return // embutido, só a casca entrega sessão
  atualizar(nova)
}

/**
 * Após um 401: avisa a casca (`modulo:token-expirado`) uma única vez e espera o token renovado.
 * Resolve `true` se um token diferente chegar a tempo. Em modo direto, resolve `false` na hora:
 * não existe casca para renovar, e o módulo nunca renova token sozinho (Contrato §4.3).
 */
export function aguardarTokenRenovado(tokenRecusado: string | undefined): Promise<boolean> {
  if (!embutido) return Promise.resolve(false)
  if (sessao && sessao.token !== tokenRecusado) return Promise.resolve(true)
  if (aguardandoToken) return aguardandoToken

  aguardandoToken = new Promise<boolean>((resolver) => {
    const encerrar = (renovado: boolean) => {
      clearTimeout(limite)
      parar()
      aguardandoToken = null
      resolver(renovado)
    }
    const parar = aoMudarSessao(() => {
      if (sessao && sessao.token !== tokenRecusado) encerrar(true)
    })
    const limite = setTimeout(() => encerrar(false), ESPERA_TOKEN_MS)
    paraCasca({ tipo: 'modulo:token-expirado' })
  })
  return aguardandoToken
}

/** Reflete a rota interna na URL da casca, relativa ao urlFrontend e sem o código (§12.2). */
export function avisarNavegacao(rota: string) {
  paraCasca({ tipo: 'modulo:navegar', rota })
}

export function notificarCasca(nivel: NivelNotificacao, texto: string) {
  paraCasca({ tipo: 'modulo:notificar', nivel, texto })
}
