<template>
  <div class="password-container">
    <h2>패스워드 관리 대장</h2>

    <!-- 검색 + 등록 -->
    <div class="toolbar">
      <input v-model="search" placeholder="DB명 또는 계정명 입력 후 Enter" @keyup.enter="searchList" />
      <button @click="searchList">🔍 검색</button>
      <button @click="clearSearch">❌ 초기화</button>
      <button @click="openModal">+ 등록</button>
    </div>

    <!-- 검색 결과가 있을 때만 테이블 렌더링 -->
    <table v-if="pagedList.length > 0" class="pass-table">
      <thead>
        <tr>
          <th>DB명</th><th>계정</th><th>패스워드</th><th>설명</th><th>관리자</th><th>삭제</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in pagedList" :key="item.seq">
          <td>{{ item.dbName }}</td>
          <td>{{ item.username }}</td>
          <td>
            <div class="password-wrapper">
              {{ visibleSeq === item.seq ? item.password : '●●●●●●●' }}
              <span @click="toggleVisible(item)">😎</span>
              <span @click="copyToClipboard(item.password)">📄</span>
            </div>
          </td>
          <td>{{ item.explanation }}</td>
          <td>{{ item.manager }}</td>
          <td>
            <button @click="editPassword(item)">✏️</button>
            <button @click="deletePassword(item.seq)">🗑️</button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 페이징 -->
    <div class="pagination" v-if="totalPages > 1">
      <button :disabled="page === 1" @click="page = 1">«</button>

      <button
        v-for="p in visiblePageNumbers"
        :key="p"
        :class="{ active: p === page }"
        @click="page = p"
      >
        {{ p }}
      </button>

      <button :disabled="page === totalPages" @click="page = totalPages">»</button>
    </div>
    <!-- 확인 모달 -->
    <div v-if="showConfirm" class="confirm-modal">
      <p class="confirm-text">{{ confirmMessage }}</p>
      <div class="confirm-buttons">
        <button class="confirm-yes" @click="confirmYes">예</button>
        <button class="confirm-no" @click="confirmNo">아니오</button>
      </div>
    </div>
    <!-- 알림 팝업 모달 -->
    <div v-if="showAlert" class="alert-modal">
      <span class="alert-text">{{ alertMessage }}</span>
      <button class="alert-close" @click="showAlert = false">닫기</button>
    </div>
    <!-- 모달 -->
    <div v-if="showModal" class="modal-backdrop">
      <div class="modal-content">
        <h3>패스워드 등록</h3>
        <form @submit.prevent="submit">
          <select v-model="form.gubun" required>
            <option disabled value="">-- 구분 선택 --</option>
            <option value="가동계">가동계</option>
            <option value="개발계">개발계</option>
            <option value="테스트계">테스트계</option>
          </select>
          <select v-model="form.dbType" required>
              <option disabled value="">-- DB타입--</option>
              <option value="ORACLE">ORACLE</option>
              <option value="TIBERO">TIBERO</option>
              <option value="MSSQL">MSSQL</option>
              <option value="MYSQL">MYSQL</option>
              <option value="MARIADB">MARIADB</option>
              <option value="EDB">EDB</option>
              <option value="POSTGRES">POSTGRES</option>
          </select>
          <input v-model="form.dbName" placeholder="DB명" required />
          <input v-model="form.username" placeholder="계정" required />
          <input v-model="form.password" placeholder="패스워드" required />
          <input v-model="form.explanation" placeholder="계정설명" />
          <input v-model="form.manager" placeholder="담당자" />
          <input v-model="form.createUser" type="hidden" />
          <input v-model="form.ipaddr" placeholder="IP 주소" />

          <div class="buttons">
            <button type="submit">저장</button>
            <button type="button" @click="closeModal">닫기</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import api from "@/api";
import { useStore } from 'vuex';

const store = useStore();
const user = computed(() => store.state.user);

const passList = ref([]);
const visibleSeq = ref(null);
const showModal = ref(false);
const search = ref('');
const page = ref(1);
const perPage = 10;
const maxPageButtons = 5;
const alertMessage = ref('');
const showAlert = ref(false);

const confirmMessage = ref('');
const showConfirm = ref(false);
let confirmCallback = null;

const showConfirmModal = (msg, callback) => {
  confirmMessage.value = msg;
  showConfirm.value = true;
  confirmCallback = callback;
};

const confirmYes = () => {
  showConfirm.value = false;
  if (typeof confirmCallback === 'function') {
    confirmCallback();
  }
};

const confirmNo = () => {
  showConfirm.value = false;
};

const form = ref({
  dbName: '', username: '', password: '',
  explanation: '', manager: '', ipaddr: ''
});

const searchList = async () => {
  if (!search.value.trim()) {
    alert("검색어를 입력하세요");
    return;
  }
  const res = await api.post('/api/pass/search', { keyword: search.value });
  passList.value = res.data;
  if (page.value > totalPages.value) {
    page.value = totalPages.value || 1;
  }
};

const editMode = ref(false);

const editPassword = (item) => {
  form.value = {
    gubun: item.gubun,
    dbType: item.dbType,
    dbName: item.dbName,
    username: item.username,
    password: item.password,
    explanation: item.explanation,
    manager: item.manager,
    ipaddr: item.ipaddr,
    createUser: user.value.username  ,
    seq: item.seq // ✨ seq 추가
  };
  editMode.value = true;
  showModal.value = true;
};



const clearSearch = () => {
  search.value = '';
  passList.value = [];
  page.value = 1;
};

const pagedList = computed(() => {
  const start = (page.value - 1) * perPage;
  return passList.value.slice(start, start + perPage);
});

const totalPages = computed(() =>
  Math.ceil(passList.value.length / perPage)
);

const visiblePageNumbers = computed(() => {
  const pages = [];
  const half = Math.floor(maxPageButtons / 2);
  let start = Math.max(1, page.value - half);
  let end = Math.min(totalPages.value, start + maxPageButtons - 1);

  if (end - start < maxPageButtons - 1) {
    start = Math.max(1, end - maxPageButtons + 1);
  }

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  return pages;
});

const toggleVisible = (item) => {
  visibleSeq.value = visibleSeq.value === item.seq ? null : item.seq;
};

const copyToClipboard = (text) => {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text)
      .then(() => alert('📋 복사되었습니다!'))
      .catch(err => {
        console.error('클립보드 복사 실패:', err);
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
      alert('📋 복사되었습니다!');
    } else {
      alert('⚠️ 복사 실패 (지원 안 함)');
    }
  } catch (e) {
    console.error('fallback 복사 에러:', e);
    alert('⚠️ 복사 기능을 사용할 수 없습니다.');
  }
};


const deletePassword = (id) => {
  showConfirmModal('🗑️ 삭제 하시겠습니까?', async () => {
    await api.delete(`/api/pass/${id}`);
    showAlertModal('🗑️ 패스워드가 삭제되었습니다.');
    const currentPage = page.value;
    await searchList();
    page.value = currentPage > totalPages.value ? totalPages.value || 1 : currentPage;
  });
};

const openModal = () => {
  form.value = {
    gubun: '',
    dbType: '',
    dbName: '',
    username: '',
    password: '',
    explanation: '',
    manager: '',
    ipaddr: '',
    createUser: user.value.username  // ✨ Vuex에서 가져옴
  };
  editMode.value = false;
  showModal.value = true;
};


const submit = async () => {
  if (editMode.value) {
    await api.put(`/api/pass/${form.value.seq}`, form.value);
    showAlertModal('✅ 패스워드가 수정되었습니다.');
  } else {
    await api.post('/api/pass', form.value);
    showAlertModal('✅ 신규 패스워드가 등록되었습니다.');
  }

  closeModal();
  await searchList();
};

const closeModal = () => {
  showModal.value = false;
  editMode.value = false;

};

const showAlertModal = (msg) => {
  alertMessage.value = msg;
  showAlert.value = true;
};

</script>
<style scoped>
.password-container {
  max-width: 1200px;
  margin: auto;
  padding: 24px;
  background: #f9f9f9;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.password-container h2 {
  margin-bottom: 20px;
  font-size: 28px;
  color: #333;
  border-bottom: 2px solid #ccc;
  padding-bottom: 10px;
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
  background-color: #1976d2;
  color: white;
  transition: background 0.3s;
}

.toolbar button:hover {
  background-color: #0d47a1;
}

.pass-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 20px;
}

.pass-table th,
.pass-table td {
  padding: 10px;
  border-bottom: 1px solid #ddd;
  text-align: center;
}

.pass-table th {
  background-color: #eeeeee;
}

.password-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 4px;
  margin-top: 16px;
}

.pagination button {
  padding: 6px 12px;
  border: 1px solid #ccc;
  background: white;
  cursor: pointer;
  border-radius: 4px;
}

.pagination button.active {
  background-color: #1976d2;
  color: white;
  font-weight: bold;
  border-color: #1976d2;
}

.pagination button:disabled {
  background: #eee;
  cursor: not-allowed;
}

/* Modal Styles */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal-content {
  background-color: white;
  padding: 24px;
  border-radius: 12px;
  width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.2);
}

.modal-content h3 {
  margin-bottom: 16px;
  font-size: 22px;
  color: #222;
}

.modal-content form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.modal-content input,
.modal-content select {
  padding: 8px;
  border-radius: 6px;
  border: 1px solid #ccc;
  font-size: 14px;
}

.modal-content .buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.modal-content .buttons button {
  padding: 8px 14px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  background-color: #1976d2;
  color: white;
  font-weight: bold;
}

.modal-content .buttons button:hover {
  background-color: #0d47a1;
}

.alert-modal {
  position: fixed;
  top: 20%;
  left: 50%;
  transform: translateX(-50%);
  background-color: #1976d2;
  color: white;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  font-size: 16px;
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 20px;
}

.alert-close {
  background: white;
  color: #1976d2;
  border: none;
  padding: 6px 12px;
  font-weight: bold;
  border-radius: 6px;
  cursor: pointer;
}

.alert-close:hover {
  background-color: #eee;
}
.confirm-modal {
  position: fixed;
  top: 25%;
  left: 50%;
  transform: translateX(-50%);
  background-color: white;
  color: #333;
  padding: 20px 30px;
  border-radius: 12px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  z-index: 2001;
  text-align: center;
  min-width: 300px;
}

.confirm-text {
  font-size: 16px;
  margin-bottom: 20px;
}

.confirm-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.confirm-yes,
.confirm-no {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
}

.confirm-yes {
  background-color: #1976d2;
  color: white;
}

.confirm-no {
  background-color: #ddd;
}

</style>
