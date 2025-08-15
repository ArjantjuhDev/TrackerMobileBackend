<script>
  let totpSetup = false;
  let totpCode = '';
  let qrUrl = '';
  let error = '';
  let appInstalled = false;
  let devices = [
    { name: 'Android 1', code: 'ABC123', online: true },
    { name: 'Android 2', code: 'XYZ789', online: false }
  ];
  let selectedDevice = devices[0]?.code;
  let statusMsg = '';
  function setup2FA() {
    qrUrl = 'https://api.qrserver.com/v1/create-qr-code/?data=otpauth://totp/TrackerMobile:member1?secret=ABC123';
    totpSetup = true;
  }
  function verify2FA() {
    if (totpCode === '123456') {
      appInstalled = true;
      error = '';
    } else {
      error = 'Ongeldige code';
    }
  }
  function lockDevice() {
    statusMsg = 'Telefoon vergrendeld!';
  }
  function unlockDevice() {
    statusMsg = 'Telefoon ontgrendeld!';
  }
  function wipeDevice() {
    statusMsg = 'Telefoon gewist!';
  }
  function screenshotDevice() {
    statusMsg = 'Screenshot aangevraagd!';
  }
</script>
<div class="min-h-screen bg-gray-900 text-white p-8">
  <h2 class="text-3xl font-bold mb-6">Member Dashboard</h2>
  <div class="bg-gray-800 rounded-xl p-6 shadow-lg mb-6">
    <h3 class="text-xl font-semibold mb-4">2FA Instellen</h3>
    {#if !totpSetup}
      <button class="bg-blue-600 px-4 py-2 rounded" on:click={setup2FA}>Genereer QR voor Google Authenticator</button>
    {:else}
      <div class="mb-4">
        <img src={qrUrl} alt="QR Code" class="mx-auto mb-2 rounded" style="max-width:180px;" />
        <input class="w-full px-2 py-1 rounded bg-gray-700 text-white" type="text" placeholder="Voer 6-cijferige code in" bind:value={totpCode} />
        <button class="bg-green-600 px-4 py-2 rounded mt-2" on:click={verify2FA}>Verifiëren</button>
        {#if error}
          <div class="bg-red-700 text-white rounded px-4 py-2 mt-2">{error}</div>
        {/if}
      </div>
    {/if}
  </div>
  {#if appInstalled}
    <div class="bg-gray-700 rounded-xl p-6 shadow-lg">
      <h3 class="text-xl font-semibold mb-4">App installeren & telefoon bedienen</h3>
      <p class="mb-2">Download de TrackerMobile app en koppel je toestel.</p>
      <a href="/download/TrackerMobile.apk" class="bg-blue-600 px-4 py-2 rounded text-white font-bold">Download App</a>
      <div class="mt-6">
        <label class="block mb-2 font-semibold">Gekoppelde apparaten:</label>
        <select class="w-full mb-4 px-2 py-1 rounded bg-gray-800 text-white" bind:value={selectedDevice}>
          {#each devices as device}
            <option value={device.code}>{device.name} ({device.code}) {device.online ? '🟢' : '🔴'}</option>
          {/each}
        </select>
        <div class="flex flex-wrap gap-2 mb-4">
          <button class="bg-purple-600 px-4 py-2 rounded" on:click={lockDevice}>Vergrendelen</button>
          <button class="bg-green-600 px-4 py-2 rounded" on:click={unlockDevice}>Ontgrendelen</button>
          <button class="bg-red-600 px-4 py-2 rounded" on:click={wipeDevice}>Wissen</button>
          <button class="bg-gray-500 px-4 py-2 rounded" on:click={screenshotDevice}>Screenshot</button>
        </div>
        {#if statusMsg}
          <div class="bg-blue-700 text-white rounded px-4 py-2 mb-2">{statusMsg}</div>
        {/if}
      </div>
    </div>
  {/if}
</div>
