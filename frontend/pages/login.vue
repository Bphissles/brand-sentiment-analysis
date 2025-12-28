<script setup lang="ts">
const { login, register, isAuthenticated } = useAuth()
const router = useRouter()

const isLoginMode = ref(true)
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref<string | null>(null)

// Redirect if already authenticated (handled by global middleware, but keep as backup)
watch(isAuthenticated, (authenticated) => {
  if (authenticated) {
    const redirectPath = sessionStorage.getItem('redirectAfterLogin') || '/'
    sessionStorage.removeItem('redirectAfterLogin')
    router.push(redirectPath)
  }
}, { immediate: true })

const handleSubmit = async () => {
  error.value = null
  
  // Validation
  if (!email.value || !password.value) {
    error.value = 'Email and password are required'
    return
  }

  if (!isLoginMode.value && password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match'
    return
  }

  if (!isLoginMode.value && password.value.length < 8) {
    error.value = 'Password must be at least 8 characters'
    return
  }

  loading.value = true

  try {
    const result = isLoginMode.value 
      ? await login(email.value, password.value)
      : await register(email.value, password.value)

    if (result.success) {
      // Check for redirect destination (set by auth middleware)
      const redirectPath = sessionStorage.getItem('redirectAfterLogin') || '/'
      sessionStorage.removeItem('redirectAfterLogin')
      router.push(redirectPath)
    } else {
      error.value = result.error || 'Authentication failed'
    }
  } catch (e: any) {
    error.value = e.message || 'An error occurred'
  } finally {
    loading.value = false
  }
}

const toggleMode = () => {
  isLoginMode.value = !isLoginMode.value
  error.value = null
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex items-center justify-center px-4">
    <div class="w-full max-w-md">
      <!-- Logo -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-gradient-to-br from-cyan-400 to-blue-500 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
          <span class="text-white font-bold text-2xl">V</span>
        </div>
        <h1 class="text-2xl font-bold text-white">Voice of the Operator</h1>
        <p class="text-slate-400 mt-2">{{ isLoginMode ? 'Sign in to your account' : 'Create a new account' }}</p>
      </div>

      <!-- Form Card -->
      <div class="bg-slate-800/50 backdrop-blur-sm rounded-2xl shadow-xl border border-slate-700 p-8">
        <!-- Error Alert -->
        <div v-if="error" class="mb-6 p-4 bg-red-900/30 border border-red-800 text-red-400 rounded-lg text-sm">
          {{ error }}
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-5">
          <!-- Email -->
          <div>
            <label for="email" class="block text-sm font-medium text-slate-300 mb-2">Email</label>
            <input
              id="email"
              v-model="email"
              type="email"
              autocomplete="email"
              required
              class="w-full px-4 py-3 bg-slate-700/50 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:ring-2 focus:ring-cyan-500 focus:border-transparent transition-all"
              placeholder="you@example.com"
            />
          </div>

          <!-- Password -->
          <div>
            <label for="password" class="block text-sm font-medium text-slate-300 mb-2">Password</label>
            <input
              id="password"
              v-model="password"
              type="password"
              autocomplete="current-password"
              required
              class="w-full px-4 py-3 bg-slate-700/50 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:ring-2 focus:ring-cyan-500 focus:border-transparent transition-all"
              placeholder="••••••••"
            />
          </div>

          <!-- Confirm Password (Register only) -->
          <div v-if="!isLoginMode">
            <label for="confirmPassword" class="block text-sm font-medium text-slate-300 mb-2">Confirm Password</label>
            <input
              id="confirmPassword"
              v-model="confirmPassword"
              type="password"
              autocomplete="new-password"
              required
              class="w-full px-4 py-3 bg-slate-700/50 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:ring-2 focus:ring-cyan-500 focus:border-transparent transition-all"
              placeholder="••••••••"
            />
          </div>

          <!-- Submit Button -->
          <button
            type="submit"
            :disabled="loading"
            class="w-full py-3 px-4 bg-gradient-to-r from-cyan-500 to-blue-500 text-white font-medium rounded-lg hover:from-cyan-600 hover:to-blue-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition-all shadow-lg"
          >
            <LoadingSpinner v-if="loading" size="sm" color="text-white" />
            <span>{{ loading ? 'Please wait...' : (isLoginMode ? 'Sign In' : 'Create Account') }}</span>
          </button>
        </form>

        <!-- Toggle Mode -->
        <div class="mt-6 text-center">
          <button
            @click="toggleMode"
            class="text-cyan-400 hover:text-cyan-300 text-sm font-medium transition-colors"
          >
            {{ isLoginMode ? "Don't have an account? Sign up" : 'Already have an account? Sign in' }}
          </button>
        </div>
      </div>

      <!-- Demo Credentials -->
      <div class="mt-6 text-center text-slate-500 text-sm">
        <p>Demo: Register with any email to get started</p>
      </div>
    </div>
  </div>
</template>
