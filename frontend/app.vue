<template>
  <div>
    <!-- Loading Screen -->
    <LoadingScreen
      v-if="!servicesReady"
      :message="loadingMessage"
      :show-status="true"
      :backend-ready="backendReady"
      :ml-ready="mlReady"
    />
    
    <!-- Main App -->
    <NuxtPage v-else />
    
    <!-- Error Toast -->
    <div
      v-if="healthError"
      class="fixed bottom-4 right-4 z-50 max-w-md rounded-lg bg-yellow-500 p-4 text-white shadow-lg"
    >
      <div class="flex items-start space-x-3">
        <svg class="h-6 w-6 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <div>
          <p class="font-semibold">Service Warning</p>
          <p class="text-sm">{{ healthError }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const { isReady, backendReady, mlReady, error, waitForServices } = useServiceHealth()
const { initAuth } = useAuth()

const servicesReady = ref(false)
const healthError = computed(() => error.value)

const loadingMessage = computed(() => {
  if (!backendReady.value) {
    return 'Starting backend API...'
  }
  if (!mlReady.value) {
    return 'Starting ML engine...'
  }
  return 'Services ready!'
})

onMounted(async () => {
  // Initialize auth from localStorage
  initAuth()
  
  // Wait for services to be ready
  await waitForServices()
  
  // Small delay to show "ready" state
  await new Promise(resolve => setTimeout(resolve, 500))
  
  servicesReady.value = true
})
</script>
