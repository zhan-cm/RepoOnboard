<template>
  <div class="app-shell">
    <a class="skip-link" href="#main-content">Skip to workspace</a>
    <slot name="sidebar" />
    <main id="main-content" class="app-shell__main" tabindex="-1">
      <slot />
    </main>
    <slot name="inspector" />
  </div>
</template>
