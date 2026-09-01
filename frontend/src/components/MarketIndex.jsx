import { useState, useEffect } from 'react'
import { getIndices } from '../api/client.js'
import { subscribeQuote } from '../api/wsClient.js'
import { fmtChange } from './format.js'

export default function MarketIndex() {
  const [list, setList] = useState([])

  useEffect(() => {
    getIndices().then(setList).catch(() => {})
    // 侧栏指数随实时推送跳动（后端不可用时仅保留 REST 初始值）
    return subscribeQuote('/topic/indices', (idx) => {
      if (Array.isArray(idx) && idx.length) setList(idx)
    })
  }, [])

  if (!list.length) return null

  return (
    <div className="card">
      <h3>📊 市场指数</h3>
      {list.map((i) => {
        const code = i.code || i.symbol
        const value = i.value != null ? i.value : i.price
        const cls = i.changePercent > 0 ? 'up' : i.changePercent < 0 ? 'down' : ''
        return (
          <div className="stock-row" key={code} style={{ cursor: 'default' }}>
            <div>
              <div className="name">{i.name}</div>
              <div className="sym">{code}</div>
            </div>
            <div className="price">
              <div>{value}</div>
              <div className={`chg ${cls}`}>{fmtChange(i.changePercent)}</div>
            </div>
          </div>
        )
      })}
    </div>
  )
}
