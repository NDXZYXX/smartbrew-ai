# ============================================================
# Smart Brew AI — Vue3 前端 Dockerfile (多阶段构建)
# Stage 1: Node 构建
# Stage 2: Nginx 托管静态文件 + 反向代理
# ============================================================

# ---- Stage 1: Build ----
FROM node:18-alpine AS builder

WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm ci --silent

COPY . .
RUN npm run build

# ---- Stage 2: Serve ----
FROM nginx:alpine

# 删除默认配置
RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1
