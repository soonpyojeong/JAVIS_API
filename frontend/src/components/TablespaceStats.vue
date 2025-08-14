<template>
  <div class="container p-4">
    <h2 class="text-2xl font-bold text-blue-600 mb-4">📊 테이블스페이스 용량 통계</h2>

    <!-- 🔎 조건 필터 영역 -->
    <div class="filters" style="display: flex; flex-wrap: wrap; align-items: center; margin-bottom: 15px;">
      <div class="filter-block">
        <Dropdown
          v-model="dateFilterType"
          :options="filterTypeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="날짜 필터 유형 선택"
          class="w-48"
        />
      </div>

      <div class="filter-block" v-if="dateFilterType === 'month'">
        <Calendar v-model="startMonth" view="month" dateFormat="yy-mm" showIcon placeholder="시작 월 선택" />
        <i class="pi pi-angle-right text-gray-500" />
        <Calendar v-model="endMonth" view="month" dateFormat="yy-mm" showIcon placeholder="종료 월 선택" />
      </div>

      <div class="filter-block">
        <Calendar v-if="dateFilterType === 'single'" v-model="selectedSingleDate" dateFormat="yy-mm-dd" showIcon placeholder="날짜 선택" />
      </div>

      <div class="filter-block">
        <Calendar v-if="dateFilterType === 'range'" v-model="selectedDateRange" selectionMode="range" dateFormat="yy-mm-dd" showIcon placeholder="일자 범위 선택" />
      </div>

      <div class="filter-block">
        <Dropdown v-model="selectedDb" :options="dbOptions" optionLabel="label" optionValue="value" placeholder="DB 선택" />
      </div>

      <div class="filter-block">
        <Dropdown v-model="selectedTs" :options="tsOptions" optionLabel="label" optionValue="value" placeholder="테이블스페이스 선택" />
      </div>

      <div class="filter-block">
        <Button label="조회" icon="pi pi-search" :disabled="isLoading" @click="fetchStats" class="p-button-success" />
        <ProgressBar v-if="isLoading" mode="indeterminate" style="height: 6px; margin-top: 12px" />
      </div>

      <div class="filter-block">
        <Button label="초기화" icon="pi pi-times" :disabled="isLoading" @click="resetFilters" class="p-button-secondary" />
      </div>
    </div>

    <!-- 📦 통계 DataTable -->
    <div class="card-grid grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
      <DataTable
        :value="tsSummaryList"
        class="p-datatable-sm mt-4"
        scrollable
        :sortField="'realUsedMb'"
        :sortOrder="-1"
      >
        <Column field="dbName" header="DB명" />
        <Column field="tsName" header="테이블스페이스명" />

        <Column field="totalSizeMb" header="TotalSize(MB)">
          <template #body="{ data }">
            {{ data.totalSizeMb?.toLocaleString() }} MB
          </template>
        </Column>

        <Column field="totUsagePercent" header="Total대비비율(%)">
          <template #body="{ data }">
            {{ data.totUsagePercent != null ? data.totUsagePercent.toFixed(2) + ' %' : '-' }}
          </template>
        </Column>

        <Column field="realUsedMb" header="실사용량(MB)">
          <template #body="{ data }">
            {{ data.realUsedMb?.toLocaleString() }} MB
          </template>
        </Column>

        <Column field="realUsedPercent" header="실사용비율(%))">
          <template #body="{ data }">
            {{ data.realUsedPercent != null ? data.realUsedPercent.toFixed(2) + ' %' : '-' }}
          </template>
        </Column>

        <Column field="remainMb" header="남은공간(MB)">
          <template #body="{ data }">
            {{ data.remainMb?.toLocaleString() }} MB
          </template>
        </Column>

        <!-- ✅ 날짜로 표기 -->
        <Column field="fullReachDate" header="FULL도달일">
          <template #body="{ data }">
            <span :style="{ color: data.dayToFull != null && data.dayToFull <= 10 ? 'red' : 'inherit' }">
              {{ data.fullReachDate ?? '-' }}
            </span>
          </template>
        </Column>

        <Column field="reach95Date" header="95% 도달일">
          <template #body="{ data }">
            <span :style="{ color: data.dayTo95Percent != null && data.dayTo95Percent <= 10 ? 'red' : 'inherit' }">
              {{ data.reach95Date ?? '-' }}
            </span>
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import api from "@/api";
import { ref, onMounted } from "vue";
import DataTable from "primevue/datatable";
import Column from "primevue/column";
import Calendar from "primevue/calendar";
import Dropdown from "primevue/dropdown";
import Button from "primevue/button";
import ProgressBar from "primevue/progressbar";

const dateFilterType = ref("month");
const filterTypeOptions = [
  { label: "월 선택", value: "month" },
  { label: "단일 날짜", value: "single" },
  { label: "일자 범위", value: "range" },
];

const startMonth = ref(null);
const endMonth = ref(null);
const selectedSingleDate = ref(null);
const selectedDateRange = ref(null);
const selectedDb = ref(null);
const selectedTs = ref(null);
const selectedDbType = ref(null); // ✅ 선택한 DB의 타입 저장

const tsOptions = ref([]);
const tsSummaryList = ref([]);
const dbOptions = ref([]);
const isLoading = ref(false);

/* ===== 유틸 ===== */
// (타임존 안전) 날짜 → YYYYMMDD
function formatDateYMD(date) {
  const d = new Date(date);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}${m}${day}`;
}

const getMonthRange = () => {
  if (!startMonth.value) {
    alert("⛔ 시작 월을 선택해주세요.");
    return { startDate: "", endDate: "" };
  }
  const start = new Date(startMonth.value);
  let end = endMonth.value ? new Date(endMonth.value) : new Date(startMonth.value);
  if (end < start) {
    alert("⛔ 종료 월은 시작 월보다 이전일 수 없습니다.");
    return { startDate: "", endDate: "" };
  }
  const yearDiff = end.getFullYear() - start.getFullYear();
  const monthDiff = end.getMonth() - start.getMonth();
  const totalMonthDiff = yearDiff * 12 + monthDiff;
  if (totalMonthDiff > 11) {
    alert("⛔ 최대 1년(12개월)까지만 선택할 수 있습니다.");
    return { startDate: "", endDate: "" };
  }
  const y1 = start.getFullYear();
  const m1 = String(start.getMonth() + 1).padStart(2, "0");
  const startDate = `${y1}${m1}01`;
  const endYear = end.getFullYear();
  const endMonthNum = end.getMonth();
  const lastDay = new Date(endYear, endMonthNum + 1, 0).getDate();
  const endDate = `${endYear}${String(endMonthNum + 1).padStart(2, "0")}${String(lastDay).padStart(2, "0")}`;
  return { startDate, endDate };
};

/* ===== 조회 ===== */
const fetchStats = async () => {
  if (!dateFilterType.value || !selectedDb.value) {
    alert("날짜 필터와 DB는 필수 선택입니다.");
    return;
  }

  // ✅ 현재 선택된 DB에서 dbType 찾아서 저장
  const selectedDbObj = dbOptions.value.find(db => db.value === selectedDb.value);
  selectedDbType.value = selectedDbObj ? selectedDbObj.type : null;

  let startDate = "", endDate = "";

  if (dateFilterType.value === "month") {
    const range = getMonthRange();
    startDate = range.startDate;
    endDate = range.endDate;
    if (!startDate || !endDate) return;
  } else if (dateFilterType.value === "single") {
    if (!selectedSingleDate.value) {
      alert("날짜를 선택해주세요.");
      return;
    }
    startDate = endDate = formatDateYMD(selectedSingleDate.value);
  } else if (dateFilterType.value === "range") {
    if (!selectedDateRange.value || selectedDateRange.value.length < 2) {
      alert("시작일과 종료일을 모두 선택해주세요.");
      return;
    }
    const start = new Date(selectedDateRange.value[0]);
    const end = new Date(selectedDateRange.value[1]);
    const diffInDays = Math.floor((end - start) / (1000 * 60 * 60 * 24));
    if (diffInDays > 365) {
      alert("⛔ 최대 1년(365일) 범위까지만 선택할 수 있습니다.");
      return;
    }
    startDate = formatDateYMD(start);
    endDate = formatDateYMD(end);
  }

  isLoading.value = true;
  try {
    // ✅ dbType 같이 넘김
    await fetchTablespaceSummary(selectedDb.value, selectedDbType.value, startDate, endDate);
  } catch (e) {
    console.error("통계 조회 실패:", e);
    alert("조회 중 오류가 발생했습니다.");
  } finally {
    isLoading.value = false;
  }
};

/* ===== API ===== */
const fetchTablespaceSummary = async (dbName, dbType, startDate, endDate) => {
  const res = await api.get(`/api/tb/summary`, { params: { dbName, dbType, startDate, endDate } });
  tsSummaryList.value = res.data;
};

const loadDbList = async () => {
  try {
    const res = await api.get("/api/db-list");
    dbOptions.value = res.data
      .map((db) => ({
        label: db.dbName + (db.sizeChk === "N" ? " (미수집)" : ""),
        value: db.dbName,
        type: db.dbType, // ✅ dbType도 저장 (예: "ORACLE" | "TIBERO")
      }))
      .sort((a, b) => a.label.localeCompare(b.label));
  } catch (e) {
    console.error("DB 목록 조회 실패:", e);
  }
};

/* ===== 초기화 ===== */
const resetFilters = () => {
  dateFilterType.value = "month";
  startMonth.value = null;
  endMonth.value = null;
  selectedSingleDate.value = null;
  selectedDateRange.value = null;
  selectedDb.value = null;
  selectedDbType.value = null;
  selectedTs.value = null;
  tsSummaryList.value = [];
};

onMounted(() => {
  loadDbList();
});
</script>

<style scoped>
.container { max-width: 1200px; margin: 0 auto; }
.filter-block { margin-right: 5px; flex-direction: column; }
</style>
