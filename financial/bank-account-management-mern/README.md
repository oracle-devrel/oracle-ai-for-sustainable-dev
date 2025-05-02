

npm init -y
npm install express mongoose body-parser cors


node app.js

curl -X POST -H "Content-Type: application/json" -d '{
"account_id": "acc_1001",
"name": "Checking",
"official_name": "Gold Standard 0% Interest Checking",
"type": "depository",
"subtype": "checking",
"mask": "1234",
"available_balance": 1200.50,
"current_balance": 1250.00,
"limit_balance": null,
"verification_status": "verified"
}' http://localhost:5000/api/accounts


curl http://localhost:5000/api/accounts
curl http://localhost:5000/api/accounts/acc_1001


curl -X PUT -H "Content-Type: application/json" -d '{
"name": "Updated Checking Account"
}' http://localhost:5000/api/accounts/acc_1001

curl -X DELETE http://localhost:5000/api/accounts/acc_1001