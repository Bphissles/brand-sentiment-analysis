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
  <div class="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-5 hover:shadow-md transition-shadow">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide">{{ title }}</p>
        <p class="text-2xl font-bold text-slate-800 dark:text-slate-100 mt-1">{{ value }}</p>
      </div>
      <div 
        v-if="trend"
        class="w-10 h-10 rounded-xl flex items-center justify-center"
        :class="{
          'bg-emerald-50 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400': trend === 'up',
          'bg-rose-50 dark:bg-rose-900/30 text-rose-600 dark:text-rose-400': trend === 'down',
          'bg-slate-100 dark:bg-slate-700 text-slate-500 dark:text-slate-400': trend === 'neutral'
        }"
      >
        <svg v-if="trend === 'up'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
        </svg>
        <svg v-else-if="trend === 'down'" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
        </svg>
        <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 12h14" />
        </svg>
      </div>
    </div>
  </div>
</template>
