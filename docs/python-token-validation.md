# Python Token Validation

Python services should validate JWT access tokens locally. Do not call Identity Provider for every request.

Install:

```bash
pip install PyJWT cryptography requests
```

## Validate With JWKS

This version fetches the public keys from Identity Provider. Cache the `PyJWKClient` instance in your app process.

```python
import jwt
from jwt import PyJWKClient

ISSUER = "http://localhost:8083"
JWKS_URL = f"{ISSUER}/.well-known/jwks.json"

jwks_client = PyJWKClient(JWKS_URL)


def validate_access_token(token: str) -> dict:
    signing_key = jwks_client.get_signing_key_from_jwt(token)
    claims = jwt.decode(
        token,
        signing_key.key,
        algorithms=["RS256"],
        issuer=ISSUER,
        options={"require": ["sub", "iss", "exp", "iat", "jti", "token_type"]},
    )
    return claims


def validate_user_token(token: str) -> dict:
    claims = validate_access_token(token)
    if claims.get("token_type") != "user":
        raise PermissionError("user token required")
    return {
        "user_id": claims["user_id"],
        "email": claims["email"],
        "first_name": claims["first_name"],
        "last_name": claims["last_name"],
        "role": claims["role"],
        "expires_at": claims["exp"],
    }


def validate_service_token(token: str, required_scope: str | None = None) -> dict:
    claims = validate_access_token(token)
    if claims.get("token_type") != "service":
        raise PermissionError("service token required")

    scopes = set(claims.get("scope", []))
    if required_scope and required_scope not in scopes:
        raise PermissionError("required scope is missing")

    return {
        "client_id": claims["client_id"],
        "service": claims["service"],
        "scopes": scopes,
        "expires_at": claims["exp"],
    }
```

## Fully Offline Validation

For production, prefer passing `JWT_PUBLIC_KEY` to each microservice through env or secrets. Then validation does not require any network call.

```python
import os
import jwt

ISSUER = os.environ["JWT_ISSUER"]
PUBLIC_KEY = os.environ["JWT_PUBLIC_KEY"].replace("\\n", "\n")


def validate_access_token_offline(token: str) -> dict:
    return jwt.decode(
        token,
        PUBLIC_KEY,
        algorithms=["RS256"],
        issuer=ISSUER,
        options={"require": ["sub", "iss", "exp", "iat", "jti", "token_type"]},
    )
```
