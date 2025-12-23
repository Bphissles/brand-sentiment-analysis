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
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
      <!-- Header -->
      <div class="p-4 border-b flex justify-between items-center bg-gray-50">
        <div>
          <h2 class="text-xl font-bold text-gray-900">{{ cluster.label }}</h2>
          <p class="text-sm text-gray-500">{{ cluster.description }}</p>
        </div>
        <button 
          @click="emit('close')"
          class="text-gray-400 hover:text-gray-600 text-2xl"
        >
          ×
        </button>
      </div>

      <!-- Stats -->
      <div class="p-4 border-b bg-gray-50 grid grid-cols-3 gap-4">
        <div class="text-center">
          <p class="text-2xl font-bold text-gray-900">{{ cluster.postCount }}</p>
          <p class="text-sm text-gray-500">Posts</p>
        </div>
        <div class="text-center">
          <SentimentBadge :sentiment="cluster.sentiment" size="lg" />
        </div>
        <div class="text-center">
          <p class="text-sm text-gray-500">Top Keywords</p>
          <div class="flex flex-wrap justify-center gap-1 mt-1">
            <span 
              v-for="kw in (cluster.keywords || []).slice(0, 5)" 
              :key="kw"
              class="px-2 py-0.5 bg-blue-100 text-blue-700 rounded text-xs"
            >
              {{ kw }}
            </span>
          </div>
        </div>
      </div>

      <!-- Posts List -->
      <div class="p-4 overflow-y-auto" style="max-height: 50vh;">
        <h3 class="font-semibold text-gray-700 mb-3">Posts in this cluster</h3>
        <div class="space-y-3">
          <div 
            v-for="post in posts" 
            :key="post.id"
            class="p-3 border rounded-lg hover:bg-gray-50"
          >
            <div class="flex justify-between items-start mb-2">
              <span class="font-medium text-gray-900">{{ post.author }}</span>
              <span class="text-xs text-gray-400 px-2 py-0.5 bg-gray-100 rounded">
                {{ post.source }}
              </span>
            </div>
            <p class="text-gray-700 text-sm">{{ post.content }}</p>
            <div class="mt-2 flex items-center gap-2">
              <SentimentBadge 
                v-if="post.sentiment" 
                :sentiment="post.sentiment.compound" 
                size="sm" 
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
