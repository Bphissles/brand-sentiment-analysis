<script setup lang="ts">
const { user, isAuthenticated, logout } = useAuth()
const router = useRouter()

const showMenu = ref(false)

const handleLogout = () => {
  logout()
  showMenu.value = false
  router.push('/login')
}

// Close menu when clicking outside
const menuRef = ref<HTMLElement | null>(null)

onMounted(() => {
  document.addEventListener('click', (e) => {
    if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
      showMenu.value = false
    }
  })
})
</script>

<template>
  <div class="relative" ref="menuRef">
    <!-- Authenticated: Show user menu -->
    <template v-if="isAuthenticated && user">
      <button
        @click="showMenu = !showMenu"
        class="flex items-center gap-2 px-3 py-2 bg-slate-700 rounded-lg hover:bg-slate-600 transition-colors"
      >
        <div class="w-8 h-8 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-full flex items-center justify-center">
          <span class="text-white text-sm font-medium">{{ user.email[0].toUpperCase() }}</span>
        </div>
        <span class="text-slate-200 text-sm hidden sm:block">{{ user.email }}</span>
        <svg class="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      <!-- Dropdown Menu -->
      <div
        v-if="showMenu"
        class="absolute right-0 mt-2 w-48 bg-slate-800 rounded-lg shadow-xl border border-slate-700 py-1 z-50"
      >
        <div class="px-4 py-2 border-b border-slate-700">
          <p class="text-sm text-slate-400">Signed in as</p>
          <p class="text-sm text-white font-medium truncate">{{ user.email }}</p>
          <span class="inline-block mt-1 px-2 py-0.5 bg-cyan-500/20 text-cyan-400 text-xs rounded">
            {{ user.role }}
          </span>
        </div>
        <button
          @click="handleLogout"
          class="w-full px-4 py-2 text-left text-sm text-slate-300 hover:bg-slate-700 flex items-center gap-2"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
          </svg>
          Sign out
        </button>
      </div>
    </template>

    <!-- Not authenticated: Show login button -->
    <template v-else>
      <NuxtLink
        to="/login"
        class="px-4 py-2 bg-gradient-to-r from-cyan-500 to-blue-500 text-white text-sm font-medium rounded-lg hover:from-cyan-600 hover:to-blue-600 transition-all"
      >
        Sign In
      </NuxtLink>
    </template>
  </div>
</template>
