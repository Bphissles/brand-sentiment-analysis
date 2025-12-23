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
    class="bg-white dark:bg-slate-800 rounded-xl shadow-sm border border-slate-200 dark:border-slate-700 p-5 cursor-pointer hover:shadow-md hover:border-slate-300 dark:hover:border-slate-600 transition-all group"
    @click="emit('click', cluster)"
  >
    <div class="flex justify-between items-start mb-3">
      <div class="flex items-center gap-2">
        <div 
          class="w-3 h-3 rounded-full"
          :class="{
            'bg-emerald-500': cluster.sentiment >= 0.3,
            'bg-rose-500': cluster.sentiment <= -0.3,
            'bg-amber-500': cluster.sentiment > -0.3 && cluster.sentiment < 0.3
          }"
        ></div>
        <h3 class="font-semibold text-slate-800 dark:text-slate-100 group-hover:text-cyan-600 dark:group-hover:text-cyan-400 transition-colors">{{ cluster.label }}</h3>
      </div>
      <span class="text-xs font-medium text-slate-400 dark:text-slate-500">{{ cluster.postCount }} posts</span>
    </div>
    
    <p class="text-sm text-slate-500 dark:text-slate-400 mb-4 line-clamp-2">{{ cluster.description }}</p>
    
    <div class="flex items-center justify-between">
      <div class="flex flex-wrap gap-1.5">
        <span 
          v-for="keyword in (cluster.keywords || []).slice(0, 3)" 
          :key="keyword"
          class="px-2 py-1 bg-slate-100 dark:bg-slate-700 text-slate-600 dark:text-slate-300 rounded-md text-xs font-medium"
        >
          {{ keyword }}
        </span>
      </div>
      <SentimentBadge :sentiment="cluster.sentiment" size="sm" />
    </div>
  </div>
</template>
