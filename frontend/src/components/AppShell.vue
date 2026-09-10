<script setup>
defineProps({
  workbench: { type: Boolean, default: false }
})
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--workbench': workbench }">
    <a class="skip-link" href="#main-content">Skip to workspace</a>
    <slot name="sidebar" />
    <header v-if="$slots.topbar" class="app-shell__topbar">
      <slot name="topbar" />
    </header>
    <main id="main-content" class="app-shell__main" tabindex="-1">
      <slot />
    </main>
    <slot name="inspector" />
  </div>
</template>
