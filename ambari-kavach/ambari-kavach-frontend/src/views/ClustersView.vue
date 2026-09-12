<template>
  <div>
    <v-row class="mb-4">
      <v-col cols="12">
        <h1 class="text-h4 font-weight-bold mb-1">Clusters</h1>
        <p class="text-body-2 text-medium-emphasis">Manage your registered Ambari clusters</p>
      </v-col>
    </v-row>

    <v-row>
      <v-col cols="12" md="3">
        <v-card :to="{ path: '/clusters/register' }" class="fill-height cursor-pointer" variant="outlined">
          <v-card-text class="text-center py-8">
            <v-icon size="56" color="primary">mdi-plus-network</v-icon>
            <h3 class="text-h6 mt-2">Register Cluster</h3>
            <p class="text-body-2 text-medium-emphasis">Add an Ambari cluster</p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card :to="{ path: '/clusters/manager' }" class="fill-height cursor-pointer" variant="outlined">
          <v-card-text class="text-center py-8">
            <v-icon size="56" color="secondary">mdi-cog</v-icon>
            <h3 class="text-h6 mt-2">Cluster Management</h3>
            <p class="text-body-2 text-medium-emphasis">Manage users & re-register</p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" md="3">
        <v-card :to="{ path: '/analytics' }" class="fill-height cursor-pointer" variant="outlined">
          <v-card-text class="text-center py-8">
            <v-icon size="56" color="info">mdi-chart-box</v-icon>
            <h3 class="text-h6 mt-2">Cluster Analytics</h3>
            <p class="text-body-2 text-medium-emphasis">Health, resources, services</p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col v-if="isSuperAdmin" cols="12" md="3">
        <v-card :to="{ path: '/admin' }" class="fill-height cursor-pointer" variant="outlined" color="error">
          <v-card-text class="text-center py-8">
            <v-icon size="56" color="error">mdi-shield-crown</v-icon>
            <h3 class="text-h6 mt-2">Admin Panel</h3>
            <p class="text-body-2 text-medium-emphasis">Delete clusters & manage admins</p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Registered Clusters - Card Grid -->
    <v-card class="mt-4 cluster-section" variant="outlined">
      <v-card-title class="d-flex align-center py-4">
        <v-icon class="mr-2" color="primary">mdi-server-network</v-icon>
        Registered Clusters
        <v-spacer />
        <v-chip v-if="!loading && serversDetail.length" color="primary" variant="tonal" size="small">
          {{ serversDetail.length }} cluster{{ serversDetail.length !== 1 ? 's' : '' }}
        </v-chip>
      </v-card-title>
      <v-divider />
      <v-card-text class="pa-4">
        <v-progress-linear v-if="loading" indeterminate color="primary" class="mb-4" />
        <v-row v-else-if="serversDetail.length" dense>
          <v-col
            v-for="cluster in serversDetail"
            :key="cluster.host"
            cols="12"
            sm="6"
            md="4"
            lg="3"
          >
            <v-card
              :to="{ path: '/analytics', query: { cluster: cluster.host } }"
              class="cluster-card fill-height"
              variant="tonal"
              elevation="0"
            >
              <v-card-text class="pa-4">
                <div class="d-flex align-start">
                  <v-avatar color="primary" variant="tonal" size="44" class="mr-3 flex-shrink-0">
                    <v-icon>mdi-server</v-icon>
                  </v-avatar>
                  <div class="flex-grow-1 min-width-0">
                    <div class="text-subtitle-1 font-weight-bold text-truncate" :title="cluster.host">
                      {{ cluster.host }}
                    </div>
                    <div class="text-caption text-medium-emphasis mt-1">
                      {{ cluster.http_method }}://{{ cluster.host }}:{{ cluster.port }}
                    </div>
                    <div class="d-flex align-center mt-2 flex-wrap gap-1">
                      <v-chip size="x-small" variant="flat" color="success">Active</v-chip>
                      <v-chip
                        v-if="cluster.manager_emails && cluster.manager_emails.length"
                        size="x-small"
                        variant="tonal"
                        color="warning"
                        :title="cluster.manager_emails.join(', ')"
                      >
                        <v-icon start size="10">mdi-account-cog</v-icon>
                        {{ cluster.manager_emails.length }} mgr{{ cluster.manager_emails.length !== 1 ? 's' : '' }}
                      </v-chip>
                      <v-spacer />
                      <v-icon size="18" class="text-medium-emphasis">mdi-chevron-right</v-icon>
                    </div>
                  </div>
                </div>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
        <v-alert v-else type="info" variant="tonal" class="ma-0">
          <template #prepend>
            <v-icon>mdi-information-outline</v-icon>
          </template>
          No clusters registered yet. Managers can
          <router-link to="/clusters/register">register a cluster</router-link>
          to get started.
        </v-alert>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiGet } from '../api/client'

const loading = ref(true)
const serversDetail = ref([])
const isSuperAdmin = ref(false)

onMounted(async () => {
  try {
    const [srv, me] = await Promise.all([
      apiGet('/api/servers'),
      apiGet('/api/me').catch(() => ({ data: {} })),
    ])
    const detail = srv.data?.servers_detail
    serversDetail.value = Array.isArray(detail) && detail.length
      ? detail
      : (srv.data?.servers || []).map((host) => ({ host, http_method: 'http', port: 8888, manager_emails: [] }))
    isSuperAdmin.value = me.data?.is_super_admin ?? false
  } catch {
    serversDetail.value = []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.cursor-pointer { cursor: pointer; }

.cluster-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.cluster-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.cluster-section {
  border-radius: 12px;
}
</style>
