<script setup>
import ContextHeader from './ContextHeader.vue'

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
    default: 'neutral'
  }
})
</script>

<template>
  <div class="page-layout">
    <ContextHeader
      :eyebrow="eyebrow"
      :title="title"
      :description="description"
      :status="status"
      :status-tone="statusTone"
    >
      <template v-if="$slots.metadata" #metadata>
        <slot name="metadata" />
      </template>
      <template v-if="$slots.actions" #actions>
        <slot name="actions" />
      </template>
    </ContextHeader>
    <div class="page-layout__content">
      <slot />
    </div>
  </div>
</template>
