# Imagem do front do módulo, servida sob /modulos/contratos/ (Contrato §12.8).
FROM node:22-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
ENV PORTA=3004
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html/modulos/contratos
EXPOSE 3004
