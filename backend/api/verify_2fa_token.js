// Vercel serverless function: /api/verify_2fa_token
import { authenticator } from 'otplib';

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  try {
    const { token, secret } = req.body || {};
    if (!token || !secret) {
      res.status(400).json({ success: false, error: 'Missing token or secret' });
      return;
    }
    // Verifieer TOTP token
    const isValid = authenticator.check(token, secret);
    res.status(200).json({ success: isValid });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
}
