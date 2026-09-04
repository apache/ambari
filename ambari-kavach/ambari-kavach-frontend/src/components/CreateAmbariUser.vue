<template>
  <div>
    <v-row>
      <v-col cols="12">
        <h1 class="text-h4 font-weight-bold mb-1">Create Ambari User</h1>
        <p class="text-body-2 text-medium-emphasis">Create a temporary Ambari user for cluster access</p>
      </v-col>
    </v-row>

    <v-row justify="center">
      <v-col cols="12" md="8" lg="6">
        <v-card class="pa-6">
          <v-card-title class="text-h5 px-0">Ambari User Creation</v-card-title>
          <p class="text-body-2 text-medium-emphasis mb-4">
            Create a temporary user for Ambari cluster access. Users expire automatically after the selected duration.
          </p>

          <v-form @submit.prevent="onSubmit" class="mt-4">
            <v-autocomplete
              v-model="ambariServer"
              v-bind="ambariServerAttrs"
              :items="servers"
              label="Ambari Server"
              variant="outlined"
              density="comfortable"
              :loading="serverLoading"
              :error-messages="errors.ambariServer"
              placeholder="Select or type server hostname"
              class="mb-4"
            />

            <v-select
              v-model="requestTime"
              v-bind="requestTimeAttrs"
              :items="timeOptions"
              item-title="label"
              item-value="value"
              label="Access Duration"
              variant="outlined"
              density="comfortable"
              :error-messages="errors.requestTime"
              class="mb-4"
            />

            <v-select
              v-model="role"
              :items="roleOptions"
              item-title="label"
              item-value="value"
              label="Role / Permission"
              hint="Cluster Administrator: full access. Cluster Read: view-only (recommended for non-admin tasks)."
              persistent-hint
              variant="outlined"
              density="comfortable"
              class="mb-4"
            />

            <v-btn
              type="submit"
              color="primary"
              size="large"
              block
              :loading="loading"
              class="mt-2"
            >
              Create User
            </v-btn>
          </v-form>

          <!-- Success -->
          <v-card v-if="response" variant="tonal" color="success" class="mt-4 pa-4">
            <div class="d-flex align-center mb-2">
              <v-icon color="success" class="mr-2">mdi-check-circle</v-icon>
              <span class="text-subtitle-1 font-weight-bold">User Created Successfully</span>
            </div>
            <v-divider class="mb-3" />
            <v-row dense class="mb-2">
              <v-col cols="6">
                <span class="text-caption text-medium-emphasis">Username</span>
                <div class="font-mono font-weight-bold text-body-1">{{ response.username }}</div>
              </v-col>
              <v-col cols="6">
                <span class="text-caption text-medium-emphasis">Cluster</span>
                <div class="text-body-2 font-weight-medium">{{ createdOnServer }}</div>
              </v-col>
              <v-col cols="6" v-if="response.role">
                <span class="text-caption text-medium-emphasis">Role</span>
                <div class="text-body-2">{{ response.role }}</div>
              </v-col>
              <v-col cols="6">
                <span class="text-caption text-medium-emphasis">Expires</span>
                <div class="text-body-2">{{ createdExpiry }}</div>
              </v-col>
            </v-row>
            <v-text-field
              :model-value="response.password"
              :type="showPassword ? 'text' : 'password'"
              label="Password"
              variant="outlined"
              density="compact"
              readonly
              class="mb-2"
              hide-details
              bg-color="white"
            >
              <template #append-inner>
                <v-btn icon variant="text" size="small" @click="showPassword = !showPassword">
                  <v-icon>{{ showPassword ? 'mdi-eye-off' : 'mdi-eye' }}</v-icon>
                </v-btn>
                <v-btn icon variant="text" size="small" @click="copyPasswordAndNotify">
                  <v-icon>mdi-content-copy</v-icon>
                </v-btn>
              </template>
            </v-text-field>
            <p class="text-caption text-medium-emphasis mb-3">
              Copy and store this password now — it will not be shown again after you leave this page.
            </p>
            <v-btn size="small" variant="tonal" color="success" @click="resetForm">
              Create Another User
            </v-btn>
          </v-card>

          <!-- Error -->
          <v-alert v-if="error" type="error" variant="tonal" class="mt-4" closable @click:close="error = ''">
            {{ error }}
          </v-alert>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useForm } from 'vee-validate'
import * as yup from 'yup'
import { apiGet, apiPost } from '../api/client'
import { useSnackbar } from '../composables/useSnackbar'
import { formatDate } from '../utils/dateUtils'

const { success } = useSnackbar()

const schema = yup.object({
  ambariServer: yup.string().required('Ambari Server is required'),
  requestTime: yup.number().required('Request Time is required'),
})

const { handleSubmit, errors, defineField, resetForm: resetVeeForm } = useForm({
  validationSchema: schema,
})

const [ambariServer, ambariServerAttrs] = defineField('ambariServer')
const [requestTime, requestTimeAttrs] = defineField('requestTime', { initialValue: 30 })
const role = ref('CLUSTER.ADMINISTRATOR')

const roleOptions = [
  { label: 'Cluster Administrator (full access)', value: 'CLUSTER.ADMINISTRATOR' },
  { label: 'Cluster Operator', value: 'CLUSTER.OPERATOR' },
  { label: 'Cluster User (read-only)', value: 'CLUSTER.USER' },
]

const loading = ref(false)
const serverLoading = ref(true)
const servers = ref([])
const response = ref(null)
const error = ref('')
const showPassword = ref(false)
const createdOnServer = ref('')
const createdDurationMin = ref(0)

const createdExpiry = computed(() => {
  if (response.value?.expire_time) return formatDate(response.value.expire_time)
  if (!createdDurationMin.value) return '—'
  const d = new Date(Date.now() + createdDurationMin.value * 60000)
  return formatDate(d.toISOString())
})

const timeOptions = [
  { label: '5 min', value: 5 },
  { label: '30 min', value: 30 },
  { label: '1 Hour', value: 60 },
  { label: '6 Hours', value: 360 },
  { label: '12 Hours', value: 720 },
]

onMounted(() => {
  apiGet('/api/servers')
    .then((res) => {
      servers.value = res.data.servers || []
    })
    .catch(() => {
      error.value = 'Failed to fetch servers. Please refresh the page.'
    })
    .finally(() => {
      serverLoading.value = false
    })
})

const onSubmit = handleSubmit(async (data) => {
  loading.value = true
  error.value = ''
  response.value = null

  try {
    const res = await apiPost('/create_user', {
      ambari_server: data.ambariServer,
      request_time: data.requestTime,
      role: role.value,
    })
    if (res.status === 201) {
      response.value = res.data
      createdOnServer.value = data.ambariServer
      createdDurationMin.value = data.requestTime
      showPassword.value = true  // Auto-reveal password on creation
    } else {
      error.value = res.data?.error || 'Unexpected error occurred'
    }
  } catch (err) {
    const msg = err.response?.data?.error || 'Server error. Please try again.'
    error.value = msg
  } finally {
    loading.value = false
  }
})

async function copyPasswordAndNotify() {
  if (response.value?.password) {
    await navigator.clipboard.writeText(response.value.password)
    success('Password copied to clipboard')
  }
}

function resetForm() {
  response.value = null
  error.value = ''
  showPassword.value = false
  createdOnServer.value = ''
  createdDurationMin.value = 0
  resetVeeForm()
  role.value = 'CLUSTER.ADMINISTRATOR'
}
</script>

<style scoped>
.font-mono {
  font-family: 'Consolas', 'Monaco', monospace;
}
</style>
