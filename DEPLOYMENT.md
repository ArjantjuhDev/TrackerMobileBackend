---
title: SvelteKit Node Adapter Render Deployment Blueprint
description: Stappenplan voor het deployen van een SvelteKit backend op Render
---

# SvelteKit Node Adapter Render Deployment Blueprint

## 1. Projectstructuur
- SvelteKit projectmap: `Website`
- API endpoints: `Website/src/routes/api/`

## 2. Vereiste bestanden
- `Website/svelte.config.js` gebruikt: `@sveltejs/adapter-node`
- `Website/package.json` scripts:
  - `"dev": "svelte-kit dev"`
  - `"build": "svelte-kit build"`
  - `"start": "svelte-kit start"`

## 3. Dependencies installeren
```sh
npm install @sveltejs/kit @sveltejs/adapter-node
```

## 4. Code naar GitHub pushen
- Push je laatste code naar GitHub.

## 5. Render Service Instellen
- Maak een nieuwe "Web Service" aan op Render.
- Root Directory: `Website`
- Build Command: `npm run build`
- Start Command: `npm start`
- Node versie: bijvoorbeeld 18+

## 6. Omgevingsvariabelen
- Voeg benodigde environment variables toe (zoals secrets, database URLs).

## 7. API Endpoints
- Backend logica in `Website/src/routes/api/`
- Voorbeeld: `/api/verify_app_code`, `/api/register_device`

## 8. Deployen
- Start de deploy op Render.
- Controleer de build logs op fouten.
- Bij foutmelding "missing root directory", zet root op `Website`.

## 9. Testen
- Bezoek jouw Render URL na deployment.
- Test API endpoints en frontend.

## 10. Troubleshooting
- Controleer bij fouten:
  - Root directory instelling
  - Build/start commands
  - Dependency installatie
  - API endpoint paden

## 11. Updates & Onderhoud
- Voor updates: push naar GitHub.
- Render zal automatisch opnieuw deployen, of je kunt handmatig triggeren.

---
