import { createApp } from 'vue'
import App from './App.vue'
import { verifyPackagedSchema } from './lib/reportSchema.js'
import './styles/tokens.css'
import './styles/base.css'
import './styles/shell.css'
import './styles/overview.css'
import './styles/modules.css'
import './styles/architecture.css'
import './styles/api.css'
import './styles/source.css'
import './styles/start-here.css'

verifyPackagedSchema()
createApp(App).mount('#app')
