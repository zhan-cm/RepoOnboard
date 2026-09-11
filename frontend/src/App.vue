<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AppShell from './components/AppShell.vue'
import ArchitectureInspector from './components/ArchitectureInspector.vue'
import ArchitectureWorkspaceView from './components/ArchitectureWorkspaceView.vue'
import InspectorPanel from './components/InspectorPanel.vue'
import ModuleExplorerView from './components/ModuleExplorerView.vue'
import ModuleInspector from './components/ModuleInspector.vue'
import OverviewView from './components/OverviewView.vue'
import PageLayout from './components/PageLayout.vue'
import SidebarNav from './components/SidebarNav.vue'
import StatePanel from './components/StatePanel.vue'
import {
  createArchitectureModel,
  createArchitectureScope,
  defaultArchitectureExploration
} from './lib/reportArchitecture.js'
import { createModuleExplorerModel } from './lib/reportModules.js'

const navigationItems = Object.freeze([
  { id: 'overview', label: 'Overview', glyph: 'O', disabled: false },
  { id: 'modules', label: 'Modules', glyph: 'M', disabled: false },
  { id: 'architecture', label: 'Architecture', glyph: 'A', active: false, disabled: false },
  { id: 'apis', label: 'APIs', glyph: '↗', active: false, disabled: true },
  { id: 'start-here', label: 'Start Here', glyph: 'S', active: false, disabled: true }
])

const report = ref(null)
const error = ref('')
const activePage = ref('overview')
const selectedModuleId = ref('')
const selectedArchitectureModuleId = ref('')
const architectureSelection = ref(null)
const architectureExploration = ref(defaultArchitectureExploration())
const navigation = computed(() => navigationItems.map((item) => ({
  ...item,
  active: item.id === activePage.value
})))
const moduleModel = computed(() => report.value ? createModuleExplorerModel(report.value) : null)
const architectureModel = computed(() => report.value ? createArchitectureModel(report.value) : null)
const selectedModule = computed(() => moduleModel.value?.modules.find(
  (module) => module.id === selectedModuleId.value) ?? moduleModel.value?.modules[0] ?? null)
const architectureScope = computed(() => architectureModel.value
  ? createArchitectureScope(architectureModel.value, selectedArchitectureModuleId.value, {
      ...architectureExploration.value,
      selectedComponentId: architectureSelection.value?.type === 'component'
        ? architectureSelection.value.id
        : null
    })
  : null)
const workbench = computed(() => activePage.value === 'modules' || activePage.value === 'architecture')

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

watch(architectureModel, (model) => {
  if (!model?.modules.some((module) => module.id === selectedArchitectureModuleId.value)) {
    selectedArchitectureModuleId.value = model?.defaultModuleId ?? ''
    architectureSelection.value = null
    architectureExploration.value = defaultArchitectureExploration()
  }
})

watch(architectureScope, (scope) => {
  if (!scope || !architectureSelection.value) return
  const values = architectureSelection.value.type === 'component' ? scope.nodes : scope.edges
  if (!values.some((item) => item.id === architectureSelection.value.id)) {
    architectureSelection.value = null
    if (architectureExploration.value.neighborhood) {
      architectureExploration.value = {
        ...architectureExploration.value,
        neighborhood: false
      }
    }
  }
})

function selectArchitectureModule(moduleId) {
  selectedArchitectureModuleId.value = moduleId
  architectureSelection.value = null
  architectureExploration.value = defaultArchitectureExploration()
}

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
  <AppShell :workbench="workbench">
    <template #sidebar>
      <SidebarNav :items="navigation" @select="activePage = $event" />
    </template>

    <template v-if="workbench" #topbar>
      <div class="workbench-topbar">
        <div><span>Repository</span><strong>{{ repositoryName }}</strong></div>
        <div class="workbench-topbar__summary">
          <span>{{ moduleModel?.modules.length ?? '—' }} modules</span>
          <span v-if="activePage === 'modules'">{{ report?.summary?.sourceFileCount ?? report?.sourceFiles?.length ?? '—' }} source files</span>
          <span v-else>{{ architectureModel?.counts.components ?? '—' }} components</span>
          <span v-if="activePage === 'architecture'">{{ architectureModel?.counts.confirmedRelations ?? '—' }} confirmed relations</span>
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

    <StatePanel
      v-else-if="activePage === 'architecture' && error"
      variant="error"
      heading="Report unavailable"
      :message="error"
    />
    <StatePanel
      v-else-if="activePage === 'architecture' && !report"
      variant="loading"
      heading="Loading architecture report"
      message="Connecting to the local RepoOnboard service…"
    />
    <ArchitectureWorkspaceView
      v-else-if="activePage === 'architecture'"
      :model="architectureModel"
      :module-id="selectedArchitectureModuleId"
      :selection="architectureSelection"
      :scope="architectureScope"
      :exploration="architectureExploration"
      @select-module="selectArchitectureModule"
      @select="architectureSelection = $event"
      @update-exploration="architectureExploration = $event"
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
      <ArchitectureInspector
        v-else-if="activePage === 'architecture'"
        :model="architectureModel"
        :scope="architectureScope"
        :selection="architectureSelection"
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
