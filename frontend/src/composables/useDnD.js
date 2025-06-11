// useDnD.js
import { useVueFlow } from '@vue-flow/core'
import { ref } from 'vue'

let nodeIdSeq = 1

export default function useDragAndDrop({ nodes }) {
  const draggedItem = ref(null)
  const isDragOver = ref(false)
  const { addNodes, screenToFlowCoordinate, onNodesInitialized, updateNode } = useVueFlow()

  function onDragStart(event, item) {
    draggedItem.value = item
    if (event.dataTransfer) {
      event.dataTransfer.setData('application/vueflow', item.id)
      event.dataTransfer.effectAllowed = 'move'
    }
  }

  function onDragOver(event) {
    event.preventDefault()
    isDragOver.value = true
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move'
    }
  }

  function onDragLeave() {
    isDragOver.value = false
  }

  function onDrop(event) {
    isDragOver.value = false
    if (!draggedItem.value) return

    const position = screenToFlowCoordinate({
      x: event.clientX,
      y: event.clientY,
    })

    const nodeId = `${draggedItem.value.isModule ? 'mod' : 'db'}_${nodeIdSeq++}`

    const newNode = {
      id: nodeId,
      type: 'custom',
      label: draggedItem.value.label || draggedItem.value.dbName,
      data: { ...draggedItem.value },
      position,
      style: {
        background: draggedItem.value.color || '#6366f1',
        color: '#fff',
        borderRadius: '7px',
        width: '120px',
        minHeight: '30px',
        fontSize: '13px',
        fontWeight: 700,
        boxShadow: '0 2px 6px #0001',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '0 6px',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        textOverflow: 'ellipsis',
      }
    }

    addNodes(newNode)
    const { off } = onNodesInitialized(() => {
      updateNode(nodeId, node => ({
        position: {
          x: node.position.x - (node.dimensions?.width ?? 0) / 2,
          y: node.position.y - (node.dimensions?.height ?? 0) / 2
        }
      }))
      off()
    })
    draggedItem.value = null
  }

  return { onDragStart, onDragOver, onDragLeave, onDrop, isDragOver }
}
