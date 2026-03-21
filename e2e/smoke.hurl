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

# 2. Workspace Validation
# Fetch the default workspace the server automatically created on signup
GET http://localhost:8080/api/v1/workspaces
Authorization: Bearer {{token}}
HTTP 200
[Captures]
workspace_id: jsonpath "$.[0].id"

POST http://localhost:8080/api/v1/workspaces/{{workspace_id}}/invitations
Authorization: Bearer {{token}}
Content-Type: application/json
{
  "email": "vader@empire.gov"
}
HTTP 200

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

# 4. Create a Board
POST http://localhost:8080/api/v1/boards
Authorization: Bearer {{token}}
X-Workspace-Id: {{workspace_id}}
Content-Type: application/json
{
  "title": "Smoke Test Board"
}
HTTP 200
[Captures]
board_id: jsonpath "$.id"

# 5. Create Columns
POST http://localhost:8080/api/v1/boards/{{board_id}}/columns
Authorization: Bearer {{token}}
X-Workspace-Id: {{workspace_id}}
Content-Type: application/json
{
  "title": "To Do",
  "orderIndex": 0
}
HTTP 200
[Captures]
col_todo_id: jsonpath "$.id"

POST http://localhost:8080/api/v1/boards/{{board_id}}/columns
Authorization: Bearer {{token}}
X-Workspace-Id: {{workspace_id}}
Content-Type: application/json
{
  "title": "Done",
  "orderIndex": 1
}
HTTP 200
[Captures]
col_done_id: jsonpath "$.id"

# 6. Create Task inside the Board
POST http://localhost:8080/api/v1/boards/{{board_id}}/tasks
Authorization: Bearer {{token}}
X-Workspace-Id: {{workspace_id}}
Content-Type: application/json
{
  "title": "Defeat the Rebels",
  "status": "{{col_todo_id}}"
}
HTTP 200

# 7. Query Board and verify Task existence
# Hurl will inherently retry these asserts if CQRS projection takes a few milliseconds
GET http://localhost:8080/api/v1/boards/{{board_id}}
Authorization: Bearer {{token}}
X-Workspace-Id: {{workspace_id}}
HTTP 200
[Asserts]
jsonpath "$.columns" count > 0
jsonpath "$.columns[0].tasks" count > 0
jsonpath "$.columns[0].tasks[0].title" == "Defeat the Rebels"

# 8. Create a Second Workspace
POST http://localhost:8080/api/v1/workspaces
Authorization: Bearer {{token}}
Content-Type: application/json
{
  "name": "Second Workspace"
}
HTTP 201
[Captures]
workspace_2_id: jsonpath "$.id"

# 9. Set the second workspace as active
POST http://localhost:8080/api/v1/workspaces/{{workspace_2_id}}/active
Authorization: Bearer {{token}}
HTTP 200