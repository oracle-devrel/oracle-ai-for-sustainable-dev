import React, { useState, useEffect } from 'react';
import axios from 'axios';

const App = () => {
  const [accounts, setAccounts] = useState([]);
  const [formData, setFormData] = useState({
    _id: '',
    accountBalance: '',
    customerId: '',
    accountName: '',
    accountOpenedDate: '',
    accountOtherDetails: '',
    accountType: '',
  });

  // Fetch all accounts
  useEffect(() => {
    fetchAccounts();
  }, []);

  const fetchAccounts = async () => {
    try {
      const response = await axios.get('/api/accounts');
      setAccounts(response.data);
    } catch (err) {
      console.error('Error fetching accounts:', err);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (formData._id) {
        // Update account
        await axios.put(`/api/accounts/${formData._id}`, formData);
      } else {
        // Create account
        await axios.post('/api/accounts', formData);
      }
      fetchAccounts();
      setFormData({
        _id: '',
        accountBalance: '',
        customerId: '',
        accountName: '',
        accountOpenedDate: '',
        accountOtherDetails: '',
        accountType: '',
      });
    } catch (err) {
      console.error('Error saving account:', err);
    }
  };

  const handleDelete = async (id) => {
    try {
      await axios.delete(`/api/accounts/${id}`);
      fetchAccounts();
    } catch (err) {
      console.error('Error deleting account:', err);
    }
  };

  return (
    <div>
      <h1>Accounts Management</h1>
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          name="_id"
          placeholder="ID"
          value={formData._id}
          onChange={handleChange}
        />
        <input
          type="text"
          name="accountName"
          placeholder="Account Name"
          value={formData.accountName}
          onChange={handleChange}
        />
        <input
          type="number"
          name="accountBalance"
          placeholder="Account Balance"
          value={formData.accountBalance}
          onChange={handleChange}
        />
        <button type="submit">{formData._id ? 'Update' : 'Create'}</button>
      </form>
      <ul>
        {accounts.map((account) => (
          <li key={account._id}>
            {account.accountName} - {account.accountBalance}
            <button onClick={() => handleDelete(account._id)}>Delete</button>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default App;