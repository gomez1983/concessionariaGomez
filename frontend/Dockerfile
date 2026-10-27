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
# Etapa 2: Servidor Nginx ultra-leve
# ==========================================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Limpa o conteúdo padrão e copia os assets gerados
RUN rm -rf ./*
COPY --from=builder /app/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
