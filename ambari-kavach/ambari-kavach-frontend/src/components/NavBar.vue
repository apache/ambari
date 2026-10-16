<template>
  <nav class="kavach-navbar">
    <div class="navbar-container">
      <!-- Enhanced Brand Section -->
      <router-link to="/main_page" class="navbar-brand">
        <div class="brand-logo-container">
          <img src="/ambari-kavach-logo.png" alt="Ambari Kavach Logo" class="brand-logo"/>
          <div class="logo-glow"></div>
        </div>
        <div class="brand-text">
          <span class="brand-name">Ambari Kavach</span>
          <span class="brand-tagline">Security Dashboard</span>
        </div>
      </router-link>

      <!-- Navigation Links -->
      <div class="navbar-center">
        <ul class="navbar-links">
          <li class="nav-item">
            <router-link to="/myusers" class="nav-link">
              <i class="nav-icon">👥</i>
              <span>My Users</span>
            </router-link>
          </li>
          <li class="nav-item">
            <router-link to="/create_ambari_users" class="nav-link">
              <i class="nav-icon">➕</i>
              <span>New Ambari User</span>
            </router-link>
          </li>
        </ul>
      </div>

      <!-- Enhanced User Profile Section -->
      <div class="navbar-user-section">
        <div class="user-greeting-card" v-if="authStore.userName">
          <span class="greeting-text">Welcome back,</span>
          <span class="user-name">{{ authStore.userName.split(' ')[0] }}!</span>
        </div>
        
        <div class="profile-container">
          <div class="profile-avatar" @click="toggleDropdown">
            <img :src="profilePictureUrl" alt="Profile" class="profile-image"/>
            <div class="online-indicator"></div>
          </div>
          
          <!-- Enhanced Dropdown -->
          <div class="profile-dropdown" :class="{ 'dropdown-visible': dropdownOpen }">
            <div class="dropdown-header">
              <img :src="profilePictureUrl" alt="Profile" class="dropdown-avatar"/>
              <div class="user-details">
                <strong class="user-full-name">{{ authStore.userName || 'User' }}</strong>
                <small class="user-email">{{ authStore.userEmail || 'email@example.com' }}</small>
              </div>
            </div>
            
            <div class="dropdown-divider"></div>
            
            <div class="dropdown-menu">
              <router-link to="/profile" class="dropdown-item" @click="closeDropdown">
                <i class="item-icon">👤</i>
                <span>My Profile</span>
                <i class="item-arrow">→</i>
              </router-link>
              
              <router-link to="/settings" class="dropdown-item" @click="closeDropdown">
                <i class="item-icon">⚙️</i>
                <span>Settings</span>
                <i class="item-arrow">→</i>
              </router-link>
              
              <div class="dropdown-divider"></div>
              
              <button @click="logout" class="dropdown-item logout-item">
                <i class="item-icon">🚪</i>
                <span>Sign Out</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Click outside overlay to close dropdown -->
    <div v-if="dropdownOpen" class="dropdown-overlay" @click="closeDropdown"></div>
  </nav>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useAuthStore } from '../stores/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();
const dropdownOpen = ref(false);

const profilePictureUrl = computed(() => {
  return authStore.userPicture || 'https://via.placeholder.com/40/667eea/FFFFFF?text=' + (authStore.userName?.charAt(0) || 'U');
});

const toggleDropdown = () => {
  dropdownOpen.value = !dropdownOpen.value;
};

const closeDropdown = () => {
  dropdownOpen.value = false;
};

const logout = () => {
  authStore.logout();
  router.push('/login');
  closeDropdown();
};
</script>

<style scoped>
/* Enhanced NavBar Styles */
.kavach-navbar {
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 1rem 0;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.navbar-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 2rem;
}

/* Enhanced Brand Section */
.navbar-brand {
  display: flex;
  align-items: center;
  gap: 1rem;
  text-decoration: none;
  color: white;
  transition: transform 0.3s ease;
}

.navbar-brand:hover {
  transform: scale(1.02);
}

.brand-logo-container {
  position: relative;
  display: flex;
  align-items: center;
}

.brand-logo {
  height: 48px;
  width: 48px;
  border-radius: 12px;
  object-fit: cover;
  position: relative;
  z-index: 2;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
  transition: transform 0.3s ease;
}

.navbar-brand:hover .brand-logo {
  transform: rotate(5deg) scale(1.1);
}

.logo-glow {
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 14px;
  opacity: 0;
  animation: pulse 3s infinite;
  z-index: 1;
}

@keyframes pulse {
  0%, 100% { opacity: 0; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.05); }
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 1.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, #ffffff 0%, #e8f4fd 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.brand-tagline {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
  letter-spacing: 0.5px;
}

/* Navigation Links */
.navbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.navbar-links {
  list-style: none;
  display: flex;
  gap: 2rem;
  margin: 0;
  padding: 0;
}

.nav-item {
  position: relative;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  font-size: 0.95rem;
  padding: 0.75rem 1.5rem;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.nav-link::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.6s ease;
}

.nav-link:hover::before {
  left: 100%;
}

.nav-link:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.2);
  color: white;
}

.nav-link.router-link-active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.nav-icon {
  font-size: 1.1rem;
}

/* Enhanced User Section */
.navbar-user-section {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.user-greeting-card {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  text-align: right;
}

.greeting-text {
  font-size: 0.8rem;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.user-name {
  font-size: 1rem;
  color: white;
  font-weight: 700;
}

/* Profile Container */
.profile-container {
  position: relative;
}

.profile-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  cursor: pointer;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  padding: 2px;
  transition: all 0.3s ease;
}

.profile-avatar:hover {
  transform: scale(1.1);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

.profile-image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid white;
}

.online-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: #22c55e;
  border: 2px solid white;
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.4);
}

/* Enhanced Dropdown */
.profile-dropdown {
  position: absolute;
  top: calc(100% + 1rem);
  right: 0;
  min-width: 320px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px) scale(0.95);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1000;
  overflow: hidden;
}

.profile-dropdown.dropdown-visible {
  opacity: 1;
  visibility: visible;
  transform: translateY(0) scale(1);
}

.dropdown-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: transparent;
  z-index: 999;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.dropdown-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid white;
}

.user-details {
  flex: 1;
}

.user-full-name {
  display: block;
  font-size: 1.1rem;
  font-weight: 700;
  margin-bottom: 0.25rem;
}

.user-email {
  font-size: 0.85rem;
  opacity: 0.9;
}

.dropdown-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 0, 0, 0.1), transparent);
  margin: 0.5rem 0;
}

.dropdown-menu {
  padding: 1rem;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  text-decoration: none;
  color: #374151;
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
  cursor: pointer;
  border: none;
  background: none;
  width: 100%;
  text-align: left;
  margin-bottom: 0.5rem;
}

.dropdown-item:hover {
  background: linear-gradient(135deg, #f3f4f6, #e5e7eb);
  transform: translateX(4px);
  color: #1f2937;
}

.dropdown-item.logout-item {
  color: #dc2626;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  margin-top: 0.5rem;
  padding-top: 1rem;
}

.dropdown-item.logout-item:hover {
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
  color: #b91c1c;
}

.item-icon {
  font-size: 1.2rem;
  width: 20px;
  text-align: center;
}

.item-arrow {
  margin-left: auto;
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s ease;
}

.dropdown-item:hover .item-arrow {
  opacity: 1;
  transform: translateX(0);
}

/* Responsive Design */
@media (max-width: 768px) {
  .navbar-container {
    padding: 0 1rem;
  }
  
  .navbar-links {
    gap: 1rem;
  }
  
  .nav-link {
    padding: 0.5rem 1rem;
    font-size: 0.9rem;
  }
  
  .nav-link span {
    display: none;
  }
  
  .user-greeting-card {
    display: none;
  }
  
  .profile-dropdown {
    min-width: 280px;
    right: -1rem;
  }
}

@media (max-width: 480px) {
  .brand-text {
    display: none;
  }
  
  .navbar-center {
    display: none;
  }
  
  .profile-dropdown {
    min-width: 260px;
    right: -2rem;
  }
}
</style>