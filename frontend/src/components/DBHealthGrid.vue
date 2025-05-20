<template>
  <div class="db-health-grid">
    <div
      v-for="instance in instances"
      :key="instance.name"
      class="instance-box"
      :class="statusClass(instance.status)"
    >
      <div class="instance-name">{{ instance.name }}</div>
      <div class="instance-status">{{ instance.status }}</div>
    </div>
  </div>
</template>

<script setup>
const { instances } = defineProps({
  instances: {
    type: Array,
    required: true
  }
})


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
.db-health-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.instance-box {
  padding: 12px;
  border-radius: 10px;
  background-color: #f3f4f6;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s;
}

.instance-box:hover {
  transform: scale(1.03);
}

.status-normal {
  border: 2px solid #10b981;
  color: #065f46;
}

.status-warning {
  border: 2px solid #f59e0b;
  color: #92400e;
}

.status-critical {
  border: 2px solid #ef4444;
  color: #991b1b;
}
</style>