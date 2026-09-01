import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * 实时行情 WebSocket 客户端（SockJS + STOMP）。
 *
 * 后端（Spring Boot）在 /ws 暴露 SockJS 端点，并每 5 秒向以下主题推送：
 *   /topic/quotes   Map<symbol, QuoteDTO>   批量个股实时行情
 *   /topic/indices  List<IndexDTO>          市场指数实时行情
 * 本模块维护一条长连接（自动重连），组件通过 subscribeQuote() 订阅主题即可拿到
 * 实时数据，替代前端的轮询，与雪球「实时推送」体验一致。
 *
 * 注意：后端未启动 / 握手失败时，订阅静默无数据，组件应保留 REST 初始加载作为兜底，
 * 因此「连接不可达」不会破坏页面，只少了实时跳动。
 */

let client = null
let status = 'idle' // idle | connecting | open | closed
const topicSubs = new Map() // topic -> Set<callback>
const statusSubs = new Set() // Set<callback(status)>

function setStatus(s) {
  if (status === s) return
  status = s
  statusSubs.forEach((cb) => cb(s))
}

function buildClient() {
  const c = new Client({
    // SockJS 在原生 WebSocket 不可用时自动降级为 HTTP 流/轮询
    webSocketFactory: () => new SockJS('/ws'),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000
  })

  c.onConnect = () => {
    setStatus('open')
    // 重连或首次连接：订阅所有已注册主题
    topicSubs.forEach((set, topic) => {
      c.subscribe(topic, (msg) => {
        let payload
        try {
          payload = JSON.parse(msg.body)
        } catch {
          payload = msg.body
        }
        set.forEach((cb) => cb(payload))
      })
    })
  }

  c.onStompError = (frame) => {
    console.warn('[ws] STOMP 错误：', frame.headers['message'], frame.body)
  }

  c.onWebSocketClose = () => setStatus('closed')
  c.onWebSocketError = () => setStatus('closed')

  return c
}

function ensureClient() {
  if (!client) client = buildClient()
  if (!client.active) {
    setStatus('connecting')
    client.activate()
  }
  return client
}

/**
 * 订阅某个行情主题。
 * @param {string} topic 例如 '/topic/quotes' 或 '/topic/indices'
 * @param {(payload:any)=>void} cb 收到实时数据回调
 * @returns {()=>void} 取消订阅函数（组件卸载时调用）
 */
export function subscribeQuote(topic, cb) {
  if (!topicSubs.has(topic)) topicSubs.set(topic, new Set())
  topicSubs.get(topic).add(cb)

  const c = ensureClient()
  // 若连接已就绪，立即订阅（否则等 onConnect 统一补订）
  if (c.connected) {
    c.subscribe(topic, (msg) => {
      let payload
      try {
        payload = JSON.parse(msg.body)
      } catch {
        payload = msg.body
      }
      cb(payload)
    })
  }

  return () => {
    const set = topicSubs.get(topic)
    if (set) {
      set.delete(cb)
      if (set.size === 0) topicSubs.delete(topic)
    }
  }
}

/**
 * 监听连接状态变化。
 * @param {(status:'idle'|'connecting'|'open'|'closed')=>void} cb
 * @returns {()=>void} 取消监听
 */
export function onSocketStatus(cb) {
  statusSubs.add(cb)
  cb(status)
  return () => statusSubs.delete(cb)
}
