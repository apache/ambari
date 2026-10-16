import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

import LoginPage from '../components/LoginPage.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import Dashboard from '../views/Dashboard.vue'
import MyUsers from '../views/MyUsers.vue'
import CreateAmbariUserPage from '../views/CreateAmbariUserPage.vue'
import ClustersView from '../views/ClustersView.vue'
import ClusterRegister from '../views/ClusterRegister.vue'
import ClusterManager from '../views/ClusterManager.vue'
import AnalyticsView from '../views/AnalyticsView.vue'
import AuditLogsView from '../views/AuditLogsView.vue'
import ProfileView from '../views/ProfileView.vue'
import AdminPanel from '../views/AdminPanel.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
    meta: { requiresAuth: false, redirectIfAuthenticated: true }
  },
  {
    path: '/',
    component: DefaultLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard
      },
      {
        path: 'main_page',
        redirect: '/dashboard'
      },
      {
        path: 'myusers',
        name: 'MyUsers',
        component: MyUsers
      },
      {
        path: 'create_ambari_users',
        name: 'CreateAmbariUser',
        component: CreateAmbariUserPage
      },
      {
        path: 'clusters',
        name: 'Clusters',
        component: ClustersView
      },
      {
        path: 'clusters/register',
        name: 'ClusterRegister',
        component: ClusterRegister
      },
      {
        path: 'clusters/manager',
        name: 'ClusterManager',
        component: ClusterManager
      },
      {
        path: 'analytics',
        name: 'Analytics',
        component: AnalyticsView
      },
      {
        path: 'audit-logs',
        name: 'AuditLogs',
        component: AuditLogsView
      },
      {
        path: 'profile',
        name: 'Profile',
        component: ProfileView
      },
      {
        path: 'admin',
        name: 'AdminPanel',
        component: AdminPanel
      },
      {
        path: 'settings',
        redirect: '/profile'
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  const loggedIn = authStore.isLoggedIn

  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!loggedIn) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  } else if (to.matched.some(record => record.meta.redirectIfAuthenticated) && loggedIn) {
    next({ path: '/dashboard' })
  } else {
    next()
  }
})

export default router
