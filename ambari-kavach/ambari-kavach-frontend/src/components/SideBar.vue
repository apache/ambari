<template>
  <aside class="kavach-sidebar" :class="{ 'sidebar-collapsed': collapsed }">
    <!-- Sidebar Header -->
    <div class="sidebar-header">
      <div class="sidebar-brand">
        <div class="brand-logo-wrapper">
          <img src="/ambari-kavach-logo.png" alt="Kavach" class="sidebar-logo-img"/>
        </div>
        <div v-if="!collapsed" class="brand-info">
          <span class="sidebar-logo-text">Kavach</span>
          <span class="sidebar-tagline">Security Portal</span>
        </div>
      </div>
      <button @click="toggleSidebar" class="sidebar-toggle">
        <i class="toggle-icon">{{ collapsed ? '→' : '←' }}</i>
      </button>
    </div>

    <!-- Navigation Menu -->
    <nav class="sidebar-nav">
      <div class="nav-section">
        <h3 v-if="!collapsed" class="section-title">
          <span class="title-icon">🏠</span>
          Main
        </h3>

        <ul class="nav-list">
          <li class="nav-item">
            <router-link to="/dashboard" class="nav-link">
              <div class="nav-icon-container">
                <i class="nav-icon">🏠</i>
              </div>
              <div v-if="!collapsed" class="nav-content">
                <span class="nav-text">Dashboard</span>
                <span class="nav-description">Overview & Stats</span>
              </div>
            </router-link>
          </li>

          <li class="nav-item">
            <router-link to="/myusers" class="nav-link">
              <div class="nav-icon-container">
                <i class="nav-icon">👥</i>
              </div>
              <div v-if="!collapsed" class="nav-content">
                <span class="nav-text">My Users</span>
                <span class="nav-description">Active & Expired</span>
              </div>
              <span v-if="!collapsed && activeUserCount > 0" class="nav-badge">{{ activeUserCount }}</span>
            </router-link>
          </li>

          <li class="nav-item">
            <router-link to="/create_ambari_users" class="nav-link">
              <div class="nav-icon-container">
                <i class="nav-icon">➕</i>
              </div>
              <div v-if="!collapsed" class="nav-content">
                <span class="nav-text">Create User</span>
                <span class="nav-description">New Ambari Account</span>
              </div>
            </router-link>
          </li>
        </ul>
      </div>

      <div class="nav-section">
        <h3 v-if="!collapsed" class="section-title">
          <span class="title-icon">🔧</span>
          Clusters
        </h3>

        <ul class="nav-list">
          <li class="nav-item">
            <router-link to="/clusters" class="nav-link">
              <div class="nav-icon-container">
                <i class="nav-icon">🖥️</i>
              </div>
              <div v-if="!collapsed" class="nav-content">
                <span class="nav-text">Clusters</span>
                <span class="nav-description">Registered Servers</span>
              </div>
            </router-link>
          </li>

          <li class="nav-item">
            <router-link to="/analytics" class="nav-link">
              <div class="nav-icon-container">
                <i class="nav-icon">📈</i>
              </div>
              <div v-if="!collapsed" class="nav-content">
                <span class="nav-text">Analytics</span>
                <span class="nav-description">Health & Resources</span>
              </div>
            </router-link>
          </li>

          <li v-if="isManager" class="nav-item">
            <router-link to="/clusters/register" class="nav-link">
              <div class="nav-icon-container">
                <i class="nav-icon">🔌</i>
              </div>
              <div v-if="!collapsed" class="nav-content">
                <span class="nav-text">Register Cluster</span>
                <span class="nav-description">Add New Server</span>
              </div>
            </router-link>
          </li>

          <li v-if="isManager" class="nav-item">
            <router-link to="/clusters/manager" class="nav-link">
              <div class="nav-icon-container">
                <i class="nav-icon">⚙️</i>
              </div>
              <div v-if="!collapsed" class="nav-content">
                <span class="nav-text">Management</span>
                <span class="nav-description">Delete Users, Re-register</span>
              </div>
            </router-link>
          </li>

          <li class="nav-item">
            <router-link to="/audit-logs" class="nav-link">
              <div class="nav-icon-container">
                <i class="nav-icon">📊</i>
              </div>
              <div v-if="!collapsed" class="nav-content">
                <span class="nav-text">Audit Logs</span>
                <span class="nav-description">Activity Tracking</span>
              </div>
            </router-link>
          </li>
        </ul>
      </div>

      <div class="nav-section">
        <h3 v-if="!collapsed" class="section-title">
          <span class="title-icon">👤</span>
          Account
        </h3>

        <ul class="nav-list">
          <li class="nav-item">
            <router-link to="/profile" class="nav-link">
              <div class="nav-icon-container">
                <i class="nav-icon">👤</i>
              </div>
              <div v-if="!collapsed" class="nav-content">
                <span class="nav-text">Profile</span>
                <span class="nav-description">Account Info</span>
              </div>
            </router-link>
          </li>
        </ul>
      </div>
    </nav>

    <!-- Sidebar Footer -->
    <div class="sidebar-footer">
      <div v-if="!collapsed" class="user-card">
        <div class="user-avatar-wrapper">
          <img v-if="userAvatar" :src="userAvatar" alt="User" class="user-avatar"/>
          <div v-else class="user-avatar user-avatar-initials">{{ initials }}</div>
          <div class="avatar-status"></div>
        </div>
        <div class="user-info">
          <span class="user-name">{{ userName }}</span>
          <span class="user-role">{{ userRole }}</span>
        </div>
      </div>
      
      <button @click="logout" class="logout-btn" :title="collapsed ? 'Logout' : ''">
        <div class="logout-icon-wrapper">
          <i class="logout-icon">🚪</i>
        </div>
        <span v-if="!collapsed" class="logout-text">Sign Out</span>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';
import { apiGet } from '../api/client';

const authStore = useAuthStore();
const router = useRouter();
const collapsed = ref(false);
const activeUserCount = ref(0);
const isManager = ref(false);

const userName = computed(() => {
  return authStore.userName?.split(' ')[0] || 'User';
});

const userRole = computed(() => isManager.value ? 'Manager' : 'User');

const userAvatar = computed(() => {
  return authStore.userPicture || '';
});

const initials = computed(() => {
  const name = authStore.userName || '';
  return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2) || 'U';
});

onMounted(async () => {
  try {
    const [meRes, usersRes] = await Promise.allSettled([
      apiGet('/api/me'),
      apiGet('/api/active_users'),
    ]);
    if (meRes.status === 'fulfilled') {
      isManager.value = meRes.value.data?.is_manager ?? false;
    }
    if (usersRes.status === 'fulfilled') {
      activeUserCount.value = usersRes.value.data?.active_users?.length ?? 0;
    }
  } catch {
    // silently ignore — sidebar data is best-effort
  }
});

const toggleSidebar = () => {
  collapsed.value = !collapsed.value;
};

const logout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<style scoped>
:root {
  --kavach-blue: #2874f0;
  --kavach-accent: #ff9f00;
  --kavach-dark-blue: #1e5bc6;
  --primary-gradient: linear-gradient(180deg, var(--kavach-blue) 0%, var(--kavach-dark-blue) 100%);
  --secondary-gradient: linear-gradient(135deg, var(--kavach-accent) 0%, #e8930a 100%);
  --shadow-soft: 0 4px 20px rgba(40, 116, 240, 0.1);
}

.kavach-sidebar {
  width: 320px;
  min-height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 999;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  background: var(--primary-gradient);
  box-shadow: var(--shadow-soft);
}

.sidebar-collapsed {
  width: 90px;
}

/* Sidebar Header */
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
}

.brand-logo-wrapper {
  position: relative;
}

.sidebar-logo-img {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 15px rgba(255, 159, 0, 0.3);
}

.brand-info {
  display: flex;
  flex-direction: column;
}

.sidebar-logo-text {
  font-size: 1.4rem;
  font-weight: 800;
  color: white;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.sidebar-tagline {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.sidebar-toggle {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  color: white;
  padding: 0.75rem;
  cursor: pointer;
  transition: all 0.3s ease;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-toggle:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

.toggle-icon {
  font-size: 1.2rem;
  font-weight: bold;
}

/* Navigation */
.sidebar-nav {
  flex: 1;
  padding: 1.5rem 1rem;
  overflow-y: auto;
}

.nav-section {
  margin-bottom: 2rem;
}

.section-title {
  font-size: 0.8rem;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.8);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.title-icon {
  font-size: 1rem;
  color: var(--kavach-accent);
}

.nav-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  text-decoration: none;
  color: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  transform: translateX(4px);
}

.nav-link.router-link-active {
  background: var(--secondary-gradient);
  color: white;
  box-shadow: 0 4px 15px rgba(255, 159, 0, 0.4);
}

.nav-icon-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.nav-link:hover .nav-icon-container {
  background: rgba(255, 255, 255, 0.2);
}

.nav-icon {
  font-size: 1.3rem;
  color: white;
}

.nav-content {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.nav-text {
  font-size: 1rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.nav-description {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.nav-badge {
  background: var(--secondary-gradient);
  color: white;
  font-size: 0.75rem;
  font-weight: 800;
  padding: 0.3rem 0.6rem;
  border-radius: 12px;
  min-width: 20px;
  text-align: center;
}

/* Sidebar Footer */
.sidebar-footer {
  padding: 1.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  margin-bottom: 1rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.user-avatar-wrapper {
  position: relative;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.user-avatar-initials {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  color: white;
  font-weight: 700;
  font-size: 0.85rem;
}

.avatar-status {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 12px;
  height: 12px;
  background: #22c55e;
  border: 2px solid white;
  border-radius: 50%;
}

.user-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.user-name {
  font-weight: 700;
  color: white;
  font-size: 1rem;
}

.user-role {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 1rem;
  width: 100%;
  padding: 1rem;
  background: rgba(220, 38, 38, 0.15);
  border: 1px solid rgba(220, 38, 38, 0.3);
  border-radius: 12px;
  color: #ff6b6b;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
}

.logout-btn:hover {
  background: rgba(220, 38, 38, 0.25);
  transform: translateY(-2px);
}

.logout-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(220, 38, 38, 0.2);
}

.logout-icon {
  font-size: 1.1rem;
  color: #ff6b6b;
}

.logout-text {
  font-size: 1rem;
}

/* Responsive Design */
@media (max-width: 768px) {
  .kavach-sidebar {
    transform: translateX(-100%);
    width: 280px;
  }
  
  .kavach-sidebar.sidebar-open {
    transform: translateX(0);
  }
  
  .sidebar-collapsed {
    width: 280px;
  }
}
</style>