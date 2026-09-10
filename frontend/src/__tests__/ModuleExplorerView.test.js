import { describe, expect, it } from 'vitest'
import ModuleExplorerView from '../components/ModuleExplorerView.vue'
import { createModuleExplorerModel } from '../lib/reportModules.js'
import { flushUi, mount } from './mount.js'

describe('ModuleExplorerView', () => {
  it('selects modules, filters the index, and switches content tabs', async () => {
    const model = createModuleExplorerModel({
      schemaVersion: '1.2', status: 'SUCCESS', project: { name: 'demo' },
      modules: [
        { id: 'root', artifactId: 'root', aggregationParentModuleId: null, packaging: 'pom', sourceRoots: [], frameworks: [], languageVersions: [], frameworkVersions: [], evidence: [] },
        { id: 'api', artifactId: 'api', aggregationParentModuleId: 'root', packaging: 'jar', sourceRoots: [], frameworks: [], languageVersions: [], frameworkVersions: [], evidence: [] }
      ],
      sourceFiles: [], components: [{ id: 'component:api', moduleId: 'api', qualifiedName: 'demo.Api', kind: 'SERVICE' }],
      endpoints: [], entryPoints: [{ id: 'entry:api', moduleId: 'api', qualifiedName: 'demo.Application', kind: 'APPLICATION_ENTRY_POINT' }],
      dependencies: [], diagnostics: []
    })
    const selected = []
    const view = mount(ModuleExplorerView, { props: { model, selectedModuleId: 'root', onSelect: (id) => selected.push(id) } })

    const rows = view.container.querySelectorAll('.module-row')
    rows[1].click()
    expect(selected).toEqual(['api'])

    const search = view.container.querySelector('input[type="search"]')
    search.value = 'api'
    search.dispatchEvent(new Event('input'))
    await flushUi()
    expect(view.container.querySelectorAll('.module-row')).toHaveLength(1)

    view.container.querySelectorAll('.content-tabs button')[1].click()
    await flushUi()
    expect(view.container.textContent).toContain('No entry points were reported')
    view.unmount()
  })
})
