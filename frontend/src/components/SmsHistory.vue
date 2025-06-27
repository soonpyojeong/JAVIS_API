<template>
  <div class="container">
    <h1 class="title">📨 SMS 전송 히스토리</h1>

    <!-- 컨트롤 박스 -->
    <div class="control-box">
      <label for="days" class="mr-2">조회할 일수</label>
      <InputNumber
        id="days"
        v-model="day"
        :min="1"
        class="mr-2"
        style="width: 60px; height: 38px;"
      />
      <div style="position: relative; display: inline-block;">
        <Button
          label="조회"
          @click="fetchSmsHistories"
          class="p-button-success"
          style="height: 38px; min-width: 50px; margin-left: 80px;"
          :disabled="loadingJobId === 'sms-history'"
        />
        <ProgressSpinner
          v-if="loadingJobId === 'sms-history'"
          style="position: absolute; left: 40%; top: 50%; transform: translate(-50%, -50%); width: 22px; height: 22px; pointer-events: none;"
          strokeWidth="4"
        />
      </div>
      <InputText
        v-model="msgSearch"
        @input="onGlobalFilter"
        placeholder="메시지 검색"
        class="ml-3"
        style="width: 220px;"
      />
      <Button label="대량 메시지 전송 처리" class="p-button-danger ml-2" @click="updateAllSmsHistories" />
      <Button label="엑셀 추출" icon="pi pi-file-excel" class="p-button-info ml-2" @click="showExcelDialog = true" />
    </div>

    <!-- 엑셀 추출 팝업 -->
    <Dialog v-model:visible="showExcelDialog" header="엑셀 추출 방식 선택" modal style="width: 350px;">
      <div class="flex flex-col gap-3 p-2">
        <Button label="즉시 추출(전체)" class="p-button-success" @click="exportExcel('all')" />
        <Button label="월별 추출" class="p-button-info" @click="exportExcel('month')" />
        <Button label="연도별 추출" class="p-button-warning" @click="exportExcel('year')" />
      </div>
    </Dialog>

    <!-- SMS 이력 테이블 -->
    <DataTable
      :value="smsHistories"
      :paginator="true"
      :rows="15"
      :filters="filters"
      filterDisplay="row"
      :globalFilterFields="['inDate','inTime','sendName','recName','msg','result','kind','errCode']"
      responsiveLayout="scroll"
      scrollable
      scrollHeight="500px"
      autoLayout
      :sortField="'inDate'"
      :sortOrder="-1"
      :rowHover="true"
      class="sms-history-table"
      dataKey="seqno"
    >
      <Column field="inDate" header="INDATE" filter filterPlaceholder="검색..." :sortable="true"
              style="width: 110px;" headerStyle="text-align: left;" bodyStyle="text-align: left;" />
      <Column field="inTime" header="INTIME" filter filterPlaceholder="검색..." :sortable="true"
              style="width: 90px;" headerStyle="text-align: left;" bodyStyle="text-align: left;" />
      <Column field="sendName" header="SENDNAME" filter filterPlaceholder="검색..."
              style="width: 110px;" headerStyle="text-align: left;" bodyStyle="text-align: left;" />
      <Column field="recName" header="RECVNAME" filter filterPlaceholder="검색..."
              style="width: 110px;" headerStyle="text-align: left;" bodyStyle="text-align: left;" />

      <!-- MSG 컬럼: ...+툴팁+줄바꿈 -->
      <Column field="msg" header="MSG" filter filterPlaceholder="검색..."
        :body="msgBody"
        style="min-width: 340px; max-width: 800px; white-space: pre-line;"
        headerStyle="text-align: center;"
        bodyStyle="text-align: left;"
      />

      <Column field="result" header="RESULT" filter filterPlaceholder="0:미전송, 1:전송"
              :body="resultBody"
              style="width: 80px;" headerStyle="text-align: center;" bodyStyle="text-align: center;" />
    </DataTable>

    <!-- 알림 모달 -->
    <Dialog v-model:visible="showModal" header="알림" modal>
      <p>{{ updateMessage }}</p>
      <Button label="확인" @click="showModal = false" />
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted,h } from 'vue';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Button from 'primevue/button';
import Dialog from 'primevue/dialog';
import InputText from 'primevue/inputtext';
import InputNumber from 'primevue/inputnumber';
import ProgressSpinner from 'primevue/progressspinner';
import api from "@/api";
import * as XLSX from "xlsx";
import { saveAs } from "file-saver";
import { useToast } from 'primevue/usetoast';

const toast = useToast();
const smsHistories = ref([]);
const day = ref(3);
const msgSearch = ref('');
const showModal = ref(false);
const updateMessage = ref('');
const showExcelDialog = ref(false);
const loadingJobId = ref(null);

const filters = ref({
  inDate: { value: null, matchMode: "contains" },
  inTime: { value: null, matchMode: "contains" },
  sendName: { value: null, matchMode: "contains" },
  recName: { value: null, matchMode: "contains" },
  msg: { value: null, matchMode: "contains" },
  result: { value: null, matchMode: "contains" },
  kind: { value: null, matchMode: "contains" },
  errCode: { value: null, matchMode: "contains" },
  global: { value: null, matchMode: 'contains' }
});

function msgBody(row) {
  if (!row) return '';
  const msg = row.msg || '';
  let shortMsg = msg.replace(/\n/g, ' ');
  if (shortMsg.length > 60) shortMsg = shortMsg.slice(0, 60) + '...';
  return h('span', { title: msg }, shortMsg);
}

function fetchSmsHistories() {
  loadingJobId.value = 'sms-history';
  api.get(`/api/sms/all?day=${day.value}`)
    .then(response => {
      if (Array.isArray(response.data)) {
        // 최신순 정렬
        const arr = response.data.sort((a, b) => {
          const dateA = (a.inDate || a.indate || '') + (a.inTime || a.intime || '');
          const dateB = (b.inDate || b.indate || '') + (b.inTime || b.intime || '');
          return dateB.localeCompare(dateA);
        });
        smsHistories.value = arr;
      } else {
        smsHistories.value = [];
      }
    })
    .catch(error => {
      console.error('Error fetching SMS histories:', error);
    })
    .finally(() => {
      loadingJobId.value = null;
    });
}

function onGlobalFilter(e) {
  filters.value['global'].value = e.target.value;
}

function resultBody(row) {
  return row.result === 0 || row.result === "0" ? "미전송" : "전송";
}

function updateAllSmsHistories() {
  api.put('/api/sms/updateall')
    .then(response => {
      const updatedCount = response.data?.updatedCount || 0;
      updateMessage.value = `전체 메시지 전송 처리가 완료되었습니다. (총 ${updatedCount}건)`;
      showModal.value = true;
      fetchSmsHistories();
    })
    .catch(error => {
      console.error('Error updating SMS histories:', error);
    });
}

// 날짜 포맷(yyyyMMdd, 8자리)
function isValidDate(dateStr) {
  return typeof dateStr === 'string' && /^\d{8}$/.test(dateStr);
}
function groupByField(data, length) {
  const groups = {};
  data.forEach(row => {
    const indate = row.inDate || row.INDATE || '';
    if (isValidDate(indate)) {
      const key = indate.substring(0, length);
      if (!groups[key]) groups[key] = [];
      groups[key].push({ ...row, INDATE: indate });
    }
  });
  return groups;
}
function exportExcel(mode = 'all') {
  showExcelDialog.value = false;
  const dataArr = smsHistories.value;
  if (!dataArr || dataArr.length === 0) {
    toast.add({ severity: 'warn', summary: '알림', detail: '데이터를 먼저 조회해 주세요.', life: 2000 });
    return;
  }
  const headers = [
    "SEQNO","INDATE","INTIME","MEMBER","SENDID","SENDNAME",
    "RPHONE1","RPHONE2","RPHONE3","RECVNAME",
    "SPHONE1","SPHONE2","SPHONE3","MSG","URL","RDATE","RTIME","RESULT","KIND","ERRCODE"
  ];
  const allData = dataArr.map(row => ({
    SEQNO: row.seqno ?? '',
    INDATE: row.inDate ?? row.INDATE ?? '',
    INTIME: row.inTime ?? row.INTIME ?? '',
    MEMBER: row.member ?? '',
    SENDID: row.sendId ?? row.SENDID ?? '',
    SENDNAME: row.sendName ?? row.SENDNAME ?? '',
    RPHONE1: row.rphone1 ?? '',
    RPHONE2: row.rphone2 ?? '',
    RPHONE3: row.rphone3 ?? '',
    RECVNAME: row.recName ?? row.RECVNAME ?? '',
    SPHONE1: row.sphone1 ?? '',
    SPHONE2: row.sphone2 ?? '',
    SPHONE3: row.sphone3 ?? '',
    MSG: row.msg ?? '',
    URL: row.url ?? '',
    RDATE: row.rdate ?? '',
    RTIME: row.rtime ?? '',
    RESULT: row.result ?? '',
    KIND: row.kind ?? '',
    ERRCODE: row.errCode ?? row.ERRCODE ?? ''
  }));
  const wb = XLSX.utils.book_new();
  let sheetCount = 0;
  if (mode === 'all') {
    if (allData.length === 0) {
      toast.add({ severity: 'warn', summary: '알림', detail: '엑셀로 내보낼 데이터가 없습니다.', life: 2000 });
      return;
    }
    const ws = XLSX.utils.json_to_sheet(allData, { header: headers });
    XLSX.utils.book_append_sheet(wb, ws, "SMS_HISTORY");
    sheetCount++;
  } else {
    const length = mode === 'month' ? 6 : 4;
    const groups = groupByField(allData, length);
    Object.entries(groups).forEach(([key, rows]) => {
      if (rows.length > 0) {
        const ws = XLSX.utils.json_to_sheet(rows, { header: headers });
        XLSX.utils.book_append_sheet(wb, ws, key);
        sheetCount++;
      }
    });
    if (sheetCount === 0) {
      toast.add({ severity: 'warn', summary: '알림', detail: `분류 기준에 맞는 데이터가 없습니다.`, life: 2000 });
      return;
    }
  }
  if (sheetCount === 0) {
    toast.add({ severity: 'warn', summary: '알림', detail: '엑셀로 내보낼 데이터가 없습니다.', life: 2000 });
    return;
  }
  const today = new Date();
  const dateStr = today.toISOString().slice(0,10).replace(/-/g,'');
  let fileName = `SMS_HISTORY_${dateStr}`;
  if (mode === 'month') fileName += '_월별';
  if (mode === 'year') fileName += '_연별';
  fileName += '.xlsx';
  const wbout = XLSX.write(wb, { bookType: "xlsx", type: "array" });
  saveAs(new Blob([wbout], { type: "application/octet-stream" }), fileName);
}

onMounted(fetchSmsHistories);
</script>

<style scoped>
.container {
  min-height: 85vh;
  font-family: 'Arial', sans-serif;
  padding: 20px;
  max-width: 1250px;
  margin: 0 auto;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 10px 20px rgba(0,0,0,0.08);
}
.title {
  font-size: 26px;
  font-weight: bold;
  margin-bottom: 30px;
}
.control-box {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background-color: #fafbfc;
}
.sms-history-table .p-datatable-thead > tr > th {
  text-align: center !important;
  vertical-align: middle;
  font-size: 15px;
}
.sms-history-table .p-datatable-tbody > tr > td {
  vertical-align: middle;
  font-size: 15px;
}
</style>
