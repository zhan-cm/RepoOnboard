<script setup>
import { onMounted, ref } from 'vue'

const report = ref(null)
const error = ref('')

onMounted(async () => {
  try {
    const response = await fetch('/api/report', {
      headers: { Accept: 'application/json' }
    })
    if (!response.ok) {
      throw new Error(`Report request failed (${response.status})`)
    }
    report.value = await response.json()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'The analysis report could not be loaded.'
  }
})
</script>

<template>
  <div class="app-frame">
    <aside aria-label="RepoOnboard navigation">
      <div class="brand">RepoOnboard</div>
      <nav>
        <span aria-current="page">Workspace</span>
        <span aria-disabled="true">Overview</span>
        <span aria-disabled="true">Modules</span>
        <span aria-disabled="true">Architecture</span>
        <span aria-disabled="true">APIs</span>
        <span aria-disabled="true">Start Here</span>
      </nav>
    </aside>

    <main>
      <header>
        <p>Repository context</p>
        <h1 v-if="report">{{ report.project.name }}</h1>
        <h1 v-else>Loading repository…</h1>
      </header>

      <section v-if="error" class="message" role="alert">
        <h2>Report unavailable</h2>
        <p>{{ error }}</p>
      </section>

      <section v-else-if="!report" class="message" aria-live="polite">
        <h2>Loading analysis report</h2>
        <p>Connecting to the local RepoOnboard service…</p>
      </section>

      <section v-else class="message" aria-live="polite">
        <h2>Workspace ready</h2>
        <p>The local analysis report is connected.</p>
        <dl>
          <div>
            <dt>Analysis status</dt>
            <dd>{{ report.status }}</dd>
          </div>
          <div>
            <dt>Report schema</dt>
            <dd>{{ report.schemaVersion }}</dd>
          </div>
        </dl>
      </section>
    </main>
  </div>
</template>

<style>
:root {
  color: #e5e7eb;
  background: #111827;
  font-family: system-ui, sans-serif;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
}

.app-frame {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 14rem 1fr;
}

aside {
  padding: 1.25rem;
  border-right: 1px solid #374151;
}

.brand {
  margin-bottom: 1.5rem;
  font-weight: 700;
}

nav {
  display: grid;
  gap: 0.75rem;
}

nav span[aria-disabled='true'] {
  color: #6b7280;
}

main {
  padding: 2rem;
}

header p {
  margin: 0;
  color: #9ca3af;
}

header h1 {
  margin-top: 0.25rem;
}

.message {
  max-width: 42rem;
  margin-top: 2rem;
  padding: 1.5rem;
  border: 1px solid #374151;
  border-radius: 0.5rem;
  background: #1f2937;
}

dl div {
  display: flex;
  gap: 0.5rem;
}

dt {
  color: #9ca3af;
}

dd {
  margin: 0;
}

@media (max-width: 720px) {
  .app-frame {
    grid-template-columns: 1fr;
  }

  aside {
    border-right: 0;
    border-bottom: 1px solid #374151;
  }
}
</style>
