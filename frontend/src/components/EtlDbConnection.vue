<template>
  <div class="p-6">
    <h2 class="text-xl font-bold mb-4">DB 연결정보 관리</h2>
    <div class="flex justify-between items-center mb-2">
      <Button label="DB 추가" icon="pi pi-plus" @click="openAddDialog" />
    </div>
    <DataTable :value="dbList" dataKey="id" class="shadow-2xl" stripedRows responsiveLayout="scroll">
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
          <div class="field mb-2">
            <label class="block mb-1">DB 타입</label>
            <Dropdown v-model="editData.dbType" :options="dbTypeOptions" optionLabel="label" optionValue="value" required />
          </div>
          <div class="field mb-2">
            <label class="block mb-1">호스트</label>
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
  </div>
</template>

<script setup>
import api from "@/api";
import { ref, onMounted } from 'vue'
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
toast.add({ severity: 'info', summary: '테스트', detail: '이게 보이면 Toast 잘됨', life: 2000 })


const handleTestConn = async () => {
  console.log('연결테스트 버튼 클릭!');
  try {
    // 1. 상세조회(id로 password까지 복호화된 정보 가져오기)
    const { data: detail } = await api.get(`/api/db-connection/${editData.value.id}`);

    // 2. 복호화된 detail로 커넥션 테스트
    const res = await testConnection(detail);
    console.log('연결 성공! 응답:', res);
    toast.add({ severity: 'success', summary: 'DB 연결', detail: '연결 성공!', life: 2000 });
    alert('연결 성공!');
  } catch (e) {
    console.error('연결 실패:', e);
    toast.add({ severity: 'error', summary: 'DB 연결', detail: e?.response?.data || '연결 실패', life: 3500 });
    alert('연결 실패: ' + (e?.response?.data || e));
  }
};


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
