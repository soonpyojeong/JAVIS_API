<template>
  <div class="etl-job-list-container">
    <h2 class="mb-4 text-xl font-bold">ETL 작업 목록</h2>
    <div class="toolbar mb-2 flex gap-2">
      <Button label="작업 등록" icon="pi pi-plus" @click="openAddDialog" />
      <Button label="새로고침" icon="pi pi-refresh" @click="fetchJobs" outlined />
    </div>
    <DataTable
      :value="jobs"
      :paginator="true"
      :rows="10"
      dataKey="id"
      class="shadow"
      @row-dblclick="openEditDialog"
      selectionMode="single"
      :selection="selectedJob"
      @update:selection="selectedJob = $event"
    >
      <Column field="id" header="ID" :sortable="true" style="width:60px"/>
      <Column field="sourceDbId" header="원본DB">
        <template #body="{data}">
          {{ dbList.find(d => d.id === data.sourceDbId)?.name || data.sourceDbId }}
        </template>
      </Column>
      <Column field="targetDbId" header="타겟DB">
        <template #body="{data}">
          {{ dbList.find(d => d.id === data.targetDbId)?.name || data.targetDbId }}
        </template>
      </Column>
      <!-- ETLJobList.vue 일부 -->
      <Column header="매핑 점검">
        <template #body="{ data }">
          <Button icon="pi pi-search" @click="openSimulateDialog(data)" outlined size="small"/>
        </template>
      </Column>
      <Dialog v-model:visible="simulateDialog" header="매핑 시뮬레이션" width="900" modal>
        <MappingSimulate v-if="simulateDialog && simulateJob" :job="simulateJob" />
      </Dialog>
      <Column field="schedule" header="스케줄"/>
      <Column field="status" header="상태"/>
      <Column header="실행">
        <template #body="{data}">
          <Button icon="pi pi-play" @click="runJob(data)" severity="success" size="small"/>
        </template>
      </Column>
        <Column field="lastResult" header="최종 실행결과">
          <template #body="{data}">
            <span :class="{
              'text-green-600': data.lastResult === 'SUCCESS',
              'text-red-600': data.lastResult && data.lastResult.startsWith('FAIL')
            }">
              {{ data.lastResult || '미수행' }}
            </span>
          </template>
        </Column>
        <Column field="lastRunAt" header="마지막 실행시각">
          <template #body="{data}">
            {{ data.lastRunAt ? formatDate(data.lastRunAt) : '미수행' }}
          </template>
        </Column>

      <Column header="로그">
        <template #body="{data}">
          <Button icon="pi pi-list" @click="openLogDialog(data)" outlined size="small"/>
        </template>
      </Column>
      <Column header="삭제">
        <template #body="{data}">
          <Button icon="pi pi-trash" @click="deleteJob(data)" severity="danger" outlined size="small"/>
        </template>
      </Column>
    </DataTable>

    <!-- 등록/수정 다이얼로그 -->
    <Dialog v-model:visible="jobDialog" :header="editMode ? '작업 수정' : '작업 등록'" :modal="true" width="600">
      <div class="p-fluid">
        <InputText v-model="editJob.jobName" placeholder="작업명" class="mb-3"/>
        <Dropdown
          v-model="editJob.sourceDbId"
          :options="dbList"
          optionLabel="name"
          optionValue="id"
          placeholder="원본 DB 선택"
        />
        <Dropdown
          v-model="editJob.targetDbId"
          :options="dbList"
          optionLabel="name"
          optionValue="id"
          placeholder="타겟 DB 선택"
        />
        <InputText v-model="editJob.targetTable" placeholder="적재 테이블명" class="mb-2"/>
        <InputText v-model="editJob.schedule" placeholder="스케줄(cron)" class="mb-2"/>
        <Dropdown v-model="editJob.status" :options="['ACTIVE','INACTIVE']" placeholder="상태"/>
      </div>
      <div class="p-extractQuery">
      <Textarea v-model="editJob.extractQuery" placeholder="추출 쿼리 또는 테이블명" rows="2" class="mb-2"/>
      </div>
      <template #footer>
        <Button label="저장" @click="saveJob" />
        <Button label="취소" @click="closeJobDialog" severity="secondary"/>
      </template>
    </Dialog>

    <!-- 실행 결과/로그 다이얼로그 -->
    <Dialog v-model:visible="logDialog" header="실행 결과 로그" width="700" modal>
      <DataTable :value="logs" :rows="10" :paginator="true" class="mt-2">
        <Column field="executedAt" header="실행시각">
          <template #body="{ data }">
            {{ formatDate(data.executedAt) }}
          </template>
        </Column>
        <Column field="result" header="결과" />
        <Column field="message" header="메시지" />
      </DataTable>
      <template #footer>
        <Button label="닫기" @click="logDialog=false"/>
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Dropdown from 'primevue/dropdown'
import Textarea from 'primevue/textarea'
import InputText from 'primevue/inputtext'
import api from "@/api"; // axios 인스턴스
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import MappingSimulate from './MappingSimulate.vue'


const jobs = ref([]);
const logs = ref([]);
const dbList = ref([]);
const jobDialog = ref(false);
const logDialog = ref(false);
const editMode = ref(false);
const editJob = ref({});
const selectedJob = ref(null);
const simulateDialog = ref(false);
const simulateJob = ref(null);


function openSimulateDialog(job) {
  simulateDialog.value = false;
  simulateJob.value = null;

  setTimeout(() => {
    simulateJob.value = job;
    simulateDialog.value = true;
  }, 0);
}

let stompClient = null;
function connectWebSocket() {
  stompClient = new Client({
    brokerURL: null,
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    onConnect: () => {
      console.log("[WebSocket] 연결 성공!");
      stompClient.subscribe('/topic/etl-job-status', message => {
        const msg = JSON.parse(message.body);
        console.log('[WebSocket 수신]', msg);

        const idx = jobs.value.findIndex(j => j.id === msg.jobId);
        console.log('idx:', idx, '기존:', jobs.value[idx]);

        if (idx > -1) {
          jobs.value[idx] = {
            ...jobs.value[idx],
            lastResult: msg.result,
            lastRunAt: msg.lastRunAt,
          };
          console.log('[jobs 최신상태]', jobs.value);
        }
      });



    },
    onStompError: frame => {
      console.error('[WebSocket STOMP 에러]', frame);
    },
    onWebSocketError: error => {
      console.error('[WebSocket 연결 에러]', error);
    }
  });
  stompClient.activate();
}


function fetchJobs() {
  api.get("/api/etl/job").then(res => {
    jobs.value = res.data;
    console.log('[fetchJobs] jobs:', jobs.value); // ← 콘솔에서 직접 확인!
  });
}
function fetchDbList() {
  api.get("/api/db-connection").then(res => {
    dbList.value = res.data.map(db => ({
      id: db.id,
      name: `[${db.dbType}] ${db.host}:${db.port}/${db.dbName} (${db.description || db.username})`
    }));
  });
}
function openAddDialog() {
  editMode.value = false;
  editJob.value = { jobName: '', sourceDbId: null, targetDbId: null, extractQuery: '', targetTable: '', schedule: '', status: 'ACTIVE' };
  jobDialog.value = true;
}
function openEditDialog(event) {
  editMode.value = true;
  editJob.value = {...event.data};
  console.log('[openEditDialog] editJob:', editJob.value); // ← 콘솔에서 직접 확인!
  jobDialog.value = true;
}
function closeJobDialog() {
  jobDialog.value = false;
}
function saveJob() {
  if (editMode.value) {
    api.put(`/api/etl/job/${editJob.value.id}`, editJob.value).then(fetchJobs);
  } else {
    api.post(`/api/etl/job`, editJob.value).then(fetchJobs);
  }
  jobDialog.value = false;
}
function deleteJob(job) {
  if (confirm("삭제할까요?")) {
    api.delete(`/api/etl/job/${job.id}`).then(fetchJobs);
  }
}
function runJob(job) {
  console.log("실행할 job 정보", job); // 여기에 extractQuery, targetTable이 있나?
  if (!job.extractQuery || !job.targetTable) {
      alert("추출쿼리나 타겟 테이블이 누락되었습니다.");
      return;
    }
  api.post(`/api/etl/job/run/${job.id}`).then(res => {
    alert(res.data || "실행 완료!");
    fetchLogs(job.id);
  });
}


function openLogDialog(job) {
  fetchLogs(job.id);
  logDialog.value = true;
}
function fetchLogs(jobId) {
  api.get(`/api/etl/job/${jobId}/logs`).then(res => {
    logs.value = res.data.sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt));
  });
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const formatted = d.toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' });
  return formatted;
}

onMounted(() => {
  fetchJobs();
  fetchDbList();
  connectWebSocket();
});
</script>

<style scoped>
.etl-job-list-container {
  padding: 2rem 1rem;
  max-width: 1300px;
  margin: auto;
}
</style>
