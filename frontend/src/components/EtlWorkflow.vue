// EtlWorkflow.vue
<script setup>
import { ref, onMounted, markRaw,h } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import CustomNode from './CustomNode.vue'
import CustomEdge from './CustomEdge.vue'
import api from '@/api'

// 관제 모듈 하드코딩 예시
const moduleList = ref([
  {
    id: 'health',
    label: 'HealthCheck',
    supportedDbTypes: 'ORACLE,TIBERO',
    queries: {
      ORACLE: 'SELECT * FROM DUAL',
      TIBERO: 'SELECT 1 FROM DUAL',
    },
    color: '#ff6363'
  },
  {
    id: 'session',
    label: 'Session',
    supportedDbTypes: 'ORACLE,MYSQL',
    queries: {
      ORACLE: 'SELECT COUNT(*) FROM V$SESSION',
      MYSQL: 'SELECT COUNT(*) FROM information_schema.processlist',
    },
    color: '#2196f3'
  },
  {
    id: 'threshold',
    label: 'Threshold',
    supportedDbTypes: 'ORACLE,TIBERO,MYSQL',
    queries: {
      ORACLE: 'SELECT * FROM TB_THRESHOLD',
      TIBERO: 'SELECT * FROM TB_THRESHOLD',
      MYSQL: 'SELECT * FROM TB_THRESHOLD',
    },
    color: '#fbc02d'
  },
])

// DB 리스트 API 불러오기
const dbList = ref([])
const loadDbList = async () => {
  const { data } = await api.get('/api/db-connection')
  dbList.value = data
}
onMounted(loadDbList)

// 워크플로우 상태
const { onConnect, addEdges, project } = useVueFlow()
const nodes = ref([])
const edges = ref([])

// 드래그 이벤트
function onDragStart(e, data) {
  e.dataTransfer.setData('application/node', JSON.stringify(data))
}
const handleDrop = (e) => {
  const data = JSON.parse(e.dataTransfer.getData('application/node'))
  const bounds = e.target.getBoundingClientRect()
  const position = project({
    x: e.clientX - bounds.left,
    y: e.clientY - bounds.top
  })
  nodes.value.push({
    id: `n_${Date.now()}`,
    type: 'custom',
    label: data.label,
    position,
    data
  })
}
const handleDragOver = (e) => e.preventDefault()

function handleNodeDelete(nodeId) {
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  // 연결된 edges도 같이 삭제하려면:
  edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId)
}

onConnect((params) => {
  addEdges([{ ...params, type: 'custom' }])
})
</script>

<template>
  <div class="main-layout">
    <!-- Sidebar 통합: 모듈 & DB -->
    <aside class="sidebar">
      <div class="description">[관제 모듈] → 워크플로우로 드래그</div>
      <div class="nodes">
        <div
          v-for="mod in moduleList"
          :key="mod.id"
          class="monitor-module"
          :style="{ background: mod.color }"
          :draggable="true"
          @dragstart="(e) => onDragStart(e, { ...mod, isModule: true })"
        >
          {{ mod.label }}
          <span v-if="mod.supportedDbTypes" class="text-xs text-gray-100 ml-2">
            ({{ mod.supportedDbTypes }})
          </span>
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
              label: db.description || db.label,
              type: db.dbType || db.type,
              color: db.color || '#777'
            })
          "
        >
          {{ db.dbName || db.label }}
          <span style="font-size:11px; margin-left:6px;">({{ db.dbType || db.type }})</span>
        </div>
      </div>
    </aside>

    <!-- Workflow Canvas -->
    <div class="workflow-canvas" @drop="handleDrop" @dragover="handleDragOver">
      <VueFlow
        v-model:nodes="nodes"
        v-model:edges="edges"
        fit-view-on-init
        :default-zoom="1.2"
        :min-zoom="0.2"
        :max-zoom="4"
        :node-types="{
        custom: markRaw((props) => h(CustomNode, {
          ...props,
          onDelete: handleNodeDelete
        }))
        }"
        :edge-types="{ custom: markRaw(CustomEdge) }"
      >
        <Background pattern-color="#aaa" :gap="8" />
        <MiniMap />
        <Controls />
      </VueFlow>
    </div>
  </div>
</template>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  background: #eaf1fa;
}
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
  font-weight: 600;
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
.workflow-canvas {
  flex: 1 1 0%;
  height: 80vh;
  background: #f2f6fc;
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
