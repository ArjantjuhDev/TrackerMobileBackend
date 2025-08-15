// Vercel serverless function: /api/register_device
export default async function handler(req, res) {
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  try {
    const { code } = req.body || {};
    if (!code || code.length !== 6) {
      res.status(400).json({ success: false, error: 'Ongeldige code' });
      return;
    }
    // Hier kun je DB koppeling toevoegen
    res.status(200).json({ success: true, message: 'Registratie geslaagd!' });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
}
