<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Coin, CreditCard, Lock, TrendCharts, Wallet } from '@element-plus/icons-vue'
import { http, type ApiResponse } from '../api/http'

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

const loading = ref(false)
const purchasing = ref('')
const billing = ref<Billing | null>(null)
const plans = ref<SubscriptionPlan[]>([])
const selectedPlanCode = ref('')
const purchaseDialogVisible = ref(false)
const pendingPlan = ref<SubscriptionPlan | null>(null)
const paymentStep = ref<'review' | 'processing' | 'done'>('review')

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

async function fetchBilling() {
  loading.value = true
  try {
    const [billingResponse, plansResponse] = await Promise.all([
      http.get<ApiResponse<Billing>>('/billing/current'),
      http.get<ApiResponse<SubscriptionPlan[]>>('/billing/plans'),
    ])
    billing.value = billingResponse.data.data
    plans.value = plansResponse.data.data
    selectedPlanCode.value = billing.value.planCode
  } finally {
    loading.value = false
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
    const message = error.response?.data?.message || '购买失败，请检查钱包余额或稍后重试'
    ElMessage.error(message)
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

function formatMoney(cents: number) {
  return `¥${(cents / 100).toFixed(0)}`
}

function formatPlanPrice(cents: number) {
  return cents === 0 ? '免费 · ¥0 / 月' : `${formatMoney(cents)} / 月`
}

function formatDate(value: string | null) {
  return value ? new Date(value).toLocaleDateString() : '按月滚动'
}

onMounted(fetchBilling)
</script>

<template>
  <div class="billing-page">
    <el-skeleton v-if="loading" :rows="10" animated />
    <template v-else-if="billing">
      <section class="billing-hero surface panel-pad">
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

      <section class="surface panel-pad billing-purchase-panel">
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
              :disabled="billing.planCode === plan.code || planRank[plan.code] < planRank[billing.planCode]"
              :loading="purchasing === plan.code"
              @click.stop="purchasePlan(plan)"
            >
              {{ billing.planCode === plan.code ? '正在使用' : planRank[plan.code] < planRank[billing.planCode] ? '不可降级' : payableCents(plan) > billing.walletBalanceCents ? '余额不足' : '购买套餐' }}
            </el-button>
          </article>
        </div>
      </section>

      <aside v-if="selectedPlan" class="surface panel-pad purchase-summary">
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
          <p>{{ paymentStep === 'processing' ? '正在模拟扣款并写入订阅账户。' : paymentStep === 'done' ? '订阅额度已刷新，可以继续使用 Agent 能力。' : '管理员充值的钱包余额会用于本次模拟购买，不接入真实支付。' }}</p>
          <div class="checkout-ledger">
            <div><span>当前余额</span><strong>{{ formatMoney(billing.walletBalanceCents) }}</strong></div>
            <div><span>本次扣款</span><strong>{{ formatMoney(payableCents(pendingPlan)) }}</strong></div>
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
            <el-button type="primary" :loading="paymentStep === 'processing'" :disabled="pendingPurchaseDelta(pendingPlan) < 0 || paymentStep === 'done'" @click="confirmPurchase">
              {{ pendingPurchaseDelta(pendingPlan) < 0 ? '余额不足' : '确认扣款并购买' }}
            </el-button>
          </div>
        </div>
      </el-dialog>
    </template>
  </div>
</template>
