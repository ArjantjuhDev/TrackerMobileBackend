
require('dotenv').config();
const express = require('express');
const sqlite3 = require('sqlite3').verbose();

const cors = require('cors');
const app = express();
app.use(cors({
    origin: 'https://tracker-mobile-private.vercel.app',
    methods: ['GET', 'POST'],
    allowedHeaders: ['Content-Type', 'x-api-key']
}));
app.use(express.json());

const API_KEY = process.env.API_KEY || 'jouw_geheime_api_key';

const db = new sqlite3.Database('locations.db');

// Devices table for pairing
db.run(`CREATE TABLE IF NOT EXISTS devices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT,
    pair_code TEXT,
    paired INTEGER DEFAULT 0,
    registered_at TEXT
)`);

db.run(`CREATE TABLE IF NOT EXISTS locations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    device_id TEXT,
    lat REAL NOT NULL,
    lon REAL NOT NULL,
    timestamp TEXT
)`);
db.run(`CREATE TABLE IF NOT EXISTS wipe_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    requested INTEGER DEFAULT 0
)`);
db.run(`CREATE TABLE IF NOT EXISTS lock_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    locked INTEGER DEFAULT 0
)`);
// Zorg dat er altijd één rij is
db.get('SELECT COUNT(*) as count FROM lock_requests', (err, row) => {
    if (!row || row.count === 0) {
        db.run('INSERT INTO lock_requests (locked) VALUES (0)');
    }
});
// Zorg dat er altijd één rij is
db.get('SELECT COUNT(*) as count FROM wipe_requests', (err, row) => {
    if (!row || row.count === 0) {
        db.run('INSERT INTO wipe_requests (requested) VALUES (0)');
    }
});

function checkApiKey(req, res, next) {
    if (req.headers['x-api-key'] !== API_KEY) {
        return res.status(401).json({ error: 'Unauthorized' });
    }
    next();
}


// Device registration: app sends device_id and pair_code on first start
app.post('/api/register_device', checkApiKey, (req, res) => {
    const { device_id, pair_code } = req.body;
    if (!device_id || !pair_code) {
        return res.status(400).json({ error: 'Invalid data' });
    }
    db.run('INSERT INTO devices (device_id, pair_code, paired, registered_at) VALUES (?, ?, 0, ?)', [device_id, pair_code, new Date().toISOString()], function(err) {
        if (err) return res.status(500).json({ error: 'DB error' });
        res.status(201).json({ status: 'registered' });
    });
});

// Pair device from dashboard
app.post('/api/pair_device', checkApiKey, (req, res) => {
    const { code } = req.body;
    if (!code || code.length !== 6) {
        return res.json({ success: false, error: 'Ongeldige code' });
    }
    db.get('SELECT * FROM devices WHERE pair_code = ? AND paired = 0', [code], (err, row) => {
        if (err) return res.json({ success: false, error: 'DB error' });
        if (!row) return res.json({ success: false, error: 'Geen toestel gevonden met deze code' });
        db.run('UPDATE devices SET paired = 1 WHERE id = ?', [row.id], function(err2) {
            if (err2) return res.json({ success: false, error: 'DB error' });
            res.json({ success: true });
        });
    });
});

app.post('/api/location', checkApiKey, (req, res) => {
    const { device_id, lat, lon, timestamp } = req.body;
    if (typeof lat !== 'number' || typeof lon !== 'number' || !device_id) {
        return res.status(400).json({ error: 'Invalid data' });
    }
    db.run('INSERT INTO locations (device_id, lat, lon, timestamp) VALUES (?, ?, ?, ?)', [device_id, lat, lon, timestamp], function(err) {
        if (err) return res.status(500).json({ error: 'DB error' });
        res.status(201).json({ status: 'ok' });
    });
});

app.get('/api/location', checkApiKey, (req, res) => {
    db.all('SELECT device_id, lat, lon, timestamp FROM locations ORDER BY id DESC', [], (err, rows) => {
        if (err) return res.status(500).json({ error: 'DB error' });
        db.get('SELECT requested FROM wipe_requests LIMIT 1', (err2, wipeRow) => {
            if (err2) return res.status(500).json({ error: 'DB error' });
            db.get('SELECT locked FROM lock_requests LIMIT 1', (err3, lockRow) => {
                if (err3) return res.status(500).json({ error: 'DB error' });
                res.json({ locations: rows, wipe: !!wipeRow.requested, lock: !!lockRow.locked });
            });
        });
    });
});
// Endpoint om een lock opdracht te geven
app.post('/api/lock', checkApiKey, (req, res) => {
    db.run('UPDATE lock_requests SET locked = 1', [], function(err) {
        if (err) return res.status(500).json({ error: 'DB error' });
        res.json({ status: 'lock requested' });
    });
});

// Endpoint om lock status te resetten (unlock)
app.post('/api/lock/reset', checkApiKey, (req, res) => {
    db.run('UPDATE lock_requests SET locked = 0', [], function(err) {
        if (err) return res.status(500).json({ error: 'DB error' });
        res.json({ status: 'lock reset' });
    });
});

// Endpoint om een wipe opdracht te geven
app.post('/api/wipe', checkApiKey, (req, res) => {
    db.run('UPDATE wipe_requests SET requested = 1', [], function(err) {
        if (err) return res.status(500).json({ error: 'DB error' });
        res.json({ status: 'wipe requested' });
    });
});

// Endpoint om wipe status te resetten (optioneel)
app.post('/api/wipe/reset', checkApiKey, (req, res) => {
    db.run('UPDATE wipe_requests SET requested = 0', [], function(err) {
        if (err) return res.status(500).json({ error: 'DB error' });
        res.json({ status: 'wipe reset' });
    });
});

// Alleen luisteren op localhost
app.listen(5000, '127.0.0.1', () => console.log('Server running on localhost:5000'));
