/**
 * Conversa com a casca por postMessage (Contrato §12.1 e §12.2).
 *
 * Embutido no iframe: a sessão chega da casca e o token vive só em memória.
 * Aberto direto no navegador (desenvolvimento): o login é feito aqui, com um usuário de teste.
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

type MensagemDaCasca =
  | { tipo: 'plataforma:sessao'; token: string; tenantId: string; usuario: Usuario; tema: Tema }
  | { tipo: 'plataforma:token'; token: string }
  | { tipo: 'plataforma:tema'; tema: Tema }

type MensagemParaCasca =
  | { tipo: 'modulo:pronto' }
  | { tipo: 'modulo:altura'; altura: number }
  | { tipo: 'modulo:navegar'; rota: string }
  | { tipo: 'modulo:token-expirado' }
  | { tipo: 'modulo:notificar'; nivel: 'sucesso' | 'erro' | 'info'; texto: string }

const embutido = window.parent !== window
let sessao: Sessao | null = null
const ouvintes = new Set<(sessao: Sessao | null) => void>()

function aplicarTema(tema: Tema) {
  document.documentElement.dataset.tema = tema
}

function atualizar(nova: Sessao | null) {
  sessao = nova
  if (nova) aplicarTema(nova.tema)
  ouvintes.forEach((ouvinte) => ouvinte(sessao))
}

function paraCasca(mensagem: MensagemParaCasca) {
  // Mesma origem: a casca e os módulos são servidos pelo mesmo gateway (§12.8)
  if (embutido) window.parent.postMessage(mensagem, window.location.origin)
}

export function iniciarSessao() {
  if (!embutido) return

  window.addEventListener('message', (evento: MessageEvent<MensagemDaCasca>) => {
    // Só aceita mensagem da própria plataforma, vinda da janela que embute o módulo
    if (evento.origin !== window.location.origin || evento.source !== window.parent) return
    const mensagem = evento.data
    switch (mensagem?.tipo) {
      case 'plataforma:sessao':
        atualizar({ token: mensagem.token, tenantId: mensagem.tenantId, usuario: mensagem.usuario, tema: mensagem.tema })
        break
      case 'plataforma:token':
        if (sessao) atualizar({ ...sessao, token: mensagem.token })
        break
      case 'plataforma:tema':
        if (sessao) atualizar({ ...sessao, tema: mensagem.tema })
        else aplicarTema(mensagem.tema)
        break
    }
  })

  // A casca ajusta a altura do iframe para não haver rolagem dupla
  new ResizeObserver(() => {
    paraCasca({ tipo: 'modulo:altura', altura: document.documentElement.scrollHeight })
  }).observe(document.body)

  paraCasca({ tipo: 'modulo:pronto' })
}

export const estaEmbutido = () => embutido
export const sessaoAtual = () => sessao

export function aoMudarSessao(ouvinte: (sessao: Sessao | null) => void) {
  ouvintes.add(ouvinte)
  return () => {
    ouvintes.delete(ouvinte)
  }
}

/** Modo direto: guarda a sessão obtida no login de desenvolvimento. Nunca em localStorage. */
export function definirSessaoDireta(nova: Sessao) {
  atualizar(nova)
}

export function avisarTokenExpirado() {
  paraCasca({ tipo: 'modulo:token-expirado' })
}

/** Reflete a rota interna na URL da casca, para recarregar e voltar funcionarem (RF38). */
export function avisarNavegacao(rota: string) {
  paraCasca({ tipo: 'modulo:navegar', rota })
}

export function notificarCasca(nivel: 'sucesso' | 'erro' | 'info', texto: string) {
  paraCasca({ tipo: 'modulo:notificar', nivel, texto })
}
