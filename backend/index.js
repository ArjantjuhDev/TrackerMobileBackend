const express = require('express');
const { ethers } = require('ethers');
const cors = require('cors');

// Replace with your deployed contract address and ABI
require('dotenv').config();
const CONTRACT_ADDRESS = process.env.CONTRACT_ADDRESS || 'YOUR_CONTRACT_ADDRESS';
const CONTRACT_ABI = require('./DevicePairingABI.json');

const provider = new ethers.JsonRpcProvider(process.env.RPC_URL || 'https://rpc-mumbai.maticvigil.com'); // Polygon Mumbai testnet
const wallet = new ethers.Wallet(process.env.PRIVATE_KEY, provider);
const contract = new ethers.Contract(CONTRACT_ADDRESS, CONTRACT_ABI, wallet);

const app = express();
app.use(cors());
app.use(express.json());

// Register device endpoint
app.post('/api/register_device', async (req, res) => {
  const { code } = req.body;
  try {
    const tx = await contract.registerDevice(code);
    await tx.wait();
    res.json({ success: true });
  } catch (err) {
    res.json({ success: false, error: err.message });
  }
});

// Pair device endpoint
app.post('/api/pair_device', async (req, res) => {
  const { code } = req.body;
  try {
    const tx = await contract.pairDevice(code);
    await tx.wait();
    const paired = await contract.isPaired(code);
    res.json({ success: paired });
  } catch (err) {
    res.json({ success: false, error: err.message });
  }
});

// Check pairing status
app.get('/api/is_paired/:code', async (req, res) => {
  const code = req.params.code;
  try {
    const paired = await contract.isPaired(code);
    res.json({ paired });
  } catch (err) {
    res.json({ error: err.message });
  }
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, () => {
  console.log(`Blockchain backend listening on port ${PORT}`);
});
