# Identity Admin Frontend

TypeScript React frontend for managing the Identity Provider service.

## Features

- Login with Identity Provider credentials.
- Refresh and persist user sessions in browser storage.
- View the current user profile.
- Register users.
- List users as an admin.
- Change user roles as an admin.

## Local Development

```bash
npm install
npm run dev
```

The development server runs on:

```text
http://localhost:3000
```

By default, Vite proxies `/identity-api` to:

```text
http://localhost:8080/api/v1
```

Override it when your Identity Provider runs on another port:

```bash
VITE_IDENTITY_API_BASE_URL=http://localhost:8083 npm run dev
```

## Docker

This service is wired into `../docker/docker-compose.yml` as
`identity-admin-frontend` and is exposed on `http://localhost:3000`.
