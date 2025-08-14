from flask import Flask, request, jsonify, render_template_string
from flask_cors import CORS
import random
import time
from functools import wraps
import uuid
import sys
import os

app = Flask(__name__)
# Use Flask-CORS for robust CORS handling
CORS(app, supports_credentials=True, resources={r"/*": {"origins": "*"}}, allow_headers=["Content-Type", "Authorization", "X-Requested-With", "X-API-Key"])
device_locations = {}  # device_id -> {lat, lon, timestamp}
device_states = {}  # device_id -> {locked: bool, taken_over: bool}
API_KEY = os.environ.get("API_KEY", "jouw_geheime_api_key")  # Zet hier een sterke geheime sleutel
last_post_time = 0


valid_pairing_codes = {}  # website_code -> expires
pending_app_codes = {}   # app_code -> {website_code, expires, verified}
registered_devices = set()
import base64
from datetime import datetime
uploaded_photos = {}  # device_id -> list of {timestamp, photo_data}
# Endpoint to receive uploaded photos (base64)
@app.route('/api/upload_photo', methods=['POST'])
def upload_photo():
    data = request.get_json()
    device_id = data.get('device_id')
    # Accept both 'photo' and 'photo_data' for compatibility
    photo_data = data.get('photo_data') or data.get('photo')
    if not device_id or not photo_data:
        return jsonify({"success": False, "error": "device_id en photo_data zijn vereist"}), 400
    timestamp = datetime.utcnow().isoformat()
    uploaded_photos.setdefault(device_id, []).append({"timestamp": timestamp, "photo_data": photo_data})
    return jsonify({"success": True, "device_id": device_id, "timestamp": timestamp})

# Endpoint to get all photos for a device
@app.route('/api/photos', methods=['GET'])
def get_photos():
    device_id = request.args.get('device_id')
    if not device_id:
        return jsonify({"success": False, "error": "device_id is vereist"}), 400
    photos = uploaded_photos.get(device_id, [])
    return jsonify({"success": True, "device_id": device_id, "photos": photos})
@app.route('/api/lock_device', methods=['POST'])
def lock_device():
    data = request.get_json()
    device_id = data.get('device_id')
    if not device_id:
        return jsonify({"success": False, "error": "device_id is vereist"}), 400
    state = device_states.setdefault(device_id, {"locked": False, "taken_over": False})
    state["locked"] = True
    return jsonify({"success": True, "device_id": device_id, "locked": True})

@app.route('/api/unlock_device', methods=['POST'])
def unlock_device():
    data = request.get_json()
    device_id = data.get('device_id')
    if not device_id:
        return jsonify({"success": False, "error": "device_id is vereist"}), 400
    state = device_states.setdefault(device_id, {"locked": False, "taken_over": False})
    state["locked"] = False
    return jsonify({"success": True, "device_id": device_id, "locked": False})

@app.route('/api/takeover_device', methods=['POST'])
def takeover_device():
    data = request.get_json()
    device_id = data.get('device_id')
    if not device_id:
        return jsonify({"success": False, "error": "device_id is vereist"}), 400
    state = device_states.setdefault(device_id, {"locked": False, "taken_over": False})
    state["taken_over"] = True
    return jsonify({"success": True, "device_id": device_id, "taken_over": True})

@app.route('/api/device_state', methods=['GET'])
def device_state():
    device_id = request.args.get('device_id')
    if not device_id:
        return jsonify({"success": False, "error": "device_id is vereist"}), 400
    state = device_states.get(device_id, {"locked": False, "taken_over": False})
    return jsonify({"success": True, "device_id": device_id, "state": state})

@app.route('/api/health', methods=['GET'])
def health():
    return jsonify({"status": "ok"})

# Generate a 6-digit pairing code
@app.route('/api/generate_code', methods=['POST'])
def generate_code():
    code = str(random.randint(100000, 999999))
    expires = int(time.time()) + 300  # 5 min valid
    valid_pairing_codes[code] = expires
    return jsonify({"code": code, "expires": expires})

@app.route('/api/validate_code', methods=['POST'])
def validate_code():
    data = request.get_json()
    website_code = data.get('code')
    now = int(time.time())
    expires = valid_pairing_codes.get(website_code)
    print(f"[validate_code] Received code: {website_code}", file=sys.stderr)
    print(f"[validate_code] valid_pairing_codes: {valid_pairing_codes}", file=sys.stderr)
    if not website_code:
        print("[validate_code] No code provided in request.", file=sys.stderr)
        return jsonify({"valid": False, "reason": "Geen code meegegeven in request body (gebruik {'code': '123456'})"}), 400
    if not expires:
        print(f"[validate_code] Code {website_code} onbekend of al gebruikt.", file=sys.stderr)
        return jsonify({"valid": False, "reason": f"Code {website_code} onbekend of al gebruikt. Geldige codes: {list(valid_pairing_codes.keys())}"}), 400
    if now > expires:
        print(f"[validate_code] Code {website_code} verlopen. Expired at {expires}, now is {now}.", file=sys.stderr)
        return jsonify({"valid": False, "reason": f"Code {website_code} verlopen. Expired at {expires}, now is {now}"}), 400
    # Generate new app code
    app_code = str(random.randint(100000, 999999))
    app_expires = now + 300
    pending_app_codes[app_code] = {
        "website_code": website_code,
        "expires": app_expires,
        "verified": False
    }
    # Remove website code so it can't be reused
    del valid_pairing_codes[website_code]
    print(f"[validate_code] Code {website_code} accepted, new app_code: {app_code}", file=sys.stderr)
    return jsonify({"valid": True, "app_code": app_code, "expires": app_expires})
# Endpoint for website to verify app code
@app.route('/api/verify_app_code', methods=['POST'])
def verify_app_code():
    data = request.get_json()
    app_code = data.get('app_code')
    now = int(time.time())
    entry = pending_app_codes.get(app_code)
    if not entry:
        return jsonify({"verified": False, "reason": "App-code onbekend of al gebruikt"}), 400
    if now > entry["expires"]:
        return jsonify({"verified": False, "reason": "App-code verlopen"}), 400
    entry["verified"] = True
    return jsonify({"verified": True, "website_code": entry["website_code"]})

@app.route('/api/register_device', methods=['POST'])
def register_device():
    data = request.get_json()
    device_id = data.get('device_id')
    if not device_id:
        return jsonify({"success": False, "error": "device_id is vereist"}), 400
    registered_devices.add(device_id)
    return jsonify({"success": True, "device_id": device_id})


def require_api_key(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        key = request.headers.get('X-API-Key')
        if key != API_KEY:
            return jsonify({"error": "Unauthorized"}), 401
        return f(*args, **kwargs)
    return decorated

def rate_limit(seconds=5):
    def decorator(f):
        @wraps(f)
        def decorated(*args, **kwargs):
            global last_post_time
            now = time.time()
            if now - last_post_time < seconds:
                return jsonify({"error": "Too many requests"}), 429
            last_post_time = now
            return f(*args, **kwargs)
        return decorated
    return decorator


# Accept device_id and store location per device
@app.route('/api/location', methods=['OPTIONS', 'POST'])
@require_api_key
@rate_limit(seconds=5)
def post_location():
    if request.method == 'OPTIONS':
        return ('', 204)
    data = request.get_json()
    device_id = data.get('device_id')
    if not device_id:
        return jsonify({"error": "device_id required"}), 400
    device_locations[device_id] = {
        "lat": data.get("lat"),
        "lon": data.get("lon"),
        "timestamp": data.get("timestamp")
    }
    return jsonify({"status": "ok"})


# Endpoint to get all device locations
@app.route('/api/locations', methods=['OPTIONS', 'GET'])
@require_api_key
def get_locations():
    if request.method == 'OPTIONS':
        return ('', 204)
    # Returns a list of {device_id, lat, lon, timestamp}
    result = []
    for device_id, loc in device_locations.items():
        entry = {"device_id": device_id}
        entry.update(loc)
        result.append(entry)
    return jsonify(result)

# For Vercel, do not run app directly. Export 'app' for ASGI/WSGI compatibility.
