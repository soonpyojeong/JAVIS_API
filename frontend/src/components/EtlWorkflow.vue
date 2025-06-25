<template>
  <div class="main-layout">
    <!-- 워크플로우 상단바 -->
    <div class="workflow-header">
      <input
        v-model="workflowName"
        class="workflow-name-input"
        placeholder="워크플로우 이름을 입력하세요"
      />
      <Button class="mr-2" label="신규" icon="pi pi-plus" @click="newWorkflow" />
      <Button class="mr-2" label="저장" icon="pi pi-save" @click="saveWorkflow" />
      <Button label="JOB등록" icon="pi pi-share-alt" @click="openJobFromWorkflow()" />
      <Button class="mr-2" label="불러오기" icon="pi pi-download" @click="openLoadDialog" />
      <Button label="이력보기" icon="pi pi-history" @click="openHistoryDialog" />
      <Button label="JSON보기" icon="pi pi-eye" class="mr-2" @click="showJsonModal = true" />
      <Button label="삭제" icon="pi pi-trash" class="mr-2" severity="danger" :disabled="!workflowId" @click="deleteWorkflow"/>

    </div>

    <div style="display:flex; flex:1 1 0%; min-height:0;">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="description">[관제 모듈] → 워크플로우로 드래그</div>
        <div class="nodes">
          <div
            v-for="mod in moduleList"
            :key="mod.id"
            class="monitor-module"
            :style="{ background: mod.color }"
            :draggable="true"
            @dragstart="(e) => onDragStart(e, {
              ...mod,
              queries: Object.fromEntries((mod.queryList || []).map(q => [q.dbType, q.queryText])),
              isModule: true
            })"
          >
            {{ mod.label }}
            <span v-if="mod.supportedDbTypes" class="text-xs text-gray-100 ml-2">
              ({{ mod.supportedDbTypes }})
            </span>
          </div>
        </div>
        <div class="description" style="margin-top:16px;">[DB 리스트] → 워크플로우로 드래그</div>
        <div class="nodes">
          <div
            v-for="db in dbList"
            :key="db.id"
            class="db-node"
            :style="{ background: db.color || '#777' }"
            :draggable="true"
            @dragstart="(e) =>
              onDragStart(e, {
                ...db,
                label: db.description || db.label,
                type: db.dbType || db.type,
                color: db.color || '#777'
              })
            "
          >
            {{ db.dbName || db.label }}
            <span style="font-size:11px; margin-left:6px;">({{ db.dbType || db.type }})</span>
          </div>
        </div>
      </aside>

      <!-- Workflow Canvas -->
      <div class="workflow-canvas" @drop="handleDrop" @dragover="handleDragOver">
        <VueFlow
          v-model:nodes="nodes"
          v-model:edges="edges"
          fit-view-on-init
          :default-zoom="1.2"
          :min-zoom="0.2"
          :max-zoom="4"
          :node-types="{
            custom: markRaw((props) => h(CustomNode, {
              ...props,
              onDelete: handleNodeDelete,
              onClick: () => handleNodeClick(props.data),
              onRoleChange: handleNodeRoleChange,
              onUpdateTargetTable: handleUpdateTargetTable // 이 부분!
            }))
          }"
          :edge-types="{ custom: markRaw(CustomEdge) }"
        >
          <Background pattern-color="#aaa" :gap="8" />
          <MiniMap />
          <Controls />
        </VueFlow>
      </div>
    </div>
    <!-- 워크플로우 JSON 미리보기 -->
    <Dialog v-model:visible="showJsonModal" header="워크플로우 JSON 미리보기" modal :style="{ width: '700px' }">
      <pre style="font-size:13px; background:#f4f8fc; border-radius:8px; padding:15px; overflow-x:auto;">
    {{ JSON.stringify({ nodes: nodes.value, edges: edges.value }, null, 2) }}
      </pre>
      <pre>{{ JSON.stringify(nodes.value, null, 2) }}</pre>
      <template #footer>
        <Button label="닫기" @click="showJsonModal = false" />
      </template>
    </Dialog>

    <!-- 상세정보 Dialog -->
    <Dialog
      v-model:visible="showModal"
      modal
      :style="{ width: '420px', borderRadius: '22px' }"
      :closable="true"
      :draggable="false"
      :header="selectedNode?.label || '모듈 상세 정보'"
      class="custom-modal"
      @hide="selectedNode = null"
    >
      <div v-if="selectedNode" class="modal-detail-content">
        <div class="modal-row">
          <div class="modal-label">모듈명</div>
          <div class="modal-value">{{ selectedNode.label }}</div>
        </div>
        <div class="modal-row">
          <div class="modal-label">지원 DB</div>
          <div class="modal-value">
            <span v-if="selectedNode.queries">{{ Object.keys(selectedNode.queries).join(', ') }}</span>
          </div>
        </div>
        <div class="modal-row">
          <div class="modal-label">DB별 쿼리</div>
          <div class="modal-value">
            <ul class="query-list">
              <li v-for="(q, db) in selectedNode.queries" :key="db">
                <b class="db-badge">{{ db }}</b>
                <code class="query-text">{{ q }}</code>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </Dialog>
    <!-- JOB 등록 모달 -->
    <Dialog v-model:visible="jobDialog" modal header="워크플로우 기반 JOB 등록" :style="{ width: '600px' }">
      <div class="p-fluid">
        <div class="p-field">
          <label>JOB 이름</label>
          <InputText v-model="editJob.jobName" />
        </div>
        <div class="p-field">
          <label>소스 DB</label>
          <ul>
            <li v-for="(name, idx) in editJob.sourceDbNames" :key="idx">
              • {{ name }}
            </li>
          </ul>
        </div>
        <div class="p-field">
          <label>타겟 DB</label>
          <div>{{ editJob.targetDbName || '-' }}</div>
        </div>
        <div class="p-field">
          <label>대상 테이블</label>
          <InputText v-model="editJob.targetTable" />
        </div>
        <div class="p-field">
          <label>관제 모듈</label>
          <div>{{ editJob.monitorModuleName || '-' }}</div>
        </div>

        <div class="p-field">
          <label>스케줄</label>
          <InputText v-model="editJob.schedule" placeholder="예: 0 0 * * *" />
        </div>
        <div class="p-field">
          <label>관제 모듈 쿼리 (DB별)</label>
          <ul class="query-list">
            <li v-for="(q, db) in editJob.queries" :key="db">
              <b class="db-badge">{{ db }}</b>
              <code class="query-text">{{ q }}</code>
            </li>
          </ul>
        </div>
        <div class="p-field">
          <label>상태</label>
          <Dropdown v-model="editJob.status" :options="['ACTIVE', 'INACTIVE']" />
        </div>
      </div>

      <template #footer>
        <Button label="취소" icon="pi pi-times" @click="jobDialog = false" class="p-button-text" />
        <Button label="등록" icon="pi pi-check" @click="submitJob" />
      </template>
    </Dialog>

    <!-- 워크플로우 불러오기 Dialog -->
    <Dialog v-model:visible="showLoadModal" modal :header="'워크플로우 불러오기'">
      <DataTable
        :value="workflowList"
        selectionMode="single"
        :rows="10"
        paginator
        responsiveLayout="scroll"
        :selection="selectedWorkflow"
        @rowClick="e => selectWorkflow(e.data)"
        dataKey="workflowId"
        style="min-width: 540px;"
      >
        <Column field="workflowName" header="워크플로우명" style="width:40%;" />
        <Column field="createdAt" header="생성일" style="width:28%;">
          <template #body="{ data }">
            {{ data.createdAt?.split('T')[0] || '-' }}
          </template>
        </Column>
        <Column field="updatedAt" header="수정일" style="width:28%;">
          <template #body="{ data }">
            {{ data.updatedAt?.split('T')[0] || '-' }}
          </template>
        </Column>
        <Column header="불러오기" style="width:70px; text-align:center;">
          <template #body="{ data }">
            <Button icon="pi pi-download" text rounded @click.stop="selectWorkflow(data)" />
          </template>
        </Column>
      </DataTable>
    </Dialog>


    <!-- 워크플로우 이력 Dialog -->
    <Dialog v-model:visible="showHistoryModal" modal :header="'이력 보기'">
      <ul>
        <li
          v-for="hist in workflowHistory"
          :key="hist.historyId"
          style="margin-bottom:8px"
        >
          <span>{{ hist.changedAt  }} | {{ hist.changeDesc || '' }}</span>
          <Button size="small" label="불러오기" class="ml-2" @click="restoreHistory(hist)" />
        </li>
      </ul>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, markRaw, h,computed } from 'vue'
import { useStore } from 'vuex'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import CustomNode from './CustomNode.vue'
import CustomEdge from './CustomEdge.vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'


import { useToast } from 'primevue/usetoast'
import api from '@/api'

import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Dropdown from 'primevue/dropdown'

// 상태
const workflowName = ref('')
const workflowId = ref(null)
const nodes = ref([])
const edges = ref([])
const moduleList = ref([])
const dbList = ref([])
const showModal = ref(false)
const showLoadModal = ref(false)
const showHistoryModal = ref(false)
const workflowList = ref([])
const workflowHistory = ref([])
const selectedNode = ref(null)
const showJsonModal = ref(false)
const toast = useToast()
const selectedWorkflow = ref(null)

const store = useStore()
const user = computed(() => store.state.user || {})
const editMode = ref(false)
const editJob = ref(null)
const jobDialog = ref(false)



function isWorkflowValid() {
  // 1. 노드가 3개(소스, 모듈, 타겟) 이상이어야 하고
  if (nodes.value.length < 3) return false;
  // 2. edges(엣지)가 2개(소스→모듈, 모듈→타겟) 이상이어야 함
  if (edges.value.length < 2) return false;
  // 3. 각 필수 연결이 실제로 존재하는지 확인 (강화)
  // 예시: 소스→모듈, 모듈→타겟
  const hasSourceToModule = edges.value.some(e => {
    const sourceNode = nodes.value.find(n => n.id === e.source);
    const targetNode = nodes.value.find(n => n.id === e.target);
    return sourceNode?.data.role === 'source' && targetNode?.data.isModule;
  });
  const hasModuleToTarget = edges.value.some(e => {
    const sourceNode = nodes.value.find(n => n.id === e.source);
    const targetNode = nodes.value.find(n => n.id === e.target);
    return sourceNode?.data.isModule && targetNode?.data.role === 'target';
  });
  return hasSourceToModule && hasModuleToTarget;
}

function openJobFromWorkflow() {
  const workflow = {
    workflowName: workflowName.value,
    schedule: '',
    nodes: nodes.value,
    edges: edges.value,
  };

  const jobObj = convertWorkflowToJob(workflow);
  editMode.value = false;
  editJob.value = jobObj;
  jobDialog.value = true;
}




function convertWorkflowToJob(workflow) {
  const sourceDbs = workflow.nodes.filter(n => n.data.role === 'source');
  const targetDb = workflow.nodes.find(n => n.data.role === 'target');
  const monitorModule = workflow.nodes.find(n => n.data.isModule);

  return {
    jobName: workflow.workflowName,
    sourceDbIds: sourceDbs.map(db => db.data.id),
    sourceDbNames: sourceDbs.map(db => db.label || db.data.dbName), // ✅ 추가
    monitorModuleId: monitorModule?.data.id,
    monitorModuleName: monitorModule?.label, // ✅ 관제 모듈명도 추가
    targetDbId: targetDb?.data.id,
    targetDbName: targetDb?.label || targetDb?.data.dbName, // ✅ 타겟 DB 이름
    targetTable: targetDb?.data.targetTable,
    schedule: workflow.schedule || '',
    status: 'ACTIVE',
    extractQueries: monitorModule?.data.queries || {}
  };
}


async function submitJob() {
  try {
    const { data } = await api.post('/api/etl/job', editJob.value)
    toast.add({
      severity: 'success',
      summary: '알림',
      detail: 'JOB 등록 완료!',
      life: 3000 // 3초
    });
    //console.log('[등록 요청 payload]', JSON.stringify(editJob.value, null, 2));
    jobDialog.value = false
  } catch (e) {
    console.error(e)
    alert('JOB 등록 실패: ' + (e?.response?.data || e.message))
    toast.add({
          severity: 'error',
          summary: '등록 실패',
          detail: 'JOB 등록 에러발생 !'||e.response?.data?.message,
          life: 3000 // 3초
        });
  }
}



// 초기 데이터 불러오기
onMounted(() => {
  loadModuleList()
  loadDbList()
})

async function loadModuleList() {
  const { data } = await api.get('/api/monitor-module/with-queries')
  moduleList.value = data.map(mod => {
    const queriesMap = (Array.isArray(mod.queries) ? mod.queries : []).reduce((acc, q) => {
      if (q.dbType && q.queryText) {
        acc[q.dbType] = q.queryText
      }
      return acc
    }, {})
    return {
      ...mod,
      label: mod.moduleName || mod.label,
      color: mod.color || '#888',
      queries: queriesMap
    }
  })
}


async function loadDbList() {
  const { data } = await api.get('/api/db-connection')
  dbList.value = data
}

// 워크플로우 저장
async function saveWorkflow() {
  // 1. 워크플로우 이름 입력 여부 체크
  if (!workflowName.value) {
    toast.add({
      severity: 'warn',
      summary: '알림',
      detail: '워크플로우 이름을 입력하세요!',
      life: 3000
    });
    return;
  }

  // 2. 노드 개수 체크 (최소 3개: 소스, 모듈, 타겟)
  if (nodes.value.length < 3) {
    toast.add({
      severity: 'warn',
      summary: '알림',
      detail: '노드가 3개(소스, 관제 모듈, 타겟) 이상이어야 합니다!',
      life: 3000
    });
    return;
  }

  // 3. 엣지 개수 체크 (최소 2개: 소스→모듈, 모듈→타겟)
  if (edges.value.length < 2) {
    toast.add({
      severity: 'warn',
      summary: '알림',
      detail: '엣지가 2개 이상(소스-모듈, 모듈-타겟) 연결되어야 합니다!',
      life: 3000
    });
    return;
  }

  // 4. 필수 엣지 연결 체크 (소스→모듈, 모듈→타겟)
  const hasSourceToModule = edges.value.some(e => {
    const sourceNode = nodes.value.find(n => n.id === e.source);
    const targetNode = nodes.value.find(n => n.id === e.target);
    return sourceNode?.data.role === 'source' && targetNode?.data.isModule;
  });
  const hasModuleToTarget = edges.value.some(e => {
    const sourceNode = nodes.value.find(n => n.id === e.source);
    const targetNode = nodes.value.find(n => n.id === e.target);
    return sourceNode?.data.isModule && targetNode?.data.role === 'target';
  });

  if (!hasSourceToModule || !hasModuleToTarget) {
    toast.add({
      severity: 'warn',
      summary: '알림',
      detail: '소스 → 관제모듈, 관제모듈 → 타겟 DB 연결(엣지)이 모두 필요합니다!',
      life: 4000
    });
    return;
  }

  // 5. 저장 요청
  const userId = user.value.userId || user.value.username || 'anonymous';
  const body = {
    workflowId: workflowId.value,
    workflowName: workflowName.value,
    workflowJson: JSON.stringify({ nodes: nodes.value, edges: edges.value }),
    description: ''
  };
  try {
    const { data } = await api.post(`/api/workflow/save?user=${encodeURIComponent(userId)}`, body);
    if (data && data.workflowId) {
      workflowId.value = data.workflowId;
      toast.add({
        severity: 'success',
        summary: '알림',
        detail: '저장 완료!',
        life: 3000
      });
    }
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: '실패',
      detail: '저장 실패! 서버 에러 또는 필드 오류',
      life: 3000
    });
  }
}




async function deleteWorkflow() {
  if (!workflowId.value) {
    toast.add({
      severity: 'warn',
      summary: '알림',
      detail: '삭제할 워크플로우를 먼저 불러오세요!',
      life: 3000
    });
    return
  }
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await api.delete(`/api/workflow/${workflowId.value}`)
    toast.add({
      severity: 'success',
      summary: '알림',
      detail: '삭제 완료!',
      life: 3000
    });
    workflowId.value = null
    workflowName.value = ''
    nodes.value = []
    edges.value = []
    workflowList.value = []
    await openLoadDialog()
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: '실패',
      detail: '삭제 실패! 서버 에러',
      life: 3000
    });
  }
}


function newWorkflow() {
  if (nodes.value.length > 0 || workflowName.value) {
    if (!confirm('현재 작업 중인 워크플로우를 초기화할까요? 저장하지 않은 데이터는 사라집니다.')) return;
  }
  workflowName.value = '';
  workflowId.value = null;
  nodes.value = [];
  edges.value = [];
}



// 워크플로우 불러오기 관련
async function openLoadDialog() {
  const { data } = await api.get('/api/workflow/list')
  workflowList.value = data
  showLoadModal.value = true
}

function selectWorkflow(wf) {
  workflowName.value = wf.workflowName
  workflowId.value = wf.workflowId
  try {
    const wfData = JSON.parse(wf.workflowJson || '{}')
    nodes.value = wfData.nodes || []
    edges.value = wfData.edges || []
  } catch (e) {
    nodes.value = []
    edges.value = []
  }
  showLoadModal.value = false
  loadWorkflowHistory()
  //console.log(nodes.value)
}


async function loadWorkflowHistory() {
  if (!workflowId.value) return
  const { data } = await api.get(`/api/workflow/${workflowId.value}/history`)
  workflowHistory.value = data
}
// 이력 보기 관련
async function openHistoryDialog() {
  if (!workflowId.value) {
    toast.add({
      severity: 'info',
      summary: '알림',
      detail: '워크플로우를 먼저 선택/불러오세요',
      life: 3000
    });
    return
  }
  const { data } = await api.get(`/api/workflow/${workflowId.value}/history`)
  workflowHistory.value = data
  showHistoryModal.value = true
}


function restoreHistory(hist) {
  try {
    const wfData = JSON.parse(hist.workflowJson || '{}')
    nodes.value = wfData.nodes || []
    edges.value = wfData.edges || []
    showHistoryModal.value = false
    toast.add({
      severity: 'success',
      summary: '알림',
      detail: '과거 이력 불러오기 완료 (저장 버튼을 누르면 반영됨)',
      life: 3000
    })
  } catch (e) {
    toast.add({
      severity: 'error',
      summary: '실패',
      detail: '이력 불러오기 실패 (JSON 파싱 오류)',
      life: 4000
    })
  }
}

function handleNodeClick(data) {
  selectedNode.value = data
  showModal.value = true
}

// VueFlow 드래그/드롭
const { onConnect, addEdges, project } = useVueFlow()
function onDragStart(e, data) {
  e.dataTransfer.setData('application/node', JSON.stringify(data))
}
function handleDrop(e) {
  const data = JSON.parse(e.dataTransfer.getData('application/node'))
  //console.log("드롭된 노드 데이터:", data)

  const bounds = e.target.getBoundingClientRect()
  const position = project({
    x: e.clientX - bounds.left,
    y: e.clientY - bounds.top
  })

  // 만약 DB 노드(type이 ORACLE, TIBERO 등)라면 기본 role을 'source'로
  const dbTypes = ['ORACLE','TIBERO','MYSQL','MSSQL','POSTGRESQL']
  const isDbNode = dbTypes.includes(data.type)
  nodes.value.push({
    id: `n_${Date.now()}`,
    type: 'custom',
    label: data.label,
    position,
    data: {
      ...data,
      role: isDbNode ? 'source' : undefined // DB면 source, 관제모듈은 role 없음
    }

  })
}


function handleNodeRoleChange(nodeId, nextRole) {
  const idx = nodes.value.findIndex(n => n.id === nodeId)
  if (idx >= 0) nodes.value[idx].data.role = nextRole
}

function handleUpdateTargetTable(nodeId, tableName) {
  const node = nodes.value.find(n => n.id === nodeId)
  if (node) node.data.targetTable = tableName
}

function handleDragOver(e) { e.preventDefault() }
function handleNodeDelete(nodeId) {
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId)
}
onConnect((params) => {
  addEdges([{ ...params, type: 'custom' }])
})
</script>

<style scoped>
.main-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #eaf1fa;
}
.workflow-header {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #f5f7fb;
  padding: 10px 16px 10px 12px;
  border-bottom: 1.5px solid #d6e2f5;
}
.workflow-name-input {
  border: 1px solid #b7cdf5;
  background: #fff;
  border-radius: 7px;
  padding: 5px 13px;
  font-size: 15px;
  min-width: 210px;
  margin-right: 12px;
  outline: none;
}
.sidebar {
  width: 200px;
  min-width: 180px;
  max-width: 230px;
  background: #f8fafc;
  padding: 14px 10px 0 10px;
  border-right: 1px solid #eee;
  height: 100vh;
  overflow-y: auto;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif;
}
.workflow-canvas {
  flex: 1 1 0%;
  height: 80vh;
  background: #f2f6fc;
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.description {
  font-size: 13px;
  margin-bottom: 8px;
  font-weight: 600;
  color: #4576ca;
}
.nodes {
  margin-bottom: 18px;
}
.monitor-module,
.db-node {
  border-radius: 7px;
  color: #fff;
  font-weight: 600;
  cursor: grab;
  padding: 7px 13px;
  margin-bottom: 8px;
  box-shadow: 0 1px 6px 0 #b6c8f74d;
  user-select: none;
  transition: box-shadow .12s;
}
.monitor-module:hover,
.db-node:hover {
  box-shadow: 0 2px 10px 1px #4576ca44;
}


.description {
  font-size: 13px;
  margin-bottom: 8px;
  font-weight: 600;
  color: #4576ca;
}
.nodes {
  margin-bottom: 18px;
}
.monitor-module,
.db-node {
  border-radius: 7px;
  color: #fff;
  font-weight: 600;
  cursor: grab;
  padding: 7px 13px;
  margin-bottom: 8px;
  box-shadow: 0 1px 6px 0 #b6c8f74d;
  user-select: none;
  transition: box-shadow .12s;
}
.monitor-module:hover,
.db-node:hover {
  box-shadow: 0 2px 10px 1px #4576ca44;
}



/* 모달 스타일 */
.custom-modal .p-dialog-header {
  background: #f8fafc;
  border-radius: 18px 18px 0 0;
  font-size: 1.18em;
  font-weight: 700;
  color: #324669;
  letter-spacing: 0.04em;
  border-bottom: 1px solid #f1f4fa;
}
.custom-modal .p-dialog-content {
  background: #f5f8fb;
  padding: 26px 24px 20px 26px !important;
  color: #24314d;
  border-radius: 0 0 18px 18px;
}

/* 내용 전체 좌측 정렬 및 가독성 강조 */
.modal-detail-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.modal-row {
  display: flex;
  align-items: flex-start;
  gap: 18px;
}
.modal-label {
  width: 82px;
  min-width: 70px;
  color: #5871a2;
  font-weight: 600;
  font-size: 15.1px;
  text-align: left;
  padding-top: 3px;
}
.modal-value {
  flex: 1;
  font-size: 15.2px;
  color: #2b2c37;
  word-break: break-all;
  text-align: left;
}
.query-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.db-badge {
  display: inline-block;
  min-width: 54px;
  padding: 2px 9px;
  margin-right: 8px;
  border-radius: 8px;
  background: #e3eafd;
  color: #4063b3;
  font-size: 13.2px;
  font-weight: 700;
  text-align: center;
}
.query-text {
  font-family: 'JetBrains Mono', 'Consolas', 'Menlo', monospace;
  font-size: 13.2px;
  color: #314059;
  background: #f0f4fc;
  border-radius: 5px;
  padding: 2.2px 8px;
}
</style>
