# Login & JWT Authentication

The feeder-server uses **stateless JWT authentication**. To access any protected endpoint you must first obtain a token via the login endpoint, then include it in every subsequent request.

---

## Flow Overview

```
1. POST /feeder-service/api/auth/login  →  { token: "eyJ..." }
2. Use the token in the Authorization header for all further requests
```

```
Client                          Server
  |                               |
  |  POST /auth/login             |
  |  { username, password }  ---> |
  |                               |  Validate credentials (BCrypt)
  |                               |  Build JWT with authorities
  |  <--- 200 { "token": "..." }  |
  |                               |
  |  GET /devices                 |
  |  Authorization: Bearer <token>|
  |  ---------------------------->'|
  |                               |  Validate JWT signature & expiry
  |                               |  Set SecurityContext
  |  <--- 200 [ ... ]             |
```

---

## Login Endpoint

| Property | Value |
|----------|-------|
| **URL**  | `POST /feeder-service/api/auth/login` |
| **Auth** | None (public) |
| **Content-Type** | `application/json` |

### Request body

```json
{
  "username": "string",
  "password": "string"
}
```

### Success response — `200 OK`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGljZSIsImF1dGhvcml0aWVzIjpbIlJPTEVfQURNSU4iXSwiaWF0IjoxNzQyMDAwMDAwLCJleHAiOjE3NDIwODY0MDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
}
```

### Error responses

| Status | Reason |
|--------|--------|
| `401 Unauthorized` | Wrong username or password |
| `403 Forbidden`    | Account is blocked |

---

## Using the Token

Include the token as a **Bearer** token in the `Authorization` header on every protected request:

```
Authorization: Bearer <token>
```

The token is valid for **24 hours** (`app.jwt.expiration-ms=86400000`).

---

## Examples

### cURL

```bash
# 1. Login and capture the token
TOKEN=$(curl -s -X POST http://localhost:8080/feeder-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice", "password": "secret"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 2. Use the token
curl -X GET http://localhost:8080/feeder-service/api/devices \
  -H "Authorization: Bearer $TOKEN"
```

### HTTPie

```bash
# 1. Login
http POST :8080/feeder-service/api/auth/login username=alice password=secret

# 2. Use the token
http GET :8080/feeder-service/api/devices \
  "Authorization: Bearer eyJhbGci..."
```

### Postman

1. Send a `POST` to `http://localhost:8080/feeder-service/api/auth/login` with body:
   ```json
   { "username": "alice", "password": "secret" }
   ```
2. Copy the `token` value from the response.
3. In subsequent requests open the **Authorization** tab, choose **Bearer Token** and paste the token.

### JavaScript (fetch)

```js
// 1. Login
const { token } = await fetch('/feeder-service/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'alice', password: 'secret' }),
}).then(res => res.json());

// 2. Use the token
const devices = await fetch('/feeder-service/api/devices', {
  headers: { Authorization: `Bearer ${token}` },
}).then(res => res.json());
```

---

## JWT Token Structure

The token is a signed **HS256** JWT. Decoded payload example:

```json
{
  "sub": "alice",
  "userId": 42,
  "authorities": ["ROLE_ADMIN", "DEVICE_READ", "DEVICE_WRITE"],
  "iat": 1742000000,
  "exp": 1742086400
}
```

| Claim | Description |
|-------|-------------|
| `sub` | Username |
| `userId` | Database ID of the authenticated user |
| `authorities` | Roles and permissions granted to the user |
| `iat` | Issued-at timestamp (Unix epoch) |
| `exp` | Expiry timestamp (Unix epoch) |

---

## Password Hashing

Passwords stored in the database must be **BCrypt-hashed**. To generate a hash for a new user:

```java
System.out.println(new BCryptPasswordEncoder().encode("yourpassword"));
```

Or using the Spring CLI / a quick test, or any online BCrypt tool.  
**Never store plain-text passwords.**

