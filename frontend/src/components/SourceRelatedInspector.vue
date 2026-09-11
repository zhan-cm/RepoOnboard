<script setup>
defineProps({
  model: { type: Object, required: true }
})

function count(value) {
  return value === null ? 'Unavailable' : value.length
}
</script>

<template>
  <aside class="source-related" aria-label="Source related facts">
    <header class="source-related__header">
      <p class="workbench-kicker">Inspector</p>
      <h2>Related Facts</h2>
    </header>

    <div v-if="!model.available" class="source-related__body">
      <p class="source-empty">Related facts are unavailable for the stale selection.</p>
    </div>

    <div v-else class="source-related__body">
      <section v-if="model.entityType === 'endpoint'">
        <p class="source-related__label">Owning controller</p>
        <article v-if="model.related.owningController" class="source-related-card">
          <strong>{{ model.related.owningController.label }}</strong>
          <code>{{ model.related.owningController.qualifiedName }}</code>
          <span>{{ model.related.owningController.kindLabel }}</span>
        </article>
        <p v-else class="source-empty">Controller details unavailable.</p>
      </section>

      <section v-if="model.entityType === 'endpoint'">
        <p class="source-related__label">Other endpoints in controller <span>{{ count(model.related.controllerEndpoints) }}</span></p>
        <ul v-if="model.related.controllerEndpoints?.length" class="source-related-list">
          <li v-for="endpoint in model.related.controllerEndpoints" :key="endpoint.id">
            <span class="api-method" :class="`api-method--${endpoint.method.toLowerCase()}`">{{ endpoint.methodLabel }}</span>
            <code>{{ endpoint.pathLabel }}</code>
          </li>
        </ul>
        <p v-else-if="model.related.controllerEndpoints" class="source-empty">No other endpoints were reported for this controller.</p>
        <p v-else class="source-empty">Controller endpoint data is unavailable.</p>
      </section>

      <section>
        <p class="source-related__label">
          {{ model.entityType === 'endpoint' ? 'Controller dependency neighbors' : 'Confirmed dependency neighbors' }}
          <span>{{ count(model.related.dependencyNeighbors) }}</span>
        </p>
        <p v-if="model.entityType === 'endpoint'" class="source-related__note">These are confirmed component relationships, not a handler call chain.</p>
        <ul v-if="model.related.dependencyNeighbors?.length" class="source-neighbor-list">
          <li v-for="neighbor in model.related.dependencyNeighbors" :key="neighbor.id">
            <div><span>{{ neighbor.direction }} {{ neighbor.kind }}</span><strong>CONFIRMED</strong></div>
            <b>{{ neighbor.component.label }}</b>
            <code>{{ neighbor.component.qualifiedName }}</code>
            <small v-if="neighbor.location">Source: {{ neighbor.location.sourceFileId }}<template v-if="neighbor.location.startLine">:{{ neighbor.location.startLine }}</template></small>
          </li>
        </ul>
        <p v-else-if="model.related.dependencyNeighbors" class="source-empty">No confirmed component relationships were reported.</p>
        <p v-else class="source-empty">Component relationship data is unavailable.</p>
      </section>

      <section>
        <p class="source-related__label">Components in this source file <span>{{ count(model.related.sameFileComponents) }}</span></p>
        <ul v-if="model.related.sameFileComponents?.length" class="source-related-list source-related-list--stacked">
          <li v-for="component in model.related.sameFileComponents" :key="component.id">
            <strong>{{ component.label }}</strong><span>{{ component.kindLabel }}</span>
            <code>{{ component.qualifiedName }}</code>
          </li>
        </ul>
        <p v-else-if="model.related.sameFileComponents" class="source-empty">No components were reported in this source file.</p>
        <p v-else class="source-empty">Component data is unavailable.</p>
      </section>

      <section>
        <p class="source-related__label">Entry points in this source file <span>{{ count(model.related.sameFileEntryPoints) }}</span></p>
        <ul v-if="model.related.sameFileEntryPoints?.length" class="source-related-list source-related-list--stacked">
          <li v-for="entryPoint in model.related.sameFileEntryPoints" :key="entryPoint.id">
            <strong>{{ entryPoint.qualifiedName }}</strong><span>{{ entryPoint.kind }}</span>
          </li>
        </ul>
        <p v-else-if="model.related.sameFileEntryPoints" class="source-empty">No entry points were reported in this source file.</p>
        <p v-else class="source-empty">Entry point data is unavailable.</p>
      </section>

      <section>
        <p class="source-related__label">File diagnostics <span>{{ count(model.related.fileDiagnostics) }}</span></p>
        <ul v-if="model.related.fileDiagnostics?.length" class="source-diagnostic-list">
          <li v-for="diagnostic in model.related.fileDiagnostics" :key="`${diagnostic.code}:${diagnostic.message}`">
            <strong>{{ diagnostic.severity }} · {{ diagnostic.code }}</strong>
            <span>{{ diagnostic.stage ?? 'Stage unavailable' }}</span>
            <p>{{ diagnostic.message ?? 'Message unavailable' }}</p>
          </li>
        </ul>
        <p v-else-if="model.related.fileDiagnostics" class="source-empty">No file diagnostics were reported for this source.</p>
        <p v-else class="source-empty">Diagnostic data is unavailable.</p>
      </section>
    </div>
  </aside>
</template>
