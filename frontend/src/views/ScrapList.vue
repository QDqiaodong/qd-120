<template>
  <div class="scrap-list">
    <el-card class="header-card">
      <div class="header-content">
        <h2 class="page-title">磨损挡块报废归档</h2>
        <el-button type="danger" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增报废
        </el-button>
      </div>
    </el-card>

    <WearLevelLegend ref="wearLegendRef" />

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="stopperNo" label="挡块编号" width="130" />
        <el-table-column prop="spec" label="规格型号" width="130" />
        <el-table-column prop="scrapDegree" label="磨损程度" width="130">
          <template #default="{ row }">
            <el-tag
              :type="wearLegendRef?.getWearLevelTagType(row.scrapDegree) || 'info'"
              size="small"
              effect="light"
            >
              <span
                class="degree-dot"
                :style="{ background: wearLegendRef?.getWearLevelColor(row.scrapDegree) || '#909399' }"
              ></span>
              {{ row.scrapDegree }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scrapReason" label="报废原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="scrapTime" label="报废时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.scrapTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150">
          <template #default="{ row }">
            <el-popover
              v-if="row.remark && row.remark.length > 15"
              placement="top"
              trigger="hover"
              :width="320"
            >
              <template #reference>
                <span class="remark-text remark-ellipsis">{{ row.remark }}</span>
              </template>
              <div class="remark-popover-content">
                <div class="popover-title">备注详情</div>
                <div class="popover-text">{{ row.remark }}</div>
              </div>
            </el-popover>
            <span v-else class="remark-text">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新增报废登记" width="550px" @close="resetForm">
      <el-form :model="scrapForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="挡块编号" prop="stopperId">
          <el-select
            v-model="scrapForm.stopperId"
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
        <el-form-item label="规格型号">
          <el-input :value="stopperSpec" disabled placeholder="选择挡块后自动显示" />
        </el-form-item>
        <el-form-item label="磨损程度" prop="scrapDegree">
          <el-select v-model="scrapForm.scrapDegree" placeholder="请选择磨损程度" style="width: 100%">
            <el-option label="轻微磨损" value="轻微" />
            <el-option label="中度磨损" value="中度" />
            <el-option label="严重磨损" value="严重" />
            <el-option label="断裂损坏" value="断裂" />
            <el-option label="完全损坏" value="完全损坏" />
          </el-select>
        </el-form-item>
        <el-form-item label="报废原因" prop="scrapReason">
          <el-input v-model="scrapForm.scrapReason" type="textarea" :rows="3" placeholder="请输入报废原因" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="scrapForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="scrapForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleSubmit">确认报废</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="报废详情" width="550px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="挡块编号">{{ viewData.stopperNo }}</el-descriptions-item>
        <el-descriptions-item label="规格型号">{{ viewData.spec }}</el-descriptions-item>
        <el-descriptions-item label="磨损程度">
          <el-tag
            :type="wearLegendRef?.getWearLevelTagType(viewData.scrapDegree) || 'info'"
            size="small"
            effect="light"
          >
            <span
              class="degree-dot"
              :style="{ background: wearLegendRef?.getWearLevelColor(viewData.scrapDegree) || '#909399' }"
            ></span>
            {{ viewData.scrapDegree }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ viewData.operator }}</el-descriptions-item>
        <el-descriptions-item label="报废时间" :span="2">
          {{ formatDate(viewData.scrapTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="报废原因" :span="2">
          {{ viewData.scrapReason }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ viewData.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <div class="legend-section" style="margin-top: 20px">
        <WearLevelLegend :highlight="viewData.scrapDegree" />
      </div>
      <template #footer>
        <el-button type="primary" @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getScrapList, addScrap } from '@/api/scrap'
import { getStopperList } from '@/api/stopper'
import { ElMessage } from 'element-plus'
import WearLevelLegend from '@/components/WearLevelLegend.vue'
import { eventBus, EVENTS } from '@/utils/eventBus'

const wearLegendRef = ref(null)

const loading = ref(false)
const tableData = ref([])
const stopperList = ref([])
const stopperSpec = ref('')

const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const formRef = ref(null)
const viewData = ref({})

const scrapForm = reactive({
  stopperId: null,
  stopperNo: '',
  scrapReason: '',
  scrapDegree: '',
  operator: '',
  remark: ''
})

const formRules = {
  stopperId: [{ required: true, message: '请选择挡块', trigger: 'change' }],
  scrapReason: [{ required: true, message: '请输入报废原因', trigger: 'blur' }],
  scrapDegree: [{ required: true, message: '请选择磨损程度', trigger: 'change' }]
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await getScrapList()
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

const handleStopperChange = (stopperId) => {
  if (stopperId) {
    const stopper = stopperList.value.find(s => s.id === stopperId)
    if (stopper) {
      stopperSpec.value = stopper.spec
      scrapForm.stopperNo = stopper.stopperNo
    }
  } else {
    stopperSpec.value = ''
    scrapForm.stopperNo = ''
  }
}

const handleAdd = () => {
  dialogVisible.value = true
}

const resetForm = () => {
  scrapForm.stopperId = null
  scrapForm.stopperNo = ''
  scrapForm.scrapReason = ''
  scrapForm.scrapDegree = ''
  scrapForm.operator = ''
  scrapForm.remark = ''
  stopperSpec.value = ''
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const scrappedStopper = await addScrap(scrapForm)
        ElMessage.success('报废登记成功')
        dialogVisible.value = false
        loadData()
        loadStoppers()
        if (scrappedStopper && scrappedStopper.id) {
          eventBus.emit(EVENTS.STOPPER_SCRAPPED, scrappedStopper)
        }
      } catch (error) {
        ElMessage.error('登记失败')
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
.scrap-list {
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

.degree-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.remark-text {
  display: inline-block;
  vertical-align: middle;
  color: #606266;
  font-size: 13px;
}

.remark-ellipsis {
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: help;
}

.remark-popover-content {
  font-size: 13px;
  line-height: 1.6;
}

.remark-popover-content .popover-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px;
}

.remark-popover-content .popover-text {
  color: #606266;
  word-break: break-all;
  white-space: pre-wrap;
}
</style>
