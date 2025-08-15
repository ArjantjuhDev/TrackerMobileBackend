const nodemailer = require('nodemailer');
const express = require('express');
const router = express.Router();

// In-memory store (voor demo, gebruik Redis/db voor productie)
const codes = {};

// Strato SMTP config
const transporter = nodemailer.createTransport({
  host: 'smtp.strato.com',
  port: 465,
  secure: true,
  auth: {
    user: 'arjan@radioredarrow.nl',
    pass: 'Looren1992!'
  }
});

function generateCode() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

router.post('/', async (req, res) => {
  const { email } = req.body;
  if (!email) return res.status(400).json({ success: false, error: 'Email required' });
  const code = generateCode();
  codes[email] = code;
  try {
    await transporter.sendMail({
      from: 'arjan@radioredarrow.nl',
      to: email,
      subject: 'Jouw verificatiecode',
      text: `Je verificatiecode is: ${code}`
    });
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// Endpoint om code te valideren
router.post('/verify', (req, res) => {
  const { email, code } = req.body;
  if (codes[email] === code) {
    delete codes[email];
    res.json({ success: true });
  } else {
    res.json({ success: false, error: 'Ongeldige code' });
  }
});

module.exports = router;
