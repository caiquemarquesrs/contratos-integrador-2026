# Contratos Integrador 2026

Repositório com duas aplicações independentes:

- `api/`: API Java com Spring Boot 3.5 e Maven.
- `front/`: interface React, TypeScript e Vite.
- `.github/workflows/ci.yml`: valida cada aplicação separadamente e publica suas imagens Docker em pushes na branch `main`.

## Pré-requisitos

- Java 21 e Maven 3.9 ou superior.
- Node.js 22 e npm.
- Docker, necessário para construir imagens e para os testes de integração da API com Testcontainers.

## Front-end

```powershell
cd front
npm ci
npm run typecheck
npm run build
npm run dev
```

O front é um módulo da plataforma: em produção ele roda dentro do iframe da casca, servido pelo gateway em `/modulos/contratos/`, e recebe sessão, token e tema por `postMessage`. Ele não tem menu, barra superior nem login próprios. Aberto direto (`http://localhost:5173/modulos/contratos/`), pede um usuário de teste do identity de desenvolvimento e encaminha `/api` ao gateway (`VITE_GATEWAY`). Detalhes em `front/docs/INTEGRACAO-API.md`, seção “Integração com a casca”.

O servidor de desenvolvimento do Vite informa a URL local ao iniciar. `npm run build` também executa a verificação TypeScript antes de gerar os arquivos em `dist/`.

O `package.json` ainda não define um script de testes automatizados para o front; a validação disponível é `npm run typecheck` e `npm run build`.

## API

```powershell
cd api
mvn -B verify
```

O `verify` compila e executa os testes Maven. Os testes de integração que usam Testcontainers precisam de um Docker disponível. A API usa a porta `8084` por padrão; conexões e credenciais dos serviços externos são configuradas por variáveis de ambiente conforme `api/src/main/resources/application.yml`.

## Google reCAPTCHA v2

No [Google reCAPTCHA Admin Console](https://www.google.com/recaptcha/admin/create), crie uma chave do tipo **reCAPTCHA v2 > Checkbox "Não sou um robô"** e inclua `localhost` na lista de domínios permitidos para desenvolvimento. O Google fornece duas chaves:

- Configure a **Site Key** em `front/.env` como `VITE_RECAPTCHA_SITE_KEY=`. Ela é pública e fica disponível ao navegador.
- Configure a **Secret Key** somente no ambiente de execução da API como `RECAPTCHA_SECRET_KEY`. Nunca a coloque no front, no Git, em imagens Docker ou em logs.

O arquivo `front/.env.example` contém as variáveis públicas necessárias e não inclui chaves reais. Crie uma cópia local chamada `.env` e preencha a Site Key obtida no Google; arquivos `.env` são ignorados pelo Git. Para executar a API, injete `RECAPTCHA_SECRET_KEY` no ambiente do processo/container. Sem essa variável, o endpoint de criação falha fechado e não persiste contratos.

A API exige o token no cabeçalho `X-Recaptcha-Token` e valida-o no endpoint oficial do Google antes de consultar o CRM ou persistir o contrato. A criação real pelo formulário continua bloqueada até que os campos da tela sejam alinhados ao DTO da API, conforme a auditoria em `front/docs/INTEGRACAO-API.md`.

## Docker

Construa cada imagem a partir do contexto da própria aplicação:

```powershell
docker build -f api/Dockerfile -t contratos-api api
docker build -f front/Dockerfile -t contratos-front front
```

A imagem da API expõe a porta `8084`; a imagem do front expõe `3004`. Os arquivos de build e dependências locais são excluídos dos contextos por seus respectivos `.dockerignore`. Não passe arquivos `.env` como parte do contexto nem os grave nas imagens.

Para iniciar o front:

```powershell
docker run --rm -p 3004:3004 contratos-front
```

Para iniciar a API, configure as variáveis exigidas pelos serviços externos conforme `api/src/main/resources/application.yml` e forneça-as em tempo de execução (por exemplo, com `--env-file`); não inclua credenciais na imagem.

## Integração contínua

O workflow executa `mvn -B verify` na API e `npm ci` seguido de `npm run build` no front em jobs independentes, em pushes para `main` e pull requests. As imagens correspondentes são publicadas somente em pushes, após a validação do respectivo job.