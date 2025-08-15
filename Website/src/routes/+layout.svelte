<script>
import '../app.css';
</script>
<nav class="bg-gray-900 text-white px-6 py-4 flex justify-between items-center shadow">
	<div class="font-bold text-xl">TrackerMobile</div>
	<div class="space-x-4">
		<a href="/login" class="hover:underline">Login</a>
		<a href="/member" class="hover:underline">Member Dashboard</a>
		<a href="/admin" class="hover:underline">Admin Dashboard</a>
	</div>
</nav>
<main>
	<slot />
</main>
