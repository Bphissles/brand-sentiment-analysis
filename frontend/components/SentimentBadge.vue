<script setup lang="ts">
interface Props {
  sentiment: number
  label?: string
  size?: 'sm' | 'md' | 'lg'
  showScore?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  showScore: true
})

const sentimentLabel = computed(() => {
  if (props.label) return props.label
  if (props.sentiment >= 0.3) return 'Positive'
  if (props.sentiment <= -0.3) return 'Negative'
  return 'Neutral'
})

const colorClasses = computed(() => {
  if (props.sentiment >= 0.3) return 'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400'
  if (props.sentiment <= -0.3) return 'bg-rose-50 dark:bg-rose-900/30 text-rose-700 dark:text-rose-400'
  return 'bg-amber-50 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'
})

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'sm': return 'px-2 py-0.5 text-xs'
    case 'lg': return 'px-4 py-2 text-base'
    default: return 'px-3 py-1 text-sm'
  }
})

const dotColor = computed(() => {
  if (props.sentiment >= 0.3) return 'bg-emerald-500'
  if (props.sentiment <= -0.3) return 'bg-rose-500'
  return 'bg-amber-500'
})
</script>

<template>
  <span 
    :class="[colorClasses, sizeClasses]" 
    class="inline-flex items-center gap-1.5 rounded-lg font-medium"
  >
    <span :class="dotColor" class="w-1.5 h-1.5 rounded-full"></span>
    <span>{{ sentimentLabel }}</span>
    <span v-if="showScore" class="opacity-60">{{ sentiment.toFixed(2) }}</span>
  </span>
</template>
