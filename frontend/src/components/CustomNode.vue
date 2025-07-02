<script setup>
import { ref } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'

const props = defineProps(['data', 'id', 'onDelete', 'onRoleChange'])
const emit = defineEmits(['updateTargetTable'])

const showTableModal = ref(false)
const tempTableName = ref(props.data.targetTable || '')

// 역할 토글(Source <-> Target)
function toggleRole(e) {
  e.stopPropagation()
  let nextRole
  if (!props.data.role || props.data.role === 'source') {
    nextRole = 'target'
    showTableModal.value = true
    tempTableName.value = props.data.targetTable || ''
  } else {
    nextRole = 'source'
  }
  if (props.onRoleChange) props.onRoleChange(props.id, nextRole)
}

// 더블클릭 시(타겟이면) 테이블명 입력 모달 오픈
function onNodeDblClick(e) {
  if (props.data.role === 'target') {
    showTableModal.value = true
    tempTableName.value = props.data.targetTable || ''
  }
}

// 테이블명 저장
function saveTableName() {
  emit('updateTargetTable', props.id, tempTableName.value)
  showTableModal.value = false
}

// 노드 삭제 버튼
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
    @dblclick="onNodeDblClick"
  >
    <!-- 삭제 버튼 -->
    <button class="delete-btn" title="노드 삭제" @click="handleDelete">✕</button>

    <!-- 역할 전환 버튼: DB 노드만 보임 -->
    <div
      class="role-toggle-btn-area"
      v-if="props.data?.isDbType"
      style="margin-bottom:6px; width:100%; display:flex; justify-content:center;"
    >
      <Button
        :label="props.data?.role === 'target' ? 'Target' : 'Source'"
        :severity="props.data?.role === 'target' ? 'danger' : 'success'"
        size="small"
        class="font-bold"
        @click="toggleRole"
        style="min-width:70px;"
      />
    </div>

    <!-- 핸들 영역 -->
    <Handle type="target" :position="Position.Top" id="top" />
    <Handle type="target" :position="Position.Left" id="left" />
    <Handle type="source" :position="Position.Right" id="right" />
    <Handle type="source" :position="Position.Bottom" id="bottom" />

    <!-- 라벨 + 타입 -->
    <div>
      {{ props.data?.label }}
      <span v-if="props.data?.type" style="font-size:11px; margin-left:5px;">
        ({{ props.data.type.toUpperCase() }})
      </span>
    </div>

    <!-- 테이블명 표시 (타겟일 경우만) -->
    <div v-if="props.data.role === 'target' && props.data.targetTable" style="font-size:11px; margin-top:4px;">
      <b>테이블명:</b> {{ props.data.targetTable }}
    </div>

    <!-- 테이블명 입력 다이얼로그 -->
    <Dialog v-model:visible="showTableModal" header="타겟 테이블명 지정" modal>
      <InputText v-model="tempTableName" placeholder="테이블명" />
      <template #footer>
        <Button label="저장" @click="saveTableName" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.custom-node {
  min-width: 100px;
  padding: 8px 8px 5px 8px;
  border: 1.5px solid #bbb;
  border-radius: 7px;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-weight: 600;
  position: relative;
  box-shadow: 0 2px 8px #0001;
  transition: box-shadow 0.13s ease, filter 0.13s ease, transform 0.12s cubic-bezier(.23,1,.32,1);
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
