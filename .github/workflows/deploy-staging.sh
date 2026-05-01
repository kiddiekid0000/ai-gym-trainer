#!/bin/bash
sudo cp -r /etc/letsencrypt/archive/* /home/azureuser/app-staging/certbot/conf/archive/
sudo cp -r /etc/letsencrypt/live/* /home/azureuser/app-staging/certbot/conf/live/
sudo chown -R azureuser:azureuser /home/azureuser/app-staging/certbot
docker rm -f gym-trainer-frontend-staging 2>/dev/null || true
set -e

echo "🚀 Starting staging deployment..."

# Clean old directory (but keep certbot folder)
find ~/app-staging -mindepth 1 -maxdepth 1 ! -name 'certbot' -exec rm -rf {} + 2>/dev/null
mkdir -p ~/app-staging
cd ~/app-staging

# Create nginx-staging.conf (SSL version)
cat > nginx-staging.conf << 'NGINXEOF'
server {
    listen 80;
    server_name gymaitrainerstaging.duckdns.org;
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name gymaitrainerstaging.duckdns.org;
    ssl_certificate /etc/letsencrypt/live/gymaitrainerstaging.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/gymaitrainerstaging.duckdns.org/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    location / {
        root /usr/share/nginx/html;
        index index.html index.htm;
        try_files $uri $uri/ /index.html;
    }
    location /api {
        proxy_pass http://gym-trainer-backend-staging:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
NGINXEOF

# Create docker-compose.staging.yml (correct version)
cat > docker-compose.staging.yml << 'DOCKEREOF'
version: "3.8"
services:
  postgres-staging:
    image: postgres:15-alpine
    container_name: gym-trainer-postgres-staging
    environment:
      POSTGRES_DB: aigym_staging
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data_staging:/var/lib/postgresql/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  backend-staging:
    image: ${DOCKER_USERNAME}/gymapp-backend:staging
    container_name: gym-trainer-backend-staging
    environment:
      - SPRING_PROFILES_ACTIVE=docker,staging
      - DB_URL=jdbc:postgresql://postgres-staging:5432/aigym_staging
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - JWT_SECRET=${JWT_SECRET}
      - ADMIN_EMAIL=${ADMIN_EMAIL}
      - ADMIN_PASSWORD=${ADMIN_PASSWORD}
      - MAIL_HOST=${MAIL_HOST}
      - MAIL_PORT=${MAIL_PORT}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
    depends_on:
      postgres-staging:
        condition: service_healthy
    restart: unless-stopped

  frontend-staging:
    image: ${DOCKER_USERNAME}/gymapp-frontend:staging
    container_name: gym-trainer-frontend-staging
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx-staging.conf:/etc/nginx/conf.d/default.conf
      - ./certbot/conf/live:/etc/letsencrypt/live
      - ./certbot/conf/archive:/etc/letsencrypt/archive
    depends_on:
      - backend-staging
    restart: unless-stopped

volumes:
  postgres_data_staging:
DOCKEREOF

# Create .env file
cat > .env << 'ENVEOF'
DOCKER_USERNAME=${DOCKER_USERNAME}
DB_URL=jdbc:postgresql://postgres-staging:5432/aigym_staging
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
ADMIN_EMAIL=${ADMIN_EMAIL}
ADMIN_PASSWORD=${ADMIN_PASSWORD}
MAIL_HOST=${MAIL_HOST}
MAIL_PORT=${MAIL_PORT}
MAIL_USERNAME=${MAIL_USERNAME}
MAIL_PASSWORD=${MAIL_PASSWORD}
ENVEOF

# Load environment variables
set -a; source .env; set +a

# Deploy with docker compose
echo "📦 Pulling images..."
docker compose -f docker-compose.staging.yml pull

echo "🐳 Starting containers..."
docker compose -f docker-compose.staging.yml up -d

echo "⏳ Waiting for containers to be ready..."
sleep 30

# Check deployment status
if docker ps | grep -q staging; then
  echo "✅ Staging Deployment Successful!"
  docker ps | grep staging
else
  echo "❌ Staging Deployment Failed!"
  docker compose -f docker-compose.staging.yml logs --tail 50
  exit 1
fi