# TrackerMobilePrivate

## Web (Vercel)
- Source: `Website/`
- Deploy: Push to GitHub, connect Vercel, set root to `Website/`.
- Vercel auto-detects SvelteKit and deploys.

## Android (Native)
- Source: `svelte-native/`
- Build: Open terminal in `svelte-native/`, run:
	```bash
	npm install
	ns run android
	```
- Or open in Android Studio and build/run as a NativeScript project.

## Notes
- SvelteKit (Website) is for web deployment (Vercel).
- Svelte NativeScript (`svelte-native/`) is for native mobile (Android/iOS).
- Both projects use the same backend API endpoints for full sync.

## Next Steps
- Test both builds and verify all features.
- Remove legacy Android code when migration is complete.
# TrackerMobilePrivate

This repository contains the source code for the TrackerMobilePrivate app, including frontend and backend.

## Structure

- `frontend/`: Frontend code (HTML, JS, CSS)
- `backend/`: Backend Node.js/Express API


## Deployment

See `frontend/README.md` and `backend/README.md` for details on deploying each part. For Vercel deployment, ensure you have a `vercel.json` config in each relevant folder.

## Setup


1. Deploy backend and frontend to Vercel.
2. Update frontend to use backend API endpoints.

## License

MIT
