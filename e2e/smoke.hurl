# ---------------------------------------------------------------------------
# Death Star - E2E Smoke Test (Powered by Hurl)
# ---------------------------------------------------------------------------

# 0. Register and Login to get Access Token
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json
{
  "email": "e2e@empire.gov",
  "password": "Password123!"
}
# Ignore 400 if user exists, accept 200 if created

POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json
{
  "email": "e2e@empire.gov",
  "password": "Password123!"
}
HTTP 200
[Captures]
token: jsonpath "$.accessToken"

# 3. Validate Asset Upload Presigned URL generation
POST http://localhost:8080/api/v1/assets/upload-url
Authorization: Bearer {{token}}
Content-Type: application/json
{
  "filename": "hurl-test.png",
  "contentType": "image/png"
}
HTTP 201
[Asserts]
# Validate that a presigned upload URL is returned
jsonpath "$.uploadUrl" exists