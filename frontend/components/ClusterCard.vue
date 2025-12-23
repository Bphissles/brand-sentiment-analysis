<script setup lang="ts">
import type { Cluster } from '~/types/models'

interface Props {
  cluster: Cluster
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'click', cluster: Cluster): void
}>()
</script>

<template>
  <div 
    class="bg-white rounded-lg shadow-md p-4 cursor-pointer hover:shadow-lg transition-shadow border-l-4"
    :class="{
      'border-green-500': cluster.sentiment >= 0.3,
      'border-red-500': cluster.sentiment <= -0.3,
      'border-yellow-500': cluster.sentiment > -0.3 && cluster.sentiment < 0.3
    }"
    @click="emit('click', cluster)"
  >
    <div class="flex justify-between items-start mb-2">
      <h3 class="font-semibold text-gray-900">{{ cluster.label }}</h3>
      <SentimentBadge :sentiment="cluster.sentiment" size="sm" />
    </div>
    
    <p class="text-sm text-gray-600 mb-3">{{ cluster.description }}</p>
    
    <div class="flex items-center justify-between text-sm">
      <span class="text-gray-500">
        <span class="font-medium text-gray-700">{{ cluster.postCount }}</span> posts
      </span>
      <div class="flex flex-wrap gap-1">
        <span 
          v-for="keyword in (cluster.keywords || []).slice(0, 3)" 
          :key="keyword"
          class="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs"
        >
          {{ keyword }}
        </span>
      </div>
    </div>
  </div>
</template>
