<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AppShell from './components/AppShell.vue'
import ArchitectureInspector from './components/ArchitectureInspector.vue'
import ArchitectureWorkspaceView from './components/ArchitectureWorkspaceView.vue'
import ApiInspector from './components/ApiInspector.vue'
import ApiMapView from './components/ApiMapView.vue'
import InspectorPanel from './components/InspectorPanel.vue'
import ModuleExplorerView from './components/ModuleExplorerView.vue'
import ModuleInspector from './components/ModuleInspector.vue'
import OverviewView from './components/OverviewView.vue'
import PageLayout from './components/PageLayout.vue'
import SidebarNav from './components/SidebarNav.vue'
import StatePanel from './components/StatePanel.vue'
import SourceDetailView from './components/SourceDetailView.vue'
import SourceRelatedInspector from './components/SourceRelatedInspector.vue'
import StartHereInspector from './components/StartHereInspector.vue'
import StartHereView from './components/StartHereView.vue'
import {
  createArchitectureModel,
  createArchitectureScope,
  defaultArchitectureExploration
} from './lib/reportArchitecture.js'
import { createModuleExplorerModel } from './lib/reportModules.js'
import { createApiModel, createApiScope, defaultApiExploration } from './lib/reportEndpoints.js'
import { createSourceDetailModel } from './lib/reportSources.js'
import { createStartHereModel } from './lib/reportStartHere.js'
import { createSourceNavigationHost } from './lib/sourceNavigationHost.js'
import { CURRENT_REPORT_SCHEMA, reportSchemaCompatibility } from './lib/reportSchema.js'

const navigationItems = Object.freeze([
  { id: 'overview', label: 'Overview', glyph: 'O', disabled: false },
  { id: 'modules', label: 'Modules', glyph: 'M', disabled: false },
  { id: 'architecture', label: 'Architecture', glyph: 'A', active: false, disabled: false },
  { id: 'apis', label: 'APIs', glyph: '↗', active: false, disabled: false },
  { id: 'start-here', label: 'Start Here', glyph: 'S', active: false, disabled: false }
])

const report = ref(null)
const error = ref('')
const activePage = ref('overview')
const selectedModuleId = ref('')
const selectedArchitectureModuleId = ref('')
const architectureSelection = ref(null)
const architectureExploration = ref(defaultArchitectureExploration())
const apiSelection = ref(null)
const apiExploration = ref(defaultApiExploration())
const sourceSelection = ref(null)
const startHereModel = ref(null)
const startHereError = ref('')
const startHereLoading = ref(false)
const startHereExpanded = ref(false)
const selectedStartHereSourceFileId = ref('')
const sourceNavigationHost = createSourceNavigationHost()
const navigation = computed(() => navigationItems.map((item) => ({
  ...item,
  active: item.id === activePage.value
})))
const moduleModel = computed(() => report.value ? createModuleExplorerModel(report.value) : null)
const architectureModel = computed(() => report.value ? createArchitectureModel(report.value) : null)
const apiModel = computed(() => report.value ? createApiModel(report.value) : null)
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
const apiScope = computed(() => apiModel.value
  ? createApiScope(apiModel.value, apiExploration.value)
  : null)
const sourceModel = computed(() => report.value && sourceSelection.value
  ? createSourceDetailModel(report.value, sourceSelection.value)
  : null)
const workbench = computed(() => ['overview', 'modules', 'architecture', 'apis', 'start-here'].includes(activePage.value))
const selectedStartHereItem = computed(() => startHereModel.value?.items.find(
  (item) => item.sourceFileId === selectedStartHereSourceFileId.value) ?? null)

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

watch(apiModel, () => {
  apiSelection.value = null
  apiExploration.value = defaultApiExploration()
})

watch(apiScope, (scope) => {
  if (!scope || !apiSelection.value) return
  if (!scope.endpoints.some((item) => item.id === apiSelection.value.id)) {
    apiSelection.value = null
  }
})

function selectArchitectureModule(moduleId) {
  selectedArchitectureModuleId.value = moduleId
  architectureSelection.value = null
  architectureExploration.value = defaultArchitectureExploration()
}

function selectPage(pageId) {
  sourceSelection.value = null
  activePage.value = pageId
  if (pageId === 'start-here') void loadStartHere()
}

function openSource(selection) {
  sourceSelection.value = {
    ...selection,
    originPage: activePage.value
  }
}

async function loadStartHere(force = false) {
  if (!report.value || startHereLoading.value || startHereModel.value && !force) return
  startHereLoading.value = true
  startHereError.value = ''
  try {
    const response = await fetch('/api/start-here', {
      headers: { Accept: 'application/json' }
    })
    if (!response.ok) {
      throw new Error(`Reading guide request failed (${response.status})`)
    }
    startHereModel.value = createStartHereModel(report.value, await response.json())
    if (!startHereModel.value.items.some(
      (item) => item.sourceFileId === selectedStartHereSourceFileId.value)) {
      selectedStartHereSourceFileId.value = startHereModel.value.items[0]?.sourceFileId ?? ''
    }
  } catch (cause) {
    startHereModel.value = null
    selectedStartHereSourceFileId.value = ''
    startHereError.value = cause instanceof Error
      ? cause.message
      : 'The Start Here reading guide could not be loaded.'
  } finally {
    startHereLoading.value = false
  }
}

function exploreStartHereComponent(componentId) {
  const component = report.value?.components?.find((item) => item.id === componentId)
  if (!component) return
  sourceSelection.value = null
  selectedArchitectureModuleId.value = component.moduleId ?? architectureModel.value?.defaultModuleId ?? ''
  architectureExploration.value = defaultArchitectureExploration()
  architectureSelection.value = { type: 'component', id: component.id }
  activePage.value = 'architecture'
}

function exploreStartHereEndpoint(endpointId) {
  const endpoint = report.value?.endpoints?.find((item) => item.id === endpointId)
  if (!endpoint) return
  sourceSelection.value = null
  apiExploration.value = {
    ...defaultApiExploration(),
    moduleId: endpoint.moduleId ?? 'all'
  }
  apiSelection.value = { type: 'endpoint', id: endpoint.id }
  activePage.value = 'apis'
}

onMounted(async () => {
  try {
    const response = await fetch('/api/report', {
      headers: { Accept: 'application/json' }
    })
    if (!response.ok) {
      throw new Error(`Report request failed (${response.status})`)
    }
    const loadedReport = await response.json()
    if (!reportSchemaCompatibility(loadedReport?.schemaVersion).compatible) {
      throw new Error(`Unsupported report schema (${loadedReport?.schemaVersion ?? 'missing'}); this UI supports schema major ${CURRENT_REPORT_SCHEMA.split('.')[0]} and is packaged with ${CURRENT_REPORT_SCHEMA}`)
    }
    report.value = loadedReport
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : 'The analysis report could not be loaded.'
  }
})
</script>

<template>
  <AppShell :workbench="workbench">
    <template #sidebar>
      <SidebarNav :items="navigation" @select="selectPage" />
    </template>

    <template v-if="workbench" #topbar>
      <div class="workbench-topbar">
        <div><span>Repository</span><strong>{{ repositoryName }}</strong></div>
        <div class="workbench-topbar__summary">
          <span>{{ moduleModel?.modules.length ?? '—' }} modules</span>
          <span v-if="activePage === 'overview' || activePage === 'modules'">{{ report?.summary?.sourceFileCount ?? report?.sourceFiles?.length ?? '—' }} source files</span>
          <span v-else-if="activePage === 'architecture'">{{ architectureModel?.counts.components ?? '—' }} components</span>
          <span v-else-if="activePage === 'apis'">{{ apiModel?.counts.endpoints ?? '—' }} endpoints</span>
          <span v-else>{{ startHereModel?.totalItemCount ?? '—' }} recommended files</span>
          <span v-if="activePage === 'architecture'">{{ architectureModel?.counts.confirmedRelations ?? '—' }} confirmed relations</span>
          <span class="workbench-status" :class="`workbench-status--${statusTone}`">{{ analysisStatus || 'UNKNOWN' }}</span>
        </div>
      </div>
    </template>

    <template v-if="sourceSelection">
      <SourceDetailView
        v-if="sourceModel"
        :model="sourceModel"
        :host="sourceNavigationHost"
        @close="sourceSelection = null"
      />
    </template>

    <template v-else-if="activePage === 'modules'">
      <StatePanel v-if="error" variant="error" heading="Report unavailable" :message="error" />
      <StatePanel
        v-else-if="!report"
        variant="loading"
        heading="Loading module report"
        message="Connecting to the local RepoOnboard service…"
      />
      <ModuleExplorerView
        v-else
        :model="moduleModel"
        :selected-module-id="selectedModule?.id"
        @select="selectedModuleId = $event"
      />
    </template>

    <template v-else-if="activePage === 'architecture'">
      <StatePanel v-if="error" variant="error" heading="Report unavailable" :message="error" />
      <StatePanel
        v-else-if="!report"
        variant="loading"
        heading="Loading architecture report"
        message="Connecting to the local RepoOnboard service…"
      />
      <ArchitectureWorkspaceView
        v-else
        :model="architectureModel"
        :module-id="selectedArchitectureModuleId"
        :selection="architectureSelection"
        :scope="architectureScope"
        :exploration="architectureExploration"
        @select-module="selectArchitectureModule"
        @select="architectureSelection = $event"
        @update-exploration="architectureExploration = $event"
      />
    </template>

    <template v-else-if="activePage === 'apis'">
      <StatePanel v-if="error" variant="error" heading="Report unavailable" :message="error" />
      <StatePanel
        v-else-if="!report"
        variant="loading"
        heading="Loading endpoint report"
        message="Connecting to the local RepoOnboard service…"
      />
      <ApiMapView
        v-else
        :model="apiModel"
        :scope="apiScope"
        :exploration="apiExploration"
        :selection="apiSelection"
        @select="apiSelection = $event"
        @update-exploration="apiExploration = $event"
      />
    </template>

    <template v-else-if="activePage === 'start-here'">
      <StatePanel v-if="error" variant="error" heading="Report unavailable" :message="error" />
      <StatePanel
        v-else-if="!report"
        variant="loading"
        heading="Loading analysis report"
        message="Connecting to the local RepoOnboard service…"
      />
      <StatePanel
        v-else-if="startHereError"
        variant="error"
        heading="Reading guide unavailable"
        :message="startHereError"
      >
        <template #actions><button type="button" @click="loadStartHere(true)">Retry reading guide</button></template>
      </StatePanel>
      <StatePanel
        v-else-if="startHereLoading || !startHereModel"
        variant="loading"
        heading="Loading Start Here"
        message="Generating the source-backed reading path from this report snapshot…"
      />
      <StartHereView
        v-else
        :model="startHereModel"
        :expanded="startHereExpanded"
        :selected-source-file-id="selectedStartHereSourceFileId"
        @select="selectedStartHereSourceFileId = $event"
        @update-expanded="startHereExpanded = $event"
      />
    </template>

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
        v-else-if="activePage === 'architecture' && !sourceSelection"
        :model="architectureModel"
        :scope="architectureScope"
        :selection="architectureSelection"
        @open-source="openSource"
      />
      <ApiInspector
        v-else-if="activePage === 'apis' && !sourceSelection"
        :model="apiModel"
        :scope="apiScope"
        :selection="apiSelection"
        @open-source="openSource"
      />
      <SourceRelatedInspector v-else-if="sourceSelection && sourceModel" :model="sourceModel" />
      <StartHereInspector
        v-else-if="activePage === 'start-here' && startHereModel"
        :model="startHereModel"
        :item="selectedStartHereItem"
        @explore-component="exploreStartHereComponent"
        @explore-endpoint="exploreStartHereEndpoint"
      />
      <InspectorPanel
        v-else-if="activePage === 'start-here'"
        title="Reading guide"
        description="Start Here is loaded independently so the other report views remain available if its projection cannot be read."
      >
        <StatePanel
          :variant="startHereError ? 'error' : 'loading'"
          compact
          :heading="startHereError ? 'Guide unavailable' : 'Preparing guide'"
          :message="startHereError || 'Source-backed recommendations will appear here when the projection is ready.'"
        />
      </InspectorPanel>
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
