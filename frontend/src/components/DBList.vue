<template>
  <div class="db-list-container">
    <!-- 1. 타이틀 (가운데) -->
    <h1 class="db-title">DB 리스트</h1>
    <!-- 2. 상단 컨트롤 (검색/버튼) -->
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
          @click="showAddDialog = true"
        />
      </div>
    </div>

      <!-- 3. DataTable 별도 분리! -->
      <div class="datatable-section">
        <DataTable
          :value="filteredDbList"
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
        <!-- DB 등록  Dialog (PrimeVue) -->
        <Dialog v-model:visible="showAddDialog" modal header="DB 등록" :closable="false" style="width: 380px; max-width: 95vw;">
          <form @submit.prevent="handleSubmit">
            <div class="flex flex-col gap-3">
              <Select
                v-model="addForm.loc"
                :invalid="!addForm.loc"
                :options="LocOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="지역 선택"
                class="w-full"
                required
                appendTo="body"
              />
              <Select
                v-model="addForm.dbType"
                :invalid="!addForm.dbType"
                :options="dbTypeOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="DB 타입 선택"
                class="w-full"
                required
                appendTo="body"
              />
              <Select
                v-model="addForm.assets"
                :invalid="!addForm.assets"
                :options="assetsOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="자산 선택"
                class="w-full"
                required
                appendTo="body"
              />

              <InputText v-model="addForm.dbVer" :invalid="!addForm.dbVer" placeholder="DB버전" class="w-full" />
              <InputText v-model="addForm.smsGroup" placeholder="SMS Group" class="w-full" />
              <InputText v-model="addForm.dbDescript" :invalid="!addForm.dbDescript" placeholder="설명" class="w-full" />
              <InputText v-model="addForm.os" placeholder="OS" class="w-full" />
              <InputText v-model="addForm.hostname" placeholder="호스트명" class="w-full" required />
              <InputText v-model="addForm.dbName" :invalid="!addForm.dbName" placeholder="DB 이름" class="w-full" required />
              <InputText v-model="addForm.instanceName" :invalid="!addForm.instanceName" placeholder="Instance Name" class="w-full" required />
              <InputText v-model="addForm.pubIp" :invalid="!addForm.pubIp" placeholder="PubIP" class="w-full" />
              <InputText v-model="addForm.vip" placeholder="VIP" class="w-full" />
              <InputNumber v-model="addForm.port" :invalid="!addForm.port" placeholder="포트" class="w-full" :useGrouping="false" />
              <InputText v-model="addForm.userid" :invalid="!addForm.userid" placeholder="USERID" class="w-full" />
              <Password v-model="addForm.pw" :invalid="!addForm.pw" placeholder="PW" class="w-full" :feedback="false" toggleMask />

            </div>
             <!-- 체크박스 영역 -->
                  <div v-if="dbCheckItems.length" class="flex flex-col gap-2 mt-3">
                    <span class="font-semibold">모니터링 항목</span>
                    <div v-for="item in dbCheckItems" :key="item.key" class="flex items-center gap-2">
                      <Checkbox
                        v-model="addForm[item.key]"
                        binary
                        trueValue="Y"
                        falseValue="N"
                        :inputId="item.key"
                      />
                      <label :for="item.key">{{ item.label }}</label>
                    </div>
                  </div><br>
            <div class="flex justify-end gap-2">
              <Button label="등록" type="submit" severity="primary" class="font-bold" />
              <Button label="취소" type="button" severity="secondary" outlined @click="showAddDialog = false" />
            </div>
          </form>
        </Dialog>
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
import api from "@/api"
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import InputNumber from 'primevue/inputnumber'
import Password from 'primevue/password'
import { connectWebSocket, disconnectWebSocket } from "@/websocket"

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
const onSort = (event) => { sortKey.value = event.sortField; sortAsc.value = event.sortOrder === 1; }






const showAddDialog = ref(false)
const addForm = ref({
    loc: '',
    dbType: '',
    assets: '',
    dbVer:'',
    smsGroup:'DKS_DBA',
    os: '',
    hostname: '',
    dbName: '',
    instanceName: '',
    pubIp: '',
    vip: '',
    port: '',
    userid: 'zenius',
    pw: 'z_nius_19',
    dbDescript: '',
    liveChk: 'Y',
    sizeChk: 'Y',
    objSegSizeChk: 'Y',
    dailyChk: 'Y',
    backupChk: 'N',
})
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

const dbCheckItems = computed(() => {
  return dbCheckItemsMap[addForm.value.dbType] || []
})

const handleSubmit = async () => {
  // 실제 API 엔드포인트에 맞게 조정
  await api.post('/api/db-list/save', addForm.value)
  showAddDialog.value = false
  // 등록 후 리스트 새로고침 등 추가 동작
}


const buttonRules = {
  liveChk: ['EDB', 'TIBERO', 'MSSQL', 'SYBASE', 'MARIADB', 'MYSQL', 'ORACLE'],
  sizeChk: ['ORACLE', 'TIBERO'],
  trnBakChk: ['SYBASE'],
  objSegSizeChk: ['ORACLE', 'TIBERO'],
  dailyChk: ['ORACLE', 'TIBERO'],
}

// 2. computed 예시
const filteredDbList = computed(() => {
  const query = searchQuery.value.toLowerCase();
  let result = dbList.value.filter((db) => {
    return (
      (db.dbDescript && db.dbDescript.toLowerCase().includes(query)) ||
      (db.hostname && db.hostname.toLowerCase().includes(query)) ||
      (db.pubIp && db.pubIp.toLowerCase().includes(query)) ||
      (db.vip && db.vip.toLowerCase().includes(query)) ||
      (db.dbType && db.dbType.toLowerCase().includes(query)) ||
      (db.dbName && db.dbName.toLowerCase().includes(query))
    );
  });

  if (sortKey.value) {
    result = [...result].sort((a, b) => {
      const valA = a[sortKey.value] ? a[sortKey.value].toString().toLowerCase() : "";
      const valB = b[sortKey.value] ? b[sortKey.value].toString().toLowerCase() : "";
      if (valA < valB) return sortAsc.value ? -1 : 1;
      if (valA > valB) return sortAsc.value ? 1 : -1;
      return 0;
    });
  }
  return result;
})

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
  return hasAnyY ? "자산중지" : "관제활성";
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

// DB_TYPE이 바뀔 때마다 해당 체크항목만 N으로 초기화
watch(() => addForm.value.dbType, (newType) => {
  (dbCheckItemsMap[newType] || []).forEach(item => {
    addForm.value[item.key] = 'Y'
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
