import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  getHotStocks, ensureAuth, getCurrentUser,
  getNotifications, getUnreadCount, markNotificationsRead
} from '../api/client.js'

/** 通知类型 → 文案前缀与目标链接 */
const NOTIF_META = {
  LIKE_POST: { icon: '♥', to: (n) => `/post/${n.targetId}` },
  COMMENT: { icon: '💬', to: (n) => `/post/${n.targetId}` },
  REPLY: { icon: '💬', to: (n) => `/post/${n.targetId}` },
  MENTION: { icon: '@', to: (n) => `/post/${n.targetId}` },
  FOLLOW: { icon: '👤', to: (n) => `/user/${n.targetId}` }
}

export default function Header() {
  const [all, setAll] = useState([])
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [me, setMe] = useState(getCurrentUser())
  const [unread, setUnread] = useState(0)
  const [notifs, setNotifs] = useState([])
  const [notifOpen, setNotifOpen] = useState(false)
  const navigate = useNavigate()
  const wrapRef = useRef(null)

  useEffect(() => {
    getHotStocks(50).then(setAll).catch(() => {})
    // 确保演示账号登录态就绪，便于显示「我的」入口与关注交互
    ensureAuth().then(() => {
      setMe(getCurrentUser())
      getUnreadCount().then(setUnread).catch(() => {})
    }).catch(() => {})
    // 未读通知轮询
    const timer = setInterval(() => getUnreadCount().then(setUnread).catch(() => {}), 30000)
    return () => clearInterval(timer)
  }, [])

  const results = query.trim()
    ? all.filter((s) =>
        s.name.includes(query.trim()) ||
        s.symbol.toLowerCase().includes(query.trim().toLowerCase()))
    : []

  const go = (symbol) => {
    navigate(`/stock/${symbol}`)
    setQuery('')
    setOpen(false)
  }

  const toggleNotifs = async () => {
    setNotifOpen((v) => !v)
    if (notifOpen) return
    const list = await getNotifications().catch(() => [])
    setNotifs(Array.isArray(list) ? list : [])
    if (unread > 0) {
      await markNotificationsRead().catch(() => {})
      setUnread(0)
    }
  }

  return (
    <header className="header">
      <Link to="/" className="logo">投资圈</Link>

      <div className="search-wrap" ref={wrapRef}>
        <input
          className="search"
          placeholder="搜索股票、代码"
          value={query}
          onChange={(e) => { setQuery(e.target.value); setOpen(true) }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              if (results[0]) go(results[0].symbol)
              else if (query.trim()) navigate(`/search?q=${encodeURIComponent(query.trim())}`)
            }
          }}
        />
        {open && results.length > 0 && (
          <div className="search-dropdown">
            {results.map((s) => (
              <div
                key={s.symbol}
                className="search-item"
                onMouseDown={() => go(s.symbol)}
              >
                <span className="si-name">{s.name}</span>
                <span className="si-sym">{s.symbol}</span>
                <span className={s.changePercent > 0 ? 'up' : s.changePercent < 0 ? 'down' : ''}>
                  {s.changePercent > 0 ? '+' : ''}{s.changePercent?.toFixed(2)}%
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      <nav className="nav">
        <Link to="/">首页</Link>
        <Link to="/market">行情</Link>
        {me ? (
          <>
            <Link to="/market">自选</Link>
            <Link to={`/user/${me.username}`}>我的</Link>
          </>
        ) : (
          <Link to="/market">自选</Link>
        )}

        <div className="notif-wrap">
          <button className="notif-btn" onClick={toggleNotifs} title="通知">
            🔔
            {unread > 0 && <span className="notif-badge">{unread > 99 ? '99+' : unread}</span>}
          </button>

          {notifOpen && (
            <div className="notif-dropdown">
              <div className="notif-title">通知</div>
              {notifs.length === 0 && <div className="notif-empty">暂无通知</div>}
              {notifs.map((n) => {
                const meta = NOTIF_META[n.type] || { icon: '•', to: () => '/#', }
                return (
                  <Link
                    key={n.id}
                    to={meta.to(n)}
                    className={`notif-item ${n.read ? '' : 'unread'}`}
                    onClick={() => setNotifOpen(false)}
                  >
                    <span className="notif-icon">{meta.icon}</span>
                    <span className="notif-text">
                      <b>{n.actorName}</b> {n.text}
                      <em className="notif-time">
                        {new Date(n.createdAt).toLocaleString('zh-CN')}
                      </em>
                    </span>
                  </Link>
                )
              })}
            </div>
          )}
        </div>
      </nav>
    </header>
  )
}
