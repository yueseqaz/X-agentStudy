<script setup lang="ts">
defineProps<{
  title: string
  module: string
}>()
</script>

<template>
  <section class="surface panel-pad">
    <div class="section-head">
      <div>
        <h2>{{ title }}</h2>
        <p>{{ module }}</p>
      </div>
      <el-tag type="warning">待实现</el-tag>
    </div>
    <el-alert
      title="该模块已进入完整 SaaS 范围，当前先保留路由、导航和页面占位。"
      type="info"
      :closable="false"
      show-icon
    />
  </section>
</template>
