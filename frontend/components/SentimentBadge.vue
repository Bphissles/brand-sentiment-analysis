<script setup lang="ts">
interface Props {
  sentiment: number
  label?: string
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md'
})

const sentimentLabel = computed(() => {
  if (props.label) return props.label
  if (props.sentiment >= 0.3) return 'Positive'
  if (props.sentiment <= -0.3) return 'Negative'
  return 'Neutral'
})

const colorClasses = computed(() => {
  if (props.sentiment >= 0.3) return 'bg-green-100 text-green-800 border-green-300'
  if (props.sentiment <= -0.3) return 'bg-red-100 text-red-800 border-red-300'
  return 'bg-yellow-100 text-yellow-800 border-yellow-300'
})

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'sm': return 'px-2 py-0.5 text-xs'
    case 'lg': return 'px-4 py-2 text-base'
    default: return 'px-3 py-1 text-sm'
  }
})

const emoji = computed(() => {
  if (props.sentiment >= 0.3) return '😊'
  if (props.sentiment <= -0.3) return '😟'
  return '😐'
})
</script>

<template>
  <span 
    :class="[colorClasses, sizeClasses]" 
    class="inline-flex items-center gap-1 rounded-full border font-medium"
  >
    <span>{{ emoji }}</span>
    <span>{{ sentimentLabel }}</span>
    <span class="opacity-60">({{ sentiment.toFixed(2) }})</span>
  </span>
</template>
