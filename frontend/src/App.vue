<template>
  <el-container class="app-container">
    <el-header class="app-header">
      <div class="header-left">
        <el-icon class="logo-icon"><Tools /></el-icon>
        <h1 class="app-title">限位挡块资产登记系统</h1>
      </div>
      <div class="header-right">
        <span class="workshop">自动化组装车间</span>
      </div>
    </el-header>
    
    <el-container class="main-container">
      <el-aside width="220px" class="app-aside">
        <el-menu
          :default-active="activeMenu"
          router
          class="side-menu"
          background-color="#001529"
          text-color="#fff"
          active-text-color="#409eff"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Grid /></el-icon>
            <span>工位总览</span>
          </el-menu-item>
          <el-menu-item index="/stopper">
            <el-icon><Document /></el-icon>
            <span>挡块档案</span>
          </el-menu-item>
          <el-menu-item index="/device-stopper">
            <el-icon><Cpu /></el-icon>
            <span>适配设备视图</span>
          </el-menu-item>
          <el-menu-item index="/shift">
            <el-icon><Switch /></el-icon>
            <span>移位登记</span>
          </el-menu-item>
          <el-menu-item index="/inventory">
            <el-icon><Tickets /></el-icon>
            <span>月度清点</span>
          </el-menu-item>
          <el-menu-item index="/scrap">
            <el-icon><Delete /></el-icon>
            <span>报废归档</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => {
  if (route.path.startsWith('/inventory/detail')) {
    return '/inventory'
  }
  return route.path
})
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.app-header {
  background: linear-gradient(90deg, #001529 0%, #003a70 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  font-size: 28px;
  color: #409eff;
}

.app-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  letter-spacing: 1px;
}

.header-right .workshop {
  font-size: 14px;
  color: #a0cfff;
}

.main-container {
  height: calc(100vh - 60px);
}

.app-aside {
  background: #001529;
}

.side-menu {
  height: 100%;
  border-right: none;
}

.side-menu .el-menu-item {
  height: 50px;
  line-height: 50px;
}

.app-main {
  background: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
