<template>
  <div>
    <Accordion :activeIndex="0">
      <AccordionTab
        v-for="(group, executionId, idx) in groupedLogs"
        :key="executionId"
        :header="triggerHeader(executionId, group)"
      >
        <div class="flex gap-2 mb-2 text-xs">
          <Tag :value="'성공: ' + countStatus(group, 'SUCCESS')" severity="success" />
          <Tag :value="'진행중: ' + countStatus(group, 'RUNNING')" severity="info" />
          <Tag :value="'실패: ' + countStatus(group, 'FAIL')" severity="danger" />
          <span class="ml-auto text-gray-400">실행 수: {{ group.length }}</span>
        </div>
        <DataTable :value="group" class="border rounded" size="small">
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
      </AccordionTab>
    </Accordion>
    <!-- 상세 실행 로그 모달 -->
    <Dialog v-model:visible="showBatchLogDialog" header="실행 로그 상세" width="700" modal>
      <ETLJobLog
        v-if="showBatchLogDialog"
        :jobId="selectedJobId"
        :batchId="selectedBatchId"
        @close="showBatchLogDialog = false"
      />
    </Dialog>
    <Dialog v-model:visible="showMessageDialog" header="전체 메시지" style="width: 500px;">
      <pre style="white-space: pre-wrap;">{{ selectedMessage }}</pre>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from "@/api"
import { Tag, DataTable, Column, Dialog } from 'primevue'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import ETLJobLog from './ETLJobLog.vue'

const props = defineProps({ schedule: Object })

const showBatchLogDialog = ref(false)
const selectedJobId = ref(null)
const selectedBatchId = ref(null)
const showMessageDialog = ref(false)
const selectedMessage = ref('')

const logs = ref([])
const groupedLogs = ref({})
const loading = ref(false)

function openBatchLog(row) {
  selectedJobId.value = row.jobId
  selectedBatchId.value = row.batchId || row.executionId || row.executedAt // 필요에 따라 선택
  showBatchLogDialog.value = true
}

async function fetchLogs(page = 0, size = 10) {
  loading.value = true
  const res = await api.get(`/api/etl/schedule-logs/${props.schedule.scheduleId}/logs`, {
    params: { page, size }
  })
  logs.value = Array.isArray(res.data.content) ? res.data.content : []
  logs.value.sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt))
  groupLogsByExecutionId()
  loading.value = false
}

watch(() => props.schedule, (val) => { if (val) fetchLogs(0, 10) }, { immediate: true })

function groupLogsByExecutionId() {
  const arr = Array.isArray(logs.value) ? logs.value : []
  groupedLogs.value = arr.reduce((acc, log) => {
    const execId =
      log.executionId ||
      log.ExecutionId ||
      log.EXECUTION_ID ||
      'NO_EXECUTION_ID'
    if (!acc[execId]) acc[execId] = []
    acc[execId].push(log)
    return acc
  }, {})
}

function openMessageDialog(msg) {
  selectedMessage.value = msg
  showMessageDialog.value = true
}

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

function countStatus(group, status) {
  return group.filter(log => log.status === status).length
}

function triggerHeader(executionId, group) {
  const last = group[0]
  const allStatus = [...new Set(group.map(g => g.status))].join(', ')
  return `트리거: ${executionId} | 실행시각: ${last.executedAt} | 상태: ${allStatus}`
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
