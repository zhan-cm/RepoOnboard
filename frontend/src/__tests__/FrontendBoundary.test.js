import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const testDirectory = dirname(fileURLToPath(import.meta.url))
const frontendDirectory = resolve(testDirectory, '../..')

describe('frontend host boundary', () => {
  it('uses the fixed same-origin report route without address-bar or filesystem APIs', () => {
    const appSource = readFileSync(resolve(frontendDirectory, 'src/App.vue'), 'utf8')
    const mainSource = readFileSync(resolve(frontendDirectory, 'src/main.js'), 'utf8')
    const source = `${appSource}\n${mainSource}`

    expect(appSource).toContain("fetch('/api/report'")
    expect(source).not.toMatch(/(?:window\.)?location\.(?:href|pathname|search|hash)/)
    expect(source).not.toContain('file://')
    expect(source).not.toContain('showOpenFilePicker')
    expect(source).not.toContain('webkitdirectory')
  })

  it('contains no desktop-container production dependency', () => {
    const packageJson = JSON.parse(readFileSync(resolve(frontendDirectory, 'package.json'), 'utf8'))
    const dependencies = {
      ...packageJson.dependencies,
      ...packageJson.optionalDependencies,
      ...packageJson.peerDependencies
    }

    expect(Object.keys(dependencies)).not.toContain('electron')
    expect(Object.keys(dependencies).some((name) => name.startsWith('@tauri-apps/'))).toBe(false)
  })
})
