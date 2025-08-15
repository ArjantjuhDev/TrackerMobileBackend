const express = require('express');
const { ethers } = require('ethers');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');

// Replace with your deployed contract address and ABI
require('dotenv').config();
let contract = null;
let blockchainEnabled = false;
try {
  const CONTRACT_ADDRESS = process.env.CONTRACT_ADDRESS || 'YOUR_CONTRACT_ADDRESS';
  const CONTRACT_ABI = require('./DevicePairingABI.json');
  const provider = new ethers.JsonRpcProvider(process.env.RPC_URL || 'https://rpc-mumbai.maticvigil.com');
  if (process.env.PRIVATE_KEY && /^0x[0-9a-fA-F]{64}$/.test(process.env.PRIVATE_KEY)) {
    const wallet = new ethers.Wallet(process.env.PRIVATE_KEY, provider);
    contract = new ethers.Contract(CONTRACT_ADDRESS, CONTRACT_ABI, wallet);
    blockchainEnabled = true;
  } else {
    console.warn('Blockchain features disabled: missing or invalid PRIVATE_KEY in .env');
  }
} catch (err) {
  console.warn('Blockchain features disabled:', err.message);
}

const app = express();
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST'],
  credentials: true
}));
app.use(express.json());

const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST'],
    credentials: true
  }
});

// Socket.IO events
io.on('connection', (socket) => {
  console.log('Socket.IO: client connected:', socket.id);
  socket.on('join-room', (code) => {
    socket.join(code);
    socket.emit('paired');
    io.to(code).emit('user-joined', socket.id);
  });
  socket.on('request-permissions', () => {
    socket.emit('permissions-accepted');
  });
  socket.on('lock-device', (unlockCode) => {
    io.emit('lock-device', unlockCode);
  });
  socket.on('unlock-device', (code) => {
    io.emit('unlock-device', code);
  });
  socket.on('wipe-device', () => {
    io.emit('wipe-device');
  });
  socket.on('request-screenshot', () => {
    io.emit('request-screenshot');
  });
  socket.on('location-update', (locObj) => {
    io.emit('location-update', locObj);
  });
  socket.on('disconnect', () => {
    console.log('Socket.IO: client disconnected:', socket.id);
  });
});


// Health check endpoint for Render
app.get('/api/check_connection', (req, res) => {
  res.json({ connected: true });
});

// Register device endpoint
app.post('/api/register_device', async (req, res) => {
  if (!blockchainEnabled) return res.json({ success: false, error: 'Blockchain disabled' });
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
  if (!blockchainEnabled) return res.json({ success: false, error: 'Blockchain disabled' });
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
  if (!blockchainEnabled) return res.json({ paired: false, error: 'Blockchain disabled' });
  const code = req.params.code;
  try {
    const paired = await contract.isPaired(code);
    res.json({ paired });
  } catch (err) {
    res.json({ error: err.message });
  }
});

const request2fa = require('./api/request_2fa');
app.use('/api/request_2fa', request2fa);

const adminAccounts = require('./api/admin_accounts');
app.use('/api/admin/accounts', adminAccounts);

const PORT = process.env.PORT || 3001;
server.listen(PORT, () => {
  console.log(`Backend with Socket.IO listening on port ${PORT}`);
});
