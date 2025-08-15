const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const router = express.Router();

const db = new sqlite3.Database('locations.db');

// Create default admin account if not exists
db.get('SELECT * FROM accounts WHERE username = ?', ['admin'], (err, row) => {
    if (!row) {
        db.run('INSERT INTO accounts (username, password, created_at) VALUES (?, ?, ?)', ['admin', 'admin2025', new Date().toISOString()]);
        console.log('Default admin account aangemaakt: admin / admin2025');
    }
});

db.run(`CREATE TABLE IF NOT EXISTS accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE,
    password TEXT,
    created_at TEXT
)`);

// List all accounts
router.get('/', (req, res) => {
    db.all('SELECT username FROM accounts', [], (err, rows) => {
        if (err) return res.json({ success: false, error: 'DB error' });
        res.json({ success: true, accounts: rows });
    });
});

// Add new account
router.post('/', (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.json({ success: false, error: 'Ongeldige gegevens' });
    db.run('INSERT INTO accounts (username, password, created_at) VALUES (?, ?, ?)', [username, password, new Date().toISOString()], function(err) {
        if (err) return res.json({ success: false, error: 'Gebruikersnaam bestaat al of DB error' });
        res.json({ success: true });
    });
});


// Simple token for demo (replace with JWT for production)
const ADMIN_TOKEN = 'admin2025token';

// Admin login endpoint
router.post('/login', (req, res) => {
    const { username, password } = req.body;
    db.get('SELECT * FROM accounts WHERE username = ? AND password = ?', [username, password], (err, row) => {
        if (err || !row) return res.json({ success: false, error: 'Ongeldige login' });
        if (username === 'admin') {
            return res.json({ success: true, token: ADMIN_TOKEN });
        }
        return res.json({ success: false, error: 'Geen admin account' });
    });
});

// Auth middleware for admin endpoints
function requireAdmin(req, res, next) {
    const auth = req.headers['authorization'];
    if (!auth || auth !== 'Bearer ' + ADMIN_TOKEN) {
        return res.status(401).json({ success: false, error: 'Niet geautoriseerd' });
    }
    next();
}

// List all accounts (admin only)
router.get('/', requireAdmin, (req, res) => {
    db.all('SELECT username FROM accounts', [], (err, rows) => {
        if (err) return res.json({ success: false, error: 'DB error' });
        res.json({ success: true, accounts: rows });
    });
});

// Add new account (admin only)
router.post('/', requireAdmin, (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) return res.json({ success: false, error: 'Ongeldige gegevens' });
    db.run('INSERT INTO accounts (username, password, created_at) VALUES (?, ?, ?)', [username, password, new Date().toISOString()], function(err) {
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
