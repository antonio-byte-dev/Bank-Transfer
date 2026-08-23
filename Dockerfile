# ================= Backend =================

FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app

# Copy pom first so dependency layer is cached between builds
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS backend
WORKDIR /app

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=backend-build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


# ================= Frontend =================

FROM node:20-alpine AS frontend-build
WORKDIR /app

COPY frontend/transference-frontend/package*.json ./
RUN npm ci

COPY frontend/transference-frontend/. .

# Baked in at build time — must be reachable from the BROWSER (localhost +
# published port), not a Docker-internal hostname like http://backend:8081.
ARG VITE_API_BASE_URL=http://localhost:8081
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

FROM nginx:1.27-alpine AS frontend

COPY --from=frontend-build /app/dist /usr/share/nginx/html

# Vue Router history mode fallback (harmless if you're using hash mode)
RUN echo 'server { \
    listen 80; \
    root /usr/share/nginx/html; \
    location / { \
      try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

EXPOSE 80