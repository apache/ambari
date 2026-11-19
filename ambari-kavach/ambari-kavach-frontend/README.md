# Ambari Kavach — Frontend

Vue 3 single-page application that provides the Ambari Kavach web dashboard. Built with Vuetify 3 for Material Design UI and Pinia for state management.

---

## Tech Stack

| Library | Version | Purpose |
|---|---|---|
| Vue 3 | ^3.x | Reactive UI framework |
| Vuetify 3 | ^3.x | Material Design component library |
| Pinia | ^3.x | Global state (auth store) |
| Vue Router 4 | ^4.x | Client-side routing with auth guards |
| Axios | ^1.x | HTTP client for backend API calls |
| vue3-google-login | ^2.x | Google OAuth button component |
| Vee Validate + Yup | ^4.x / ^1.x | Form validation |
| Material Design Icons | latest | Icon set |

---

## Directory Layout

```
src/
├── App.vue                    # Root component
├── main.js                    # Entry point — mounts app, registers plugins
├── api/
│   └── client.js              # Axios instance (base URL + JWT interceptor)
├── assets/
│   └── logo.png
├── components/
│   ├── AppLayout.vue          # Shell layout (navbar + sidebar + content slot)
│   ├── CreateAmbariUser.vue   # Temp user creation form (reusable widget)
│   ├── LoginPage.vue          # Google sign-in page
│   ├── NavBar.vue             # Top navigation bar
│   ├── RegisterClusterForm.vue# Cluster registration form
│   └── SideBar.vue            # Left sidebar navigation
├── composables/
│   └── useSnackbar.js         # Global toast notification composable
├── layouts/
│   └── DefaultLayout.vue      # Default page wrapper
├── plugins/
│   └── vuetify.js             # Vuetify instance + theme config
├── router/
│   └── index.js               # Routes + navigation guard (auth check)
├── stores/
│   └── auth.js                # Pinia auth store (token, email, role)
├── utils/
│   └── dateUtils.js           # Date formatting helpers
└── views/
    ├── AdminPanel.vue          # Super admin — cluster + manager management
    ├── AnalyticsView.vue       # Usage charts and statistics
    ├── AuditLogsView.vue       # Paginated audit event log
    ├── ClusterManager.vue      # Manager — per-cluster user management
    ├── ClusterRegister.vue     # Cluster registration page
    ├── ClustersView.vue        # All registered clusters
    ├── CreateAmbariUserPage.vue# Temp user creation page
    ├── Dashboard.vue           # Summary dashboard
    ├── MyUsers.vue             # Logged-in user's temp accounts
    └── ProfileView.vue         # User profile and role info
```

---

## Setup

### Prerequisites

- Node.js 16+
- npm 8+
- Kavach backend running at `http://localhost:5000`
- Google OAuth Client ID

### Install dependencies

```bash
npm install
```

### Environment variables

Create `.env.local` (gitignored):

```env
VUE_APP_GOOGLE_CLIENT_ID=<your-client-id>.apps.googleusercontent.com
```

For production builds, create `.env.production`:

```env
VUE_APP_GOOGLE_CLIENT_ID=<your-client-id>.apps.googleusercontent.com
VUE_APP_API_BASE=https://kavach.yourcompany.com
```

### Run development server

```bash
npm run serve
```

Runs at `http://localhost:8080`. API calls are automatically proxied to `http://localhost:5000` via `vue.config.js`.

### Build for production

```bash
npm run build
```

Output goes to `dist/`. Serve with Nginx or any static file server. See the [root README](../README.md#production-deployment) for an example Nginx configuration.

### Lint

```bash
npm run lint
```

---

## Routing

All routes except `/login` require a valid JWT in `localStorage`. The navigation guard in `router/index.js` redirects unauthenticated users to `/login`.

| Route | View | Access |
|---|---|---|
| `/login` | LoginPage | Public |
| `/dashboard` | Dashboard | Authenticated |
| `/myusers` | MyUsers | Authenticated |
| `/create_ambari_users` | CreateAmbariUserPage | Authenticated |
| `/clusters` | ClustersView | Authenticated |
| `/clusters/register` | ClusterRegister | Super Admin |
| `/clusters/manager` | ClusterManager | Manager+ |
| `/analytics` | AnalyticsView | Authenticated |
| `/audit-logs` | AuditLogsView | Authenticated |
| `/profile` | ProfileView | Authenticated |
| `/admin` | AdminPanel | Super Admin |

---

## State Management (Pinia)

`stores/auth.js` holds:

| State | Type | Description |
|---|---|---|
| `token` | String | JWT (persisted to localStorage) |
| `email` | String | Logged-in user's email |
| `isLoggedIn` | Boolean | Derived: `token != null` |
| `isSuperAdmin` | Boolean | Populated from `/api/me` response |
| `isManager` | Boolean | Populated from `/api/me` response |

---

## API Client

`api/client.js` exports an Axios instance that:

1. Sets `baseURL` from `VUE_APP_API_BASE` (defaults to `/` for proxied dev)
2. Injects `Authorization: Bearer <token>` on every request
3. Injects `X-Email: <email>` on every request (backend validates this against the JWT identity)
4. On 401 response — clears the auth store and redirects to `/login`

---

## Dev Proxy

`vue.config.js` proxies the following paths to `http://localhost:5000` during development:

| Prefix | Backend usage |
|---|---|
| `/api` | All REST API endpoints |
| `/auth` | Google OAuth callback |
| `/create_user` | Temporary user creation |
| `/manager` | Manager-scoped actions |
