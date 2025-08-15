// Vercel serverless function: /api/2fa_keyuri
import { authenticator } from 'otplib';

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  try {
    const { user, service, secret } = req.body || {};
    if (!user || !service || !secret) {
      res.status(400).json({ success: false, error: 'Missing user, service, or secret' });
      return;
    }
    // Genereer otpauth:// URI voor QR code
    const keyuri = authenticator.keyuri(user, service, secret);
    res.status(200).json({ success: true, keyuri });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
}
