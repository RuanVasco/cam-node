FROM node:24.22.3-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.31.8-alpine

# Runs as the image's nginx user (UID 101): the stock default site is dropped and conf.d is made writable so the
# entrypoint can render the template with API_UPSTREAM.
RUN rm /etc/nginx/conf.d/default.conf && chown 101:101 /etc/nginx/conf.d
COPY nginx.conf /etc/nginx/nginx.conf
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/cam-node-frontend/browser /usr/share/nginx/html

ENV API_UPSTREAM=backend:8080
USER 101
EXPOSE 8080
