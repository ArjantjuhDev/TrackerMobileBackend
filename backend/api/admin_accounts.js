const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const router = express.Router();

const db = new sqlite3.Database('locations.db');

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

// Delete account
router.delete('/:username', (req, res) => {
    const username = req.params.username;
    db.run('DELETE FROM accounts WHERE username = ?', [username], function(err) {
        if (err) return res.json({ success: false, error: 'DB error' });
        res.json({ success: true });
    });
});

module.exports = router;
