import SockJS from 'sockjs-client'
import Stomp from 'stompjs/lib/stomp.js'

let stompClient = null
// 订阅记录：topic -> { subscription, callback }
// 保留 callback 用于断线重连后重新订阅
const subscriptions = {}

// 重连控制：指数退避，最多重连 10 次
let reconnectAttempts = 0
const MAX_RECONNECT = 10
let currentToken = null
let firstConnection = true

/**
 * 连接 WebSocket（带指数退避自动重连）
 * <p>首次连接成功/失败通过 Promise 通知调用方；
 * 后续断线自动重连，重连成功后自动恢复之前的订阅。
 *
 * @param token JWT token
 */
export function connect(token) {
  currentToken = token
  return new Promise((resolve, reject) => {
    const socket = new SockJS('/ws')
    stompClient = Stomp.over(socket)
    stompClient.connect(
      { Authorization: `Bearer ${token}` },
      () => {
        // 连接成功：重置重连计数，恢复之前的订阅
        reconnectAttempts = 0
        if (!firstConnection) {
          console.log('[WS] 重连成功，恢复订阅')
          resubscribeAll()
        }
        firstConnection = false
        resolve()
      },
      () => {
        // 连接失败或断开
        if (reconnectAttempts < MAX_RECONNECT) {
          // 指数退避：1s, 2s, 4s, 8s, 16s, 30s, 30s, 30s, 30s, 30s
          const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000)
          reconnectAttempts++
          console.warn(`[WS] 连接断开，${delay}ms 后重连（第 ${reconnectAttempts}/${MAX_RECONNECT} 次）`)
          setTimeout(() => connect(currentToken).catch(() => {}), delay)
        } else {
          console.error('[WS] 达到最大重连次数，停止重连')
        }
        // 仅首次连接失败才 reject（让调用方知道首次失败）
        if (firstConnection) {
          reject(new Error('WebSocket 连接失败'))
        }
      }
    )
  })
}

/**
 * 订阅主题（如果已存在同名订阅，先取消旧的）
 *
 * @param topic 主题路径
 * @param cb    消息回调（接收 JSON 解析后的对象）
 */
export function subscribe(topic, cb) {
  if (!stompClient || !stompClient.connected) return
  // 已存在同名订阅：先取消旧的
  if (subscriptions[topic]) {
    subscriptions[topic].subscription.unsubscribe()
  }
  const subscription = stompClient.subscribe(topic, msg => cb(JSON.parse(msg.body)))
  subscriptions[topic] = { subscription, callback: cb }
}

/**
 * 取消订阅
 */
export function unsubscribe(topic) {
  if (subscriptions[topic]) {
    subscriptions[topic].subscription.unsubscribe()
    delete subscriptions[topic]
  }
}

/**
 * 断开连接
 */
export function disconnect() {
  if (stompClient) stompClient.disconnect()
  Object.keys(subscriptions).forEach(t => delete subscriptions[t])
}

/**
 * 重连后恢复之前的订阅（用保存的 callback 重新订阅所有 topic）
 */
function resubscribeAll() {
  const topics = Object.keys(subscriptions)
  for (const topic of topics) {
    const cb = subscriptions[topic].callback
    const subscription = stompClient.subscribe(topic, msg => cb(JSON.parse(msg.body)))
    subscriptions[topic] = { subscription, callback: cb }
  }
}
