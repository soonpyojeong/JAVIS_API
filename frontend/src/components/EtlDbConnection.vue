<template>
  <div class="db-conn-wrapper">
    <h2 class="text-xl font-bold mb-4">DB 연결정보 관리</h2>
    <div class="flex justify-between items-center mb-2">
      <Button label="DB 추가" icon="pi pi-plus" @click="openAddDialog" />
      <InputText
        v-model="tableSearch"
        placeholder="설명 또는 DB명 검색"
        class="mb-3"
        style="width: 250px"
      />
    </div>
<DataTable
  :value="filteredList"
  dataKey="id"
  stripedRows
  responsiveLayout="scroll"
  paginator
  :rows="10"
  class="shadow-2xl"
>
  <Column field="dbType" header="DB 타입" />
  <Column field="host" header="호스트" />
  <Column field="port" header="포트" />
  <Column field="dbName" header="DB명" />
  <Column field="username" header="계정" />
  <Column field="description" header="설명" />
  <Column header="액션" style="width: 120px">
    <template #body="slotProps">
      <Button icon="pi pi-pencil" size="small" text @click="editDb(slotProps.data)" />
      <Button icon="pi pi-trash" size="small" text severity="danger" class="ml-1" @click="removeDb(slotProps.data)" />
    </template>
  </Column>
</DataTable>

    <!-- 등록/수정 Modal -->
    <Dialog v-model:visible="showDialog" :header="dialogHeader" modal style="width:400px">
      <form @submit.prevent="submitDb">
        <div class="p-fluid">
          <Button label="DBLIST 불러오기" icon="pi pi-database" class="ml-2" @click="openDbListLookup" />
          <div class="field mb-2">
            <label class="block mb-1">DB 타입</label>
            <Dropdown v-model="editData.dbType" :options="dbTypeOptions" optionLabel="label" optionValue="value" required />
          </div>
          <div class="field mb-2">
            <label class="block mb-1">IP ADRR</label>
            <InputText v-model="editData.host" required />
          </div>
          <div class="field mb-2">
            <label class="block mb-1">포트</label>
            <InputText v-model="editData.port" required />
          </div>
          <div class="field mb-2">
            <label class="block mb-1">DB명</label>
            <InputText v-model="editData.dbName" required />
          </div>
          <div class="field mb-2">
            <label class="block mb-1">계정</label>
            <InputText v-model="editData.username" required />
          </div>
          <div class="field mb-2">
            <label class="block mb-1">비밀번호</label>
            <InputText v-model="editData.password" type="password" autocomplete="off" required />
          </div>
          <div class="field mb-2">
            <label class="block mb-1">설명</label>
            <InputText v-model="editData.description" />
          </div>
        </div>
        <div class="flex justify-end mt-4">
          <Button type="button" label="취소" class="mr-2" text @click="closeDialog" />
          <Button type="submit" label="저장" />
          <Button label="연결테스트" icon="pi pi-link" severity="info" class="mb-2" @click="handleTestConn" />

        </div>
      </form>
    </Dialog>

    <Dialog v-model:visible="showDbListDialog" header="DBLIST에서 불러오기" modal style="width:700px">
      <div class="mb-3">
        <InputText v-model="dbListSearch" placeholder="설명, DB명 등으로 검색" style="width: 100%;" />
      </div>
      <DataTable :value="filteredDbList" dataKey="id" stripedRows sortField="dbDescript" :sortOrder="1">
        <Column field="dbType" header="DB 타입" />
        <Column field="dbDescript" header="설명" />
        <Column field="dbName" header="DB명" />
        <Column header="선택">
          <template #body="slotProps">
            <Button label="선택" size="small" @click="applyDbList(slotProps.data)" />
          </template>
        </Column>
      </DataTable>
    </Dialog>


  </div>
</template>

<script setup>
import api from "@/api";
import { ref, onMounted ,computed } from 'vue'
import { useDbConnection } from '@/composables/useDbConnection'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Dropdown from 'primevue/dropdown'
import InputText from 'primevue/inputtext'
import { useToast } from 'primevue/usetoast'
const { testConnection } = useDbConnection()
const toast = useToast()


const handleTestConn = async () => {
  if (!editData.value.id) {
    toast.add({ severity: 'warn', summary: 'DB 연결', detail: '먼저 DB를 저장한 후 테스트 가능합니다.', life: 3000 });
    return;
  }

  try {
    const { data: detail } = await api.get(`/api/db-connection/${editData.value.id}`);
    const res = await testConnection(detail);
    toast.add({ severity: 'success', summary: 'DB 연결', detail: '연결 성공!', life: 2000 });
  } catch (e) {
    console.error('연결 실패:', e);
    toast.add({ severity: 'error', summary: 'DB 연결', detail: e?.response?.data || '연결 실패', life: 3500 });
  }
};


const showDbListDialog = ref(false)
const isNewMode = computed(() => !editData.value.id)
const dbListAll = ref([])
const sortField = ref('dbDescript')
const sortOrder = ref(1) // 1 = ASC, -1 = DESC
const openDbListLookup = async () => {
  // 백엔드에서 /api/db-list/all 호출
  const { data } = await api.get('/api/db-list/all')
  dbListAll.value = data
  showDbListDialog.value = true
}

const applyDbList = (selected) => {
  editData.value = {
    dbType: selected.dbType,
     host: selected.pubIp,
    port: selected.port,
    dbName: selected.dbName,
    username: selected.userid,
    password: selected.pw,
    description: selected.dbDescript,
  }
  showDbListDialog.value = false
  showDialog.value = true
  dialogHeader.value = 'DB 연결정보 등록' // 등록 or 수정 분기도 가능
}

const dbListSearch = ref('')
const tableSearch = ref("")

const filteredList = computed(() => {
  if (!tableSearch.value) return dbList.value
  const keyword = tableSearch.value.toLowerCase()
  return dbList.value.filter(item =>
    (item.description || "").toLowerCase().includes(keyword) ||
    (item.dbName || "").toLowerCase().includes(keyword)
  )
})


const filteredDbList = computed(() => {
  if (!dbListSearch.value) return dbListAll.value
  const keyword = dbListSearch.value.toLowerCase()
  return dbListAll.value.filter(item =>
    (item.dbType || '').toLowerCase().includes(keyword) ||
    (item.dbDescript || '').toLowerCase().includes(keyword) ||
    (item.dbName || '').toLowerCase().includes(keyword)
  )
})

const dbTypeOptions = [
  { label: 'Oracle', value: 'ORACLE' },
  { label: 'Tibero', value: 'TIBERO' },
  { label: 'MySQL', value: 'MYSQL' },
  { label: 'MariaDB', value: 'MARIADB' },
  { label: 'PostgreSQL', value: 'POSTGRESQL' },
  { label: 'EDB', value: 'EDB' },
  { label: 'MSSQL', value: 'MSSQL' },
  { label: 'Sybase', value: 'SYBASE' },
  { label: 'SAP HANA', value: 'HANA' }
]

const { getDbList, addDb, updateDb, deleteDb } = useDbConnection()
const dbList = ref([])
const showDialog = ref(false)
const dialogHeader = ref('DB 연결정보 등록')
const editData = ref({})

// 최초 DB 목록 불러오기
const loadDbList = async () => {
  const { data } = await getDbList()
  dbList.value = data
}
onMounted(loadDbList)

// 등록 다이얼로그 열기
const openAddDialog = () => {
  dialogHeader.value = 'DB 연결정보 등록'
  editData.value = { dbType: '', host: '', port: '', dbName: '', username: '', password: '', description: '' }
  showDialog.value = true
}

// 수정 다이얼로그 열기
const editDb = (row) => {
  dialogHeader.value = 'DB 연결정보 수정'
  editData.value = { ...row }
  showDialog.value = true
}

// 등록/수정 처리
const submitDb = async () => {
  if (!editData.value.id) {
    await addDb(editData.value)
  } else {
    await updateDb(editData.value.id, editData.value)
  }
  showDialog.value = false
  await loadDbList()
}

// 삭제
const removeDb = async (row) => {
  if (confirm('정말 삭제하시겠습니까?')) {
    await deleteDb(row.id)
    await loadDbList()
  }
}

// 다이얼로그 닫기
const closeDialog = () => {
  showDialog.value = false
  editData.value = {}
}
</script>
<style scoped>
.db-conn-wrapper {
  max-width: 1200px;
  margin: 32px auto 0 auto;
  background: var(--surface-card);
  border-radius: 14px;
  box-shadow: 0 4px 24px 0 rgba(80, 80, 100, 0.06);
  padding: 32px 24px 32px 24px;
}
.data-table-sm .p-datatable-tbody > tr > td,
.data-table-sm .p-datatable-thead > tr > th {
  padding: 0.5rem 0.7rem;
  font-size: 0.97rem;
}
.form-sm .field {
  margin-bottom: 0.8rem;
}
.form-sm label {
  font-size: 0.96rem;
}
.form-sm input, .form-sm .p-dropdown {
  font-size: 1rem;
  padding: 0.35rem 0.7rem;
}
@media (max-width: 900px) {
  .db-conn-wrapper {
    max-width: 97vw;
    padding: 10px;
  }
  .data-table-sm {
    font-size: 0.93rem;
  }
}
</style>