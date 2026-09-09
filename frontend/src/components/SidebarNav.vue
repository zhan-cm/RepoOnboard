<script setup>
defineProps({
  items: {
    type: Array,
    required: true
  }
})

defineEmits(['select'])
</script>

<template>
  <aside class="sidebar" aria-label="RepoOnboard navigation">
    <div class="sidebar__brand" aria-label="RepoOnboard">
      <svg class="sidebar__mark" viewBox="0 0 32 32" aria-hidden="true">
        <path d="M7 7h18v5H7zM7 14h12v5H7zM7 21h18v5H7z" />
      </svg>
      <span>RepoOnboard</span>
    </div>

    <p class="sidebar__label">Explore</p>
    <nav class="sidebar__nav" aria-label="Project views">
      <button
        v-for="item in items"
        :key="item.id"
        class="nav-item"
        :class="{ 'nav-item--active': item.active }"
        type="button"
        :disabled="item.disabled"
        :aria-current="item.active ? 'page' : undefined"
        @click="$emit('select', item.id)"
      >
        <span class="nav-item__glyph" aria-hidden="true">{{ item.glyph }}</span>
        <span>{{ item.label }}</span>
        <span v-if="item.disabled" class="nav-item__soon">Soon</span>
      </button>
    </nav>

    <div class="sidebar__footer">
      <span class="status-dot" aria-hidden="true"></span>
      <span>Local workspace</span>
    </div>
  </aside>
</template>
