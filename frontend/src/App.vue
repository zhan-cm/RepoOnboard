<script setup>
import { computed, onMounted, ref } from 'vue'
import AppShell from './components/AppShell.vue'
import InspectorPanel from './components/InspectorPanel.vue'
import PageLayout from './components/PageLayout.vue'
import SidebarNav from './components/SidebarNav.vue'
import StatePanel from './components/StatePanel.vue'

const navigation = Object.freeze([
  { id: 'workspace', label: 'Workspace', glyph: 'W', active: true, disabled: false },
  { id: 'overview', label: 'Overview', glyph: 'O', active: false, disabled: true },
  { id: 'modules', label: 'Modules', glyph: 'M', active: false, disabled: true },
  { id: 'architecture', label: 'Architecture', glyph: 'A', active: false, disabled: true },
  { id: 'apis', label: 'APIs', glyph: '↗', active: false, disabled: true },
  { id: 'start-here', label: 'Start Here', glyph: 'S', active: false, disabled: true }
])

const report = ref(null)
const error = ref('')

const repositoryName = computed(() => report.value?.project?.name ?? 'Preparing workspace')
const analysisStatus = computed(() => report.value?.status ?? '')
const statusTone = computed(() => {
  if (analysisStatus.value === 'SUCCESS') return 'success'
  if (analysisStatus.value === 'PARTIAL') return 'warning'
  if (analysisStatus.value === 'FAILED') return 'danger'
  return 'neutral'
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
  <AppShell>
    <template #sidebar>
      <SidebarNav :items="navigation" />
    </template>

    <PageLayout
      eyebrow="Local analysis workspace"
      :title="repositoryName"
      description="A private, read-only view of the report generated on this machine."
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
      <StatePanel
        v-else
        variant="ready"
        heading="Workspace foundation ready"
        message="The report is connected. Product views will appear here as the M7 tasks are completed."
      >
        <template #details>
          <dl class="inline-facts">
            <div>
              <dt>Analysis status</dt>
              <dd>{{ report.status }}</dd>
            </div>
            <div>
              <dt>Report schema</dt>
              <dd>{{ report.schemaVersion }}</dd>
            </div>
          </dl>
        </template>
      </StatePanel>
    </PageLayout>

    <template #inspector>
      <InspectorPanel
        title="Selection details"
        description="Evidence and source context will stay close to the selected item."
      >
        <StatePanel
          variant="empty"
          compact
          heading="Nothing selected"
          message="Choose an item in a future project view to inspect its evidence."
        />
      </InspectorPanel>
    </template>
  </AppShell>
</template>
