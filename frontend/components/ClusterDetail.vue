<script setup lang="ts">
import type { Cluster, Post } from '~/types/models'

interface Props {
  cluster: Cluster
  posts: Post[]
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
}>()
</script>

<template>
  <div class="fixed inset-0 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center z-50 p-4" @click.self="emit('close')">
    <div class="bg-white dark:bg-slate-800 rounded-2xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
      <!-- Header -->
      <div class="p-6 border-b border-slate-200 dark:border-slate-700 bg-gradient-to-r from-slate-50 dark:from-slate-800 to-white dark:to-slate-800">
        <div class="flex justify-between items-start">
          <div class="flex items-center gap-3">
            <div 
              class="w-4 h-4 rounded-full"
              :class="{
                'bg-emerald-500': cluster.sentiment >= 0.3,
                'bg-rose-500': cluster.sentiment <= -0.3,
                'bg-amber-500': cluster.sentiment > -0.3 && cluster.sentiment < 0.3
              }"
            ></div>
            <div>
              <h2 class="text-xl font-bold text-slate-800 dark:text-slate-100">{{ cluster.label }}</h2>
              <p class="text-sm text-slate-500 dark:text-slate-400 mt-0.5">{{ cluster.description }}</p>
            </div>
          </div>
          <button 
            @click="emit('close')"
            class="w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Stats -->
      <div class="px-6 py-4 border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900/50 grid grid-cols-3 gap-6">
        <div class="text-center">
          <p class="text-3xl font-bold text-slate-800 dark:text-slate-100">{{ cluster.postCount }}</p>
          <p class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mt-1">Posts</p>
        </div>
        <div class="text-center flex flex-col items-center justify-center">
          <SentimentBadge :sentiment="cluster.sentiment" size="lg" />
        </div>
        <div class="text-center">
          <p class="text-xs font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wide mb-2">Top Keywords</p>
          <div class="flex flex-wrap justify-center gap-1.5">
            <span 
              v-for="kw in (cluster.keywords || []).slice(0, 5)" 
              :key="kw"
              class="px-2 py-1 bg-cyan-50 dark:bg-cyan-900/30 text-cyan-700 dark:text-cyan-400 rounded-md text-xs font-medium"
            >
              {{ kw }}
            </span>
          </div>
        </div>
      </div>

      <!-- Posts List -->
      <div class="p-6 overflow-y-auto scrollbar-custom" style="max-height: 50vh;">
        <h3 class="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-4">{{ posts.length }} posts in this cluster</h3>
        <div class="space-y-3">
          <div 
            v-for="post in posts" 
            :key="post.id"
            class="p-4 border border-slate-200 dark:border-slate-700 rounded-xl hover:border-slate-300 dark:hover:border-slate-600 hover:shadow-sm transition-all"
          >
            <div class="flex justify-between items-start mb-2">
              <span class="font-medium text-slate-800 dark:text-slate-200">{{ post.author }}</span>
              <span 
                class="text-xs font-medium px-2 py-1 rounded-md"
                :class="{
                  'bg-sky-50 dark:bg-sky-900/30 text-sky-700 dark:text-sky-400': post.source === 'twitter',
                  'bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-400': post.source === 'youtube',
                  'bg-violet-50 dark:bg-violet-900/30 text-violet-700 dark:text-violet-400': post.source === 'forums'
                }"
              >
                {{ post.source === 'twitter' ? 'Twitter/X' : post.source === 'youtube' ? 'YouTube' : 'Forum' }}
              </span>
            </div>
            <p class="text-slate-600 dark:text-slate-300 text-sm leading-relaxed">{{ post.content }}</p>
            <div class="mt-3 flex flex-wrap items-center gap-2">
              <SentimentBadge 
                v-if="post.sentimentCompound !== undefined && post.sentimentCompound !== null" 
                :sentiment="post.sentimentCompound" 
                size="sm"
                :showScore="false"
              />
              <div v-if="post.sentimentCompound !== undefined && post.sentimentCompound !== null" class="text-xs text-slate-500 dark:text-slate-400">
                <span class="font-mono">Score: {{ post.sentimentCompound.toFixed(3) }}</span>
              </div>
            </div>
            <div v-if="post.publishedAt" class="mt-2 text-xs text-slate-400 dark:text-slate-500">
              Published: {{ new Date(post.publishedAt).toLocaleDateString() }} • 
              Fetched: {{ new Date(post.fetchedAt).toLocaleDateString() }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
