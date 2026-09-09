import { createApp, h, nextTick } from 'vue'

export function mount(component, { props = {}, slots = {} } = {}) {
  const container = document.createElement('div')
  document.body.append(container)
  const app = createApp({
    render: () => h(component, props, slots)
  })
  app.mount(container)
  return {
    container,
    unmount() {
      app.unmount()
      container.remove()
    }
  }
}

export async function flushUi() {
  await Promise.resolve()
  await Promise.resolve()
  await nextTick()
}
