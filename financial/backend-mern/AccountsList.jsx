// src/components/AccountsList.jsx

import React, { useEffect, useState } from 'react';
import axios from 'axios';

const AccountsList = () => {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchAccounts = async () => {
      try {
        const response = await axios.get('http://localhost:5000/api/accounts');
        setAccounts(response.data);
      } catch (err) {
        setError('Failed to fetch accounts.');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    fetchAccounts();
  }, []);

  if (loading) return <p>Loading accounts...</p>;
  if (error) return <p style={{ color: 'red' }}>{error}</p>;

  return (
    <div>
      <h2>Accounts</h2>
      <table border="1" cellPadding="8" style={{ width: '100%', textAlign: 'left' }}>
        <thead>
          <tr>
            <th>Name</th>
            <th>Type</th>
            <th>Subtype</th>
            <th>Mask</th>
            <th>Available</th>
            <th>Current</th>
            <th>Limit</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {accounts.map(account => (
            <tr key={account.account_id}>
              <td>{account.name}</td>
              <td>{account.type}</td>
              <td>{account.subtype}</td>
              <td>{account.mask}</td>
              <td>{account.available_balance ?? '—'}</td>
              <td>{account.current_balance ?? '—'}</td>
              <td>{account.limit_balance ?? '—'}</td>
              <td>{account.verification_status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default AccountsList;