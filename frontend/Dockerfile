FROM --platform=$BUILDPLATFORM node:22-alpine AS build
WORKDIR /app

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source
COPY . .

ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ── Runtime: nginx on latest Alpine patch ────────────────────────────────────
# nginx:stable-alpine tracks the latest stable nginx on the latest Alpine
# minor. The `apk upgrade` ensures all OS packages (openssl, musl, libexpat,
# libpng, libxml2, zlib, nghttp2, c-ares) are at their newest patched version
# at image build time, which eliminates the class of Trivy HIGH/CRITICAL CVEs
# that come from stale Alpine OS packages in a pinned base tag.
FROM nginx:stable-alpine

RUN apk upgrade --no-cache

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s CMD wget -qO- http://127.0.0.1/ || exit 1
