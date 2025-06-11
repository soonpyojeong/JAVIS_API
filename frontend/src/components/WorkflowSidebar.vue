//WorkflowSidebar.vue
<script setup>
const props = defineProps(['dbList', 'moduleList', 'onDragStart'])
</script>

<template>
  <aside class="sidebar">
    <div class="description">[관제 모듈] → 워크플로우로 드래그</div>
    <div class="nodes">
      <div
        v-for="mod in moduleList"
        :key="mod.id"
        class="monitor-module"
        draggable="true"
        @dragstart="(e) => onDragStart(e, { ...mod, isModule: true })"
      >
        {{ mod.label }}
        <span v-if="mod.supportedDbTypes" class="text-xs text-gray-500 ml-2">({{ mod.supportedDbTypes }})</span>
      </div>
    </div>
    <div class="description" style="margin-top:16px;">[DB 리스트] → 워크플로우로 드래그</div>
    <div class="nodes">
      <div
        v-for="db in dbList"
        :key="db.id"
        class="db-node"
        :style="{ background: db.color || '#777' }"
        :draggable="true"
        @dragstart="(e) =>
          onDragStart(e, {
            ...db,
            label: db.dbName,
            type: db.dbType,
            color: db.color || '#777'
          })
        "
      >
        {{ db.dbName }}
        <span style="font-size:11px; margin-left:6px;">({{ db.dbType }})</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 200px;
  min-width: 180px;
  max-width: 230px;
  background: #f8fafc;
  padding: 14px 10px 0 10px;
  border-right: 1px solid #eee;
  height: 100vh;
  overflow-y: auto;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif;
}
.description {
  font-size: 13px;
  margin-bottom: 8px;
  font-weight: 600;
  color: #4576ca;
}
.nodes {
  margin-bottom: 18px;
}
.monitor-module,
.db-node {
  border-radius: 7px;
  color: #fff;
  font-weight: 700;
  cursor: grab;
  padding: 7px 13px;
  margin-bottom: 8px;
  box-shadow: 0 1px 6px 0 #b6c8f74d;
  user-select: none;
  transition: box-shadow .12s;
}
.monitor-module:hover,
.db-node:hover {
  box-shadow: 0 2px 10px 1px #4576ca44;
}
</style>
