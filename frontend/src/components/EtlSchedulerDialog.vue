<template>
  <div class="register-card">
    <div class="mb-4">
      <label for="scheduleName" style="display:block; margin-bottom:8px; font-weight:600;">
        스케줄러 이름
      </label>
      <InputText
        id="scheduleName"
        v-model="form.scheduleName"
        placeholder="스케줄러 이름을 입력하세요"
        style="width:100%;"
        maxlength="100"
      />
    </div>

    <div class="mb-4">
      <label class="block mb-2 font-semibold">JOB 선택</label>
      <MultiSelect
        v-model="form.jobIds"
        :options="jobOptions"
        optionLabel="label"
        optionValue="value"
        placeholder="작업을 선택하세요"
        class="w-full"
        display="chip"
      />
    </div>

    <div class="mb-4 flex gap-4 items-center">
      <RadioButton v-model="form.type" inputId="daily" value="daily" />
      <label for="daily" class="ml-1 mr-3">매일</label>
      <RadioButton v-model="form.type" inputId="weekly" value="weekly" />
      <label for="weekly" class="ml-1 mr-3">매주</label>
      <RadioButton v-model="form.type" inputId="monthly" value="monthly" />
      <label for="monthly" class="ml-1">매월</label>
    </div>

    <!-- 매일: 시간 선택만 -->
    <div v-if="form.type==='daily'" class="mb-4">
      <label class="block mb-2 font-semibold">시간 선택</label>
      <div class="flex flex-wrap gap-2">
        <div v-for="(t, idx) in form.times" :key="idx" class="flex items-center gap-1 mb-1">
          <DatePicker v-model="form.times[idx]" timeOnly hourFormat="24" showIcon class="w-24" size="small" />
          <Button icon="pi pi-times" text size="small" @click="removeTime(idx)" v-if="form.times.length>1"/>
        </div>
        <Button icon="pi pi-plus" text size="small" @click="addTime" />
      </div>
    </div>

    <!-- 매주: 요일 토글+시간 선택 -->
    <div v-if="form.type==='weekly'" class="mb-4">
      <label class="block mb-2 font-semibold">요일 선택</label>
      <div class="flex gap-1 mb-3">
        <Button
          v-for="day in days"
          :key="day.value"
          :label="day.label"
          :outlined="!form.days.includes(day.value)"
          :severity="form.days.includes(day.value) ? 'primary' : 'secondary'"
          class="px-3 py-2 rounded"
          @click="toggleDay(day.value)"
        />
      </div>
      <label class="block mb-2 font-semibold">시간 선택</label>
      <div class="flex flex-wrap gap-2">
        <div v-for="(t, idx) in form.times" :key="idx" class="flex items-center gap-1 mb-1">
          <DatePicker v-model="form.times[idx]" timeOnly hourFormat="24" showIcon class="w-24" size="small" />
          <Button icon="pi pi-times" text size="small" @click="removeTime(idx)" v-if="form.times.length>1"/>
        </div>
        <Button icon="pi pi-plus" text size="small" @click="addTime" />
      </div>
    </div>

    <!-- 매월: N주차 요일 + 시간 선택 -->
    <div v-if="form.type==='monthly'" class="mb-4">
      <label class="block mb-2 font-semibold">주차/요일 선택</label>
      <div class="flex items-center gap-3 mb-3">
        <Dropdown
          v-model="form.weekNo"
          :options="weekNos"
          optionLabel="label"
          optionValue="value"
          placeholder="주차"
          class="w-20"
        >
          <template #option="slotProps">{{ slotProps.option.label }}</template>
          <template #value="slotProps">
            {{ slotProps.value ? weekNos.find(w => w.value === slotProps.value)?.label : '' }}
          </template>
        </Dropdown>
        <Button
          v-for="day in days"
          :key="day.value"
          :label="day.label"
          :outlined="form.dayOfWeek!==day.value"
          :severity="form.dayOfWeek===day.value ? 'primary' : 'secondary'"
          class="px-3 py-2 rounded"
          @click="form.dayOfWeek = day.value"
        />
      </div>
      <label class="block mb-2 font-semibold">시간 선택</label>
      <div class="flex flex-wrap gap-2">
        <div v-for="(t, idx) in form.times" :key="idx" class="flex items-center gap-1 mb-1">
          <DatePicker v-model="form.times[idx]" timeOnly hourFormat="24" showIcon class="w-24" size="small" />
          <Button icon="pi pi-times" text size="small" @click="removeTime(idx)" v-if="form.times.length>1"/>
        </div>
        <Button icon="pi pi-plus" text size="small" @click="addTime" />
      </div>
    </div>

    <div class="mb-3 text-base font-semibold text-blue-700">
      {{ previewText }}
    </div>
    <!-- 상태 토글 버튼 (시간선택, 날짜선택 위/아래 아무데나) -->
    <div class="mb-4 flex items-center gap-3">
      <label class="mb-4  block font-semibold">스케줄 상태 : </label>
      <Button
        :label="form.enabledYn === 'Y' ? 'Active' : 'InActive'"
        :severity="form.enabledYn === 'Y' ? 'success' : 'danger'"
        class="px-4 py-2 rounded font-bold"
        @click="form.enabledYn = (form.enabledYn === 'Y' ? 'N' : 'Y')"
        style="min-width:90px"
      />

    </div>

    <div class="mb-4 flex gap-3">
      <div>
        <label class="block mb-1 font-semibold" for="startDate">시작일</label>
        <DatePicker
          id="startDate"
          v-model="form.startDate"
          dateFormat="yy-mm-dd"
          placeholder="시작일"
          class="w-32"
          showIcon
        />
      </div>
      <div>
        <label class="block mb-1 font-semibold" for="endDate">종료일</label>
        <DatePicker
          id="endDate"
          v-model="form.endDate"
          dateFormat="yy-mm-dd"
          placeholder="종료일"
          class="w-32"
          showIcon
        />
      </div>
    </div>

    <div class="flex justify-end gap-2 mt-6">
      <Button label="취소" text @click="$emit('cancel')" />
      <Button :label="isEditMode ? '수정' : '등록'" @click="onSave" :disabled="!isValid" />

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted ,watch } from 'vue'
import { Button, DataTable, Column, Tag, Dialog } from 'primevue'
import RadioButton from 'primevue/radiobutton'
import DatePicker from 'primevue/datepicker'
import Dropdown from 'primevue/dropdown'
import InputText from 'primevue/inputtext'
import MultiSelect from 'primevue/multiselect'
import api from "@/api"; // axios 인스턴스
import { useStore } from 'vuex'
const store = useStore()
const user = computed(() => store.state.user || {})

const isEditMode = computed(() => !!form.value.scheduleId)
const jobOptions = ref([])
const props = defineProps({
  editData: { type: Object, default: null }
})

const form = ref({
  scheduleId: null,
  scheduleName: '',
  scheduleType: 'DAILY',
  scheduleExpr: '',
  startDate: null,
  endDate: null,
  enabledYn: 'Y',
  jobIds: [],
  type: 'daily',
  days: [],
  times: [new Date(new Date().setHours(0, 0, 0, 0))],
  weekNo: 1,
  dayOfWeek: 'MON',
  updatedUser: user.value.userId || user.value.username
})




const days = [
  { label: '월', value: 'MON' },
  { label: '화', value: 'TUE' },
  { label: '수', value: 'WED' },
  { label: '목', value: 'THU' },
  { label: '금', value: 'FRI' },
  { label: '토', value: 'SAT' },
  { label: '일', value: 'SUN' }
]
const weekNos = [
  { label: '첫째', value: 1 },
  { label: '둘째', value: 2 },
  { label: '셋째', value: 3 },
  { label: '넷째', value: 4 },
  { label: '다섯째', value: 5 }
]

function addTime() {
  form.value.times.push(new Date(new Date().setHours(0, 0, 0, 0)))
}
function removeTime(idx) {
  form.value.times.splice(idx, 1)
}
function toggleDay(day) {
  const idx = form.value.days.indexOf(day)
  if (idx === -1) form.value.days.push(day)
  else form.value.days.splice(idx, 1)
}

const isValid = computed(() => {
  if (!form.value.jobIds.length) return false
  if (!form.value.times.length) return false
  if (form.value.type === 'weekly' && !form.value.days.length) return false
  if (form.value.type === 'monthly' && !form.value.dayOfWeek) return false
  return true
})

const previewText = computed(() => {
  if (form.value.type === 'daily') {
    return `매일 ${form.value.times.map(formatTime).join(', ')}에 실행됩니다.`
  }
  if (form.value.type === 'weekly') {
    const dayLabels = days.filter(d => form.value.days.includes(d.value)).map(d => d.label)
    return `매주 ${dayLabels.length ? dayLabels.join(', ') : '요일 미선택'} ${form.value.times.map(formatTime).join(', ')}에 실행됩니다.`
  }
  if (form.value.type === 'monthly') {
    const weekLabel = weekNos.find(w => w.value === form.value.weekNo)?.label
    const dayLabel = days.find(d => d.value === form.value.dayOfWeek)?.label
    return `매월 ${weekLabel} ${dayLabel}요일 ${form.value.times.map(formatTime).join(', ')}에 실행됩니다.`
  }
  return ''
})

function formatTime(date) {
  if (!date) return ''
  const d = new Date(date)
  const hh = d.getHours().toString().padStart(2, '0')
  const mm = d.getMinutes().toString().padStart(2, '0')
  return `${hh}:${mm}`
}

function makeScheduleExpr(form) {
  // 여러 시간 지원(문자열로 합치기)
  const times = form.times.map(formatTime).join(',')

  if (form.type === 'daily') {
    return times
  }
  if (form.type === 'weekly') {
    return `${form.days.join(',')}|${times}`
  }
  if (form.type === 'monthly') {
    return `${form.weekNo}주차:${form.dayOfWeek}|${times}`
  }
  return ''
}

const emit = defineEmits(['save', 'cancel'])

function toYyyyMmDd(date) {
  if (!date) return null
  const d = new Date(date)
  const yyyy = d.getFullYear()
  const mm = (d.getMonth() + 1).toString().padStart(2, '0')
  const dd = d.getDate().toString().padStart(2, '0')
  return `${yyyy}-${mm}-${dd}`
}

// endDate → 23:59:59 붙이기
function toYyyyMmDd235959(date) {
  if (!date) return null
  const d = new Date(date)
  const yyyy = d.getFullYear()
  const mm = (d.getMonth() + 1).toString().padStart(2, '0')
  const dd = d.getDate().toString().padStart(2, '0')
  return `${yyyy}-${mm}-${dd} 23:59:59`
}


async function onSave() {
  try {
    const payload = {
      scheduleId: form.value.scheduleId,
      scheduleName: form.value.scheduleName,
      jobIds: form.value.jobIds,
      scheduleType: form.value.type.toUpperCase(),
      scheduleExpr: makeScheduleExpr(form.value),
      startDate: toYyyyMmDd(form.value.startDate) || null,
      endDate: toYyyyMmDd235959(form.value.endDate) || null,
      enabledYn: 'Y',
      updatedUser: user.value.username || user.value.id || '',
    }
    if (isEditMode.value) {
      // 수정(PUT)
      console.log('수정:', payload)
      await api.put(`/api/etl/schedules/${payload.scheduleId}`, payload)
    } else {
      // 신규(POST)
      console.log('신규:', payload)
      await api.post('/api/etl/schedules', payload)
    }
    emit('save')
  } catch (e) {
    alert((isEditMode.value ? '수정' : '등록') + ' 실패: ' + (e.response?.data?.message || e.message))
  }
}


onMounted(async () => {
  // 실제 API 경로로 교체 필요!
  const res = await api.get('/api/etl/schedules/jobs')
  jobOptions.value = res.data.map(job => ({
    label: job.jobName,
    value: job.id
  }))
})



watch(
  () => props.editData, (val) => {
    if (val) {
      // 기본값 세팅
      form.value = {
        ...form.value,
        ...val,
        jobIds: val.jobIds || (val.jobId ? [val.jobId] : []),
        type: (val.scheduleType || 'DAILY').toLowerCase(),
        days: [],
        times: [],
        weekNo: 1,
        dayOfWeek: 'MON',
        enabledYn: val.enabledYn ?? 'Y',
        startDate: val.startDate ? new Date(val.startDate) : null,
        endDate: val.endDate ? new Date(val.endDate) : null,
        updatedUser: user.value.userId || user.value.username
      }
      // scheduleExpr 파싱
      if (val.scheduleType === 'DAILY') {
        // 예: "08:00,14:00"
        form.value.times = val.scheduleExpr.split(',').map(t => {
          // "08:00" → Date 객체(오늘 날짜로)
          const [h, m] = t.split(':')
          const d = new Date()
          d.setHours(+h, +m, 0, 0)
          return d
        })
      } else if (val.scheduleType === 'WEEKLY') {
        // 예: "MON,WED|08:00,14:00"
        const [days, times] = val.scheduleExpr.split('|')
        form.value.days = days ? days.split(',') : []
        form.value.times = times ? times.split(',').map(t => {
          const [h, m] = t.split(':')
          const d = new Date()
          d.setHours(+h, +m, 0, 0)
          return d
        }) : []
      } else if (val.scheduleType === 'MONTHLY') {
        // 예: "2주차:MON|10:00,14:00"
        const [weekday, times] = val.scheduleExpr.split('|')
        if (weekday && weekday.includes('주차:')) {
          const [week, day] = weekday.split(':')
          form.value.weekNo = parseInt(week)
          form.value.dayOfWeek = day
        }
        form.value.times = times ? times.split(',').map(t => {
          const [h, m] = t.split(':')
          const d = new Date()
          d.setHours(+h, +m, 0, 0)
          return d
        }) : []
      }
    } else {
      // 신규등록: 초기화
      form.value = {
        scheduleId: null,
        scheduleName: '',
        scheduleType: 'DAILY',
        scheduleExpr: '',
        startDate: null,
        endDate: null,
        enabledYn: 'Y',
        jobIds: [],
        type: 'daily',
        days: [],
        times: [new Date(new Date().setHours(0, 0, 0, 0))],
        weekNo: 1,
        dayOfWeek: 'MON',
        updatedUser: user.value.userId || user.value.username
      }
    }
  },
  { immediate: true }
)

</script>

<style scoped>
.register-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px #0002;
  padding: 30px 24px 22px 24px;
  width: 540px;
  max-width: 96vw;
  min-width: 280px;
  box-sizing: border-box;
}
.p-multiselect .p-multiselect-label-container {
  display: flex;
  flex-wrap: wrap !important;
  align-items: center;
  min-height: 38px;
  max-width: 100%;
  box-sizing: border-box;
}
.p-multiselect .p-multiselect-token {
  max-width: 100%;
  margin-bottom: 4px;
  white-space: normal !important;
  box-sizing: border-box;
}
.p-multiselect .p-multiselect-label {
  white-space: normal !important;
  overflow-wrap: break-word;
  word-break: break-all;
  box-sizing: border-box;
}
.p-multiselect {
  width: 100% !important;
  min-width: 0 !important;
  box-sizing: border-box;
}
.mb-4 { margin-bottom: 1.7rem; }
.mb-5 { margin-bottom: 1.7rem; }
.mb-3 { margin-bottom: 1.7rem; }
.space-y-5 > :not([hidden]) ~ :not([hidden]) { margin-top: 1.2rem; }
</style>
