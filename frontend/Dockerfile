# ── Dev Server ──────────────────────────────────
FROM node:22-alpine
WORKDIR /app

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source
COPY . .

EXPOSE 5173

# Run Vite dev server, bind to 0.0.0.0 so Docker can access it
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
