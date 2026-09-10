<script setup>
defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: Number,
    default: null
  },
  description: {
    type: String,
    default: ''
  },
  compact: {
    type: Boolean,
    default: false
  }
})
</script>

<template>
  <article class="metric-card" :class="{ 'metric-card--compact': compact }">
    <p class="metric-card__label">{{ label }}</p>
    <p v-if="value !== null" class="metric-card__value">{{ value.toLocaleString() }}</p>
    <p v-else class="metric-card__unknown">Not available</p>
    <p v-if="description" class="metric-card__description">{{ description }}</p>
  </article>
</template>
