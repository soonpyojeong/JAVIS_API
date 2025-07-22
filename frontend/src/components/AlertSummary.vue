<template>
  <div class="alert-summary">
    <span class="collect-time">(최종 수집: {{ collectedAt }})</span>
    <ul>
    <li
      v-for="alert in alerts"
      :key="alert.alertType + '-' + alert.time + '-' + alert.dbDesc + '-' + alert.message"
      :class="alertClass(alert.lvl)"
    >
      <span class="type">{{ alertTypeIcon(alert.alertType) }}</span>
      <span class="time">[{{ alert.time }}]</span>
      <span class="db">{{ alert.dbDesc }}</span>
      <span class="chktype">{{ alert.chkType }}</span>
      <span class="message">{{ alert.message }}</span>
    </li>

    </ul>
  </div>
</template>

<script setup>
const { alerts, collectedAt } = defineProps({
  alerts: { type: Array, required: true },
  collectedAt: { type: String, default: '' }
})


const alertClass = (lvl) => {
  switch (lvl) {
    case 'INFO': return 'info'
    case 'WARN': return 'warn'
    case 'ERROR': return 'error'
    default: return 'default'
  }
}

const alertTypeIcon = (type) => {
  switch (type) {
    case 'INVALID': return '❗'
    case 'LOG': return '📝'
    case 'TBS': return '💾'
    default: return '🔔'
  }
}

</script>


<style scoped>
.alert-summary {
  background-color: #fff;
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
  gap: 10px;
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
.type {
  font-size: 1.2em;
  width: 2em;
  text-align: center;
}
.time {
  font-weight: bold;
  width: 9.5em;        /* 기존보다 좀 넓게 */
  min-width: 9.5em;    /* 최소폭 보장 */
  flex-shrink: 0;
  white-space: nowrap; /* 줄바꿈 방지!! */
}
.db {
  font-weight: 600;
  color: #6366f1;
  width: 10em;
  min-width: 12em;
  flex-shrink: 0;
  margin-left: 10px;    /* ← 왼쪽 여백 추가 */
}
.chktype {
  color: #6366f1;
  min-width: 4em;
  flex-shrink: 0;
  margin-right: 5px;  /* ← 오른쪽 여백 기존대로 */
}
.message {
  flex: 1;
  word-break: break-all;
}
</style>
