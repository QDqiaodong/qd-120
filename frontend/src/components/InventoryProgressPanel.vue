<template>
  <el-card class="inventory-progress-panel">
    <template #header>
      <div class="card-header">
        <div class="header-left">
          <el-icon class="header-icon"><DataAnalysis /></el-icon>
          <span class="card-title">月度清点进度</span>
          <el-tag type="info" size="small">{{ progressData?.inventoryMonth || '-' }}</el-tag>
        </div>
        <div class="header-right">
          <el-tag :type="statusTagType" size="small">{{ statusText }}</el-tag>
          <el-button type="primary" link size="small" @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>
    </template>

    <div class="progress-summary" v-loading="loading">
      <div class="summary-item total">
        <div class="summary-icon">
          <el-icon><Collection /></el-icon>
        </div>
        <div class="summary-content">
          <div class="summary-value">{{ progressData?.totalCount || 0 }}</div>
          <div class="summary-label">总数量</div>
        </div>
      </div>

      <div class="summary-item completion">
        <div class="completion-ring">
          <svg viewBox="0 0 100 100" class="ring-svg">
            <circle class="ring-bg" cx="50" cy="50" r="42" fill="none" stroke-width="8" />
            <circle
              class="ring-progress"
              cx="50"
              cy="50"
              r="42"
              fill="none"
              stroke-width="8"
              :stroke-dasharray="circumference"
              :stroke-dashoffset="progressOffset"
              stroke-linecap="round"
              :transform="'rotate(-90 50 50)'"
            />
            <text class="ring-text" x="50" y="50" text-anchor="middle" dominant-baseline="middle">
              {{ progressData?.completionRatio || 0 }}%
            </text>
          </svg>
        </div>
        <div class="completion-label">完成比例</div>
      </div>

      <div class="summary-item counted">
        <div class="summary-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="summary-content">
          <div class="summary-value success">{{ progressData?.countedCount || 0 }}</div>
          <div class="summary-label">已盘</div>
        </div>
      </div>

      <div class="summary-item discrepancy">
        <div class="summary-icon">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="summary-content">
          <div class="summary-value danger">{{ progressData?.discrepancyCount || 0 }}</div>
          <div class="summary-label">差异</div>
        </div>
      </div>

      <div class="summary-item unprocessed">
        <div class="summary-icon">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="summary-content">
          <div class="summary-value warning">{{ progressData?.unprocessedCount || 0 }}</div>
          <div class="summary-label">未处理</div>
        </div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="detail-tabs" v-if="progressData && progressData.inventoryStatus !== 'NOT_STARTED'">
      <el-tab-pane label="已盘" name="counted">
        <div class="tab-badge counted-badge">{{ progressData?.countedCount || 0 }}</div>
        <el-table :data="progressData?.countedList || []" border stripe size="small" max-height="300">
          <el-table-column prop="stopperNo" label="挡块编号" width="120" />
          <el-table-column prop="spec" label="规格型号" width="120" />
          <el-table-column prop="station" label="存放工位" width="130" />
          <el-table-column label="状态" width="80">
            <template #default>
              <el-tag type="success" size="small">正常</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!progressData?.countedList?.length" description="暂无已盘数据" :image-size="80" />
      </el-tab-pane>

      <el-tab-pane label="差异" name="discrepancy">
        <div class="tab-badge discrepancy-badge">{{ progressData?.discrepancyCount || 0 }}</div>
        <el-table :data="progressData?.discrepancyList || []" border stripe size="small" max-height="300">
          <el-table-column prop="stopperNo" label="挡块编号" width="120" />
          <el-table-column prop="spec" label="规格型号" width="120" />
          <el-table-column prop="station" label="存放工位" width="130" />
          <el-table-column label="状态" width="80">
            <template #default>
              <el-tag type="danger" size="small">差异</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="diffReason" label="差异原因" min-width="150" show-overflow-tooltip />
        </el-table>
        <el-empty v-if="!progressData?.discrepancyList?.length" description="暂无差异数据" :image-size="80" />
      </el-tab-pane>

      <el-tab-pane label="未处理" name="unprocessed">
        <div class="tab-badge unprocessed-badge">{{ progressData?.unprocessedCount || 0 }}</div>
        <el-table :data="progressData?.unprocessedList || []" border stripe size="small" max-height="300">
          <el-table-column prop="stopperNo" label="挡块编号" width="120" />
          <el-table-column prop="spec" label="规格型号" width="120" />
          <el-table-column prop="station" label="存放工位" width="130" />
          <el-table-column label="状态" width="80">
            <template #default>
              <el-tag type="info" size="small">未盘</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!progressData?.unprocessedList?.length" description="暂无未处理数据" :image-size="80" />
      </el-tab-pane>
    </el-tabs>

    <div class="empty-state" v-else-if="progressData?.inventoryStatus === 'NOT_STARTED'">
      <el-empty description="本月尚未发起清点，请先前往月度清点页面发起盘点" :image-size="100">
        <el-button type="primary" @click="goToInventory">
          <el-icon><Plus /></el-icon>
          发起盘点
        </el-button>
      </el-empty>
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getInventoryProgress } from '@/api/inventory'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Refresh, Collection, CircleCheck, Warning, Clock, Plus } from '@element-plus/icons-vue'

const props = defineProps({
  autoRefresh: {
    type: Boolean,
    default: true
  },
  refreshInterval: {
    type: Number,
    default: 30000
  }
})

const emit = defineEmits(['refresh'])

const router = useRouter()
const loading = ref(false)
const progressData = ref(null)
const activeTab = ref('counted')
let refreshTimer = null

const circumference = 2 * Math.PI * 42

const progressOffset = computed(() => {
  const ratio = progressData.value?.completionRatio || 0
  return circumference * (1 - ratio / 100)
})

const statusTagType = computed(() => {
  const status = progressData.value?.inventoryStatus
  if (status === 'COMPLETED') return 'success'
  if (status === 'PROCESSING') return 'warning'
  return 'info'
})

const statusText = computed(() => {
  const status = progressData.value?.inventoryStatus
  if (status === 'COMPLETED') return '已完成'
  if (status === 'PROCESSING') return '进行中'
  return '未开始'
})

const loadData = async () => {
  loading.value = true
  try {
    progressData.value = await getInventoryProgress()
    emit('refresh', progressData.value)
  } catch (error) {
    ElMessage.error('加载清点进度失败')
  } finally {
    loading.value = false
  }
}

const handleRefresh = () => {
  loadData()
}

const goToInventory = () => {
  router.push('/inventory')
}

const startAutoRefresh = () => {
  if (props.autoRefresh && props.refreshInterval > 0) {
    stopAutoRefresh()
    refreshTimer = setInterval(() => {
      loadData()
    }, props.refreshInterval)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => {
  loadData()
  startAutoRefresh()
})

watch(() => props.autoRefresh, (val) => {
  if (val) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
})

defineExpose({
  loadData,
  stopAutoRefresh
})
</script>

<style scoped>
.inventory-progress-panel {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  color: #409eff;
  font-size: 18px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-summary {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8f9fb;
  border-radius: 8px;
}

.summary-item.total .summary-icon {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.summary-item.counted .summary-icon {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.summary-item.discrepancy .summary-icon {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.summary-item.unprocessed .summary-icon {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  flex-shrink: 0;
}

.summary-content {
  flex: 1;
  min-width: 0;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.summary-value.success {
  color: #67c23a;
}

.summary-value.danger {
  color: #f56c6c;
}

.summary-value.warning {
  color: #e6a23c;
}

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.summary-item.completion {
  flex-direction: column;
  text-align: center;
  gap: 8px;
}

.completion-ring {
  position: relative;
  width: 80px;
  height: 80px;
}

.ring-svg {
  width: 100%;
  height: 100%;
}

.ring-bg {
  stroke: #e4e7ed;
}

.ring-progress {
  stroke: #409eff;
  transition: stroke-dashoffset 0.5s ease;
}

.ring-text {
  font-size: 16px;
  font-weight: 700;
  fill: #303133;
}

.completion-label {
  font-size: 13px;
  color: #909399;
}

.detail-tabs {
  margin-top: 20px;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.detail-tabs :deep(.el-tabs__item) {
  position: relative;
}

.tab-badge {
  position: absolute;
  top: 8px;
  right: -8px;
  min-width: 18px;
  height: 18px;
  line-height: 18px;
  text-align: center;
  border-radius: 9px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  padding: 0 5px;
}

.counted-badge {
  background: #67c23a;
}

.discrepancy-badge {
  background: #f56c6c;
}

.unprocessed-badge {
  background: #e6a23c;
}

.empty-state {
  margin-top: 20px;
}

@media (max-width: 1200px) {
  .progress-summary {
    grid-template-columns: repeat(3, 1fr);
  }
  
  .summary-item.completion {
    grid-column: span 3;
  }
}

@media (max-width: 768px) {
  .progress-summary {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .summary-item.completion {
    grid-column: span 2;
  }
}
</style>
