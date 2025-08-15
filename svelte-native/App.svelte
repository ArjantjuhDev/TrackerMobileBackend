<script>
  import { onMount } from 'svelte';
  import { permissionGranted, locationPermissionGranted, cameraPermissionGranted, fatalError, locationText, requestAllPermissions, uploadLocation } from './permissions.js';
  import { verificationState, verificationError, startVerificationPolling } from './pairing.js';

  let appCode = '';
  let deviceName = '';
  let paired = false;

  onMount(() => {
    requestAllPermissions();
  });

  function handlePair() {
    startVerificationPolling(appCode, deviceName, (id) => {
      paired = true;
    });
  }

  function handleLocationUpload() {
    // Example: upload dummy location
    uploadLocation(52.370216, 4.895168); // Amsterdam
  }
</script>

<page>
  <actionBar title="Tracker Mobile" />
  <stackLayout>
    <label text="Pair device" />
    <textField bind:text={appCode} hint="App code" />
    <textField bind:text={deviceName} hint="Device name" />
    <button text="Pair" on:tap={handlePair} />
    {#if $verificationError}
      <label text={$verificationError} style="color: red;" />
    {/if}
    {#if $verificationState}
      <label text="Device paired!" style="color: green;" />
      <button text="Upload Location" on:tap={handleLocationUpload} />
      <label text={$locationText} />
    {/if}
    {#if $fatalError}
      <label text={$fatalError} style="color: red;" />
    {/if}
  </stackLayout>
</page>
