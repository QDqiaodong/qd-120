<template>
  <div class="stopper-list">
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="关键字">
          <el-input
            v-model="filterForm.keyword"
            placeholder="编号/规格/设备"
            clearable
            @keyup.enter="handleSearch"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="规格">
          <el-select v-model="filterForm.spec" placeholder="全部规格" clearable filterable style="width: 160px">
            <el-option v-for="s in specs" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="工位">
          <el-select
            v-model="filterForm.station"
            placeholder="全部工位"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="remoteSearchStation"
            :loading="stationLoading"
            style="width: 160px"
          >
            <el-option v-for="s in filterStations" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="适配设备">
          <el-select
            v-model="filterForm.equipment"
            placeholder="全部设备"
            clearable
            filterable
            remote
            reserve-keyword
            :remote-method="remoteSearchEquipment"
            :loading="equipmentLoading"
            style="width: 180px"
          >
            <el-option v-for="e in equipments" :key="e" :label="e" :value="e" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="报废" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增挡块
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="stopperNo" label="挡块编号" width="120" />
        <el-table-column prop="spec" label="规格型号" width="130" />
        <el-table-column prop="adaptEquipment" label="适配设备" min-width="180" />
        <el-table-column prop="station" label="存放工位" width="130" />
        <el-table-column prop="storageTime" label="入库时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.storageTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '报废' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图片" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              class="stopper-thumb"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              :preview-teleported="true"
              :hide-on-click-modal="true"
              fit="contain"
            >
              <template #placeholder>
                <div class="image-slot">
                  <el-icon class="is-loading"><Loading /></el-icon>
                </div>
              </template>
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                  <span>加载失败</span>
                </div>
              </template>
            </el-image>
            <span v-else class="no-image">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button
              type="primary"
              size="small"
              link
              :disabled="row.status === 2"
              @click="handleShift(row)"
            >移位</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="resetForm">
      <el-form :model="stopperForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item v-if="!isEdit" label="编号方式">
          <el-radio-group v-model="autoGenerate">
            <el-radio :label="true">自动编号</el-radio>
            <el-radio :label="false">手动编号</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="挡块编号" prop="stopperNo">
          <el-input
            v-model="stopperForm.stopperNo"
            placeholder="请输入挡块编号"
            :disabled="!isEdit && autoGenerate"
            @input="clearServerFieldError('stopperNo')"
          >
            <template #append v-if="!isEdit && autoGenerate">
              <el-button
                :loading="generatingNo"
                :disabled="!stopperForm.spec"
                @click="handleGenerateNo"
              >
                生成编号
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="规格型号" prop="spec">
          <el-select
            v-model="stopperForm.spec"
            placeholder="请选择或输入规格型号"
            filterable
            allow-create
            default-first-option
            style="width: 100%"
            @change="handleSpecChange"
          >
            <el-option v-for="s in specs" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="适配设备" prop="adaptEquipment">
          <el-input
            v-model="stopperForm.adaptEquipment"
            placeholder="请输入适配设备"
            @input="checkEquipmentChange"
          />
        </el-form-item>
        <el-form-item
          v-if="isEdit && equipmentChanged"
          label="更换原因"
          prop="changeReason"
        >
          <el-input
            v-model="stopperForm.changeReason"
            type="textarea"
            :rows="2"
            placeholder="适配设备变更时必须填写更换原因"
            @input="clearServerFieldError('changeReason')"
          />
        </el-form-item>
        <el-form-item label="操作人" v-if="isEdit && equipmentChanged">
          <el-input v-model="stopperForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="存放工位" prop="station">
          <el-select
            v-model="stopperForm.station"
            placeholder="请选择或输入存放工位"
            filterable
            allow-create
            default-first-option
            remote
            reserve-keyword
            :remote-method="remoteSearchStationForm"
            :loading="formStationLoading"
            style="width: 100%"
            @change="clearServerFieldError('station')"
          >
            <el-option v-for="s in formStations" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="stopperForm.imageUrl" placeholder="请输入图片URL" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stopperForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shiftDialogVisible" title="挡块移位登记" width="500px" @close="resetShiftForm">
      <el-form :model="shiftForm" :rules="shiftFormRules" ref="shiftFormRef" label-width="100px">
        <el-form-item label="挡块编号">
          <el-input :value="shiftForm.stopperNo" disabled />
        </el-form-item>
        <el-form-item label="当前工位">
          <el-input :value="shiftForm.fromStation" disabled />
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
            :remote-method="remoteSearchStationShift"
            :loading="shiftStationLoading"
            style="width: 100%"
          >
            <el-option v-for="s in shiftStations" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="shiftForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="移位原因" prop="shiftReason">
          <el-input v-model="shiftForm.shiftReason" type="textarea" :rows="2" placeholder="请输入移位原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shiftDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleShiftSubmit">确认移位</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  getStopperPage,
  getAllStations as getStopperStations,
  getAllSpecs,
  getAllEquipments,
  addStopper,
  updateStopper,
  deleteStopper,
  generateStopperNo
} from '@/api/stopper'
import { getStationNames, ensureStation } from '@/api/station'
import { addShift } from '@/api/shift'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const specs = ref([])
const filterStations = ref([])
const stationLoading = ref(false)
const equipments = ref([])
const equipmentLoading = ref(false)
const formStations = ref([])
const formStationLoading = ref(false)
const shiftStations = ref([])
const shiftStationLoading = ref(false)

const filterForm = reactive({
  keyword: '',
  spec: '',
  station: '',
  equipment: '',
  status: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增挡块')
const isEdit = ref(false)
const autoGenerate = ref(true)
const generatingNo = ref(false)
const formRef = ref(null)

const stopperForm = reactive({
  id: null,
  stopperNo: '',
  spec: '',
  adaptEquipment: '',
  station: '',
  imageUrl: '',
  remark: '',
  changeReason: '',
  operator: ''
})

const originalAdaptEquipment = ref('')
const equipmentChanged = ref(false)

const serverFieldErrors = reactive({
  stopperNo: '',
  spec: '',
  station: '',
  changeReason: ''
})

const createFieldValidator = (field, requiredMessage) => {
  return (rule, value, callback) => {
    if (value === undefined || value === null || String(value).trim() === '') {
      callback(new Error(requiredMessage))
      return
    }
    if (serverFieldErrors[field]) {
      callback(new Error(serverFieldErrors[field]))
      return
    }
    callback()
  }
}

const validateChangeReason = (rule, value, callback) => {
  if (equipmentChanged.value) {
    const oldEq = (originalAdaptEquipment.value || '').trim()
    const newEq = (stopperForm.adaptEquipment || '').trim()
    if ((oldEq !== '' || newEq !== '') && (!value || String(value).trim() === '')) {
      callback(new Error('适配设备变更时必须填写更换原因'))
      return
    }
  }
  if (serverFieldErrors.changeReason) {
    callback(new Error(serverFieldErrors.changeReason))
    return
  }
  callback()
}

const formRules = {
  stopperNo: [{ validator: createFieldValidator('stopperNo', '请输入挡块编号'), trigger: 'blur' }],
  spec: [{ validator: createFieldValidator('spec', '请输入规格型号'), trigger: 'blur' }],
  station: [{ validator: createFieldValidator('station', '请输入存放工位'), trigger: 'blur' }],
  changeReason: [{ validator: validateChangeReason, trigger: 'blur' }]
}

const checkEquipmentChange = () => {
  const oldEq = (originalAdaptEquipment.value || '').trim()
  const newEq = (stopperForm.adaptEquipment || '').trim()
  equipmentChanged.value = oldEq !== newEq
  if (!equipmentChanged.value) {
    stopperForm.changeReason = ''
    stopperForm.operator = ''
    clearServerFieldError('changeReason')
  }
}

const shiftDialogVisible = ref(false)
const shiftFormRef = ref(null)
const shiftForm = reactive({
  stopperId: null,
  stopperNo: '',
  fromStation: '',
  toStation: '',
  operator: '',
  shiftReason: ''
})

const validateShiftToStation = (rule, value, callback) => {
  if (value && shiftForm.fromStation && value.trim() === shiftForm.fromStation.trim()) {
    callback(new Error('同工位不可重复登记'))
  } else {
    callback()
  }
}

const shiftFormRules = {
  toStation: [
    { required: true, message: '请输入目标工位', trigger: 'blur' },
    { validator: validateShiftToStation, trigger: 'blur' }
  ],
  shiftReason: [{
    required: () => shiftForm.fromStation && shiftForm.fromStation.includes('维修'),
    message: '维修区返回需填写原因',
    trigger: 'blur'
  }]
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await getStopperPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: filterForm.keyword || undefined,
      spec: filterForm.spec || undefined,
      station: filterForm.station || undefined,
      equipment: filterForm.equipment || undefined,
      status: filterForm.status !== '' ? filterForm.status : undefined
    })
    tableData.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const loadSpecs = async () => {
  try {
    specs.value = await getAllSpecs()
  } catch (error) {
    console.error('加载规格列表失败')
  }
}

const loadFilterStations = async () => {
  try {
    filterStations.value = await getStationNames()
  } catch (error) {
    console.error('加载工位列表失败，使用挡块表工位兜底')
    try {
      filterStations.value = await getStopperStations()
    } catch (e) {
      console.error('加载工位列表失败')
    }
  }
}

const loadFormStations = async () => {
  try {
    formStations.value = await getStationNames()
  } catch (error) {
    console.error('加载工位列表失败，使用挡块表工位兜底')
    try {
      formStations.value = await getStopperStations()
    } catch (e) {
      console.error('加载工位列表失败')
    }
  }
}

const loadShiftStations = async () => {
  try {
    shiftStations.value = await getStationNames()
  } catch (error) {
    console.error('加载工位列表失败，使用挡块表工位兜底')
    try {
      shiftStations.value = await getStopperStations()
    } catch (e) {
      console.error('加载工位列表失败')
    }
  }
}

const remoteSearchStation = async (query) => {
  stationLoading.value = true
  try {
    filterStations.value = await getStationNames(query)
  } catch (error) {
    console.error('搜索工位失败')
  } finally {
    stationLoading.value = false
  }
}

const remoteSearchStationForm = async (query) => {
  formStationLoading.value = true
  try {
    formStations.value = await getStationNames(query)
  } catch (error) {
    console.error('搜索工位失败')
  } finally {
    formStationLoading.value = false
  }
}

const remoteSearchStationShift = async (query) => {
  shiftStationLoading.value = true
  try {
    shiftStations.value = await getStationNames(query)
  } catch (error) {
    console.error('搜索工位失败')
  } finally {
    shiftStationLoading.value = false
  }
}

const loadEquipments = async () => {
  try {
    equipments.value = await getAllEquipments()
  } catch (error) {
    console.error('加载设备列表失败')
  }
}

const remoteSearchEquipment = async (query) => {
  equipmentLoading.value = true
  try {
    const allEquipments = await getAllEquipments()
    if (query) {
      equipments.value = allEquipments.filter(e => e && e.toLowerCase().includes(query.toLowerCase()))
    } else {
      equipments.value = allEquipments
    }
  } catch (error) {
    console.error('搜索设备失败')
  } finally {
    equipmentLoading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.spec = ''
  filterForm.station = ''
  filterForm.equipment = ''
  filterForm.status = ''
  pageNum.value = 1
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增挡块'
  autoGenerate.value = true
  clearAllServerFieldErrors()
  stopperForm.stopperNo = ''
  loadFormStations()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑挡块'
  clearAllServerFieldErrors()
  originalAdaptEquipment.value = row.adaptEquipment || ''
  equipmentChanged.value = false
  Object.assign(stopperForm, {
    id: row.id,
    stopperNo: row.stopperNo,
    spec: row.spec,
    adaptEquipment: row.adaptEquipment,
    station: row.station,
    imageUrl: row.imageUrl || '',
    remark: row.remark || '',
    changeReason: '',
    operator: ''
  })
  loadFormStations()
  dialogVisible.value = true
}

const handleView = (row) => {
  router.push(`/stopper/detail/${row.id}`)
}

const resetForm = () => {
  clearAllServerFieldErrors()
  stopperForm.id = null
  stopperForm.stopperNo = ''
  stopperForm.spec = ''
  stopperForm.adaptEquipment = ''
  stopperForm.station = ''
  stopperForm.imageUrl = ''
  stopperForm.remark = ''
  stopperForm.changeReason = ''
  stopperForm.operator = ''
  originalAdaptEquipment.value = ''
  equipmentChanged.value = false
  autoGenerate.value = true
  formRef.value?.resetFields()
}

const handleGenerateNo = async () => {
  if (!stopperForm.spec) {
    ElMessage.warning('请先选择规格型号')
    return
  }
  generatingNo.value = true
  try {
    const no = await generateStopperNo(stopperForm.spec)
    stopperForm.stopperNo = no
    ElMessage.success('编号生成成功')
  } catch (error) {
    ElMessage.error(error?.message || '编号生成失败')
  } finally {
    generatingNo.value = false
  }
}

const handleSpecChange = () => {
  clearServerFieldError('spec')
  if (!isEdit.value && autoGenerate.value) {
    stopperForm.stopperNo = ''
    clearServerFieldError('stopperNo')
  }
}

const clearServerFieldError = (field) => {
  if (serverFieldErrors[field]) {
    serverFieldErrors[field] = ''
    formRef.value?.clearValidate?.([field])
  }
}

const clearAllServerFieldErrors = () => {
  Object.keys(serverFieldErrors).forEach((field) => {
    serverFieldErrors[field] = ''
  })
}

const handleSubmit = async () => {
  if (!formRef.value) return
  if (!isEdit.value && autoGenerate.value && !stopperForm.stopperNo) {
    ElMessage.warning('请点击"生成编号"按钮生成挡块编号')
    return
  }
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (stopperForm.station) {
          try {
            await ensureStation(stopperForm.station.trim())
          } catch (e) {
            console.warn('确保工位存在失败，可能已在挡块新增时自动处理')
          }
        }
        if (isEdit.value) {
          const submitData = {
            ...stopperForm
          }
          if (equipmentChanged.value) {
            const oldEq = (originalAdaptEquipment.value || '').trim()
            const newEq = (stopperForm.adaptEquipment || '').trim()
            if (oldEq === '' && newEq === '') {
              delete submitData.changeReason
              delete submitData.operator
            }
          } else {
            delete submitData.changeReason
            delete submitData.operator
          }
          await updateStopper(submitData)
          ElMessage.success('更新成功')
        } else {
          const submitData = {
            ...stopperForm,
            autoGenerate: autoGenerate.value
          }
          await addStopper(submitData)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadData()
        loadSpecs()
        loadFilterStations()
      } catch (error) {
        if (error.fieldErrors) {
          clearAllServerFieldErrors()
          const fieldNames = []
          let firstMessage = ''
          Object.keys(error.fieldErrors).forEach(field => {
            let message = error.fieldErrors[field]
            if (field === 'stopperNo' && error.data) {
              const statusText = error.data.status === 1 ? '正常' : '报废'
              message = `${message}，已存在挡块：规格 ${error.data.spec || '-'}，工位 ${error.data.station || '-'}，状态 ${statusText}`
            }
            if (Object.prototype.hasOwnProperty.call(serverFieldErrors, field)) {
              serverFieldErrors[field] = message
            }
            if (!firstMessage) {
              firstMessage = message
            }
            fieldNames.push(field)
          })
          if (equipmentChanged.value && !fieldNames.includes('changeReason') && serverFieldErrors.changeReason) {
            fieldNames.push('changeReason')
          }
          await nextTick()
          formRef.value?.validateField?.(fieldNames).catch(() => {})
          ElMessage.error(firstMessage)
        } else {
          ElMessage.error(isEdit.value ? '更新失败' : '添加失败')
        }
      }
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除挡块 ${row.stopperNo} 吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteStopper(row.id)
      ElMessage.success('删除成功')
      loadData()
      loadSpecs()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const handleShift = (row) => {
  shiftForm.stopperId = row.id
  shiftForm.stopperNo = row.stopperNo
  shiftForm.fromStation = row.station
  shiftForm.toStation = ''
  shiftForm.operator = ''
  shiftForm.shiftReason = ''
  loadShiftStations()
  shiftDialogVisible.value = true
}

const resetShiftForm = () => {
  shiftForm.stopperId = null
  shiftForm.stopperNo = ''
  shiftForm.fromStation = ''
  shiftForm.toStation = ''
  shiftForm.operator = ''
  shiftForm.shiftReason = ''
  shiftFormRef.value?.resetFields()
}

const handleShiftSubmit = async () => {
  if (!shiftFormRef.value) return
  await shiftFormRef.value.validate(async (valid) => {
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
        ElMessage.success('移位登记成功')
        shiftDialogVisible.value = false
        loadData()
        loadFilterStations()
      } catch (error) {
        if (error.fieldErrors) {
          const fields = []
          Object.keys(error.fieldErrors).forEach(field => {
            fields.push({
              field: field,
              message: error.fieldErrors[field]
            })
          })
          shiftFormRef.value.setFields(fields)
        }
      }
    }
  })
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadData()
}

const handlePageChange = (val) => {
  pageNum.value = val
  loadData()
}

onMounted(() => {
  loadData()
  loadSpecs()
  loadFilterStations()
  loadEquipments()
})
</script>

<style scoped>
.stopper-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-card,
.table-card {
  border-radius: 8px;
}

.filter-form {
  margin: 0;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.stopper-thumb {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  background: #f5f7fa;
  cursor: pointer;
}

.stopper-thumb :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.image-slot {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #909399;
  font-size: 11px;
  background: #f5f7fa;
  border-radius: 4px;
}

.image-slot .el-icon {
  font-size: 16px;
}

.no-image {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
