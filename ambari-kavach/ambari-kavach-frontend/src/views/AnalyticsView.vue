<template>
  <div>
    <v-row class="mb-4">
      <v-col cols="12">
        <h1 class="text-h4 font-weight-bold mb-1">Cluster Analytics</h1>
        <p class="text-body-2 text-medium-emphasis">
          View health, resources, and services for a registered cluster. Each Ambari server manages one cluster.
        </p>
      </v-col>
    </v-row>

    <v-row>
      <v-col cols="12" md="6">
        <v-select
          v-model="selectedCluster"
          :items="servers"
          label="Cluster"
          density="comfortable"
          variant="outlined"
          clearable
          hint="Select a registered Ambari cluster (server hostname)"
          @update:model-value="loadOverview"
        />
        <!-- Manager info for selected cluster -->
        <div v-if="selectedClusterManagers.length" class="mt-2 d-flex flex-wrap align-center gap-1">
          <span class="text-caption text-medium-emphasis mr-1">Managers:</span>
          <v-chip
            v-for="mgr in selectedClusterManagers"
            :key="mgr"
            size="x-small"
            color="warning"
            variant="tonal"
            prepend-icon="mdi-account-cog"
          >{{ mgr }}</v-chip>
        </div>
      </v-col>
    </v-row>

    <v-progress-linear v-if="loadingOverview" indeterminate class="mb-4" />

    <template v-if="overview">
      <!-- Cluster info banner -->
      <v-alert v-if="overview.cluster_info?.cluster_name" type="info" variant="tonal" density="compact" class="mb-4">
        <strong>{{ overview.cluster_info.cluster_name }}</strong>
        <span v-if="overview.cluster_info?.version"> · v{{ overview.cluster_info.version }}</span>
        <span v-if="overview.health_summary?.critical_alerts" class="ml-2">
          · <v-icon color="error" size="small">mdi-alert</v-icon> {{ overview.health_summary.critical_alerts }} critical
        </span>
        <span v-if="overview.health_summary?.warning_alerts" class="ml-1">
          · <v-icon color="warning" size="small">mdi-alert</v-icon> {{ overview.health_summary.warning_alerts }} warnings
        </span>
      </v-alert>

      <!-- Resource metrics -->
      <v-row>
        <v-col cols="6" sm="4" md="2">
          <v-card variant="tonal" color="primary">
            <v-card-text class="text-center py-4">
              <v-icon size="32" color="primary">mdi-server</v-icon>
              <p class="text-caption mt-1 mb-0">Hosts</p>
              <p class="text-h5 font-weight-bold">{{ overview.resource_summary?.total_hosts || 0 }}</p>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="4" md="2">
          <v-card variant="tonal" color="info">
            <v-card-text class="text-center py-4">
              <v-icon size="32" color="info">mdi-memory</v-icon>
              <p class="text-caption mt-1 mb-0">Memory (GB)</p>
              <p class="text-h5 font-weight-bold">{{ overview.resource_summary?.total_memory_gb ?? 0 }}</p>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="4" md="2">
          <v-card variant="tonal" color="success">
            <v-card-text class="text-center py-4">
              <v-icon size="32" color="success">mdi-chip</v-icon>
              <p class="text-caption mt-1 mb-0">CPU Cores</p>
              <p class="text-h5 font-weight-bold">{{ overview.resource_summary?.total_cpu_cores ?? 0 }}</p>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="4" md="2">
          <v-card variant="tonal" color="secondary">
            <v-card-text class="text-center py-4">
              <v-icon size="32" color="secondary">mdi-harddisk</v-icon>
              <p class="text-caption mt-1 mb-0">Disk (GB)</p>
              <p class="text-h5 font-weight-bold">{{ overview.resource_summary?.total_disk_gb ?? 0 }}</p>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="6" sm="4" md="2">
          <v-card variant="tonal" color="warning">
            <v-card-text class="text-center py-4">
              <v-icon size="32" color="warning">mdi-cog</v-icon>
              <p class="text-caption mt-1 mb-0">Services</p>
              <p class="text-h5 font-weight-bold">{{ overview.resource_summary?.total_services ?? 0 }}</p>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <v-row class="mt-4">
        <v-col cols="12" md="4">
          <v-card variant="outlined">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2">mdi-cog</v-icon>
              Services
            </v-card-title>
            <v-divider />
            <v-card-text>
              <v-chip
                v-for="s in (overview.services || [])"
                :key="s.service_name"
                :color="s.state === 'STARTED' ? 'success' : 'warning'"
                size="small"
                class="ma-1"
              >
                {{ s.service_name }}: {{ s.state }}
              </v-chip>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" md="4">
          <v-card variant="outlined">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2">mdi-server</v-icon>
              Hosts
            </v-card-title>
            <v-divider />
            <v-card-text>
              <v-list density="compact">
                <v-list-item
                  v-for="h in (overview.hosts || []).slice(0, 10)"
                  :key="h.hostname"
                  :title="h.hostname"
                  :subtitle="`${h.memory_gb ?? 0} GB RAM · ${h.cpu_cores ?? 0} cores · ${h.disk_gb ?? 0} GB disk`"
                />
              </v-list>
              <p v-if="(overview.hosts || []).length > 10" class="text-caption">
                + {{ (overview.hosts || []).length - 10 }} more
              </p>
            </v-card-text>
          </v-card>
        </v-col>
        <v-col cols="12" md="4">
          <v-card variant="outlined">
            <v-card-title class="d-flex align-center">
              <v-icon class="mr-2">mdi-account-group</v-icon>
              All Users (this cluster)
            </v-card-title>
            <v-divider />
            <v-card-text style="max-height: 280px; overflow-y: auto;">
              <div v-for="u in (overview.past_users || [])" :key="u.username + (u.created_at || '')" class="d-flex align-center py-2">
                <v-chip :color="u.status === 'active' ? 'success' : 'default'" size="small" class="mr-2">{{ u.status }}</v-chip>
                <div>
                  <div class="font-weight-medium">{{ u.username }}</div>
                  <div class="text-caption text-medium-emphasis">{{ u.email }}</div>
                </div>
              </div>
              <p v-if="!(overview.past_users || []).length" class="text-body-2 text-medium-emphasis">No users created yet</p>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </template>

    <v-alert v-else-if="selectedCluster && !loadingOverview && apiError" type="error" variant="tonal" class="mt-4">
      <strong>Failed to load cluster data</strong>
      <p class="mt-2 mb-0">{{ apiError }}</p>
      <p class="text-caption mt-2 mb-0">Ensure the Ambari server is running and reachable. Check that the cluster is registered with the correct host and port.</p>
      <v-btn size="small" variant="tonal" color="error" class="mt-3" prepend-icon="mdi-refresh" @click="loadOverview">Retry</v-btn>
    </v-alert>
    <v-alert v-else-if="selectedCluster && !loadingOverview && !overview" type="info" variant="tonal">
      Select a cluster to view its analytics. Clusters are registered Ambari servers (typically one cluster per server).
    </v-alert>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { apiGet } from '../api/client'

const route = useRoute()
const servers = ref([])
const serversDetail = ref([])
const selectedCluster = ref(null)
const overview = ref(null)
const loadingOverview = ref(false)
const apiError = ref(null)

const selectedClusterManagers = computed(() => {
  if (!selectedCluster.value) return []
  const detail = serversDetail.value.find(s => s.host === selectedCluster.value)
  return detail?.manager_emails || []
})

onMounted(async () => {
  try {
    const r = await apiGet('/api/servers')
    servers.value = r.data?.servers || []
    serversDetail.value = r.data?.servers_detail || []
    const clusterFromQuery = route.query?.cluster
    if (clusterFromQuery && servers.value.includes(clusterFromQuery)) {
      selectedCluster.value = clusterFromQuery
      await loadOverview()
    }
  } catch {
    servers.value = []
    serversDetail.value = []
  }
})

async function loadOverview() {
  if (!selectedCluster.value) {
    overview.value = null
    apiError.value = null
    return
  }
  loadingOverview.value = true
  apiError.value = null
  try {
    const r = await apiGet(`/api/ambari/clusters?ambari_server=${encodeURIComponent(selectedCluster.value)}`)
    const items = r.data?.items || []
    const clusterName = items[0]?.Clusters?.cluster_name
    if (!clusterName) {
      overview.value = null
      apiError.value = 'No cluster found on this Ambari server.'
      return
    }
    const ov = await apiGet(
      `/api/analytics/cluster_overview?ambari_server=${encodeURIComponent(selectedCluster.value)}&cluster_name=${encodeURIComponent(clusterName)}`
    )
    overview.value = ov.data
  } catch (err) {
    overview.value = null
    const msg = err.response?.data?.error || err.message || 'Unknown error'
    apiError.value = msg
    if (err.response?.status === 404) {
      apiError.value = 'Ambari server not registered or not found.'
    } else if (msg.includes('Connection') || msg.includes('timeout') || msg.includes('refused')) {
      apiError.value = `Cannot connect to Ambari at ${selectedCluster.value}. Ensure the Ambari server is running and the host/port are correct.`
    }
  } finally {
    loadingOverview.value = false
  }
}
</script>
