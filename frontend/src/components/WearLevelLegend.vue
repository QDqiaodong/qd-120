<template>
  <div class="wear-level-legend">
    <div class="legend-title" v-if="showTitle">
      <el-icon><InfoFilled /></el-icon>
      <span>磨损程度说明</span>
    </div>
    <div class="legend-items">
      <div
        v-for="item in wearLevels"
        :key="item.value"
        class="legend-item"
        :class="{ 'highlight': highlight === item.value }"
      >
        <span class="level-color" :style="{ background: item.color }"></span>
        <span class="level-name">{{ item.label }}</span>
        <span class="level-desc" v-if="showDescription">{{ item.description }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { InfoFilled } from '@element-plus/icons-vue'

defineProps({
  showTitle: {
    type: Boolean,
    default: true
  },
  showDescription: {
    type: Boolean,
    default: true
  },
  highlight: {
    type: String,
    default: ''
  }
})

const wearLevels = [
  {
    value: '轻微',
    label: '轻微磨损',
    color: '#67c23a',
    description: '表面轻微划痕，不影响正常使用'
  },
  {
    value: '中度',
    label: '中度磨损',
    color: '#e6a23c',
    description: '磨损较明显，需关注使用情况'
  },
  {
    value: '严重',
    label: '严重磨损',
    color: '#f56c6c',
    description: '磨损严重，建议尽快更换'
  },
  {
    value: '断裂',
    label: '断裂损坏',
    color: '#d9001b',
    description: '已断裂或完全损坏，无法继续使用'
  },
  {
    value: '完全损坏',
    label: '完全损坏',
    color: '#b30000',
    description: '已完全损坏，无法继续使用'
  }
]

const getWearLevelColor = (level) => {
  const found = wearLevels.find(item => item.value === level)
  return found ? found.color : '#909399'
}

const getWearLevelTagType = (level) => {
  const colorMap = {
    '轻微': 'success',
    '中度': 'warning',
    '严重': 'danger',
    '断裂': 'danger',
    '完全损坏': 'danger'
  }
  return colorMap[level] || 'info'
}

defineExpose({
  getWearLevelColor,
  getWearLevelTagType,
  wearLevels
})
</script>

<style scoped>
.wear-level-legend {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 16px;
}

.legend-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.legend-title .el-icon {
  color: #409eff;
}

.legend-items {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  transition: all 0.2s ease;
}

.legend-item.highlight {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.level-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.level-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
}

.level-desc {
  font-size: 12px;
  color: #909399;
}
</style>
