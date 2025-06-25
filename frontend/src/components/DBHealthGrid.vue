<template>
  <div class="db-health-groups">
    <template v-for="group in groups" :key="group.status">
      <div v-if="group.instances.length" class="group-section">
        <div class="group-title" :style="{ color: group.color }">
          <i :class="group.icon" :style="{ marginRight: '8px', fontSize:'1.1em' }" />
          {{ group.label }}
          <span class="count">({{ group.instances.length }})</span>
        </div>
        <div class="db-health-grid">
          <div
            v-for="instance in group.instances"
            :key="instance.name"
            class="instance-box"
            :class="statusClass(group.status)"
          >
            <div class="instance-icon">
              <i
                class="pi pi-database"
                :style="{
                  color: group.color,
                  fontSize: '32px',
                  filter: group.status === '정상'
                    ? 'drop-shadow(0 0 3px #10b98155)'
                    : group.status === '위험'
                    ? 'drop-shadow(0 0 3px #ef444455)'
                    : ''
                }"
              />
            </div>
            <div class="instance-name">{{ instance.name }}</div>
            <div class="instance-status">{{ instance.status }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

// 샘플 DB 상태 데이터
const instances = [
  { name: 'ORACLE_PRD', status: '정상' },
  { name: 'TIBERO_DEV', status: '주의' },
  { name: 'PGSQL_OPS', status: '위험' },
  { name: 'MSSQL_INT', status: '정상' },
  { name: 'MONETDB', status: '정상' },
  { name: 'MYSQL_BI', status: '위험' },
  { name: 'DM_DB', status: '주의' }
]

const groupMeta = [
  { status: '위험', label: '위험', color: '#ef4444', icon: 'pi pi-times-circle' },
  { status: '주의', label: '주의', color: '#f59e0b', icon: 'pi pi-exclamation-circle' },
  { status: '정상', label: '정상', color: '#10b981', icon: 'pi pi-check-circle' }
]

const groups = computed(() =>
  groupMeta.map(meta => ({
    ...meta,
    instances: instances.filter(i => i.status === meta.status)
  }))
)

const statusClass = (status) => {
  switch (status) {
    case '정상': return 'status-normal'
    case '주의': return 'status-warning'
    case '위험': return 'status-critical'
    default: return 'status-unknown'
  }
}
</script>

<style scoped>
.db-health-groups {
  display: flex;
  flex-direction: column;
  gap: 26px;
}

.group-title {
  font-weight: bold;
  font-size: 1.06em;
  margin-bottom: 7px;
  letter-spacing: 0.01em;
  display: flex;
  align-items: center;
  padding-left: 3px;
}
.group-title .count {
  font-weight: normal;
  font-size: 0.97em;
  color: #8a8f9f;
  margin-left: 6px;
}

.group-section {
  margin-bottom: 7px;
}

/* 그리드! (여기서 가로로 나열 & 넘치면 줄바꿈) */
.db-health-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  /* grid의 최소/최대 크기 조정 가능 */
}

.instance-box {
  flex: 0 0 158px;
  min-width: 0;
  max-width: 210px;
  padding: 16px 8px 12px 8px;
  border-radius: 13px;
  background-color: #f3f4f6;
  box-shadow: none;
  text-align: center;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.06);
  transition: transform 0.18s, box-shadow 0.18s;
  position: relative;
}
.instance-box:hover {
  transform: scale(1.06);
  box-shadow: 0 2px 12px 0 #d4f3e0aa;
}
.instance-icon {
  margin-bottom: 7px;
}
.status-normal {
  border: 2.5px solid #10b981;
  color: #065f46;
}
.status-warning {
  border: 2.5px solid #f59e0b;
  color: #92400e;
}
.status-critical {
  border: 2.5px solid #ef4444;
  color: #991b1b;
}
.status-unknown {
  border: 2.5px dashed #bbb;
  color: #888;
}
.instance-name {
  font-size: 1.02em;
  font-weight: bold;
  margin-bottom: 3px;
}
.instance-status {
  font-size: 0.98em;
  margin-top: 2px;
  letter-spacing: 0.02em;
}

/* 모바일에서 2개씩만 */
@media (max-width: 800px) {
  .db-health-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
