import { useState, useEffect, useRef, useCallback } from 'react'
import { getFeed, getHotStocks, createPost } from '../api/client.js'
import PostCard from './PostCard.jsx'
import StockRow from './StockRow.jsx'
import MarketIndex from './MarketIndex.jsx'

const PAGE_SIZE = 5
const CASHTAG_RE = /\$([^\s$%，。、！？；：,!?;:]+)/g

/** 从草稿中解析 $代码/$名称，匹配热门股票列表得到关联股票代码 */
function extractSymbols(text, hotList) {
  const found = []
  const seen = new Set()
  let m
  CASHTAG_RE.lastIndex = 0
  while ((m = CASHTAG_RE.exec(text))) {
    const token = m[1]
    const hit = hotList.find((s) => s.name === token || s.symbol === token)
    if (hit && !seen.has(hit.symbol)) {
      seen.add(hit.symbol)
      found.push(hit)
    }
  }
  return found
}

export default function FeedPage() {
  const [posts, setPosts] = useState([])
  const [hot, setHot] = useState([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [draft, setDraft] = useState('')
  const [posting, setPosting] = useState(false)
  const sentinelRef = useRef(null)

  // 首屏加载
  useEffect(() => {
    let alive = true
    Promise.all([getFeed(0, PAGE_SIZE), getHotStocks(8)]).then(([f, h]) => {
      if (!alive) return
      setPosts(f)
      setHot(h)
      setHasMore(f.length === PAGE_SIZE)
      setLoading(false)
    })
    return () => { alive = false }
  }, [])

  const linkedStocks = hot.length ? extractSymbols(draft, hot) : []

  // 发帖
  const onPublish = async () => {
    const text = draft.trim()
    if (!text || posting) return
    setPosting(true)
    try {
      const created = await createPost(text, linkedStocks.map((s) => s.symbol))
      setPosts((prev) => [created, ...prev])
      setDraft('')
    } finally {
      setPosting(false)
    }
  }

  // 加载下一页（无限滚动触发）
  const loadMore = useCallback(() => {
    if (loadingMore || !hasMore) return
    setLoadingMore(true)
    const next = page + 1
    getFeed(next, PAGE_SIZE).then((more) => {
      setPosts((prev) => [...prev, ...more])
      setPage(next)
      setHasMore(more.length === PAGE_SIZE)
      setLoadingMore(false)
    }).catch(() => setLoadingMore(false))
  }, [page, loadingMore, hasMore])

  // 哨兵进入视口即加载（提前 200px 触发）
  useEffect(() => {
    const el = sentinelRef.current
    if (!el) return
    const io = new IntersectionObserver(
      (entries) => { if (entries[0].isIntersecting) loadMore() },
      { rootMargin: '200px' }
    )
    io.observe(el)
    return () => io.disconnect()
  }, [loadMore])

  return (
    <>
      <div>
        <div className="banner">演示数据 · 内容为示意性市场评论，非真实行情，不构成投资建议</div>

        {/* 发帖框 */}
        <div className="card compose">
          <textarea
            className="compose-input"
            placeholder="分享你的市场观点…（输入 $代码 或 $名称 可关联股票，演示账号自动登录直接发布）"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            rows={3}
          />
          {linkedStocks.length > 0 && (
            <div className="linked-chips">
              {linkedStocks.map((s) => (
                <span key={s.symbol} className="linked-chip">${s.name}</span>
              ))}
            </div>
          )}
          <div className="compose-actions">
            <span className="meta">支持 $代码 形式关联股票</span>
            <button className="btn-primary" disabled={!draft.trim() || posting} onClick={onPublish}>
              {posting ? '发布中…' : '发布'}
            </button>
          </div>
        </div>

        {loading ? <FeedSkeleton /> :
          posts.map((p) => <PostCard key={p.id} post={p} />)}

        {/* 无限滚动哨兵：进入视口自动加载下一页 */}
        {!loading && hasMore && (
          <div ref={sentinelRef} className="sentinel">
            {loadingMore ? <Spinner /> : ''}
          </div>
        )}
        {!loading && !hasMore && posts.length > 0 && (
          <div className="end-hint">—— 没有更多了 ——</div>
        )}
      </div>

      <aside className="side">
        <div className="card">
          <h3>🔥 热门股票</h3>
          {hot.map((s) => <StockRow key={s.symbol} stock={s} />)}
        </div>
        <MarketIndex />
        <div className="card">
          <h3>关于本演示</h3>
          <p className="meta" style={{ fontSize: 12 }}>
            前端 React + Vite，后端 Java/Spring Boot（见 backend 目录）。
            数据为本地 Mock，后端未启动时自动回退，UI 可直接预览。
          </p>
        </div>
      </aside>
    </>
  )
}

function Spinner() {
  return <span className="spinner" aria-label="加载中" />
}

function FeedSkeleton() {
  return (
    <>
      {[0, 1, 2].map((i) => (
        <div className="card post skeleton-card" key={i}>
          <div className="post-head">
            <div className="skeleton-avatar" />
            <div style={{ flex: 1 }}>
              <div className="skeleton-bar" style={{ width: '40%' }} />
              <div className="skeleton-bar" style={{ width: '25%', marginTop: 6 }} />
            </div>
          </div>
          <div className="skeleton-bar" style={{ width: '95%', marginTop: 12 }} />
          <div className="skeleton-bar" style={{ width: '80%', marginTop: 8 }} />
          <div className="skeleton-bar" style={{ width: '60%', marginTop: 8 }} />
        </div>
      ))}
    </>
  )
}
