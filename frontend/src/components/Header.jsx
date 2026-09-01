import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { getHotStocks } from '../api/client.js'

export default function Header() {
  const [all, setAll] = useState([])
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const wrapRef = useRef(null)

  useEffect(() => {
    getHotStocks(50).then(setAll).catch(() => {})
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
        <a href="#">自选</a>
      </nav>
    </header>
  )
}
