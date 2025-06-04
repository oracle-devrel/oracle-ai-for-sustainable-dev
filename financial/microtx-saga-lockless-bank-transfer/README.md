See blog at https://dzone.com/articles/developing-saga-participant-code-for-compensating for details




TODO: these do not populate customer_id, only account_id and account_balance
curl -X POST "http://account.financial:8080/api/v1/createAccountWith1000Balance" -H "Content-Type: application/json" -d '{ "AccountCustomerId": "testcustomerid1" }'
curl -X POST "http://account.financial:8080/api/v1/createAccountWith1000Balance" -H "Content-Type: application/json" -d '{ "AccountCustomerId": "testcustomerid2" }'



