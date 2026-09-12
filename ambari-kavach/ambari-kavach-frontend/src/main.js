import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router/index.js'
import vuetify from './plugins/vuetify'
import vue3GoogleLogin from 'vue3-google-login'

const app = createApp(App)
const pinia = createPinia()

app.use(vuetify)

// Configure Google Login - set VUE_APP_GOOGLE_CLIENT_ID in .env.local
app.use(vue3GoogleLogin, {
  clientId: process.env.VUE_APP_GOOGLE_CLIENT_ID || '659008154711-uti94rnrgdai87ui901stu7dekhef315.apps.googleusercontent.com',
  scope: 'email profile openid',
  prompt: 'select_account',
  popupType: 'TOKEN'
})

app.use(router)
app.use(pinia)
app.mount('#app')