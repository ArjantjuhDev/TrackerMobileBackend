# Backend

Node.js/Express API for device pairing using SQLite.

- index.js
- devices.db
- .env
- koppeling met frontend

### Endpoints

- `POST /api/register_device` — Register a device code
- `POST /api/pair_device` — Pair a device code
- `GET /api/is_paired/:code` — Check if a device code is paired

### Environment Variables

- `PORT` — Port to run the backend
