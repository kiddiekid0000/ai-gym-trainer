// src/features/workouts/services/websocketService.ts
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WEBSOCKET_URL } from '../../../config/urlConfig';

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: { [key: string]: any } = {};

  connect(onConnect?: () => void, onError?: (error: any) => void) {
    this.client = new Client({
      webSocketFactory: () => new SockJS(`${window.location.protocol}//${window.location.host}${WEBSOCKET_URL}`),
      connectHeaders: {
        // Add auth headers if needed
      },
      debug: (str) => {
        console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = () => {
      console.log('Connected to WebSocket');
      if (onConnect) onConnect();
    };

    this.client.onStompError = (frame) => {
      console.error('STOMP error', frame.headers['message']);
      if (onError) onError(frame);
    };

    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }

  subscribe(destination: string, callback: (message: any) => void) {
    if (this.client && this.client.connected) {
      const subscription = this.client.subscribe(destination, (message) => {
        const body = JSON.parse(message.body);
        callback(body);
      });
      this.subscriptions[destination] = subscription;
      return subscription;
    }
    return null;
  }

  unsubscribe(destination: string) {
    if (this.subscriptions[destination]) {
      this.subscriptions[destination].unsubscribe();
      delete this.subscriptions[destination];
    }
  }

  send(destination: string, body: any) {
    if (this.client && this.client.connected) {
      this.client.publish({
        destination,
        body: JSON.stringify(body),
      });
    }
  }
}

const websocketService = new WebSocketService();
export default websocketService;