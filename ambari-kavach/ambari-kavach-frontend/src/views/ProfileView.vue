<template>
  <div>
    <v-row class="mb-4">
      <v-col cols="12">
        <h1 class="text-h4 font-weight-bold mb-1">Profile</h1>
        <p class="text-body-2 text-medium-emphasis">Your account information</p>
      </v-col>
    </v-row>

    <v-row>
      <!-- Identity + role card -->
      <v-col cols="12" md="5">
        <v-card>
          <v-card-text class="pa-6">
            <!-- Avatar + name -->
            <div class="d-flex flex-column align-center text-center mb-5">
              <v-avatar size="80" color="primary" class="mb-3">
                <v-img v-if="authStore.userPicture" :src="authStore.userPicture" />
                <span v-else class="text-h4 font-weight-bold">{{ initials }}</span>
              </v-avatar>
              <div class="text-h6 font-weight-bold">{{ authStore.userName || 'User' }}</div>
              <div class="text-body-2 text-medium-emphasis mb-2">{{ authStore.userEmail }}</div>
              <v-chip
                :color="isSuperAdmin ? 'error' : isManager ? 'warning' : 'primary'"
                variant="tonal"
                size="small"
                :prepend-icon="isSuperAdmin ? 'mdi-shield-crown' : isManager ? 'mdi-account-cog' : 'mdi-account'"
              >
                {{ isSuperAdmin ? 'Super Admin' : isManager ? 'Cluster Manager' : 'User' }}
              </v-chip>
            </div>

            <v-divider class="mb-4" />

            <!-- Info list -->
            <v-list density="compact" class="pa-0">
              <v-list-item>
                <template #prepend><v-icon color="primary" size="20">mdi-email-outline</v-icon></template>
                <v-list-item-title class="text-caption font-weight-medium text-medium-emphasis">Email</v-list-item-title>
                <v-list-item-subtitle>{{ authStore.userEmail || '—' }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <template #prepend><v-icon color="primary" size="20">mdi-domain</v-icon></template>
                <v-list-item-title class="text-caption font-weight-medium text-medium-emphasis">Domain</v-list-item-title>
                <v-list-item-subtitle>{{ authStore.userHd || '—' }}</v-list-item-subtitle>
              </v-list-item>
              <v-list-item>
                <template #prepend><v-icon color="primary" size="20">mdi-shield-account</v-icon></template>
                <v-list-item-title class="text-caption font-weight-medium text-medium-emphasis">Access Level</v-list-item-title>
                <v-list-item-subtitle>
                  {{ isSuperAdmin ? 'Full system access' : isManager ? `Manages ${managedClusters.length} cluster(s)` : 'Standard access' }}
                </v-list-item-subtitle>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Role summary card -->
      <v-col cols="12" md="7">
        <v-card>
          <v-card-title class="d-flex align-center gap-2 py-4">
            <v-icon :color="isSuperAdmin ? 'error' : isManager ? 'warning' : 'primary'">
              {{ isSuperAdmin ? 'mdi-shield-crown' : isManager ? 'mdi-account-cog' : 'mdi-account-circle' }}
            </v-icon>
            Role &amp; Capabilities
          </v-card-title>
          <v-divider />
          <v-card-text>

            <!-- Super Admin -->
            <template v-if="isSuperAdmin">
              <v-alert type="error" variant="tonal" class="mb-4" density="compact" prominent>
                <template #title>Super Admin</template>
                You have the highest privilege level — full system control.
              </v-alert>
              <v-list density="compact" lines="one">
                <v-list-item prepend-icon="mdi-server-plus" title="Register & delete clusters" />
                <v-list-item prepend-icon="mdi-account-cog" title="Assign & update cluster managers" />
                <v-list-item prepend-icon="mdi-account-remove" title="Force-delete any user from any cluster" />
                <v-list-item prepend-icon="mdi-refresh" title="Re-register any cluster" />
                <v-list-item prepend-icon="mdi-history" title="Full audit log access" />
              </v-list>
              <v-btn class="mt-4" color="error" variant="tonal" prepend-icon="mdi-shield-crown" to="/admin">
                Open Admin Panel
              </v-btn>
            </template>

            <!-- Cluster Manager -->
            <template v-else-if="isManager">
              <v-alert type="warning" variant="tonal" class="mb-4" density="compact" prominent>
                <template #title>Cluster Manager</template>
                You manage {{ managedClusters.length }} cluster(s).
              </v-alert>
              <div class="d-flex flex-wrap gap-1 mb-4">
                <v-chip v-for="c in managedClusters" :key="c" color="warning" variant="tonal" size="small" prepend-icon="mdi-server">
                  {{ c }}
                </v-chip>
              </div>
              <v-list density="compact" lines="one">
                <v-list-item prepend-icon="mdi-account-remove" title="Delete users from your cluster(s)" />
                <v-list-item prepend-icon="mdi-refresh" title="Re-register your cluster(s)" />
                <v-list-item prepend-icon="mdi-account-plus" title="Create temporary access users" />
              </v-list>
              <v-btn class="mt-4" color="warning" variant="tonal" prepend-icon="mdi-cog" to="/clusters/manager">
                Cluster Management
              </v-btn>
            </template>

            <!-- Regular User -->
            <template v-else>
              <v-alert type="info" variant="tonal" class="mb-4" density="compact" prominent>
                <template #title>User</template>
                Standard access — create temporary credentials for Ambari clusters.
              </v-alert>
              <v-list density="compact" lines="one">
                <v-list-item prepend-icon="mdi-account-plus" title="Create temporary Ambari users" />
                <v-list-item prepend-icon="mdi-account-group" title="View your active & expired users" />
                <v-list-item prepend-icon="mdi-chart-box" title="View cluster analytics" />
                <v-list-item prepend-icon="mdi-history" title="View audit logs" />
              </v-list>
              <v-btn class="mt-4" color="primary" variant="tonal" prepend-icon="mdi-account-plus" to="/create_ambari_users">
                Create User
              </v-btn>
            </template>
          </v-card-text>
        </v-card>

        <!-- About card -->
        <v-card class="mt-4" variant="outlined">
          <v-card-text>
            <p class="text-body-2 text-medium-emphasis mb-0">
              <strong>Ambari Kavach</strong> is the security access layer for Apache Ambari clusters.
              All credentials are temporary, time-limited, and fully audited. Use the minimum role necessary for your task.
            </p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
/* eslint-disable vue/multi-word-component-names */
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import { apiGet } from '../api/client'

const authStore = useAuthStore()
const isSuperAdmin = ref(false)
const managedClusters = ref([])
const isManager = computed(() => isSuperAdmin.value || managedClusters.value.length > 0)

onMounted(async () => {
  try {
    const r = await apiGet('/api/me')
    isSuperAdmin.value = r.data?.is_super_admin ?? false
    managedClusters.value = r.data?.managed_clusters ?? []
  } catch {
    isSuperAdmin.value = false
    managedClusters.value = []
  }
})

const initials = computed(() => {
  const name = authStore.userName || ''
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || 'U'
})
</script>
