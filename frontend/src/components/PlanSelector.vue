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
  allowEmpty?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
  change: [value: number | null]
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
  if (value || props.allowEmpty) {
    emit('update:modelValue', value)
    emit('change', value)
  }
})

async function fetchPlans() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<PlanSummary[]>>('/plans')
    plans.value = response.data.data
    if (!props.allowEmpty && !selected.value && plans.value.length > 0) {
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
    <el-option v-if="allowEmpty" label="不关联计划" :value="null" />
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
