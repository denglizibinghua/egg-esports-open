import SockJS from 'sockjs-client'
import Stomp from 'stompjs/lib/stomp.js'

let stompClient = null
const subs = {}

export function connect(token) {
  return new Promise((resolve, reject) => {
    const socket = new SockJS('/ws')
    stompClient = Stomp.over(socket)
    stompClient.connect({ Authorization: `Bearer ${token}` }, resolve, reject)
  })
}

export function subscribe(topic, cb) {
  if (!stompClient || !stompClient.connected) return
  if (subs[topic]) subs[topic].unsubscribe()
  subs[topic] = stompClient.subscribe(topic, msg => cb(JSON.parse(msg.body)))
}

export function unsubscribe(topic) {
  if (subs[topic]) {
    subs[topic].unsubscribe()
    delete subs[topic]
  }
}

export function disconnect() {
  if (stompClient) stompClient.disconnect()
}
