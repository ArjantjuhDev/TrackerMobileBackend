<script>
  import { BACKEND_API_URL } from '../lib/config.js';
  let username = '';
  let password = '';
  let error = '';
  let loading = false;
  let success = false;
  let show2FA = false;
  let totpCode = '';
  let totpError = '';
  let qrUrl = '';
  let isAdmin = false;
  let pendingLogin = null;

  async function handleLogin(e) {
    e.preventDefault();
    loading = true;
    error = '';
    success = false;
    show2FA = false;
    totpError = '';
    // Echte backend API call
    try {
      const res = await fetch(`${BACKEND_API_URL}/totp_2fa/status`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username })
      });
      const data = await res.json();
      if (data.success && (data.has2fa || data.qr)) {
        pendingLogin = { username, password };
        isAdmin = username === 'admin';
        qrUrl = data.qr || '';
        show2FA = true;
      } else {
        error = 'Ongeldige gegevens of 2FA status niet opgehaald.';
      }
    } catch (err) {
      error = 'Serverfout: ' + err.message;
    }
    loading = false;
  }

  async function handle2FA(e) {
    e.preventDefault();
    totpError = '';
    try {
      const res = await fetch(`${BACKEND_API_URL}/totp_2fa/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: pendingLogin.username, password: pendingLogin.password, token: totpCode })
      });
      const data = await res.json();
      if (data.success && data.token) {
        localStorage.setItem('jwtToken', data.token);
        success = true;
        show2FA = false;
        setTimeout(() => window.location.href = isAdmin ? '/admin' : '/member', 800);
      } else {
        totpError = 'Ongeldige 2FA code.';
      }
    } catch (err) {
      totpError = 'Serverfout: ' + err.message;
    }
  }
</script>
<div class="min-h-screen flex items-center justify-center bg-gray-900">
  <form class="bg-gray-800 p-8 rounded-xl shadow-lg w-full max-w-md" on:submit={handleLogin}>
    <h2 class="text-2xl font-bold text-white mb-6 text-center">Login</h2>
    {#if error}
      <div class="bg-red-700 text-white rounded px-4 py-2 mb-4">{error}</div>
    {/if}
    {#if success}
      <div class="bg-green-700 text-white rounded px-4 py-2 mb-4">Succesvol ingelogd! Je wordt doorgestuurd...</div>
    {/if}
    <input class="w-full mb-4 px-4 py-2 rounded bg-gray-700 text-white" type="text" placeholder="Gebruikersnaam" bind:value={username} />
    <input class="w-full mb-6 px-4 py-2 rounded bg-gray-700 text-white" type="password" placeholder="Wachtwoord" bind:value={password} />
    <button class="w-full py-2 rounded bg-blue-600 text-white font-bold hover:bg-blue-700 transition" type="submit" disabled={loading}>
      {loading ? 'Bezig...' : 'Login'}
    </button>
  </form>
  {#if show2FA}
    <form class="bg-gray-800 p-8 rounded-xl shadow-lg w-full max-w-md mt-8" on:submit={handle2FA}>
      <h2 class="text-xl font-bold text-white mb-4 text-center">2FA Verificatie</h2>
      <div class="mb-4 flex flex-col items-center">
        <img src={qrUrl} alt="QR Code" class="mb-2 rounded" style="max-width:180px;" />
        <span class="text-gray-300 text-sm">Scan deze QR met Google Authenticator</span>
      </div>
      <input class="w-full mb-4 px-4 py-2 rounded bg-gray-700 text-white" type="text" placeholder="Voer 6-cijferige code in" bind:value={totpCode} />
      <button class="w-full py-2 rounded bg-green-600 text-white font-bold hover:bg-green-700 transition" type="submit">Verifiëren</button>
      {#if totpError}
        <div class="bg-red-700 text-white rounded px-4 py-2 mt-4">{totpError}</div>
      {/if}
    </form>
  {/if}
</div>
