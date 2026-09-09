<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    required: true,
    validator: (value) => ['loading', 'empty', 'error', 'ready'].includes(value)
  },
  heading: {
    type: String,
    required: true
  },
  message: {
    type: String,
    required: true
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const role = computed(() => props.variant === 'error' ? 'alert' : 'status')
const live = computed(() => props.variant === 'error' ? 'assertive' : 'polite')
</script>

<template>
  <section
    class="state-panel"
    :class="[`state-panel--${variant}`, { 'state-panel--compact': compact }]"
    :role="role"
    :aria-live="live"
  >
    <div class="state-panel__icon" aria-hidden="true">
      <span v-if="variant === 'loading'" class="spinner"></span>
      <span v-else-if="variant === 'error'">!</span>
      <span v-else-if="variant === 'ready'">✓</span>
      <span v-else>·</span>
    </div>
    <div class="state-panel__copy">
      <p class="state-panel__kicker">{{ variant }}</p>
      <h2>{{ heading }}</h2>
      <p>{{ message }}</p>
      <div v-if="$slots.details" class="state-panel__details">
        <slot name="details" />
      </div>
      <div v-if="$slots.actions" class="state-panel__actions">
        <slot name="actions" />
      </div>
    </div>
  </section>
</template>
