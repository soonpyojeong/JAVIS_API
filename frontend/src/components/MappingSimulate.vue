<template>
  <div class="max-w-screen-lg mx-auto p-6 space-y-8">
    <!-- 제목 -->
    <div class="text-center space-y-1">
      <h2 class="text-2xl font-bold text-gray-800">
        <i class="pi pi-sliders-h text-blue-500 mr-2" />매핑 시뮬레이션
      </h2>
      <h3 class="text-lg text-gray-500">
        <i class="pi pi-tag mr-1" />{{ job.jobName || 'JOB 없음' }}
      </h3>
    </div>

    <!-- 입력 영역 -->
    <div class="flex flex-wrap items-end gap-6 p-5 bg-white border border-gray-200 rounded-xl shadow">
      <!-- 소스 쿼리 -->
      <div class="flex flex-col flex-1 min-w-[280px]">
        <label class="text-sm font-medium text-gray-700 mb-1">
          <i class="pi pi-database text-green-500 mr-1" />소스 쿼리
        </label>
        <Textarea v-model="extractQuery" autoResize rows="1"
                  class="w-full p-2 text-sm border border-gray-300 rounded"
                  placeholder="예: SELECT * FROM TB_XXX" />
      </div>

      <!-- 샘플 수 -->
      <div class="flex flex-col w-[100px]">
        <label class="text-sm font-medium text-gray-700 mb-1">
          <i class="pi pi-list-numbered text-indigo-500 mr-1" />샘플 수
        </label>
        <InputText v-model="sampleCount" type="number" min="1"
                   class="p-2 text-sm border border-gray-300 rounded" />
      </div>

      <!-- 타겟 테이블 -->
      <div class="flex flex-col w-[200px]">
        <label class="text-sm font-medium text-gray-700 mb-1">
          <i class="pi pi-table text-red-500 mr-1" />타겟 테이블
        </label>
        <div class="h-[38px] flex items-center px-2 font-semibold text-gray-800 bg-gray-50 border border-gray-300 rounded">
          {{ job.targetTable || '미지정' }}
        </div>
      </div>

      <!-- 실행 버튼 -->
      <div class="ml-auto">
        <Button label="시뮬레이션 실행" icon="pi pi-play"
                class="p-button-sm p-button-success"
                @click="handleSimulate" />
      </div>
    </div>

    <!-- 결과 테이블 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- 매핑 정의 -->
      <div class="bg-white p-5 rounded-xl border border-gray-200 shadow">
        <h4 class="text-base font-semibold mb-4">
          <i class="pi pi-share-alt mr-2" />매핑 정의
        </h4>
        <DataTable :value="mappingResults" stripedRows size="small">
          <Column field="sourceColumn" header="소스" />
          <Column field="sourceType" header="타입" />
          <Column header="→" />
          <Column field="targetColumn" header="타겟" />
          <Column field="targetType" header="타입" />
          <Column field="status" header="상태">
            <template #body="{ data }">
              <span :class="{ 'text-green-600': data.status === 'OK', 'text-red-600': data.status !== 'OK' }">
                {{ data.status }}
              </span>
              <span v-if="data.warning" class="ml-1 text-xs text-red-500">
                <i class="pi pi-exclamation-triangle mr-1" />{{ data.warning }}
              </span>
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- 샘플 결과 -->
      <div class="bg-white p-5 rounded-xl border border-gray-200 shadow">
        <h4 class="text-base font-semibold mb-4">
          <i class="pi pi-box mr-2" />샘플 데이터 변환 결과
        </h4>
        <DataTable :value="results" stripedRows size="small">
          <Column header="변환 데이터">
            <template #body="{ data }">
              <div v-for="col in data.columns" :key="col.targetColumn" class="text-sm mb-1">
                <span class="font-mono" :class="{ 'text-red-600 font-semibold': col.warning }">
                  {{ col.targetColumn }}: {{ col.value }} ({{ col.valueType }})
                </span>
                <span v-if="col.warning" class="ml-2 text-xs text-red-500">
                  <i class="pi pi-exclamation-circle mr-1" />{{ col.warning }}
                </span>
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <!-- 매핑 JSON -->
    <div class="bg-white p-5 rounded-xl border border-gray-200 shadow">
      <label class="text-sm font-semibold block mb-2">
        <i class="pi pi-file-code mr-1" />매핑 규칙 (JSON)
      </label>
      <Textarea v-model="mappingJson" rows="6"
                class="w-full text-sm p-2 border border-gray-300 rounded font-mono"
                placeholder="자동 생성 또는 직접 작성 가능" />
    </div>

    <!-- 에러 메시지 -->
    <div v-if="errorMessage" class="p-4 bg-red-50 text-red-600 border border-red-300 rounded font-bold text-center">
      <i class="pi pi-times-circle mr-1" />{{ errorMessage }}
    </div>
    <!-- 테스트 버튼 -->
    <button class="p-2 border rounded bg-blue-100">
      <i class="pi pi-check mr-1"></i>
      아이콘 적용 테스트
    </button>
  </div>

</template>

<script setup>
import { ref, watch } from 'vue'
import Textarea from 'primevue/textarea'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import api from '@/api'
import 'primeicons/primeicons.css';

const props = defineProps({ job: Object })

const extractQuery = ref('')
const mappingJson = ref('')
const sampleCount = ref(3)
const targetColumns = ref([])
const mappingResults = ref([])
const sampleRows = ref([])
const results = ref([])
const errorMessage = ref('')

const getTypeName = (val) => {
  if (val == null) return 'null'
  if (typeof val === 'string') return 'String'
  if (typeof val === 'number') return 'Number'
  if (typeof val === 'boolean') return 'Boolean'
  if (val instanceof Date) return 'Date'
  return typeof val
}

const fetchTargetColumns = async () => {
  try {
    const res = await api.get('/api/etl/table/columns', {
      params: {
        dbId: props.job.targetDbId,
        tableName: props.job.targetTable
      }
    })
    targetColumns.value = res.data || []
  } catch {
    errorMessage.value = '타겟 테이블 컬럼 정보를 불러오지 못했습니다.'
  }
}

const autoGenerateMapping = () => {
  if (!sampleRows.value.length || !targetColumns.value.length) return []

  const sourceCols = Object.keys(sampleRows.value[0])
  return targetColumns.value.map(tc => {
    let status = 'OK'
    let warning = ''
    if (!sourceCols.includes(tc.name)) {
      status = 'NOT_FOUND'
      warning = '소스 컬럼 없음'
    } else {
      const val = sampleRows.value[0][tc.name]
      const valType = getTypeName(val)
      if (valType.toLowerCase() !== tc.type.toLowerCase()) {
        status = 'TYPE_MISMATCH'
        warning = `타입 불일치(${valType}→${tc.type})`
      }
    }

    return {
      sourceColumn: tc.name,
      sourceType: getTypeName(sampleRows.value[0][tc.name] || '-'),
      targetColumn: tc.name,
      targetType: tc.type,
      status,
      warning,
      transform: null,
      defaultValue: null
    }
  })
}

const buildResultRows = () => {
  if (!sampleRows.value.length || !mappingResults.value.length) return []

  return sampleRows.value.map(row => ({
    columns: mappingResults.value.map(map => {
      const val = row[map.sourceColumn]
      const valType = getTypeName(val)
      let warning = ''
      if (valType.toLowerCase() !== (map.targetType || '').toLowerCase()) {
        warning = `타입(${valType}→${map.targetType}) 변환 필요`
      }
      return {
        targetColumn: map.targetColumn,
        value: val,
        valueType: valType,
        warning
      }
    })
  }))
}

const handleSimulate = async () => {
  errorMessage.value = ''

  if (!props.job.sourceDbId || !props.job.targetDbId || !props.job.targetTable) {
    errorMessage.value = 'DB 정보 또는 타겟 테이블 정보가 누락되었습니다.'
    return
  }

  try {
    const res = await api.post('/api/etl/mapping/simulate', {
      extractQuery: extractQuery.value,
      sampleCount: sampleCount.value,
      sourceDbId: props.job.sourceDbId,
      targetDbId: props.job.targetDbId,
      targetTable: props.job.targetTable,
      mappingJson: mappingJson.value
    })

    sampleRows.value = res.data.sampleRows || []
    mappingResults.value = res.data.mappingResults || []

    if (!mappingJson.value || mappingJson.value === '[]') {
      const auto = autoGenerateMapping()
      mappingResults.value = auto
      mappingJson.value = JSON.stringify(auto, null, 2)
    } else {
      try {
        mappingResults.value = JSON.parse(mappingJson.value)
      } catch {
        errorMessage.value = '매핑 JSON 형식이 올바르지 않습니다.'
        results.value = []
        return
      }
    }

    results.value = buildResultRows()
  } catch (err) {
    errorMessage.value = '매핑 시뮬레이션 실행에 실패했습니다.'
    results.value = []
  }
}

watch(() => props.job, async (job) => {
  if (!job || !job.sourceDbId || !job.targetDbId || !job.targetTable) return

  extractQuery.value = job.extractQuery || ''
  mappingJson.value = job.mappingJson || ''
  sampleCount.value = 3
  results.value = []

  await fetchTargetColumns()
  await handleSimulate()
}, { immediate: true })
</script>

<style scoped>
.text-green-600 { color: #059669; }
.text-red-600 { color: #dc2626; }

</style>
