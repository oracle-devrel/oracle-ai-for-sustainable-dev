import React from 'react';
import {gql, useQuery} from "@apollo/client";

const ACCOUNT_INFORMATION_QUERY = gql`
  {findAllAccounts {
      id
      balance
      description
      bank {
          id
          name
      }
  }}
`;

function AccountInformation() {
    const {loading, error, data} = useQuery(ACCOUNT_INFORMATION_QUERY);
    if (loading) return <p>Loading...</p>;
    if (error) return <p>Error :(</p>;
    return data.findAllAccounts.map( (account:any) =>
        <div key={account.id}>
            bank id: {account.bank.id} , bank name: {account.bank.name} , account id: {account.id}, account description: {account.description}, account balance: {account.balance}
        </div>
    );
}

function App() {
    return (
        <div>
            <h2>Bank Account(s) Information...</h2>
            <AccountInformation/>
        </div>
    );
}

export default App;
