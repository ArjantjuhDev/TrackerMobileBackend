// Example SvelteKit API endpoint for /api/register_device
export async function POST({ request }) {
  const { device_id, device_name } = await request.json();
  // TODO: Replace with your actual registration logic
  // For now, always return status: 'registered'
  return new Response(JSON.stringify({ status: 'registered' }), {
    headers: { 'Content-Type': 'application/json' }
  });
}
