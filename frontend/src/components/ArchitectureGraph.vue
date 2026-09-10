<script setup>
import cytoscape from 'cytoscape'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  nodes: { type: Array, required: true },
  edges: { type: Array, required: true },
  selection: { type: Object, default: null }
})
const emit = defineEmits(['select', 'error'])
const container = ref(null)
let graph = null
let resizeObserver = null

const graphStyles = [
  {
    selector: 'node',
    style: {
      width: 190,
      height: 76,
      shape: 'round-rectangle',
      'background-color': '#ffffff',
      'border-width': 1,
      'border-color': '#cbd5e1',
      label: 'data(graphLabel)',
      color: '#172033',
      'font-family': 'Inter, ui-sans-serif, system-ui, sans-serif',
      'font-size': 11,
      'font-weight': 600,
      'text-wrap': 'wrap',
      'text-max-width': 154,
      'text-valign': 'center',
      'text-halign': 'center',
      'overlay-opacity': 0
    }
  },
  { selector: 'node.controller', style: { 'border-color': '#8b5cf6', 'border-width': 1.5 } },
  { selector: 'node.service', style: { 'border-color': '#0284c7', 'border-width': 1.5 } },
  { selector: 'node.repository', style: { 'border-color': '#16a36a', 'border-width': 1.5 } },
  { selector: 'node.configuration', style: { 'border-color': '#d18a16', 'border-width': 1.5 } },
  {
    selector: 'node.connected',
    style: { 'border-color': '#38bdf8', 'border-width': 2 }
  },
  {
    selector: 'node:selected',
    style: {
      'border-color': '#087eb8',
      'border-width': 3,
      'background-color': '#f0f9ff'
    }
  },
  {
    selector: 'edge',
    style: {
      width: 1.35,
      'curve-style': 'bezier',
      'line-color': '#aab6c7',
      'target-arrow-color': '#7a879d',
      'target-arrow-shape': 'triangle',
      'arrow-scale': 0.8,
      'overlay-opacity': 0
    }
  },
  {
    selector: 'edge.connected, edge:selected',
    style: {
      width: 2.25,
      'line-color': '#087eb8',
      'target-arrow-color': '#087eb8'
    }
  }
]

onMounted(render)
onBeforeUnmount(destroy)

watch(() => [props.nodes, props.edges], render)
watch(() => props.selection, applySelection)

async function render() {
  await nextTick()
  destroy()
  if (!container.value) return

  try {
    const headless = container.value.clientWidth === 0 || container.value.clientHeight === 0
    graph = cytoscape({
      container: headless ? undefined : container.value,
      headless,
      elements: [
        ...props.nodes.map((node) => ({
          group: 'nodes',
          data: {
            id: node.id,
            graphLabel: `${node.label}\n${node.kindLabel} · ${node.moduleLabel}`
          },
          classes: node.kindFamily
        })),
        ...props.edges.map((edge) => ({
          group: 'edges',
          data: { id: edge.id, source: edge.sourceId, target: edge.targetId }
        }))
      ],
      style: headless ? [] : graphStyles,
      layout: {
        name: 'breadthfirst',
        directed: true,
        circle: false,
        grid: true,
        spacingFactor: 0.92,
        padding: 24,
        animate: false
      },
      minZoom: 0.5,
      maxZoom: 2.25,
      wheelSensitivity: 0.18,
      boxSelectionEnabled: false
    })

    graph.on('tap', 'node', (event) => {
      emit('select', { type: 'component', id: event.target.id() })
    })
    graph.on('tap', 'edge', (event) => {
      emit('select', { type: 'dependency', id: event.target.id() })
    })
    graph.on('tap', (event) => {
      if (event.target === graph) emit('select', null)
    })
    if (!headless && graph.zoom() < 0.62) {
      graph.zoom(0.62)
      graph.center()
    }
    applySelection()

    if (!headless && typeof ResizeObserver === 'function') {
      resizeObserver = new ResizeObserver(() => {
        graph?.resize()
      })
      resizeObserver.observe(container.value)
    }
  } catch (cause) {
    emit('error', cause instanceof Error ? cause.message : 'The architecture graph could not be rendered.')
  }
}

function applySelection() {
  if (!graph) return
  graph.elements().removeClass('connected').unselect()
  if (!props.selection?.id) return
  const selected = graph.getElementById(props.selection.id)
  if (selected.empty()) return
  selected.select()
  if (selected.isNode()) {
    selected.connectedEdges().addClass('connected')
    selected.neighborhood('node').addClass('connected')
  } else {
    selected.connectedNodes().addClass('connected')
  }
}

function destroy() {
  resizeObserver?.disconnect()
  resizeObserver = null
  graph?.destroy()
  graph = null
}

function zoomBy(factor) {
  if (!graph) return
  graph.zoom({
    level: Math.min(2.25, Math.max(0.5, graph.zoom() * factor)),
    renderedPosition: { x: graph.width() / 2, y: graph.height() / 2 }
  })
}

function fit() {
  graph?.fit(undefined, 46)
}

defineExpose({ fit })
</script>

<template>
  <div class="architecture-graph-frame">
    <div
      ref="container"
      class="architecture-graph"
      role="img"
      :aria-label="`Confirmed component graph with ${nodes.length} components and ${edges.length} relationships`"
    ></div>
    <div class="architecture-graph-controls" aria-label="Graph view controls">
      <button type="button" aria-label="Zoom in" @click="zoomBy(1.2)">+</button>
      <button type="button" aria-label="Zoom out" @click="zoomBy(0.8)">−</button>
      <button type="button" aria-label="Fit graph to view" @click="fit">⌗</button>
    </div>
  </div>
</template>
