<template>
  <div class="maintenance-list">
    <el-card class="header-card">
      <div class="header-content">
        <h2 class="page-title">挡块维修周转登记</h2>
        <el-button type="warning" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增送修
        </el-button>
      </div>
    </el-card>

    <el-card class="stat-card">
      <div class="stat-row">
        <div class="stat-item">
          <span class="stat-label">维修中</span>
          <span class="stat-value stat-warn">{{ activeCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">已完成</span>
          <span class="stat-value stat-success">{{ completedCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">转报废</span>
          <span class="stat-value stat-danger">{{ scrapCount }}</span>
        </div>
        <div class="stat-filter">
          <el-radio-group v-model="statusFilter" @change="applyFilter">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button :label="1">维修中</el-radio-button>
            <el-radio-button :label="2">已完成</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="filteredData" v-loading="loading" border stripe>
        <el-table-column prop="stopperNo" label="挡块编号" width="120" />
        <el-table-column prop="spec" label="规格型号" width="120">
          <template #default="{ row }">
            <span class="spec-tag">{{ row.spec || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="adaptEquipment" label="适配设备" width="160" show-overflow-tooltip />
        <el-table-column prop="originalStation" label="原工位" width="130" />
        <el-table-column prop="repairReason" label="维修原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="expectedReturnDate" label="预计返回" width="120" />
        <el-table-column prop="sendOperator" label="送修人" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'warning' : 'success'" size="small" effect="light">
              {{ row.status === 1 ? '维修中' : '已完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理结果" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.outcome === 'RETURN'" type="primary" size="small">返回原工位</el-tag>
            <el-tag v-else-if="row.outcome === 'SCRAP'" type="danger" size="small">转报废</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sendTime" label="送修时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.sendTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="success" size="small" link @click="handleComplete(row)">完成</el-button>
            <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增送修登记" width="560px" @close="resetForm">
      <el-form :model="sendForm" :rules="formRules" ref="formRef" label-width="110px">
        <el-form-item label="挡块编号" prop="stopperId">
          <el-select
            v-model="sendForm.stopperId"
            filterable
            placeholder="请选择挡块"
            style="width: 100%"
            @change="handleStopperChange"
          >
            <el-option
              v-for="s in stopperOptions"
              :key="s.id"
              :label="`${s.stopperNo} - ${s.spec} (${s.station})`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="原工位">
          <el-input :value="currentStation" disabled placeholder="选择挡块后自动显示" />
        </el-form-item>
        <el-form-item label="维修原因" prop="repairReason">
          <el-input v-model="sendForm.repairReason" type="textarea" :rows="3" placeholder="请输入维修原因" />
        </el-form-item>
        <el-form-item label="预计返回日期" prop="expectedReturnDate">
          <el-date-picker
            v-model="sendForm.expectedReturnDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择预计返回日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="送修操作人">
          <el-input v-model="sendForm.sendOperator" placeholder="请输入送修操作人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="sendForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleSubmit">确认送修</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="completeDialogVisible" title="维修完成处理" width="520px" @close="resetCompleteForm">
      <el-alert
        v-if="completeForm.outcome === 'SCRAP'"
        title="转报废将自动生成报废归档记录，挡块状态变更为已报废"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-form :model="completeForm" :rules="completeRules" ref="completeFormRef" label-width="110px">
        <el-form-item label="挡块编号">
          <el-input :value="completeForm.stopperNo" disabled />
        </el-form-item>
        <el-form-item label="原工位">
          <el-input :value="completeForm.originalStation || '-'" disabled />
        </el-form-item>
        <el-form-item label="处理结果" prop="outcome">
          <el-radio-group v-model="completeForm.outcome">
            <el-radio label="RETURN">返回原工位</el-radio>
            <el-radio label="SCRAP">转报废</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="completeForm.outcome === 'RETURN'" label="返回工位" prop="returnStation">
          <el-select
            v-model="completeForm.returnStation"
            filterable
            allow-create
            default-first-option
            placeholder="默认返回原工位，可调整"
            style="width: 100%"
          >
            <el-option v-for="s in stationOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="完成操作人">
          <el-input v-model="completeForm.completeOperator" placeholder="请输入完成操作人" />
        </el-form-item>
        <el-form-item label="完成备注">
          <el-input v-model="completeForm.completeRemark" type="textarea" :rows="2" placeholder="请输入完成备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button :type="completeForm.outcome === 'SCRAP' ? 'danger' : 'success'" @click="handleCompleteSubmit">确认完成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="维修详情" width="560px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="挡块编号">{{ viewData.stopperNo }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ viewData.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="适配设备" :span="2">{{ viewData.adaptEquipment || '-' }}</el-descriptions-item>
        <el-descriptions-item label="原工位">{{ viewData.originalStation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计返回">{{ viewData.expectedReturnDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="维修原因" :span="2">{{ viewData.repairReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="送修人">{{ viewData.sendOperator || '-' }}</el-descriptions-item>
        <el-descriptions-item label="送修时间">{{ formatDate(viewData.sendTime) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.status === 1 ? 'warning' : 'success'" size="small">
            {{ viewData.status === 1 ? '维修中' : '已完成' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理结果">
          <el-tag v-if="viewData.outcome === 'RETURN'" type="primary" size="small">返回原工位</el-tag>
          <el-tag v-else-if="viewData.outcome === 'SCRAP'" type="danger" size="small">转报废</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="viewData.outcome === 'RETURN'" label="返回工位" :span="2">
          {{ viewData.returnStation || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="完成人">{{ viewData.completeOperator || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ formatDate(viewData.completeTime) }}</el-descriptions-item>
        <el-descriptions-item label="完成备注" :span="2">{{ viewData.completeRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ viewData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getMaintenanceList, sendMaintenance, completeMaintenance } from '@/api/maintenance'
import { getStopperList } from '@/api/stopper'
import { getStationNames } from '@/api/station'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const stopperList = ref([])
const stationOptions = ref([])
const statusFilter = ref('')

const dialogVisible = ref(false)
const completeDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const formRef = ref(null)
const completeFormRef = ref(null)
const currentStation = ref('')
const viewData = ref({})

const sendForm = reactive({
  stopperId: null,
  repairReason: '',
  expectedReturnDate: '',
  sendOperator: '',
  remark: ''
})

const completeForm = reactive({
  id: null,
  stopperNo: '',
  originalStation: '',
  outcome: 'RETURN',
  returnStation: '',
  completeOperator: '',
  completeRemark: ''
})

const formRules = {
  stopperId: [{ required: true, message: '请选择挡块', trigger: 'change' }],
  repairReason: [{ required: true, message: '请输入维修原因', trigger: 'blur' }],
  expectedReturnDate: [{ required: true, message: '请选择预计返回日期', trigger: 'change' }]
}

const validateOutcome = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请选择处理结果'))
  } else {
    callback()
  }
}

const completeRules = {
  outcome: [{ required: true, validator: validateOutcome, trigger: 'change' }]
}

const stopperOptions = computed(() => {
  return stopperList.value.filter(s => s.status === 1 && s.station !== '维修区')
})

const filteredData = computed(() => {
  if (statusFilter.value === '') return tableData.value
  return tableData.value.filter(r => r.status === statusFilter.value)
})

const activeCount = computed(() => tableData.value.filter(r => r.status === 1).length)
const completedCount = computed(() => tableData.value.filter(r => r.status === 2).length)
const scrapCount = computed(() => tableData.value.filter(r => r.outcome === 'SCRAP').length)

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const applyFilter = () => {}

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await getMaintenanceList()
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadStoppers = async () => {
  try {
    const list = await getStopperList()
    stopperList.value = list || []
  } catch (error) {
    console.error('加载挡块列表失败')
  }
}

const loadStations = async () => {
  try {
    stationOptions.value = await getStationNames()
  } catch (error) {
    console.error('加载工位列表失败')
  }
}

const handleStopperChange = (stopperId) => {
  if (stopperId) {
    const stopper = stopperList.value.find(s => s.id === stopperId)
    if (stopper) {
      currentStation.value = stopper.station
    }
  } else {
    currentStation.value = ''
  }
}

const handleAdd = () => {
  dialogVisible.value = true
}

const resetForm = () => {
  sendForm.stopperId = null
  sendForm.repairReason = ''
  sendForm.expectedReturnDate = ''
  sendForm.sendOperator = ''
  sendForm.remark = ''
  currentStation.value = ''
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await sendMaintenance(sendForm)
        ElMessage.success('送修登记成功')
        dialogVisible.value = false
        loadData()
        loadStoppers()
      } catch (error) {
        if (error.fieldErrors) {
          const fields = []
          Object.keys(error.fieldErrors).forEach(field => {
            fields.push({ field, message: error.fieldErrors[field] })
          })
          formRef.value.setFields(fields)
        } else {
          ElMessage.error('登记失败')
        }
      }
    }
  })
}

const handleComplete = (row) => {
  completeForm.id = row.id
  completeForm.stopperNo = row.stopperNo
  completeForm.originalStation = row.originalStation
  completeForm.outcome = 'RETURN'
  completeForm.returnStation = row.originalStation || ''
  completeForm.completeOperator = ''
  completeForm.completeRemark = ''
  loadStations()
  completeDialogVisible.value = true
}

const resetCompleteForm = () => {
  completeForm.id = null
  completeForm.stopperNo = ''
  completeForm.originalStation = ''
  completeForm.outcome = 'RETURN'
  completeForm.returnStation = ''
  completeForm.completeOperator = ''
  completeForm.completeRemark = ''
  completeFormRef.value?.resetFields()
}

const handleCompleteSubmit = async () => {
  if (!completeFormRef.value) return
  await completeFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await completeMaintenance(completeForm.id, {
          outcome: completeForm.outcome,
          returnStation: completeForm.outcome === 'RETURN' ? completeForm.returnStation : '',
          completeOperator: completeForm.completeOperator,
          completeRemark: completeForm.completeRemark
        })
        ElMessage.success('维修完成处理成功')
        completeDialogVisible.value = false
        loadData()
        loadStoppers()
      } catch (error) {
        if (error.fieldErrors) {
          const fields = []
          Object.keys(error.fieldErrors).forEach(field => {
            fields.push({ field, message: error.fieldErrors[field] })
          })
          completeFormRef.value.setFields(fields)
        } else {
          ElMessage.error('处理失败')
        }
      }
    }
  })
}

const handleView = (row) => {
  viewData.value = row
  viewDialogVisible.value = true
}

onMounted(() => {
  loadData()
  loadStoppers()
})
</script>

<style scoped>
.maintenance-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card,
.stat-card,
.table-card {
  border-radius: 8px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 40px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-warn {
  color: #e6a23c;
}

.stat-success {
  color: #67c23a;
}

.stat-danger {
  color: #f56c6c;
}

.stat-filter {
  margin-left: auto;
}

.spec-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 12px;
}

.text-muted {
  color: #c0c4cc;
}
</style>
