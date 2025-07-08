<template>
  <div>
    <div class="flex justify-between mb-2 items-center">
      <div class="flex gap-2">
        <Button label="새로고침" icon="pi pi-refresh" @click="refreshLogs" size="small" outlined />
        <Button label="Expand All" icon="pi pi-plus" @click="expandAll" size="small" text />
        <Button label="Collapse All" icon="pi pi-minus" @click="collapseAll" size="small" text />
      </div>
    </div>

    <DataTable
      v-model:expandedRows="expandedRows"
      :value="groupedLogs"
      dataKey="executionId"
      class="border rounded"
      size="small"
      stripedRows
      responsiveLayout="scroll"
      paginator
      :rows="10"
    >
      <Column expander style="width: 3rem" />
      <Column field="executionId" header="Execution ID" />
      <Column field="executedAt" header="트리거 시각">
        <template #body="{ data }">
          {{ data.logs[0]?.executedAt }}
        </template>
      </Column>
      <Column header="요약">
        <template #body="{ data }">
          <Tag class="mr-1" severity="success">성공: {{ countStatus(data.logs, 'SUCCESS') }}</Tag>
          <Tag class="mr-1" severity="info">진행중: {{ countStatus(data.logs, 'RUNNING') }}</Tag>
          <Tag severity="danger">실패: {{ countStatus(data.logs, 'FAIL') }}</Tag>
        </template>
      </Column>

      <template #expansion="{ data }">
        <DataTable :value="data.logs" dataKey="jobId" size="small">
          <Column field="executedAt" header="실행시각" />
          <Column field="status" header="상태">
            <template #body="{ data }">
              <Tag :value="data.status" :severity="statusSeverity(data.status)" />
            </template>
          </Column>
          <Column field="jobId" header="JOB ID">
            <template #body="{ data }">
              <span
                class="clickable-jobid"
                style="color: #0073e6; cursor: pointer; font-weight: bold;"
                @click="openBatchLog(data)"
              >{{ data.jobId }}</span>
            </template>
          </Column>
          <Column field="jobName" header="JOB명" />
          <Column field="message" header="메시지">
            <template #body="{ data }">
              <div
                class="message-cell"
                :title="data.message"
                @click="openMessageDialog(data.message)"
                v-html="formatMessageShort(data.message)"
              ></div>
            </template>
          </Column>
        </DataTable>
      </template>
    </DataTable>

    <!-- 상세 실행 로그 모달 -->
    <Dialog v-model:visible="showBatchLogDialog" header="실행 로그 상세" width="700" modal>
      <ETLJobLog
        v-if="showBatchLogDialog"
        :jobId="selectedJobId"
        @close="showBatchLogDialog = false"
      />
    </Dialog>

    <!-- 메시지 전체 보기 Dialog -->
    <Dialog v-model:visible="showMessageDialog" header="전체 메시지" style="width: 500px;">
      <pre style="white-space: pre-wrap;">{{ selectedMessage }}</pre>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import api from "@/api"
import { Tag, DataTable, Column, Dialog } from 'primevue'
import Button from 'primevue/button'
import ETLJobLog from './ETLJobLog.vue'

const props = defineProps({ schedule: Object })

const logs = ref([])
const groupedLogs = computed(() => {
  const groups = {}
  for (const log of logs.value) {
    const execId = log.executionId || 'NO_EXECUTION_ID'
    if (!groups[execId]) groups[execId] = { executionId: execId, logs: [] }
    groups[execId].logs.push(log)
  }
  return Object.values(groups)
})

const expandedRows = ref([])
const showBatchLogDialog = ref(false)
const selectedJobId = ref(null)
const selectedBatchId = ref(null)
const showMessageDialog = ref(false)
const selectedMessage = ref('')
const loading = ref(false)

function openBatchLog(row) {
  if (!row.jobId) return
  selectedJobId.value = row.jobId
  showBatchLogDialog.value = true
}

function openMessageDialog(msg) {
  selectedMessage.value = msg
  showMessageDialog.value = true
}

function expandAll() {
  expandedRows.value = groupedLogs.value.map(g => g.executionId)
}

function collapseAll() {
  expandedRows.value = []
}

async function fetchLogs(page = 0, size = 10) {
  loading.value = true
  try {
    const res = await api.get(`/api/etl/schedule-logs/${props.schedule.scheduleId}/logs`, {
      params: { page, size }
    })
    logs.value = Array.isArray(res.data.content) ? res.data.content : []
    console.table(logs.value.map(l => ({ jobId: l.jobId, batchId: l.batchId, executionId: l.executionId })))

    logs.value.sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt))
  } finally {
    loading.value = false
  }
}

watch(() => props.schedule, (val) => {
  if (val) fetchLogs(0, 10)
}, { immediate: true })

function statusSeverity(status) {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAIL': return 'danger'
    default: return 'info'
  }
}

function formatMessageShort(msg) {
  if (!msg) return ''
  const lines = msg.split('\n').slice(0, 2)
  let short = lines.join('<br>')
  if (msg.split('\n').length > 2 || msg.length > 50) short += ' ...'
  return short
}

function countStatus(logList, status) {
  return logList.filter(log => log.status === status).length
}
</script>

<style>
.message-cell {
  max-width: 250px;
  white-space: pre-line;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  cursor: pointer;
}
</style>