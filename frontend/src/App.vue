<script setup lang="ts">
import {
  ArrowLeft,
  Fold,
  Expand,
  Bell,
  Calendar,
  Collection,
  Files,
  Histogram,
  Reading,
  Setting,
  Moon,
  Sunny,
  Trophy,
  UserFilled,
} from '@element-plus/icons-vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FirstRunGuide, { type GuideStep } from './components/FirstRunGuide.vue'
import { useAuthStore } from './stores/auth'
import { cleanupElementPlusOverlays } from './utils/overlay'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const publicPage = computed(() => route.meta.public)
const sidebarCollapsed = ref(localStorage.getItem('x-agent-study-sidebar-collapsed') === '1')
const theme = ref(localStorage.getItem('x-agent-study-theme') || 'light')
document.documentElement.dataset.theme = theme.value
const canGoBack = computed(() => route.path !== '/')
const routeGuide = computed(() => {
  const path = route.path
  if (path === '/') {
    return guide('dashboard', '工作台使用引导', [
      ['查看学习概况', '这里汇总你的学习计划、知识库资料、题库和待复习数量。'],
      ['继续学习', '最近的学习计划会显示在中间区域，可以直接进入计划、知识库或工作流。'],
      ['关注额度', '右侧会显示当前账号的 Agent 调用额度，生成计划、文档和题目都会消耗额度。'],
    ])
  }
  if (path === '/directions') {
    return guide('directions', '学习方向使用引导', [
      ['先创建方向', '方向名称尽量具体，例如「Vue 前端」或「Java 后端」，后续推荐资源会用它做匹配。'],
      ['填写技能分类', '技能分类建议写成「前端开发」「后端开发」「数学基础」这类范围，方便匹配资源学科范畴。'],
      ['继续画像', '创建方向后会进入画像问答，系统会根据目标、基础和周期生成学习计划。'],
    ])
  }
  if (path === '/calendar') {
    return guide('calendar', '每日打卡使用引导', [
      ['记录当天学习', '在这里记录每天完成的任务、学习感受和进展。'],
      ['查看连续情况', '日历会帮助你看到学习节奏，避免计划长期中断。'],
      ['结合计划复盘', '打卡内容会和计划进度一起形成学习回顾。'],
    ])
  }
  if (path === '/outcomes') {
    return guide('outcomes', '学习成果使用引导', [
      ['切换统计周期', '可以查看最近 7 天、30 天、90 天、全部或自定义范围。'],
      ['看跨计划成果', '这里聚合的是全部学习计划，不是某个计划的局部报告。'],
      ['生成成果海报', '当前统计范围可以直接生成一张成果展示图。'],
    ])
  }
  if (path === '/resources') {
    return guide('resources', '资源库使用引导', [
      ['用筛选快速定位', '可以按类型、学科名、学科范畴和标签筛选，也可以搜索标题和简介。'],
      ['理解推荐规则', '资源学科名匹配方向名称，学科范畴匹配技能分类，标签用于补充命中。'],
    ])
  }
  if (path === '/admin') {
    return guide('admin', '管理后台使用引导', [
      ['概览运营状态', '概览页查看用户、学习资产、题库、任务和额度情况。'],
      ['维护学习资源', '学习资源 Tab 用于上传视频、音频、图片和文档，并填写匹配标签。'],
      ['处理平台配置', '模型网关、任务、额度、用户安全和审计日志都在这里维护。'],
    ])
  }
  if (path === '/account') {
    return guide('account', '个人中心使用引导', [
      ['管理账号资料', '可以查看账号状态、头像和安全信息。'],
      ['查看订阅额度', '订阅页展示当前套餐、额度使用和升级入口。'],
      ['处理安全操作', '密码重置、邮箱验证和登录状态都集中在这里。'],
    ])
  }
  if (path.includes('/workflow')) {
    return guide('workflow', '工作流使用引导', [
      ['按节点看学习链路', '工作流把目标、阶段、知识库、问答、测验、复习和报告连成一条路径。'],
      ['点击节点跳转', '每个节点都可以进入对应功能。'],
      ['看下一步建议', '页面会根据当前资料、题目和复习状态给出下一步建议。'],
    ])
  }
  return guide('', '', [])
})

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('x-agent-study-sidebar-collapsed', sidebarCollapsed.value ? '1' : '0')
}

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
}

function guide(key: string, title: string, items: [string, string][]) {
  return {
    key,
    title,
    steps: items.map(([stepTitle, body]) => ({ title: stepTitle, body })) as GuideStep[],
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/')
}

onMounted(() => {
  document.documentElement.dataset.theme = theme.value
  auth.fetchMe()
  cleanupElementPlusOverlays()
})

watch(theme, (value) => {
  document.documentElement.dataset.theme = value
  localStorage.setItem('x-agent-study-theme', value)
})

watch(
  () => route.fullPath,
  () => cleanupElementPlusOverlays(),
)
</script>

<template>
  <router-view v-if="publicPage" />
  <el-container v-else class="app-shell">
    <el-aside :width="sidebarCollapsed ? '84px' : '252px'" class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="brand">
        <div class="brand-mark">XA</div>
        <div v-if="!sidebarCollapsed">
          <strong>X-AgentStudy</strong>
          <span>Agent learning OS</span>
        </div>
        <el-button
          class="sidebar-toggle"
          circle
          :icon="sidebarCollapsed ? Expand : Fold"
          @click="toggleSidebar"
        />
      </div>

      <el-menu
        router
        :default-active="route.path"
        class="nav-menu"
        :collapse="sidebarCollapsed"
        :collapse-transition="false"
      >
        <el-menu-item index="/">
          <el-icon><Histogram /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/directions">
          <el-icon><Collection /></el-icon>
          <span>学习方向</span>
        </el-menu-item>
        <el-menu-item index="/calendar">
          <el-icon><Calendar /></el-icon>
          <span>每日打卡</span>
        </el-menu-item>
        <el-menu-item index="/outcomes">
          <el-icon><Trophy /></el-icon>
          <span>学习成果</span>
        </el-menu-item>
        <el-menu-item index="/resources">
          <el-icon><Files /></el-icon>
          <span>资源库</span>
        </el-menu-item>
        <el-menu-item v-if="auth.ready && auth.isAdmin" index="/admin">
          <el-icon><Setting /></el-icon>
          <span>管理后台</span>
        </el-menu-item>
        <el-menu-item index="/account">
          <el-icon><UserFilled /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <el-button
            v-if="canGoBack"
            class="topbar-back"
            :icon="ArrowLeft"
            text
            @click="goBack"
          />
          <h1>多 Agent 协作技能学习平台</h1>
        </div>
        <div class="topbar-actions">
          <el-tag v-if="auth.user" type="success">
            {{ auth.user.nickname }} · {{ auth.isAdmin ? 'ADMIN' : auth.user.role }}
          </el-tag>
          <el-button
            class="theme-toggle"
            :icon="theme === 'dark' ? Sunny : Moon"
            circle
            :title="theme === 'dark' ? '切换浅色' : '切换深色'"
            @click="toggleTheme"
          />
          <el-button :icon="Bell" circle />
          <el-button :icon="Reading" type="primary" @click="router.push('/directions')">创建学习方向</el-button>
          <el-button @click="router.push({ path: '/account', query: { tab: 'billing' } })">订阅</el-button>
          <el-button @click="router.push('/account')">个人中心</el-button>
          <el-button @click="auth.clearSession(); router.push('/login')">退出</el-button>
        </div>
      </el-header>
      <el-main class="main-panel">
        <router-view />
      </el-main>
    </el-container>
    <FirstRunGuide
      :guide-key="routeGuide.key"
      :title="routeGuide.title"
      :steps="routeGuide.steps"
    />
  </el-container>
</template>
