import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from './views/DashboardView.vue'
import AdminView from './views/AdminView.vue'
import AccountView from './views/AccountView.vue'
import CalendarView from './views/CalendarView.vue'
import DirectionsView from './views/DirectionsView.vue'
import LoginView from './views/LoginView.vue'
import PlanView from './views/PlanView.vue'
import ProfileView from './views/ProfileView.vue'
import ResourceLibraryView from './views/ResourceLibraryView.vue'
import WorkflowView from './views/WorkflowView.vue'
import { cleanupElementPlusOverlays } from './utils/overlay'

function parseJwtPayload(token: string): { role?: string } {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
  } catch {
    return {}
  }
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/', component: DashboardView },
    { path: '/calendar', component: CalendarView },
    { path: '/directions', component: DirectionsView },
    { path: '/resources', component: ResourceLibraryView },
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

router.beforeEach((to) => {
  if (to.meta.public) {
    return true
  }
  const token = localStorage.getItem('x-agent-study-token')
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin && parseJwtPayload(token).role !== 'ADMIN') {
    return { path: '/' }
  }
  return true
})

router.afterEach(() => {
  cleanupElementPlusOverlays()
})
