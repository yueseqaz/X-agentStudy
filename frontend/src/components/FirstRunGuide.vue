<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ArrowLeft, ArrowRight, Close } from '@element-plus/icons-vue'

export interface GuideStep {
  title: string
  body: string
}

const props = defineProps<{
  guideKey: string
  title: string
  steps: GuideStep[]
}>()

const visible = ref(false)
const activeIndex = ref(0)
const storageKey = computed(() => `x-agent-study-guide-seen:${props.guideKey}`)
const activeStep = computed(() => props.steps[activeIndex.value])
const isLast = computed(() => activeIndex.value >= props.steps.length - 1)

watch(
  () => [props.guideKey, props.steps.length],
  () => {
    activeIndex.value = 0
    if (!props.guideKey || props.steps.length === 0) {
      visible.value = false
      return
    }
    visible.value = localStorage.getItem(storageKey.value) !== '1'
  },
  { immediate: true },
)

function closeGuide() {
  localStorage.setItem(storageKey.value, '1')
  visible.value = false
}

function nextStep() {
  if (isLast.value) {
    closeGuide()
    return
  }
  activeIndex.value += 1
}

function previousStep() {
  activeIndex.value = Math.max(0, activeIndex.value - 1)
}
</script>

<template>
  <teleport to="body">
    <div v-if="visible && activeStep" class="first-run-guide">
      <div class="first-run-guide-card">
        <button class="guide-close" type="button" @click="closeGuide">
          <el-icon><Close /></el-icon>
        </button>
        <span>{{ activeIndex + 1 }} / {{ steps.length }}</span>
        <h2>{{ title }}</h2>
        <h3>{{ activeStep.title }}</h3>
        <p>{{ activeStep.body }}</p>
        <div class="guide-progress">
          <i
            v-for="(_step, index) in steps"
            :key="index"
            :class="{ active: index === activeIndex }"
          />
        </div>
        <div class="guide-actions">
          <el-button :icon="ArrowLeft" :disabled="activeIndex === 0" @click="previousStep">上一步</el-button>
          <el-button text @click="closeGuide">不再提示</el-button>
          <el-button type="primary" :icon="ArrowRight" @click="nextStep">
            {{ isLast ? '知道了' : '下一步' }}
          </el-button>
        </div>
      </div>
    </div>
  </teleport>
</template>
