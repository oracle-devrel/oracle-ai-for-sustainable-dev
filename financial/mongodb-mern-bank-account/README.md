

npm init -y
npm install express mongoose body-parser cors


node app.js

curl -X POST -H "Content-Type: application/json" -d '{
"account_id": "1001",
[...]
}' http://localhost:5000/api/accounts


curl http://localhost:5000/api/accounts
curl http://localhost:5000/api/accounts/acc_1001


curl -X PUT -H "Content-Type: application/json" -d '{
"name": "Updated Checking Account"
}' http://localhost:5000/api/accounts/acc_1001

curl -X DELETE http://localhost:5000/api/accounts/acc_1001