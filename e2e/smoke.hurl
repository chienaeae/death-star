# ---------------------------------------------------------------------------
# Death Star - E2E Smoke Test (Powered by Hurl)
# ---------------------------------------------------------------------------

# 1. Validate GET route and database connectivity
# Retries are allowed because Java and the database may take time to start
GET http://localhost:8080/api/v1/todos
HTTP 200
[Asserts]
# Validate that the response is a JSON array
header "Content-Type" contains "application/json"
jsonpath "$" count >= 0


# 2. Validate POST route (Mutation) and state persistence
POST http://localhost:8080/api/v1/todos
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