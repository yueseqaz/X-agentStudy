<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import {
  Camera,
  CircleCheck,
  Coin,
  CreditCard,
  Key,
  Lock,
  Message,
  RefreshRight,
  TrendCharts,
  User,
  Wallet,
} from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { http, type ApiResponse } from '../api/http'
import { useAuthStore } from '../stores/auth'

interface SecurityStatus {
  userId: number
  account: string
  role: string
  avatarUrl: string | null
  disabled: boolean
  disabledUntil: string | null
  emailVerified: boolean
  failedLoginCount: number
  lockedUntil: string | null
  lastLoginAt: string | null
  createdAt: string
  refreshTokenStored: boolean
}

interface TokenResponse {
  message: string
  token: string
}

interface AuthPayload {
  token: string
  refreshToken: string
  user: {
    id: number
    nickname: string
    account: string
    role: string
    resourceManager: boolean
    avatarUrl: string | null
    emailVerified: boolean
    disabled: boolean
    disabledUntil: string | null
  }
}

interface UserSession {
  id: number
  nickname: string
  account: string
  role: string
  resourceManager: boolean
  avatarUrl: string | null
  emailVerified: boolean
  disabled: boolean
  disabledUntil: string | null
}

interface Billing {
  planCode: string
  planName: string
  monthlyPriceCents: number
  monthlyAgentQuota: number
  usedAgentCalls: number
  remainingAgentCalls: number
  storageQuotaMb: number
  usedStorageMb: number
  planQuota: number
  usedPlanCount: number
  walletBalanceCents: number
  periodStart: string
  periodEnd: string | null
}

interface SubscriptionPlan {
  code: string
  displayName: string
  monthlyPriceCents: number
  monthlyAgentQuota: number
  storageQuotaMb: number
  planQuota: number
  features: string[]
}

const auth = useAuthStore()
const route = useRoute()
const loading = ref(false)
const billingLoading = ref(false)
const security = ref<SecurityStatus | null>(null)
const activeAccountTab = ref(route.query.tab === 'billing' ? 'billing' : 'profile')
const resetToken = ref('')
const verifyToken = ref('')
const purchasing = ref('')
const billing = ref<Billing | null>(null)
const plans = ref<SubscriptionPlan[]>([])
const selectedPlanCode = ref('')
const purchaseDialogVisible = ref(false)
const pendingPlan = ref<SubscriptionPlan | null>(null)
const paymentStep = ref<'review' | 'processing' | 'done'>('review')
const localRefreshToken = computed(() => localStorage.getItem('x-agent-study-refresh-token') || '')
const selectedPlan = computed(() => plans.value.find((plan) => plan.code === selectedPlanCode.value) || plans.value[0])
const planQuotaMap: Record<string, number> = { FREE: 1, PLUS: 5, PRO: 20 }
const effectivePlanQuota = computed(() => {
  if (!billing.value) return 0
  const fromPlanList = plans.value.find((item) => item.code === billing.value?.planCode)?.planQuota
  const fallback = planQuotaMap[billing.value.planCode] || 1
  const candidate = fromPlanList || billing.value.planQuota || fallback
  return candidate > 200 ? fallback : candidate
})
const agentPercent = computed(() => billing.value ? Math.min(100, Math.round((billing.value.usedAgentCalls / billing.value.monthlyAgentQuota) * 100)) : 0)
const planPercent = computed(() => {
  if (!billing.value) return 0
  const quota = Math.max(1, effectivePlanQuota.value)
  return Math.min(100, Math.round((billing.value.usedPlanCount / quota) * 100))
})
const walletEnough = computed(() => {
  if (!billing.value || !selectedPlan.value) return true
  return billing.value.walletBalanceCents >= payableCents(selectedPlan.value)
})
const purchaseDelta = computed(() => {
  if (!billing.value || !selectedPlan.value) return 0
  return billing.value.walletBalanceCents - payableCents(selectedPlan.value)
})
const planRank = {
  FREE: 0,
  PLUS: 1,
  PRO: 2,
} as Record<string, number>

async function fetchSecurity() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<SecurityStatus>>('/auth/security')
    security.value = response.data.data
  } finally {
    loading.value = false
  }
}

async function uploadAvatar(options: UploadRequestOptions) {
  const formData = new FormData()
  formData.append('file', options.file)
  const response = await http.post<ApiResponse<UserSession>>('/auth/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  auth.user = response.data.data
  ElMessage.success('头像已更新')
  await fetchSecurity()
  options.onSuccess(response.data)
}

async function requestPasswordReset() {
  if (!security.value) return
  const response = await http.post<ApiResponse<TokenResponse>>('/auth/password-reset/request', {
    account: security.value.account,
  })
  resetToken.value = response.data.data.token
  ElMessage.success('已生成密码重置 token')
}

async function requestEmailVerification() {
  const response = await http.post<ApiResponse<TokenResponse>>('/auth/email-verification/request')
  verifyToken.value = response.data.data.token
  ElMessage.success('已生成邮箱验证 token')
}

async function confirmEmailVerification() {
  if (!verifyToken.value) {
    ElMessage.warning('请先生成邮箱验证 token')
    return
  }
  await http.post('/auth/email-verification/confirm', { refreshToken: verifyToken.value })
  ElMessage.success('邮箱已验证')
  auth.fetchMe()
  fetchSecurity()
}

async function refreshToken() {
  if (!localRefreshToken.value) {
    ElMessage.warning('本地没有 refresh token')
    return
  }
  const response = await http.post<ApiResponse<AuthPayload>>('/auth/refresh', {
    refreshToken: localRefreshToken.value,
  })
  auth.setSession(response.data.data)
  ElMessage.success('Token 已刷新')
  fetchSecurity()
}

async function fetchBilling() {
  billingLoading.value = true
  try {
    const [billingResponse, plansResponse] = await Promise.all([
      http.get<ApiResponse<Billing>>('/billing/current'),
      http.get<ApiResponse<SubscriptionPlan[]>>('/billing/plans'),
    ])
    billing.value = billingResponse.data.data
    plans.value = plansResponse.data.data
    selectedPlanCode.value = billing.value.planCode
  } finally {
    billingLoading.value = false
  }
}

function selectPlan(plan: SubscriptionPlan) {
  selectedPlanCode.value = plan.code
}

async function purchasePlan(plan: SubscriptionPlan) {
  if (!billing.value) return
  selectedPlanCode.value = plan.code
  if (billing.value.planCode === plan.code) {
    ElMessage.info('当前已经是该套餐')
    return
  }
  if (planRank[plan.code] < planRank[billing.value.planCode]) {
    ElMessage.warning('当前套餐等级更高，暂不支持降级购买')
    return
  }
  pendingPlan.value = plan
  paymentStep.value = 'review'
  purchaseDialogVisible.value = true
}

async function confirmPurchase() {
  if (!billing.value || !pendingPlan.value) return
  const payable = payableCents(pendingPlan.value)
  if (billing.value.walletBalanceCents < payable) {
    ElMessage.error(`钱包余额不足，还差 ${formatMoney(payable - billing.value.walletBalanceCents)}`)
    return
  }
  purchasing.value = pendingPlan.value.code
  paymentStep.value = 'processing'
  try {
    await new Promise((resolve) => window.setTimeout(resolve, 650))
    const response = await http.post<ApiResponse<Billing>>('/billing/purchase', { planCode: pendingPlan.value?.code })
    billing.value = response.data.data
    selectedPlanCode.value = response.data.data.planCode
    paymentStep.value = 'done'
    ElMessage.success(`已购买 ${response.data.data.planName}`)
    window.setTimeout(() => {
      purchaseDialogVisible.value = false
      pendingPlan.value = null
      paymentStep.value = 'review'
    }, 900)
  } catch (error: any) {
    paymentStep.value = 'review'
    ElMessage.error(error.response?.data?.message || '购买失败，请检查钱包余额或稍后重试')
  } finally {
    purchasing.value = ''
  }
}

function pendingPurchaseDelta(plan: SubscriptionPlan | null) {
  if (!billing.value || !plan) return 0
  return billing.value.walletBalanceCents - payableCents(plan)
}

function payableCents(plan: SubscriptionPlan) {
  if (!billing.value) {
    return plan.monthlyPriceCents
  }
  return Math.max(0, plan.monthlyPriceCents - billing.value.monthlyPriceCents)
}

function formatTime(value: string | null) {
  if (!value) {
    return '无'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function formatMoney(cents: number) {
  return `¥${(cents / 100).toFixed(0)}`
}

function formatPlanPrice(cents: number) {
  return cents === 0 ? '免费 · ¥0 / 月' : `${formatMoney(cents)} / 月`
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleDateString() : '按月滚动'
}

function planButtonText(plan: SubscriptionPlan) {
  if (!billing.value) return '购买套餐'
  if (billing.value.planCode === plan.code) return '正在使用'
  if (planRank[plan.code] < planRank[billing.value.planCode]) return '不可降级'
  if (payableCents(plan) > billing.value.walletBalanceCents) return '余额不足'
  return `购买 ${plan.displayName}`
}

function canPurchasePlan(plan: SubscriptionPlan) {
  if (!billing.value) return false
  return billing.value.planCode !== plan.code
    && planRank[plan.code] > planRank[billing.value.planCode]
}

watch(() => route.query.tab, (tab) => {
  if (tab === 'billing') {
    activeAccountTab.value = 'billing'
  }
})

onMounted(() => {
  fetchSecurity()
  fetchBilling()
})
</script>

<template>
  <section class="account-page surface panel-pad">
    <el-skeleton v-if="loading && !security" :rows="8" animated />
    <template v-else-if="security">
      <div class="section-head">
        <div>
          <h2>个人中心</h2>
          <p>账号身份、登录安全、邮箱验证和令牌状态。</p>
        </div>
        <el-tag :type="security.disabled ? 'danger' : 'success'">
          {{ security.disabled ? '已禁用' : '正常' }}
        </el-tag>
      </div>

      <el-tabs v-model="activeAccountTab" class="account-tabs">
        <el-tab-pane label="资料" name="profile">
          <div class="account-hero">
            <div class="account-avatar">
              <img v-if="security.avatarUrl" :src="security.avatarUrl" alt="avatar" />
              <span v-else>{{ auth.user?.nickname?.slice(0, 1) || 'U' }}</span>
            </div>
            <div>
              <span>{{ security.role }}</span>
              <h3>{{ auth.user?.nickname }}</h3>
              <p>{{ security.account }}</p>
            </div>
            <el-upload
              :show-file-list="false"
              :http-request="uploadAvatar"
              accept="image/png,image/jpeg,image/webp,image/gif"
            >
              <el-button :icon="Camera">上传头像</el-button>
            </el-upload>
          </div>

          <div class="security-grid">
            <div>
              <el-icon><User /></el-icon>
              <span>用户 ID</span>
              <strong>{{ security.userId }}</strong>
            </div>
            <div>
              <el-icon><Message /></el-icon>
              <span>邮箱验证</span>
              <strong>{{ security.emailVerified ? '已验证' : '未验证' }}</strong>
            </div>
            <div>
              <el-icon><Lock /></el-icon>
              <span>登录失败次数</span>
              <strong>{{ security.failedLoginCount }}</strong>
            </div>
            <div>
              <el-icon><Key /></el-icon>
              <span>Refresh Token</span>
              <strong>{{ security.refreshTokenStored ? '已存储' : '未存储' }}</strong>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="安全" name="security">
          <div class="security-timeline">
            <div>
              <span>最近登录</span>
              <strong>{{ formatTime(security.lastLoginAt) }}</strong>
            </div>
            <div>
              <span>锁定截止</span>
              <strong>{{ formatTime(security.lockedUntil) }}</strong>
            </div>
            <div>
              <span>封禁截止</span>
              <strong>{{ formatTime(security.disabledUntil) }}</strong>
            </div>
            <div>
              <span>创建时间</span>
              <strong>{{ formatTime(security.createdAt) }}</strong>
            </div>
          </div>

          <div class="account-actions">
            <el-button :icon="RefreshRight" type="primary" @click="refreshToken">刷新 Token</el-button>
            <el-button :icon="Message" @click="requestEmailVerification">生成邮箱验证 token</el-button>
            <el-button :icon="Key" @click="requestPasswordReset">生成密码重置 token</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="邮箱验证" name="email">
          <section class="account-token-panel">
            <div class="section-head">
              <div>
                <h2>邮箱验证</h2>
                <p>生成 token 后可立即验证当前账号。</p>
              </div>
            </div>
            <el-input v-model="verifyToken" placeholder="邮箱验证 token" />
            <el-button type="primary" style="width: 100%; margin-top: 10px" @click="confirmEmailVerification">
              确认邮箱验证
            </el-button>
          </section>
        </el-tab-pane>

        <el-tab-pane label="密码重置" name="password">
          <section class="account-token-panel">
            <div class="section-head">
              <div>
                <h2>密码重置 token</h2>
                <p>这里只展示生成结果，重置提交入口在登录体系接口中。</p>
              </div>
            </div>
            <el-input v-model="resetToken" type="textarea" :rows="3" readonly placeholder="尚未生成" />
          </section>
        </el-tab-pane>

        <el-tab-pane label="订阅钱包" name="billing">
          <div class="billing-page account-billing-page">
            <el-skeleton v-if="billingLoading" :rows="10" animated />
            <template v-else-if="billing">
              <section class="billing-hero">
                <div>
                  <span class="eyebrow">Subscription Wallet</span>
                  <h2>订阅钱包</h2>
                  <p>钱包由管理员充值，购买 Plus / Pro 时从钱包扣款。Agent 调用、文档生成、题目生成和 AI 批改都会计入额度。</p>
                </div>
                <div class="wallet-ledger">
                  <el-icon><Wallet /></el-icon>
                  <span>钱包余额</span>
                  <strong>{{ formatMoney(billing.walletBalanceCents) }}</strong>
                  <small>余额不足时购买会被拦截</small>
                </div>
              </section>

              <section class="billing-status-grid">
                <div class="metric-card billing-status-card">
                  <el-icon><CreditCard /></el-icon>
                  <span>当前套餐</span>
                  <strong>{{ billing.planName }}</strong>
                  <small>有效期至 {{ formatDate(billing.periodEnd) }}</small>
                </div>
                <div class="metric-card billing-status-card">
                  <el-icon><TrendCharts /></el-icon>
                  <span>Agent 调用</span>
                  <strong>{{ billing.usedAgentCalls }} / {{ billing.monthlyAgentQuota }}</strong>
                  <el-progress :percentage="agentPercent" />
                  <small>剩余 {{ billing.remainingAgentCalls }} 次</small>
                </div>
                <div class="metric-card billing-status-card">
                  <el-icon><Coin /></el-icon>
                  <span>学习计划数</span>
                  <strong>{{ billing.usedPlanCount }} / {{ effectivePlanQuota }} 个</strong>
                  <el-progress :percentage="planPercent" />
                </div>
              </section>

              <section class="billing-purchase-panel">
                <div class="section-head">
                  <div>
                    <h2>选择套餐</h2>
                    <p>仅支持向上升级；Plus 升 Pro 按差价扣款，降级暂不开放。</p>
                  </div>
                  <el-tag :type="walletEnough ? 'success' : 'danger'">
                    {{ walletEnough ? '余额可购买所选套餐' : '余额不足' }}
                  </el-tag>
                </div>

                <div class="subscription-plans billing-plan-grid">
                  <article
                    v-for="plan in plans"
                    :key="plan.code"
                    class="subscription-card billing-plan-card"
                    :class="{ active: billing.planCode === plan.code, selected: selectedPlanCode === plan.code }"
                    @click="selectPlan(plan)"
                  >
                    <div class="plan-card-head">
                      <span>{{ plan.code }}</span>
                      <el-tag v-if="billing.planCode === plan.code" type="success">当前套餐</el-tag>
                    </div>
                    <h3>{{ plan.displayName }}</h3>
                    <strong>{{ formatPlanPrice(plan.monthlyPriceCents) }}</strong>
                    <p>{{ plan.monthlyAgentQuota }} 次 Agent 调用 · {{ plan.planQuota }} 个学习计划</p>
                    <ul>
                      <li v-for="feature in plan.features" :key="feature">
                        <el-icon><CircleCheck /></el-icon>
                        {{ feature }}
                      </li>
                    </ul>
                    <el-button
                      :type="billing.planCode === plan.code ? 'success' : selectedPlanCode === plan.code ? 'primary' : 'default'"
                      :plain="billing.planCode !== plan.code"
                      :disabled="!canPurchasePlan(plan)"
                      :loading="purchasing === plan.code"
                      @click.stop="purchasePlan(plan)"
                    >
                      {{ planButtonText(plan) }}
                    </el-button>
                  </article>
                </div>
              </section>

              <aside v-if="selectedPlan" class="purchase-summary">
                <div class="section-head">
                  <div>
                    <h2>购买预览</h2>
                    <p>购买前再次确认扣款和额度变化。</p>
                  </div>
                  <el-icon><Lock /></el-icon>
                </div>
                <div class="purchase-summary-grid">
                  <div><span>所选套餐</span><strong>{{ selectedPlan.displayName }}</strong></div>
                  <div><span>扣款金额</span><strong>{{ formatMoney(payableCents(selectedPlan)) }}</strong></div>
                  <div><span>购买后余额</span><strong :class="{ danger: purchaseDelta < 0 }">{{ purchaseDelta >= 0 ? formatMoney(purchaseDelta) : `差 ${formatMoney(Math.abs(purchaseDelta))}` }}</strong></div>
                  <div><span>Agent 额度</span><strong>{{ selectedPlan.monthlyAgentQuota }} 次 / 月</strong></div>
                </div>
              </aside>

              <el-dialog
                v-model="purchaseDialogVisible"
                width="520px"
                class="checkout-dialog"
                :show-close="paymentStep !== 'processing'"
                destroy-on-close
              >
                <div v-if="pendingPlan && billing" class="checkout-panel">
                  <div class="checkout-orbit" :class="paymentStep">
                    <el-icon v-if="paymentStep === 'done'"><CircleCheck /></el-icon>
                    <el-icon v-else><Wallet /></el-icon>
                  </div>
                  <span class="eyebrow">Wallet Checkout</span>
                  <h2>{{ paymentStep === 'done' ? '购买完成' : `购买 ${pendingPlan.displayName}` }}</h2>
                  <p>
                    {{ paymentStep === 'processing'
                      ? '正在模拟扣款并写入订阅账户。'
                      : paymentStep === 'done'
                        ? '订阅额度已刷新，可以继续使用 Agent 能力。'
                        : '管理员充值的钱包余额会用于本次模拟购买，不接入真实支付。' }}
                  </p>

                  <div class="checkout-ledger">
                    <div>
                      <span>当前余额</span>
                      <strong>{{ formatMoney(billing.walletBalanceCents) }}</strong>
                    </div>
                    <div>
                      <span>本次扣款</span>
                      <strong>{{ formatMoney(payableCents(pendingPlan)) }}</strong>
                    </div>
                    <div>
                      <span>购买后余额</span>
                      <strong :class="{ danger: pendingPurchaseDelta(pendingPlan) < 0 }">
                        {{ pendingPurchaseDelta(pendingPlan) >= 0 ? formatMoney(pendingPurchaseDelta(pendingPlan)) : `差 ${formatMoney(Math.abs(pendingPurchaseDelta(pendingPlan)))}` }}
                      </strong>
                    </div>
                  </div>

                  <div class="checkout-steps">
                    <span :class="{ active: paymentStep === 'review' }">确认订单</span>
                    <span :class="{ active: paymentStep === 'processing' }">钱包扣款</span>
                    <span :class="{ active: paymentStep === 'done' }">额度生效</span>
                  </div>

                  <div class="checkout-actions">
                    <el-button :disabled="paymentStep === 'processing'" @click="purchaseDialogVisible = false">取消</el-button>
                    <el-button
                      type="primary"
                      :loading="paymentStep === 'processing'"
                      :disabled="pendingPurchaseDelta(pendingPlan) < 0 || paymentStep === 'done'"
                      @click="confirmPurchase"
                    >
                      {{ pendingPurchaseDelta(pendingPlan) < 0 ? '余额不足' : '确认扣款并购买' }}
                    </el-button>
                  </div>
                </div>
              </el-dialog>
            </template>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </section>
</template>
