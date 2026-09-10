<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AppShell from './components/AppShell.vue'
import InspectorPanel from './components/InspectorPanel.vue'
import ModuleExplorerView from './components/ModuleExplorerView.vue'
import ModuleInspector from './components/ModuleInspector.vue'
import OverviewView from './components/OverviewView.vue'
import PageLayout from './components/PageLayout.vue'
import SidebarNav from './components/SidebarNav.vue'
import StatePanel from './components/StatePanel.vue'
import { createModuleExplorerModel } from './lib/reportModules.js'

const navigationItems = Object.freeze([
  { id: 'overview', label: 'Overview', glyph: 'O', disabled: false },
  { id: 'modules', label: 'Modules', glyph: 'M', disabled: false },
  { id: 'architecture', label: 'Architecture', glyph: 'A', active: false, disabled: true },
  { id: 'apis', label: 'APIs', glyph: '↗', active: false, disabled: true },
  { id: 'start-here', label: 'Start Here', glyph: 'S', active: false, disabled: true }
])

const report = ref(null)
const error = ref('')
const activePage = ref('overview')
const selectedModuleId = ref('')
const navigation = computed(() => navigationItems.map((item) => ({
  ...item,
  active: item.id === activePage.value
})))
const moduleModel = computed(() => report.value ? createModuleExplorerModel(report.value) : null)
const selectedModule = computed(() => moduleModel.value?.modules.find(
  (module) => module.id === selectedModuleId.value) ?? moduleModel.value?.modules[0] ?? null)

const repositoryName = computed(() => {
  if (!report.value) return 'Preparing workspace'
  return report.value.project?.name ?? 'Repository name unavailable'
})
const analysisStatus = computed(() => report.value?.status ?? '')
const statusTone = computed(() => {
  if (analysisStatus.value === 'SUCCESS') return 'success'
  if (analysisStatus.value === 'PARTIAL') return 'warning'
  if (analysisStatus.value === 'FAILED') return 'danger'
  return 'neutral'
})

watch(moduleModel, (model) => {
  if (!model?.modules.some((module) => module.id === selectedModuleId.value)) {
    selectedModuleId.value = model?.modules[0]?.id ?? ''
  }
})

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
  <AppShell :workbench="activePage === 'modules'">
    <template #sidebar>
      <SidebarNav :items="navigation" @select="activePage = $event" />
    </template>

    <template v-if="activePage === 'modules'" #topbar>
      <div class="workbench-topbar">
        <div><span>Repository</span><strong>{{ repositoryName }}</strong></div>
        <div class="workbench-topbar__summary">
          <span>{{ moduleModel?.modules.length ?? '—' }} modules</span>
          <span>{{ report?.summary?.sourceFileCount ?? report?.sourceFiles?.length ?? '—' }} source files</span>
          <span class="workbench-status" :class="`workbench-status--${statusTone}`">{{ analysisStatus || 'UNKNOWN' }}</span>
        </div>
      </div>
    </template>

    <StatePanel
      v-if="activePage === 'modules' && error"
      variant="error"
      heading="Report unavailable"
      :message="error"
    />
    <StatePanel
      v-else-if="activePage === 'modules' && !report"
      variant="loading"
      heading="Loading module report"
      message="Connecting to the local RepoOnboard service…"
    />
    <ModuleExplorerView
      v-else-if="activePage === 'modules'"
      :model="moduleModel"
      :selected-module-id="selectedModule?.id"
      @select="selectedModuleId = $event"
    />

    <PageLayout
      v-else
      eyebrow="Repository overview"
      :title="repositoryName"
      description="A confirmed snapshot of the repository, its technology footprint, and analysis coverage."
      :status="analysisStatus"
      :status-tone="statusTone"
    >
      <template v-if="report" #metadata>
        <span>Schema {{ report.schemaVersion }}</span>
        <span aria-hidden="true">•</span>
        <span>Loopback session</span>
      </template>

      <StatePanel
        v-if="error"
        variant="error"
        heading="Report unavailable"
        :message="error"
      />
      <StatePanel
        v-else-if="!report"
        variant="loading"
        heading="Loading analysis report"
        message="Connecting to the local RepoOnboard service…"
      />
      <OverviewView v-else :report="report" />
    </PageLayout>

    <template #inspector>
      <ModuleInspector
        v-if="activePage === 'modules'"
        :module="selectedModule"
        :status="analysisStatus || 'UNKNOWN'"
        :schema-version="report?.schemaVersion"
      />
      <InspectorPanel
        v-else
        title="About this overview"
        description="Every visible value comes from the current report or a deterministic count of its entities."
      >
        <dl v-if="report" class="inline-facts inline-facts--inspector">
          <div>
            <dt>Analysis status</dt>
            <dd>{{ report.status ?? 'Not available' }}</dd>
          </div>
          <div>
            <dt>Report schema</dt>
            <dd>{{ report.schemaVersion ?? 'Not available' }}</dd>
          </div>
          <div>
            <dt>Build system</dt>
            <dd>{{ report.project?.buildSystem ?? 'Not available' }}</dd>
          </div>
        </dl>
        <StatePanel
          v-else
          variant="empty"
          compact
          heading="Overview not ready"
          message="Report facts will appear after the local analysis response is available."
        />
      </InspectorPanel>
    </template>
  </AppShell>
</template>
