import { describe, expect, it } from 'vitest'
import { h } from 'vue'
import AppShell from '../components/AppShell.vue'
import InspectorPanel from '../components/InspectorPanel.vue'
import PageLayout from '../components/PageLayout.vue'
import SidebarNav from '../components/SidebarNav.vue'
import StatePanel from '../components/StatePanel.vue'
import { mount } from './mount.js'

describe('product shell components', () => {
  it('provides reusable navigation semantics and keyboard-reachable active navigation', () => {
    const selected = []
    const view = mount(SidebarNav, {
      props: {
        onSelect: (id) => selected.push(id),
        items: [
          { id: 'workspace', label: 'Workspace', glyph: 'W', active: true, disabled: false },
          { id: 'overview', label: 'Overview', glyph: 'O', active: false, disabled: true }
        ]
      }
    })

    const buttons = view.container.querySelectorAll('button')
    expect(buttons[0].getAttribute('aria-current')).toBe('page')
    expect(buttons[0].disabled).toBe(false)
    expect(buttons[1].disabled).toBe(true)

    buttons[0].focus()
    expect(document.activeElement).toBe(buttons[0])
    buttons[0].click()
    expect(selected).toEqual(['workspace'])
    view.unmount()
  })

  it('provides a skip link and stable main landmark', () => {
    const view = mount(AppShell, {
      slots: {
        sidebar: () => h('aside', 'Navigation'),
        default: () => h('p', 'Workspace content'),
        inspector: () => h('aside', 'Inspector')
      }
    })

    expect(view.container.querySelector('.skip-link').getAttribute('href')).toBe('#main-content')
    expect(view.container.querySelector('main').id).toBe('main-content')
    expect(view.container.querySelector('main').getAttribute('tabindex')).toBe('-1')
    view.container.querySelector('.skip-link').focus()
    expect(document.activeElement).toBe(view.container.querySelector('.skip-link'))
    view.unmount()
  })

  it('composes context, page content, and inspector foundations through slots', () => {
    const page = mount(PageLayout, {
      props: {
        eyebrow: 'Repository',
        title: 'sample',
        description: 'Local report',
        status: 'SUCCESS',
        statusTone: 'success'
      },
      slots: {
        metadata: () => h('span', 'Schema 1.1'),
        default: () => h('p', 'Page content')
      }
    })
    const inspector = mount(InspectorPanel, {
      props: {
        title: 'Selection details',
        description: 'Evidence context'
      },
      slots: { default: () => h('p', 'Nothing selected') }
    })

    expect(page.container.querySelector('h1').textContent).toBe('sample')
    expect(page.container.textContent).toContain('Schema 1.1')
    expect(page.container.textContent).toContain('Page content')
    expect(inspector.container.querySelector('.inspector').getAttribute('aria-label')).toBe('Selection details')
    expect(inspector.container.textContent).toContain('Nothing selected')
    page.unmount()
    inspector.unmount()
  })

  it.each([
    ['loading', 'status', 'polite'],
    ['empty', 'status', 'polite'],
    ['warning', 'status', 'polite'],
    ['ready', 'status', 'polite'],
    ['error', 'alert', 'assertive']
  ])('gives the %s state an accessible live-region contract', (variant, role, live) => {
    const view = mount(StatePanel, {
      props: {
        variant,
        heading: 'State heading',
        message: 'State explanation'
      }
    })

    const panel = view.container.querySelector('.state-panel')
    expect(panel.getAttribute('role')).toBe(role)
    expect(panel.getAttribute('aria-live')).toBe(live)
    expect(panel.textContent).toContain('State heading')
    expect(panel.textContent).toContain('State explanation')
    view.unmount()
  })
})
