import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from './views/DashboardView.vue'
import AdminView from './views/AdminView.vue'
import AccountView from './views/AccountView.vue'
import CalendarView from './views/CalendarView.vue'
import DirectionsView from './views/DirectionsView.vue'
import LoginView from './views/LoginView.vue'
import OutcomesView from './views/OutcomesView.vue'
import PlanView from './views/PlanView.vue'
import ProfileView from './views/ProfileView.vue'
import ResourceApplicationView from './views/ResourceApplicationView.vue'
import ResourceContributorView from './views/ResourceContributorView.vue'
import ResourceLibraryView from './views/ResourceLibraryView.vue'
import { readStoredUserSession, writeStoredUserSession, type UserSession } from './stores/auth'
import WorkflowView from './views/WorkflowView.vue'
import { cleanupElementPlusOverlays } from './utils/overlay'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/', component: DashboardView },
    { path: '/outcomes', component: OutcomesView },
    { path: '/calendar', component: CalendarView },
    { path: '/directions', component: DirectionsView },
    { path: '/resources', component: ResourceLibraryView },
    { path: '/resources/apply', component: ResourceApplicationView },
    { path: '/resource-management', component: ResourceContributorView, meta: { resourceManager: true } },
    { path: '/directions/:directionId/profile', component: ProfileView },
    { path: '/plans/:planId', component: PlanView },
    { path: '/plans/:planId/workflow', component: WorkflowView },
    { path: '/plans/:planId/knowledge', redirect: (to) => ({ path: `/plans/${to.params.planId}`, query: { tab: 'knowledge' } }) },
    { path: '/knowledge', redirect: '/directions' },
    { path: '/qa', redirect: '/directions' },
    { path: '/quiz', redirect: '/directions' },
    { path: '/review', redirect: '/directions' },
    { path: '/report', redirect: '/directions' },
    { path: '/model-settings', redirect: '/admin', meta: { admin: true } },
    { path: '/billing', redirect: { path: '/account', query: { tab: 'billing' } } },
    { path: '/admin', component: AdminView, meta: { admin: true } },
    { path: '/account', component: AccountView },
  ],
})

router.beforeEach(async (to) => {
  if (to.meta.public) {
    return true
  }
  const token = localStorage.getItem('x-agent-study-token')
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  let user = readStoredUserSession()
  if (!user && (to.meta.admin || to.meta.resourceManager)) {
    try {
      const response = await fetch('/api/v1/auth/me', {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      })
      if (response.ok) {
        const payload = await response.json() as { data?: UserSession }
        user = payload.data || null
        writeStoredUserSession(user)
      }
    } catch {
      user = null
    }
  }
  if (to.meta.admin && user?.role !== 'ADMIN') {
    return { path: '/' }
  }
  if (to.meta.resourceManager && !(user?.resourceManager || user?.role === 'ADMIN')) {
    return { path: '/resources' }
  }
  return true
})

router.afterEach(() => {
  cleanupElementPlusOverlays()
})
