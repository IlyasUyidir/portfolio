# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@test.com","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# Create Goal
echo -e "\nCreate Goal:"
curl -s -X POST http://localhost:8080/api/v1/goals \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"New Car","targetAmount":1500000,"targetDate":"2026-12-31"}' 

# Test active goal limit constraint for Standard users
echo -e "\n\nTest Goal Limit:"
curl -s -X POST http://localhost:8080/api/v1/goals \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Another Goal","targetAmount":50000,"targetDate":"2026-10-31"}'

# Add Contribution
echo -e "\n\nAdd Contribution:"
curl -s -X POST http://localhost:8080/api/v1/goals/1/contribute \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"amount":500000}'

# Check Progress
echo -e "\n\nCheck Progress:"
curl -s http://localhost:8080/api/v1/goals/1/progress \
  -H "Authorization: Bearer $TOKEN"
