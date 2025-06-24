<template>
  <div class="scheduler-container">
    <div class="toolbar flex gap-2 mb-4">
      <Button label="스케줄 등록" icon="pi pi-plus" @click="openDialog" />
      <Button label="새로고침" icon="pi pi-refresh" @click="fetchSchedules" outlined />
    </div>
    <DataTable
      :value="displaySchedules"
      :paginator="true"
      :rows="10"
      class="shadow text-xs compact-table"
      style="min-width:720px;"
      :pt="{
        bodyRow: { style: 'height:32px;' },
        cell: { style: 'padding:2px 8px;' }
      }"
    >
      <Column field="name" header="스케줄 이름" />
      <Column field="type" header="반복유형" />
      <Column field="days" header="요일/날짜">
        <template #body="{data}">
          {{ displayDays(data) }}
        </template>
      </Column>
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
          <Button icon="pi pi-trash" text rounded @click="deleteSchedule(data)" />
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
import api from "@/api"
import { Button, DataTable, Column, Tag, Dialog } from 'primevue'
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

// 최초 로딩
onMounted(() => {
  fetchJobs()
  fetchSchedules()
})

// JOB 목록
async function fetchJobs() {
  try {
    const res = await api.get('/api/etl/schedules/jobs')
    jobOptions.value = res.data
  } catch (e) {
    $toast.add({
      severity: 'warn',
      summary: 'JOB 조회 실패',
      detail: e?.message || 'JOB 목록을 가져오지 못했습니다.',
      life: 2000
    })
  }
}

function openDialog() {
  editData.value = null
  dialogVisible.value = true
}


function editSchedule(schedule) {
  const original = schedules.value.find(s =>
    s.scheduleId === schedule.scheduleId
    || s.id === schedule.scheduleId
  )
  editData.value = original ? JSON.parse(JSON.stringify(original)) : {}
  dialogVisible.value = true
}


function formatDate(val) {
  if (!val) return ''
  // Date 객체가 아니면 변환
  const d = typeof val === 'string' ? new Date(val) : val
  if (isNaN(d)) return ''
  const yyyy = d.getFullYear()
  const mm = (d.getMonth() + 1).toString().padStart(2, '0')
  const dd = d.getDate().toString().padStart(2, '0')
  return `${yyyy}/${mm}/${dd}`
}



function closeDialog() {
  dialogVisible.value = false
}


function saveSchedule() {
  dialogVisible.value = false
  fetchSchedules()   // 저장/수정은 자식에서, 여기는 목록 새로고침만!
}


async function fetchSchedules() {
  try {
    const res = await api.get('/api/etl/schedules')
    schedules.value = res.data
  } catch (e) {
    $toast.add({
      severity: 'error',
      summary: '조회 실패',
      detail: e?.message || '목록을 가져오지 못했습니다.',
      life: 2000
    })
  }
}


function deleteSchedule(data) {
  if (confirm('삭제하시겠습니까?')) {
    api.delete(`/api/etl/schedules/${data.scheduleId || data.id}`).then(() => {
      fetchSchedules()
      $toast.add({
        severity: 'success',
        summary: '삭제 완료',
        detail: '스케줄이 삭제되었습니다.',
        life: 2000
      })
    }).catch(e => {
      $toast.add({
        severity: 'error',
        summary: '삭제 실패',
        detail: e?.message || '삭제 중 오류',
        life: 2000
      })
    })
  }
}

// === 화면 출력용 스케줄 가공 데이터 ===
const displaySchedules = computed(() => {
  return schedules.value.map(s => {
    // 연결된 JOB 이름 찾기
    const job = jobOptions.value.find(j => j.id === s.jobId || j.value === s.jobId)
    // 반복 유형 한글
    const typeKor = s.scheduleType === 'DAILY' ? '매일'
                 : s.scheduleType === 'WEEKLY' ? '매주'
                 : s.scheduleType === 'MONTHLY' ? '매월'
                 : s.scheduleType
    // 요일/날짜/시간 파싱
    let days = ''
    let times = []
    if (s.scheduleType === 'DAILY') {
      times = s.scheduleExpr ? s.scheduleExpr.split(',') : []
    } else if (s.scheduleType === 'WEEKLY') {
      const [d, t] = s.scheduleExpr ? s.scheduleExpr.split('|') : ['','']
      days = d
      times = t ? t.split(',') : []
    } else if (s.scheduleType === 'MONTHLY') {
      const [d, t] = s.scheduleExpr ? s.scheduleExpr.split('|') : ['','']
      days = d
      times = t ? t.split(',') : []
    }
    return {
      ...s,
      name: s.scheduleName,
      type: typeKor,
      days,
      times,
      jobs: s.jobs,
      active: s.enabledYn === 'Y'
    }
  })
})

function openLogDialog(row) {
  // row에서 스케줄 정보를 얻어 logSchedule에 세팅 (예시: row.schedule, row.scheduleId 등)
  logSchedule.value = {
    scheduleId: row.scheduleId,
    name: row.scheduleName || row.jobName || '스케줄'
  }
  logDialogVisible.value = true
}
function closeLogDialog() {
  logDialogVisible.value = false
}


// days가 string일 수도 있으므로 그대로 출력
function displayDays(data) {
  if (!data.days) return '-'
  return data.days
}
</script>

<style scoped>
.scheduler-container {
    padding: 2rem 1rem;
    max-width: 1300px;
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
