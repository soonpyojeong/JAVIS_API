<template>
  <div class="session-lock-monitor">
    <div v-if="isSupportedDbType">
        <div class="toolbar">
          <Dropdown
            v-model="selectedServer"
            :options="serverOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Server">
          </Dropdown>
          <Button icon="pi pi-refresh" label="Refresh" @click="fetchLockData" class="p-button-sm ml-2" />
          <ToggleButton v-model="notifications" onLabel="Notifications On" offLabel="Off" class="ml-2" />
          <span class="last-refresh">Last Refresh: {{ lastRefresh }}</span>
        </div>

        <div class="content-grid">
          <!-- Blocking Tree -->
          <Card class="tree-section">
            <template #title>Blocking Tree</template>
            <Tree :value="lockTree" :expandedKeys="expandedKeys" selectionMode="single" @nodeSelect="onNodeSelect" />
          </Card>

          <!-- Session Resource Usage & SQL Info -->
          <div class="info-section">
            <Card>
              <template #title>Session Resource Usage</template>
              <p>CPU: <ProgressBar :value="selectedSession.cpuUsage" /></p>
              <p>PGA: <ProgressBar :value="selectedSession.pgaUsage" /></p>
              <p>Event: {{ selectedSession.event }}</p>
            </Card>

            <Card class="mt-3">
              <template #title>SQL Information</template>
              <p>{{ selectedSession.sqlText }}</p>
            </Card>
          </div>
        </div>

        <!-- Session Details Table -->
        <Card class="mt-3">
          <template #title>Session Details</template>
          <DataTable :value="sessionList" :responsiveLayout="'scroll'" @rowDblclick="onRowDblClick">
            <Column field="sid" header="SID" />
            <Column field="username" header="USER" />
            <Column field="event" header="EVENT" />
            <Column field="waitTime" header="WAIT_TIME" />
            <Column field="blockingSession" header="BLOCKED BY" />
            <Column header="KILL">
              <template #body="slotProps">
                <Button icon="pi pi-times" class="p-button-danger p-button-sm" @click="killSession(slotProps.data)" />
              </template>
            </Column>
          </DataTable>
        </Card>
      </div>
    <Card class="mt-3">
      <template #title>
      DB Error Details

      <div>
        <p><strong>수집시간:</strong> {{ resolvedInstance.chkDate }}</p>
        <p><strong>상태:</strong> {{ resolvedInstance.status }}</p>

        <template v-if="!!(resolvedInstance?.error && resolvedInstance.error.trim() !== '')">
          <pre
            style="white-space: pre-wrap; color: red; background: #fef2f2; padding: 8px; border-radius: 6px; font-size: 0.87rem; max-height: 240px; overflow-y: auto;">
           🔴 {{ resolvedInstance.error }}
          </pre>
        </template>

        <template v-else>
          <p style="color: #666; font-size: 0.85rem;">
            🟢수집된 에러 정보가 없습니다.
          </p>
        </template>
      </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref, onMounted,watch,computed  } from 'vue'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'
import Tree from 'primevue/tree'
import ProgressBar from 'primevue/progressbar'
import ToggleButton from 'primevue/togglebutton'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'


const props = defineProps({
  instance: {
    type: Object,
    required: true
  },
  allInstances: {
    type: Array,
    default: () => []
  }
})

// 선언부
const selectedServer = ref('')
const serverOptions = ref([])

const isSupportedDbType = computed(() => {
  return (
    resolvedInstance.value &&
    ['ORACLE', 'TIBERO', 'POSTGRESQL', 'EDB'].includes(resolvedInstance.value.dbType)
  )
})
const resolvedInstance = computed(() => {
  if (
    props.instance?.chkDate &&
    props.instance?.status &&
    props.instance?.error !== undefined
  ) {
    return props.instance
  }

  return props.allInstances.find(i => i.name === props.instance.name) || {
    name: '',
    dbType: '',
    chkDate: '',
    status: '',
    error: '',
    message: ''
  }
})



onMounted(() => {
  console.log('[🐛 props.instance.name]', props.instance.name)
  console.log('[🐛 resolvedInstance.value]', resolvedInstance.value)
  console.log('[🐛 resolvedInstance.error]', resolvedInstance.value.error)
})

onMounted(() => {
  // 1. serverOptions 구성
  serverOptions.value = props.allInstances.map(i => ({
    label: i.name,
    value: i.name
  }))

  // 2. props.instance.name이 실제 있는지 확인
  const defaultValue = props.instance.name
  const found = serverOptions.value.find(opt => opt.value === defaultValue)
  if (found) {
    selectedServer.value = found.value  // ✅ 여기가 핵심
  } else {
    serverOptions.value.push({ label: defaultValue, value: defaultValue })
    selectedServer.value = defaultValue
  }

  // 3. (선택) 락 조회 호출
  fetchLockDataFor(selectedServer.value)

})



function fetchLockDataFor(dbName) {
  // 여기서 실제 API 호출

  lastRefresh.value = new Date().toLocaleTimeString()

  // 예시용 더미 로직
  sessionList.value = [
    { sid: 123, username: 'SYS', event: 'enq: TX - row lock contention', waitTime: 20, blockingSession: 456 }
  ]
  lockTree.value = [
    {
      key: 'root',
      label: `Blocking Tree - ${dbName}`,
      data: {},
      children: [{ key: '1', label: 'SID 123', data: { cpuUsage: 10, pgaUsage: 5, event: 'Wait...', sqlText: 'SELECT * FROM ...' } }]
    }
  ]
}



const lastRefresh = ref('')
const notifications = ref(true)

const lockTree = ref([])
const expandedKeys = ref({})
const selectedSession = ref({ cpuUsage: 0, pgaUsage: 0, event: '', sqlText: '' })
const sessionList = ref([])

const showDialog = ref(false)
const selectedInstance = ref(null)

function fetchLockData() {
  // API 호출
  lastRefresh.value = new Date().toLocaleTimeString()
}

function onNodeSelect(event) {
  selectedSession.value = event.node.data
}

function killSession(session) {
  // 세션 종료 API 호출
  console.log('Killing session', session.sid)
}

function onRowDblClick(event) {
  selectedInstance.value = event.data
  showDialog.value = true
}

watch(
  () => serverOptions.value.length,
  (len) => {
    if (len > 0 && !selectedServer.value) {
      const defaultVal = props.instance.name
      const found = serverOptions.value.find(opt => opt.value === defaultVal)
      if (found) {
        selectedServer.value = found.value
        console.log('🎯 selectedServer set:', selectedServer.value)
      }
    }
  },
  { immediate: true }
)
watch(() => props.instance.name, (newVal) => {
  if (newVal) {
    selectedServer.value = newVal
    fetchLockDataFor(newVal)
  }
})
</script>

<style scoped>
.session-lock-monitor {
  padding: 1rem;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}
.content-grid {
  display: grid;
  grid-template-columns: 2fr 1.5fr;
  gap: 1rem;
}
.tree-section {
  height: 100%;
}
.info-section > * {
  margin-bottom: 1rem;
}
.last-refresh {
  margin-left: auto;
  font-size: 0.9rem;
  color: #666;
}
.mt-3 {
  margin-top: 1rem;
}

</style>
