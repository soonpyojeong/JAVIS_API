<template>
  <div class="kpi-cards">
    <div
      class="kpi-card"
      v-for="(item, key) in statsList"
      :key="key"
      :style="{ borderColor: item.color }"
    >
      <div class="kpi-icon" :style="{ color: item.color, background: item.bg }">
        <i :class="item.icon"></i>
      </div>
      <div class="kpi-label">{{ item.label }}</div>
      <div class="kpi-value" :style="{ color: item.color }">{{ item.value }}</div>
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

// KPI 리스트(순서/아이콘/컬러/라벨 등)
const statsList = [
  {
    key: 'totalDBs',
    label: '총 DB 수',
    value: props.stats.totalDBs,
    color: '#3b82f6',
    icon: 'pi pi-database',
    bg: '#eff6ff'
  },
  {
    key: 'normal',
    label: '정상',
    value: props.stats.normal,
    color: '#10b981',
    icon: 'pi pi-check-circle',
    bg: '#ecfdf5'
  },
  {
    key: 'warning',
    label: '다운',
    value: props.stats.warning,
    color: '#f59e0b',
    icon: 'pi pi-exclamation-circle',
    bg: '#fef7e4'
  },
  {
    key: 'critical',
    label: '미관제',
    value: props.stats.critical,
    color: '#ef4444',
    icon: 'pi pi-times-circle',
    bg: '#fee2e2'
  }
]
</script>

<style scoped>
.kpi-cards {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: stretch;
}

.kpi-card {
  flex: 1 1 130px;
  min-width: 145px;
  max-width: 210px;
  padding: 20px 12px 18px 12px;
  background-color: #fff;
  border-radius: 14px;
  border: 2.3px solid #e5e7eb;
  box-shadow: 0 4px 16px 0 #a5b4fc18;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: box-shadow .14s, border-color .13s, transform .14s;
  cursor: pointer;
}
.kpi-card:hover {
  border-color: #60a5fa;
  transform: translateY(-2px) scale(1.04);
  box-shadow: 0 7px 22px 0 #60a5fa11;
}
.kpi-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 7px;
  font-size: 1.85em;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: #eff6ff;
  box-shadow: 0 2px 8px 0 #cbd5e11c;
}
.kpi-label {
  font-size: 15px;
  color: #6b7280;
  margin-bottom: 2px;
  margin-top: 4px;
  font-weight: 500;
  letter-spacing: 0.02em;
}
.kpi-value {
  font-size: 27px;
  font-weight: bold;
  margin-top: 3px;
  letter-spacing: 0.04em;
}
@media (max-width: 800px) {
  .kpi-cards {
    flex-direction: column;
    gap: 13px;
  }
  .kpi-card {
    min-width: 90vw;
    max-width: 98vw;
  }
}
</style>
