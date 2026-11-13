/**
 * Rotas internas do módulo, servido sob /modulos/contratos/ (Contrato §12.8).
 *
 * A rota "interna" é sempre relativa ao urlFrontend e sem o código do módulo — "/contratos/9f1c",
 * nunca "/modulos/contratos/contratos/9f1c" — que é o formato de `modulo:navegar` (§12.2), dos
 * itens de submenu do registro (modulos/contratos.json) e da busca global (§8.6).
 *
 * Embutido, quem guarda o histórico é a casca: o módulo só substitui a própria URL (replaceState)
 * e avisa a casca com `modulo:navegar`, para não duplicar entradas no botão voltar.
 * Em modo direto, o histórico é do próprio navegador (pushState).
 */
import { useSyncExternalStore } from 'react'
import { avisarNavegacao, estaEmbutido } from './sessao'

/** "/modulos/contratos" — o mesmo `base` do vite.config.ts. */
const BASE = import.meta.env.BASE_URL.replace(/\/+$/, '')
export const ROTA_INICIAL = '/contratos'

const ouvintes = new Set<() => void>()
let rota = lerRotaDaUrl()
let ultimaAvisada: string | null = null

function normalizar(caminho: string): string {
  const limpo = `/${caminho}`.replace(/\/{2,}/g, '/').replace(/\/+$/, '')
  return limpo === '' ? '/' : limpo
}

function lerRotaDaUrl(): string {
  let caminho = window.location.pathname
  if (caminho === BASE || caminho.startsWith(`${BASE}/`)) caminho = caminho.slice(BASE.length)
  return normalizar(caminho)
}

function publicar() {
  if (rota !== ultimaAvisada) {
    ultimaAvisada = rota
    avisarNavegacao(rota)
  }
  ouvintes.forEach((ouvinte) => ouvinte())
}

/** Navega para uma rota interna ("/contratos/123"). */
export function navegar(destino: string, opcoes: { substituir?: boolean } = {}) {
  const nova = normalizar(destino)
  if (nova === rota) return
  rota = nova
  const url = `${BASE}${nova}${window.location.search}`
  if (opcoes.substituir || estaEmbutido()) window.history.replaceState(null, '', url)
  else window.history.pushState(null, '', url)
  publicar()
}

/** Chamada uma vez, antes do render: corrige "/" para a rota inicial e acompanha voltar/avançar. */
export function iniciarRotas() {
  window.addEventListener('popstate', () => {
    rota = lerRotaDaUrl()
    publicar()
  })
  if (rota === '/') navegar(ROTA_INICIAL, { substituir: true })
  else publicar()
}

function assinar(ouvinte: () => void) {
  ouvintes.add(ouvinte)
  return () => {
    ouvintes.delete(ouvinte)
  }
}

export function useRota(): string {
  return useSyncExternalStore(assinar, () => rota)
}
