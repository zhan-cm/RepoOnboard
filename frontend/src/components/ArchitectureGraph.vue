<script setup>
import cytoscape from 'cytoscape'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  nodes: { type: Array, required: true },
  edges: { type: Array, required: true },
  selection: { type: Object, default: null },
  theme: { type: String, default: 'dark' }
})
const emit = defineEmits(['select', 'error'])

const container = ref(null)
const isHeadless = ref(false)
const isReady = ref(false)
const panState = ref({ x: 0, y: 0 })
const zoomState = ref(1)
const nodeModelPositions = ref(new Map())

const CARD_WIDTH = 216
const CARD_HEIGHT = 88

let graph = null
let resizeObserver = null

const connectedNodeIds = computed(() => {
  const set = new Set()
  if (!props.selection?.id) return set
  if (props.selection.type === 'component') {
    set.add(props.selection.id)
    for (const edge of props.edges) {
      if (edge.sourceId === props.selection.id) set.add(edge.targetId)
      if (edge.targetId === props.selection.id) set.add(edge.sourceId)
    }
  } else if (props.selection.type === 'dependency') {
    const edge = props.edges.find((e) => e.id === props.selection.id)
    if (edge) {
      set.add(edge.sourceId)
      set.add(edge.targetId)
    }
  }
  return set
})

function isContextMuted(cardId) {
  if (!props.selection?.id) return false
  return !connectedNodeIds.value.has(cardId)
}

function getRelationCounts(nodeId) {
  let inCount = 0
  let outCount = 0
  for (const edge of props.edges) {
    if (edge.targetId === nodeId) inCount++
    if (edge.sourceId === nodeId) outCount++
  }
  return { inCount, outCount }
}

function formatPackage(qualifiedName) {
  if (!qualifiedName) return ''
  const lastDot = qualifiedName.lastIndexOf('.')
  if (lastDot === -1) return ''
  const pkg = qualifiedName.substring(0, lastDot)
  const segments = pkg.split('.')
  if (segments.length <= 3) return pkg
  const prefix = segments.slice(0, -2).map((s) => s[0]).join('.')
  const suffix = segments.slice(-2).join('.')
  return `${prefix}.${suffix}`
}

const cards = computed(() => {
  const map = nodeModelPositions.value
  return props.nodes.map((node) => {
    const pos = map.get(node.id) || { x: 0, y: 0 }
    const { inCount, outCount } = getRelationCounts(node.id)
    return {
      ...node,
      modelX: pos.x,
      modelY: pos.y,
      inCount,
      outCount,
      formattedPackage: formatPackage(node.qualifiedName)
    }
  })
})

function getGraphStyles() {
  const isLight = props.theme === 'light'
  return [
    {
      selector: 'node',
      style: {
        width: CARD_WIDTH,
        height: CARD_HEIGHT,
        shape: 'round-rectangle',
        'background-opacity': 0,
        'border-opacity': 0,
        'border-width': 0,
        label: '',
        'overlay-opacity': 0
      }
    },
    {
      selector: 'edge',
      style: {
        width: 1.5,
        'curve-style': 'bezier',
        'line-color': isLight ? '#cbd5e1' : '#475569',
        'target-arrow-color': isLight ? '#94a3b8' : '#64748b',
        'target-arrow-shape': 'triangle',
        'arrow-scale': 0.85,
        'overlay-opacity': 0
      }
    },
    {
      selector: 'edge.connected, edge:selected',
      style: {
        width: 2.25,
        'line-color': isLight ? '#0284c7' : '#38bdf8',
        'target-arrow-color': isLight ? '#0284c7' : '#38bdf8'
      }
    },
    {
      selector: 'node.context-muted, edge.context-muted',
      style: { opacity: 0.2 }
    }
  ]
}

function syncModelPositions() {
  if (!graph) return
  const map = new Map()
  for (const node of props.nodes) {
    const cyNode = graph.getElementById(node.id)
    if (!cyNode.empty()) {
      const pos = cyNode.position()
      map.set(node.id, {
        x: typeof pos?.x === 'number' && !isNaN(pos.x) ? pos.x : 0,
        y: typeof pos?.y === 'number' && !isNaN(pos.y) ? pos.y : 0
      })
    }
  }
  nodeModelPositions.value = map
}

function updateViewport() {
  if (!graph) return
  panState.value = graph.pan() || { x: 0, y: 0 }
  zoomState.value = graph.zoom() || 1
}

function handleCardPointerDown(event, cardId) {
  if (event.button !== 0) return
  const cyNode = graph?.getElementById(cardId)
  if (!cyNode || cyNode.empty()) return

  const startX = event.clientX
  const startY = event.clientY
  const startModelPos = { ...cyNode.position() }
  const currentZoom = graph.zoom() || 1
  let isDragging = false

  function onPointerMove(e) {
    const dx = e.clientX - startX
    const dy = e.clientY - startY
    if (!isDragging && Math.hypot(dx, dy) > 4) {
      isDragging = true
    }
    if (isDragging) {
      cyNode.position({
        x: startModelPos.x + dx / currentZoom,
        y: startModelPos.y + dy / currentZoom
      })
      syncModelPositions()
    }
  }

  function onPointerUp() {
    window.removeEventListener('pointermove', onPointerMove)
    window.removeEventListener('pointerup', onPointerUp)
    if (!isDragging) {
      emit('select', { type: 'component', id: cardId })
    }
  }

  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', onPointerUp)
}

function handleKeyDown(event) {
  if (event.key === 'Escape') {
    emit('select', null)
  }
}

onMounted(() => {
  render()
  if (typeof window !== 'undefined') {
    window.addEventListener('keydown', handleKeyDown)
  }
})

onBeforeUnmount(() => {
  destroy()
  if (typeof window !== 'undefined') {
    window.removeEventListener('keydown', handleKeyDown)
  }
})

watch(() => [props.nodes, props.edges], render)
watch(() => props.selection, applySelection)
watch(() => props.theme, applyThemeStyles)

function applyThemeStyles() {
  if (graph && !isHeadless.value) {
    graph.style(getGraphStyles()).update()
  }
}

async function render() {
  await nextTick()
  destroy()
  if (!container.value) return

  try {
    const headless = container.value.clientWidth === 0 || container.value.clientHeight === 0
    isHeadless.value = headless

    graph = cytoscape({
      container: headless ? undefined : container.value,
      headless,
      elements: [
        ...props.nodes.map((node) => ({
          group: 'nodes',
          data: { id: node.id },
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
        spacingFactor: 1.25,
        padding: 36,
        animate: false
      },
      minZoom: 0.35,
      maxZoom: 2.5,
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

    if (!headless) {
      graph.on('pan zoom', updateViewport)
      graph.on('position layoutstop', syncModelPositions)

      if (graph.zoom() < 0.62) {
        graph.zoom(0.62)
        graph.center()
      }

      syncModelPositions()
      updateViewport()
    }

    applySelection()
    isReady.value = true

    if (!headless && typeof ResizeObserver === 'function') {
      resizeObserver = new ResizeObserver(() => {
        graph?.resize()
        updateViewport()
        syncModelPositions()
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
  resizeObserver?.disconnect()
  resizeObserver = null
  graph?.destroy()
  graph = null
  isReady.value = false
}

function zoomBy(factor) {
  if (!graph) return
  graph.zoom({
    level: Math.min(2.5, Math.max(0.35, graph.zoom() * factor)),
    renderedPosition: { x: graph.width() / 2, y: graph.height() / 2 }
  })
  updateViewport()
}

function fit() {
  if (!graph) return
  graph.fit(undefined, 46)
  updateViewport()
}

function focus(id) {
  if (!graph || !id) return
  const element = graph.getElementById(id)
  if (element.empty()) return
  graph.center(element)
  graph.zoom({ level: Math.max(1, graph.zoom()), position: element.position() })
  updateViewport()
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

    <!-- HTML Node Cards Overlay Layer -->
    <div
      v-if="!isHeadless && isReady"
      class="architecture-graph-nodes-layer"
      :style="{
        transform: `translate3d(${panState.x}px, ${panState.y}px, 0) scale(${zoomState})`,
        transformOrigin: '0 0'
      }"
    >
      <div
        v-for="card in cards"
        :key="card.id"
        class="architecture-node-card"
        :class="[
          `architecture-node-card--${card.kindFamily}`,
          {
            'is-selected': selection?.type === 'component' && selection.id === card.id,
            'is-connected': connectedNodeIds.has(card.id),
            'is-muted': isContextMuted(card.id)
          }
        ]"
        :style="{
          transform: `translate3d(${card.modelX - CARD_WIDTH / 2}px, ${card.modelY - CARD_HEIGHT / 2}px, 0)`
        }"
        @pointerdown="handleCardPointerDown($event, card.id)"
      >
        <!-- Top header: badges + in/out degree -->
        <div class="architecture-node-card__header">
          <div class="architecture-node-card__badges">
            <span :class="`architecture-kind-badge architecture-kind-badge--${card.kindFamily}`">
              {{ card.kind }}
            </span>
            <span v-if="selection?.type === 'component' && selection.id === card.id" class="architecture-node-card__selected-pill">
              SELECTED
            </span>
          </div>
          <span class="architecture-node-card__relations font-mono">
            {{ card.inCount }} in · {{ card.outCount }} out
          </span>
        </div>

        <!-- Middle: title + package -->
        <div class="architecture-node-card__body">
          <div class="architecture-node-card__title" :title="card.label">
            <span>{{ card.label }}</span>
            <span v-if="selection?.type === 'component' && selection.id === card.id" class="architecture-node-card__selected-dot"></span>
          </div>
          <div class="architecture-node-card__package font-mono" :title="card.qualifiedName">
            {{ card.formattedPackage }}
          </div>
        </div>

        <!-- Bottom: module pill + kind label -->
        <div class="architecture-node-card__footer font-mono">
          <span class="architecture-node-card__module-pill" :title="card.moduleLabel">
            {{ card.moduleLabel }}
          </span>
          <span :class="`architecture-node-card__kind-label architecture-node-card__kind-label--${card.kindFamily}`">
            {{ card.kindLabel }}
          </span>
        </div>
      </div>
    </div>

    <!-- Canvas Floating Bottom-Left Keyboard Helper -->
    <div class="architecture-graph-helper" aria-hidden="true">
      <span>Click card or edge to inspect</span>
      <span>·</span>
      <span><kbd>Esc</kbd> clear</span>
      <span>·</span>
      <span>Pan canvas to explore</span>
    </div>

    <!-- Floating Zoom / Fit Controls -->
    <div class="architecture-graph-controls" aria-label="Graph view controls">
      <button type="button" title="Zoom in" aria-label="Zoom in" @click="zoomBy(1.2)">
        <svg class="icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 4v16m8-8H4" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <span class="architecture-graph-controls__zoom-level font-mono">{{ Math.round(zoomState * 100) }}%</span>
      <button type="button" title="Zoom out" aria-label="Zoom out" @click="zoomBy(0.8)">
        <svg class="icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M20 12H4" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
      <div class="architecture-graph-controls__divider"></div>
      <button type="button" title="Fit graph to view" aria-label="Fit graph to view" @click="fit">
        <svg class="icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
      </button>
    </div>
  </div>
</template>
