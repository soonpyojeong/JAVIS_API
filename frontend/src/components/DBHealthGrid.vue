<template>
  <div class="db-health-groups">
    <template v-for="group in groups" :key="group.status">
      <div v-if="group.instances.length" class="group-section">
        <div class="group-title" :style="{ color: group.color }">
          <i :class="group.icon" style="margin-right: 8px; font-size: 1.1em;" />
          {{ group.label }}
          <span class="count">({{ group.instances.length }})</span>
        </div>
        <div class="db-health-grid">
          <div
            v-for="instance in group.instances"
            :key="instance.name"
            class="instance-box"
            :class="statusClass(group.status)"
            :title="group.status === '위험' && instance.error ? instance.error : ''"
            @dblclick="handleDoubleClick(group, instance)"
          >
            <div class="instance-icon">
              <i
                class="pi pi-database"
                :style="{
                  color: group.color,
                  fontSize: '32px',
                  filter: group.status === '정상'
                    ? 'drop-shadow(0 0 3px #10b98155)'
                    : group.status === '위험'
                    ? 'drop-shadow(0 0 3px #ef444455)'
                    : ''
                }"
              />
            </div>
            <div class="instance-name">{{ instance.name }}</div>
            <div class="instance-status">{{ instance.status }}</div>
          </div>
        </div>

      </div>
    </template>
    <Dialog v-model:visible="showDialog" header="DB 상세 정보" modal style="width: 500px;">
      <div v-if="selectedInstance">
        <p><strong>DB 이름:</strong> {{ selectedInstance.name }}</p>
        <p><strong>상태:</strong> {{ selectedInstance.status }}</p>
        <p><strong>에러:</strong></p>
        <pre
          style="
            white-space: pre-wrap;
            color: red;
            background: #fef2f2;
            padding: 8px;
            border-radius: 6px;
            font-size: 0.87rem;
            max-height: 240px;
            overflow-y: auto;
          "
        >
    {{ selectedInstance.error }}
        </pre>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted,onBeforeUnmount  } from 'vue'
import api from '@/api'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { connectWebSocket, disconnectWebSocket } from '@/websocket' // ✅ 요기!
import Dialog from 'primevue/dialog'

let stompClient = null

const fetchStatuses = async () => {
  try {
    const { data } = await api.get('/api/dashboard/live-statuses')
    console.log('[✅ 상태 응답]', data)
    instances.value = data
  } catch (e) {
    console.error('[❌ 상태 조회 실패]', e)
  }
}


const selectedInstance = ref(null)
const showDialog = ref(false)

const handleDoubleClick = (group, instance) => {
  if (group.status === '위험') {
    selectedInstance.value = instance
    showDialog.value = true
  }
}


onMounted(async () => {
  await fetchStatuses()

  connectWebSocket({
    onDbLiveStatusMessage: async (payload) => {
      console.log('📡 /topic/db-live-status 수신:', payload)
      await fetchStatuses()
    }
  })
})

onBeforeUnmount(() => {
  disconnectWebSocket()
})

const instances = ref([])



const groupMeta = [
  { status: '위험', label: '위험', color: '#ef4444', icon: 'pi pi-times-circle' },
  { status: '주의', label: '주의', color: '#f59e0b', icon: 'pi pi-exclamation-circle' },
  { status: '정상', label: '정상', color: '#10b981', icon: 'pi pi-check-circle' },
  { status: '미수집', label: '미수집', color: '#6b7280', icon: 'pi pi-question-circle' }
]

const groups = computed(() =>
  groupMeta.map(meta => ({
    ...meta,
    instances: instances.value
      .filter(i => i.status === meta.status)
      .sort((a, b) => a.name.localeCompare(b.name))
  }))
)

const statusClass = (status) => {
  switch (status) {
    case '정상': return 'status-normal'
    case '주의': return 'status-warning'
    case '위험': return 'status-critical'
    case '미수집': return 'status-unknown'
    default: return ''
  }
}
</script>

<style scoped>
.db-health-groups {
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.group-title {
  font-weight: bold;
  font-size: 1.06em;
  margin-bottom: 7px;
  display: flex;
  align-items: center;
  padding-left: 3px;
}
.group-title .count {
  font-weight: normal;
  font-size: 0.97em;
  color: #8a8f9f;
  margin-left: 6px;
}

.group-section {
  margin-bottom: 7px;
}

.db-health-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.instance-box {
  flex: 0 0 80px;  /* 이전의 158px → 80px로 줄임 */
  max-width: 100px;
  padding: 8px 4px;
  border-radius: 10px;
  background-color: #f9f9f9;
  box-shadow: 0 1px 5px rgba(0, 0, 0, 0.05);
  text-align: center;
  transition: transform 0.12s;
  font-size: 0.75rem;
}
.instance-box:hover {
  transform: scale(1.05);
}

.instance-icon {
  margin-bottom: 4px;
}

.instance-name {
  font-size: 0.72rem;
  font-weight: 600;
  word-break: break-all;
}

.instance-status {
  display: none; /* 상태는 툴팁으로 처리하고 안 보이게 */
}

.status-normal {
  border: 2.5px solid #10b981;
  color: #065f46;
}
.status-warning {
  border: 2.5px solid #f59e0b;
  color: #92400e;
}
.status-critical {
  border: 2.5px solid #ef4444;
  color: #991b1b;
}
.status-unknown {
  border: 2.5px dashed #bbb;
  color: #888;
}

.instance-name {
  font-size: 1.02em;
  font-weight: bold;
  margin-bottom: 3px;
}
.instance-status {
  font-size: 0.98em;
  margin-top: 2px;
  letter-spacing: 0.02em;
}

@media (max-width: 800px) {
  .db-health-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
