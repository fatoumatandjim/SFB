# =============================================================================
# GesY Frontend - Build multi-stage (Node + Nginx)
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build Angular
# -----------------------------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

# Injected au build: URL de l'API (accessible depuis le navigateur)
ARG API_URL=https://api.sfb-petroleum.net/api

# Cache des dépendances (devDependencies requises pour ng build)
COPY package.json package-lock.json* ./
RUN npm ci

COPY . .

# Injection de l'API URL dans l'environnement (avant build)
RUN sed -i "s|__API_URL__|${API_URL}|g" src/environments/environment.docker.ts

# Build production avec config docker
RUN npm run build -- --configuration=docker

# -----------------------------------------------------------------------------
# Stage 2: Serveur Nginx
# -----------------------------------------------------------------------------
FROM nginx:alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/www /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
