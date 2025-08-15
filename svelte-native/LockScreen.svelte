<script>
  import { deviceLocked, lockCodeError, validateLockCode } from './deviceState.js';
  let lockCodeInput = '';
  let deviceId = '';

  function handleUnlock() {
    if (lockCodeInput.length === 6) {
      validateLockCode(deviceId, lockCodeInput);
    } else {
      lockCodeError.set('Voer een geldige 6-cijferige code in.');
    }
  }
</script>

{#if $deviceLocked}
<stackLayout>
  <label text="Nu moet u eerst een code invoeren" style="color: white; font-size: 22;" />
  <textField bind:text={lockCodeInput} hint="Ontgrendelingscode" />
  <button text="Ontgrendel" on:tap={handleUnlock} />
  {#if $lockCodeError}
    <label text={$lockCodeError} style="color: red; font-size: 14;" />
  {/if}
</stackLayout>
{/if}
