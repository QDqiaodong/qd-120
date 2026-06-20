<template>
  <div class="device-stopper-view">
    <div class="stats-cards">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon equipment">
            <el-icon><Cpu /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ equipmentCount }}</div>
            <div class="stat-label">适配设备数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon total">
            <el-icon><Tools /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalCount }}</div>
            <div class="stat-label">挡块总数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon station">
            <el-icon><Location /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stationCount }}</div>
            <div class="stat-label">涉及工位数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon shift">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ noShiftCount }}</div>
            <div class="stat-label">未移位挡块</div>
          </div>
        </div>
      </el-card>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="关键字">
          <el-input
            v-model="filterForm.keyword"
            placeholder="设备名称/挡块编号/规格/工位"
            clearable
            @keyup.enter="handleFilter"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">
            <el-icon><Search /></el-icon>筛选
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="equipment-view-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">适配设备视图</span>
          <el-tag type="info" size="small">按自动化组装设备聚合挡块</el-tag>
        </div>
      </template>

      <div class="equipment-groups" v-loading="loading">
        <div
          v-for="(stoppers, equipment) in filteredEquipmentGroups"
          :key="equipment"
          class="equipment-group"
        >
          <div class="equipment-header">
            <el-icon class="equipment-icon"><Cpu /></el-icon>
            <span class="equipment-name">{{ equipment }}</span>
            <el-tag type="primary" effect="light" size="small">{{ stoppers.length }} 个挡块</el-tag>
          </div>

          <el-table :data="stoppers" border stripe size="small" class="stopper-table">
            <el-table-column prop="stopperNo" label="挡块编号" width="120" />
            <el-table-column prop="spec" label="规格型号" width="150" />
            <el-table-column prop="station" label="存放工位" width="140">
              <template #default="{ row }">
                <el-tag type="success" effect="plain" size="small">{{ row.station }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastShiftTime" label="最新移位时间" min-width="170">
              <template #default="{ row }">
                <span v-if="row.lastShiftTime" class="shift-time">
                  {{ formatDate(row.lastShiftTime) }}
                </span>
                <el-tag v-else type="info" effect="plain" size="small">暂无移位记录</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-empty
          v-if="!loading && Object.keys(filteredEquipmentGroups).length === 0"
          description="暂无匹配数据"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getStopperGroupByEquipment } from '@/api/stopper'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const equipmentGroups = ref({})

const filterForm = reactive({
  keyword: ''
})

const equipmentCount = computed(() => Object.keys(equipmentGroups.value).length)

const totalCount = computed(() => {
  return Object.values(equipmentGroups.value).reduce((sum, list) => sum + list.length, 0)
})

const stationCount = computed(() => {
  const stations = new Set()
  Object.values(equipmentGroups.value).forEach(list => {
    list.forEach(s => {
      if (s.station) stations.add(s.station)
    })
  })
  return stations.size
})

const noShiftCount = computed(() => {
  let count = 0
  Object.values(equipmentGroups.value).forEach(list => {
    list.forEach(s => {
      if (!s.lastShiftTime) count++
    })
  })
  return count
})

const filteredEquipmentGroups = computed(() => {
  const keyword = filterForm.keyword.trim().toLowerCase()
  if (!keyword) {
    return equipmentGroups.value
  }

  const result = {}
  Object.entries(equipmentGroups.value).forEach(([equipment, stoppers]) => {
    if (equipment.toLowerCase().includes(keyword)) {
      result[equipment] = stoppers
    } else {
      const filtered = stoppers.filter(s =>
        (s.stopperNo && s.stopperNo.toLowerCase().includes(keyword)) ||
        (s.spec && s.spec.toLowerCase().includes(keyword)) ||
        (s.station && s.station.toLowerCase().includes(keyword))
      )
      if (filtered.length > 0) {
        result[equipment] = filtered
      }
    }
  })
  return result
})

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await getStopperGroupByEquipment()
    equipmentGroups.value = data || {}
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
}

const handleReset = () => {
  filterForm.keyword = ''
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.device-stopper-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  border-radius: 8px;
  overflow: hidden;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.stat-icon.equipment {
  background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);
}

.stat-icon.total {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.stat-icon.station {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.stat-icon.shift {
  background: linear-gradient(135deg, #13c2c2 0%, #36cfc9 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.filter-card,
.equipment-view-card {
  border-radius: 8px;
}

.filter-form {
  margin: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.equipment-groups {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.equipment-group {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 16px;
}

.equipment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.equipment-icon {
  color: #722ed1;
  font-size: 18px;
}

.equipment-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.stopper-table {
  background: #fff;
  border-radius: 6px;
}

.shift-time {
  font-size: 13px;
  color: #606266;
}

@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
