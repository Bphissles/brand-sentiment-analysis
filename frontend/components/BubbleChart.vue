<script setup lang="ts">
import * as d3 from 'd3'
import type { Cluster } from '~/types/models'

interface Props {
  clusters: Cluster[]
  width?: number
  height?: number
}

const props = withDefaults(defineProps<Props>(), {
  width: 800,
  height: 600
})

const emit = defineEmits<{
  (e: 'clusterClick', cluster: Cluster): void
}>()

const chartRef = ref<HTMLDivElement | null>(null)

// Sentiment color scale
const getSentimentColor = (sentiment: number): string => {
  if (sentiment >= 0.3) return '#22c55e' // green
  if (sentiment <= -0.3) return '#ef4444' // red
  return '#eab308' // yellow
}

// Draw the bubble chart
const drawChart = () => {
  if (!chartRef.value || !props.clusters.length) return

  // Clear previous chart
  d3.select(chartRef.value).selectAll('*').remove()

  const svg = d3.select(chartRef.value)
    .append('svg')
    .attr('width', props.width)
    .attr('height', props.height)
    .attr('viewBox', `0 0 ${props.width} ${props.height}`)

  // Create bubble data
  const bubbleData = props.clusters.map(cluster => ({
    ...cluster,
    value: cluster.postCount || 1
  }))

  // Create bubble layout
  const pack = d3.pack<typeof bubbleData[0]>()
    .size([props.width - 20, props.height - 20])
    .padding(15)

  const root = d3.hierarchy({ children: bubbleData } as any)
    .sum((d: any) => d.value || 1)

  const nodes = pack(root as any).leaves()

  // Create bubble groups
  const bubbles = svg.selectAll('.bubble')
    .data(nodes)
    .enter()
    .append('g')
    .attr('class', 'bubble')
    .attr('transform', (d: any) => `translate(${d.x + 10}, ${d.y + 10})`)
    .style('cursor', 'pointer')
    .on('click', (_event: any, d: any) => {
      emit('clusterClick', d.data)
    })

  // Add circles
  bubbles.append('circle')
    .attr('r', (d: any) => d.r)
    .attr('fill', (d: any) => getSentimentColor(d.data.sentiment || 0))
    .attr('fill-opacity', 0.7)
    .attr('stroke', (d: any) => getSentimentColor(d.data.sentiment || 0))
    .attr('stroke-width', 2)
    .on('mouseenter', function() {
      d3.select(this)
        .transition()
        .duration(200)
        .attr('fill-opacity', 0.9)
        .attr('stroke-width', 3)
    })
    .on('mouseleave', function() {
      d3.select(this)
        .transition()
        .duration(200)
        .attr('fill-opacity', 0.7)
        .attr('stroke-width', 2)
    })

  // Add labels
  bubbles.append('text')
    .attr('text-anchor', 'middle')
    .attr('dy', '-0.5em')
    .attr('fill', 'white')
    .attr('font-weight', 'bold')
    .attr('font-size', (d: any) => Math.min(d.r / 3, 16))
    .text((d: any) => d.data.label)
    .each(function(d: any) {
      // Truncate text if too long
      const text = d3.select(this)
      const maxWidth = d.r * 1.8
      let textLength = (this as SVGTextElement).getComputedTextLength()
      let textContent = d.data.label
      while (textLength > maxWidth && textContent.length > 0) {
        textContent = textContent.slice(0, -1)
        text.text(textContent + '...')
        textLength = (this as SVGTextElement).getComputedTextLength()
      }
    })

  // Add post count
  bubbles.append('text')
    .attr('text-anchor', 'middle')
    .attr('dy', '1em')
    .attr('fill', 'white')
    .attr('font-size', (d: any) => Math.min(d.r / 4, 14))
    .text((d: any) => `${d.data.postCount} posts`)

  // Add sentiment indicator
  bubbles.append('text')
    .attr('text-anchor', 'middle')
    .attr('dy', '2.5em')
    .attr('fill', 'white')
    .attr('font-size', (d: any) => Math.min(d.r / 5, 12))
    .text((d: any) => {
      const s = d.data.sentiment || 0
      if (s >= 0.3) return '😊 Positive'
      if (s <= -0.3) return '😟 Negative'
      return '😐 Neutral'
    })
}

// Watch for changes and redraw
watch(() => props.clusters, drawChart, { deep: true })

onMounted(() => {
  drawChart()
})
</script>

<template>
  <div ref="chartRef" class="bubble-chart"></div>
</template>

<style scoped>
.bubble-chart {
  width: 100%;
  display: flex;
  justify-content: center;
}

.bubble-chart :deep(svg) {
  overflow: visible;
}
</style>
