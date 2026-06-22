<template>
  <div class="inventory-review">
    <el-card class="header-card">
      <div class="header-content">
        <h2 class="page-title">清点差异复核</h2>
      </div>

      <div class="summary-stats">
        <div class="stat-item">
          <span class="stat-label">差异总数</span>
          <span class="stat-value primary">{{ summaryTotal.totalDiffCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">待复核</span>
          <span class="stat-value warning">{{ summaryTotal.pendingReviewCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">已复核</span>
          <span class="stat-value success">{{ summaryTotal.reviewedCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">修正工位</span>
          <span class="stat-value info">{{ summaryTotal.correctStationCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">已找回</span>
          <span class="stat-value success">{{ summaryTotal.foundCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">已报废</span>
          <span class="stat-value danger">{{ summaryTotal.scrapCount }}</span>
        </div>
      </div>
    </el-card>

    <el-card class="filter-card">
      <div class="filter-content">
        <el-form :inline="true" :model="filterForm">
          <el-form-item label="盘点单">
            <el-select
              v-model="filterForm.inventoryId"
              placeholder="全部盘点单"
              clearable
              style="width: 240px"
              @change="loadAllData"
            >
              <el-option
                v-for="item in summaryList"
                :key="item.inventoryId"
                :label="`${item.inventoryNo} (${item.inventoryMonth})`"
                :value="item.inventoryId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="筛选类型">
            <el-radio-group v-model="activeTab" @change="handleTabChange">
              <el-radio-button label="pending">待复核</el-radio-button>
              <el-radio-button label="reviewed">已复核</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="loadAllData">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>{{ activeTab === 'pending' ? '待复核差异列表' : '已复核差异列表' }}</span>
        </div>
      </template>

      <el-table
        :data="activeTab === 'pending' ? pendingList : reviewedList"
        v-loading="loading"
        border
        stripe
      >
        <el-table-column prop="inventoryNo" label="盘点单号" width="170" />
        <el-table-column prop="inventoryMonth" label="盘点月份" width="110" />
        <el-table-column prop="stopperNo" label="挡块编号" width="130">
          <template #default="{ row }">
            <el-link type="primary" @click="goStopperDetail(row.stopperId)">
              {{ row.stopperNo }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="spec" label="规格型号" width="130" />
        <el-table-column prop="freezeStation" label="冻结工位" width="130">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.freezeStation || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentStation" label="当前工位" width="130">
          <template #default="{ row }">
            <span
              :class="{ 'station-changed': row.freezeStation && row.currentStation !== row.freezeStation }"
            >
              {{ row.currentStation || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="差异原因" width="120">
          <template #default="{ row }">
            <el-tag :type="getDiffReasonTagType(row.diffReasonCode)" size="small">
              {{ getDiffReasonLabel(row.diffReasonCode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="差异说明" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.diffReason || '-' }}
          </template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'reviewed'" label="复核结果" width="120">
          <template #default="{ row }">
            <el-tag :type="getReviewResultTagType(row.reviewResult)" size="small">
              {{ getReviewResultLabel(row.reviewResult) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'reviewed'" prop="correctedStation" label="修正后工位" width="130">
          <template #default="{ row }">
            {{ row.correctedStation || '-' }}
          </template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'reviewed'" label="复核信息" width="200">
          <template #default="{ row }">
            <div class="review-info">
              <div><span class="info-label">操作人：</span>{{ row.reviewOperator || '-' }}</div>
              <div><span class="info-label">时间：</span>{{ formatDate(row.reviewTime) }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="activeTab === 'pending' ? 340 : 150" fixed="right">
          <template #default="{ row }">
            <template v-if="activeTab === 'pending'">
              <el-button type="primary" size="small" link @click="openSecondConfirm(row)">
                二次确认
              </el-button>
              <el-button v-if="row.diffReasonCode === 'MISPLACED'" type="success" size="small" link @click="openCorrectStation(row)">
                修正工位
              </el-button>
              <el-button v-if="row.diffReasonCode === 'MISSING'" type="warning" size="small" link @click="openMarkFound(row)">
                标记找回
              </el-button>
              <el-button type="danger" size="small" link @click="openProcessScrap(row)">
                报废流程
              </el-button>
            </template>
            <template v-else>
              <el-button type="info" size="small" link @click="viewHistory(row)">
                查看历史
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="secondConfirmVisible" title="二次确认差异" width="480px" @close="resetForms">
      <el-form :model="confirmForm" label-width="100px">
        <el-form-item label="挡块编号">
          <el-input :value="confirmForm.stopperNo" disabled />
        </el-form-item>
        <el-form-item label="差异类型">
          <el-tag :type="getDiffReasonTagType(confirmForm.diffReasonCode)" size="large">
            {{ getDiffReasonLabel(confirmForm.diffReasonCode) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="冻结工位">
          <el-input :value="confirmForm.freezeStation" disabled />
        </el-form-item>
        <el-form-item label="当前工位">
          <el-input :value="confirmForm.currentStation" disabled />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="confirmForm.operator" placeholder="请输入操作人姓名" />
        </el-form-item>
        <el-form-item label="确认备注">
          <el-input v-model="confirmForm.remark" type="textarea" :rows="3" placeholder="请输入确认说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="secondConfirmVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSecondConfirm">确认提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="correctStationVisible" title="修正工位（错位挡块）" width="480px" @close="resetForms">
      <el-form :model="stationForm" :rules="stationRules" ref="stationFormRef" label-width="100px">
        <el-form-item label="挡块编号">
          <el-input :value="stationForm.stopperNo" disabled />
        </el-form-item>
        <el-form-item label="原工位">
          <el-input :value="stationForm.currentStation" disabled />
        </el-form-item>
        <el-form-item label="正确工位" prop="correctedStation">
          <el-select
            v-model="stationForm.correctedStation"
            placeholder="请选择或输入正确工位"
            filterable
            allow-create
            default-first-option
            style="width: 100%"
          >
            <el-option v-for="s in stationOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="stationForm.operator" placeholder="请输入操作人姓名" />
        </el-form-item>
        <el-form-item label="修正说明">
          <el-input v-model="stationForm.remark" type="textarea" :rows="3" placeholder="请输入修正说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctStationVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCorrectStation">确认修正</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="markFoundVisible" title="标记挡块找回" width="480px" @close="resetForms">
      <el-form :model="foundForm" :rules="foundRules" ref="foundFormRef" label-width="100px">
        <el-form-item label="挡块编号">
          <el-input :value="foundForm.stopperNo" disabled />
        </el-form-item>
        <el-form-item label="冻结工位">
          <el-input :value="foundForm.freezeStation" disabled />
        </el-form-item>
        <el-form-item label="找回工位">
          <el-select
            v-model="foundForm.foundStation"
            placeholder="请选择或输入找回的工位（可选）"
            filterable
            allow-create
            default-first-option
            style="width: 100%"
          >
            <el-option v-for="s in stationOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="foundForm.operator" placeholder="请输入操作人姓名" />
        </el-form-item>
        <el-form-item label="找回说明">
          <el-input v-model="foundForm.remark" type="textarea" :rows="3" placeholder="请输入找回说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="markFoundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMarkFound">确认找回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scrapVisible" title="挡块报废流程" width="480px" @close="resetForms">
      <el-form :model="scrapForm" :rules="scrapRules" ref="scrapFormRef" label-width="100px">
        <el-form-item label="挡块编号">
          <el-input :value="scrapForm.stopperNo" disabled />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input :value="scrapForm.spec" disabled />
        </el-form-item>
        <el-form-item label="差异类型">
          <el-tag :type="getDiffReasonTagType(scrapForm.diffReasonCode)" size="large">
            {{ getDiffReasonLabel(scrapForm.diffReasonCode) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="报废原因" prop="scrapReason">
          <el-input v-model="scrapForm.scrapReason" type="textarea" :rows="2" placeholder="请输入报废原因" />
        </el-form-item>
        <el-form-item label="磨损程度">
          <el-select v-model="scrapForm.scrapDegree" placeholder="请选择磨损程度（可选）" style="width: 100%">
            <el-option label="轻微磨损" value="LIGHT" />
            <el-option label="中度磨损" value="MEDIUM" />
            <el-option label="严重磨损" value="SEVERE" />
            <el-option label="完全损坏" value="BROKEN" />
            <el-option label="遗失" value="LOST" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="scrapForm.operator" placeholder="请输入操作人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="scrapForm.remark" type="textarea" :rows="2" placeholder="请输入备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scrapVisible = false">取消</el-button>
        <el-button type="danger" @click="submitProcessScrap">确认报废</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyVisible" title="复核操作历史" width="600px">
      <el-timeline v-if="currentHistory && currentHistory.length > 0">
        <el-timeline-item
          v-for="(item, index) in currentHistory"
          :key="item.id"
          :timestamp="formatDate(item.reviewTime)"
          :type="getHistoryItemType(item.reviewAction)"
          :hollow="index === 0"
        >
          <el-card shadow="never" class="history-card">
            <div class="history-header">
              <el-tag :type="getHistoryTagType(item.reviewAction)" size="large">
                {{ getActionLabel(item.reviewAction) }}
              </el-tag>
              <span class="history-operator">操作人：{{ item.reviewOperator || '-' }}</span>
            </div>
            <div class="history-content">
              <div v-if="item.originalStation" class="history-row">
                <span class="history-label">原工位：</span>
                <span>{{ item.originalStation }}</span>
              </div>
              <div v-if="item.currentStation" class="history-row">
                <span class="history-label">当前工位：</span>
                <span>{{ item.currentStation }}</span>
              </div>
              <div v-if="item.correctedStation" class="history-row">
                <span class="history-label">修正/找回工位：</span>
                <span class="history-value-highlight">{{ item.correctedStation }}</span>
              </div>
              <div v-if="item.reviewRemark" class="history-row">
                <span class="history-label">备注：</span>
                <span>{{ item.reviewRemark }}</span>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无复核历史记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getReviewSummary,
  getPendingReviews,
  getReviewedList,
  getReviewHistory,
  secondConfirm,
  correctStation,
  markFound,
  processScrap
} from '@/api/inventoryReview'
import { getStationNames } from '@/api/station'
import { ElMessage, ElMessageBox } from 'element-plus'
import { eventBus, EVENTS } from '@/utils/eventBus'

const router = useRouter()

const loading = ref(false)
const activeTab = ref('pending')
const summaryList = ref([])
const pendingList = ref([])
const reviewedList = ref([])
const stationOptions = ref([])

const filterForm = reactive({
  inventoryId: null
})

const summaryTotal = computed(() => {
  return {
    totalDiffCount: summaryList.value.reduce((sum, i) => sum + (i.totalDiffCount || 0), 0),
    pendingReviewCount: summaryList.value.reduce((sum, i) => sum + (i.pendingReviewCount || 0), 0),
    reviewedCount: summaryList.value.reduce((sum, i) => sum + (i.reviewedCount || 0), 0),
    correctStationCount: summaryList.value.reduce((sum, i) => sum + (i.correctStationCount || 0), 0),
    foundCount: summaryList.value.reduce((sum, i) => sum + (i.foundCount || 0), 0),
    scrapCount: summaryList.value.reduce((sum, i) => sum + (i.scrapCount || 0), 0)
  }
})

const diffReasonOptions = [
  { value: 'MISSING', label: '缺失', tagType: 'danger' },
  { value: 'MISPLACED', label: '错位', tagType: 'warning' },
  { value: 'WORN', label: '编号磨损', tagType: 'info' },
  { value: 'PICTURE_MISMATCH', label: '图片不符', tagType: 'info' }
]

const reviewResultOptions = [
  { value: 'CORRECT_STATION', label: '修正工位', tagType: 'success' },
  { value: 'FOUND', label: '标记找回', tagType: 'warning' },
  { value: 'SCRAP', label: '报废', tagType: 'danger' }
]

const getDiffReasonLabel = (code) => {
  const opt = diffReasonOptions.find(i => i.value === code)
  return opt ? opt.label : code
}
const getDiffReasonTagType = (code) => {
  const opt = diffReasonOptions.find(i => i.value === code)
  return opt ? opt.tagType : 'info'
}
const getReviewResultLabel = (code) => {
  const opt = reviewResultOptions.find(i => i.value === code)
  return opt ? opt.label : code
}
const getReviewResultTagType = (code) => {
  const opt = reviewResultOptions.find(i => i.value === code)
  return opt ? opt.tagType : 'info'
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const handleTabChange = () => {}

const loadStationOptions = async () => {
  try {
    const names = await getStationNames()
    stationOptions.value = names || []
  } catch (e) {}
}

const loadAllData = async () => {
  loading.value = true
  try {
    const [summary, pending, reviewed] = await Promise.all([
      getReviewSummary(),
      getPendingReviews(filterForm.inventoryId),
      getReviewedList(filterForm.inventoryId)
    ])
    summaryList.value = summary || []
    pendingList.value = pending || []
    reviewedList.value = reviewed || []
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const goStopperDetail = (stopperId) => {
  if (stopperId) {
    router.push(`/stopper/detail/${stopperId}`)
  }
}

const secondConfirmVisible = ref(false)
const confirmForm = reactive({
  detailId: null,
  stopperNo: '',
  diffReasonCode: '',
  freezeStation: '',
  currentStation: '',
  operator: '',
  remark: ''
})

const openSecondConfirm = (row) => {
  confirmForm.detailId = row.detailId
  confirmForm.stopperNo = row.stopperNo
  confirmForm.diffReasonCode = row.diffReasonCode
  confirmForm.freezeStation = row.freezeStation
  confirmForm.currentStation = row.currentStation
  confirmForm.operator = ''
  confirmForm.remark = ''
  secondConfirmVisible.value = true
}

const submitSecondConfirm = async () => {
  if (!confirmForm.operator) {
    ElMessage.warning('请输入操作人')
    return
  }
  try {
    await secondConfirm({
      detailId: confirmForm.detailId,
      operator: confirmForm.operator,
      remark: confirmForm.remark
    })
    ElMessage.success('二次确认完成，请继续选择具体处理方式')
    secondConfirmVisible.value = false
    await loadAllData()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const correctStationVisible = ref(false)
const stationFormRef = ref(null)
const stationForm = reactive({
  detailId: null,
  stopperNo: '',
  currentStation: '',
  correctedStation: '',
  operator: '',
  remark: ''
})
const stationRules = {
  correctedStation: [{ required: true, message: '请选择正确工位', trigger: 'change' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }]
}

const openCorrectStation = (row) => {
  stationForm.detailId = row.detailId
  stationForm.stopperNo = row.stopperNo
  stationForm.currentStation = row.currentStation
  stationForm.correctedStation = ''
  stationForm.operator = ''
  stationForm.remark = ''
  correctStationVisible.value = true
}

const submitCorrectStation = async () => {
  if (!stationFormRef.value) return
  await stationFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await ElMessageBox.confirm(
          `确认将挡块 ${stationForm.stopperNo} 的工位从「${stationForm.currentStation}」修正为「${stationForm.correctedStation}」？`,
          '工位修正确认',
          { type: 'warning' }
        )
        await correctStation({
          detailId: stationForm.detailId,
          correctedStation: stationForm.correctedStation,
          operator: stationForm.operator,
          remark: stationForm.remark
        })
        ElMessage.success('工位修正成功')
        correctStationVisible.value = false
        await loadAllData()
      } catch (e) {
        if (e !== 'cancel') {
          ElMessage.error(e?.message || '操作失败')
        }
      }
    }
  })
}

const markFoundVisible = ref(false)
const foundFormRef = ref(null)
const foundForm = reactive({
  detailId: null,
  stopperNo: '',
  freezeStation: '',
  foundStation: '',
  operator: '',
  remark: ''
})
const foundRules = {
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }]
}

const openMarkFound = (row) => {
  foundForm.detailId = row.detailId
  foundForm.stopperNo = row.stopperNo
  foundForm.freezeStation = row.freezeStation
  foundForm.foundStation = ''
  foundForm.operator = ''
  foundForm.remark = ''
  markFoundVisible.value = true
}

const submitMarkFound = async () => {
  if (!foundFormRef.value) return
  await foundFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await ElMessageBox.confirm(
          `确认挡块 ${foundForm.stopperNo} 已找回${foundForm.foundStation ? `，放置于「${foundForm.foundStation}」` : ''}？`,
          '标记找回确认',
          { type: 'warning' }
        )
        await markFound({
          detailId: foundForm.detailId,
          foundStation: foundForm.foundStation,
          operator: foundForm.operator,
          remark: foundForm.remark
        })
        ElMessage.success('标记找回成功')
        markFoundVisible.value = false
        await loadAllData()
      } catch (e) {
        if (e !== 'cancel') {
          ElMessage.error(e?.message || '操作失败')
        }
      }
    }
  })
}

const scrapVisible = ref(false)
const scrapFormRef = ref(null)
const scrapForm = reactive({
  detailId: null,
  stopperNo: '',
  spec: '',
  diffReasonCode: '',
  scrapReason: '',
  scrapDegree: '',
  operator: '',
  remark: ''
})
const scrapRules = {
  scrapReason: [{ required: true, message: '请输入报废原因', trigger: 'blur' }],
  operator: [{ required: true, message: '请输入操作人', trigger: 'blur' }]
}

const openProcessScrap = (row) => {
  scrapForm.detailId = row.detailId
  scrapForm.stopperNo = row.stopperNo
  scrapForm.spec = row.spec
  scrapForm.diffReasonCode = row.diffReasonCode
  scrapForm.scrapReason = ''
  scrapForm.scrapDegree = ''
  scrapForm.operator = ''
  scrapForm.remark = ''
  scrapVisible.value = true
}

const submitProcessScrap = async () => {
  if (!scrapFormRef.value) return
  await scrapFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await ElMessageBox.confirm(
          `确认将挡块 ${scrapForm.stopperNo} 进行报废处理？此操作不可撤销。`,
          '报废确认',
          { type: 'error', confirmButtonText: '确认报废' }
        )
        const scrappedStopper = await processScrap({
          detailId: scrapForm.detailId,
          scrapReason: scrapForm.scrapReason,
          scrapDegree: scrapForm.scrapDegree,
          operator: scrapForm.operator,
          remark: scrapForm.remark
        })
        ElMessage.success('报废处理成功')
        scrapVisible.value = false
        await loadAllData()
        if (scrappedStopper && scrappedStopper.id) {
          eventBus.emit(EVENTS.STOPPER_SCRAPPED, scrappedStopper)
        }
      } catch (e) {
        if (e !== 'cancel') {
          ElMessage.error(e?.message || '操作失败')
        }
      }
    }
  })
}

const resetForms = () => {
  stationFormRef.value?.resetFields()
  foundFormRef.value?.resetFields()
  scrapFormRef.value?.resetFields()
}

const historyVisible = ref(false)
const currentHistory = ref([])

const viewHistory = async (row) => {
  try {
    const history = await getReviewHistory(row.detailId)
    currentHistory.value = history || []
    historyVisible.value = true
  } catch (e) {
    ElMessage.error('加载历史记录失败')
  }
}

const getActionLabel = (action) => {
  const map = {
    SECOND_CONFIRM: '二次确认',
    CORRECT_STATION: '修正工位',
    FOUND: '标记找回',
    SCRAP: '报废处理'
  }
  return map[action] || action
}

const getHistoryTagType = (action) => {
  const map = {
    SECOND_CONFIRM: 'primary',
    CORRECT_STATION: 'success',
    FOUND: 'warning',
    SCRAP: 'danger'
  }
  return map[action] || 'info'
}

const getHistoryItemType = (action) => {
  const map = {
    SECOND_CONFIRM: 'primary',
    CORRECT_STATION: 'success',
    FOUND: 'warning',
    SCRAP: 'danger'
  }
  return map[action] || 'primary'
}

onMounted(() => {
  loadStationOptions()
  loadAllData()
})
</script>

<style scoped>
.inventory-review {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card,
.filter-card,
.table-card {
  border-radius: 8px;
}

.header-content {
  margin-bottom: 20px;
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

.stat-value.primary {
  color: #409eff;
}

.stat-value.success {
  color: #67c23a;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-value.danger {
  color: #f56c6c;
}

.stat-value.info {
  color: #909399;
}

.filter-content {
  display: flex;
  align-items: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.station-changed {
  color: #e6a23c;
  font-weight: 600;
}

.review-info {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
}

.info-label {
  color: #909399;
}

.history-card {
  border: none;
  background: #fafafa;
  margin: 0;
}

.history-card :deep(.el-card__body) {
  padding: 12px 16px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.history-operator {
  font-size: 12px;
  color: #909399;
}

.history-content {
  font-size: 13px;
  color: #606266;
}

.history-row {
  padding: 2px 0;
}

.history-label {
  color: #909399;
  margin-right: 8px;
}

.history-value-highlight {
  color: #67c23a;
  font-weight: 600;
}
</style>
