# PrintDoc backend quick guide

See the root `README.md` for the full repository audit, architecture tree, API reference, security boundaries, production notes, and frontend integration details.

## Run

```powershell
$env:JWT_SECRET = 'generate-a-random-string-of-at-least-32-characters'
cmd /c mvnw.cmd test
cmd /c mvnw.cmd spring-boot:run
```

Endpoints: `http://localhost:8080/api/v1`; OpenAPI UI `/swagger-ui.html`; health `/actuator/health`.

## Registration

`POST /api/v1/auth/shop/register` creates a shop plus an owner account and returns a JWT. It is gated by `printdoc.auth.registration-enabled` (`ALLOW_PUBLIC_REGISTRATION`): enabled by default on the `dev` profile, disabled in `prod` unless you opt in.

```powershell
$env:ALLOW_PUBLIC_REGISTRATION = 'true'   # only where self-signup is acceptable
```

Body: `{ "shopName", "ownerName", "email", "password", "shopCode"? }`. `shopCode` is optional; when omitted the server derives a unique one. Duplicate email/shop code → `409`, invalid input → `400`, disabled → `403`.

This endpoint has no email verification, CAPTCHA, or password reset, so keep it disabled for public deployments. For a private account, set `BOOTSTRAP_ADMIN_ENABLED=true` with `BOOTSTRAP_ADMIN_EMAIL` and a new `BOOTSTRAP_ADMIN_PASSWORD` of at least 12 characters. Never reuse upstream demo credentials.
