# Render Setup: SvelteKit Node Adapter

Volg deze stappen om de fout "Service Root Directory ... is missing" op Render te voorkomen en je SvelteKit backend correct te deployen:

---

## 1. Root Directory instellen
- Ga naar je Render Web Service instellingen.
- Zet de "Root Directory" op:
  
  ```
  Website
  ```
  (Niet op `src/backend` of een andere map!)

## 2. Build en Start Commands
- Build Command:
  ```
  npm run build
  ```
- Start Command:
  ```
  npm start
  ```

## 3. Node Versie
- Kies een recente Node versie, bijvoorbeeld 18+.

## 4. Deploy
- Sla de instellingen op.
- Trigger een nieuwe deploy.
- Render zal nu je SvelteKit backend correct vinden en starten.

## 5. Troubleshooting
- Zie je opnieuw een root directory fout? Controleer of de root exact op `Website` staat.
- Controleer of je project op GitHub de map `Website` bevat met alle SvelteKit bestanden.

---

Met deze stappen is je Render setup altijd correct en kun je direct deployen zonder root directory fouten.
