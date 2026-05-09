TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test2@test.com","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# Get KPIs
echo -e "\nKPIs for 2026-05:"
curl -s http://localhost:8080/api/v1/dashboard/kpis/2026-05 \
  -H "Authorization: Bearer $TOKEN"

# Get Spending
echo -e "\n\nSpending (2026-05):"
curl -s "http://localhost:8080/api/v1/dashboard/spending?month=2026-05" \
  -H "Authorization: Bearer $TOKEN"

# Download CSV
echo -e "\n\nCSV Export:"
curl -s http://localhost:8080/api/v1/export/csv \
  -H "Authorization: Bearer $TOKEN" | head -n 5
