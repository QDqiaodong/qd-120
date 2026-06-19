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
        <el-form-item label="工位">
          <el-select v-model="filterForm.station" placeholder="全部工位" clearable style="width: 160px">
            <el-option v-for="s in stations" :key="s" :label="s" :value="s" />
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
        <el-form-item label="挡块编号" prop="stopperNo">
          <el-input v-model="stopperForm.stopperNo" placeholder="请输入挡块编号" />
        </el-form-item>
        <el-form-item label="规格型号" prop="spec">
          <el-input v-model="stopperForm.spec" placeholder="请输入规格型号" />
        </el-form-item>
        <el-form-item label="适配设备" prop="adaptEquipment">
          <el-input v-model="stopperForm.adaptEquipment" placeholder="请输入适配设备" />
        </el-form-item>
        <el-form-item label="存放工位" prop="station">
          <el-input v-model="stopperForm.station" placeholder="请输入存放工位" />
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
          <el-input v-model="shiftForm.toStation" placeholder="请输入目标工位" />
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
import { ref, reactive, onMounted } from 'vue'
import { getStopperPage, getAllStations, addStopper, updateStopper, deleteStopper } from '@/api/stopper'
import { addShift } from '@/api/shift'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const stations = ref([])

const filterForm = reactive({
  keyword: '',
  station: '',
  status: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增挡块')
const isEdit = ref(false)
const formRef = ref(null)

const stopperForm = reactive({
  id: null,
  stopperNo: '',
  spec: '',
  adaptEquipment: '',
  station: '',
  imageUrl: '',
  remark: ''
})

const formRules = {
  stopperNo: [{ required: true, message: '请输入挡块编号', trigger: 'blur' }],
  spec: [{ required: true, message: '请输入规格型号', trigger: 'blur' }],
  station: [{ required: true, message: '请输入存放工位', trigger: 'blur' }]
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
      station: filterForm.station || undefined,
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

const loadStations = async () => {
  try {
    stations.value = await getAllStations()
  } catch (error) {
    console.error('加载工位列表失败')
  }
}

const handleSearch = () => {
  pageNum.value = 1
  loadData()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.station = ''
  filterForm.status = ''
  pageNum.value = 1
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增挡块'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑挡块'
  Object.assign(stopperForm, {
    id: row.id,
    stopperNo: row.stopperNo,
    spec: row.spec,
    adaptEquipment: row.adaptEquipment,
    station: row.station,
    imageUrl: row.imageUrl || '',
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const handleView = (row) => {
  ElMessageBox.alert(
    `挡块编号: ${row.stopperNo}\n规格: ${row.spec}\n适配设备: ${row.adaptEquipment}\n工位: ${row.station}\n状态: ${row.status === 1 ? '正常' : '报废'}`,
    '挡块详情',
    { confirmButtonText: '确定' }
  )
}

const resetForm = () => {
  stopperForm.id = null
  stopperForm.stopperNo = ''
  stopperForm.spec = ''
  stopperForm.adaptEquipment = ''
  stopperForm.station = ''
  stopperForm.imageUrl = ''
  stopperForm.remark = ''
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (isEdit.value) {
          await updateStopper(stopperForm)
          ElMessage.success('更新成功')
        } else {
          await addStopper(stopperForm)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        if (error.fieldErrors) {
          const fields = {}
          Object.keys(error.fieldErrors).forEach(field => {
            fields[field] = {
              message: error.fieldErrors[field],
              field: field
            }
          })
          formRef.value.setFields(fields)
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
        await addShift(shiftForm)
        ElMessage.success('移位登记成功')
        shiftDialogVisible.value = false
        loadData()
      } catch (error) {
        if (error.fieldErrors) {
          const fields = {}
          Object.keys(error.fieldErrors).forEach(field => {
            fields[field] = {
              message: error.fieldErrors[field],
              field: field
            }
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
  loadStations()
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
