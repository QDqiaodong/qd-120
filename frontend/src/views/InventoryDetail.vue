<template>
  <div class="inventory-detail">
    <el-card class="header-card">
      <div class="header-content">
        <div class="header-left">
          <el-button type="primary" text @click="goBack">
            <el-icon><ArrowLeft /></el-icon>返回
          </el-button>
          <h2 class="page-title">盘点明细 - {{ inventoryInfo?.inventoryNo }}</h2>
        </div>
        <div class="header-right">
          <el-tag :type="inventoryInfo?.inventoryStatus === 'COMPLETED' ? 'success' : 'warning'" size="large">
            {{ inventoryInfo?.inventoryStatus === 'COMPLETED' ? '已完成' : '进行中' }}
          </el-tag>
        </div>
      </div>
      
      <div class="summary-stats">
        <div class="stat-item">
          <span class="stat-label">账面数量</span>
          <span class="stat-value">{{ inventoryInfo?.totalCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">实盘数量</span>
          <span class="stat-value success">{{ inventoryInfo?.actualCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">差异数量</span>
          <span class="stat-value danger">{{ inventoryInfo?.diffCount || 0 }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">未盘数量</span>
          <span class="stat-value warning">{{ pendingCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">盘点月份</span>
          <span class="stat-value">{{ inventoryInfo?.inventoryMonth || '-' }}</span>
        </div>
      </div>
    </el-card>

    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <span>挡块盘点明细</span>
          <div class="header-actions" v-if="inventoryInfo?.inventoryStatus !== 'COMPLETED'">
            <el-button type="success" @click="handleComplete">
              <el-icon><Check /></el-icon>完成盘点
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="detailList" v-loading="loading" border stripe>
        <el-table-column prop="stopperNo" label="挡块编号" width="130" />
        <el-table-column prop="spec" label="规格型号" width="130" />
        <el-table-column prop="station" label="存放工位" width="130" />
        <el-table-column label="盘点状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.inventoryStatus === 1" type="success" size="small">已盘</el-tag>
            <el-tag v-else-if="row.inventoryStatus === 2" type="danger" size="small">差异</el-tag>
            <el-tag v-else type="info" size="small">未盘</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diffReason" label="差异原因" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.diffReason || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" v-if="inventoryInfo?.inventoryStatus !== 'COMPLETED'">
          <template #default="{ row }">
            <el-button type="success" size="small" link @click="markNormal(row)">
              <el-icon><CircleCheck /></el-icon>正常
            </el-button>
            <el-button type="danger" size="small" link @click="openDiffDialog(row)">
              <el-icon><Warning /></el-icon>差异
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="diffDialogVisible" title="标记差异" width="450px" @close="resetDiffForm">
      <el-form :model="diffForm" label-width="100px">
        <el-form-item label="挡块编号">
          <el-input :value="diffForm.stopperNo" disabled />
        </el-form-item>
        <el-form-item label="差异原因">
          <el-input v-model="diffForm.diffReason" type="textarea" :rows="3" placeholder="请输入差异原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="diffDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDiff">确认标记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getInventoryById, getInventoryDetail, markInventoryItem, completeInventory } from '@/api/inventory'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const inventoryInfo = ref(null)
const detailList = ref([])

const pendingCount = computed(() => {
  return detailList.value.filter((item) => {
    const status = item.inventoryStatus
    return status === undefined || status === null || status === 0 || (status !== 1 && status !== 2)
  }).length
})

const diffDialogVisible = ref(false)
const diffForm = reactive({
  detailId: null,
  stopperNo: '',
  diffReason: ''
})

const loadData = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const details = await getInventoryDetail(id)
    detailList.value = details
    const info = await getInventoryById(id)
    inventoryInfo.value = info
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/inventory')
}

const markNormal = async (row) => {
  try {
    await markInventoryItem({
      detailId: row.id,
      status: 1,
      diffReason: ''
    })
    ElMessage.success('标记成功')
    loadData()
  } catch (error) {
    ElMessage.error('标记失败')
  }
}

const openDiffDialog = (row) => {
  diffForm.detailId = row.id
  diffForm.stopperNo = row.stopperNo
  diffForm.diffReason = row.diffReason || ''
  diffDialogVisible.value = true
}

const resetDiffForm = () => {
  diffForm.detailId = null
  diffForm.stopperNo = ''
  diffForm.diffReason = ''
}

const submitDiff = async () => {
  try {
    await markInventoryItem({
      detailId: diffForm.detailId,
      status: 2,
      diffReason: diffForm.diffReason
    })
    ElMessage.success('标记成功')
    diffDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('标记失败')
  }
}

const handleComplete = async () => {
  if (pendingCount.value > 0) {
    ElMessageBox.confirm(
      `还有 ${pendingCount.value} 项挡块未盘点，请先全部处理后再完成盘点。`,
      '存在未盘明细',
      {
        confirmButtonText: '我知道了',
        showCancelButton: false,
        type: 'warning'
      }
    )
    return
  }
  try {
    const { value } = await ElMessageBox.prompt('请输入盘点备注', '完成盘点', {
      confirmButtonText: '确认完成',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入盘点备注（可选）',
      inputType: 'textarea'
    })
    await completeInventory({
      inventoryId: route.params.id,
      remark: value || ''
    })
    ElMessage.success('盘点完成')
    await loadData()
    await nextTick()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error?.message || '操作失败')
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.inventory-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card,
.detail-card {
  border-radius: 8px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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

.summary-stats {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 20px;
  padding: 20px 0 0 0;
  border-top: 1px solid #ebeef5;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-value.warning {
  color: #e6a23c;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
