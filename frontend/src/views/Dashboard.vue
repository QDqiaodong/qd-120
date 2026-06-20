<template>
  <div class="dashboard">
    <div class="stats-cards">
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
          <div class="stat-icon normal">
            <el-icon><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ normalCount }}</div>
            <div class="stat-label">正常使用</div>
          </div>
        </div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon scrap">
            <el-icon><CircleClose /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ scrapCount }}</div>
            <div class="stat-label">已报废</div>
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
            <div class="stat-label">工位数</div>
          </div>
        </div>
      </el-card>
    </div>

    <InventoryProgressPanel />

    <el-card class="station-view-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">设备工位分组视图</span>
          <el-tag type="info" size="small">按工位分组展示所有挡块</el-tag>
        </div>
      </template>
      
      <div class="station-groups" v-loading="loading">
        <div v-for="(stoppers, station) in stationGroups" :key="station" class="station-group">
          <div class="station-header">
            <el-icon class="station-icon"><Location /></el-icon>
            <span class="station-name">{{ station }}</span>
            <el-tag type="primary" effect="light" size="small">{{ stoppers.length }} 个</el-tag>
          </div>
          <div class="stopper-grid">
            <div
              v-for="stopper in stoppers"
              :key="stopper.id"
              class="stopper-card"
              @click="viewStopper(stopper)"
            >
              <div class="stopper-img-wrapper">
                <img
                  v-if="stopper.imageUrl"
                  :src="stopper.imageUrl"
                  :alt="stopper.stopperNo"
                  class="stopper-image"
                />
                <div v-else class="stopper-image-placeholder">
                  <el-icon :size="32"><Tools /></el-icon>
                </div>
              </div>
              <div class="stopper-info">
                <div class="stopper-no">{{ stopper.stopperNo }}</div>
                <div class="stopper-spec">{{ stopper.spec }}</div>
                <div class="stopper-equip">{{ stopper.adaptEquipment }}</div>
              </div>
              <div class="stopper-status">
                <el-tag :type="stopper.status === 1 ? 'success' : 'danger'" size="small">
                  {{ stopper.status === 1 ? '正常' : '报废' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <el-empty v-if="!loading && Object.keys(stationGroups).length === 0" description="暂无挡块数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStopperList, getStopperGroupByStation } from '@/api/stopper'
import { ElMessage } from 'element-plus'
import InventoryProgressPanel from '@/components/InventoryProgressPanel.vue'

const router = useRouter()

const loading = ref(false)
const stopperList = ref([])
const stationGroups = ref({})

const totalCount = computed(() => stopperList.value.length)
const normalCount = computed(() => stopperList.value.filter(s => s.status === 1).length)
const scrapCount = computed(() => stopperList.value.filter(s => s.status === 2).length)
const stationCount = computed(() => Object.keys(stationGroups.value).length)

const loadData = async () => {
  loading.value = true
  try {
    const [list, groups] = await Promise.all([
      getStopperList(),
      getStopperGroupByStation()
    ])
    stopperList.value = list || []
    stationGroups.value = groups || {}
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const viewStopper = (stopper) => {
  router.push(`/stopper/detail/${stopper.id}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
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

.stat-icon.total {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.stat-icon.normal {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.stat-icon.scrap {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.stat-icon.station {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
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

.station-view-card {
  border-radius: 8px;
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

.station-groups {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.station-group {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 16px;
}

.station-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.station-icon {
  color: #409eff;
  font-size: 18px;
}

.station-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.stopper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.stopper-card {
  background: #fff;
  border-radius: 6px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stopper-card:hover {
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);
  border-color: #409eff;
  transform: translateY(-2px);
}

.stopper-img-wrapper {
  width: 100%;
  height: 80px;
  overflow: hidden;
  border-radius: 4px;
}

.stopper-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #f0f0f0;
}

.stopper-image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4ff 0%, #f0f0f0 100%);
  color: #909399;
}

.stopper-info {
  flex: 1;
  min-width: 0;
}

.stopper-no {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stopper-spec {
  font-size: 12px;
  color: #606266;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stopper-equip {
  font-size: 11px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stopper-status {
  text-align: right;
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
  
  .stopper-grid {
    grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  }
}
</style>
