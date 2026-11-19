<template>
  <div>
    <v-row class="mb-4">
      <v-col cols="12">
        <h1 class="text-h4 font-weight-bold mb-1">Cluster Management</h1>
        <p class="text-body-2 text-medium-emphasis">View and manage users per cluster (Managers only)</p>
      </v-col>
    </v-row>

    <v-progress-linear v-if="pageLoading" indeterminate color="primary" class="mb-4" />
    <v-alert v-else-if="!isManager" type="warning" variant="tonal" class="mb-4">
      You need manager permissions to access this page.
    </v-alert>

    <template v-else>
      <!-- Section 1: Delete Users -->
      <v-card class="mb-4">
        <v-card-title>Delete Users</v-card-title>
        <v-card-subtitle>Select a cluster to view and delete users</v-card-subtitle>
        <v-card-text>
          <v-select
            v-model="selectedServer"
            :items="availableServers"
            label="Select Cluster"
            density="comfortable"
            variant="outlined"
            clearable
            @update:model-value="loadClusterUsers"
          />
          <!-- Show cluster managers for reference -->
          <div v-if="selectedServer && selectedClusterManagers.length" class="mb-3">
            <p class="text-caption text-medium-emphasis mb-1">Cluster managers:</p>
            <div class="d-flex flex-wrap gap-1">
              <v-chip
                v-for="mgr in selectedClusterManagers"
                :key="mgr"
                size="x-small"
                color="warning"
                variant="tonal"
              >{{ mgr }}</v-chip>
            </div>
          </div>
          <v-divider class="my-4" />
          <template v-if="selectedServer">
            <v-progress-linear v-if="loadingUsers" indeterminate />
            <v-alert v-else-if="clusterLoadError" type="error" variant="tonal" density="compact" class="mt-2">
              {{ clusterLoadError }}
            </v-alert>
            <div v-else-if="clusterData">
              <v-chip class="mb-2" color="success">Active: {{ clusterData.active_users_count }}</v-chip>
              <v-chip class="mb-2 ml-2" color="grey">Expired: {{ clusterData.expired_users_count }}</v-chip>
              <v-table class="mt-4">
                <thead>
                  <tr>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Status</th>
                    <th>Expires</th>
                    <th>Delete</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="u in [...(clusterData.active_users || []), ...(clusterData.expired_users || [])]" :key="u.username">
                    <td>{{ u.username }}</td>
                    <td>{{ u.email }}</td>
                    <td>
                      <v-chip :color="u.status === 'active' ? 'success' : 'grey'" size="small">
                        {{ u.status }}
                      </v-chip>
                    </td>
                    <td>{{ u.status === 'active' ? formatDate(u.expire_time) : '-' }}</td>
                    <td>
                      <v-btn
                        v-if="u.status === 'active'"
                        size="small"
                        color="error"
                        variant="tonal"
                        @click="confirmDelete(u)"
                      >
                        Delete
                      </v-btn>
                      <span v-else class="text-caption text-medium-emphasis">—</span>
                    </td>
                  </tr>
                </tbody>
              </v-table>
            </div>
          </template>
        </v-card-text>
      </v-card>

      <!-- Section 2: Re-register Cluster -->
      <v-card>
        <v-card-title>Re-register Cluster</v-card-title>
        <v-card-subtitle>Select a cluster to re-register (resets vault and admin_dr credentials)</v-card-subtitle>
        <v-card-text>
          <v-select
            v-model="selectedServerReregister"
            :items="availableServers"
            label="Select Cluster"
            density="comfortable"
            variant="outlined"
            clearable
          />
          <v-btn
            v-if="selectedServerReregister"
            class="mt-4"
            color="primary"
            variant="tonal"
            :loading="reregistering"
            @click="confirmReregister"
          >
            <v-icon start>mdi-refresh</v-icon>
            Re-register Cluster
          </v-btn>
        </v-card-text>
      </v-card>
    </template>

    <v-dialog v-model="deleteDialog" max-width="400" persistent>
      <v-card>
        <v-card-title>Delete User?</v-card-title>
        <v-card-text>
          Delete {{ userToDelete?.username }} from {{ selectedServer }}? This action cannot be undone.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="deleteDialog = false">Cancel</v-btn>
          <v-btn color="error" :loading="deleting" @click="doDelete">Delete</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="reregisterDialog" max-width="450" persistent>
      <v-card>
        <v-card-title>Re-register Cluster?</v-card-title>
        <v-card-text>
          Re-register <strong>{{ selectedServerReregister }}</strong>? This will reset vault and admin_dr credentials.
          The cluster must have admin/admin credentials. This action is for managers only.
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn @click="reregisterDialog = false">Cancel</v-btn>
          <v-btn color="primary" :loading="reregistering" @click="doReregister">Re-register</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { apiGet, apiPost } from '../api/client'
import { useSnackbar } from '../composables/useSnackbar'
import { formatDate } from '../utils/dateUtils'

const { success, error: showError } = useSnackbar()

const pageLoading = ref(true)
const isManager = ref(false)
const isSuperAdmin = ref(false)
const managedClusters = ref([])
const servers = ref([])
const serversDetail = ref([])
const selectedServer = ref(null)
const selectedServerReregister = ref(null)
const clusterData = ref(null)
const loadingUsers = ref(false)
const clusterLoadError = ref(null)
const deleteDialog = ref(false)
const userToDelete = ref(null)
const deleting = ref(false)
const reregisterDialog = ref(false)
const reregistering = ref(false)

// Only show clusters that the user manages (super admin sees all)
const availableServers = computed(() =>
  isSuperAdmin.value ? servers.value : servers.value.filter(s => managedClusters.value.includes(s))
)

// Find managers for selected cluster
const selectedClusterManagers = computed(() => {
  if (!selectedServer.value) return []
  const detail = serversDetail.value.find(s => s.host === selectedServer.value)
  return detail?.manager_emails || []
})

async function loadClusterUsers() {
  if (!selectedServer.value) {
    clusterData.value = null
    clusterLoadError.value = null
    return
  }
  loadingUsers.value = true
  clusterLoadError.value = null
  try {
    const r = await apiGet(`/api/cluster_users?ambari_server=${encodeURIComponent(selectedServer.value)}`)
    clusterData.value = r.data
  } catch (e) {
    clusterData.value = null
    clusterLoadError.value = e.response?.data?.error || 'Failed to load cluster users'
  } finally {
    loadingUsers.value = false
  }
}

function confirmDelete(user) {
  userToDelete.value = user
  deleteDialog.value = true
}

async function doDelete() {
  if (!userToDelete.value) return
  deleting.value = true
  try {
    await apiPost('/api/manager/delete_user', { user_name: userToDelete.value.username })
    deleteDialog.value = false
    success(`User '${userToDelete.value.username}' deleted successfully`)
    userToDelete.value = null
    loadClusterUsers()
  } catch (e) {
    showError(e.response?.data?.error || 'Delete failed. Please try again.')
  } finally {
    deleting.value = false
  }
}

function confirmReregister() {
  reregisterDialog.value = true
}

async function doReregister() {
  if (!selectedServerReregister.value) return
  const detail = serversDetail.value.find(s => s.host === selectedServerReregister.value)
  if (!detail) {
    showError('Server details not found. Please refresh and try again.')
    return
  }
  reregistering.value = true
  try {
    await apiPost('/api/re-register', {
      ambari_server: detail.host,
      port: detail.port,
      http_method: detail.http_method || 'http',
    })
    reregisterDialog.value = false
    success(`Cluster '${selectedServerReregister.value}' re-registered successfully`)
    if (selectedServer.value === selectedServerReregister.value) loadClusterUsers()
  } catch (e) {
    showError(e.response?.data?.error || 'Re-registration failed. Ensure admin/admin credentials are active.')
  } finally {
    reregistering.value = false
  }
}

onMounted(async () => {
  try {
    const [srv, me] = await Promise.all([
      apiGet('/api/servers'),
      apiGet('/api/me').catch(() => ({ data: {} })),
    ])
    servers.value = srv.data?.servers || []
    serversDetail.value = srv.data?.servers_detail || []
    isSuperAdmin.value = me.data?.is_super_admin ?? false
    managedClusters.value = me.data?.managed_clusters ?? []
    isManager.value = me.data?.is_manager ?? false
  } catch {
    servers.value = []
    serversDetail.value = []
  } finally {
    pageLoading.value = false
  }
})
</script>
