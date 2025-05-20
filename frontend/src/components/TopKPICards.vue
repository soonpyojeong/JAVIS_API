<template>
  <div class="kpi-cards">
    <div class="kpi-card" v-for="(item, key) in statsMap" :key="key">
      <div class="kpi-label">{{ item.label }}</div>
      <div class="kpi-value">{{ item.value }}</div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  stats: {
    type: Object,
    default: () => ({ totalDBs: 0, normal: 0, warning: 0, critical: 0 })
  }
})

const statsMap = {
  totalDBs: { label: '총 DB 수', value: props.stats.totalDBs },
  normal: { label: '정상', value: props.stats.normal },
  warning: { label: '다운', value: props.stats.warning },
  critical: { label: '미관제', value: props.stats.critical }
}
</script>

<style scoped>
.kpi-cards {
  display: flex;
  justify-content: space-around;
  gap: 16px;
}

.kpi-card {
  flex: 1;
  padding: 16px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  text-align: center;
}

.kpi-label {
  font-size: 14px;
  color: #6b7280;
}

.kpi-value {
  font-size: 24px;
  font-weight: bold;
  color: #111827;
  margin-top: 4px;
}
</style>
