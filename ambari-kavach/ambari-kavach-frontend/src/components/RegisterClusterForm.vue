<template>
  <div>
    <!-- Page header -->
    <v-row class="mb-5">
      <v-col cols="12">
        <div class="d-flex align-center gap-3">
          <div class="page-icon-wrap">
            <v-icon size="26" color="primary">mdi-plus-network</v-icon>
          </div>
          <div>
            <h1 class="text-h4 font-weight-bold mb-0">Register Cluster</h1>
            <p class="text-body-2 text-medium-emphasis mb-0">Add an Ambari cluster to Kavach Security Vault</p>
          </div>
        </div>
      </v-col>
    </v-row>

    <v-row>
      <!-- ── Main form ─────────────────────────────────────────────── -->
      <v-col cols="12" md="8">
        <v-card elevation="0" border>

          <!-- ① Connection Details -->
          <div class="form-section-label">
            <div class="label-dot" style="background: rgb(var(--v-theme-primary));" />
            <v-icon size="15" color="primary" class="mr-1">mdi-lan-connect</v-icon>
            <span>Connection Details</span>
          </div>
          <v-divider />
          <div class="form-section-body">
            <v-alert type="info" variant="tonal" density="compact" rounded="lg" class="mb-5" icon="mdi-account-circle">
              Logged in as <strong>{{ authStore.userEmail }}</strong> &nbsp;·&nbsp;
              Cluster must currently have <code>admin / admin</code> credentials
            </v-alert>

            <v-text-field
              v-model="form.ambari_server"
              label="Ambari Server Hostname / IP"
              placeholder="e.g. ambari.company.com or 192.168.1.100"
              variant="outlined"
              density="comfortable"
              prepend-inner-icon="mdi-server-network"
              :error-messages="errors.ambari_server"
              @update:model-value="errors.ambari_server = ''; testResult = null"
              class="mb-4"
            />

            <v-row dense class="mb-1">
              <v-col cols="12" sm="4">
                <v-select
                  v-model="form.http_method"
                  :items="['http', 'https']"
                  label="Protocol"
                  variant="outlined"
                  density="comfortable"
                  prepend-inner-icon="mdi-lock-outline"
                />
              </v-col>
              <v-col cols="12" sm="4">
                <v-text-field
                  v-model.number="form.port"
                  label="Port"
                  type="number"
                  min="1"
                  max="65535"
                  placeholder="8888"
                  variant="outlined"
                  density="comfortable"
                  prepend-inner-icon="mdi-numeric"
                  :error-messages="errors.port"
                  @update:model-value="errors.port = ''; testResult = null"
                />
              </v-col>
              <v-col cols="12" sm="4" class="d-flex align-center pb-1">
                <v-btn
                  color="secondary"
                  variant="tonal"
                  block
                  :loading="testing"
                  :disabled="!canTest"
                  prepend-icon="mdi-connection"
                  rounded="lg"
                  @click="testConnection"
                >
                  Test
                </v-btn>
              </v-col>
            </v-row>

            <v-slide-y-transition>
              <v-alert
                v-if="testResult"
                :type="testResult.type"
                variant="tonal"
                density="compact"
                rounded="lg"
                class="mb-1"
              >
                {{ testResult.message }}
              </v-alert>
            </v-slide-y-transition>
          </div>

          <!-- ② Cluster Managers -->
          <v-divider />
          <div class="form-section-label">
            <div class="label-dot" style="background: rgb(var(--v-theme-warning));" />
            <v-icon size="15" color="warning" class="mr-1">mdi-account-cog</v-icon>
            <span>Cluster Managers</span>
            <v-chip v-if="form.manager_emails.length" size="x-small" color="warning" variant="tonal" class="ml-2">
              {{ form.manager_emails.length }}
            </v-chip>
          </div>
          <v-divider />
          <div class="form-section-body">
            <p class="text-caption text-medium-emphasis mb-3">
              Managers can delete temporary users and re-register this cluster. Super admins can update this list later from the Admin Panel.
            </p>

            <div v-if="form.manager_emails.length" class="d-flex flex-wrap gap-2 mb-3">
              <v-chip
                v-for="(mgr, idx) in form.manager_emails"
                :key="mgr"
                size="small"
                color="warning"
                variant="tonal"
                closable
                @click:close="removeManagerEmail(idx)"
              >
                {{ mgr }}
              </v-chip>
            </div>

            <v-text-field
              v-model="newManagerEmail"
              label="Manager email address"
              variant="outlined"
              density="comfortable"
              placeholder="manager@company.com"
              prepend-inner-icon="mdi-email-plus-outline"
              :error-messages="newManagerEmailError"
              hint="Press Enter or click Add"
              @keyup.enter="addManagerEmail"
              @update:model-value="newManagerEmailError = ''"
            >
              <template #append>
                <v-btn color="warning" variant="tonal" size="small" :disabled="!newManagerEmail" @click="addManagerEmail">
                  Add
                </v-btn>
              </template>
            </v-text-field>

            <v-alert
              v-if="form.manager_emails.length === 0"
              type="warning"
              variant="tonal"
              density="compact"
              rounded="lg"
              class="mt-3"
              icon="mdi-alert-circle-outline"
            >
              At least one cluster manager email is required before registering.
            </v-alert>
          </div>

          <!-- ③ Access Policy -->
          <v-divider />
          <div class="form-section-label">
            <div class="label-dot" style="background: rgb(var(--v-theme-info));" />
            <v-icon size="15" color="info" class="mr-1">mdi-shield-account</v-icon>
            <span>Access Policy</span>
          </div>
          <v-divider />
          <div class="form-section-body pb-5">
            <p class="text-caption text-medium-emphasis mb-3">
              Choose how many temporary users can be active on this cluster at the same time.
            </p>
            <v-row dense>
              <!-- Option A: Multiple users -->
              <v-col cols="12" sm="6">
                <div
                  class="policy-card"
                  :class="{ 'policy-card--selected': !form.single_user_mode, 'policy-card--selected-multi': !form.single_user_mode }"
                  @click="form.single_user_mode = false"
                >
                  <div class="policy-card__icon-wrap policy-card__icon-wrap--multi">
                    <v-icon size="24" color="success">mdi-account-multiple</v-icon>
                  </div>
                  <div class="policy-card__content">
                    <div class="policy-card__title">Multiple Users</div>
                    <div class="policy-card__desc">Several users can hold active credentials simultaneously. Best for shared or team clusters.</div>
                  </div>
                  <div class="policy-card__check">
                    <v-icon v-if="!form.single_user_mode" size="18" color="success">mdi-check-circle</v-icon>
                    <v-icon v-else size="18" color="grey-lighten-1">mdi-circle-outline</v-icon>
                  </div>
                </div>
              </v-col>

              <!-- Option B: Single user -->
              <v-col cols="12" sm="6">
                <div
                  class="policy-card"
                  :class="{ 'policy-card--selected': form.single_user_mode, 'policy-card--selected-single': form.single_user_mode }"
                  @click="form.single_user_mode = true"
                >
                  <div class="policy-card__icon-wrap policy-card__icon-wrap--single">
                    <v-icon size="24" color="info">mdi-account-lock</v-icon>
                  </div>
                  <div class="policy-card__content">
                    <div class="policy-card__title">Single User at a Time</div>
                    <div class="policy-card__desc">New requests are blocked while an active user exists. Best for high-security clusters.</div>
                  </div>
                  <div class="policy-card__check">
                    <v-icon v-if="form.single_user_mode" size="18" color="info">mdi-check-circle</v-icon>
                    <v-icon v-else size="18" color="grey-lighten-1">mdi-circle-outline</v-icon>
                  </div>
                </div>
              </v-col>
            </v-row>
          </div>

          <!-- Footer -->
          <v-divider />
          <div class="pa-4 d-flex justify-end">
            <v-btn
              color="primary"
              size="large"
              :loading="registering"
              :disabled="!canRegister"
              prepend-icon="mdi-check-circle"
              rounded="lg"
              @click="registerCluster"
            >
              Register Cluster
            </v-btn>
          </div>
        </v-card>

        <v-slide-y-transition>
          <v-alert v-if="statusMessage" :type="statusMessage.type" variant="tonal" rounded="lg" class="mt-4">
            {{ statusMessage.text }}
          </v-alert>
        </v-slide-y-transition>
      </v-col>

      <!-- ── Sidebar ─────────────────────────────────────────────────── -->
      <v-col cols="12" md="4">

        <!-- Live URL -->
        <v-card elevation="0" border class="mb-4">
          <div class="form-section-label">
            <v-icon size="15" color="primary" class="mr-1">mdi-web</v-icon>
            <span>Connection URL</span>
          </div>
          <v-divider />
          <div class="px-4 py-3">
            <div class="url-box" :class="{ 'url-box--empty': !connectionUrl }">
              <v-icon size="14" class="mr-1 flex-shrink-0" :color="connectionUrl ? 'primary' : 'grey-lighten-1'">mdi-link-variant</v-icon>
              <span class="url-box__text">{{ connectionUrl || 'Fill in hostname and port…' }}</span>
            </div>
          </div>
        </v-card>

        <!-- Managers summary -->
        <v-card v-if="form.manager_emails.length" elevation="0" border class="mb-4">
          <div class="form-section-label">
            <v-icon size="15" color="warning" class="mr-1">mdi-account-group</v-icon>
            <span>Managers assigned</span>
            <v-chip size="x-small" color="warning" variant="tonal" class="ml-auto">{{ form.manager_emails.length }}</v-chip>
          </div>
          <v-divider />
          <div class="px-4 py-3 d-flex flex-wrap gap-1">
            <v-chip v-for="m in form.manager_emails" :key="m" size="x-small" color="warning" variant="tonal">{{ m }}</v-chip>
          </div>
        </v-card>

        <!-- Policy badge -->
        <v-card elevation="0" border class="mb-4">
          <div class="form-section-label">
            <v-icon size="15" color="info" class="mr-1">mdi-shield-account</v-icon>
            <span>Access Policy</span>
          </div>
          <v-divider />
          <div class="px-4 py-3 d-flex align-center gap-3">
            <v-avatar :color="form.single_user_mode ? 'info' : 'success'" variant="tonal" size="36" rounded="lg">
              <v-icon size="18" :color="form.single_user_mode ? 'info' : 'success'">
                {{ form.single_user_mode ? 'mdi-account-lock' : 'mdi-account-multiple' }}
              </v-icon>
            </v-avatar>
            <div>
              <div class="text-caption font-weight-bold">{{ form.single_user_mode ? 'Single user at a time' : 'Multiple users allowed' }}</div>
              <div class="text-caption text-medium-emphasis">{{ form.single_user_mode ? 'High-security mode' : 'Default mode' }}</div>
            </div>
          </div>
        </v-card>

        <!-- What happens -->
        <v-card elevation="0" border>
          <div class="form-section-label">
            <v-icon size="15" color="secondary" class="mr-1">mdi-information-outline</v-icon>
            <span>What happens on register</span>
          </div>
          <v-divider />
          <v-list density="compact" class="py-2">
            <v-list-item
              v-for="(step, i) in registrationSteps"
              :key="i"
              :prepend-icon="step.icon"
              class="px-4"
            >
              <v-list-item-title class="text-caption" v-html="step.text" />
            </v-list-item>
          </v-list>
        </v-card>
      </v-col>
    </v-row>

    <!-- Confirmation dialog -->
    <v-dialog v-model="confirmDialog" max-width="500" persistent>
      <v-card rounded="xl">
        <div class="pa-5 d-flex align-center gap-3">
          <v-avatar color="warning" variant="tonal" size="44" rounded="lg">
            <v-icon color="warning">mdi-alert-circle-outline</v-icon>
          </v-avatar>
          <div>
            <div class="text-subtitle-1 font-weight-bold">Confirm Registration</div>
            <div class="text-caption text-medium-emphasis">This action cannot be undone</div>
          </div>
        </div>
        <v-divider />
        <div class="pa-5">
          <p class="text-body-2 mb-3">
            Register <strong>{{ form.ambari_server }}</strong>
            via <code>{{ form.http_method }}://{{ form.ambari_server }}:{{ form.port }}</code>?
          </p>
          <v-alert type="warning" variant="tonal" density="compact" rounded="lg" class="mb-3">
            <ul class="mt-1 pl-4 mb-0 text-caption">
              <li>All existing non-admin Ambari users will be removed</li>
              <li><code>vault</code> and <code>ambari_admin_dr</code> accounts will be created</li>
              <li>The original <code>admin/admin</code> account will be deleted</li>
              <li v-if="form.manager_emails.length">
                <strong>{{ form.manager_emails.length }}</strong> manager(s) will be assigned
              </li>
              <li>Access policy: <strong>{{ form.single_user_mode ? 'Single user at a time' : 'Multiple concurrent users' }}</strong></li>
            </ul>
          </v-alert>
        </div>
        <v-divider />
        <div class="pa-4 d-flex justify-end gap-2">
          <v-btn variant="text" @click="confirmDialog = false">Cancel</v-btn>
          <v-btn color="primary" rounded="lg" :loading="registering" prepend-icon="mdi-check" @click="doRegisterCluster">
            Confirm & Register
          </v-btn>
        </div>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { apiPost } from '../api/client'

const authStore = useAuthStore()
const form = reactive({
  ambari_server: '',
  port: 8888,
  http_method: 'http',
  manager_emails: [],
  single_user_mode: false,
})
const errors = reactive({ ambari_server: '', port: '' })
const testing = ref(false)
const registering = ref(false)
const testResult = ref(null)
const statusMessage = ref(null)
const confirmDialog = ref(false)
const newManagerEmail = ref('')
const newManagerEmailError = ref('')

const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/

const registrationSteps = [
  { icon: 'mdi-account-check', text: 'Verify <code>admin/admin</code> credentials' },
  { icon: 'mdi-account-remove', text: 'Remove all existing non-admin users' },
  { icon: 'mdi-key-plus', text: 'Create <code>vault</code> admin account' },
  { icon: 'mdi-shield-key', text: 'Create <code>ambari_admin_dr</code> backup account' },
  { icon: 'mdi-database-lock', text: 'Store encrypted credentials in Kavach DB' },
  { icon: 'mdi-account-cancel', text: 'Delete original <code>admin/admin</code> account' },
]

function addManagerEmail() {
  const email = newManagerEmail.value.trim().toLowerCase()
  if (!email) return
  if (!EMAIL_REGEX.test(email)) { newManagerEmailError.value = 'Enter a valid email address'; return }
  if (form.manager_emails.includes(email)) { newManagerEmailError.value = 'Email already added'; return }
  form.manager_emails.push(email)
  newManagerEmail.value = ''
  newManagerEmailError.value = ''
}

function removeManagerEmail(idx) {
  form.manager_emails.splice(idx, 1)
}

const connectionUrl = computed(() =>
  form.ambari_server && form.port ? `${form.http_method}://${form.ambari_server}:${form.port}` : ''
)

const canTest = computed(() => form.ambari_server && form.port >= 1 && form.port <= 65535)
const canRegister = computed(() =>
  canTest.value && Object.values(errors).every(e => !e) && form.manager_emails.length > 0
)

async function testConnection() {
  if (!canTest.value) return
  testing.value = true
  testResult.value = null
  try {
    const res = await apiPost('/api/test_connection', {
      ambari_server: form.ambari_server,
      port: form.port,
      http_method: form.http_method,
    })
    const ok = res.data?.ok
    testResult.value = { type: ok ? 'success' : 'error', message: res.data?.message || 'Test complete.' }
  } catch (e) {
    testResult.value = { type: 'error', message: e.response?.data?.message || e.response?.data?.error || e.message || 'Connection failed.' }
  } finally {
    testing.value = false
  }
}

function registerCluster() {
  if (!canRegister.value) return
  confirmDialog.value = true
}

async function doRegisterCluster() {
  confirmDialog.value = false
  registering.value = true
  statusMessage.value = null
  try {
    await apiPost('/api/register', {
      ambari_server: form.ambari_server,
      port: form.port,
      http_method: form.http_method,
      manager_emails: form.manager_emails,
      single_user_mode: form.single_user_mode,
    })
    const managerNote = form.manager_emails.length ? ` ${form.manager_emails.length} manager(s) assigned.` : ''
    const policyNote = form.single_user_mode ? ' Single-user access policy enforced.' : ''
    statusMessage.value = { type: 'success', text: `Cluster '${form.ambari_server}' registered successfully.${managerNote}${policyNote}` }
    form.ambari_server = ''
    form.port = 8888
    form.http_method = 'http'
    form.manager_emails = []
    form.single_user_mode = false
    testResult.value = null
  } catch (e) {
    statusMessage.value = { type: 'error', text: e.response?.data?.error || e.message || 'Registration failed.' }
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
/* ── Page header icon ────────────────────────────── */
.page-icon-wrap {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: rgba(var(--v-theme-primary), 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* ── Section label bar ───────────────────────────── */
.form-section-label {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  background: rgba(0, 0, 0, 0.025);
  font-size: 0.78rem;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.6);
  letter-spacing: 0.02em;
  text-transform: uppercase;
  gap: 2px;
}

.label-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  flex-shrink: 0;
}

/* ── Section body padding ────────────────────────── */
.form-section-body {
  padding: 20px 20px 12px;
}

/* ── Connection URL box ──────────────────────────── */
.url-box {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  padding: 10px 12px;
  background: rgba(var(--v-theme-primary), 0.06);
  border-radius: 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.82rem;
  color: rgb(var(--v-theme-primary));
  word-break: break-all;
  line-height: 1.5;
  min-height: 40px;
}

.url-box--empty {
  background: rgba(0, 0, 0, 0.04);
  color: rgba(0, 0, 0, 0.35);
  font-style: italic;
}

.url-box__text {
  flex: 1;
}

/* ── Access policy selector cards ───────────────── */
.policy-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 14px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.18s, background 0.18s, box-shadow 0.18s;
  height: 100%;
  background: #fff;
  user-select: none;
}

.policy-card:hover {
  border-color: rgba(0, 0, 0, 0.22);
  background: rgba(0, 0, 0, 0.01);
}

.policy-card--selected-multi {
  border-color: rgb(var(--v-theme-success)) !important;
  background: rgba(var(--v-theme-success), 0.05) !important;
  box-shadow: 0 0 0 1px rgba(var(--v-theme-success), 0.3);
}

.policy-card--selected-single {
  border-color: rgb(var(--v-theme-info)) !important;
  background: rgba(var(--v-theme-info), 0.05) !important;
  box-shadow: 0 0 0 1px rgba(var(--v-theme-info), 0.3);
}

.policy-card__icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.policy-card__icon-wrap--multi {
  background: rgba(var(--v-theme-success), 0.12);
}

.policy-card__icon-wrap--single {
  background: rgba(var(--v-theme-info), 0.12);
}

.policy-card__content {
  flex: 1;
  min-width: 0;
}

.policy-card__title {
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.3;
  margin-bottom: 4px;
}

.policy-card__desc {
  font-size: 0.75rem;
  color: rgba(0, 0, 0, 0.55);
  line-height: 1.45;
}

.policy-card__check {
  flex-shrink: 0;
  margin-top: 2px;
}
</style>
