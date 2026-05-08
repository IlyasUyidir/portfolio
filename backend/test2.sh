TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test2@test.com","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo "Token: $TOKEN"

curl -s -X POST http://localhost:8080/api/v1/budgets \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"categoryId":1,"budgetYear":2026,"budgetMonth":5,"limitAmount":100000}' 
