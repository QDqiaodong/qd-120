<template>
  <div class="stopper-detail">
    <el-card class="header-card">
      <div class="header-content">
        <div class="header-left">
          <el-button type="primary" text @click="goBack">
            <el-icon><ArrowLeft /></el-icon>返回
          </el-button>
          <h2 class="page-title">挡块详情 - {{ stopperInfo?.stopperNo || '-' }}</h2>
        </div>
        <div class="header-right">
          <el-tag :type="stopperInfo?.status === 1 ? 'success' : 'danger'" size="large">
            {{ stopperInfo?.status === 1 ? '正常' : '报废' }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <el-card class="info-card" v-loading="loading">
      <template #header>
        <span class="card-title">基本信息</span>
      </template>
      <div class="info-content">
        <div class="info-image">
          <img
            v-if="stopperInfo?.imageUrl"
            :src="stopperInfo.imageUrl"
            :alt="stopperInfo.stopperNo"
            class="stopper-image"
          />
          <div v-else class="image-placeholder">
            <el-icon :size="64"><Tools /></el-icon>
            <span>暂无图片</span>
          </div>
        </div>
        <el-descriptions :column="2" border class="info-desc">
          <el-descriptions-item label="挡块编号">{{ stopperInfo?.stopperNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="规格型号">{{ stopperInfo?.spec || '-' }}</el-descriptions-item>
          <el-descriptions-item label="适配设备">{{ stopperInfo?.adaptEquipment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="存放工位">{{ stopperInfo?.station || '-' }}</el-descriptions-item>
          <el-descriptions-item label="入库时间">{{ formatDate(stopperInfo?.storageTime) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="stopperInfo?.status === 1 ? 'success' : 'danger'" size="small">
              {{ stopperInfo?.status === 1 ? '正常使用' : '已报废' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ stopperInfo?.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <WearLevelLegend :highlight="latestScrap?.scrapDegree" />

    <el-card class="scrap-history-card" v-if="scrapHistory.length > 0">
      <template #header>
        <div class="card-header">
          <span class="card-title">报废历史</span>
          <el-tag type="danger" size="small">{{ scrapHistory.length }} 条记录</el-tag>
        </div>
      </template>
      <el-table :data="scrapHistory" border stripe>
        <el-table-column prop="scrapDegree" label="磨损程度" width="130">
          <template #default="{ row }">
            <el-tag
              :type="wearLegendRef?.getWearLevelTagType(row.scrapDegree) || 'info'"
              size="small"
              effect="light"
            >
              <span
                class="degree-dot"
                :style="{ background: wearLegendRef?.getWearLevelColor(row.scrapDegree) || '#909399' }"
              ></span>
              {{ row.scrapDegree }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scrapReason" label="报废原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="scrapTime" label="报废时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.scrapTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card class="no-scrap-card" v-else-if="!loading && stopperInfo?.status === 1">
      <el-empty description="该挡块暂无报废记录，使用状态良好" :image-size="80">
        <el-icon class="empty-icon"><CircleCheck /></el-icon>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getStopperById } from '@/api/stopper'
import { getScrapByStopperId } from '@/api/scrap'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Tools, CircleCheck } from '@element-plus/icons-vue'
import WearLevelLegend from '@/components/WearLevelLegend.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const stopperInfo = ref(null)
const scrapHistory = ref([])
const wearLegendRef = ref(null)

const latestScrap = computed(() => {
  return scrapHistory.value.length > 0 ? scrapHistory.value[0] : null
})

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const loadData = async () => {
  const id = route.params.id
  if (!id) {
    ElMessage.error('参数错误')
    goBack()
    return
  }

  loading.value = true
  try {
    const [stopper, scraps] = await Promise.all([
      getStopperById(id),
      getScrapByStopperId(id)
    ])
    stopperInfo.value = stopper
    scrapHistory.value = scraps || []
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/stopper')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.stopper-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card,
.info-card,
.scrap-history-card,
.no-scrap-card {
  border-radius: 8px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-content {
  display: flex;
  gap: 30px;
}

.info-image {
  width: 200px;
  flex-shrink: 0;
}

.stopper-image {
  width: 100%;
  height: 200px;
  object-fit: contain;
  background: #f5f7fa;
  border-radius: 8px;
}

.image-placeholder {
  width: 100%;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: linear-gradient(135deg, #e8f4ff 0%, #f0f0f0 100%);
  border-radius: 8px;
  color: #909399;
}

.info-desc {
  flex: 1;
}

.degree-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.empty-icon {
  font-size: 48px;
  color: #67c23a;
}

.no-scrap-card :deep(.el-empty__description) {
  color: #67c23a;
}

@media (max-width: 768px) {
  .info-content {
    flex-direction: column;
  }
  
  .info-image {
    width: 100%;
  }
  
  .stopper-image,
  .image-placeholder {
    height: 150px;
  }
}
</style>
