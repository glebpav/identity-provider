# Identity Provider

Spring Boot identity-provider for user authentication and service-to-service JWT issuing.

## What It Does

- Registers users.
- Issues user `accessToken` and `refreshToken`.
- Rotates refresh tokens.
- Stores refresh tokens only as SHA-256 hashes.
- Issues service access tokens for internal microservices.
- Exposes JWKS so Python services can validate access tokens without calling an introspection endpoint.
- Lets admins read users and change user roles.

## User Model

User ids are UUIDs.

User fields:

- `id`
- `email`
- `firstName`
- `lastName`
- `role`

Roles are stored in PostgreSQL as strings and exposed in API responses as enum values:

- `TRADER`
- `POSITIONER`
- `ADMIN`
- `AUDITOR`

Default role on registration: `AUDITOR`.

## Token Claims

User access token contains:

- `sub`: user UUID
- `user_id`: user UUID
- `email`
- `first_name`
- `last_name`
- `role`
- `token_type=user`
- `iss`
- `iat`
- `exp`
- `jti`

Service access token contains:

- `sub`: client id
- `client_id`
- `service`
- `scope`
- `token_type=service`
- `iss`
- `iat`
- `exp`
- `jti`

## Local Run

```bash
cd ../docker
cp .sample.env .env
docker compose up --build -d
```

Current local port in `.env`:

```text
http://localhost:8083
```

Swagger:

```text
http://localhost:8083/swagger-ui.html
```

JWKS:

```text
http://localhost:8083/.well-known/jwks.json
```

Health:

```text
http://localhost:8083/actuator/health
```

Identity admin frontend:

```text
http://localhost:3000
```

The frontend is a separate TypeScript microservice in `../identity-admin-frontend`.
It is included in `../docker/docker-compose.yml` as `identity-admin-frontend`
and proxies browser API calls to the Identity Provider container.

## JWT RSA Keys

For local development, leave both values empty and the app will generate an in-memory key pair:

```env
JWT_PRIVATE_KEY=
JWT_PUBLIC_KEY=
```

For a server deployment, configure both keys. Generate the correct formats:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl rsa -pubout -in jwt-private.pem -out jwt-public.pem
```

`jwt-private.pem` must start with:

```text
-----BEGIN PRIVATE KEY-----
```

`jwt-public.pem` must start with:

```text
-----BEGIN PUBLIC KEY-----
```

In `.env` or Docker env vars, keep each PEM on one line with escaped newlines:

```bash
JWT_PRIVATE_KEY="$(awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' jwt-private.pem)"
JWT_PUBLIC_KEY="$(awk 'NF {sub(/\r/, ""); printf "%s\\n",$0;}' jwt-public.pem)"
```

Do not use `BEGIN RSA PRIVATE KEY` or `BEGIN RSA PUBLIC KEY` for these env values.

## Bootstrap Admin

Local `.env` creates the first admin if it does not exist:

```env
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=change-this-local-admin-password
BOOTSTRAP_ADMIN_FIRST_NAME=Admin
BOOTSTRAP_ADMIN_LAST_NAME=User
```

## Internal Service Clients

Service clients are configured from env, not from DB:

```env
FX_DEALING_CLIENT_ID=fx-dealing
FX_DEALING_CLIENT_SECRET=fx-dealing-local-secret
FX_DEALING_SCOPES=fx-dealing:read,fx-dealing:write

POSITIONER_CLIENT_ID=positioner
POSITIONER_CLIENT_SECRET=positioner-local-secret
POSITIONER_SCOPES=positioner:read,positioner:write

REPORTS_CLIENT_ID=reports
REPORTS_CLIENT_SECRET=reports-local-secret
REPORTS_SCOPES=reports:read,reports:write
```

## API

Public:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/oauth/token`
- `GET /.well-known/jwks.json`

Authenticated user:

- `GET /api/v1/users/me`

Admin:

- `GET /api/v1/users`
- `GET /api/v1/users/{id}`
- `PATCH /api/v1/users/{id}/role`

## Examples

Register:

```bash
curl -X POST http://localhost:8083/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "user@example.com",
    "firstName": "Ivan",
    "lastName": "Ivanov",
    "password": "very-secure-password"
  }'
```

Login:

```bash
curl -X POST http://localhost:8083/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@example.com",
    "password": "change-this-local-admin-password"
  }'
```

Refresh:

```bash
curl -X POST http://localhost:8083/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken": "<refresh-token>"}'
```

Change role as admin:

```bash
curl -X PATCH http://localhost:8083/api/v1/users/<user-id>/role \
  -H 'Authorization: Bearer <admin-access-token>' \
  -H 'Content-Type: application/json' \
  -d '{"role": "TRADER"}'
```

Service token:

```bash
curl -X POST http://localhost:8083/api/v1/oauth/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials' \
  -d 'client_id=fx-dealing' \
  -d 'client_secret=fx-dealing-local-secret' \
  -d 'scope=fx-dealing:read'
```

## Python Validation

See [docs/python-token-validation.md](docs/python-token-validation.md).

## Build

```bash
./gradlew test
docker compose build
```
