<script setup>
import { ref, onMounted, watch } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'
import ColorPicker from 'primevue/colorpicker'
import api from '@/api'

const modules = ref([])
const editModule = ref(null)
const showForm = ref(false)
const isEdit = ref(false)

const dbTypeOptions = [
  { label: 'TIBERO', value: 'TIBERO' },
  { label: 'EDB', value: 'EDB' },
  { label: 'MSSQL', value: 'MSSQL' },
  { label: 'SYBASE', value: 'SYBASE' },
  { label: 'MARIADB', value: 'MARIADB' },
  { label: 'MYSQL', value: 'MYSQL' },
  { label: 'ORACLE', value: 'ORACLE' },
]

const moduleCodeOptions = [
  { label: 'HEALTH', value: 'HEALTH' },
  { label: 'INVALID_OBJECT', value: 'INVALID_OBJECT' },
  { label: 'PROC', value: 'PROC' }
]

// 모듈 목록 불러오기
const fetchModules = async () => {
  const { data } = await api.get('/api/monitor-module')
  modules.value = data.map(mod => ({
    ...mod,
    moduleId: mod.moduleId ?? mod.id   // id가 있으면 moduleId로 사용

  }))
}

onMounted(fetchModules)

// 등록/수정 폼 열기
function openForm(module = null) {
  if (module) {
    editModule.value = { ...module }
    // label → moduleName 매핑 추가
    editModule.value.moduleName = editModule.value.label;    // ← 추가!
    editModule.value.label = editModule.value.moduleName;    // (동기화 용도, 선택)
    if (editModule.value.color && !editModule.value.color.startsWith('#')) {
      editModule.value.color = '#' + editModule.value.color.replace(/^#*/, '')
    }
    isEdit.value = true
  } else {
    editModule.value = {
      moduleName: '',
      label: '',
      moduleCode: '',
      color: '#2196f3',
      remark: '',
      queryList: []
    }
    isEdit.value = false
  }
  showForm.value = true
}




// 저장 (등록/수정)
async function saveModule() {
  // ...색상 보정 등 기존 로직
  editModule.value.label = editModule.value.moduleName; // ← 여기도 추가
  // 저장 API 호출
  if (isEdit.value) {
    await api.put(`/api/monitor-module/${editModule.value.moduleId}`, editModule.value)
  } else {
    await api.post('/api/monitor-module', editModule.value)
  }
  showForm.value = false
  await fetchModules()
}


// 삭제
async function deleteModule(id) {
  console.log('삭제 시도하는 id:', id)
  if (confirm('정말 삭제하시겠습니까?')) {
    await api.delete(`/api/monitor-module/${id}`)
    await fetchModules()
  }
}

// 색상 항상 #포함 되게 보정
watch(
  () => editModule.value && editModule.value.color,
  (val) => {
    if (val && !val.startsWith('#')) {
      editModule.value.color = '#' + val.replace(/^#*/, '')
    }
  }
)
</script>


<template>
  <div class="monitor-manage-layout">
    <div class="module-list">
      <div class="list-header">
        <b>관제 모듈 목록</b>
        <Button label="신규 등록" icon="pi pi-plus" size="small" @click="openForm()" />
      </div>
      <table class="module-table">
        <thead>
          <tr>
            <th>코드</th>
            <th>이름</th>
            <th>색상</th>
            <th>비고</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="mod in modules" :key="mod.moduleId">
            <td>{{ mod.moduleCode }}</td>
            <td>{{ mod.label }}</td>
            <td>
              <span :style="{ background: mod.color, color:'#fff', borderRadius:'5px', padding:'2px 8px' }">{{ mod.color }}</span>
            </td>
            <td>{{ mod.remark }}</td>
            <td>
              <Button icon="pi pi-pencil" size="small" @click="openForm(mod)" text />
              <Button icon="pi pi-trash" size="small" severity="danger" @click="deleteModule(mod.moduleId)" text />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 등록/수정 모달 -->
    <Dialog v-model:visible="showForm" modal :header="isEdit ? '모듈 수정' : '신규 등록'" :style="{ width: '800px' }" class="custom-modal">
      <form @submit.prevent="saveModule">
        <div class="form-row">
          <label>모듈 코드</label>
          <Dropdown
            v-model="editModule.moduleCode"
            :options="moduleCodeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="코드 선택"
            :disabled="isEdit"
            style="width:100%"
            required
          />
        </div>
        <div class="form-row">
          <label>모듈명</label>
          <InputText v-model="editModule.moduleName" required style="width:100%" />
        </div>
        <div class="form-row">
          <label>색상</label>
           <div class="flex align-items-center gap-2">
                <!-- ColorPicker 버튼: 클릭하면 팔레트가 뜸 -->
                <ColorPicker v-model="editModule.color" :format="'hex'" style="width:32px; height:50px" />
                <!-- 선택된 색상 코드가 자동 반영되는 InputText -->
                <InputText v-model="editModule.color" placeholder="#2196f3" style="width:100px" />
            </div>
        </div>
        <div class="form-row">
          <label>비고</label>
          <InputText v-model="editModule.remark" style="width:95%" />
        </div>
        <div style="margin-top:22px; text-align:right;">
          <Button label="저장" icon="pi pi-check" type="submit" />
          <Button label="취소" icon="pi pi-times" severity="secondary" style="margin-left:7px" @click="showForm=false" />
        </div>
      </form>
      <label>DB타입별 쿼리</label>
        <div v-if="editModule.queryList && editModule.queryList.length">
          <div
            v-for="(q, idx) in editModule.queryList"
            :key="q.moduleQueryId || idx"
            class="query-item"
            style="display:flex; gap:6px; margin-bottom:6px;"
          >
            <Dropdown
              v-model="q.dbType"
              :options="dbTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="DB타입 선택"
              style="width: 150px"
            />

            <InputText v-model="q.queryText" placeholder="쿼리문" style="width:320px"/>
            <InputText v-model="q.remark" placeholder="비고" style="width:200px"/>
            <!-- 필요하면 쿼리 삭제 버튼도 추가 -->
            <Button icon="pi pi-trash" severity="danger" text size="small" @click="editModule.queryList.splice(idx,1)"/>
          </div>
          <Button label="쿼리 추가" icon="pi pi-plus" size="small"
            @click="editModule.queryList.push({ dbType: '', queryText: '', remark: '' })"/>
        </div>
        <div v-else>
          <Button label="쿼리 추가" icon="pi pi-plus" size="small"
            @click="editModule.queryList = [{ dbType: '', queryText: '', remark: '' }]"/>
        </div>
    </Dialog>
  </div>
</template>

<style scoped>
.monitor-manage-layout {
  max-width: 1000px;
  margin: 36px auto;
  background: #f7fafc;
  padding: 30px 30px 20px 30px;
  border-radius: 18px;
  box-shadow: 0 4px 24px 0 #aac6f855;
}
.module-list {
  width: 100%;
}
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.module-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1.5px 6px #d5e7f688;
}
.module-table th, .module-table td {
  padding: 7px 10px;
  font-size: 15px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
}
.module-table tr:last-child td {
  border-bottom: none;
}
.form-row {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
  gap: 10px;
}
.form-row label {
  width: 76px;
  color: #365485;
  font-weight: 600;
}
</style>
