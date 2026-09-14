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
let themeObserver = null

function getGraphStyles() {
  const isLight = typeof document !== 'undefined' && document.documentElement.getAttribute('data-theme') === 'light'
  if (isLight) {
    return [
      {
        selector: 'node',
        style: {
          width: 190,
          height: 76,
          shape: 'round-rectangle',
          'background-color': '#ffffff',
          'border-width': 1,
          'border-color': '#d0d7de',
          label: 'data(graphLabel)',
          color: '#1f2328',
          'font-family': '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
          'font-size': 11,
          'font-weight': 600,
          'text-wrap': 'wrap',
          'text-max-width': 154,
          'text-valign': 'center',
          'text-halign': 'center',
          'overlay-opacity': 0
        }
      },
      { selector: 'node.controller', style: { 'border-color': '#8250df', 'border-width': 1.5 } },
      { selector: 'node.service', style: { 'border-color': '#0969da', 'border-width': 1.5 } },
      { selector: 'node.repository', style: { 'border-color': '#1a7f37', 'border-width': 1.5 } },
      { selector: 'node.configuration', style: { 'border-color': '#9a6700', 'border-width': 1.5 } },
      {
        selector: 'node.connected',
        style: { 'border-color': '#0969da', 'border-width': 2 }
      },
      {
        selector: 'node:selected',
        style: {
          'border-color': '#0969da',
          'border-width': 3,
          'background-color': '#ddf4ff'
        }
      },
      {
        selector: 'edge',
        style: {
          width: 1.35,
          'curve-style': 'bezier',
          'line-color': '#afb8c1',
          'target-arrow-color': '#8c959f',
          'target-arrow-shape': 'triangle',
          'arrow-scale': 0.8,
          'overlay-opacity': 0
        }
      },
      {
        selector: 'edge.connected, edge:selected',
        style: {
          width: 2.25,
          'line-color': '#0969da',
          'target-arrow-color': '#0969da'
        }
      },
      {
        selector: 'node.context-muted, edge.context-muted',
        style: { opacity: 0.24 }
      }
    ]
  }

  // Dark Theme (Default)
  return [
    {
      selector: 'node',
      style: {
        width: 190,
        height: 76,
        shape: 'round-rectangle',
        'background-color': '#161b22',
        'border-width': 1,
        'border-color': '#30363d',
        label: 'data(graphLabel)',
        color: '#f0f6fc',
        'font-family': '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        'font-size': 11,
        'font-weight': 600,
        'text-wrap': 'wrap',
        'text-max-width': 154,
        'text-valign': 'center',
        'text-halign': 'center',
        'overlay-opacity': 0
      }
    },
    { selector: 'node.controller', style: { 'border-color': '#d2a8ff', 'border-width': 1.5 } },
    { selector: 'node.service', style: { 'border-color': '#58a6ff', 'border-width': 1.5 } },
    { selector: 'node.repository', style: { 'border-color': '#7ee787', 'border-width': 1.5 } },
    { selector: 'node.configuration', style: { 'border-color': '#f2cc60', 'border-width': 1.5 } },
    {
      selector: 'node.connected',
      style: { 'border-color': '#58a6ff', 'border-width': 2 }
    },
    {
      selector: 'node:selected',
      style: {
        'border-color': '#58a6ff',
        'border-width': 3,
        'background-color': '#1f293d'
      }
    },
    {
      selector: 'edge',
      style: {
        width: 1.35,
        'curve-style': 'bezier',
        'line-color': '#484f58',
        'target-arrow-color': '#6e7681',
        'target-arrow-shape': 'triangle',
        'arrow-scale': 0.8,
        'overlay-opacity': 0
      }
    },
    {
      selector: 'edge.connected, edge:selected',
      style: {
        width: 2.25,
        'line-color': '#58a6ff',
        'target-arrow-color': '#58a6ff'
      }
    },
    {
      selector: 'node.context-muted, edge.context-muted',
      style: { opacity: 0.24 }
    }
  ]
}

onMounted(() => {
  render()
  if (typeof MutationObserver === 'function' && typeof document !== 'undefined') {
    themeObserver = new MutationObserver(() => {
      if (graph) {
        graph.style(getGraphStyles()).update()
      }
    })
    themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
  }
})
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
      style: headless ? [] : getGraphStyles(),
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
  graph.elements().removeClass('connected context-muted').unselect()
  if (!props.selection?.id) return
  const selected = graph.getElementById(props.selection.id)
  if (selected.empty()) return
  graph.elements().addClass('context-muted')
  selected.removeClass('context-muted')
  selected.select()
  if (selected.isNode()) {
    selected.connectedEdges().removeClass('context-muted').addClass('connected')
    selected.neighborhood('node').removeClass('context-muted').addClass('connected')
  } else {
    selected.connectedNodes().removeClass('context-muted').addClass('connected')
  }
}

function destroy() {
  themeObserver?.disconnect()
  themeObserver = null
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

function focus(id) {
  if (!graph || !id) return
  const element = graph.getElementById(id)
  if (element.empty()) return
  graph.center(element)
  graph.zoom({ level: Math.max(1, graph.zoom()), position: element.position() })
}

defineExpose({ fit, focus })
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
