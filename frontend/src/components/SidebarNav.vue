<script setup>
defineProps({
  items: {
    type: Array,
    required: true
  },
  repositoryName: {
    type: String,
    default: ''
  },
  moduleCount: {
    type: [Number, String],
    default: ''
  },
  frameworks: {
    type: String,
    default: 'Java 17 • Spring Boot • Maven'
  }
})

defineEmits(['select'])
</script>

<template>
  <aside class="sidebar" aria-label="RepoOnboard navigation">
    <div class="sidebar__top">
      <div class="sidebar__brand" aria-label="RepoOnboard">
        <div class="sidebar__brand-logo">
          <svg class="sidebar__mark" viewBox="0 0 32 32" aria-hidden="true">
            <path d="M7 7h18v5H7zM7 14h12v5H7zM7 21h18v5H7z" />
          </svg>
          <span class="sidebar__brand-name">RepoOnboard</span>
        </div>
        <span class="sidebar__version-badge">v0.1.0-local</span>
      </div>

      <div v-if="repositoryName" class="sidebar__target-repo">
        <div class="sidebar__target-repo-header">
          <span class="sidebar__target-repo-label">Target Repository</span>
          <span class="sidebar__target-repo-icon" aria-hidden="true">⌥</span>
        </div>
        <div class="sidebar__target-repo-name" :title="repositoryName">
          {{ repositoryName }}
        </div>
        <div class="sidebar__target-repo-meta">
          {{ frameworks }}
        </div>
      </div>

      <div class="sidebar__label">Perspectives</div>
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
          <span class="nav-item__label">{{ item.label }}</span>
          <span v-if="item.disabled" class="nav-item__soon">Soon</span>
        </button>
      </nav>
    </div>

    <div class="sidebar__footer">
      <div class="sidebar__engine-status">
        <div class="sidebar__engine-header">
          <span class="sidebar__engine-title">AST Engine</span>
          <span class="sidebar__engine-badge">
            <span class="status-dot" aria-hidden="true"></span>
            <span>Ready</span>
          </span>
        </div>
        <div class="sidebar__engine-detail">
          {{ moduleCount ? `${moduleCount} modules analyzed` : 'Local workspace' }}
        </div>
      </div>
      <div class="sidebar__runtime-info">
        <span>port 8080</span>
        <span>•</span>
        <span>file-backed</span>
      </div>
    </div>
  </aside>
</template>
