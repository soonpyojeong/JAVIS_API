<template>
  <div>
    <div v-if="!jobId" class="p-8 text-center text-gray-400">JOB을 선택해 주세요.</div>
    <template v-else>
      <div class="flex justify-end mb-2">
        <Button label="새로고침" icon="pi pi-refresh" @click="refreshLogs" size="small" outlined />
      </div>
      <Accordion multiple>
        <AccordionTab
          v-for="batch in batchLogs"
          :key="batch.batchId"
          :header="`[${formatDate(batch.executedAt)}] 배치번호: ${batch.batchId} / 결과: ${getBatchStatus(batch)}`">
          <table class="w-full border mb-2">
            <thead>
              <tr>
                <th>Source DB</th>
                <th>실행시각</th>
                <th>상태</th>
                <th>메시지</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in batch.logs" :key="log.logId">
                <td>{{ log.sourceDbName }}</td>
                <td>{{ formatDate(log.executedAt) }}</td>
                <td>
                  <Tag :severity="log.result === 'FAIL' ? 'danger' : 'success'">{{ log.result }}</Tag>
                </td>
                <td>{{ log.message }}</td>
                <td>
                  <Button
                    v-if="log.result === 'FAIL'"
                    label="재수행"
                    icon="pi pi-refresh"
                    severity="danger"
                    size="small"
                    @click="$emit('retry', log)"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </AccordionTab>
      </Accordion>
      <div v-if="batchLogs.length === 0" class="text-center text-gray-400 py-8">로그가 없습니다.</div>
    </template>
    <div class="flex justify-end mt-4">
      <Button label="닫기" @click="$emit('close')" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '@/api'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'
import { useToast } from 'primevue/usetoast'

const props = defineProps({ jobId: Number, batchId: [Number, String] })

const emit = defineEmits(['retry', 'close'])

const batchLogs = ref([])
const toast = useToast()

watch(() => props.jobId, (val) => {
  if (val) {
    fetchBatchLogs(val)
  } else {
    batchLogs.value = []
  }
}, { immediate: true })

async function fetchBatchLogs(jobId) {
  try {
    const { data } = await api.get(`/api/etl/job/${jobId}/batch-logs`)
    batchLogs.value = data
  } catch (e) {
    batchLogs.value = []
    toast.add({ severity: 'error', summary: '실패', detail: '로그 조회 실패', life: 3000 })
  }
}

function refreshLogs() {
  if (props.jobId) fetchBatchLogs(props.jobId)
}

function getBatchStatus(batch) {
  return (batch.logs || []).some(log => log.result === 'FAIL') ? 'FAIL' : 'SUCCESS'
}

function formatDate(str) {
  if (!str) return ''
  return new Date(str).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' })
}
</script>
