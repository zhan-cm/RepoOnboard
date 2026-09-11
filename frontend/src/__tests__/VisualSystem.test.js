import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const tokens = readFileSync(resolve('src/styles/tokens.css'), 'utf8')
const shell = readFileSync(resolve('src/styles/shell.css'), 'utf8')
const architecture = readFileSync(resolve('src/styles/architecture.css'), 'utf8')

function token(name) {
  const match = tokens.match(new RegExp(`--${name}:\\s*(#[0-9a-fA-F]{6})`))
  if (!match) throw new Error(`Missing color token: ${name}`)
  return match[1]
}

function luminance(hex) {
  const channels = [1, 3, 5].map((index) => {
    const channel = Number.parseInt(hex.slice(index, index + 2), 16) / 255
    return channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
}

function contrast(foreground, background) {
  const brightest = Math.max(luminance(foreground), luminance(background))
  const darkest = Math.min(luminance(foreground), luminance(background))
  return (brightest + 0.05) / (darkest + 0.05)
}

describe('visual system', () => {
  it('centralizes the required semantic, typography, spacing, and shape tokens', () => {
    const requiredTokens = [
      'color-canvas',
      'color-surface',
      'color-text',
      'color-text-secondary',
      'color-accent',
      'font-sans',
      'font-mono',
      'space-4',
      'space-8',
      'radius-md',
      'radius-lg',
      'border-thin',
      'focus-ring'
    ]

    for (const name of requiredTokens) {
      expect(tokens).toContain(`--${name}:`)
    }
  })

  it.each([
    ['color-text', 'color-canvas'],
    ['color-text-secondary', 'color-surface'],
    ['color-text-muted', 'color-surface'],
    ['color-accent', 'color-accent-soft'],
    ['color-success', 'color-success-soft'],
    ['color-warning', 'color-warning-soft'],
    ['color-danger', 'color-danger-soft']
  ])('keeps %s readable against %s', (foreground, background) => {
    expect(contrast(token(foreground), token(background))).toBeGreaterThanOrEqual(4.5)
  })

  it('defines stable desktop, compact desktop, tablet, and narrow viewport behavior', () => {
    expect(shell).toContain('grid-template-columns: var(--sidebar-width) minmax(0, 1fr) var(--inspector-width)')
    expect(shell).toContain('@media (max-width: 70rem)')
    expect(shell).toContain('@media (max-width: 47.5rem)')
    expect(shell).toContain('@media (max-width: 31rem)')
    expect(shell).toContain('overflow-x: auto')
  })

  it('provides the positioned container required by Cytoscape UI rendering', () => {
    expect(architecture).toContain('.architecture-graph { position: relative; }')
  })
})
