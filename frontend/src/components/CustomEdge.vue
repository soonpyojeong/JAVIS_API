//CustomEdge.vue
<script setup>

import { computed } from 'vue'
import { BaseEdge, EdgeLabelRenderer, getBezierPath, useVueFlow } from '@vue-flow/core'
defineOptions({ inheritAttrs: false }) // 또는 export default { inheritAttrs: false }

const props = defineProps({
  id: String,
  sourceX: Number,
  sourceY: Number,
  targetX: Number,
  targetY: Number,
  sourcePosition: String,
  targetPosition: String,
  markerEnd: String,
  style: Object
})
const { removeEdges } = useVueFlow()
const path = computed(() => getBezierPath(props))
</script>

<template>
  <BaseEdge :path="path[0]" :marker-end="props.markerEnd" :style="props.style" />
  <EdgeLabelRenderer>
    <div
      :style="{
        pointerEvents: 'all',
        position: 'absolute',
        transform: `translate(-50%, -50%) translate(${path[1]}px,${path[2]}px)`
      }"
      class="nodrag nopan"
    >
      <button class="edgebutton" @click="removeEdges(props.id)">×</button>
    </div>
  </EdgeLabelRenderer>
</template>

<style scoped>
.edgebutton {
  border-radius: 999px;
  cursor: pointer;
  background: #fff;
  color: #ff0072;
  border: 1px solid #ff0072;
  width: 20px;
  height: 20px;
  font-size: 14px;
  font-weight: bold;
  line-height: 18px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.edgebutton:hover {
  box-shadow: 0 0 0 2px pink, 0 0 0 4px #f05f75;
  background: #ffe9f0;
}
</style>
