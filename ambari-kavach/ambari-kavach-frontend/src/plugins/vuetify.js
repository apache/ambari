import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import '@mdi/font/css/materialdesignicons.css'

export default createVuetify({
  components,
  directives,
  theme: {
    defaultTheme: 'kavach',
    themes: {
      kavach: {
        dark: false,
        colors: {
          primary: '#1976D2',
          secondary: '#455A64',
          accent: '#00BCD4',
          error: '#C62828',
          info: '#0277BD',
          success: '#2E7D32',
          warning: '#E65100',
          background: '#F5F5F5',
          surface: '#FFFFFF',
        },
      },
    },
  },
  defaults: {
    VBtn: {
      variant: 'flat',
      density: 'comfortable',
    },
    VCard: {
      elevation: 2,
      rounded: 'lg',
    },
    VTextField: {
      density: 'comfortable',
      variant: 'outlined',
    },
    VSelect: {
      density: 'comfortable',
      variant: 'outlined',
    },
  },
})
