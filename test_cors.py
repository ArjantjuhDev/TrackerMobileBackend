import requests

BASE_URL = "http://127.0.0.1:5000"

# Test device registration endpoint
resp = requests.post(f"{BASE_URL}/api/register_device", json={"device_id": "testdevice123"})
print("/api/register_device:", resp.status_code, resp.json())

# Test QR code generation endpoint
resp = requests.post(f"{BASE_URL}/api/get_qr_code", json={"code": "testcode123"})
print("/api/get_qr_code:", resp.status_code, resp.json())

# Test QR code validation endpoint
qr_data = resp.json().get("qr_data")
if qr_data:
    resp2 = requests.post(f"{BASE_URL}/api/validate_qr", json={"qr_code": qr_data})
    print("/api/validate_qr:", resp2.status_code, resp2.json())
else:
    print("No qr_data returned from /api/get_qr_code")

# Test location posting endpoint
headers = {"X-API-Key": "jouw_geheime_api_key"}
loc_data = {
    "device_id": "testdevice123",
    "lat": 52.0,
    "lon": 5.0,
    "timestamp": "2025-08-14T12:00:00"
}
resp = requests.post(f"{BASE_URL}/api/location", json=loc_data, headers=headers)
print("/api/location:", resp.status_code, resp.json())

# Test get locations endpoint
resp = requests.get(f"{BASE_URL}/api/locations", headers=headers)
print("/api/locations:", resp.status_code, resp.json())
