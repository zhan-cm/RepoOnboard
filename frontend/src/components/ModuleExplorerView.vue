<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  model: { type: Object, required: true },
  selectedModuleId: { type: String, default: '' }
})
const emit = defineEmits(['select'])
const query = ref('')
const filter = ref('all')
const contentTab = ref('components')

const filteredModules = computed(() => {
  const needle = query.value.trim().toLowerCase()
  return props.model.modules.filter((module) => {
    const matchesText = !needle || [module.label, module.gav, module.pomFileId, module.baseDirectory]
      .filter(Boolean).some((value) => value.toLowerCase().includes(needle))
    const matchesFilter = filter.value === 'all'
      || (filter.value === 'source' && (module.counts.sourceFiles ?? 0) > 0)
      || (filter.value === 'spring' && module.frameworks.includes('Spring Boot'))
    return matchesText && matchesFilter
  })
})

const selected = computed(() => props.model.modules.find(
  (module) => module.id === props.selectedModuleId) ?? props.model.modules[0] ?? null)
const content = computed(() => contentTab.value === 'components'
  ? selected.value?.components
  : selected.value?.entryPoints)

function shown(value) {
  return value ?? 'Unavailable'
}
</script>

<template>
  <div class="module-explorer">
    <section class="module-index" aria-label="Module index">
      <header class="module-index__header">
        <div>
          <p class="workbench-kicker">Project structure</p>
          <h1>Modules</h1>
        </div>
        <span class="module-count">{{ model.modules.length }}</span>
      </header>

      <label class="module-search">
        <span aria-hidden="true">⌕</span>
        <span class="sr-only">Search modules</span>
        <input v-model="query" type="search" placeholder="Search modules" />
      </label>
      <select v-model="filter" class="module-filter" aria-label="Filter modules">
        <option value="all">All modules</option>
        <option value="source">With source files</option>
        <option value="spring">Spring Boot</option>
      </select>

      <div v-if="!model.hierarchySupported" class="contract-note" role="note">
        <strong>Hierarchy unavailable</strong>
        <span>This report predates explicit aggregation relationships.</span>
      </div>

      <div v-if="filteredModules.length" class="module-tree">
        <button
          v-for="module in filteredModules"
          :key="module.id"
          class="module-row"
          :class="{ 'module-row--active': module.id === selected?.id }"
          :style="{ '--tree-depth': module.depth }"
          type="button"
          @click="emit('select', module.id)"
        >
          <span class="module-row__branch" aria-hidden="true">{{ module.depth ? '└' : '▣' }}</span>
          <span class="module-row__copy">
            <strong>{{ module.label }}</strong>
            <small>{{ shown(module.packaging) }}</small>
          </span>
          <span class="module-row__metric">{{ module.counts.sourceFiles ?? '—' }}</span>
        </button>
      </div>
      <div v-else class="module-index__empty">No modules match this search.</div>
    </section>

    <section v-if="selected" class="module-workspace" aria-label="Selected module details">
      <header class="module-hero">
        <div class="module-hero__icon" aria-hidden="true">▦</div>
        <div class="module-hero__copy">
          <p class="workbench-kicker">Selected module</p>
          <h2>{{ selected.label }}</h2>
          <p>{{ shown(selected.gav) }}</p>
        </div>
        <span class="packaging-badge">{{ shown(selected.packaging) }}</span>
      </header>

      <div class="module-stat-grid" aria-label="Module statistics">
        <article v-for="(value, key) in selected.counts" :key="key" class="module-stat">
          <strong>{{ value ?? '—' }}</strong>
          <span>{{ { sourceFiles: 'Source files', components: 'Components', endpoints: 'Endpoints', entryPoints: 'Entry points' }[key] }}</span>
        </article>
      </div>

      <div class="module-section-grid">
        <article class="module-card">
          <header><h3>Module metadata</h3><span>Confirmed</span></header>
          <dl class="module-facts">
            <div><dt>Group ID</dt><dd>{{ shown(selected.groupId) }}</dd></div>
            <div><dt>Artifact ID</dt><dd>{{ shown(selected.artifactId) }}</dd></div>
            <div><dt>Version</dt><dd>{{ shown(selected.version) }}</dd></div>
            <div><dt>POM path</dt><dd>{{ shown(selected.pomFileId) }}</dd></div>
            <div><dt>Base directory</dt><dd>{{ shown(selected.baseDirectory) }}</dd></div>
          </dl>
        </article>

        <article class="module-card">
          <header><h3>Technology</h3><span>From report</span></header>
          <div class="technology-list">
            <div v-for="item in selected.languageVersions" :key="`${item.kind}:${item.version}`">
              <span>{{ item.label }}</span><strong>{{ item.version }}</strong>
            </div>
            <div v-for="item in selected.frameworkVersions" :key="`${item.kind}:${item.version}`">
              <span>{{ item.label }}</span><strong>{{ item.version }}</strong>
            </div>
            <div v-for="framework in selected.frameworks.filter((name) => !selected.frameworkVersions.some((item) => item.label === name))" :key="framework">
              <span>{{ framework }}</span><strong>Version unavailable</strong>
            </div>
            <p v-if="!selected.languageVersions.length && !selected.frameworks.length" class="module-empty-copy">Technology information is unavailable.</p>
          </div>
        </article>
      </div>

      <article class="module-card module-card--roots">
        <header><h3>Source roots</h3><span>{{ selected.sourceRoots?.length ?? '—' }}</span></header>
        <ul v-if="selected.sourceRoots?.length" class="source-root-list">
          <li v-for="root in selected.sourceRoots" :key="root"><span aria-hidden="true">⌁</span><code>{{ root }}</code></li>
        </ul>
        <p v-else-if="selected.sourceRoots" class="module-empty-copy">No source roots were reported.</p>
        <p v-else class="module-empty-copy">Source roots are unavailable.</p>
      </article>

      <article class="module-card module-card--relationships">
        <header><h3>Module relationships</h3><span>Exact Maven coordinates</span></header>
        <div v-if="!model.relationshipsSupported" class="module-empty-copy">Internal module relationships are unavailable in this report schema.</div>
        <div v-else-if="selected.outgoing.length || selected.incoming.length" class="relationship-columns">
          <section>
            <h4>Depends on</h4>
            <ul><li v-for="item in selected.outgoing" :key="item.id"><strong>{{ item.targetLabel }}</strong><small>{{ item.declaredTarget }}</small></li></ul>
          </section>
          <section>
            <h4>Used by</h4>
            <ul><li v-for="item in selected.incoming" :key="item.id"><strong>{{ item.sourceLabel }}</strong><small>{{ item.declaredTarget }}</small></li></ul>
          </section>
        </div>
        <p v-else class="module-empty-copy">No confirmed internal module dependencies.</p>
      </article>

      <article class="module-card module-card--contents">
        <header class="content-header">
          <h3>Module contents</h3>
          <div class="content-tabs" role="tablist" aria-label="Module contents">
            <button type="button" :class="{ active: contentTab === 'components' }" @click="contentTab = 'components'">Components</button>
            <button type="button" :class="{ active: contentTab === 'entryPoints' }" @click="contentTab = 'entryPoints'">Entry points</button>
          </div>
        </header>
        <ul v-if="content?.length" class="content-list">
          <li v-for="item in content" :key="item.id">
            <span class="content-list__icon" aria-hidden="true">◇</span>
            <span><strong>{{ item.title }}</strong><small>{{ item.qualifiedName }}</small></span>
            <span class="content-kind">{{ item.kind }}</span>
            <code>{{ item.location ?? 'Location unavailable' }}</code>
          </li>
        </ul>
        <p v-else-if="content" class="module-empty-copy">No {{ contentTab === 'components' ? 'components' : 'entry points' }} were reported for this module.</p>
        <p v-else class="module-empty-copy">Module contents are unavailable.</p>
      </article>
    </section>

    <section v-else class="module-workspace module-workspace--empty">
      <h1>No modules reported</h1>
      <p>The analysis report contains a known empty module collection.</p>
    </section>
  </div>
</template>
