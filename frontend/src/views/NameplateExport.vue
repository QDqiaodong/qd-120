<template>
  <div class="nameplate-export">
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
            style="width: 160px"
          >
            <el-option v-for="s in stations" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="适配设备">
          <el-select
            v-model="filterForm.equipment"
            placeholder="全部设备"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option v-for="e in equipments" :key="e" :label="e" :value="e" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="select-card">
          <template #header>
            <div class="card-header">
              <span>选择挡块</span>
              <span class="selected-count">已选 {{ selectedIds.length }} 项</span>
            </div>
          </template>
          <el-table
            ref="selectTableRef"
            :data="tableData"
            v-loading="loading"
            border
            stripe
            height="500"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column prop="stopperNo" label="挡块编号" width="120" />
            <el-table-column prop="spec" label="规格型号" width="130" />
            <el-table-column prop="adaptEquipment" label="适配设备" min-width="150" />
            <el-table-column prop="station" label="存放工位" width="130" />
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
      </el-col>

      <el-col :span="12">
        <el-card class="preview-card">
          <template #header>
            <div class="card-header">
              <span>铭牌数据预览</span>
              <div class="header-actions">
                <el-button
                  type="primary"
                  :disabled="nameplateData.length === 0"
                  @click="handleGenerate"
                >
                  <el-icon><Document /></el-icon>生成铭牌
                </el-button>
                <el-button
                  type="success"
                  :disabled="nameplateData.length === 0"
                  @click="handlePrint"
                >
                  <el-icon><Printer /></el-icon>打印
                </el-button>
              </div>
            </div>
          </template>
          <div v-if="nameplateData.length === 0" class="empty-tip">
            <el-empty description="请在左侧选择挡块后点击生成铭牌" />
          </div>
          <div v-else class="nameplate-preview">
            <el-table :data="nameplateData" border stripe height="500">
              <el-table-column prop="stopperNo" label="编号" width="140" />
              <el-table-column prop="spec" label="规格" width="130" />
              <el-table-column prop="adaptEquipment" label="适配设备" min-width="150" />
              <el-table-column prop="station" label="工位" width="130" />
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="printDialogVisible"
      title="打印预览"
      width="900px"
      top="5vh"
    >
      <div class="print-area" id="printArea">
        <div class="print-title">挡块铭牌明细</div>
        <table class="print-table">
          <thead>
            <tr>
              <th style="width: 25%">编号</th>
              <th style="width: 20%">规格</th>
              <th style="width: 35%">适配设备</th>
              <th style="width: 20%">工位</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in nameplateData" :key="item.id">
              <td>{{ item.stopperNo }}</td>
              <td>{{ item.spec }}</td>
              <td>{{ item.adaptEquipment || '-' }}</td>
              <td>{{ item.station }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <template #footer>
        <el-button @click="printDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="confirmPrint">
          <el-icon><Printer /></el-icon>确认打印
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  getStopperPage,
  getAllStations as getStopperStations,
  getAllSpecs,
  getAllEquipments,
  getNameplates
} from '@/api/stopper'
import { getStationNames } from '@/api/station'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Document, Printer } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const specs = ref([])
const stations = ref([])
const equipments = ref([])
const selectedIds = ref([])
const selectedRows = ref([])
const selectTableRef = ref(null)
const nameplateData = ref([])
const printDialogVisible = ref(false)

const filterForm = reactive({
  keyword: '',
  spec: '',
  station: '',
  equipment: ''
})

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
      status: 1
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

const loadStations = async () => {
  try {
    stations.value = await getStationNames()
  } catch (error) {
    try {
      stations.value = await getStopperStations()
    } catch (e) {
      console.error('加载工位列表失败')
    }
  }
}

const loadEquipments = async () => {
  try {
    equipments.value = await getAllEquipments()
  } catch (error) {
    console.error('加载设备列表失败')
  }
}

const handleSearch = () => {
  pageNum.value = 1
  selectedIds.value = []
  selectedRows.value = []
  nameplateData.value = []
  loadData()
}

const handleReset = () => {
  filterForm.keyword = ''
  filterForm.spec = ''
  filterForm.station = ''
  filterForm.equipment = ''
  pageNum.value = 1
  selectedIds.value = []
  selectedRows.value = []
  nameplateData.value = []
  loadData()
}

const handleSelectionChange = (selection) => {
  selectedRows.value = selection
  selectedIds.value = selection.map(item => item.id)
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadData()
}

const handlePageChange = (val) => {
  pageNum.value = val
  loadData()
}

const handleGenerate = async () => {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择挡块')
    return
  }
  try {
    const data = await getNameplates(selectedIds.value)
    nameplateData.value = data || []
    ElMessage.success(`已生成 ${nameplateData.value.length} 条铭牌数据`)
  } catch (error) {
    ElMessage.error('生成铭牌数据失败')
  }
}

const handlePrint = () => {
  if (nameplateData.value.length === 0) {
    ElMessage.warning('请先生成铭牌数据')
    return
  }
  printDialogVisible.value = true
}

const confirmPrint = () => {
  const printContent = document.getElementById('printArea').innerHTML
  const printWindow = window.open('', '_blank')
  printWindow.document.write(`
    <html>
      <head>
        <title>挡块铭牌明细</title>
        <style>
          body { font-family: 'Microsoft YaHei', Arial, sans-serif; padding: 20px; }
          .print-title { text-align: center; font-size: 24px; font-weight: bold; margin-bottom: 20px; }
          .print-table { width: 100%; border-collapse: collapse; }
          .print-table th, .print-table td {
            border: 1px solid #333;
            padding: 10px;
            text-align: center;
            font-size: 14px;
          }
          .print-table th { background: #f0f0f0; font-weight: bold; }
          @media print {
            body { padding: 0; }
          }
        </style>
      </head>
      <body>${printContent}</body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
  }, 300)
}

onMounted(() => {
  loadData()
  loadSpecs()
  loadStations()
  loadEquipments()
})
</script>

<style scoped>
.nameplate-export {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-card,
.select-card,
.preview-card {
  border-radius: 8px;
}

.filter-form {
  margin: 0;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-count {
  color: #409eff;
  font-size: 13px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.empty-tip {
  padding: 60px 0;
}

.print-area {
  padding: 10px;
}

.print-title {
  text-align: center;
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 20px;
}

.print-table {
  width: 100%;
  border-collapse: collapse;
}

.print-table th,
.print-table td {
  border: 1px solid #333;
  padding: 10px;
  text-align: center;
  font-size: 14px;
}

.print-table th {
  background: #f0f0f0;
  font-weight: bold;
}
</style>
