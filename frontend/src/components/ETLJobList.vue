<template>
  <div class="etl-job-list-container">
    <h2 class="mb-4 text-xl font-bold">ETL 작업 목록</h2>

    <div class="toolbar mb-2 flex gap-2">
      <Button label="새로고침" icon="pi pi-refresh" @click="refreshJobs" outlined />
    </div>

    <DataTable
      :value="jobs"
      :paginator="true"
      :rows="10"
      dataKey="id"
      class="shadow"
      selectionMode="single"
      :selection="selectedJob"
      @update:selection="selectedJob = $event"
      sortField="lastRunAt"
      :sortOrder="-1"
      :rowHover="true"
    >
      <Column field="id" header="ID" :sortable="true" style="width:80px" />

      <Column field="jobName" header="JOB이름" :sortable="true">
        <template #body="{ data }">
          {{ dbList.find(d => d.id === data.jobName)?.name || data.jobName }}
        </template>
      </Column>

      <Column header="상세" :sortable="false" style="width:90px">
        <template #body="{ data }">
          <Button icon="pi pi-eye" size="small" @click="openJobDetail(data.id)" outlined />
        </template>
      </Column>

      <Column field="status" header="상태" :sortable="true" style="width:120px" />

      <Column header="실행" :sortable="false" style="width:120px">
        <template #body="{ data }">
          <Button icon="pi pi-play" @click="runJob(data)" severity="success" size="small" />
          <ProgressBar
            v-if="loadingJobId === data.id"
            mode="indeterminate"
            style="height:6px; margin-top:12px"
          />
        </template>
      </Column>

      <Column field="lastResult" header="최종 결과" :sortable="true" style="width:130px">
        <template #body="{ data }">
          <Tag :severity="data.lastResult === 'FAIL' ? 'danger' : 'success'">
            {{ data.lastResult || 'N/A' }}
          </Tag>
        </template>
      </Column>

      <Column field="lastRunAt" header="마지막 실행시각" :sortable="true" style="width:250px">
        <template #body="{ data }">
          {{ data.lastRunAt ? formatDate(data.lastRunAt) : '미수행' }}
        </template>
      </Column>

      <Column header="로그" :sortable="false" style="width:100px">
        <template #body="{ data }">
          <Button icon="pi pi-list" @click="openLogDialog(data)" outlined size="small" />
        </template>
      </Column>

      <Column header="삭제" :sortable="false" style="width:100px">
        <template #body="{ data }">
          <Button icon="pi pi-trash" @click="deleteJob(data)" severity="danger" outlined size="small" />
        </template>
      </Column>
    </DataTable>

    <!-- 상세정보 Dialog -->
    <Dialog v-model:visible="showJobModal" header="ETL JOB 상세정보" width="600" modal>
      <div v-if="selectedJob">
        <p><b>작업명:</b> {{ selectedJob.jobName }}</p>
        <p><b>스케줄:</b> {{ selectedJob.schedule }}</p>
        <p><b>상태:</b> {{ selectedJob.status }}</p>
        <p><b>관제 모듈:</b> {{ selectedJob.monitorModule?.label || selectedJob.monitorModuleLabel || 'N/A' }}</p>

        <div v-if="selectedJob.extractQueries && Object.keys(selectedJob.extractQueries).length">
          <p><b>쿼리 목록 (DB별)</b></p>
          <ul>
            <li v-for="(query, dbType) in selectedJob.extractQueries" :key="dbType">
              <b>{{ dbType }}:</b> <code>{{ query }}</code>
            </li>
          </ul>
        </div>

        <p><b>Source DBs:</b></p>
        <ul>
          <li v-for="src in selectedJob.sourceDbs" :key="src.id">
            {{ src.dbName }} ({{ src.dbType }})
          </li>
        </ul>

        <p><b>Target DB:</b> {{ selectedJob.targetDb?.dbName }} ({{ selectedJob.targetDb?.dbType }})</p>
      </div>
      <template #footer>
        <Button label="닫기" @click="showJobModal = false" />
      </template>
    </Dialog>

    <!-- 실행 로그 Dialog -->
    <Dialog v-model:visible="logDialogVisible" header="실행 로그" width="900" modal>
      <ETLJobLog
        v-if="logDialogVisible"
        :jobId="logJobId"
        @retry="retryJob"
        @close="logDialogVisible = false"
      />
      <template #footer>
        <Button label="닫기" @click="logDialogVisible = false" />
      </template>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Button from 'primevue/button';
import Dialog from 'primevue/dialog';
import ProgressBar from 'primevue/progressbar';
import Tag from 'primevue/tag';
import ETLJobLog from './ETLJobLog.vue';
import api from "@/api"; // axios 인스턴스
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useToast } from 'primevue/usetoast';

const toast = useToast();

const jobs = ref([]);
const dbList = ref([]);
const modules = ref([]);

const selectedJob = ref(null);
const showJobModal = ref(false);

const loadingJobId = ref(null);

const logDialogVisible = ref(false);
const logJobId = ref(null);

function openLogDialog(job) {
  logJobId.value = job.id;
  logDialogVisible.value = true;
}

/** 모듈 먼저 로드 -> 잡 로드 (매핑 정확성 보장) */
async function fetchModules() {
  const { data } = await api.get("/api/monitor-module");
  modules.value = data;
}

async function fetchJobs() {
  const { data } = await api.get("/api/etl/job");
  jobs.value = data.map(job => {
    const matchedModule = modules.value.find(mod => mod.id === job.monitorModuleId);
    return {
      ...job,
      monitorModuleLabel: matchedModule?.moduleName || matchedModule?.label || 'N/A'
    };
  });
}

async function refreshJobs() {
  try {
    // 모듈을 먼저 갱신하고 그 기준으로 잡 매핑
    await fetchModules();
    await fetchJobs();
    toast.add({ severity: 'info', summary: '새로고침', detail: '목록이 갱신되었습니다.', life: 2000 });
  } catch (e) {
    toast.add({ severity: 'error', summary: '새로고침 실패', detail: e.response?.data?.message || '에러 발생', life: 3000 });
  }
}

function fetchDbList() {
  api.get("/api/db-connection").then(res => {
    dbList.value = res.data.map(db => ({
      id: db.id,
      name: `[${db.dbType}] ${db.host}:${db.port}/${db.dbName} (${db.description || db.username})`
    }));
  });
}

// 재수행 (로그 Dialog에서 emit 받아서 실행)
const retryJob = async (log) => {
  const jobId = log.jobId;
  const sourceDbId = log.sourceDbId;

  if (!jobId || !sourceDbId) {
    toast.add({ severity: 'warn', summary: '재수행 불가', detail: '실행 정보를 찾을 수 없습니다.', life: 3000 });
    return;
  }
  try {
    await api.post(`/api/etl/job/${jobId}/retry/${sourceDbId}`);
    toast.add({ severity: 'success', summary: '재수행 완료', detail: '해당 작업이 다시 실행되었습니다.', life: 3000 });
  } catch (e) {
    toast.add({ severity: 'error', summary: '재수행 실패', detail: e.response?.data?.message || '에러 발생', life: 4000 });
  }
};

function deleteJob(job) {
  if (confirm("삭제할까요?")) {
    api.delete(`/api/etl/job/${job.id}`).then(() => {
      refreshJobs();
    });
  }
}

function runJob(job) {
  loadingJobId.value = job.id;
  api.post(`/api/etl/job/run/${job.id}`)
    .then(res => {
      toast.add({
        severity: 'success',
        summary: 'ETL 실행',
        detail: res.data || '실행 완료!',
        life: 3000
      });
    })
    .catch(e => {
      toast.add({
        severity: 'error',
        summary: '실행 실패',
        detail: e.response?.data?.message || '에러 발생',
        life: 4000
      });
    })
    .finally(() => {
      loadingJobId.value = null;
    });
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  // 화면 표시만 로컬 포맷, 정렬은 원본 값(lastRunAt) 기준으로 DataTable가 처리
  return d.toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' });
}

async function openJobDetail(jobId) {
  if (!jobId) {
    alert('jobId가 없습니다!');
    return;
  }
  try {
    const { data } = await api.get(`/api/etl/job/${jobId}/db-info`);
    const job = jobs.value.find(j => j.id === jobId);
    if (!job) {
      alert('화면에 표시된 JOB을 찾을 수 없습니다.');
      return;
    }
    selectedJob.value = {
      ...job,
      sourceDbs: data.sourceDbs || [],
      targetDb: data.targetDb || null,
      monitorModule: data.monitorModule || null,
      extractQueries: data.extractQueries || {}
    };
    showJobModal.value = true;
  } catch (err) {
    alert('상세 정보 조회 중 에러 발생: ' + (err.response?.data?.message || err.message));
    console.error(err);
  }
}

let stompClient = null;
function connectWebSocket() {
  stompClient = new Client({
    brokerURL: null,
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    onConnect: () => {
      stompClient.subscribe('/topic/etl-job-status', message => {
        const msg = JSON.parse(message.body);
        const idx = jobs.value.findIndex(j => j.id === msg.jobId);
        if (idx > -1) {
          jobs.value[idx] = {
            ...jobs.value[idx],
            lastResult: msg.result,
            lastRunAt: msg.lastRunAt,
          };
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

async function initData() {
  // 정렬/매핑이 깨지지 않도록 순서 보장
  await fetchModules();
  await fetchJobs();
  fetchDbList();
}

onMounted(() => {
  initData();
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
