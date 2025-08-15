<script>
  import { onMount } from 'svelte';
  import { deviceLocked, deviceTakenOver, startWipeLockPolling } from './deviceState.js';
  import LockScreen from './LockScreen.svelte';
  let deviceId = '';

  onMount(() => {
    if (deviceId) {
      startWipeLockPolling(deviceId);
    }
  });
</script>

<page>
  <actionBar title="Tracker Mobile" />
  <stackLayout>
    <LockScreen />
    {#if $deviceTakenOver}
      <label text="Device is overgenomen!" style="color: green; font-size: 22;" />
    {/if}
    <!-- Add more UI for device state, location, etc. -->
  </stackLayout>
</page>
