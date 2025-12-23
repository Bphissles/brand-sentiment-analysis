<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
    <div class="text-center">
      <!-- Animated Logo/Icon -->
      <div class="mb-8 flex justify-center">
        <div class="relative">
          <div class="h-24 w-24 rounded-full border-4 border-blue-500/30"></div>
          <div class="absolute inset-0 h-24 w-24 animate-spin rounded-full border-4 border-transparent border-t-blue-500"></div>
          <div class="absolute inset-2 flex items-center justify-center">
            <svg class="h-12 w-12 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
        </div>
      </div>

      <!-- Title -->
      <h1 class="mb-4 text-3xl font-bold text-white">
        Peterbilt Sentiment Analyzer
      </h1>

      <!-- Loading Message -->
      <p class="mb-6 text-lg text-blue-200">
        {{ message }}
      </p>

      <!-- Progress Dots -->
      <div class="flex justify-center space-x-2">
        <div class="h-3 w-3 animate-bounce rounded-full bg-blue-500" style="animation-delay: 0ms"></div>
        <div class="h-3 w-3 animate-bounce rounded-full bg-blue-400" style="animation-delay: 150ms"></div>
        <div class="h-3 w-3 animate-bounce rounded-full bg-blue-300" style="animation-delay: 300ms"></div>
      </div>

      <!-- Service Status -->
      <div v-if="showStatus" class="mt-8 space-y-2 text-sm text-slate-300">
        <div class="flex items-center justify-center space-x-2">
          <div :class="statusClass('backend')"></div>
          <span>Backend API</span>
        </div>
        <div class="flex items-center justify-center space-x-2">
          <div :class="statusClass('ml')"></div>
          <span>ML Engine</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  message?: string
  showStatus?: boolean
  backendReady?: boolean
  mlReady?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  message: 'Initializing services...',
  showStatus: false,
  backendReady: false,
  mlReady: false
})

const statusClass = (service: 'backend' | 'ml') => {
  const isReady = service === 'backend' ? props.backendReady : props.mlReady
  return [
    'h-2 w-2 rounded-full',
    isReady ? 'bg-green-500' : 'bg-yellow-500 animate-pulse'
  ]
}
</script>
