<template>
  <div class="password-container">
    <h2 class="text-2xl font-bold text-gray-700 mb-4 border-b pb-2">패스워드 관리 대장</h2>

    <!-- 검색 + 등록 -->
    <div class="toolbar flex items-center mb-4">
      <InputText v-model="search" placeholder="DB명 또는 계정명 입력 복합 검색(DB명;계정명) 후 Enter" @keyup.enter="searchList" class="w-full max-w-sm mr-2" />
      <button @click="searchList" class="px-3 py-1 rounded bg-blue-100 hover:bg-blue-200 text-sm mr-2">🔍 검색</button>
      <button @click="clearSearch" class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300 text-sm mr-2">❌ 초기화</button>
      <button @click="openModal" class="px-3 py-1 rounded bg-green-100 hover:bg-green-200 text-sm ml-auto">➕ 등록</button>
    </div>

    <!-- 테이블 -->
    <DataTable
      :value="passList"
      paginator
      :rows="perPage"
      :rowsPerPageOptions="[10, 30, 50, 100]"
      class="mb-4 text-sm font-semibold"
      responsiveLayout="scroll"
      @page="onPageChange"
    >
      <Column
        field="gubun"
        header="구분"
        :style="{ width: '100px' }"
        :bodyStyle="{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }"
      />
      <Column field="dbName" header="DB명" />
      <Column field="username" header="계정" />
      <Column field="dbType" header="DB TYPE" />
      <Column header="패스워드">
        <template #body="slotProps">
          <div class="flex items-center gap-2 justify-center">
            <span>{{ visibleSeq === slotProps.data.seq ? slotProps.data.password : '●●●●●●●' }}</span>
            <Button icon="pi pi-eye" text size="small" @click="toggleVisible(slotProps.data)" />
            <Button icon="pi pi-copy" text size="small" @click="copyToClipboard(slotProps.data.password)" />
          </div>
        </template>
      </Column>
    <Column field="explanation" header="설명" :style="{ width: '250px' }">
      <template #body="slotProps">
        <span
          :title="slotProps.data.explanation"
          style="display: inline-block; max-width: 230px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: middle;"
        >
          {{ slotProps.data.explanation }}
        </span>
      </template>
    </Column>
      <Column field="manager" header="관리자" />
      <Column field="ipaddr" header="IP 주소" />
      <Column header="삭제">
        <template #body="slotProps">
          <Button icon="pi pi-pencil" text size="small" @click="editPassword(slotProps.data)" />
          <Button icon="pi pi-trash" text severity="danger" size="small" @click="deletePassword(slotProps.data.seq)" />
        </template>
      </Column>
    </DataTable>

    <!-- 등록/수정 모달 -->
    <Dialog v-model:visible="showModal" :header="editMode ? '패스워드 수정' : '패스워드 등록'" modal class="w-[500px]">
      <form class="flex flex-col gap-3" @submit.prevent="submit">
        <Dropdown v-model="form.gubun" :options="['가동계','개발계','테스트계']" placeholder="-- 구분 선택 --" />
        <Dropdown v-model="form.dbType" :options="['ORACLE','TIBERO','MSSQL','MYSQL','MARIADB','EDB','POSTGRES']" placeholder="-- DB타입 --" />
        <InputText v-model="form.dbName" placeholder="DB명" required />
        <InputText v-model="form.username" placeholder="계정" required />
        <Password v-model="form.password" toggleMask placeholder="패스워드" required :feedback="false" />
        <InputText v-model="form.explanation" placeholder="계정설명" />
        <InputText v-model="form.manager" placeholder="담당자" />
        <InputText v-model="form.ipaddr" placeholder="IP 주소" />

        <div class="flex justify-end gap-2 mt-4">
          <Button label="저장" icon="pi pi-check" type="submit" />
          <Button label="닫기" icon="pi pi-times" severity="secondary" @click="closeModal" />
        </div>
      </form>
    </Dialog>

    <!-- 확인 모달 -->
    <ConfirmDialog />
    <!-- 알림 팝업 -->
  </div>
</template>


<script setup>
import { ref, computed } from 'vue';
import InputText from 'primevue/inputtext';
import Button from 'primevue/button';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Password from 'primevue/password';
import Dropdown from 'primevue/dropdown';
import Dialog from 'primevue/dialog';
import Toast from 'primevue/toast';
import ConfirmDialog from 'primevue/confirmdialog';
import Paginator from 'primevue/paginator';
import { useToast } from 'primevue/usetoast';
import { useConfirm } from 'primevue/useconfirm';
import { useStore } from 'vuex';
import api from '@/api';

const store = useStore();
const toast = useToast();
const confirm = useConfirm();
const user = computed(() => store.state.user);

const search = ref('');
const passList = ref([]);
const visibleSeq = ref(null);
const showModal = ref(false);
const page = ref(1);
const perPage = 10;
const editMode = ref(false);

const form = ref({
  dbName: '', username: '', password: '',
  explanation: '', manager: '', ipaddr: '', gubun: '', dbType: '', createUser: ''
});

const searchList = async () => {
  if (!search.value.trim()) return;

  // 세미콜론 있는지 검사
  const hasSemicolon = search.value.includes(';');

  if (!hasSemicolon) {
    // 세미콜론 없으면: keyword 그대로 API에 전달
    const res = await api.post('/api/pass/search', { keyword: search.value });
    passList.value = res.data;
    return;
  }

  // 세미콜론 있을 경우: 전체 가져와서 프론트에서 필터링
  const res = await api.post('/api/pass/search', { keyword: '' });
  const [dbKeyword, userKeyword] = search.value.trim().toLowerCase().split(';');

  passList.value = res.data.filter(item => {
    const db = item.dbName?.toLowerCase() || '';
    const user = item.username?.toLowerCase() || '';

    if (dbKeyword && userKeyword) {
      return db.includes(dbKeyword) && user.includes(userKeyword);
    }

    const keyword = dbKeyword || userKeyword;
    return db.includes(keyword) || user.includes(keyword);
  });
};


const clearSearch = () => {
  search.value = '';
  passList.value = [];
};

const pagedList = computed(() => {
  const start = (page.value - 1) * perPage;
  return passList.value.slice(start, start + perPage);
});

const totalPages = computed(() => Math.ceil(passList.value.length / perPage));

const onPageChange = (e) => {
  page.value = e.page + 1;
};

const toggleVisible = (item) => {
  visibleSeq.value = visibleSeq.value === item.seq ? null : item.seq;
};

const copyToClipboard = (text) => {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text)
      .then(() => {
        toast.add({
          severity: 'info',
          summary: '복사 완료',
          detail: '비밀번호가 클립보드에 복사되었습니다.',
          life: 2000
        });
      })
      .catch(err => {
        console.error('클립보드 복사 실패:', err);
        toast.add({
          severity: 'error',
          summary: '복사 실패',
          detail: '클립보드 복사 중 오류가 발생했습니다.',
          life: 3000
        });
        fallbackCopy(text);
      });
  } else {
    fallbackCopy(text);
  }
};

const fallbackCopy = (text) => {
  try {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', '');
    textarea.style.position = 'absolute';
    textarea.style.left = '-9999px';
    document.body.appendChild(textarea);
    textarea.select();

    const result = document.execCommand('copy');
    document.body.removeChild(textarea);

    if (result) {
      toast.add({
        severity: 'info',
        summary: '복사 완료',
        detail: '비밀번호가 클립보드에 복사되었습니다.',
        life: 2000
      });
    } else {
      toast.add({
        severity: 'warn',
        summary: '복사 실패',
        detail: '복사 기능이 제한되어 있습니다.',
        life: 3000
      });
    }
  } catch (e) {
    console.error('fallback 복사 에러:', e);
    toast.add({
      severity: 'error',
      summary: '복사 실패',
      detail: '복사 기능을 사용할 수 없습니다.',
      life: 3000
    });
  }
};


const deletePassword = (id) => {
  confirm.require({
    message: '정말 삭제하시겠습니까?',
    header: '삭제 확인',
    icon: 'pi pi-exclamation-triangle',
    accept: async () => {
      await api.post('/api/pass/delete', { id, username: user.value.username });
      toast.add({ severity: 'success', summary: '삭제됨', detail: '패스워드가 삭제되었습니다.', life: 2000 });
      await searchList();
    }
  });
};

const openModal = () => {
  form.value = {
    dbName: '', username: '', password: '', explanation: '', manager: '', ipaddr: '', gubun: '', dbType: '', createUser: user.value.username
  };
  editMode.value = false;
  showModal.value = true;
};

const editPassword = (item) => {
  form.value = { ...item, createUser: user.value.username };
  editMode.value = true;
  showModal.value = true;
};

const submit = async () => {
  if (editMode.value) {
    await api.put(`/api/pass/${form.value.seq}`, form.value);
    toast.add({ severity: 'success', summary: '수정 완료', detail: '패스워드가 수정되었습니다.', life: 2000 });
  } else {
    await api.post('/api/pass', form.value);
    toast.add({ severity: 'success', summary: '등록 완료', detail: '신규 패스워드가 등록되었습니다.', life: 2000 });
  }
  closeModal();
  await searchList();
};

const closeModal = () => {
  showModal.value = false;
  editMode.value = false;
};
</script>

<style scoped>
.password-container {
  max-width: 1550px;
  margin: auto;
  padding: 24px;
  background: #f9f9f9;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}


.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
}

.toolbar input {
  flex: 1;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #ccc;
}

.toolbar button {
  padding: 8px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  background-color: #4CAF50;
  color: white;
  transition: background 0.3s;
}

</style>
