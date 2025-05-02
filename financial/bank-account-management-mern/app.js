const express = require('express');
const mongoose = require('mongoose');
const bodyParser = require('body-parser');
const cors = require('cors');

// Initialize Express app
const app = express();
const PORT = 5000;

// Middleware
app.use(bodyParser.json());
app.use(cors());

// MongoDB connection
const MONGO_URI = 'mongodb://financial:Welcome12345@IJ1TYZIR3WPWLPE-FINANCIALDB.adb.eu-frankfurt-1.oraclecloudapps.com:27017/financial?authMechanism=PLAIN&authSource=$external&ssl=true&retryWrites=false&loadBalanced=true'; // Replace with your MongoDB URI
mongoose
  .connect(MONGO_URI, { useNewUrlParser: true, useUnifiedTopology: true })
  .then(() => console.log('Connected to MongoDB'))
  .catch((err) => console.error('Error connecting to MongoDB:', err));

// Define the schema
const accountSchema = new mongoose.Schema({
  account_id: { type: String, required: true, unique: true },
  name: { type: String, required: true },
  official_name: { type: String },
  type: { type: String, required: true },
  subtype: { type: String },
  mask: { type: String },
  available_balance: { type: Number },
  current_balance: { type: Number },
  limit_balance: { type: Number },
  verification_status: { type: String },
});

// Create the model
const Account = mongoose.model('Account', accountSchema);

// CRUD Endpoints

// Create an account
app.post('/api/accounts', async (req, res) => {
  try {
    const account = new Account(req.body);
    await account.save();
    res.status(201).json(account);
  } catch (err) {
    console.error('Error creating account:', err);
    res.status(500).json({ error: 'Failed to create account' });
  }
});

// Read all accounts
app.get('/api/accounts', async (req, res) => {
  try {
    const accounts = await Account.find({});
    res.json(accounts);
  } catch (err) {
    console.error('Error fetching accounts:', err);
    res.status(500).json({ error: 'Failed to fetch accounts' });
  }
});

// Read a single account by ID
app.get('/api/accounts/:id', async (req, res) => {
  try {
    const account = await Account.findOne({ account_id: req.params.id });
    if (!account) {
      return res.status(404).json({ error: 'Account not found' });
    }
    res.json(account);
  } catch (err) {
    console.error('Error fetching account:', err);
    res.status(500).json({ error: 'Failed to fetch account' });
  }
});

// Update an account by ID
app.put('/api/accounts/:id', async (req, res) => {
  try {
    const account = await Account.findOneAndUpdate(
      { account_id: req.params.id },
      req.body,
      { new: true, runValidators: true }
    );
    if (!account) {
      return res.status(404).json({ error: 'Account not found' });
    }
    res.json(account);
  } catch (err) {
    console.error('Error updating account:', err);
    res.status(500).json({ error: 'Failed to update account' });
  }
});

// Delete an account by ID
app.delete('/api/accounts/:id', async (req, res) => {
  try {
    const account = await Account.findOneAndDelete({ account_id: req.params.id });
    if (!account) {
      return res.status(404).json({ error: 'Account not found' });
    }
    res.json({ message: 'Account deleted successfully' });
  } catch (err) {
    console.error('Error deleting account:', err);
    res.status(500).json({ error: 'Failed to delete account' });
  }
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
});