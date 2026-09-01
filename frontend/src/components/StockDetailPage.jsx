import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getStock, getStockPosts, getQuote } from '../api/client.js'
import { subscribeQuote } from '../api/wsClient.js'
import PostCard from './PostCard.jsx'
import Sparkline from './Sparkline.jsx'
import { fmtChange } from './format.js'

export default function StockDetailPage() {
  const { symbol } = useParams()
  const [stock, setStock] = useState(null)
  const [quote, setQuote] = useState(null)
  const [posts, setPosts] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let alive = true
    let unsub = () => {}
    Promise.all([getStock(symbol), getStockPosts(symbol, 0, 10)]).then(([s, p]) => {
      if (!alive) return
      setStock(s)
      setPosts(p)
      setLoading(false)
    })

    // 拉取实时行情刷新价格（后端未启动则回退示例数据）
    getQuote(symbol).then((q) => {
      if (!alive) return
      setQuote(q)
      setStock((prev) => prev ? { ...prev, price: q.price, changePercent: q.changePercent } : prev)
    }).catch(() => {})

    // 实时推送：若该标的在后端推送清单内，价格即时跳动
    unsub = subscribeQuote('/topic/quotes', (map) => {
      if (!alive || !map) return
      const q = map[symbol]
      if (q) {
        setQuote(q)
        setStock((prev) => prev ? { ...prev, price: q.price, changePercent: q.changePercent } : prev)
      }
    })

    return () => { alive = false; unsub() }
  }, [symbol])

  if (loading) return (
    <div className="card skeleton-card" style={{ gridColumn: '1 / -1' }}>
      <div className="stock-hero">
        <div style={{ flex: 1 }}>
          <div className="skeleton-bar" style={{ width: '45%', height: 22 }} />
          <div className="skeleton-bar" style={{ width: '60%', marginTop: 8 }} />
        </div>
        <div className="skeleton-bar" style={{ width: 90, height: 28 }} />
      </div>
      <div className="skeleton-bar" style={{ width: '100%', height: 120, marginTop: 16 }} />
    </div>
  )
  if (!stock) return <div className="card" style={{ gridColumn: '1 / -1' }}>未找到该股票</div>

  const pct = stock.changePercent
  const cls = pct > 0 ? 'up' : pct < 0 ? 'down' : ''
  const up = (stock.kline?.[stock.kline.length - 1] ?? 0) >= (stock.kline?.[0] ?? 0)

  const cell = (label, val) => (
    <div className="qcell">
      <div className="qlabel">{label}</div>
      <div className="qval">{val == null || val === '' ? '—' : val}</div>
    </div>
  )

  return (
    <div style={{ gridColumn: '1 / -1' }}>
      <Link to="/" className="back">← 返回首页</Link>

      <div className="card">
        <div className="stock-hero">
          <div>
            <h2 style={{ margin: '0 0 2px' }}>{stock.name}</h2>
            <div className="meta">{stock.symbol} · {stock.market} · {stock.industry}</div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div className={`big-price ${cls}`}>{stock.price}</div>
            <div className={`big-chg ${cls}`}>{fmtChange(pct)}</div>
            {quote?.time && <div className="meta" style={{ fontSize: 11 }}>行情时间 {quote.time}</div>}
          </div>
        </div>

        <div className="kline">
          <Sparkline data={stock.kline} width={900} height={120} fluid />
          <div className="meta" style={{ marginTop: 4 }}>
            近 30 日走势（{up ? '区间上涨' : '区间下跌'}）
          </div>
        </div>

        {quote && (
          <div className="quote-grid">
            {cell('今开', quote.open)}
            {cell('最高', quote.high)}
            {cell('最低', quote.low)}
            {cell('昨收', quote.prevClose)}
            {cell('换手率', quote.turnoverRate != null ? quote.turnoverRate + '%' : null)}
            {cell('市盈率', quote.pe)}
            {cell('市净率', quote.pb)}
            {cell('总市值', quote.marketCap != null ? quote.marketCap + ' 亿' : null)}
          </div>
        )}
      </div>

      <div className="detail-posts">
        <div className="card">
          <h3>相关讨论（{posts.length}）</h3>
          {posts.length === 0 ? <div className="meta">暂无相关帖子</div> :
            posts.map((p) => <PostCard key={p.id} post={p} />)}
        </div>
      </div>
    </div>
  )
}
