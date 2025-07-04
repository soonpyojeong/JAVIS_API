import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

let stompClient = null

export function connectWebSocket({
  onDbStatusMessage,
  onAlertMessage,
  onDbLiveStatusMessage
}) {
  const socketUrl = import.meta.env.VITE_APP_SOCKET_URL
  const socket = new SockJS(socketUrl)

  // 중복 연결 방지
  if (stompClient?.active) {
    console.warn('[STOMP] 이미 연결되어 있음 → 재연결 방지')
    return
  }

  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,

    onConnect: () => {
      if (!stompClient.connected) {
        console.warn('[STOMP] 연결되지 않은 상태에서 onConnect 호출됨')
        return
      }

      // ✅ /topic/db-status
      if (typeof onDbStatusMessage === 'function') {
        stompClient.subscribe('/topic/db-status', (message) => {
          try {
            const payload = JSON.parse(message.body)
            onDbStatusMessage(payload)
          } catch (e) {
            console.error('[STOMP] DB 상태 메시지 처리 오류:', e)
          }
        })
      }

      // ✅ /topic/alert
      if (typeof onAlertMessage === 'function') {
        stompClient.subscribe('/topic/alert', (message) => {
          try {
            const payload = JSON.parse(message.body)
            if (payload?.message) {
              onAlertMessage(payload)
            } else {
              console.warn('[STOMP] 알림 메시지에 message 필드가 없음:', payload)
            }
          } catch (e) {
            console.error('[STOMP] 알림 메시지 처리 오류:', e)
          }
        })
      }

      // ✅ /topic/db-live-status
      if (typeof onDbLiveStatusMessage === 'function') {
        stompClient.subscribe('/topic/db-live-status', (message) => {
          try {
            const payload = JSON.parse(message.body)
            onDbLiveStatusMessage(payload)
          } catch (e) {
            console.error('[STOMP] DB 라이브 상태 메시지 처리 오류:', e)
          }
        })
      }
    },

    onWebSocketError: (err) => {
      console.error('[STOMP] WebSocket 에러 발생:', err)
    },

    onStompError: (frame) => {
      console.error('[STOMP ERROR]', frame.headers['message'])
      console.error('Details:', frame.body)
    }
  })

  stompClient.activate()
}

export function disconnectWebSocket() {
  try {
    if (stompClient?.active) {
      stompClient.deactivate()
      console.info('[STOMP] 연결 해제 완료')
    }
  } catch (e) {
    console.warn('[STOMP] 연결 해제 중 오류 발생:', e)
  }
}
