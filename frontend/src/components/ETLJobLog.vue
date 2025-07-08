<template>
  <div>
    <div v-if="!jobId" class="p-8 text-center text-gray-400">JOB을 선택해 주세요.</div>
    <template v-else>
      <div class="flex justify-between mb-2 items-center">
        <div class="flex gap-2">
          <Button label="새로고침" icon="pi pi-refresh" @click="refreshLogs" size="small" outlined />
          <Button label="Expand All" icon="pi pi-plus" @click="expandAll" size="small" text />
          <Button label="Collapse All" icon="pi pi-minus" @click="collapseAll" size="small" text />
        </div>
        <div class="flex items-center gap-2">
          <InputText v-model="searchText" placeholder="검색 (배치번호/시간)" size="small" />
          <Checkbox v-model="failOnly" :binary="true" />
          <label class="text-sm">FAIL만 보기</label>
        </div>
      </div>

      <DataTable
        :value="batchLogs"
        v-model:expandedRows="expandedRows"
        :totalRecords="totalRecords"
        dataKey="batchId"
        paginator
        :rows="10"
        lazy
        @page="onPage"
        stripedRows
        responsiveLayout="scroll"
        class="custom-datatable"
      >
        <Column expander style="width: 3rem" />
        <Column field="batchId" header="배치번호" sortable />
        <Column field="executedAt" header="실행시각" sortable>
          <template #body="slotProps">
            {{ formatDate(slotProps.data.executedAt) }}
          </template>
        </Column>
        <Column header="결과">
          <template #body="slotProps">
            <Tag :severity="getBatchStatus(slotProps.data) === 'FAIL' ? 'danger' : 'success'">
              {{ getBatchStatus(slotProps.data) }}
            </Tag>
          </template>
        </Column>

        <template #expansion="slotProps">
          <transition name="fade">
            <div class="p-3 border-t">
              <table class="w-full border text-sm">
                <thead>
                  <tr>
                    <th>Source DB</th>
                    <th>실행시각</th>
                    <th>상태</th>
                    <th style="width: 300px">메시지</th>
                    <th>작업</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="log in slotProps.data.logs" :key="log.logId">
                    <td>{{ log.sourceDbName }}</td>
                    <td>{{ formatDate(log.executedAt) }}</td>
                    <td>
                      <Tag :severity="log.result === 'FAIL' ? 'danger' : 'success'">{{ log.result }}</Tag>
                    </td>
                    <td style="white-space: pre-wrap; word-break: break-word;">
                      {{ log.message }}
                    </td>
                    <td>
                      <Button
                        v-if="log.result === 'FAIL'"
                        label="재수행"
                        icon="pi pi-refresh"
                        size="small"
                        severity="danger"
                        @click="$emit('retry', log)"
                      />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </transition>
        </template>
      </DataTable>

      <div v-if="batchLogs.length === 0" class="text-center text-gray-400 py-8">로그가 없습니다.</div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '@/api'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Checkbox from 'primevue/checkbox'
import { useToast } from 'primevue/usetoast'

const props = defineProps({ jobId: Number })
const emit = defineEmits(['retry', 'close'])

const batchLogs = ref([])
const totalRecords = ref(0)
const expandedRows = ref({})
const searchText = ref('')
const failOnly = ref(false)
const toast = useToast()

const lazyParams = ref({ page: 0, rows: 10 })

watch(() => props.jobId, (val) => {
  if (val) {
    lazyParams.value.page = 0 // 새로 선택되면 1페이지부터
    fetchBatchLogs()
  } else {
    batchLogs.value = []
    totalRecords.value = 0
    expandedRows.value = {}
  }
}, { immediate: true })

function onPage(event) {
  lazyParams.value = event
  fetchBatchLogs()
}

async function fetchBatchLogs() {
  try {
    const { page, rows } = lazyParams.value
    const { data } = await api.get(`/api/etl/job/${props.jobId}/batch-logs`, {
      params: { page, size: rows, keyword: searchText.value, failOnly: failOnly.value }
    })
    const sorted = data.content.sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt))
    batchLogs.value = sorted
    totalRecords.value = data.totalElements
  } catch (e) {
    batchLogs.value = []
    totalRecords.value = 0
    toast.add({ severity: 'error', summary: '실패', detail: '로그 조회 실패', life: 3000 })
  }
}

function refreshLogs() {
  fetchBatchLogs()
}

function formatDate(str) {
  if (!str) return ''
  return new Date(str).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' })
}

function getBatchStatus(batch) {
  return (batch.logs || []).some(log => log.result === 'FAIL') ? 'FAIL' : 'SUCCESS'
}

function expandAll() {
  expandedRows.value = Object.fromEntries(batchLogs.value.map(b => [b.batchId, true]))
}
function collapseAll() {
  expandedRows.value = {}
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
