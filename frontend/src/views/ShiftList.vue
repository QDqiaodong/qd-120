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
        <el-table-column prop="fromStation" label="原工位" width="140" />
        <el-table-column prop="toStation" label="目标工位" width="140" />
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
          <el-input v-model="shiftForm.toStation" placeholder="请输入目标工位" />
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
import { getStopperList, getStopperById } from '@/api/stopper'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const stopperList = ref([])

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
        await addShift(shiftForm)
        ElMessage.success('登记成功')
        dialogVisible.value = false
        loadData()
        loadStoppers()
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
</style>
