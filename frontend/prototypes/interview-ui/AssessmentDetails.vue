<script setup lang="ts">
import { computed } from 'vue'
import type { Dimension } from './resume'

const props = defineProps<{ rows: readonly Dimension[]; headingId: string }>()

const center = { x: 224, y: 153 }
const radius = 104
const ticks = [25, 50, 75, 100]
const lowestScore = computed(() => Math.min(...props.rows.map((row) => row.score)))

const axes = computed(() => props.rows.map((row, index) => {
  const angle = ((2 * Math.PI * index) / props.rows.length) - Math.PI / 2
  const direction = { x: Math.cos(angle), y: Math.sin(angle) }
  const labelRadius = radius + 17

  return {
    ...row,
    direction,
    end: { x: center.x + direction.x * radius, y: center.y + direction.y * radius },
    point: { x: center.x + direction.x * radius * row.score / 100, y: center.y + direction.y * radius * row.score / 100 },
    label: {
      x: center.x + direction.x * labelRadius,
      y: center.y + direction.y * labelRadius,
      anchor: Math.abs(direction.x) < 0.15 ? 'middle' : direction.x > 0 ? 'start' : 'end',
    },
  }
}))

const grid = computed(() => ticks.map((score) => {
  const distance = radius * score / 100
  const points = axes.value.map(({ direction }) => `${center.x + direction.x * distance},${center.y + direction.y * distance}`).join(' ')
  return { score, points, y: center.y - distance }
}))
const profile = computed(() => axes.value.map(({ point }) => `${point.x},${point.y}`).join(' '))
</script>

<template>
  <section class="assessment-details" :aria-labelledby="headingId">
    <div class="section-heading assessment-heading">
      <h2 :id="headingId">多维度评分</h2>
      <span>每项满分 100</span>
    </div>
    <template v-if="rows.length">
      <svg class="score-radar" viewBox="0 0 448 290" aria-hidden="true" focusable="false">
        <polygon v-for="tick in grid" :key="tick.score" :points="tick.points" class="radar-grid" />
        <line v-for="axis in axes" :key="axis.id" :x1="center.x" :y1="center.y" :x2="axis.end.x" :y2="axis.end.y" class="radar-axis" />
        <polygon :points="profile" class="radar-profile" />
        <text v-for="tick in grid.slice(0, -1)" :key="tick.score" :x="center.x + 8" :y="tick.y + 3" class="radar-tick">{{ tick.score }}</text>
        <g v-for="axis in axes" :key="axis.id" :class="{ 'radar-low': axis.score === lowestScore }">
          <circle :cx="axis.point.x" :cy="axis.point.y" r="4" class="radar-point" />
          <text :x="axis.label.x" :y="axis.label.y - 3" :text-anchor="axis.label.anchor" class="radar-name">{{ axis.dimension }}</text>
          <text :x="axis.label.x" :y="axis.label.y + 17" :text-anchor="axis.label.anchor" class="radar-score">{{ axis.score }}<tspan v-if="axis.score === lowestScore" class="radar-minimum"> 最低</tspan></text>
        </g>
      </svg>
      <ul class="visually-hidden">
        <li v-for="row in rows" :key="row.id">{{ row.dimension }} {{ row.score }} 分<span v-if="row.score === lowestScore">，最低分</span></li>
      </ul>
    </template>
    <p v-else class="analysis-empty">简历分析完成后显示各维度的评分。</p>
  </section>
</template>
