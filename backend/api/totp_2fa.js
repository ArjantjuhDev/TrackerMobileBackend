const express = require('express');
const router = express.Router();
const speakeasy = require('speakeasy');
const qrcode = require('qrcode');

// In-memory store for demo (use DB for production)
const userSecrets = {};

// Generate TOTP secret and QR code
router.post('/register', async (req, res) => {
  const { username } = req.body;
  if (!username) return res.status(400).json({ success: false, error: 'Username required' });
  const secret = speakeasy.generateSecret({ name: `TrackerMobile (${username})` });
  userSecrets[username] = secret.base32;
  const otpauthUrl = secret.otpauth_url;
  try {
    const qr = await qrcode.toDataURL(otpauthUrl);
    res.json({ success: true, secret: secret.base32, qr });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Verify TOTP code
router.post('/verify', (req, res) => {
  const { username, token } = req.body;
  const secret = userSecrets[username];
  if (!secret) return res.status(400).json({ success: false, error: 'No secret registered for user' });
  const verified = speakeasy.totp.verify({
    secret,
    encoding: 'base32',
    token,
    window: 1
  });
  if (verified) {
    // Issue JWT token for admin only
    let tokenJwt = null;
    if (username === 'admin') {
      const jwt = require('jsonwebtoken');
      const JWT_SECRET = process.env.JWT_SECRET || 'tracker2025supersecret';
      tokenJwt = jwt.sign({ username: 'admin', role: 'admin' }, JWT_SECRET, { expiresIn: '2h' });
    }
    res.json({ success: true, token: tokenJwt });
  } else {
    res.json({ success: false, error: 'Invalid code' });
  }
});

module.exports = router;
