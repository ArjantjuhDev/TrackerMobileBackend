# Deployment Guide

This guide explains how to deploy the TrackerMobilePrivate app (frontend, backend, blockchain) to GitHub and Vercel.

## 1. Smart Contract Deployment

- Compile and deploy `blockchain/DevicePairing.sol` to Polygon Mumbai testnet (or similar).
- Save the contract address and ABI JSON file in `blockchain/`.

## 2. Backend Setup

- Update `backend/index.js` with the deployed contract address and ABI path.
- Add your wallet private key for transaction signing (use environment variables for security).
- For Vercel, move API code to `backend/api/` as serverless functions if needed.
- Deploy `backend/` to Vercel.

## 3. Frontend Setup

- Place your `index.html` and assets in `frontend/`.
- Update fetch URLs to point to the deployed backend API (Vercel URL).
- Deploy `frontend/` to Vercel.

## 4. GitHub Repository

- Push all code to your GitHub repository.
- Include `README.md` files in each folder for structure and instructions.

## 5. Environment Variables

- Use Vercel's dashboard to set secrets (e.g., wallet private key, RPC URL).

## 6. Testing

- Test device pairing end-to-end: frontend → backend → blockchain.
- Check logs and error messages for troubleshooting.

## 7. Maintenance

- Update contract, backend, or frontend as needed.
- Redeploy to Vercel after changes.

---

For questions, see the main README or contact the project maintainer.
# Vercel Deployment Instructions

1. Install dependencies:
   pip install -r requirements.txt

2. Set your API key in Vercel:
   - Edit `vercel.json` and set the value for `API_KEY` under `env`.
   - Or set it in the Vercel dashboard as an environment variable.

3. Deploy to Vercel:
   - Push your code to your Git repository connected to Vercel.
   - Vercel will automatically build and deploy using `tracker_server.py` for API routes and `public/index.html` for the website.

4. For local production:
   - Use a WSGI server (e.g., gunicorn) for better performance:
     gunicorn tracker_server:app

5. Make sure your API endpoints are accessible at `/api/*` and your website at `/`.

6. For troubleshooting, check Vercel logs and ensure your environment variables are set correctly.
