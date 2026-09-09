<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <aside class="inspector" :aria-label="title">
    <div class="inspector__header">
      <p class="eyebrow">Inspector</p>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </div>
    <div class="inspector__body">
      <slot />
    </div>
  </aside>
</template>
