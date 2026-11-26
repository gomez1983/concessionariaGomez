# ==========================================
# Etapa 1: Build da aplicação React com Node 22
# ==========================================
FROM node:22-alpine AS builder
WORKDIR /app

# Cache de dependências npm
COPY package*.json ./
RUN npm ci

# Código e build de produção
COPY . .
RUN npm run build

# ==========================================
# Etapa 2: Nginx sem privilégios de root (SEC-16)
# ==========================================
# O processo roda com o usuário nginx (UID 101) e escuta na porta 8080, porque portas abaixo de
# 1024 exigem root. O docker-compose.yml publica 127.0.0.1:80 -> 8080.
FROM nginxinc/nginx-unprivileged:alpine

# Copiar os arquivos exige root; eles ficam do root e só leitura para o nginx
USER root
RUN rm -rf /usr/share/nginx/html/*
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx/ /etc/nginx/snippets/
USER 101

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
