<template>
  <div>
    <v-row class="mb-4">
      <v-col cols="12">
        <h1 class="text-h4 font-weight-bold mb-1">Audit Logs</h1>
        <p class="text-body-2 text-medium-emphasis">Recent security and management events</p>
      </v-col>
    </v-row>

    <v-card>
      <v-card-title class="d-flex align-center flex-wrap gap-2">
        <v-icon class="mr-2">mdi-history</v-icon>
        Audit Events
        <v-chip v-if="!loading" size="small" variant="tonal" class="ml-2">{{ filteredLogs.length }}</v-chip>
        <v-spacer />
        <v-text-field
          v-model="search"
          density="compact"
          variant="outlined"
          placeholder="Search user, event, entity…"
          prepend-inner-icon="mdi-magnify"
          clearable
          hide-details
          style="max-width: 260px;"
          class="mr-2"
        />
        <v-select
          v-model="eventFilter"
          :items="eventTypes"
          density="compact"
          variant="outlined"
          label="Event type"
          clearable
          hide-details
          style="max-width: 200px;"
          class="mr-2"
        />
        <v-btn size="small" variant="tonal" :loading="loading" @click="loadLogs">
          <v-icon>mdi-refresh</v-icon>
        </v-btn>
      </v-card-title>
      <v-divider />
      <v-card-text class="pa-0">
        <v-progress-linear v-if="loading" indeterminate />
        <v-table v-else-if="filteredLogs.length">
          <thead>
            <tr>
              <th>Timestamp</th>
              <th>User</th>
              <th>Event</th>
              <th>Entity</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in filteredLogs" :key="(log.timestamp || '') + '|' + (log.event || '') + '|' + (log.entity || '') + '|' + (log.user || '')">
              <td class="text-body-2" style="white-space: nowrap;">{{ formatDate(log.timestamp) }}</td>
              <td class="text-body-2">{{ log.user || '—' }}</td>
              <td>
                <v-chip size="small" :color="getEventColor(log.event)" label>
                  {{ log.event }}
                </v-chip>
              </td>
              <td class="text-body-2">{{ log.entity || '—' }}</td>
            </tr>
          </tbody>
        </v-table>
        <v-alert v-else-if="!loading && apiError" type="error" variant="tonal" class="ma-4">
          {{ apiError }}
        </v-alert>
        <v-alert v-else-if="!loading" type="info" variant="tonal" class="ma-4">
          {{ search || eventFilter ? 'No logs match your filter.' : 'No audit logs found.' }}
        </v-alert>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { apiGet } from '../api/client'
import { formatDate } from '../utils/dateUtils'

const loading = ref(true)
const logs = ref([])
const apiError = ref(null)
const search = ref('')
const eventFilter = ref(null)

const eventTypes = computed(() => {
  const types = [...new Set(logs.value.map(l => l.event).filter(Boolean))]
  return types.sort()
})

const filteredLogs = computed(() => {
  let result = logs.value
  if (eventFilter.value) {
    result = result.filter(l => l.event === eventFilter.value)
  }
  if (search.value) {
    const q = search.value.toLowerCase()
    result = result.filter(l =>
      (l.user || '').toLowerCase().includes(q) ||
      (l.event || '').toLowerCase().includes(q) ||
      (l.entity || '').toLowerCase().includes(q)
    )
  }
  return result
})

function getEventColor(event) {
  if (!event) return 'default'
  if (event.includes('DELETE') || event.includes('COMPROMISED')) return 'error'
  if (event.includes('REREGISTRATION')) return 'warning'
  if (event.includes('EXPIRED')) return 'warning'
  if (event === 'USER_CREATED') return 'success'
  if (event === 'CLUSTER_REGISTERED') return 'info'
  return 'default'
}

async function loadLogs() {
  loading.value = true
  apiError.value = null
  try {
    const r = await apiGet('/api/audit_logs')
    logs.value = r.data?.audit_logs || []
  } catch (e) {
    logs.value = []
    apiError.value = e.response?.data?.error || e.message || 'Failed to load audit logs'
  } finally {
    loading.value = false
  }
}

onMounted(loadLogs)
</script>
