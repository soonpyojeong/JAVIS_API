import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

let stompClient = null

// 핸들러 저장용
let dbStatusHandler = null
let alertHandler = null
let dbLiveStatusHandler = null

export function connectWebSocket(handlers = {}) {
  const socketUrl = import.meta.env.VITE_APP_SOCKET_URL
  //console.log('[STOMP] WebSocket 연결 시작:', socketUrl)

  // 핸들러 갱신
  dbStatusHandler = handlers.onDbStatusMessage || dbStatusHandler
  alertHandler = handlers.onAlertMessage || alertHandler
  dbLiveStatusHandler = handlers.onDbLiveStatusMessage || dbLiveStatusHandler

  // 이미 연결된 경우 → 구독만 갱신
  if (stompClient?.connected) {
    //console.warn('[STOMP] 이미 연결됨 → 구독 재등록')
    subscribeAll()
    return
  }

  const socket = new SockJS(socketUrl)

  stompClient = new Client({
    webSocketFactory: () => socket,
    reconnectDelay: 5000,

    onConnect: () => {
      //console.log('[✅ STOMP 연결 완료]')
      subscribeAll()
    },

    onWebSocketError: (err) => {
      console.error('[❌ STOMP] WebSocket 에러 발생:', err)
    },

    onStompError: (frame) => {
      console.error('[❌ STOMP ERROR]', frame.headers['message'])
      console.error('Details:', frame.body)
    }
  })

  //console.log('[STOMP] stompClient.activate() 호출됨')
  stompClient.activate()
}

function subscribeAll() {
  if (!stompClient || !stompClient.connected) {
    console.warn('[STOMP] 연결되지 않은 상태에서 구독 시도')
    return
  }

  // ✅ /topic/db-status
  if (typeof dbStatusHandler === 'function') {
    //console.log('[STOMP] /topic/db-status 구독 시작')
    stompClient.subscribe('/topic/db-status', (message) => {
      try {
        const payload = JSON.parse(message.body)
        //console.log('[📡 db-status 수신]:', payload)
        dbStatusHandler(payload)
      } catch (e) {
        console.error('[STOMP] DB 상태 메시지 처리 오류:', e)
      }
    })
  }

  // ✅ /topic/alert
  if (typeof alertHandler === 'function') {
   // console.log('[STOMP] /topic/alert 구독 시작')
    stompClient.subscribe('/topic/alert', (message) => {
      try {
        const payload = JSON.parse(message.body)
       // console.log('[📡 alert 수신]:', payload)
        alertHandler(payload)
      } catch (e) {
        console.error('[STOMP] 알림 메시지 처리 오류:', e)
      }
    })
  }

  // ✅ /topic/db-live-status
  if (typeof dbLiveStatusHandler === 'function') {
   // console.log('[STOMP] /topic/db-live-status 구독 시작')
    stompClient.subscribe('/topic/db-live-status', (message) => {
      //console.log('[📥 수신된 raw 메시지 body]:', message.body)
      try {
        const payload = JSON.parse(message.body)
        //console.log('[📡 파싱된 db-live-status 수신]:', payload)
        dbLiveStatusHandler(payload)
      } catch (e) {
        console.error('[STOMP] DB 라이브 상태 메시지 처리 오류:', e)
        console.warn('[STOMP] 파싱 실패한 메시지 내용:', message.body)
      }
    })
    //console.log('[STOMP] /topic/db-live-status 구독 완료')
  }
}

export function disconnectWebSocket() {
  try {
    if (stompClient?.active) {
      stompClient.deactivate()
     // console.info('[STOMP] 연결 해제 완료')
    }
  } catch (e) {
    console.warn('[STOMP] 연결 해제 중 오류 발생:', e)
  }
}
