<template>
  <div class="scheduler-container">
    <div class="toolbar flex gap-2 mb-4">
      <Button label="스케줄 등록" icon="pi pi-plus" @click="openDialog" />
      <Button
        label="새로고침"
        icon="pi pi-refresh"
        :loading="loading"
        :disabled="loading"
        outlined
        @click="handleRefresh"
      />
    </div>

    <DataTable
      :key="tableKey"
      :value="displaySchedules"
      :paginator="true"
      :rows="10"
      :loading="loading"
      class="shadow text-xs compact-table"
      style="min-width:720px;"
      :pt="{
        bodyRow: { style: 'height:32px;' },
        cell: { style: 'padding:2px 8px;' }
      }"
    >
      <Column field="scheduleId" header="SCH_ID" />
      <Column field="name" header="스케줄 이름" />
      <Column field="type" header="유형" />
      <Column field="times" header="스케줄시간">
        <template #body="{data}">
          {{ Array.isArray(data.times) ? data.times.join(', ') : data.times }}
        </template>
      </Column>
      <Column field="startDate" header="시작일">
        <template #body="{data}">
          {{ formatDate(data.startDate) }}
        </template>
      </Column>
      <Column field="endDate" header="종료일">
        <template #body="{data}">
          {{ formatDate(data.endDate) }}
        </template>
      </Column>
      <Column field="status" header="상태">
        <template #body="{data}">
          <Tag :value="data.active ? 'Active' : 'Inactive'" :severity="data.active ? 'success' : 'secondary'" />
        </template>
      </Column>
      <Column field="jobs" header="연결된 JOB">
        <template #body="{ data }">
          <template v-if="Array.isArray(data.jobs) && data.jobs.length > 1">
            {{ data.jobs[0] }} 외 {{ data.jobs.length - 1 }}건
          </template>
          <template v-else-if="Array.isArray(data.jobs)">
            {{ data.jobs[0] }}
          </template>
          <template v-else>
            {{ data.jobs || '' }}
          </template>
        </template>
      </Column>
      <Column header="액션" :exportable="false" style="min-width:140px">
        <template #body="{data}">
          <Button icon="pi pi-pencil" text rounded @click="editSchedule(data)" />
          <Button icon="pi pi-trash" text rounded severity="danger" @click="deleteSchedule(data)" />
        </template>
      </Column>
      <Column header="스케줄 로그">
        <template #body="{ data }">
          <Button icon="pi pi-history" text @click="openLogDialog(data)" />
        </template>
      </Column>
    </DataTable>

    <!-- 스케줄 등록/수정 Dialog -->
    <Dialog v-model:visible="dialogVisible" modal header="스케줄 등록/수정">
      <SchedulerDialog :editData="editData" @save="saveSchedule" @cancel="closeDialog" />
    </Dialog>

    <!-- 로그 Dialog -->
    <Dialog v-model:visible="logDialogVisible" modal header="실행 로그">
      <SchedulerLog :schedule="logSchedule" @close="closeLogDialog" />
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '@/api'

// ✅ PrimeVue 4.5: 개별 컴포넌트 import
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import Dialog from 'primevue/dialog'

import SchedulerDialog from './EtlSchedulerDialog.vue'
import SchedulerLog from './EtlSchedulerLog.vue'
import { useToast } from 'primevue/usetoast'
const $toast = useToast()

const schedules = ref([])
const jobOptions = ref([]) // 반드시 필요!
const dialogVisible = ref(false)
const editData = ref(null)
const logDialogVisible = ref(false)
const logSchedule = ref(null)

const loading = ref(false)
const tableKey = ref(0) // 강제 리렌더용

onMounted(() => {
  handleRefresh()
})

// === 새로고침(전체 재조회) ===
async function handleRefresh() {
  if (loading.value) return
  loading.value = true
  try {
    await Promise.all([fetchJobs(), fetchSchedules()])
    tableKey.value++ // 테이블 강제 갱신(열 정의/정렬 상태 초기화 필요시)
    $toast.add({
      severity: 'success',
      summary: '새로고침 완료',
      detail: 'JOB/스케줄 최신 정보로 갱신했습니다.',
      life: 1500
    })
  } catch (e) {
    $toast.add({
      severity: 'error',
      summary: '새로고침 실패',
      detail: e?.message || '데이터 갱신 중 오류가 발생했습니다.',
      life: 2500
    })
  } finally {
    loading.value = false
  }
}

// === 개별 API ===
async function fetchJobs() {
  const res = await api.get('/api/etl/schedules/jobs')
  jobOptions.value = res.data || []
}

async function fetchSchedules() {
  const res = await api.get('/api/etl/schedules')
  schedules.value = res.data || []
}

// (옵션) 스케줄러 재로딩 트리거
async function reloadScheduler() {
  await api.post('/api/etl/schedules/reload')
  $toast.add({
    severity: 'success',
    summary: '스케줄러 재등록 완료',
    detail: 'DB의 최신 스케줄 정보로 갱신되었습니다.'
  })
}

// === 다이얼로그/CRUD ===
function openDialog() {
  editData.value = null
  dialogVisible.value = true
}

function editSchedule(schedule) {
  const original = schedules.value.find(s =>
    s.scheduleId === schedule.scheduleId || s.id === schedule.scheduleId
  )
  editData.value = original ? JSON.parse(JSON.stringify(original)) : {}
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
}

function saveSchedule() {
  dialogVisible.value = false
  handleRefresh() // 저장/수정 후 새로고침
}

function deleteSchedule(data) {
  if (!confirm('삭제하시겠습니까?')) return
  api.delete(`/api/etl/schedules/${data.scheduleId || data.id}`)
    .then(() => {
      handleRefresh()
      $toast.add({
        severity: 'success',
        summary: '삭제 완료',
        detail: '스케줄이 삭제되었습니다.',
        life: 2000
      })
    })
    .catch(e => {
      $toast.add({
        severity: 'error',
        summary: '삭제 실패',
        detail: e?.message || '삭제 중 오류',
        life: 2000
      })
    })
}

// === 화면 표시용 가공 ===
const displaySchedules = computed(() => {
  return schedules.value.map(s => {
    // 연결된 JOB 이름(옵션이 label/value 형태일 수 있음)
    const job = jobOptions.value.find(j => j.id === s.jobId || j.value === s.jobId)
    // 반복 유형 한글
    const typeKor =
      s.scheduleType === 'DAILY' ? '매일' :
      s.scheduleType === 'WEEKLY' ? '매주' :
      s.scheduleType === 'MONTHLY' ? '매월' :
      s.scheduleType === 'INTERVAL' ? '주기적' : s.scheduleType

    // 요일/시간 파싱
    let days = ''
    let times = []
    if (s.scheduleType === 'DAILY') {
      times = s.scheduleExpr ? s.scheduleExpr.split(',') : []
    } else if (s.scheduleType === 'WEEKLY' || s.scheduleType === 'MONTHLY') {
      const [d, t] = s.scheduleExpr ? s.scheduleExpr.split('|') : ['','']
      days = d
      times = t ? t.split(',') : []
    } else if (s.scheduleType === 'INTERVAL') {
      times = [intervalDisplay(s.scheduleExpr)]
    }

    return {
      ...s,
      name: s.scheduleName || s.name,
      type: typeKor,
      days,
      times,
      jobs: s.jobs || (job ? [job.label || job.name] : [])
        // ↑ s.jobs가 없을 때도 표기되도록 보정
        || []
        ,
      active: s.enabledYn === 'Y'
    }
  })
})

function intervalDisplay(expr) {
  if (!expr) return '-'
  const [val, unit] = expr.split(' ')
  if (unit === 'minute') return `${val}분마다`
  if (unit === 'hour') return `${val}시간마다`
  if (unit === '분') return `${val}분마다`
  if (unit === '시간') return `${val}시간마다`
  return `${val} ${unit}`
}

function openLogDialog(row) {
  logSchedule.value = {
    scheduleId: row.scheduleId,
    name: row.scheduleName || row.jobName || '스케줄'
  }
  logDialogVisible.value = true
}

function closeLogDialog() {
  logDialogVisible.value = false
}

// 유틸
function formatDate(val) {
  if (!val) return ''
  const d = typeof val === 'string' ? new Date(val) : val
  if (isNaN(d)) return ''
  const yyyy = d.getFullYear()
  const mm = (d.getMonth() + 1).toString().padStart(2, '0')
  const dd = d.getDate().toString().padStart(2, '0')
  return `${yyyy}/${mm}/${dd}`
}
</script>

<style scoped>
.scheduler-container {
  padding: 2rem 1rem;
  max-width: 1500px;
  margin: auto;
}
.compact-table .p-datatable-tbody > tr > td,
.compact-table .p-datatable-thead > tr > th {
  padding: 2px 8px !important;
  font-size: 13px !important;
}
.compact-table .p-button,
.compact-table .p-tag {
  font-size: 11px !important;
  padding: 2px 6px !important;
}
</style>
