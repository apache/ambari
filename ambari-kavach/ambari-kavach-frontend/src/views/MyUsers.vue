<template>
  <div>
    <v-row class="mb-4">
      <v-col cols="12">
        <h1 class="text-h4 font-weight-bold mb-1">My Ambari Users</h1>
        <p class="text-body-2 text-medium-emphasis">Active and expired temporary users. Use the eye icon to reveal passwords; copy to clipboard when needed.</p>
      </v-col>
    </v-row>

    <!-- Active Users -->
    <v-card class="mb-6">
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2" color="success">mdi-account-check</v-icon>
        Active Users
        <v-chip class="ml-2" color="success" size="small">{{ activeUsers.length }}</v-chip>
        <v-spacer />
        <v-btn variant="text" size="small" :loading="loading" class="mr-2" @click="refresh">
          <v-icon>mdi-refresh</v-icon>
        </v-btn>
        <v-btn color="primary" size="small" :to="{ name: 'CreateAmbariUser' }">
          Create New
        </v-btn>
      </v-card-title>
      <v-divider />
      <v-card-text class="pa-0">
        <v-progress-linear v-if="loading" indeterminate />
        <v-table v-else-if="activeUsers.length">
          <thead>
            <tr>
              <th>Ambari Server</th>
              <th>User Name</th>
              <th>Password</th>
              <th>Expires</th>
              <th>Time Left</th>
              <th class="text-center">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in activeUsers" :key="user.username">
              <td class="text-body-2">{{ user.ambari_server }}</td>
              <td class="font-mono text-body-2">{{ user.username }}</td>
              <td>
                <span class="font-mono text-body-2">{{ visiblePasswords[user.ambari_server + ':' + user.username] ? user.password : '••••••••••••' }}</span>
              </td>
              <td class="text-body-2">{{ formatDate(user.expire_time) }}</td>
              <td>
                <v-chip :color="expiryColor(user.expire_time)" size="small" variant="tonal">
                  {{ timeUntil(user.expire_time) }}
                </v-chip>
              </td>
              <td class="text-center" style="white-space: nowrap;">
                <v-tooltip :text="visiblePasswords[user.username] ? 'Hide password' : 'Show password'" location="top">
                  <template #activator="{ props }">
                    <v-btn icon variant="text" size="small" v-bind="props" @click="togglePassword(user.ambari_server + ':' + user.username)">
                      <v-icon>{{ visiblePasswords[user.ambari_server + ':' + user.username] ? 'mdi-eye-off' : 'mdi-eye' }}</v-icon>
                    </v-btn>
                  </template>
                </v-tooltip>
                <v-tooltip text="Copy password" location="top">
                  <template #activator="{ props }">
                    <v-btn icon variant="text" size="small" v-bind="props" @click="copyPassword(user.password, user.username)">
                      <v-icon>mdi-content-copy</v-icon>
                    </v-btn>
                  </template>
                </v-tooltip>
              </td>
            </tr>
          </tbody>
        </v-table>
        <v-alert v-else type="info" variant="tonal" class="ma-4">
          No active users. <v-btn variant="text" size="small" :to="{ name: 'CreateAmbariUser' }">Create one</v-btn>
        </v-alert>
      </v-card-text>
    </v-card>

    <!-- User History (Expired) -->
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon class="mr-2">mdi-account-clock</v-icon>
        User History (Expired)
        <v-chip class="ml-2" color="grey" size="small">{{ expiredUsers.length }}</v-chip>
      </v-card-title>
      <v-divider />
      <v-card-text class="pa-0">
        <v-progress-linear v-if="loading" indeterminate />
        <v-table v-else-if="expiredUsers.length">
          <thead>
            <tr>
              <th>Ambari Server</th>
              <th>User Name</th>
              <th>Created At</th>
              <th>Expired At</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in expiredUsers" :key="user.username + user.created_at">
              <td class="text-body-2">{{ user.ambari_server }}</td>
              <td class="font-mono text-body-2">{{ user.username }}</td>
              <td class="text-body-2">{{ formatDate(user.created_at) }}</td>
              <td class="text-body-2">{{ formatDate(user.expired_at || user.expire_time) }}</td>
            </tr>
          </tbody>
        </v-table>
        <v-alert v-else type="info" variant="tonal" class="ma-4">
          No expired users yet.
        </v-alert>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { apiGet } from '../api/client'
import { useSnackbar } from '../composables/useSnackbar'
import { formatDate, timeUntil, expiryColor } from '../utils/dateUtils'

const { success } = useSnackbar()

const loading = ref(true)
const activeUsers = ref([])
const expiredUsers = ref([])
const visiblePasswords = ref({})
let refreshTimer = null

function togglePassword(key) {
  visiblePasswords.value[key] = !visiblePasswords.value[key]
}

async function copyPassword(password, username) {
  if (password) {
    await navigator.clipboard.writeText(password)
    success(`Password for ${username} copied to clipboard`)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const [activeRes, expiredRes] = await Promise.allSettled([
      apiGet('/api/active_users'),
      apiGet('/api/expired_users')
    ])
    activeUsers.value = activeRes.status === 'fulfilled' ? (activeRes.value.data?.active_users || []) : []
    const rawExpired = expiredRes.status === 'fulfilled' ? (expiredRes.value.data?.all_users || []) : []
    expiredUsers.value = rawExpired.map(u => ({
      ...u,
      expired_at: u.expired_at || u.expire_time
    }))
  } catch {
    activeUsers.value = []
    expiredUsers.value = []
  } finally {
    loading.value = false
  }
}

async function refresh() {
  await fetchData()
}

onMounted(() => {
  fetchData()
  // Auto-refresh every 60s so expiry info stays current
  refreshTimer = setInterval(fetchData, 60000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.font-mono {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.9em;
}
</style>
