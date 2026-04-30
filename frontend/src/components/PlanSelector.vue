<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { http, type ApiResponse } from '../api/http'

interface PlanSummary {
  id: number
  directionId: number
  directionName: string
  title: string
  status: string
  createdAt: string
}

const props = defineProps<{
  modelValue: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number]
  change: [value: number]
}>()

const loading = ref(false)
const plans = ref<PlanSummary[]>([])
const selected = ref<number | null>(props.modelValue)

watch(
  () => props.modelValue,
  (value) => {
    selected.value = value
  },
)

watch(selected, (value) => {
  if (value) {
    emit('update:modelValue', value)
    emit('change', value)
  }
})

async function fetchPlans() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<PlanSummary[]>>('/plans')
    plans.value = response.data.data
    if (!selected.value && plans.value.length > 0) {
      selected.value = plans.value[0].id
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchPlans)
</script>

<template>
  <el-select
    v-model="selected"
    class="plan-selector"
    :loading="loading"
    placeholder="选择学习计划"
    value-key="id"
  >
    <el-option
      v-for="plan in plans"
      :key="plan.id"
      :label="`${plan.directionName} / ${plan.title}`"
      :value="plan.id"
    >
      <div class="plan-option">
        <strong>{{ plan.title }}</strong>
        <span>{{ plan.directionName }} · Plan #{{ plan.id }}</span>
      </div>
    </el-option>
  </el-select>
</template>
