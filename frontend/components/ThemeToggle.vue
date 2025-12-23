<script setup lang="ts">
const { isDark, toggle, loadPreference } = useColorMode()
const mounted = ref(false)

onMounted(() => {
  loadPreference()
  mounted.value = true
})
</script>

<template>
  <button
    v-if="mounted"
    @click="toggle"
    class="w-9 h-9 flex items-center justify-center rounded-lg transition-colors"
    :class="isDark ? 'bg-slate-700 hover:bg-slate-600 text-amber-400' : 'bg-slate-200 hover:bg-slate-300 text-slate-600'"
    :title="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
  >
    <!-- Sun icon (shown in dark mode) -->
    <svg v-if="isDark" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3v1m0 16v1m9-9h-1M4 12H3m15.364 6.364l-.707-.707M6.343 6.343l-.707-.707m12.728 0l-.707.707M6.343 17.657l-.707.707M16 12a4 4 0 11-8 0 4 4 0 018 0z" />
    </svg>
    <!-- Moon icon (shown in light mode) -->
    <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
    </svg>
  </button>
  <!-- Placeholder during SSR -->
  <div v-else class="w-9 h-9 rounded-lg bg-slate-200"></div>
</template>
