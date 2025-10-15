<script setup>
import { ref } from 'vue'
import api from '@/api';

const datafiles = ref([])
const loading = ref(false)

async function loadDatafiles({ dbName, tsName, dbType }) {
  loading.value = true
  try {
    const res = await axios.get('/api/tb/datafiles', {
      params: {
        dbname: dbName,
        tablespace_name: tsName,
        dbtype: dbType
      }
    })
    datafiles.value = res.data
  } finally {
    loading.value = false
  }
}

// 예시: 테이블스페이스를 클릭할 때 호출
function onTablespaceClick(row) {
  loadDatafiles({
    dbName: row.dbName,
    tsName: row.tablespaceName,
    dbType: row.dbType
  })
}
</script>

<template>
  <DataTable :value="datafiles" :loading="loading" stripedRows>
    <Column field="fileName" header="File Name" />
    <Column field="status" header="Status" />
    <Column field="autoExtensible" header="AutoExtensible" />
    <Column header="Size (MB)">
      <template #body="{ data }">{{ (data.bytes / 1024 / 1024).toFixed(1) }}</template>
    </Column>
    <Column header="Max (MB)">
      <template #body="{ data }">{{ (data.maxBytes / 1024 / 1024).toFixed(1) }}</template>
    </Column>
    <Column header="Increment (MB)">
      <template #body="{ data }">{{ (data.incrementBytes / 1024 / 1024).toFixed(1) }}</template>
    </Column>
    <Column field="snapDt" header="Snapshot" />
  </DataTable>
</template>
