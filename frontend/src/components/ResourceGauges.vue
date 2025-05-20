<template>
  <div class="gauges-wrapper">
    <div class="gauge-card" v-for="(metric, key) in metrics" :key="key">
      <div class="gauge-label">{{ metric.label }}</div>
      <div class="gauge-chart">
        <canvas :ref="el => gaugeRefs[key] = el"></canvas>
      </div>
      <div class="gauge-value">{{ metric.value }}%</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, watch, ref } from 'vue'
import { Chart, registerables } from 'chart.js'
Chart.register(...registerables)

const props = defineProps({
  cpu: Number,
  memory: Number,
  disk: Number
})

const gaugeRefs = ref({})
const charts = ref({})

const metrics = ref({
  cpu: { label: 'CPU 사용률', value: props.cpu || 0, color: '#4f46e5' },
  memory: { label: '메모리 사용률', value: props.memory || 0, color: '#f59e0b' },
  disk: { label: '디스크 사용률', value: props.disk || 0, color: '#ef4444' }
})

const renderGauge = (ctx, value, color) => {
  return new Chart(ctx, {
    type: 'doughnut',
    data: {
      datasets: [
        {
          data: [value, 100 - value],
          backgroundColor: [color, '#e5e7eb'],
          borderWidth: 0,
          cutout: '80%'
        }
      ]
    },
    options: {
      rotation: -90,
      circumference: 180,
      plugins: { legend: { display: false }, tooltip: { enabled: false } },
    }
  })
}

onMounted(() => {
  for (const [key, metric] of Object.entries(metrics.value)) {
    const ctx = gaugeRefs.value[key].getContext('2d')
    charts.value[key] = renderGauge(ctx, metric.value, metric.color)
  }
})

watch(() => [props.cpu, props.memory, props.disk], ([cpu, memory, disk]) => {
  metrics.value.cpu.value = cpu
  metrics.value.memory.value = memory
  metrics.value.disk.value = disk

  for (const [key, metric] of Object.entries(metrics.value)) {
    const chart = charts.value[key]
    chart.data.datasets[0].data = [metric.value, 100 - metric.value]
    chart.update()
  }
})
</script>

<style scoped>
.gauges-wrapper {
  display: flex;
  justify-content: space-around;
  padding: 20px;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.06);
  gap: 20px;
}

.gauge-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 150px;
}

.gauge-label {
  font-weight: 600;
  margin-bottom: 8px;
}

.gauge-chart {
  width: 100px;
  height: 50px;
  position: relative;
}

.gauge-value {
  font-size: 20px;
  font-weight: bold;
  margin-top: 8px;
  color: #111827;
}
</style>