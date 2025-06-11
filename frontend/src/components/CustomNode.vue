<script setup>
import { Handle, Position } from '@vue-flow/core'
const props = defineProps(['data', 'id', 'onDelete'])

function handleDelete(e) {
  e.stopPropagation()
  if (props.onDelete) props.onDelete(props.id)
}
</script>
<template>
  <div
    class="custom-node"
    :style="{
      background: props.data?.color || '#fff',
      color: '#fff',
      borderColor: props.data?.color || '#bbb'
    }"
  >
    <!-- 삭제 버튼 -->
    <button class="delete-btn" title="노드 삭제" @click="handleDelete">✕</button>
    <Handle type="target" :position="Position.Top" id="top" />
    <Handle v-if="props.data?.type && ['ORACLE','TIBERO','MYSQL','MSSQL'].includes(props.data.type)" type="target" :position="Position.Left" id="left" />
    <Handle v-if="props.data?.type && ['ORACLE','TIBERO','MYSQL','MSSQL'].includes(props.data.type)" type="source" :position="Position.Right" id="right" />
    <Handle type="source" :position="Position.Bottom" id="bottom" />
    <div>
      {{ props.data?.label }}
      <span v-if="props.data?.type" style="font-size:11px; margin-left:5px;">({{ props.data.type }})</span>
    </div>
  </div>
</template>

<style scoped>
.custom-node {
  min-width: 100px;
  padding: 8px;
  border: 1.5px solid #bbb;
  border-radius: 7px;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-weight: 600;
  position: relative;
  box-shadow: 0 2px 8px #0001;
  transition:
    box-shadow 0.13s ease,
    filter 0.13s ease,
    transform 0.12s cubic-bezier(.23,1,.32,1);
}
.delete-btn {
  position: absolute;
  top: 5px;
  right: 7px;
  z-index: 2;
  background: rgba(20, 20, 20, 0.14);
  color: #fff;
  border: none;
  border-radius: 50%;
  font-size: 15px;
  width: 22px;
  height: 22px;
  line-height: 1;
  cursor: pointer;
  opacity: 0.6;
  transition: background 0.13s, opacity 0.13s;
}
.delete-btn:hover {
  background: #ff3d6b;
  color: #fff;
  opacity: 1;
}
</style>
