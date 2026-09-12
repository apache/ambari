import { ref, nextTick } from 'vue'

const visible = ref(false)
const message = ref('')
const color = ref('success')
const timeout = ref(3000)

export function useSnackbar() {
  async function show(msg, type = 'success', duration = 3000) {
    // Briefly hide to force Vuetify to restart its internal timer
    if (visible.value) {
      visible.value = false
      await nextTick()
    }
    message.value = msg
    color.value = type
    timeout.value = duration
    visible.value = true
  }

  function success(msg) { show(msg, 'success') }
  function error(msg) { show(msg, 'error', 4000) }
  function info(msg) { show(msg, 'info') }
  function warn(msg) { show(msg, 'warning') }

  return { visible, message, color, timeout, show, success, error, info, warn }
}
