//EdgeWithButton.vue
<script setup>
import { BaseEdge, EdgeLabelRenderer, getBezierPath, useVueFlow } from '@vue-flow/core'
import { computed } from 'vue'

const props = defineProps({
  id: String,
  sourceX: Number,
  sourceY: Number,
  targetX: Number,
  targetY: Number,
  sourcePosition: String,
  targetPosition: String,
  markerEnd: String,
  style: Object,
})

const { removeEdges } = useVueFlow()
const path = computed(() => getBezierPath(props))
</script>

<script>
export default {
  inheritAttrs: false,
}
</script>

<template>
  <BaseEdge :id="id" :style="style" :path="path[0]" :marker-end="markerEnd" />

  <EdgeLabelRenderer>
    <div
      :style="{
        pointerEvents: 'all',
        position: 'absolute',
        transform: `translate(-50%, -50%) translate(${path[1]}px,${path[2]}px)`,
      }"
      class="nodrag nopan"
    >
      <button class="edgebutton" @click="removeEdges(id)" style="border:none; background:#f87171; color:#fff; border-radius:50%; width:20px; height:20px; font-size:16px; cursor:pointer; box-shadow:0 2px 6px #0001;">×</button>
    </div>
  </EdgeLabelRenderer>
</template>
