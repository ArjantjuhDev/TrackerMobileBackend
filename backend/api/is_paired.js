// Vercel serverless function: /api/is_paired/[code]
export default async function handler(req, res) {
  if (req.method !== 'GET') {
    res.status(405).json({ error: 'Method not allowed' });
    return;
  }
  try {
    const { code } = req.query;
    if (!code || code.length !== 6) {
      res.status(400).json({ paired: false, error: 'Ongeldige code' });
      return;
    }
    // Hier kun je DB koppeling toevoegen
    res.status(200).json({ paired: true }); // Demo: altijd gepaired
  } catch (e) {
    res.status(500).json({ paired: false, error: e.message });
  }
}
