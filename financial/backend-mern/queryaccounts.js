const express = require('express');
const router = express.Router();
const mongoose = require('mongoose');

const accountSchema = new mongoose.Schema({
  account_id: { type: String, required: true, unique: true },
  name: String,
  official_name: String,
  type: String,
  subtype: String,
  mask: String,
  available_balance: Number,
  current_balance: Number,
  limit_balance: Number,
  verification_status: String
}, { collection: 'accounts' });

const Account = mongoose.model('Account', accountSchema);

router.get('/', async (req, res) => {
  try {
    const accounts = await Account.find({});
    res.json(accounts);
  } catch (err) {
    console.error('Error fetching accounts:', err);
    res.status(500).json({ error: 'Failed to fetch accounts' });
  }
});

module.exports = router;