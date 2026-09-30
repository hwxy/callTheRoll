import { createSSRApp } from 'vue'
import App from './App.vue'
// #ifdef H5
import ContactFloat from './components/ContactFloat.vue'
// #endif
export function createApp() {
  const app = createSSRApp(App)
  // #ifdef H5
  app.component('ContactFloat', ContactFloat)
  // #endif
  return { app }
}
