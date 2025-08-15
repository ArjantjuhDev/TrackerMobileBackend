const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const router = express.Router();

const db = new sqlite3.Database('locations.db');

// Zorg dat de accounts tabel altijd bestaat vóór gebruik
db.run(`CREATE TABLE IF NOT EXISTS accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE,
    password TEXT,
    created_at TEXT
)`);

const JWT_SECRET = process.env.JWT_SECRET || 'tracker2025supersecret';

// Create default admin account if not exists (hashed password)
db.get('SELECT * FROM accounts WHERE username = ?', ['admin'], (err, row) => {
    if (!row) {
        const hash = bcrypt.hashSync('admin2025', 10);
        db.run('INSERT INTO accounts (username, password, created_at) VALUES (?, ?, ?)', ['admin', hash, new Date().toISOString()]);
        console.log('Default admin account aangemaakt: admin / admin2025');
    }
});

// ...existing code...


// Admin login endpoint (JWT)
router.post('/login', (req, res) => {
    const { username, password } = req.body;
    db.get('SELECT * FROM accounts WHERE username = ?', [username], (err, row) => {
        if (err || !row) return res.json({ success: false, error: 'Ongeldige login' });
        if (!bcrypt.compareSync(password, row.password)) return res.json({ success: false, error: 'Ongeldige login' });
        if (username === 'admin') {
            const token = jwt.sign({ username: 'admin', role: 'admin' }, JWT_SECRET, { expiresIn: '2h' });
            return res.json({ success: true, token });
        }
        return res.json({ success: false, error: 'Geen admin account' });
    });
});

// Auth middleware for admin endpoints (JWT)
function requireAdmin(req, res, next) {
    const auth = req.headers['authorization'];
    if (!auth || !auth.startsWith('Bearer ')) {
        return res.status(401).json({ success: false, error: 'Niet geautoriseerd' });
    }
    const token = auth.replace('Bearer ', '');
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        if (decoded.username !== 'admin' || decoded.role !== 'admin') {
            return res.status(401).json({ success: false, error: 'Niet geautoriseerd' });
        }
        next();
    } catch (err) {
        return res.status(401).json({ success: false, error: 'Token ongeldig' });
    }
}

// List all accounts (admin only)
router.get('/', requireAdmin, (req, res) => {
    db.all('SELECT username FROM accounts', [], (err, rows) => {
        if (err) return res.json({ success: false, error: 'DB error' });
        res.json({ success: true, accounts: rows });
    });
});

// Add new account (admin only, hashed password)
router.post('/', requireAdmin, (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.json({ success: false, error: 'Ongeldige gegevens' });
    const hash = bcrypt.hashSync(password, 10);
    db.run('INSERT INTO accounts (username, password, created_at) VALUES (?, ?, ?)', [username, hash, new Date().toISOString()], function(err) {
        if (err) return res.json({ success: false, error: 'Gebruikersnaam bestaat al of DB error' });
        res.json({ success: true });
    });
});

// Delete account (admin only)
router.delete('/:username', requireAdmin, (req, res) => {
    const username = req.params.username;
    db.run('DELETE FROM accounts WHERE username = ?', [username], function(err) {
        if (err) return res.json({ success: false, error: 'DB error' });
        res.json({ success: true });
    });
});

module.exports = router;
