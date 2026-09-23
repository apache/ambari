<template>
  <div>
    <v-row class="mb-4">
      <v-col cols="12">
        <div class="d-flex align-center gap-3">
          <v-icon color="error" size="32">mdi-shield-crown</v-icon>
          <div>
            <h1 class="text-h4 font-weight-bold mb-0">Admin Panel</h1>
            <p class="text-body-2 text-medium-emphasis mb-0">Super admin controls — cluster management and access delegation</p>
          </div>
        </div>
      </v-col>
    </v-row>

    <!-- Access guard -->
    <v-alert v-if="!isSuperAdmin && !loading" type="error" variant="tonal" class="mb-4" prominent>
      <template #prepend><v-icon>mdi-lock</v-icon></template>
      <strong>Access Denied</strong> — this panel requires super admin privileges.
    </v-alert>

    <template v-if="isSuperAdmin">
      <!-- Stats row -->
      <v-row class="mb-4">
        <v-col cols="12" sm="4">
          <v-card color="error" variant="tonal">
            <v-card-text class="d-flex align-center justify-space-between">
              <div>
                <p class="text-caption text-medium-emphasis">Registered Clusters</p>
                <p class="text-h4 font-weight-bold">{{ clusters.length }}</p>
              </div>
              <v-icon size="40" color="error">mdi-server-network</v-icon>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" sm="4">
          <v-card color="warning" variant="tonal">
            <v-card-text class="d-flex align-center justify-space-between">
              <div>
                <p class="text-caption text-medium-emphasis">Cluster Managers</p>
                <p class="text-h4 font-weight-bold">{{ totalManagers }}</p>
              </div>
              <v-icon size="40" color="warning">mdi-account-cog</v-icon>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" sm="4">
          <v-card color="primary" variant="tonal">
            <v-card-text class="d-flex align-center justify-space-between">
              <div>
                <p class="text-caption text-medium-emphasis">Total Active Users</p>
                <p class="text-h4 font-weight-bold">{{ totalActiveUsers }}</p>
              </div>
              <v-icon size="40" color="primary">mdi-account-check</v-icon>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <!-- Cluster management table -->
      <v-card class="mb-4">
        <v-card-title class="d-flex align-center py-4">
          <v-icon class="mr-2" color="error">mdi-server</v-icon>
          Cluster Administration
          <v-spacer />
          <v-btn color="primary" prepend-icon="mdi-plus-network" size="small" to="/clusters/register">
            Register New
          </v-btn>
        </v-card-title>
        <v-divider />
        <v-card-text class="pa-0">
          <v-progress-linear v-if="loading" indeterminate color="primary" />
          <v-table v-else-if="clusters.length">
            <thead>
              <tr>
                <th>Cluster</th>
                <th>Connection</th>
                <th>Managers</th>
                <th>Policy</th>
                <th>Active Users</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cluster in clusters" :key="cluster.host">
                <td>
                  <div class="d-flex align-center gap-2">
                    <v-avatar color="primary" variant="tonal" size="32">
                      <v-icon size="small">mdi-server</v-icon>
                    </v-avatar>
                    <span class="font-weight-medium">{{ cluster.host }}</span>
                  </div>
                </td>
                <td>
                  <v-chip size="x-small" variant="tonal" color="success">
                    {{ cluster.http_method }}://{{ cluster.host }}:{{ cluster.port }}
                  </v-chip>
                </td>
                <td>
                  <div v-if="cluster.manager_emails && cluster.manager_emails.length" class="d-flex flex-wrap gap-1">
                    <v-chip v-for="mgr in cluster.manager_emails" :key="mgr" size="x-small" color="warning" variant="tonal">
                      {{ mgr }}
                    </v-chip>
                  </div>
                  <span v-else class="text-caption text-medium-emphasis">None assigned</span>
                </td>
                <td>
                  <v-chip
                    size="x-small"
                    :color="cluster.single_user_mode ? 'info' : 'success'"
                    variant="tonal"
                    :prepend-icon="cluster.single_user_mode ? 'mdi-account-lock' : 'mdi-account-multiple'"
                  >
                    {{ cluster.single_user_mode ? 'Single user' : 'Multi-user' }}
                  </v-chip>
                </td>
                <td>
                  <v-chip size="x-small" color="success" variant="flat">
                    {{ clusterUserCounts[cluster.host] ?? '…' }}
                  </v-chip>
                </td>
                <td>
                  <div class="d-flex gap-2">
                    <v-btn
                      size="small"
                      color="warning"
                      variant="tonal"
                      prepend-icon="mdi-cog"
                      @click="openEditSettings(cluster)"
                    >
                      Configure
                    </v-btn>
                    <v-btn
                      size="small"
                      color="error"
                      variant="tonal"
                      prepend-icon="mdi-delete"
                      @click="confirmDeleteCluster(cluster)"
                    >
                      Delete
                    </v-btn>
                  </div>
                </td>
              </tr>
            </tbody>
          </v-table>
          <v-alert v-else type="info" variant="tonal" class="ma-4">
            No clusters registered yet.
          </v-alert>
        </v-card-text>
      </v-card>
    </template>

    <!-- Configure Cluster Dialog (managers + access policy) -->
    <v-dialog v-model="editSettingsDialog" max-width="600" persistent>
      <v-card rounded="xl">
        <!-- Header -->
        <div class="pa-5 d-flex align-center gap-3">
          <v-avatar color="warning" variant="tonal" size="44" rounded="lg">
            <v-icon color="warning">mdi-cog</v-icon>
          </v-avatar>
          <div>
            <div class="text-subtitle-1 font-weight-bold">Configure Cluster</div>
            <div class="text-caption text-medium-emphasis font-mono">{{ editingCluster?.host }}</div>
          </div>
        </div>
        <v-divider />

        <v-card-text class="pt-4">
          <!-- Managers section -->
          <p class="text-caption text-medium-emphasis font-weight-bold text-uppercase mb-2" style="letter-spacing:.05em">
            Cluster Managers
          </p>
          <p class="text-caption text-medium-emphasis mb-3">
            Managers can delete temporary users and re-register this cluster.
          </p>
          <div class="d-flex flex-wrap gap-2 mb-3">
            <v-chip
              v-for="(mgr, idx) in editingManagers"
              :key="mgr"
              size="small"
              color="warning"
              variant="tonal"
              closable
              @click:close="removeManager(idx)"
            >
              {{ mgr }}
            </v-chip>
            <v-chip v-if="!editingManagers.length" size="small" variant="outlined" color="grey">
              No managers assigned
            </v-chip>
          </div>
          <v-text-field
            v-model="newManagerEmail"
            label="Add manager email"
            variant="outlined"
            density="comfortable"
            placeholder="user@company.com"
            prepend-inner-icon="mdi-email-plus-outline"
            :error-messages="newManagerEmailError"
            hint="Press Enter or click Add"
            @keyup.enter="addManager"
            @update:model-value="newManagerEmailError = ''"
          >
            <template #append>
              <v-btn color="warning" variant="tonal" size="small" :disabled="!newManagerEmail" @click="addManager">
                Add
              </v-btn>
            </template>
          </v-text-field>

          <v-divider class="my-4" />

          <!-- Access policy section -->
          <p class="text-caption text-medium-emphasis font-weight-bold text-uppercase mb-2" style="letter-spacing:.05em">
            Access Policy
          </p>
          <p class="text-caption text-medium-emphasis mb-3">
            Choose how many temporary users can be active on this cluster at the same time.
          </p>
          <v-row dense>
            <!-- Option A: Multiple users -->
            <v-col cols="12" sm="6">
              <div
                class="policy-card"
                :class="{ 'policy-card--selected-multi': !editingSingleUserMode }"
                @click="editingSingleUserMode = false"
              >
                <div class="policy-card__icon-wrap policy-card__icon-wrap--multi">
                  <v-icon size="22" color="success">mdi-account-multiple</v-icon>
                </div>
                <div class="policy-card__content">
                  <div class="policy-card__title">Multiple Users</div>
                  <div class="policy-card__desc">Several users can hold active credentials simultaneously.</div>
                </div>
                <v-icon v-if="!editingSingleUserMode" size="16" color="success" class="flex-shrink-0">mdi-check-circle</v-icon>
                <v-icon v-else size="16" color="grey-lighten-1" class="flex-shrink-0">mdi-circle-outline</v-icon>
              </div>
            </v-col>
            <!-- Option B: Single user -->
            <v-col cols="12" sm="6">
              <div
                class="policy-card"
                :class="{ 'policy-card--selected-single': editingSingleUserMode }"
                @click="editingSingleUserMode = true"
              >
                <div class="policy-card__icon-wrap policy-card__icon-wrap--single">
                  <v-icon size="22" color="info">mdi-account-lock</v-icon>
                </div>
                <div class="policy-card__content">
                  <div class="policy-card__title">Single User at a Time</div>
                  <div class="policy-card__desc">New requests are blocked while an active user exists.</div>
                </div>
                <v-icon v-if="editingSingleUserMode" size="16" color="info" class="flex-shrink-0">mdi-check-circle</v-icon>
                <v-icon v-else size="16" color="grey-lighten-1" class="flex-shrink-0">mdi-circle-outline</v-icon>
              </div>
            </v-col>
          </v-row>
        </v-card-text>

        <v-divider />
        <div class="pa-4 d-flex justify-end gap-2">
          <v-btn variant="text" @click="editSettingsDialog = false">Cancel</v-btn>
          <v-btn color="warning" rounded="lg" :loading="savingSettings" prepend-icon="mdi-check" @click="saveSettings">
            Save Changes
          </v-btn>
        </div>
      </v-card>
    </v-dialog>

    <!-- Delete Cluster Dialog -->
    <v-dialog v-model="deleteClusterDialog" max-width="480" persistent>
      <v-card rounded="xl">
        <div class="pa-5 d-flex align-center gap-3">
          <v-avatar color="error" variant="tonal" size="44" rounded="lg">
            <v-icon color="error">mdi-alert-circle</v-icon>
          </v-avatar>
          <div>
            <div class="text-subtitle-1 font-weight-bold">Delete Cluster?</div>
            <div class="text-caption text-medium-emphasis">This action is irreversible</div>
          </div>
        </div>
        <v-divider />
        <v-card-text class="pt-4">
          <v-alert type="error" variant="tonal" density="compact" rounded="lg" class="mb-3">
            Deleting <strong>{{ deletingCluster?.host }}</strong> will:
            <ul class="mt-1 pl-4">
              <li>Remove all active temporary users from Ambari</li>
              <li>Restore <code>admin</code> / <code>admin</code> credentials on the Ambari server</li>
              <li>Delete the <code>vault</code> and <code>ambari_admin_dr</code> service accounts</li>
              <li>Remove the cluster from Kavach permanently</li>
            </ul>
          </v-alert>
          <p class="text-body-2 mb-2">Type the cluster hostname to confirm:</p>
          <v-text-field
            v-model="deleteConfirmText"
            :placeholder="deletingCluster?.host"
            variant="outlined"
            density="comfortable"
          />
        </v-card-text>
        <v-divider />
        <div class="pa-4 d-flex justify-end gap-2">
          <v-btn variant="text" @click="deleteClusterDialog = false">Cancel</v-btn>
          <v-btn
            color="error"
            rounded="lg"
            :loading="deletingClusterLoading"
            :disabled="deleteConfirmText !== deletingCluster?.host"
            @click="doDeleteCluster"
          >
            Delete Permanently
          </v-btn>
        </div>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
/* eslint-disable vue/multi-word-component-names */
import { ref, computed, onMounted } from 'vue'
import { apiGet } from '../api/client'
import { useSnackbar } from '../composables/useSnackbar'
import axios from 'axios'

const { success, error: showError } = useSnackbar()

const loading = ref(true)
const isSuperAdmin = ref(false)
const clusters = ref([])
const clusterUserCounts = ref({})

const editSettingsDialog = ref(false)
const editingCluster = ref(null)
const editingManagers = ref([])
const editingSingleUserMode = ref(false)
const newManagerEmail = ref('')
const newManagerEmailError = ref('')
const savingSettings = ref(false)

const deleteClusterDialog = ref(false)
const deletingCluster = ref(null)
const deleteConfirmText = ref('')
const deletingClusterLoading = ref(false)

const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

const totalManagers = computed(() =>
  clusters.value.reduce((sum, c) => sum + (c.manager_emails?.length ?? 0), 0)
)
const totalActiveUsers = computed(() =>
  Object.values(clusterUserCounts.value).reduce((s, v) => s + v, 0)
)

async function loadData() {
  loading.value = true
  try {
    const [me, srv] = await Promise.all([
      apiGet('/api/me'),
      apiGet('/api/servers'),
    ])
    isSuperAdmin.value = me.data?.is_super_admin ?? false
    clusters.value = srv.data?.servers_detail ?? []

    clusters.value.forEach(async (c) => {
      try {
        const r = await apiGet(`/api/cluster_users?ambari_server=${encodeURIComponent(c.host)}`)
        clusterUserCounts.value[c.host] = r.data?.active_users_count ?? 0
      } catch {
        clusterUserCounts.value[c.host] = 0
      }
    })
  } catch {
    clusters.value = []
  } finally {
    loading.value = false
  }
}

function openEditSettings(cluster) {
  editingCluster.value = cluster
  editingManagers.value = [...(cluster.manager_emails || [])]
  editingSingleUserMode.value = !!cluster.single_user_mode
  newManagerEmail.value = ''
  newManagerEmailError.value = ''
  editSettingsDialog.value = true
}

function addManager() {
  const email = newManagerEmail.value.trim().toLowerCase()
  if (!email) return
  if (!EMAIL_REGEX.test(email)) { newManagerEmailError.value = 'Enter a valid email address'; return }
  if (editingManagers.value.includes(email)) { newManagerEmailError.value = 'Email already added'; return }
  editingManagers.value.push(email)
  newManagerEmail.value = ''
  newManagerEmailError.value = ''
}

function removeManager(idx) {
  editingManagers.value.splice(idx, 1)
}

async function saveSettings() {
  if (!editingCluster.value) return
  savingSettings.value = true
  try {
    const token = localStorage.getItem('access_token')
    const email = localStorage.getItem('user_email')
    await axios.put(
      `/api/clusters/${encodeURIComponent(editingCluster.value.host)}/managers`,
      { manager_emails: editingManagers.value, single_user_mode: editingSingleUserMode.value },
      { headers: { Authorization: `Bearer ${token}`, 'X-Email': email } }
    )
    success(`Cluster '${editingCluster.value.host}' settings updated`)
    editSettingsDialog.value = false
    const idx = clusters.value.findIndex(c => c.host === editingCluster.value.host)
    if (idx !== -1) {
      clusters.value[idx] = {
        ...clusters.value[idx],
        manager_emails: [...editingManagers.value],
        single_user_mode: editingSingleUserMode.value,
      }
    }
  } catch (e) {
    showError(e.response?.data?.error || 'Failed to update cluster settings')
  } finally {
    savingSettings.value = false
  }
}

function confirmDeleteCluster(cluster) {
  deletingCluster.value = cluster
  deleteConfirmText.value = ''
  deleteClusterDialog.value = true
}

async function doDeleteCluster() {
  if (!deletingCluster.value) return
  deletingClusterLoading.value = true
  try {
    const token = localStorage.getItem('access_token')
    const email = localStorage.getItem('user_email')
    await axios.delete(
      `/api/clusters/${encodeURIComponent(deletingCluster.value.host)}`,
      { headers: { Authorization: `Bearer ${token}`, 'X-Email': email } }
    )
    success(`Cluster '${deletingCluster.value.host}' deleted`)
    deleteClusterDialog.value = false
    clusters.value = clusters.value.filter(c => c.host !== deletingCluster.value.host)
    delete clusterUserCounts.value[deletingCluster.value.host]
  } catch (e) {
    showError(e.response?.data?.error || 'Failed to delete cluster')
  } finally {
    deletingClusterLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.font-mono { font-family: 'Consolas', 'Monaco', monospace; }

/* Policy selector cards — same design as RegisterClusterForm */
.policy-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
  background: #fff;
  user-select: none;
  height: 100%;
}

.policy-card:hover {
  border-color: rgba(0, 0, 0, 0.2);
  background: rgba(0, 0, 0, 0.01);
}

.policy-card--selected-multi {
  border-color: rgb(var(--v-theme-success)) !important;
  background: rgba(var(--v-theme-success), 0.05) !important;
  box-shadow: 0 0 0 1px rgba(var(--v-theme-success), 0.25);
}

.policy-card--selected-single {
  border-color: rgb(var(--v-theme-info)) !important;
  background: rgba(var(--v-theme-info), 0.05) !important;
  box-shadow: 0 0 0 1px rgba(var(--v-theme-info), 0.25);
}

.policy-card__icon-wrap {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.policy-card__icon-wrap--multi { background: rgba(var(--v-theme-success), 0.12); }
.policy-card__icon-wrap--single { background: rgba(var(--v-theme-info), 0.12); }

.policy-card__content { flex: 1; min-width: 0; }

.policy-card__title {
  font-size: 0.82rem;
  font-weight: 600;
  line-height: 1.3;
  margin-bottom: 2px;
}

.policy-card__desc {
  font-size: 0.72rem;
  color: rgba(0, 0, 0, 0.5);
  line-height: 1.4;
}
</style>
