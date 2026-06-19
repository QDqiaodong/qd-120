<template>
  <div class="inventory-list">
    <el-card class="header-card">
      <div class="header-content">
        <h2 class="page-title">月度资产清点</h2>
        <el-button type="primary" @click="handleStart">
          <el-icon><Plus /></el-icon>发起盘点
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="inventoryNo" label="盘点单号" width="170" />
        <el-table-column prop="inventoryMonth" label="盘点月份" width="110" />
        <el-table-column prop="totalCount" label="账面数量" width="100" align="center" />
        <el-table-column prop="actualCount" label="实盘数量" width="100" align="center" />
        <el-table-column prop="diffCount" label="差异数量" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'diff-positive': row.diffCount > 0 }">{{ row.diffCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="inventoryStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.inventoryStatus === 'COMPLETED' ? 'success' : 'warning'" size="small">
              {{ row.inventoryStatus === 'COMPLETED' ? '已完成' : '进行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="inventoryTime" label="盘点时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.inventoryTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDetail(row)">
              {{ row.inventoryStatus === 'COMPLETED' ? '查看' : '盘点' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="startDialogVisible" title="发起月度盘点" width="450px" @close="resetStartForm">
      <el-form :model="startForm" :rules="startRules" ref="startFormRef" label-width="100px">
        <el-form-item label="盘点月份" prop="inventoryMonth">
          <el-date-picker
            v-model="startForm.inventoryMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="startForm.operator" placeholder="请输入操作人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStartSubmit">确认发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { getInventoryList, startInventory } from '@/api/inventory'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])

const startDialogVisible = ref(false)
const startFormRef = ref(null)
const startForm = reactive({
  inventoryMonth: '',
  operator: ''
})

const startRules = {
  inventoryMonth: [{ required: true, message: '请选择盘点月份', trigger: 'change' }]
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await getInventoryList()
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleStart = () => {
  startDialogVisible.value = true
}

const resetStartForm = () => {
  startForm.inventoryMonth = ''
  startForm.operator = ''
  startFormRef.value?.resetFields()
}

const handleStartSubmit = async () => {
  if (!startFormRef.value) return
  await startFormRef.value.validate(async (valid) => {
    if (valid) {
      const existingProcessing = tableData.value.find(
        (item) => item.inventoryMonth === startForm.inventoryMonth && item.inventoryStatus === 'PROCESSING'
      )
      if (existingProcessing) {
        ElMessage.warning(`${startForm.inventoryMonth} 已有进行中的盘点单，请先完成后再发起`)
        return
      }
      try {
        const result = await startInventory(startForm)
        ElMessage.success('盘点发起成功')
        startDialogVisible.value = false
        router.push(`/inventory/detail/${result.id}`)
      } catch (error) {
        ElMessage.error(error?.message || '发起失败')
      }
    }
  })
}

const viewDetail = (row) => {
  router.push(`/inventory/detail/${row.id}`)
}

onMounted(() => {
  loadData()
})

onActivated(() => {
  loadData()
})
</script>

<style scoped>
.inventory-list {
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

.diff-positive {
  color: #f56c6c;
  font-weight: 600;
}
</style>
