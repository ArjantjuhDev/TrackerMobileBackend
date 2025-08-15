<script>
  // TODO: Replace with real API call
  let accounts = [
    { username: 'admin', totp: true },
    { username: 'member1', totp: false }
  ];
  let editUser = '';
  let showEdit = false;
  function editAccount(user) {
    editUser = user.username;
    showEdit = true;
  }
  function deleteAccount(user) {
    accounts = accounts.filter(a => a.username !== user.username);
  }
</script>
<div class="min-h-screen bg-gray-900 text-white p-8">
  <h2 class="text-3xl font-bold mb-6">Admin Dashboard</h2>
  <div class="bg-gray-800 rounded-xl p-6 shadow-lg">
    <h3 class="text-xl font-semibold mb-4">Accounts</h3>
    <table class="w-full mb-6">
      <thead>
        <tr class="bg-gray-700">
          <th class="p-2 text-left">Gebruiker</th>
          <th class="p-2 text-left">2FA</th>
          <th class="p-2 text-left">Acties</th>
        </tr>
      </thead>
      <tbody>
        {#each accounts as account}
          <tr class="border-b border-gray-700">
            <td class="p-2">{account.username}</td>
            <td class="p-2">{account.totp ? 'Ja' : 'Nee'}</td>
            <td class="p-2">
              <button class="bg-blue-600 px-3 py-1 rounded mr-2" on:click={() => editAccount(account)}>Bewerk</button>
              <button class="bg-red-600 px-3 py-1 rounded" on:click={() => deleteAccount(account)}>Verwijder</button>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
    {#if showEdit}
      <div class="bg-gray-700 p-4 rounded mb-4">
        <h4 class="font-bold mb-2">Bewerk account: {editUser}</h4>
        <input class="w-full mb-2 px-2 py-1 rounded bg-gray-800 text-white" type="text" bind:value={editUser} />
        <button class="bg-green-600 px-3 py-1 rounded" on:click={() => showEdit = false}>Opslaan</button>
      </div>
    {/if}
  </div>
</div>
