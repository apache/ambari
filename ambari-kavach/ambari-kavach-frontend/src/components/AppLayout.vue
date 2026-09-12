<template>
  <div class="app-layout">
    <v-navigation-drawer
      v-model="drawer"
      :rail="rail"
      permanent
      class="kavach-drawer"
      @click="rail = false"
    >
      <v-list density="compact" nav>
        <v-list-item class="sidebar-brand-item">
          <template #prepend>
            <div class="sidebar-logo-box">
              <img src="/ambari-kavach-logo.png" alt="Ambari Kavach" style="width:26px;height:26px;object-fit:contain;" />
            </div>
          </template>
          <v-list-item-title v-if="!rail" class="font-weight-bold text-h6 sidebar-brand-title">Ambari Kavach</v-list-item-title>
          <template #append>
            <v-btn icon size="small" variant="text" @click.stop="rail = !rail">
              <v-icon>{{ rail ? 'mdi-chevron-right' : 'mdi-chevron-left' }}</v-icon>
            </v-btn>
          </template>
        </v-list-item>
        <v-divider />

        <v-list-subheader v-if="!rail">Main</v-list-subheader>
        <v-list-item to="/dashboard" prepend-icon="mdi-view-dashboard" title="Dashboard" rounded="lg" />
        <v-list-item to="/myusers" prepend-icon="mdi-account-group" title="My Users" rounded="lg" />
        <v-list-item to="/create_ambari_users" prepend-icon="mdi-account-plus" title="Create User" rounded="lg" />
        <v-list-item to="/clusters" prepend-icon="mdi-server" title="Clusters" rounded="lg" />

        <v-divider class="my-2" />

        <v-list-subheader v-if="!rail">Management</v-list-subheader>
        <v-list-item to="/clusters/manager" prepend-icon="mdi-cog" title="Cluster Manager" rounded="lg" />
        <v-list-item to="/analytics" prepend-icon="mdi-chart-box" title="Analytics" rounded="lg" />
        <v-list-item to="/audit-logs" prepend-icon="mdi-history" title="Audit Logs" rounded="lg" />
        <v-list-item v-if="isSuperAdmin" to="/clusters/register" prepend-icon="mdi-plus-network" title="Register Cluster" rounded="lg" />

        <template v-if="isSuperAdmin">
          <v-divider class="my-2" />
          <v-list-subheader v-if="!rail" class="text-error font-weight-bold">Super Admin</v-list-subheader>
          <v-list-item to="/admin" prepend-icon="mdi-shield-crown" title="Admin Panel" rounded="lg" color="error" />
        </template>

        <v-divider class="my-2" />

        <v-list-subheader v-if="!rail">Account</v-list-subheader>
        <v-list-item to="/profile" prepend-icon="mdi-account" title="Profile" rounded="lg" />
      </v-list>

      <template #append>
        <div class="sidebar-footer">
          <div class="user-card" :class="{ 'rail-mode': rail }">
            <v-avatar size="40" color="primary" class="user-avatar">
              <v-img v-if="authStore.userPicture" :src="authStore.userPicture" cover />
              <span v-else class="text-body-2 font-weight-medium">{{ initials }}</span>
            </v-avatar>
            <div v-if="!rail" class="user-info">
              <span class="user-name">{{ authStore.userName || 'User' }}</span>
              <span class="user-email">{{ authStore.userEmail }}</span>
              <v-chip
                v-if="!rail"
                :color="isSuperAdmin ? 'error' : isManager ? 'warning' : 'primary'"
                size="x-small"
                variant="tonal"
                class="mt-1"
                style="width: fit-content"
              >
                {{ isSuperAdmin ? 'Super Admin' : isManager ? 'Cluster Manager' : 'User' }}
              </v-chip>
            </div>
          </div>
          <v-btn
            block
            variant="tonal"
            color="error"
            prepend-icon="mdi-logout"
            class="logout-btn"
            rounded="lg"
            @click="logout"
          >
            <span v-if="!rail">Sign Out</span>
          </v-btn>
        </div>
      </template>
    </v-navigation-drawer>

    <div class="main-area">
    <v-app-bar color="primary" density="compact" elevation="2">
      <v-app-bar-nav-icon @click="drawer = !drawer" />
      <div class="app-bar-brand">
        <div class="brand-logo-wrapper">
          <img src="/ambari-kavach-logo.png" alt="Ambari Kavach" style="width:30px;height:30px;object-fit:contain;" />
        </div>
        <div class="brand-text">
          <span class="brand-name">Ambari Kavach</span>
          <span class="brand-context">{{ pageTitle }}</span>
        </div>
      </div>
      <v-spacer />
      <v-chip
        v-if="isSuperAdmin || isManager"
        size="small"
        :color="isSuperAdmin ? 'error' : 'warning'"
        variant="flat"
        class="mr-2"
        :prepend-icon="isSuperAdmin ? 'mdi-shield-crown' : 'mdi-account-cog'"
      >
        {{ isSuperAdmin ? 'Super Admin' : 'Cluster Manager' }}
      </v-chip>
      <v-chip size="small" color="white" variant="flat" class="mr-2">
        {{ authStore.userEmail }}
      </v-chip>
      <v-menu>
        <template #activator="{ props }">
          <v-btn icon v-bind="props">
            <v-avatar size="32">
              <v-img v-if="authStore.userPicture" :src="authStore.userPicture" />
              <span v-else class="text-caption">{{ initials }}</span>
            </v-avatar>
          </v-btn>
        </template>
        <v-list>
          <v-list-item :title="authStore.userName" :subtitle="authStore.userEmail" />
          <v-divider />
          <v-list-item prepend-icon="mdi-account" title="Profile" to="/profile" />
          <v-list-item prepend-icon="mdi-logout" title="Sign Out" @click="logout" />
        </v-list>
      </v-menu>
    </v-app-bar>

      <v-main class="main-with-marquee">
      <v-container fluid class="pa-4">
        <slot />
      </v-container>
      </v-main>

      <!-- Audit notice marquee -->
    <div class="marquee-bar">
      <span class="marquee-text">
        All Ambari users created are highly audited. Create users only when necessary.
        Use read-only credentials for non-admin operations when possible.
      </span>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { apiGet } from '../api/client'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const drawer = ref(true)
const rail = ref(false)
const isSuperAdmin = ref(false)
const managedClusters = ref([])
const isManager = computed(() => isSuperAdmin.value || managedClusters.value.length > 0)

const pageTitle = computed(() => {
  const name = route.name || ''
  const titles = {
    Dashboard: 'Dashboard',
    MyUsers: 'My Users',
    CreateAmbariUser: 'Create Ambari User',
    Clusters: 'Clusters',
    ClusterRegister: 'Register Cluster',
    ClusterManager: 'Cluster Management',
    Analytics: 'Cluster Analytics',
    AuditLogs: 'Audit Logs',
    Profile: 'Profile',
    AdminPanel: 'Admin Panel',
  }
  return titles[name] || 'Ambari Kavach'
})

const initials = computed(() => {
  const name = authStore.userName || ''
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || 'U'
})

async function checkManager() {
  try {
    const r = await apiGet('/api/me')
    isSuperAdmin.value = r.data?.is_super_admin ?? false
    managedClusters.value = r.data?.managed_clusters ?? []
  } catch {
    isSuperAdmin.value = false
    managedClusters.value = []
  }
}

function logout() {
  authStore.logout()
  router.push('/login')
}

onMounted(checkManager)
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  flex-direction: row;
}

.app-layout :deep(.v-navigation-drawer) {
  flex-shrink: 0;
}

.app-layout :deep(.v-main) {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.app-layout :deep(.v-main .v-container) {
  flex: 1;
}

.kavach-drawer {
  border-right: 1px solid rgba(0, 0, 0, 0.12);
}

.sidebar-brand-item {
  margin-bottom: 8px;
}

.sidebar-logo-box {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(var(--v-theme-primary), 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sidebar-brand-title {
  color: rgb(var(--v-theme-primary));
  font-size: 1.1rem !important;
}

.sidebar-footer {
  padding: 12px 16px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0 12px;
}

.user-card.rail-mode {
  justify-content: center;
  padding: 8px 0;
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-email {
  font-size: 0.75rem;
  color: rgba(0, 0, 0, 0.6);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  text-transform: none;
  letter-spacing: normal;
}

.kavach-drawer.v-navigation-drawer--rail .logout-btn {
  min-width: 48px;
}

.main-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.main-area :deep(.v-app-bar) {
  flex-shrink: 0;
}

.app-bar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 4px;
}

.brand-logo-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.brand-name {
  font-size: 1.1rem;
  font-weight: 700;
  color: white;
  letter-spacing: -0.02em;
}

.brand-context {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 500;
}

.main-with-marquee {
  padding-bottom: 48px !important;
  flex: 1;
  overflow-y: auto;
}

.marquee-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.08);
  padding: 8px 0;
  overflow: hidden;
  z-index: 100;
}

.marquee-text {
  display: inline-block;
  color: #546e7a;
  font-size: 13px;
  padding-left: 100%;
  white-space: nowrap;
  animation: marquee 40s linear infinite;
}

@keyframes marquee {
  0% { transform: translateX(100%); }
  100% { transform: translateX(-100%); }
}
</style>
