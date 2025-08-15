// Vercel serverless function: /api/generate_2fa_secret
import { authenticator } from 'otplib';

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  try {
    const { user } = req.body || {};
    if (!user) {
      res.status(400).json({ success: false, error: 'Missing user' });
      return;
    }
    // Genereer een base32 secret voor Google Authenticator
    const secret = authenticator.generateSecret();
    // Sla secret op in DB (hier: demo, niet persistent)
    // In productie: koppel aan user in SQLite
    res.status(200).json({ success: true, secret });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
}
