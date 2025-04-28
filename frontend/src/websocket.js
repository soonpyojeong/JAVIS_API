import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

let stompClient = null;

export function connectWebSocket({ onDbStatusMessage, onAlertMessage }) {
  const socketUrl = process.env.VUE_APP_SOCKET_URL; // ✅ 환경변수로 분기
  const socket = new SockJS(socketUrl);

  stompClient = new Client({
    webSocketFactory: () => socket,
    debug: (str) => {
      console.log('[STOMP DEBUG]', str); // ✅ 디버깅 로그 출력
    },
    reconnectDelay: 5000,
    onConnect: () => {
      //console.log('[STOMP] 연결 성공');

      if (typeof onDbStatusMessage === 'function') {
        stompClient.subscribe('/topic/db-status', (message) => {
          try {
            const payload = JSON.parse(message.body);
            onDbStatusMessage(payload);
          } catch (e) {
            console.error('[STOMP] DB 상태 메시지 처리 오류:', e);
          }
        });
      }

if (typeof onAlertMessage === 'function') {
  stompClient.subscribe('/topic/alert', (message) => {
    try {
      const payload = JSON.parse(message.body);

      // 📦 콘솔 디버깅: 실제 payload 확인
      //console.log('[STOMP] 수신된 alert payload:', payload);

      // ✅ message 필드가 있는 경우만 전달
      if (payload && payload.message) {
        onAlertMessage(payload);
      } else {
        console.warn('[STOMP] 알림 메시지에 message 필드가 없습니다:', payload);
      }

    } catch (e) {
      console.error('[STOMP] 알림 메시지 처리 오류:', e);
    }
  });
}


    },
    onStompError: (frame) => {
      console.error('[STOMP ERROR]', frame.headers['message']);
      console.error('Details:', frame.body);
    },
  });

  stompClient.activate();
}

export function disconnectWebSocket() {
  if (stompClient && stompClient.active) {
    stompClient.deactivate();
  }
}