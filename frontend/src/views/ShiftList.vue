<template>
  <div class="shift-list">
    <el-card class="header-card">
      <div class="header-content">
        <h2 class="page-title">挡块工位移位登记</h2>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增移位
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="stopperNo" label="挡块编号" width="130" />
        <el-table-column prop="spec" label="规格型号" width="120">
          <template #default="{ row }">
            <span class="spec-tag">{{ row.spec || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="adaptEquipment" label="适配设备" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.adaptEquipment || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="工位移位路径" min-width="280">
          <template #default="{ row }">
            <div class="shift-path">
              <span :class="['station-tag', 'from-station', getStationClass(row.fromStation)]">
                {{ row.fromStation }}
              </span>
              <span :class="['arrow-icon', isCrossZone(row) ? 'cross-zone' : '', isRepairMove(row) ? 'repair-move' : '']">
                <el-icon><Right /></el-icon>
              </span>
              <span :class="['station-tag', 'to-station', getStationClass(row.toStation)]">
                {{ row.toStation }}
              </span>
              <span v-if="isCrossZone(row)" class="cross-zone-badge">跨区</span>
              <span v-if="isRepairMove(row)" class="repair-badge">维修</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="shiftReason" label="移位原因" min-width="180" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="shiftTime" label="移位时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.shiftTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增移位登记" width="550px" @close="resetForm">
      <el-form :model="shiftForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="挡块编号" prop="stopperId">
          <el-select
            v-model="shiftForm.stopperId"
            filterable
            placeholder="请选择挡块"
            style="width: 100%"
            @change="handleStopperChange"
          >
            <el-option
              v-for="s in stopperList"
              :key="s.id"
              :label="`${s.stopperNo} - ${s.spec}`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="当前工位">
          <el-input :value="currentStation" disabled placeholder="选择挡块后自动显示" />
        </el-form-item>
        <el-form-item label="目标工位" prop="toStation">
          <el-select
            v-model="shiftForm.toStation"
            placeholder="请选择或输入目标工位"
            filterable
            allow-create
            default-first-option
            remote
            reserve-keyword
            :remote-method="remoteSearchStation"
            :loading="stationLoading"
            style="width: 100%"
          >
            <el-option v-for="s in stationOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="shiftForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="移位原因">
          <el-input v-model="shiftForm.shiftReason" type="textarea" :rows="3" placeholder="请输入移位原因" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="shiftForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确认登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getShiftList, addShift } from '@/api/shift'
import { getStopperList, getStopperById, getAllStations as getStopperStations } from '@/api/stopper'
import { getStationNames, ensureStation } from '@/api/station'
import { ElMessage } from 'element-plus'
import { Right } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const stopperList = ref([])
const stationOptions = ref([])
const stationLoading = ref(false)

const dialogVisible = ref(false)
const formRef = ref(null)
const shiftForm = reactive({
  stopperId: null,
  stopperNo: '',
  toStation: '',
  operator: '',
  shiftReason: '',
  remark: ''
})

const currentStation = ref('')

const validateToStation = (rule, value, callback) => {
  if (value && currentStation.value && value.trim() === currentStation.value.trim()) {
    callback(new Error('同工位不可重复登记'))
  } else {
    callback()
  }
}

const formRules = {
  stopperId: [{ required: true, message: '请选择挡块', trigger: 'change' }],
  toStation: [
    { required: true, message: '请输入目标工位', trigger: 'blur' },
    { validator: validateToStation, trigger: 'blur' }
  ],
  shiftReason: [{
    required: () => currentStation.value && currentStation.value.includes('维修'),
    message: '维修区返回需填写原因',
    trigger: 'blur'
  }]
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const getZone = (station) => {
  if (!station) return ''
  const match = station.match(/^(.+?)区/)
  return match ? match[1] : ''
}

const isCrossZone = (row) => {
  const fromZone = getZone(row.fromStation)
  const toZone = getZone(row.toStation)
  return fromZone && toZone && fromZone !== toZone
}

const isRepairMove = (row) => {
  const fromRepair = row.fromStation && row.fromStation.includes('维修')
  const toRepair = row.toStation && row.toStation.includes('维修')
  return fromRepair || toRepair
}

const getStationClass = (station) => {
  if (!station) return ''
  if (station.includes('维修')) return 'station-repair'
  const zone = getZone(station)
  if (zone) return `zone-${zone.toLowerCase()}`
  return ''
}

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await getShiftList()
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadStoppers = async () => {
  try {
    const list = await getStopperList()
    stopperList.value = list.filter(s => s.status === 1)
  } catch (error) {
    console.error('加载挡块列表失败')
  }
}

const loadStations = async () => {
  try {
    stationOptions.value = await getStationNames()
  } catch (error) {
    console.error('加载工位列表失败，使用挡块表工位兜底')
    try {
      stationOptions.value = await getStopperStations()
    } catch (e) {
      console.error('加载工位列表失败')
    }
  }
}

const remoteSearchStation = async (query) => {
  stationLoading.value = true
  try {
    stationOptions.value = await getStationNames(query)
  } catch (error) {
    console.error('搜索工位失败')
  } finally {
    stationLoading.value = false
  }
}

const handleStopperChange = async (stopperId) => {
  if (formRef.value) {
    formRef.value.clearValidate()
  }
  if (stopperId) {
    try {
      const stopper = stopperList.value.find(s => s.id === stopperId)
      if (stopper) {
        currentStation.value = stopper.station
        shiftForm.stopperNo = stopper.stopperNo
      }
    } catch (error) {
      console.error('获取挡块信息失败')
    }
  } else {
    currentStation.value = ''
    shiftForm.stopperNo = ''
  }
}

const handleAdd = () => {
  loadStations()
  dialogVisible.value = true
}

const resetForm = () => {
  shiftForm.stopperId = null
  shiftForm.stopperNo = ''
  shiftForm.toStation = ''
  shiftForm.operator = ''
  shiftForm.shiftReason = ''
  shiftForm.remark = ''
  currentStation.value = ''
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (shiftForm.toStation) {
          try {
            await ensureStation(shiftForm.toStation.trim())
          } catch (e) {
            console.warn('确保工位存在失败，可能已在移位时自动处理')
          }
        }
        await addShift(shiftForm)
        ElMessage.success('登记成功')
        dialogVisible.value = false
        loadData()
        loadStoppers()
      } catch (error) {
        if (error.fieldErrors) {
          const fields = []
          Object.keys(error.fieldErrors).forEach(field => {
            fields.push({
              field: field,
              message: error.fieldErrors[field]
            })
          })
          formRef.value.setFields(fields)
        }
      }
    }
  })
}

onMounted(() => {
  loadData()
  loadStoppers()
})
</script>

<style scoped>
.shift-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card,
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

.spec-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 12px;
}

.shift-path {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.station-tag {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  background: #f0f2f5;
  color: #606266;
}

.station-tag.zone-a {
  background: #ecf5ff;
  color: #409eff;
  border: 1px solid #d9ecff;
}

.station-tag.zone-b {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}

.station-tag.zone-c {
  background: #fdf6ec;
  color: #e6a23c;
  border: 1px solid #faecd8;
}

.station-tag.station-repair {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
  font-weight: 600;
}

.arrow-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 16px;
  transition: all 0.3s;
}

.arrow-icon.cross-zone {
  color: #e6a23c;
  font-weight: bold;
  transform: scale(1.2);
}

.arrow-icon.repair-move {
  color: #f56c6c;
  font-weight: bold;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.cross-zone-badge {
  display: inline-block;
  padding: 2px 6px;
  background: #e6a23c;
  color: #fff;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 500;
}

.repair-badge {
  display: inline-block;
  padding: 2px 6px;
  background: #f56c6c;
  color: #fff;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 500;
}
</style>
