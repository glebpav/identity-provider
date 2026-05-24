# FX Dealing

Root repository for the FX dealing services.

## Services

- `identity-provider` - Spring Boot identity service.
- `identity-admin-frontend` - TypeScript React admin console for the identity service.
- `docker` - Compose and environment files for local runtime.

## Local Run

```bash
cd docker
cp .sample.env .env
docker compose up --build -d
```

Default local URLs:

```text
Identity Provider: http://localhost:8083
Identity Console:  http://localhost:3000
Swagger:           http://localhost:8083/swagger-ui.html
```

## Frontend Development

```bash
cd identity-admin-frontend
npm install
VITE_IDENTITY_API_BASE_URL=http://localhost:8083 npm run dev
```
