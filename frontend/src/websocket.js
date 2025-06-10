import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

let stompClient = null;

export function connectWebSocket({ onDbStatusMessage, onAlertMessage }) {
  // Vite 방식 환경변수로 변경!
  const socketUrl = import.meta.env.VITE_APP_SOCKET_URL;
  const socket = new SockJS(socketUrl);

  stompClient = new Client({
    webSocketFactory: () => socket,
    debug: (str) => {
      console.log('[STOMP DEBUG]', str);
    },
    reconnectDelay: 5000,
    onConnect: () => {
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
