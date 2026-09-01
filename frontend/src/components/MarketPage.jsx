import { useState, useEffect, useCallback } from 'react'
import { Link } from 'react-router-dom'
import {
  getIndices, getQuotes, getHotStocks, getWatchlist, toggleWatchlist,
  getPositions, savePosition, removePosition
} from '../api/client.js'
import { subscribeQuote, onSocketStatus } from '../api/wsClient.js'
import { fmtChange } from './format.js'

const REFRESH_MS = 15000

function fmt(n, digits = 2) {
  if (n == null) return '—'
  const v = typeof n === 'string' ? parseFloat(n) : Number(n)
  return isNaN(v) ? '—' : v.toFixed(digits)
}
const clsOf = (v) => (v > 0 ? 'up' : v < 0 ? 'down' : '')

export default function MarketPage() {
  const [tab, setTab] = useState('watch') // watch | positions
  const [indices, setIndices] = useState([])
  const [watch, setWatch] = useState([])
  const [positions, setPositions] = useState([])
  const [hot, setHot] = useState([])
  const [live, setLive] = useState(true)
  const [updating, setUpdating] = useState(false)
  const [wsOpen, setWsOpen] = useState(false)

  const [addSym, setAddSym] = useState('')
  const [posForm, setPosForm] = useState({ symbol: '', shares: '', avgCost: '' })
  const [editing, setEditing] = useState(null)

  const loadWatch = useCallback(() => {
    getWatchlist().then(setWatch).catch(() => {})
  }, [])
  const loadPositions = useCallback(() => {
    getPositions().then(setPositions).catch(() => {})
  }, [])

  // 实时推送连接状态
  useEffect(() => onSocketStatus((s) => setWsOpen(s === 'open')), [])

  useEffect(() => {
    getIndices().then(setIndices).catch(() => {})
    getHotStocks(8).then(setHot).catch(() => {})
    loadWatch()
    loadPositions()

    const unsubs = []
    if (live) {
      const timer = setInterval(() => {
        setUpdating(true)
        // 同时刷新自选行情（实时推送覆盖到的标的由 WS 补充）
        getWatchlist().then(setWatch).catch(() => {})
        getPositions().then(setPositions).catch(() => {})
        getHotStocks(8).then(setHot).catch(() => {})
        setUpdating(false)
      }, REFRESH_MS)
      unsubs.push(() => clearInterval(timer))

      unsubs.push(subscribeQuote('/topic/indices', (list) => {
        if (Array.isArray(list) && list.length) setIndices(list)
      }))
      unsubs.push(subscribeQuote('/topic/quotes', (map) => {
        if (!map || typeof map !== 'object') return
        setWatch((prev) => prev.map((q) => (map[q.symbol] ? { ...q, ...map[q.symbol] } : q)))
        setPositions((prev) => prev.map((p) => {
          const q = map[p.symbol]
          if (!q || q.price == null) return p
          const price = Number(q.price), sh = Number(p.shares), cost = Number(p.avgCost)
          const costValue = cost * sh
          const marketValue = price * sh
          const pnl = marketValue - costValue
          return {
            ...p, price: q.price, changePercent: q.changePercent,
            marketValue, costValue, pnl,
            pnlPercent: costValue ? (pnl * 100 / costValue) : 0
          }
        }))
      }))
    }
    return () => unsubs.forEach((f) => f())
  }, [live, loadWatch, loadPositions])

  // 添加自选
  const onAddWatch = async (e) => {
    e.preventDefault()
    const sym = addSym.trim().toUpperCase()
    if (!sym) return
    await toggleWatchlist(sym).catch(() => {})
    setAddSym('')
    loadWatch()
  }
  const onRemoveWatch = async (sym) => {
    await toggleWatchlist(sym).catch(() => {})
    loadWatch()
  }

  // 持仓保存 / 更新
  const onSavePos = async (e) => {
    e.preventDefault()
    const symbol = posForm.symbol.trim().toUpperCase()
    const shares = parseFloat(posForm.shares)
    const avgCost = parseFloat(posForm.avgCost)
    if (!symbol || isNaN(shares) || isNaN(avgCost)) return
    await savePosition({ symbol, shares, avgCost }).catch(() => {})
    setPosForm({ symbol: '', shares: '', avgCost: '' })
    setEditing(null)
    loadPositions()
  }
  const onEditPos = (p) => {
    setEditing(p.symbol)
    setPosForm({ symbol: p.symbol, shares: String(p.shares), avgCost: String(p.avgCost) })
  }
  const onRemovePos = async (sym) => {
    await removePosition(sym).catch(() => {})
    loadPositions()
  }

  const totalPnl = positions.reduce((s, p) => s + (Number(p.pnl) || 0), 0)
  const totalCost = positions.reduce((s, p) => s + (Number(p.costValue) || 0), 0)
  const totalMV = positions.reduce((s, p) => s + (Number(p.marketValue) || 0), 0)
  const totalPct = totalCost ? (totalPnl * 100 / totalCost) : 0

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
        </div>
      </div>

      {indices.length > 0 && (
        <div className="card index-strip">
          {indices.map((i) => {
            const cls = clsOf(i.changePercent)
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

      {/* Tab 切换 */}
      <div className="market-tabs">
        <button className={`mtab ${tab === 'watch' ? 'active' : ''}`} onClick={() => setTab('watch')}>自选股</button>
        <button className={`mtab ${tab === 'positions' ? 'active' : ''}`} onClick={() => setTab('positions')}>
          持仓{positions.length ? `（${positions.length}）` : ''}
        </button>
      </div>

      {tab === 'watch' && (
        <div className="card">
          <h3>自选股 · 实时报价</h3>
          <form className="add-row" onSubmit={onAddWatch}>
            <input
              className="mini-input"
              placeholder="输入代码加自选，如 SH600519 / AAPL"
              value={addSym}
              onChange={(e) => setAddSym(e.target.value)}
            />
            <button className="mini-btn" type="submit">+ 加自选</button>
          </form>
          <div className="quote-table">
            <div className="qrow qhead">
              <span>名称</span><span>最新</span><span>涨跌幅</span>
              <span className="hide-sm">开</span><span className="hide-sm">高</span><span className="hide-sm">低</span>
              <span className="hide-sm">市盈率</span><span></span>
            </div>
            {watch.length === 0 && <div className="meta" style={{ padding: 12 }}>暂无自选，输入代码或在下方热门里「+自选」</div>}
            {watch.map((q) => (
              <div className="qrow" key={q.symbol}>
                <Link to={`/stock/${q.symbol}`} className="qname" style={{ flex: 1 }}>
                  {q.name}<em className="qsym">{q.symbol}</em>
                </Link>
                <span className={clsOf(q.changePercent)}>{fmt(q.price)}</span>
                <span className={clsOf(q.changePercent)}>{fmtChange(q.changePercent)}</span>
                <span className="hide-sm">{fmt(q.open)}</span>
                <span className="hide-sm">{fmt(q.high)}</span>
                <span className="hide-sm">{fmt(q.low)}</span>
                <span className="hide-sm">{fmt(q.pe)}</span>
                <button className="x-btn" title="移除自选" onClick={() => onRemoveWatch(q.symbol)}>×</button>
              </div>
            ))}
          </div>

          <h3 style={{ marginTop: 18 }}>🔥 热门股票</h3>
          {hot.map((s) => (
            <div className="stock-row" key={s.symbol}>
              <Link to={`/stock/${s.symbol}`} style={{ flex: 1 }}>
                <div className="name">{s.name}</div>
                <div className="sym">{s.symbol}</div>
              </Link>
              <div className="price">
                <div>{s.price}</div>
                <div className={clsOf(s.changePercent)}>{fmtChange(s.changePercent)}</div>
              </div>
              <button className="mini-btn" onClick={() => toggleWatchlist(s.symbol).then(loadWatch).catch(() => {})}>
                + 自选
              </button>
            </div>
          ))}
        </div>
      )}

      {tab === 'positions' && (
        <div className="card">
          <h3>我的持仓</h3>
          <div className="pos-summary">
            <span>总成本 <b>{fmt(totalCost)}</b></span>
            <span>市值 <b>{fmt(totalMV)}</b></span>
            <span className={clsOf(totalPnl)}>
              浮动盈亏 <b>{fmt(totalPnl)}（{fmt(totalPct)}%）</b>
            </span>
          </div>

          <form className="add-row" onSubmit={onSavePos}>
            <input className="mini-input" placeholder="代码" style={{ maxWidth: 120 }}
              value={posForm.symbol} disabled={!!editing}
              onChange={(e) => setPosForm({ ...posForm, symbol: e.target.value })} />
            <input className="mini-input" placeholder="份额" style={{ maxWidth: 100 }}
              value={posForm.shares}
              onChange={(e) => setPosForm({ ...posForm, shares: e.target.value })} />
            <input className="mini-input" placeholder="成本价" style={{ maxWidth: 100 }}
              value={posForm.avgCost}
              onChange={(e) => setPosForm({ ...posForm, avgCost: e.target.value })} />
            <button className="mini-btn" type="submit">{editing ? '更新' : '添加持仓'}</button>
            {editing && <button className="mini-btn" type="button" onClick={() => { setEditing(null); setPosForm({ symbol: '', shares: '', avgCost: '' }) }}>取消</button>}
          </form>

          <div className="pos-table">
            <div className="prow phead">
              <span>名称</span><span>份额</span><span>成本价</span><span>现价</span>
              <span>市值</span><span>盈亏</span><span>盈亏%</span><span></span>
            </div>
            {positions.length === 0 && <div className="meta" style={{ padding: 12 }}>暂无持仓，填写上方表单添加</div>}
            {positions.map((p) => (
              <div className="prow" key={p.symbol}>
                <Link to={`/stock/${p.symbol}`} className="pname">{p.name}<em className="qsym">{p.symbol}</em></Link>
                <span>{fmt(p.shares)}</span>
                <span>{fmt(p.avgCost)}</span>
                <span className={clsOf(p.changePercent)}>{fmt(p.price)}</span>
                <span>{fmt(p.marketValue)}</span>
                <span className={clsOf(p.pnl)}>{fmt(p.pnl)}</span>
                <span className={clsOf(p.pnlPercent)}>{fmt(p.pnlPercent)}%</span>
                <span className="pos-ops">
                  <button className="mini-btn" onClick={() => onEditPos(p)}>编辑</button>
                  <button className="x-btn" title="删除" onClick={() => onRemovePos(p.symbol)}>×</button>
                </span>
              </div>
            ))}
          </div>
          <div className="meta" style={{ fontSize: 12, marginTop: 8 }}>
            行情来自公开接口（腾讯 gtimg），交易时段实时；后端未启动时回退示例数据，不构成投资建议。
          </div>
        </div>
      )}
    </div>
  )
}
