<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-gradient" />
      <div class="bg-pattern" />
      <div class="floating-orb orb-1" />
      <div class="floating-orb orb-2" />
      <div class="floating-orb orb-3" />
    </div>

    <div class="login-wrapper">
      <div class="login-hero">
        <div class="hero-brand">
          <div class="hero-logo">
            <img src="/ambari-kavach-logo.png" alt="Ambari Kavach Logo" style="width:64px;height:64px;object-fit:contain;"/>
          </div>
          <h1 class="hero-title">Ambari Kavach</h1>
          <p class="hero-tagline">Secure Access Management for Apache Ambari</p>
        </div>
        <div class="hero-features">
          <div class="feature">
            <v-icon color="white" size="24">mdi-shield-check</v-icon>
            <span>Enterprise-grade security</span>
          </div>
          <div class="feature">
            <v-icon color="white" size="24">mdi-google</v-icon>
            <span>Google SSO authentication</span>
          </div>
          <div class="feature">
            <v-icon color="white" size="24">mdi-clock-outline</v-icon>
            <span>Time-limited cluster access</span>
          </div>
        </div>
      </div>

      <div class="login-card">
        <div class="card-brand">
          <div class="card-logo">
            <img src="/ambari-kavach-logo.png" alt="Ambari Kavach" style="width:44px;height:44px;object-fit:contain;" />
          </div>
          <h2 class="card-title">Welcome back</h2>
          <p class="card-subtitle">Sign in with your Google account to continue</p>
        </div>

        <div class="card-body">
          <v-alert v-if="successMessage" type="success" variant="tonal" class="mb-4">
            <v-alert-title>Login successful</v-alert-title>
            {{ successMessage }}
          </v-alert>

          <v-alert v-else-if="errorMessage && !isLoading" type="error" variant="tonal" class="mb-4">
            <v-alert-title>Authentication failed</v-alert-title>
            {{ errorMessage }}
          </v-alert>

          <div v-if="isLoading" class="loading-box">
            <v-progress-circular indeterminate color="primary" size="48" width="4" />
            <p class="mt-3">Authenticating...</p>
          </div>

          <template v-else-if="!successMessage">
            <div class="google-wrapper">
              <GoogleLogin :callback="callbackHandler" prompt class="google-btn" />
            </div>
          </template>

          <div v-if="successMessage && !isLoading" class="redirect-box">
            <v-progress-circular indeterminate color="primary" size="32" width="3" />
            <p>Redirecting to dashboard...</p>
          </div>
        </div>

        <div class="card-footer">
          <span>&copy; {{ new Date().getFullYear() }} Ambari Kavach</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { decodeCredential } from 'vue3-google-login'
import { useAuthStore } from '../stores/auth'
import { useToast } from 'vue-toast-notification'
import 'vue-toast-notification/dist/theme-sugar.css'
import { useRouter } from 'vue-router'

const $toast = useToast()
const authStore = useAuthStore()
const router = useRouter()
const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

onMounted(() => {
  errorMessage.value = ''
  successMessage.value = ''
  if (authStore.message) authStore.message = ''
})

const callbackHandler = async (response) => {
  if (!response.credential) {
    errorMessage.value = 'Google login did not return a credential. Please try again.'
    $toast.error(errorMessage.value, { position: 'top-right', duration: 4000 })
    return
  }

  errorMessage.value = ''
  successMessage.value = ''
  isLoading.value = true

  try {
    const userData = decodeCredential(response.credential)
    await authStore.handleGoogleLogin(userData)
    successMessage.value = `Welcome ${userData.name}! You have been successfully authenticated.`
    $toast.success('Login successful! Redirecting...', { position: 'top-right', duration: 2000 })
    authStore.message = ''
    setTimeout(() => router.push('/dashboard'), 2000)
  } catch (err) {
    errorMessage.value = err.response?.data?.message || err.message || 'Login failed. Please try again.'
    $toast.error(errorMessage.value, { position: 'top-right', duration: 4000 })
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.login-bg {
  position: fixed;
  inset: 0;
  overflow: hidden;
}

.bg-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1565c0 0%, #0d47a1 50%, #1565c0 100%);
}

.bg-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 20% 80%, rgba(255,255,255,0.08) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.06) 0%, transparent 50%);
}

.floating-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  animation: float 25s ease-in-out infinite;
}

.orb-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -50px;
  animation-delay: 0s;
}

.orb-2 {
  width: 200px;
  height: 200px;
  bottom: -50px;
  left: -50px;
  animation-delay: -8s;
}

.orb-3 {
  width: 150px;
  height: 150px;
  top: 50%;
  left: 10%;
  animation-delay: -16s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.06; }
  33% { transform: translate(30px, -20px) scale(1.05); opacity: 0.1; }
  66% { transform: translate(-20px, 20px) scale(0.95); opacity: 0.08; }
}

.login-wrapper {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4rem;
  padding: 2rem;
  width: 100%;
  max-width: 1100px;
}

.login-hero {
  flex: 1;
  max-width: 420px;
  color: white;
}

.hero-brand {
  margin-bottom: 3rem;
}

.hero-logo {
  width: 100px;
  height: 100px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 800;
  margin: 0 0 0.5rem;
  letter-spacing: -0.03em;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2);
}

.hero-tagline {
  font-size: 1.1rem;
  opacity: 0.9;
  margin: 0;
  font-weight: 500;
}

.hero-features {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.feature {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 0.95rem;
  font-weight: 500;
}

.login-card {
  flex-shrink: 0;
  width: 100%;
  max-width: 420px;
  background: white;
  border-radius: 24px;
  padding: 2.5rem;
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.card-brand {
  text-align: center;
  margin-bottom: 2rem;
}

.card-logo {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: rgba(25, 118, 210, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.25rem;
}

.card-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 0.5rem;
}

.card-subtitle {
  font-size: 0.95rem;
  color: #6b7280;
  margin: 0;
}

.card-body {
  min-height: 120px;
}

.google-wrapper {
  display: flex;
  justify-content: center;
}

.google-btn {
  width: 100%;
}

.loading-box,
.redirect-box {
  text-align: center;
  padding: 2rem;
}

.loading-box p,
.redirect-box p {
  margin: 0;
  color: #6b7280;
  font-size: 0.95rem;
}

.card-footer {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e5e7eb;
  text-align: center;
}

.card-footer span {
  font-size: 0.8rem;
  color: #9ca3af;
}

@media (max-width: 900px) {
  .login-wrapper {
    flex-direction: column;
    gap: 2.5rem;
  }

  .login-hero {
    text-align: center;
    max-width: 100%;
  }

  .hero-logo {
    margin-left: auto;
    margin-right: auto;
  }

  .hero-title {
    font-size: 2rem;
  }
}

@media (max-width: 480px) {
  .login-wrapper {
    padding: 1rem;
  }

  .login-card {
    padding: 1.5rem;
  }

  .hero-title {
    font-size: 1.75rem;
  }
}
</style>
