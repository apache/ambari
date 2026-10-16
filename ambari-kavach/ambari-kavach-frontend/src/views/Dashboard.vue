<template>
  <div>
    <v-row class="mb-4">
      <v-col cols="12" class="d-flex align-center justify-space-between">
        <div>
          <h1 class="text-h4 font-weight-bold mb-1">Dashboard</h1>
          <p class="text-body-2 text-medium-emphasis mb-0">Overview of your Ambari access and clusters</p>
        </div>
        <v-btn variant="tonal" size="small" :loading="loading || loadingServers" prepend-icon="mdi-refresh" @click="refresh">
          Refresh
        </v-btn>
      </v-col>
    </v-row>

    <!-- Role-aware welcome banner -->
    <v-alert
      :type="isSuperAdmin ? 'error' : isManager ? 'warning' : 'info'"
      variant="tonal"
      density="compact"
      class="mb-4"
    >
      <template #prepend>
        <v-icon>{{ isSuperAdmin ? 'mdi-shield-crown' : isManager ? 'mdi-account-cog' : 'mdi-information' }}</v-icon>
      </template>
      <span v-if="isSuperAdmin">
        Welcome, <strong>{{ firstName }}</strong>. You have <strong>Super Admin</strong> access — you can delete clusters and manage the full system.
      </span>
      <span v-else-if="isManager">
        Welcome, <strong>{{ firstName }}</strong>. You are a <strong>Cluster Manager</strong> for {{ managedClusters.length }} cluster(s).
      </span>
      <span v-else>
        <strong>Ambari Kavach</strong> provides temporary, time-limited Ambari credentials. Create users only when needed; all access is audited.
        Use <strong>Cluster Read</strong> for view-only operations.
      </span>
    </v-alert>

    <!-- Stats Cards -->
    <v-row>
      <v-col cols="12" sm="6" md="3">
        <v-card class="fill-height" color="primary" variant="tonal">
          <v-card-text>
            <div class="d-flex align-center justify-space-between">
              <div>
                <p class="text-caption text-medium-emphasis">Active Users</p>
                <p class="text-h4 font-weight-bold">{{ activeCount }}</p>
              </div>
              <v-icon size="48" color="primary">mdi-account-check</v-icon>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card class="fill-height" color="success" variant="tonal">
          <v-card-text>
            <div class="d-flex align-center justify-space-between">
              <div>
                <p class="text-caption text-medium-emphasis">Registered Clusters</p>
                <p class="text-h4 font-weight-bold">{{ servers.length }}</p>
              </div>
              <v-icon size="48" color="success">mdi-server</v-icon>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card class="fill-height" color="info" variant="tonal">
          <v-card-text>
            <div class="d-flex align-center justify-space-between">
              <div>
                <p class="text-caption text-medium-emphasis">Expired Users</p>
                <p class="text-h4 font-weight-bold">{{ expiredCount }}</p>
              </div>
              <v-icon size="48" color="info">mdi-account-clock</v-icon>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card class="fill-height" color="warning" variant="tonal">
          <v-card-text>
            <div class="d-flex align-center justify-space-between">
              <div>
                <p class="text-caption text-medium-emphasis">Quick Action</p>
                <p class="text-body-2 font-weight-medium">Create User</p>
              </div>
              <v-btn color="warning" icon="mdi-plus" :to="{ name: 'CreateAmbariUser' }" />
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Quick Actions & Active Users -->
    <v-row>
      <v-col cols="12" md="8">
        <v-card>
          <v-card-title class="d-flex align-center">
            <v-icon class="mr-2">mdi-account-group</v-icon>
            My Active Users
            <v-spacer />
            <v-btn color="primary" size="small" :to="{ name: 'MyUsers' }">View All</v-btn>
          </v-card-title>
          <v-divider />
          <v-card-text>
            <v-progress-linear v-if="loading" indeterminate color="primary" />
            <v-list v-else-if="activeUsers.length">
              <v-list-item
                v-for="u in activeUsers.slice(0, 5)"
                :key="u.username"
                :subtitle="u.ambari_server"
                lines="two"
              >
                <template #prepend>
                  <v-icon color="success">mdi-check-circle</v-icon>
                </template>
                <v-list-item-title>{{ u.username }}</v-list-item-title>
                <v-list-item-subtitle>
                  <v-chip :color="expiryColor(u.expire_time)" size="x-small" class="mr-1">{{ timeUntil(u.expire_time) }}</v-chip>
                  {{ u.ambari_server }}
                </v-list-item-subtitle>
                <template #append>
                  <v-tooltip text="Copy password" location="top">
                    <template #activator="{ props }">
                      <v-btn size="small" variant="text" icon="mdi-content-copy" v-bind="props" @click="copyPassword(u.password)" />
                    </template>
                  </v-tooltip>
                </template>
              </v-list-item>
            </v-list>
            <v-alert v-else type="info" variant="tonal" class="ma-0">
              No active users. Temporary users expire after the selected duration. <router-link to="/create_ambari_users">Create one</router-link>
            </v-alert>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card variant="outlined">
          <v-card-title class="d-flex align-center py-3">
            <v-icon class="mr-2" color="primary">mdi-server-network</v-icon>
            Registered Clusters
            <v-spacer />
            <v-btn v-if="servers.length" size="small" variant="text" :to="{ path: '/clusters' }">
              View all
            </v-btn>
          </v-card-title>
          <v-divider />
          <v-card-text class="pa-3">
            <v-progress-linear v-if="loadingServers" indeterminate color="primary" />
            <v-list v-else-if="servers.length" density="compact" class="py-0">
              <v-list-item
                v-for="s in servers.slice(0, 5)"
                :key="s"
                :to="{ path: '/analytics', query: { cluster: s } }"
                class="px-0"
                rounded="lg"
              >
                <template #prepend>
                  <v-avatar color="primary" variant="tonal" size="36">
                    <v-icon size="small">mdi-server</v-icon>
                  </v-avatar>
                </template>
                <v-list-item-title class="text-body-2 font-weight-medium">{{ s }}</v-list-item-title>
                <template #append>
                  <v-icon size="small">mdi-chevron-right</v-icon>
                </template>
              </v-list-item>
              <v-list-item
                v-if="servers.length > 5"
                :to="{ path: '/clusters' }"
                class="px-0"
                rounded="lg"
              >
                <v-list-item-title class="text-caption text-medium-emphasis">
                  +{{ servers.length - 5 }} more
                </v-list-item-title>
                <template #append>
                  <v-icon size="small">mdi-chevron-right</v-icon>
                </template>
              </v-list-item>
            </v-list>
            <v-alert v-else type="info" variant="tonal" density="compact" class="ma-0">
              No clusters registered. Managers can register from the <router-link to="/clusters">Clusters</router-link> page.
            </v-alert>
          </v-card-text>
        </v-card>

        <v-card class="mt-4">
          <v-card-title>Quick Actions</v-card-title>
          <v-divider />
          <v-card-text>
            <v-list density="compact">
              <v-list-item to="/create_ambari_users" prepend-icon="mdi-account-plus" title="Create Ambari User" />
              <v-list-item to="/myusers" prepend-icon="mdi-account-group" title="View My Users" />
              <v-list-item v-if="isSuperAdmin" to="/clusters/register" prepend-icon="mdi-plus-network" title="Register Cluster" />
              <v-list-item v-if="isManager" to="/clusters/manager" prepend-icon="mdi-cog" title="Cluster Manager" />
              <v-list-item v-if="isSuperAdmin" to="/admin" prepend-icon="mdi-shield-crown" title="Admin Panel" class="text-error" />
              <v-list-item to="/analytics" prepend-icon="mdi-chart-box" title="Cluster Analytics" />
              <v-list-item to="/audit-logs" prepend-icon="mdi-history" title="Audit Logs" />
            </v-list>
          </v-card-text>
        </v-card>

        <!-- Recent Activity -->
        <v-card class="mt-4" variant="outlined">
          <v-card-title class="d-flex align-center py-3">
            <v-icon class="mr-2" color="secondary">mdi-history</v-icon>
            Recent Activity
            <v-spacer />
            <v-btn size="small" variant="text" to="/audit-logs">View all</v-btn>
          </v-card-title>
          <v-divider />
          <v-card-text class="pa-2">
            <v-progress-linear v-if="loadingAudit" indeterminate color="primary" />
            <v-list v-else-if="recentAudit.length" density="compact" class="py-0">
              <v-list-item
                v-for="log in recentAudit"
                :key="log.timestamp + log.event"
                class="px-2"
              >
                <template #prepend>
                  <v-icon :color="auditEventColor(log.event)" size="18">{{ auditEventIcon(log.event) }}</v-icon>
                </template>
                <v-list-item-title class="text-caption font-weight-medium">{{ log.event }}</v-list-item-title>
                <v-list-item-subtitle class="text-caption">{{ log.entity }} · {{ log.user }}</v-list-item-subtitle>
              </v-list-item>
            </v-list>
            <p v-else class="text-caption text-medium-emphasis pa-2 mb-0">No recent activity.</p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
/* eslint-disable vue/multi-word-component-names */
import { ref, computed, onMounted } from 'vue'
import { apiGet } from '../api/client'
import { useSnackbar } from '../composables/useSnackbar'
import { useAuthStore } from '../stores/auth'
import { timeUntil, expiryColor } from '../utils/dateUtils'

const { success } = useSnackbar()
const authStore = useAuthStore()

const loading = ref(true)
const loadingServers = ref(true)
const loadingAudit = ref(true)
const activeUsers = ref([])
const expiredUsers = ref([])
const servers = ref([])
const isSuperAdmin = ref(false)
const managedClusters = ref([])
const recentAudit = ref([])

const activeCount = computed(() => activeUsers.value.length)
const expiredCount = computed(() => expiredUsers.value.length)
const isManager = computed(() => isSuperAdmin.value || managedClusters.value.length > 0)
const firstName = computed(() => authStore.userName?.split(' ')[0] || 'there')

const AUDIT_EVENT_COLORS = {
  CLUSTER_REGISTERED: 'success',
  CLUSTER_DELETED: 'error',
  CLUSTER_MANAGERS_UPDATED: 'warning',
  USER_CREATED: 'primary',
  MANAGER_VETO_DELETE: 'error',
  USER_EXPIRED_AUTO_DELETED: 'grey',
  MANAGER_REREGISTRATION_DONE: 'warning',
}
const AUDIT_EVENT_ICONS = {
  CLUSTER_REGISTERED: 'mdi-server-plus',
  CLUSTER_DELETED: 'mdi-server-remove',
  CLUSTER_MANAGERS_UPDATED: 'mdi-account-cog',
  USER_CREATED: 'mdi-account-plus',
  MANAGER_VETO_DELETE: 'mdi-account-remove',
  USER_EXPIRED_AUTO_DELETED: 'mdi-clock-remove',
  MANAGER_REREGISTRATION_DONE: 'mdi-refresh',
}
function auditEventColor(event) { return AUDIT_EVENT_COLORS[event] || 'grey' }
function auditEventIcon(event) { return AUDIT_EVENT_ICONS[event] || 'mdi-history' }

async function copyPassword(pwd) {
  if (pwd) {
    await navigator.clipboard.writeText(pwd)
    success('Password copied to clipboard')
  }
}

async function loadAll() {
  loading.value = true
  loadingServers.value = true
  loadingAudit.value = true
  try {
    const [active, expired, srv, me, audit] = await Promise.allSettled([
      apiGet('/api/active_users'),
      apiGet('/api/expired_users'),
      apiGet('/api/servers'),
      apiGet('/api/me'),
      apiGet('/api/audit_logs?limit=5'),
    ])
    activeUsers.value = active.status === 'fulfilled' ? (active.value.data?.active_users || []) : []
    expiredUsers.value = expired.status === 'fulfilled' ? (expired.value.data?.all_users || []) : []
    servers.value = srv.status === 'fulfilled' ? (srv.value.data?.servers || []) : []
    isSuperAdmin.value = me.status === 'fulfilled' ? (me.value.data?.is_super_admin ?? false) : false
    managedClusters.value = me.status === 'fulfilled' ? (me.value.data?.managed_clusters ?? []) : []
    recentAudit.value = audit.status === 'fulfilled' ? (audit.value.data?.audit_logs || []) : []
  } catch {
    activeUsers.value = []
    expiredUsers.value = []
    servers.value = []
  } finally {
    loading.value = false
    loadingServers.value = false
    loadingAudit.value = false
  }
}

async function refresh() {
  await loadAll()
  success('Dashboard refreshed')
}

onMounted(loadAll)
</script>
