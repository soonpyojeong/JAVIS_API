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
      selectionMode="single"
      :selection="selectedJob"
      @update:selection="selectedJob = $event"
    >
      <Column field="id" header="ID" :sortable="true" style="width:60px"/>
      <Column field="jobName" header="JOB이름">
        <template #body="{data}">
          {{ dbList.find(d => d.id === data.jobName)?.name || data.jobName }}
        </template>
      </Column>
      <Column field="targetDbId" header="타겟DB">
        <template #body="{data}">
          {{ dbList.find(d => d.id === data.targetDbId)?.name || data.targetDbId }}
        </template>
      </Column>
      <Column header="상세">
        <template #body="{ data }">
          <Button icon="pi pi-eye" size="small" @click="openJobDetail(data.id)" outlined />
        </template>
      </Column>
      <Column field="schedule" header="스케줄"/>
      <Column field="status" header="상태"/>
        <Column header="실행">
          <template #body="{data}">
            <Button icon="pi pi-play" @click="runJob(data)" severity="success" size="small"/>
            <ProgressBar
              v-if="loadingJobId === data.id"
              mode="indeterminate"
              style="height:6px; margin-top:12px"
            />
          </template>
        </Column>
      <Column field="lastResult" header="최종 결과">
        <template #body="{ data }">
          <Tag :severity="data.lastResult === 'FAIL' ? 'danger' : 'success'">
            {{ data.lastResult }}
          </Tag>
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

    <Dialog v-model:visible="showJobModal" header="ETL JOB 상세정보" width="600" modal>
      <div v-if="selectedJob">
        <p><b>작업명:</b> {{ selectedJob.jobName }}</p>
        <p><b>스케줄:</b> {{ selectedJob.schedule }}</p>
        <p><b>상태:</b> {{ selectedJob.status }}</p>
        <p><b>관제 모듈:</b> {{ selectedJob.monitorModule?.label || 'N/A' }}</p>

        <div v-if="selectedJob.monitorModule?.queryList">
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
        <Button label="닫기" @click="showJobModal = false"/>
      </template>
    </Dialog>

    <!-- 실행 로그 (배치별 트리 구조) -->
     <Dialog v-model:visible="logDialog" header="실행 결과 로그 (트리)" width="900" modal>
       <!-- 상단 새로고침 버튼 추가 -->
       <div class="flex justify-end mb-2">
         <Button
           label="새로고침"
           icon="pi pi-refresh"
           @click="refreshTree"
           size="small"
           outlined
         />
       </div>
       <TreeTable :value="treeData" :tableStyle="{ minWidth: '60rem' }">
         <Column field="executedAt" header="배치 실행일/DB명" expander>
           <template #body="{ node }">
             <span v-if="node.type === 'batch'">🗂️ {{ formatDate(node.executedAt) }}</span>
             <span v-else>{{ node.sourceDbName }}</span>
           </template>
         </Column>
         <Column field="status" header="결과">
           <template #body="{ node }">
             <Tag v-if="node.type === 'batch'" :severity="node.status === 'FAIL' ? 'danger' : 'success'">
               {{ node.status }}
             </Tag>
             <Tag v-else :severity="node.result === 'FAIL' ? 'danger' : 'success'">
               {{ node.result }}
             </Tag>
           </template>
         </Column>
         <Column field="executedAt" header="실행시각">
           <template #body="{ node }">
             <span v-if="node.type === 'log'">{{ formatDate(node.executedAt) }}</span>
           </template>
         </Column>
         <Column field="message" header="메시지">
           <template #body="{ node }">
             <span v-if="node.type === 'log'">{{ node.message }}</span>
           </template>
         </Column>
         <Column header="작업">
           <template #body="{ node }">
             <Button
               v-if="node.type === 'log' && node.result === 'FAIL'"
               label="재수행"
               icon="pi pi-refresh"
               severity="danger"
               size="small"
               @click="retryJob(node)"
             />
           </template>
         </Column>
       </TreeTable>
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
import TreeTable from 'primevue/treetable'
import ProgressBar from 'primevue/progressbar'
import Tag from 'primevue/tag'
import api from "@/api"; // axios 인스턴스
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import MappingSimulate from './MappingSimulate.vue'
import { useToast } from 'primevue/usetoast';
const logTreeDialog = ref(false)
const toast = useToast();
const treeData = ref([]);
const jobs = ref([]);
const logs = ref([]);
const dbList = ref([]);
const jobDialog = ref(false);
const logDialog = ref(false);
const editMode = ref(false);
const editJob = ref({});
const simulateDialog = ref(false);
const simulateJob = ref(null);
const selectedJob = ref(null)
const showJobModal = ref(false)
const modules = ref([])
const selectedJobId = ref(null);
const batchLogs = ref([]); // 배치별 그룹핑 로그
const loading = ref(false)
const loadingJobId = ref(null);


function openLogTree(job) {
  selectedJob.value = job;
  logTreeDialog.value = true;
}


function fetchModules() {
  api.get("/api/monitor-module").then(res => {
    modules.value = res.data
  })
}

// 새로고침 함수 (현재 선택된 job의 로그 트리 갱신)
function refreshTree() {
  if (!selectedJob.value || !selectedJob.value.id) return;
  fetchBatchLogs(selectedJob.value.id)
    .then(() => {
      treeData.value = buildTreeTableData(batchLogs.value);
    });
}

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
// 기존 batchLogs -> 트리 구조로 변환
function buildTreeTableData(batchLogs) {
  return batchLogs.map(batch => ({
    key: 'batch-' + batch.batchId,
    type: 'batch',
    executedAt: batch.executedAt,
    status: getBatchStatus(batch),
    batchId: batch.batchId,
    sourceDbName: '', // batch엔 DB명이 없음
    message: '',
    // 로그를 children으로 추가
    children: batch.logs.map(log => ({
      key: 'log-' + log.logId,
      type: 'log',
      sourceDbName: log.sourceDbName,
      executedAt: log.executedAt,
      result: log.result,
      message: log.message,
      jobId: log.jobId,
      sourceDbId: log.sourceDbId,
      logId: log.logId,
      status: log.result // log에서는 status == result
    }))
  }));
}




async function fetchJobs() {
  const { data } = await api.get("/api/etl/job")
  jobs.value = data.map(job => {
    const matchedModule = modules.value.find(mod => mod.id === job.monitorModuleId)
    return {
      ...job,
      monitorModuleLabel: matchedModule?.moduleName || matchedModule?.label || 'N/A'
    }
  })
}
function fetchDbList() {
  api.get("/api/db-connection").then(res => {
    dbList.value = res.data.map(db => ({
      id: db.id,
      name: `[${db.dbType}] ${db.host}:${db.port}/${db.dbName} (${db.description || db.username})`
    }));
  });
}


// 배치의 결과는 logs 중 하나라도 FAIL이면 FAIL
function getBatchStatus(batch) {
  return batch.logs.some(log => log.result === 'FAIL') ? 'FAIL' : 'SUCCESS';
}

// 재수행
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
    // 재조회 및 트리 갱신
    await fetchBatchLogs(jobId);
    treeData.value = buildTreeTableData(batchLogs.value);
  } catch (e) {
    toast.add({ severity: 'error', summary: '재수행 실패', detail: e.response?.data?.message || '에러 발생', life: 4000 });
  }
};


// 배치 단위로 로그 그룹핑해서 가져오기 (API 설계에 맞게 수정)
async function fetchBatchLogs(jobId) {
  // API에서 바로 배치별 구조로 내려주는게 베스트
  // 예: /api/etl/job/{jobId}/batch-logs
  const { data } = await api.get(`/api/etl/job/${jobId}/batch-logs`);
  batchLogs.value = data; // [{ batchId, executedAt, logs: [...] }, ...]

}



function openAddDialog() {
  editMode.value = false;
  editJob.value = { jobName: '', sourceDbId: null, targetDbId: null, targetTable: '', schedule: '', status: 'ACTIVE' };
  jobDialog.value = true;
}
function openEditDialog(event) {
  editMode.value = true;
  editJob.value = {...event.data};
  console.log('[openEditDialog] editJob:', editJob.value); // ← 콘솔에서 직접 확인!
  showJobModal.value = true;
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
  loading.value = true; // 실행 시작할 때 로딩 시작
  api.post(`/api/etl/job/run/${job.id}`)
    .then(res => {
      toast.add({
        severity: 'success',
        summary: 'ETL 실행',
        detail: res.data || '실행 완료!',
        life: 3000
      });
      fetchLogs(job.id);
    })
    .catch(e => {
      toast.add({
        severity: 'error',
        summary: '실행 실패',
        detail: e.response?.data?.message || '에러 발생',
        life: 4000
      });
      fetchLogs(job.id);
    })
    .finally(() => {
      loading.value = false; // 무조건 로딩 바 닫기
    });
}


// 로그 Dialog 열기: API로 batch별 그룹핑 로그 받아오기
async function openLogDialog(job) {
  if (!job || !job.id) {
    toast.add({ severity: 'warn', summary: '실패', detail: 'JOB ID가 없습니다', life: 3000 });
    return;
  }
  await fetchBatchLogs(job.id); // batchLogs.value 세팅
  treeData.value = buildTreeTableData(batchLogs.value); // 트리로 변환
  logDialog.value = true;
  console.log(batchLogs.value);
  console.log(treeData.value);
}


function fetchLogs(jobId) {
    if (!jobId) {
      alert('작업을 선택해주세요!');
      return;
    }
  api.get(`/api/etl/job/${jobId}/logs`)
    .then(res => {
      logs.value = res.data.sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt)); // 최신순 정렬
    })
    .catch(err => {
      console.error("로그 조회 실패:", err);
      logs.value = [];
    });
}


function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const formatted = d.toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' });
  return formatted;
}

async function openJobDetail(jobId) {
  const { data } = await api.get(`/api/etl/job/${jobId}/db-info`)
  const job = jobs.value.find(j => j.id === jobId)
  selectedJob.value = {
    ...job,
    sourceDbs: data.sourceDbs,
    targetDb: data.targetDb,
    monitorModule: data.monitorModule,
    extractQueries: data.extractQueries || {}
  }
  showJobModal.value = true
}



onMounted(() => {
  fetchJobs()
  fetchDbList()
  fetchModules()
  connectWebSocket()
})
</script>

<style scoped>
.etl-job-list-container {
  padding: 2rem 1rem;
  max-width: 1300px;
  margin: auto;
}
</style>
