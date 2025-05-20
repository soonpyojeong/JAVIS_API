<template>
  <div class="alert-summary">
    <h3>최근 알람 요약</h3>
    <ul>
      <li v-for="alert in alerts" :key="alert.id" :class="alertClass(alert.level)">
        <span class="time">[{{ alert.time }}]</span>
        <span class="message">{{ alert.message }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup>
const { alerts } = defineProps({
  alerts: {
    type: Array,
    required: true
  }
})


const alertClass = (level) => {
  switch (level) {
    case 'INFO': return 'info'
    case 'WARN': return 'warn'
    case 'ERROR': return 'error'
    default: return 'default'
  }
}
</script>

<style scoped>
.alert-summary {
  background-color: #ffffff;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.05);
}

.alert-summary h3 {
  margin-bottom: 12px;
  font-size: 18px;
  color: #111827;
}

.alert-summary ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.alert-summary li {
  padding: 8px;
  border-radius: 6px;
  margin-bottom: 6px;
  font-size: 14px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.alert-summary li.info {
  background-color: #e0f2fe;
  color: #0369a1;
}

.alert-summary li.warn {
  background-color: #fef3c7;
  color: #92400e;
}

.alert-summary li.error {
  background-color: #fee2e2;
  color: #991b1b;
}

.time {
  font-weight: bold;
}
</style>