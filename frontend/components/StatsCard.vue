<script setup lang="ts">
interface Props {
  title: string
  value: string | number
  icon?: string
  trend?: 'up' | 'down' | 'neutral'
}

defineProps<Props>()
</script>

<template>
  <div class="bg-white rounded-lg shadow p-4">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm text-gray-500">{{ title }}</p>
        <p class="text-2xl font-bold text-gray-900">{{ value }}</p>
      </div>
      <div 
        v-if="trend"
        class="w-10 h-10 rounded-full flex items-center justify-center"
        :class="{
          'bg-green-100 text-green-600': trend === 'up',
          'bg-red-100 text-red-600': trend === 'down',
          'bg-gray-100 text-gray-600': trend === 'neutral'
        }"
      >
        <span v-if="trend === 'up'">↑</span>
        <span v-else-if="trend === 'down'">↓</span>
        <span v-else>→</span>
      </div>
    </div>
  </div>
</template>
