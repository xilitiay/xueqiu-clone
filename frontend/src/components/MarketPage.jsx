import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { getIndices, getQuotes, getHotStocks } from '../api/client.js'
import { subscribeQuote, onSocketStatus } from '../api/wsClient.js'
import { fmtChange } from './format.js'

/** 自选股列表：接入实时行情，自动刷新 + 手动刷新 */
const WATCHLIST = ['SH600519', 'SZ300750', 'SZ002594', 'SH601318', 'SH600036', 'HK00700', 'AAPL', 'NVDA']
const REFRESH_MS = 15000

function fmt(n, digits = 2) {
  if (n == null) return '—'
  const v = typeof n === 'string' ? parseFloat(n) : n
  return isNaN(v) ? '—' : v.toFixed(digits)
}

export default function MarketPage() {
  const [indices, setIndices] = useState([])
  const [quotes, setQuotes] = useState({})
  const [hot, setHot] = useState([])
  const [live, setLive] = useState(true)
  const [updating, setUpdating] = useState(false)
  const [wsOpen, setWsOpen] = useState(false)

  const refresh = useCallback(() => {
    setUpdating(true)
    getQuotes(WATCHLIST)
      .then(setQuotes)
      .catch(() => {})
      .finally(() => setUpdating(false))
  }, [])

  // 实时推送连接状态（用于指示「已连上」或「后端未启动，走轮询」）
  useEffect(() => onSocketStatus((s) => setWsOpen(s === 'open')), [])

  useEffect(() => {
    getIndices().then(setIndices).catch(() => {})
    getHotStocks(8).then(setHot).catch(() => {})
    refresh()

    const unsubs = []
    if (live) {
      // REST 轮询兜底（覆盖全部自选股，含后端未推送的标的）
      const timer = setInterval(refresh, REFRESH_MS)
      unsubs.push(() => clearInterval(timer))

      // WebSocket 实时推送：指数与热点个股即时刷新
      unsubs.push(subscribeQuote('/topic/indices', (list) => {
        if (Array.isArray(list) && list.length) setIndices(list)
      }))
      unsubs.push(subscribeQuote('/topic/quotes', (map) => {
        if (map && typeof map === 'object') {
          setQuotes((prev) => ({ ...prev, ...map }))
        }
      }))
    }
    return () => unsubs.forEach((f) => f())
  }, [refresh, live])

  const rows = WATCHLIST.map((sym) => quotes[sym]).filter(Boolean)
  const liveCls = (q) => (q.changePercent > 0 ? 'up' : q.changePercent < 0 ? 'down' : '')

  return (
    <div style={{ gridColumn: '1 / -1' }}>
      <div className="card market-head">
        <h2 style={{ margin: 0 }}>行情中心</h2>
        <div className="live-ctrl">
          <span className={`live-dot ${live ? 'on' : ''}`} />
          <span className="meta">
            {live ? (wsOpen ? '实时推送已连接' : '轮询中（后端未启动）') : '已暂停'}
          </span>
          <button className="mini-btn" onClick={() => setLive((v) => !v)}>{live ? '暂停' : '开启'}</button>
          <button className="mini-btn" onClick={refresh} disabled={updating}>{updating ? '刷新中' : '手动刷新'}</button>
        </div>
      </div>

      {/* 指数条 */}
      {indices.length > 0 && (
        <div className="card index-strip">
          {indices.map((i) => {
            const cls = i.changePercent > 0 ? 'up' : i.changePercent < 0 ? 'down' : ''
            const code = i.code || i.symbol
            const value = i.value != null ? i.value : i.price
            return (
              <div className="index-cell" key={code}>
                <div className="name">{i.name}</div>
                <div className={`val ${cls}`}>{value}</div>
                <div className={`chg ${cls}`}>{fmtChange(i.changePercent)}</div>
              </div>
            )
          })}
        </div>
      )}

      {/* 自选股实时报价表 */}
      <div className="card">
        <h3>自选股 · 实时报价</h3>
        <div className="quote-table">
          <div className="qrow qhead">
            <span>名称</span><span>最新</span><span>涨跌幅</span>
            <span className="hide-sm">开</span><span className="hide-sm">高</span><span className="hide-sm">低</span>
            <span className="hide-sm">换手</span><span className="hide-sm">市盈率</span>
          </div>
          {rows.length === 0 && <div className="meta" style={{ padding: 12 }}>行情加载中…</div>}
          {rows.map((q) => (
            <Link to={`/stock/${q.symbol}`} className="qrow" key={q.symbol}>
              <span className="qname">{q.name}<em className="qsym">{q.symbol}</em></span>
              <span className={liveCls(q)}>{fmt(q.price)}</span>
              <span className={liveCls(q)}>{fmtChange(q.changePercent)}</span>
              <span className="hide-sm">{fmt(q.open)}</span>
              <span className="hide-sm">{fmt(q.high)}</span>
              <span className="hide-sm">{fmt(q.low)}</span>
              <span className="hide-sm">{fmt(q.turnoverRate)}%</span>
              <span className="hide-sm">{fmt(q.pe)}</span>
            </Link>
          ))}
        </div>
        <div className="meta" style={{ fontSize: 12, marginTop: 8 }}>
          行情来自公开接口（腾讯 gtimg），交易时段实时；后端未启动时回退示例数据。不构成投资建议。
        </div>
      </div>

      {/* 热门股票 */}
      <div className="card">
        <h3>🔥 热门股票</h3>
        {hot.map((s) => (
          <Link to={`/stock/${s.symbol}`} className="stock-row" key={s.symbol}>
            <div>
              <div className="name">{s.name}</div>
              <div className="sym">{s.symbol}</div>
            </div>
            <div className="price">
              <div>{s.price}</div>
              <div className={s.changePercent > 0 ? 'up' : s.changePercent < 0 ? 'down' : ''}>{fmtChange(s.changePercent)}</div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
