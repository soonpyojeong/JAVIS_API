//PrimevueTest.vue

<template>
  <div class="db-list-container">
    <h1 class="db-title">DB 리스트</h1>
    <div class="top-container flex justify-between items-center mb-4">
      <InputText
        v-model="searchQuery"
        placeholder="검색: 설명, IP, DB 이름"
        class="w-64"
        clearable
        style="height: 40px;"
      />
      <div class="toolbar">
        <Button
          :label="allChkStatus === 'N' ? '전체관제중지중' : '전체관제 해제'"
          :severity="allChkStatus === 'N' ? 'danger' : 'primary'"
          size="small"
          @click="showAllChkModal"
        />
        <Button
          label="DB 등록"
          icon="pi pi-plus"
          iconPos="left"
          severity="primary"
          size="small"
          @click="openAddModal"
        />
      </div>
    </div>

    <div class="datatable-section">
      <DataTable
        :value="filteredDbList"
        @row-dblclick="openEditModal"
        tableClass="text-center"
        headerClass="text-center"
        stripedRows
        showGridlines
        paginator
        :rows="100"
        :rowsPerPageOptions="[10,15,30,50,100]"
        scrollable
        scrollHeight="70vh"
        class="shadow-3"
        v-if="filteredDbList.length > 0"
        responsiveLayout="scroll"
        sortMode="single"
        :sortField="sortKey"
        :sortOrder="sortAsc ? 1 : -1"
        @sort="onSort"
        :globalFilterFields="['dbDescript','pubIp','vip','dbName']"
        :globalFilter="searchQuery"
        dataKey="id"
      >
        <Column field="loc" header="지역" sortable bodyClass="text-center" headerClass="text-center" />
        <Column field="assets" header="자산" sortable bodyClass="text-center" headerClass="text-center" />
        <Column field="dbDescript" header="설명" sortable bodyClass="text-center" headerClass="text-center" />
        <Column field="hostname" header="호스트" sortable bodyClass="text-center" headerClass="text-center" />
        <Column field="pubIp" header="PubIP" sortable bodyClass="text-center" headerClass="text-center" />
        <Column field="dbName" header="DB 이름" sortable bodyClass="text-center" headerClass="text-center" />
        <Column field="dbType" header="DB 타입" sortable bodyClass="text-center" headerClass="text-center" />
      <Column header="LIVE">
        <template #body="{ data }">
          <Button
            :label="data.liveChk === 'Y' ? 'On' : 'Off'"
            :severity="data.liveChk === 'Y' ? 'success' : 'danger'"
            :disabled="!isButtonEnabled(data, 'liveChk')"
            @click="showModal(data, 'liveChk')"
            size="small"
            class="w-full font-bold"
          />
        </template>
      </Column>

      <Column header="TBS">
        <template #body="{ data }">
          <Button
            :label="data.sizeChk === 'Y' ? 'On' : 'Off'"
            :severity="data.sizeChk === 'Y' ? 'success' : 'danger'"
            :disabled="!isButtonEnabled(data, 'sizeChk')"
            @click="showModal(data, 'sizeChk')"
            size="small"
            class="w-full font-bold"
          />
        </template>
      </Column>

      <Column header="TRN">
        <template #body="{ data }">
          <Button
            :label="data.trnBakChk === 'Y' ? 'On' : 'Off'"
            :severity="data.trnBakChk === 'Y' ? 'success' : 'danger'"
            :disabled="!isButtonEnabled(data, 'trnBakChk')"
            @click="showModal(data, 'trnBakChk')"
            size="small"
            class="w-full font-bold"
          />
        </template>
      </Column>

      <Column header="OBJ">
        <template #body="{ data }">
          <Button
            :label="data.objSegSizeChk === 'Y' ? 'On' : 'Off'"
            :severity="data.objSegSizeChk === 'Y' ? 'success' : 'danger'"
            :disabled="!isButtonEnabled(data, 'objSegSizeChk')"
            @click="showModal(data, 'objSegSizeChk')"
            size="small"
            class="w-full font-bold"
          />
        </template>
      </Column>

      <Column header="일일점검">
        <template #body="{ data }">
          <Button
            :label="data.dailyChk === 'Y' ? 'On' : 'Off'"
            :severity="data.dailyChk === 'Y' ? 'success' : 'danger'"
            :disabled="!isButtonEnabled(data, 'dailyChk')"
            @click="showModal(data, 'dailyChk')"
            size="small"
            class="w-full font-bold"
          />
        </template>
      </Column>
      <Column header="자산중지">
        <template #body="{ data }">
          <Button
            :label="getAssetButtonLabel(data)"
            :severity="getAssetButtonClass(data) === 'btn-gray' ? 'secondary' : 'danger'"
            @click="showPauseModal(data)"
            size="small"
            class="w-full font-bold"
          />
        </template>
      </Column>
    </DataTable>
    <div v-else class="text-center text-gray-500 p-6">데이터가 없습니다.</div>
        <DbEditForm
             v-model:visible="editDialogVisible"
            :mode="editMode"
            :form="editForm"
            :locOptions="LocOptions"
            :dbTypeOptions="dbTypeOptions"
            :assetsOptions="assetsOptions"
            :dbCheckItems="computedDbCheckItems"
            @submit="handleDbEditSubmit"
            @cancel="closeEditDialog"
            @dbTypeChange="onChildDbTypeChange"
        />
        <!-- 팝업 모달 Dialog (PrimeVue) -->
        <Dialog v-model:visible="isModalVisible" modal header="상태 변경" :closable="false">
          <div class="text-lg mb-4">정말 수정하시겠습니까?</div>
          <div class="flex gap-2 justify-center">
            <Button label="확인" severity="danger" @click="confirmUpdate" />
            <Button label="취소" severity="secondary" @click="cancelUpdate" outlined />
          </div>
        </Dialog>
        <Dialog v-model:visible="isPauseModalVisible" modal header="자산중지" :closable="false">
          <div class="text-lg mb-4">정말 자산을 {{ getAssetButtonLabel(targetPauseDb) }} 하시겠습니까?</div>
          <div class="flex gap-2 justify-center">
            <Button label="확인" severity="danger" @click="confirmPauseAssets" />
            <Button label="취소" severity="secondary" @click="cancelPauseAssets" outlined />
          </div>
        </Dialog>
        <Dialog v-model:visible="isAllChkModalVisible" modal header="전체관제 상태 변경" :closable="false">
          <div class="text-lg mb-4">정말 변경 하시겠습니까?</div>
          <div class="flex gap-2 justify-center">
            <Button label="확인" severity="danger" @click="confirmAllChkUpdate" />
            <Button label="취소" severity="secondary" @click="cancelAllChkUpdate" outlined />
          </div>
        </Dialog>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount,watch } from 'vue'
import { connectWebSocket, disconnectWebSocket } from "@/websocket"
import api from "@/api"
import DbEditForm from '@/components/DbEditForm.vue'
import DataTable from 'primevue/datatable'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Column from 'primevue/column'

// 1. 상태 선언 (Composition API 방식)
const dbList = ref([])
const searchQuery = ref("")
const isModalVisible = ref(false)
const currentDb = ref(null)
const currentField = ref(null)
const allChkStatus = ref(null)
const isAllChkModalVisible = ref(false)
const sortKey = ref("dbDescript")
const sortAsc = ref(true)
const isPauseModalVisible = ref(false)
const targetPauseDb = ref(null)
const onSort = (event) => {
  sortKey.value = event.sortField; sortAsc.value = event.sortOrder === 1;
}
const editForm = ref({})
const editDialogVisible = ref(false)
const editMode = ref('add')

const defaultForm = {
  loc: '', dbType: '', assets: '', dbVer: '', smsGroup: 'DKS_DBA',
  dbDescript: '', os: '', hostname: '', dbName: '', instanceName: '',
  pubIp: '', vip: '', port: '', userid: 'zenius', pw: 'z_nius_19',
  liveChk: 'Y', sizeChk: 'Y', objSegSizeChk: 'Y', dailyChk: 'Y', backupChk: 'N'
}



const dbTypeOptions = [
  { label: 'TIBERO', value: 'TIBERO' },
  { label: 'EDB', value: 'EDB' },
  { label: 'MSSQL', value: 'MSSQL' },
  { label: 'MARIADB', value: 'MARIADB' },
  { label: 'MYSQL', value: 'MYSQL' },
  { label: 'ORACLE', value: 'ORACLE' },
  { label: 'POSTGRES', value: 'POSTGRES' },
]

const LocOptions = [
  { label: 'IDC', value: 'IDC' },
  { label: 'NCP', value: 'NCP' },
  { label: '당진', value: '당진' },
  { label: '부산', value: '부산' },
  { label: '인천', value: '인천' },
  { label: '포항', value: '포항' },
]
const assetsOptions = [
  { label: '공통', value: '공통' },
  { label: '동국CM', value: '동국CM' },
  { label: '동국제강', value: '동국제강' },
  { label: '동국홀딩스', value: '동국홀딩스' },
  { label: '동국시스템즈', value: '동국시스템즈' },
  { label: 'ORACLE', value: 'ORACLE' }
]

// DB_TYPE별 체크박스 항목
const dbCheckItemsMap = {
  ORACLE: [
    { key: 'liveChk', label: 'LIVE' },
    { key: 'sizeChk', label: 'TBS' },
    { key: 'objSegSizeChk', label: 'OBJ' },
    { key: 'dailyChk', label: '일일점검' },
    { key: 'backupChk', label: '백업' }
  ],
  TIBERO: [
    { key: 'liveChk', label: 'LIVE' },
    { key: 'sizeChk', label: 'TBS' },
    { key: 'objSegSizeChk', label: 'OBJ' },
    { key: 'dailyChk', label: '일일점검' },
    { key: 'backupChk', label: '백업' }
  ],
  SYBASE: [
    { key: 'liveChk', label: 'LIVE' }
  ],
  EDB: [
    { key: 'liveChk', label: 'LIVE' }
  ],
  POSTGRES: [
    { key: 'liveChk', label: 'LIVE' }
  ],
  MYSQL: [
    { key: 'liveChk', label: 'LIVE' }
  ],
  MARIADB: [
    { key: 'liveChk', label: 'LIVE' }
  ]
}

// =========== 등록 =============
function openAddModal() {
  editForm.value = { ...defaultForm }
  editMode.value = 'add'
  editDialogVisible.value = true
}
// =========== 수정 =============
function openEditModal({ data }) {
  editForm.value = { ...defaultForm, ...data }
  // null → ""로 변환
  Object.keys(editForm.value).forEach(k => {
    if (editForm.value[k] === null) editForm.value[k] = '';
  })
  editMode.value = 'edit'
  editDialogVisible.value = true
}

// =========== 폼에서 체크박스 항목 제공 =============
const computedDbCheckItems = computed(() => {
  console.log(computedDbCheckItems.value)
  return dbCheckItemsMap[editForm.value.dbType] || []
})

function onChildDbTypeChange(newType) {
  editForm.value.dbType = newType // 부모 editForm의 dbType도 동기화
}

// =========== 저장 로직 ===========
async function handleDbEditSubmit(form) {
  if (editMode.value === 'add') {
    const { data } = await api.post('/api/db-list/save', form);
    dbList.value.unshift(data);
  } else {
    const { data } = await api.put(`/api/db-list/update/${form.id}`, form);
    const idx = dbList.value.findIndex(d => d.id === form.id);
    if (idx !== -1) dbList.value.splice(idx, 1, data);
  }
  editDialogVisible.value = false;
}


function closeEditDialog() { editDialogVisible.value = false }
// =========== 목록 로딩 ===========
async function loadDbList() {
  const { data } = await api.get("/api/db-list/all")
  dbList.value = data
}
onMounted(loadDbList)



const buttonRules = {
  liveChk: ['EDB', 'TIBERO', 'MSSQL', 'SYBASE', 'MARIADB', 'MYSQL', 'ORACLE'],
  sizeChk: ['ORACLE', 'TIBERO'],
  trnBakChk: ['SYBASE'],
  objSegSizeChk: ['ORACLE', 'TIBERO'],
  dailyChk: ['ORACLE', 'TIBERO'],
}

// 2. computed 예시
const filteredDbList = computed(() => {
  const query = searchQuery.value?.trim().toLowerCase();
  let arr = dbList.value;
  if (query) {
    arr = arr.filter(db =>
      ['dbDescript', 'hostname', 'pubIp', 'vip', 'dbType', 'dbName']
        .some(key => (db[key] || '').toLowerCase().includes(query))
    );
  }
  if (sortKey.value) {
    arr = arr.slice().sort((a, b) => {
      const valA = (a[sortKey.value] ?? '').toString().toLowerCase();
      const valB = (b[sortKey.value] ?? '').toString().toLowerCase();
      return valA.localeCompare(valB) * (sortAsc.value ? 1 : -1);
    });
  }
  return arr;
});


// 3. methods를 함수로
function showPauseModal(db) {
  targetPauseDb.value = db;
  isPauseModalVisible.value = true;
}

function confirmPauseAssets() {
  const username = window.$store?.state?.user?.username || ''; // 실제 전역스토어에서 꺼내기
  const db = targetPauseDb.value;
  if (!db) return;

  const targetFields = ['liveChk', 'sizeChk', 'trnBakChk', 'objSegSizeChk', 'dailyChk'];
  const managedFields = targetFields.filter(field => {
    return buttonRules[field]?.includes(db.dbType);
  });

  const hasAnyY = managedFields.some(field => db[field] === "Y");
  const newStatus = hasAnyY ? "N" : "Y";

  managedFields.forEach(field => {
    if (field in db) {
      db[field] = newStatus;
    }
  });

  api.put(`/api/db-list/update/${db.id}`, {
    ...db,
    username,
  })
  .then(() => {
    isPauseModalVisible.value = false;
    targetPauseDb.value = null;
  })
  .catch((error) => {
    console.error("자산중지 업데이트 실패", error);
    isPauseModalVisible.value = false;
    targetPauseDb.value = null;
  });
}

function cancelPauseAssets() {
  isPauseModalVisible.value = false;
  targetPauseDb.value = null;
}

function getAssetButtonLabel(db) {
  if (!db) return '';
  const targetFields = ['liveChk', 'sizeChk', 'trnBakChk', 'objSegSizeChk', 'dailyChk'];
  const managedFields = targetFields.filter(field => buttonRules[field]?.includes(db.dbType));
  const hasAnyY = managedFields.some(field => db[field] === "Y");
  return hasAnyY ? "중지" : "활성";
}

function getAssetButtonClass(db) {
  const targetFields = ['liveChk', 'sizeChk', 'trnBakChk', 'objSegSizeChk', 'dailyChk'];
  const managedFields = targetFields.filter(field => buttonRules[field]?.includes(db.dbType));
  const hasAnyY = managedFields.some(field => db[field] === "Y");
  return hasAnyY ? 'btn-gray' : 'btn-off';
}

function isButtonEnabled(db, field) {
  const allowedDbTypes = buttonRules[field];
  if (!allowedDbTypes) return false;
  return allowedDbTypes.includes(db.dbType);
}

function showModal(db, field) {
  currentDb.value = db;
  currentField.value = field;
  isModalVisible.value = true;
}

function confirmUpdate() {
  if (currentDb.value && currentField.value) {
    const newStatus = currentDb.value[currentField.value] === "Y" ? "N" : "Y";
    currentDb.value[currentField.value] = newStatus;

    const username = window.$store?.state?.user?.username || '';
    api.put(`/api/db-list/update/${currentDb.value.id}`, {
      ...currentDb.value,
      username,
    })
      .then(() => {
        isModalVisible.value = false;
      })
      .catch((error) => {
        console.error(`${currentField.value} 업데이트 실패`, error);
        currentDb.value[currentField.value] = newStatus === "Y" ? "N" : "Y";
        isModalVisible.value = false;
      });
  }
}

function cancelUpdate() {
  isModalVisible.value = false;
}

function showAllChkModal() {
  isAllChkModalVisible.value = true;
}

function confirmAllChkUpdate() {
  const newStatus = allChkStatus.value === "N" ? null : "N";
  api.put("/api/db-list/update-allchk", { status: newStatus })
    .then(() => {
      allChkStatus.value = newStatus;
      isAllChkModalVisible.value = false;
    })
    .catch((error) => {
      console.error("전체관제 상태 변경 실패", error);
      isAllChkModalVisible.value = false;
    });
}

function cancelAllChkUpdate() {
  isAllChkModalVisible.value = false;
}

function handleWebSocketMessage(updatedDb) {
  const index = dbList.value.findIndex((db) => db.id === updatedDb.id);
  if (index !== -1) {
    dbList.value.splice(index, 1, updatedDb);
  }
}

// 4. 라이프사이클
onMounted(() => {
  api.get("/api/db-list/all")
    .then((response) => {
      dbList.value = response.data;
    })
    .catch((error) => {
      console.error("API 호출 오류:", error);
    });

  api.get("/api/db-list/allchk")
    .then((response) => {
      allChkStatus.value = response.data;
    })
    .catch((error) => {
      console.error("전체관제 상태 조회 실패", error);
    });

  connectWebSocket({
    onDbStatusMessage: handleWebSocketMessage,
  });
})

onBeforeUnmount(() => {
  disconnectWebSocket();
})

// =========== 체크박스 자동 초기화 ===========
watch(() => editForm.value.dbType, (newType) => {
  const keys = Object.keys(defaultForm).filter(k => /Chk$/.test(k))
  const validKeys = (dbCheckItemsMap[newType] || []).map(i => i.key)
  keys.forEach(key => {
    if (validKeys.includes(key)) {
      if (editForm.value[key] !== 'Y' && editForm.value[key] !== 'N') editForm.value[key] = 'Y'
    } else {
      editForm.value[key] = 'N'
    }
  })
})

</script>


<style scoped>
.db-list-container {
font-family: 'Arial', sans-serif;
padding: 20px;
max-width: 1600px;
margin: 0 auto;
background: #ffffff;
border-radius: 10px;
box-shadow: 0 10px 20px rgba(0, 0, 0, 0.1);
}
.db-title {
  margin-top: 1px;   /* 위 간격 */
  margin-bottom: 1px;  /* 아래 간격 */
  text-align: center;
  font-size: 2.2rem;
  font-weight: bold;
}
.top-container {
display: flex;
justify-content: space-between;
align-items: center;
}
/* DataTable 별도 마진 */
.datatable-section {
margin-top: 20px;
}
/* DataTable 헤더와 바디 */
.p-datatable-thead th,
.p-datatable-thead > tr > th {
text-align: center !important;
vertical-align: middle !important;
}
.p-datatable-tbody td,
.p-datatable-tbody > tr > td {
text-align: center !important;
vertical-align: middle !important;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
}

.toolbar button {
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  background-color: #1976d2;
  color: white;
  transition: background 0.3s;
}

.toolbar button:hover {
  background-color: #0d47a1;
}



</style>
