<script setup>
defineProps({
  eyebrow: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  status: {
    type: String,
    default: ''
  },
  statusTone: {
    type: String,
    default: 'neutral',
    validator: (value) => ['neutral', 'success', 'warning', 'danger'].includes(value)
  }
})
</script>

<template>
  <header class="context-header">
    <div class="context-header__copy">
      <p class="eyebrow">{{ eyebrow }}</p>
      <div class="context-header__title-row">
        <h1>{{ title }}</h1>
        <span v-if="status" class="status-badge" :class="`status-badge--${statusTone}`">
          {{ status }}
        </span>
      </div>
      <p v-if="description" class="context-header__description">{{ description }}</p>
      <div v-if="$slots.metadata" class="context-header__metadata">
        <slot name="metadata" />
      </div>
    </div>
    <div v-if="$slots.actions" class="context-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>
