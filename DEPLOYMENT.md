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
