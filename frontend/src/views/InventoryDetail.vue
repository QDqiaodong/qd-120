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
          <span class="stat-label">待处理数量</span>
          <span class="stat-value danger">{{ unprocessedCount }}</span>
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
          <div class="header-actions">
            <el-tag v-if="freezeTime" type="info" size="large">
              <el-icon><Clock /></el-icon>
              冻结基准时间: {{ formatDate(freezeTime) }}
            </el-tag>
            <el-switch
              v-model="showFreezeSnapshot"
              active-text="对比冻结快照"
              inactive-text="仅显示当前"
              size="large"
            />
            <el-switch
              v-model="showOnlyUnprocessed"
              active-text="只看未处理差异"
              inactive-text="显示全部"
              size="large"
            />
            <el-button
              v-if="inventoryInfo?.inventoryStatus !== 'COMPLETED'"
              type="success"
              @click="handleComplete"
            >
              <el-icon><Check /></el-icon>完成盘点
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredList" v-loading="loading" border stripe>
        <el-table-column prop="stopperNo" label="挡块编号" width="130">
          <template #default="{ row }">
            <span v-if="row.stopperNoChanged && showFreezeSnapshot" class="changed-value" :title="'冻结值: ' + row.freezeStopperNo">
              {{ row.stopperNo }}
              <el-icon class="change-icon"><WarningFilled /></el-icon>
            </span>
            <span v-else>{{ row.stopperNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="spec" label="规格型号" width="130">
          <template #default="{ row }">
            <span v-if="row.specChanged && showFreezeSnapshot" class="changed-value" :title="'冻结值: ' + row.freezeSpec">
              {{ row.spec }}
              <el-icon class="change-icon"><WarningFilled /></el-icon>
            </span>
            <span v-else>{{ row.spec }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showFreezeSnapshot" prop="freezeSpec" label="冻结规格" width="130">
          <template #default="{ row }">
            <span class="freeze-value">{{ row.freezeSpec }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="station" label="当前工位" width="130">
          <template #default="{ row }">
            <span v-if="row.stationChanged && showFreezeSnapshot" class="changed-value" :title="'冻结值: ' + row.freezeStation">
              {{ row.station }}
              <el-icon class="change-icon"><WarningFilled /></el-icon>
            </span>
            <span v-else>{{ row.station }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="showFreezeSnapshot" prop="freezeStation" label="冻结工位" width="130">
          <template #default="{ row }">
            <span class="freeze-value">{{ row.freezeStation }}</span>
          </template>
        </el-table-column>
        <el-table-column label="盘点状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.inventoryStatus === 1" type="success" size="small">已盘</el-tag>
            <el-tag v-else-if="row.inventoryStatus === 2" type="danger" size="small">差异</el-tag>
            <el-tag v-else type="info" size="small">未盘</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="差异原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.diffReasonCode">
              {{ getDiffReasonLabel(row.diffReasonCode) }}
              <span v-if="row.diffReason" class="diff-reason-sub">（{{ row.diffReason }}）</span>
            </span>
            <span v-else>-</span>
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
        <el-form-item label="标准原因">
          <el-select v-model="diffForm.diffReasonCode" placeholder="请选择标准原因" style="width: 100%">
            <el-option
              v-for="item in diffReasonOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="diffForm.diffReason" type="textarea" :rows="3" placeholder="请输入补充说明（可选）" />
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
import { getInventoryById, getInventoryDetail, getInventoryFreeze, markInventoryItem, completeInventory } from '@/api/inventory'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const inventoryInfo = ref(null)
const detailList = ref([])
const freezeList = ref([])
const freezeTime = ref(null)
const showOnlyUnprocessed = ref(false)
const showFreezeSnapshot = ref(false)

const pendingCount = computed(() => {
  return detailList.value.filter((item) => {
    const status = item.inventoryStatus
    return status === undefined || status === null || status === 0 || (status !== 1 && status !== 2)
  }).length
})

const unprocessedCount = computed(() => {
  return detailList.value.filter((item) => {
    const status = item.inventoryStatus
    const reviewStatus = item.reviewStatus
    const diffReasonCode = item.diffReasonCode

    const isUncounted = status === undefined || status === null || status === 0 || (status !== 1 && status !== 2)

    const isDiscrepancy = status === 2
    const needsReview = isDiscrepancy && (diffReasonCode === 'MISSING' || diffReasonCode === 'MISPLACED')
    const isUnreviewed = reviewStatus === undefined || reviewStatus === null || reviewStatus !== 1

    return isUncounted || (isDiscrepancy && needsReview && isUnreviewed)
  }).length
})

const filteredList = computed(() => {
  const sourceList = showFreezeSnapshot.value ? combinedList.value : detailList.value
  if (!showOnlyUnprocessed.value) {
    return sourceList
  }
  return sourceList.filter((item) => {
    const status = item.inventoryStatus
    const reviewStatus = item.reviewStatus
    const diffReasonCode = item.diffReasonCode

    const isUncounted = status === undefined || status === null || status === 0 || (status !== 1 && status !== 2)

    const isDiscrepancy = status === 2
    const needsReview = isDiscrepancy && (diffReasonCode === 'MISSING' || diffReasonCode === 'MISPLACED')
    const isUnreviewed = reviewStatus === undefined || reviewStatus === null || reviewStatus !== 1

    return isUncounted || (isDiscrepancy && needsReview && isUnreviewed)
  })
})

const freezeMap = computed(() => {
  const map = new Map()
  freezeList.value.forEach((f) => {
    map.set(f.stopperId, f)
  })
  return map
})

const combinedList = computed(() => {
  return detailList.value.map((d) => {
    const freeze = freezeMap.value.get(d.stopperId)
    return {
      ...d,
      freezeStation: freeze?.station || '-',
      freezeSpec: freeze?.spec || '-',
      freezeStopperNo: freeze?.stopperNo || '-',
      freezeStatus: freeze?.status,
      stationChanged: freeze && d.station !== freeze.station,
      specChanged: freeze && d.spec !== freeze.spec,
      stopperNoChanged: freeze && d.stopperNo !== freeze.stopperNo
    }
  })
})

const diffDialogVisible = ref(false)
const diffForm = reactive({
  detailId: null,
  stopperNo: '',
  diffReasonCode: '',
  diffReason: ''
})

const diffReasonOptions = [
  { value: 'MISSING', label: '缺失' },
  { value: 'MISPLACED', label: '错位' },
  { value: 'WORN', label: '编号磨损' },
  { value: 'PICTURE_MISMATCH', label: '图片不符' }
]

const getDiffReasonLabel = (code) => {
  const option = diffReasonOptions.find(item => item.value === code)
  return option ? option.label : code
}

const loadData = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const [details, info, freezes] = await Promise.all([
      getInventoryDetail(id),
      getInventoryById(id),
      getInventoryFreeze(id)
    ])
    detailList.value = details
    inventoryInfo.value = info
    freezeList.value = freezes
    if (freezes && freezes.length > 0) {
      freezeTime.value = freezes[0].freezeTime
    }
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
      diffReasonCode: '',
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
  diffForm.diffReasonCode = row.diffReasonCode || ''
  diffForm.diffReason = row.diffReason || ''
  diffDialogVisible.value = true
}

const resetDiffForm = () => {
  diffForm.detailId = null
  diffForm.stopperNo = ''
  diffForm.diffReasonCode = ''
  diffForm.diffReason = ''
}

const submitDiff = async () => {
  if (!diffForm.diffReasonCode) {
    ElMessage.warning('请选择标准原因')
    return
  }
  try {
    await markInventoryItem({
      detailId: diffForm.detailId,
      status: 2,
      diffReasonCode: diffForm.diffReasonCode,
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
    if (error === 'cancel') {
      return
    }
    if (error && error.data && error.data.unprocessedCount !== undefined) {
      const { unprocessedCount, unprocessedStopperNos } = error.data
      let message = `存在 ${unprocessedCount} 项未处理，请先处理后再完成盘点。`
      if (unprocessedStopperNos && unprocessedStopperNos.length > 0) {
        const showCount = unprocessedStopperNos.length > 5 ? 5 : unprocessedStopperNos.length
        const moreText = unprocessedCount > 5 ? ` 等 ${unprocessedCount} 项` : ''
        message += `\n未处理挡块：${unprocessedStopperNos.slice(0, 5).join('、')}${moreText}`
      }
      ElMessageBox.alert(message, '存在未处理项', {
        confirmButtonText: '我知道了',
        type: 'warning',
        dangerouslyUseHTMLString: false
      })
    } else {
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
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
  padding: 20px 0 0 0;
  border-top: 1px solid #ebeef5;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
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

.changed-value {
  color: #e6a23c;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.change-icon {
  font-size: 12px;
  color: #e6a23c;
}

.freeze-value {
  color: #909399;
  font-style: italic;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
}

.diff-reason-sub {
  color: #909399;
  font-size: 12px;
}
</style>
