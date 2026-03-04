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

# 1. Validate GET route and database connectivity
# Retries are allowed because Java and the database may take time to start
GET http://localhost:8080/api/v1/todos
Authorization: Bearer {{token}}
HTTP 200
[Asserts]
# Validate that the response is a JSON array
header "Content-Type" contains "application/json"
jsonpath "$" count >= 0


# 2. Validate POST route (Mutation) and state persistence
POST http://localhost:8080/api/v1/todos
Authorization: Bearer {{token}}
Content-Type: application/json
{
  "title": "Automated Smoke Test Objective"
}
HTTP 201
[Asserts]
# Validate that a resource is created and an ID is returned
jsonpath "$.id" exists
jsonpath "$.title" == "Automated Smoke Test Objective"
jsonpath "$.completed" == false