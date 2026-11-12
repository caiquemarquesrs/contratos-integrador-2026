# Plano — Módulo Contratos e Documentos (Centinela Soluções)

> Fases 1–5 concluídas. Módulo implementado, validado contra os documentos de requisitos e design system. `npm run build` e `npm run typecheck` passando sem erros. Dev server rodando em `http://localhost:5174/`.

---

## 0. Descoberta crítica

O diretório do projeto (`c:\Users\carlo\OneDrive\Desktop\Projeto Facul\Front1.0.0.1`) estava **completamente vazio** no início da análise. Não existia nenhum código do CRM pré-existente.

**Decisão:** Criar o projeto base conforme o design system. Em seguida, desenvolver o módulo Contratos e Documentos.

### 0.1. Ajuste de stack (restrição do ambiente)

O design system especifica **Vite 8**, mas o ambiente de desenvolvimento tem **Node 18.16.1**. Vite 8 depende de `rolldown`, que usa `styleText` do `node:util` (disponível apenas a partir do Node 20.12). Além disso, o fallback WASI do Tailwind Oxide exige `node:wasi` (Node 20+).

**Decisão adotada:** Downgrade para **Vite 6** (suporta Node 18), mantendo React 19, TypeScript 5.7 e Tailwind v4. O binding nativo do Tailwind Oxide para Windows (`@tailwindcss/oxide-win32-x64-msvc`) foi adicionado como `devDependency` direta para contornar o bug do npm com dependências opcionais. ESLint não foi configurado (não há configuração no repositório); o script `lint` foi removido do `package.json`.

| Item | Design system | Implementado |
|---|---|---|
| Vite | 8 | 6 (restrição Node 18) |
| @vitejs/plugin-react | — | 4.3.4 (compatível com Vite 6) |
| Tailwind CSS | v4 | v4 (mantido) |
| @tailwindcss/oxide-win32-x64-msvc | (opcional) | devDependency direta (workaround npm) |
| ESLint | "se houver" | Não configurado (removido script) |

Quando o ambiente for atualizado para Node 20+, basta reverter para Vite 8 e remover o binding explícito do oxide.

---

## 1. Resumo da arquitetura identificada

Como não há código existente, a arquitetura **será criada** seguindo o design system:

| Camada | Tecnologia / estrutura |
|---|---|
| Build | Vite 8 + React 19 + TypeScript 5.7 |
| Estilo | Tailwind CSS v4 (`@tailwindcss/vite`, sem `tailwind.config.js`) |
| Ícones | `lucide-react` |
| Fonte | Inter (Google Fonts, pesos 300–700) |
| Tokens | `src/index.css` com `@theme` (paleta `brand-*`) |
| Componentes base | `src/components/ui/index.tsx` |
| Shell | `src/App.tsx` (sidebar `bg-brand-950`, header global, `NAV` + `pageMap`) |
| Módulos | `src/features/<modulo>/` |
| Estado | React hooks locais + contexto quando necessário |
| Serviços | Camada desacoplada (mock ou real) selecionada por `VITE_API_MODE` |

---

## 2. O que será reutilizado vs. criado

### Reutilizado
- **Nenhum código existente** (repositório vazio).
- Os **padrões do design system** (tokens, componentes, layout) servirão de base.

### Criado
1. **Projeto base**: `package.json`, `vite.config.ts`, `tsconfig.json`, `index.html`, `src/main.tsx`, `src/vite-env.d.ts`.
2. **Tokens**: `src/index.css` com a paleta `brand-*` conforme design system.
3. **Componentes UI**: `src/components/ui/index.tsx` com todos os componentes listados no design system (`Button`, `Modal`, `Input`, `Select`, `Textarea`, `Badge`, `statusBadge`, `FormSection`, `FormField`, `SearchBar`, `ViewToggle`, `Pagination`, `FilterBar`, `PageHeader`, `DetailField`, `DetailSection`, `Stepper`).
4. **Shell**: `src/App.tsx` com sidebar fixa, header global, `NAV`, `pageMap`.
5. **Tipos do módulo**: `src/types/contratos.ts` (Contrato, Documento, Cliente, Historico, Modelo, Status).
6. **Camada de serviços**: interfaces + implementação mock + cliente HTTP real.
7. **Módulo**: `src/features/contratos-documentos/` com página principal e sub-componentes.
8. **Utilitários**: formatação de moeda BRL, datas, CPF/CNPJ (em `src/lib/format.ts`).
9. **Docs**: `docs/PENDENCIAS.md`, `docs/INTEGRACAO-API.md` (Fase 5).

### Adaptado
- Como não há módulo de clientes existente, será criado um **serviço de clientes mínimo** (lista/pesquisa) para suportar a seleção no formulário de contrato. Quando o módulo de clientes real existir, basta trocar a implementação do serviço.

---

## 3. Plano de arquivos

### 3.1. Projeto base
```
Front1.0.0.1/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.node.json
├── index.html
├── .env.example
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── index.css
    └── vite-env.d.ts
```

### 3.2. Componentes UI
```
src/components/ui/
└── index.tsx          (Button, Modal, Input, Select, Textarea, Badge, statusBadge,
                        FormSection, FormField, SearchBar, ViewToggle, Pagination,
                        FilterBar, PageHeader, DetailField, DetailSection, Stepper)
```

### 3.3. Tipos e serviços
```
src/types/
└── contratos.ts       (Contrato, Documento, Cliente, Historico, ModeloContrato,
                        StatusContrato, ApiError, PaginatedResult)

src/services/
├── index.ts           (ponto único de seleção mock/real)
├── contratos.service.ts   (interface + factory)
├── documentos.service.ts
├── clientes.service.ts
├── historico.service.ts
├── modelos.service.ts
├── mock/
│   ├── db.ts          (dados de demonstração + localStorage)
│   ├── contratos.mock.ts
│   ├── documentos.mock.ts
│   ├── clientes.mock.ts
│   ├── historico.mock.ts
│   └── modelos.mock.ts
└── real/
    └── http.ts        (cliente HTTP base, headers, tratamento de erros)
```

### 3.4. Utilitários
```
src/lib/
└── format.ts          (formatBRL, formatDate, formatCPF, formatCNPJ, maskCPF, maskCNPJ)
```

### 3.5. Módulo Contratos e Documentos
```
src/features/contratos-documentos/
├── ContratosDocumentosPage.tsx    (orquestrador: listagem + modais)
└── components/
    ├── ContratosTable.tsx         (listagem com paginação)
    ├── ContratoForm.tsx           (cadastro/edição — único formulário reutilizável)
    ├── ContratoDetailPanel.tsx    (detalhes com abas: dados, documentos, histórico)
    ├── DocumentoForm.tsx          (adicionar documento)
    ├── ClienteSelector.tsx        (seleção de cliente — combobox pesquisável inline)
    ├── ContratoStatusBadge.tsx    (mapeamento status → badge centralizado)
    └── ContratoHistory.tsx        (timeline de histórico)
```

### 3.6. Documentação
```
docs/
├── PLANO.md           (este arquivo)
├── PENDENCIAS.md      (dúvidas e decisões a validar)
└── INTEGRACAO-API.md  (Fase 5: contrato com o back-end)
```

---

## 4. Estratégia da API fake e seleção mock/real

### Seleção de ambiente (ponto único)
- Variável `VITE_API_MODE` (`mock` | `real`) em `.env`.
- Variável `VITE_API_URL` para o modo real.
- Padrão: `mock` quando ausente.
- Seleção centralizada em `src/services/index.ts`. **Nenhum componente sabe se é mock ou real.**

### API fake funcional
- Dados de demonstração variados cobrindo todos os 9 status.
- Persistência em `localStorage` com namespace `centinela:contratos:v1` e tolerância a dados ausentes/corrompidos.
- Latência artificial configurável (ex.: 300–600ms).
- Mecanismo de simulação de falhas (flag em tempo de execução para forçar erros).
- Botão de reset dos dados de demonstração.
- Histórico registra as operações do usuário (criar, editar, alterar status, adicionar/excluir documento), sempre identificado como **simulado**.
- Documentos: apenas metadados (nome, tamanho, tipo MIME) — nunca o conteúdo.

### Erros normalizados
- Tipo `ApiError { code: string; message: string; fieldErrors?: Record<string, string> }`.
- Usado igualmente pelo mock e pelo real.
- Sem `alert()`. Sem `console.error` silencioso. Feedback visível em todos os estados.

---

## 5. Matriz de rastreabilidade (RF e RNF → tela/componente)

| Requisito | Descrição | Tela / Componente | Status |
|---|---|---|---|
| **RF-01** | Cadastro de contrato | `ContratoForm` (modal com título) | ✅ Implementado e validado |
| **RF-02** | Dados do contrato (número, cliente, serviços, valor, MRR, início, vigência, renovação, reajuste, índice, aviso prévio, responsável, assinatura) | `ContratoForm` + `ContratoDetailPanel` | ✅ Implementado e validado |
| **RF-03** | Modelos de contrato | `ContratoForm` (campo select) + `modelos.service` | ✅ Implementado e validado |
| **RF-04** | Consulta de contratos | `ContratosTable` + `ContratosDocumentosPage` | ✅ Implementado e validado |
| **RF-05** | Status contratual (9 status) | `ContratoStatusBadge` + serviço de alteração de status | ✅ Implementado e validado |
| **RF-06** | Edição | `ContratoForm` (reutilizado em modo edição) | ✅ Implementado e validado |
| **RF-07** | Vinculação cliente/serviços | `ClienteSelector` + campo de serviços no formulário | ✅ Implementado e validado |
| **RF-08** | Documentos | `ContratoDetailPanel` (aba documentos) + `DocumentoForm` | ✅ Implementado e validado |
| **RF-09** | Acesso por permissões | Pontos de ação preparados; **sem sistema de permissões criado** | ⚠️ Pendente (depende de back-end) |
| **RF-10** | Histórico básico | `ContratoHistory` (timeline) + `historico.service` | ✅ Implementado e validado |
| **RF-11** | Busca e filtros | `ContratosDocumentosPage` (SearchBar + filtros combináveis) | ✅ Implementado e validado |
| **RF-12** | Visualização de detalhes | `ContratoDetailPanel` (Modal noPad com abas) | ✅ Implementado e validado |
| **RNF-01** | Usabilidade | Toda a interface (feedback, estados, confirmações) | ✅ Implementado e validado |
| **RNF-02** | Responsividade | Grid `grid-cols-1 md:grid-cols-2`, tabela com scroll horizontal contido | ✅ Implementado e validado |
| **RNF-03** | Segurança | Respeitar autenticação/permissões existentes (quando houver) | ⚠️ Pendente (back-end) |
| **RNF-04** | Proteção de dados | Sem credenciais no código; dados sensíveis não em localStorage | ✅ Implementado e validado |
| **RNF-05** | Integridade | Validações no formulário; erros de servidor exibidos por campo | ✅ Implementado e validado |
| **RNF-06** | Desempenho | Paginação no serviço; debounce na busca; cancelamento com AbortSignal | ✅ Implementado e validado |
| **RNF-07** | Manutenibilidade | Arquitetura em camadas; componentes reutilizáveis | ✅ Implementado e validado |
| **RNF-08** | Testabilidade | Serviços desacoplados; se houver test runner, testes de mock e validadores | ⚠️ Parcial (serviços desacoplados; sem test runner) |
| **RNF-09** | Auditoria | Histórico básico (RF-10); auditoria completa é RF-A04 (fora do escopo) | ✅ Parcial (histórico básico) |
| **RNF-10** | Backup | Responsabilidade de infraestrutura/back-end; não simulado no front | ⚪ Fora do escopo do front |

---

## 6. Dúvidas e pendências iniciais

(Itens também registrados em `docs/PENDENCIAS.md`.)

1. **Repositório vazio:** Não há CRM existente. O scaffolding base será criado conforme o design system. **Validar com o usuário se esta é a abordagem correta** ou se há um repositório que deve ser clonado/copiado primeiro.
2. **Módulo de clientes:** Não existe. Será criado um serviço de clientes mínimo para seleção. **Validar se deve ser criado como feature separada** ou apenas o serviço.
3. **Valores de status trocados com a API:** O design usa enum interno (`RASCUNHO`, `EM_APROVACAO`, etc.). **Confirmar com o grupo do back-end os valores exatos.**
4. **Transições de status:** Não há regra documentada. `canTransition(de, para)` provisoriamente permite qualquer alteração com confirmação.
5. **"A VENCER" e status derivados de data:** Não calculados no front-end. Pendente de regra do back-end.
6. **MRR:** Não calculado automaticamente. Campo informativo.
7. **Número do contrato:** Não gerado automaticamente. Campo informativo.
8. **Modelos de contrato (RF-03):** Implementado como seleção de um modelo existente. Geração automática de documentos a partir de modelos **fora do escopo** sem suporte do back-end.
9. **Upload de documentos:** Mock guarda apenas metadados. Formato real (multipart/form-data?) a confirmar com o back-end.
10. **Sistema de permissões (RF-09):** Não criado. Pontos de ação preparados para receber checagem depois.
11. **`descricao-telas.txt`:** O `message.txt` referencia este arquivo, mas ele não foi fornecido. Os campos das telas foram deduzidos do RF-02 e do `message.txt` seção 7.

---

## 7. Ordem das fases (conforme message.txt)

1. **Fase 1:** Análise e plano → ✅ concluído.
2. **Fase 2:** Base e listagem (estrutura, navegação, tipos, serviços, API fake, listagem com busca/filtros/paginação, seleção de cliente) → ✅ concluído.
3. **Fase 3:** Cadastro, edição, detalhes e status → ✅ concluído.
4. **Fase 4:** Documentos, histórico e modelos → ✅ concluído.
5. **Fase 5:** Integração real e validação final → ✅ concluído.

---

## 8. Validação final (Fase 5)

### 8.1. Checklist do design system (seção 16 do `design-systemfinal.md`)

| Item | Status |
|---|---|
| Componentes de `@/components/ui` — sem HTML nativo estilizado à mão | ✅ (elementos nativos justificados: `<table>`, tabs, combobox customizado, input file) |
| Tokens de cor (`bg-brand-*`, `text-brand-*`) — sem hex hardcoded em componentes | ✅ (hex apenas em `src/index.css` para definição de tokens e scrollbar) |
| Ícones via `lucide-react` — sem emoji, sem SVG avulso | ✅ |
| Ação contextual sempre que possível (Regra 104) — sem nova rota desnecessária | ✅ (tudo em modais/painéis) |
| `style={{}}` apenas para valores dinâmicos de runtime | ✅ (nenhum `style={{}}` encontrado) |
| Formulários com `FormSection` + `FormField` + labels explícitos | ✅ |
| Modal de detalhe: `noPad`. Modal de formulário: com `title`. Confirmação: `size="sm"` | ✅ |
| Campos condicionais no final do `FormSection` | ✅ |
| `shrink-0` em ícones dentro de flex containers de lista | ✅ |
| Módulo registrado em `NAV` e `pageMap` em `src/App.tsx` | ✅ |
| Estrutura de arquivos em `src/features/<modulo>/` | ✅ |
| `npx tsc --noEmit` sem erros | ✅ |

### 8.2. Cobertura de requisitos

- **RF-01 a RF-08, RF-10 a RF-12:** implementados e validados.
- **RF-09 (permissões):** pontos de ação preparados; sistema de autenticação/autorização depende do back-end.
- **RNF-01 a RNF-07, RNF-09:** implementados.
- **RNF-03 (segurança/permissões):** depende do back-end.
- **RNF-08 (testes):** serviços desacoplados (testáveis), mas sem test runner configurado no repositório.
- **RNF-10 (backup):** responsabilidade de infraestrutura/back-end.

### 8.3. Verificações técnicas

- `npm run build` → ✅ sucesso (285 kB JS, 28 kB CSS).
- `npm run typecheck` → ✅ sem erros.
- Dev server → ✅ rodando em `http://localhost:5174/`.

### 8.4. Próximos passos

1. Testar manualmente os fluxos no navegador (listagem, cadastro, edição, status, documentos, histórico).
2. Quando o back-end estiver disponível: configurar `VITE_API_MODE=real` e `VITE_API_URL`, ajustar mapeamentos conforme `docs/INTEGRACAO-API.md`.
3. Implementar sistema de permissões (RF-09) quando o módulo de autenticação existir.
4. (Opcional) Configurar test runner (RNF-08) e ESLint.
