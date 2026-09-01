import { useState, useEffect } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { search } from '../api/client.js'
import { fmtChange } from './format.js'

/** 把关键词在文本中高亮（不区分大小写，避免正则注入用 split+indexOf） */
function highlight(text, kw) {
  if (!text || !kw) return text
  const lower = text.toLowerCase()
  const k = kw.toLowerCase()
  const nodes = []
  let i = 0
  let idx
  while ((idx = lower.indexOf(k, i)) !== -1) {
    if (idx > i) nodes.push(text.slice(i, idx))
    nodes.push(<mark key={idx} className="hl">{text.slice(idx, idx + kw.length)}</mark>)
    i = idx + kw.length
  }
  if (i < text.length) nodes.push(text.slice(i))
  return nodes
}

export default function SearchPage() {
  const [params, setParams] = useSearchParams()
  const q = params.get('q') || ''
  const [input, setInput] = useState(q)
  const [type, setType] = useState('all')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setInput(q)
    if (!q.trim()) { setResult({ posts: [], stocks: [], users: [] }); setLoading(false); return }
    setLoading(true)
    const t = setTimeout(() => {
      search(q, type).then((r) => { setResult(r); setLoading(false) }).catch(() => setLoading(false))
    }, 200)
    return () => clearTimeout(t)
  }, [q, type])

  const tabs = [['all', '综合'], ['post', '帖子'], ['stock', '股票'], ['user', '用户']]

  return (
    <div>
      <div className="card">
        <form className="search-page-bar" onSubmit={(e) => { e.preventDefault(); setParams({ q: input.trim() }) }}>
          <input
            className="search"
            autoFocus
            placeholder="搜索帖子、股票、用户"
            value={input}
            onChange={(e) => setInput(e.target.value)}
          />
          <button className="btn-primary" type="submit">搜索</button>
        </form>
        <div className="tabs">
          {tabs.map(([t, label]) => (
            <span
              key={t}
              className={type === t ? 'tab active' : 'tab'}
              onClick={() => setType(t)}
            >{label}</span>
          ))}
        </div>
      </div>

      {loading && <SearchSkeleton />}

      {!loading && result && (
        <>
          {type === 'all' || type === 'post' ? (
            <Section title="帖子">
              {result.posts.length === 0 ? <Empty kw={q} what="帖子" /> :
                result.posts.map((p) => (
                  <Link key={p.id} to={`/post/${p.id}`} className="card search-post">
                    <div className="author">{p.author?.name}</div>
                    <div className="content">{highlight(p.content, q)}</div>
                  </Link>
                ))}
            </Section>
          ) : null}

          {type === 'all' || type === 'stock' ? (
            <Section title="股票">
              {result.stocks.length === 0 ? <Empty kw={q} what="股票" /> :
                result.stocks.map((s) => {
                  const cls = s.changePercent > 0 ? 'up' : s.changePercent < 0 ? 'down' : ''
                  return (
                    <Link key={s.symbol} to={`/stock/${s.symbol}`} className="card stock-row">
                      <div><div className="name">{highlight(s.name, q)}</div><div className="sym">{s.symbol}</div></div>
                      <div className={`chg ${cls}`}>{fmtChange(s.changePercent)}</div>
                    </Link>
                  )
                })}
            </Section>
          ) : null}

          {type === 'all' || type === 'user' ? (
            <Section title="用户">
              {result.users.length === 0 ? <Empty kw={q} what="用户" /> :
                result.users.map((u) => (
                  <Link key={u.id} to={`/user/${u.id}`} className="card stock-row">
                    <div className="avatar" style={{ background: u.avatarColor || '#E64340' }}>
                      {(u.name || '?').charAt(0)}
                    </div>
                    <div><div className="name">{highlight(u.name, q)}</div><div className="meta">{u.followers} 关注</div></div>
                  </Link>
                ))}
            </Section>
          ) : null}
        </>
      )}

      {!loading && result && type === 'all' &&
        result.posts.length === 0 && result.stocks.length === 0 && result.users.length === 0 && (
        <Empty kw={q} what="相关内容" />
      )}
    </div>
  )
}

function Section({ title, children }) {
  return (
    <div style={{ marginTop: 12 }}>
      <h3 style={{ margin: '12px 4px 6px', fontSize: 14, color: '#888' }}>{title}</h3>
      {children}
    </div>
  )
}

function Empty({ kw, what }) {
  return <div className="meta" style={{ padding: '10px 4px' }}>没有找到与「{kw}」相关的{what}</div>
}

function SearchSkeleton() {
  return (
    <div className="card skeleton-card" style={{ marginTop: 12 }}>
      <div className="skeleton-bar" style={{ width: '70%' }} />
      <div className="skeleton-bar" style={{ width: '90%', marginTop: 10 }} />
      <div className="skeleton-bar" style={{ width: '50%', marginTop: 10 }} />
    </div>
  )
}
