// Vercel serverless function: /api/check_connection
export default async function handler(req, res) {
  if (req.method !== 'GET') {
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  try {
    // Hier kun je 2FA en DB koppeling toevoegen
    res.status(200).json({ connected: true, message: 'Connection OK' });
  } catch (e) {
    res.status(500).json({ connected: false, error: e.message });
  }
}
