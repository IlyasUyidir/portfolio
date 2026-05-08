# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test2@test.com","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# Create Budget for category 13 (Alimentation for user 3)
echo -e "\nCreate Budget:"
curl -s -X POST http://localhost:8080/api/v1/budgets \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"categoryId":13,"budgetYear":2026,"budgetMonth":5,"limitAmount":100000}' 

# Create Transaction
echo -e "\n\nCreate Transaction:"
curl -s -X POST http://localhost:8080/api/v1/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Groceries","amount":85000,"type":"DEPENSE","categoryId":13,"txDate":"2026-05-10"}'

# Check Progress
echo -e "\n\nCheck Progress:"
# We need the ID of the created budget. It should be 1 since this is the first budget created.
curl -s http://localhost:8080/api/v1/budgets/1/progress \
  -H "Authorization: Bearer $TOKEN" 
