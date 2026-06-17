<template>
  <el-container style="min-height: 100vh">
    <el-header style="background: #1f2937; color: #fff; display: flex; align-items: center; padding: 0 20px">
      <h2 style="margin: 0">Smart Brew AI - 监控看板</h2>
      <el-menu
        mode="horizontal"
        :default-active="route.path"
        router
        style="margin-left: 40px; background: transparent; border-bottom: none"
        text-color="#9ca3af"
        active-text-color="#fff"
      >
        <el-menu-item index="/">实时监控</el-menu-item>
        <el-menu-item index="/history">历史曲线</el-menu-item>
        <el-menu-item index="/alarms">告警中心</el-menu-item>
        <el-menu-item index="/control">设备控制</el-menu-item>
        <el-menu-item index="/ai">AI 分析</el-menu-item>
        <el-menu-item index="/knowledge">知识库</el-menu-item>
      </el-menu>
    </el-header>
    <el-main style="background: #f3f4f6; padding: 20px">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>
