import { useState, useEffect, useCallback } from 'react'
import { getComments, createComment } from '../api/client.js'

const SIZE = 20

export default function CommentList({ postId }) {
  const [list, setList] = useState([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [draft, setDraft] = useState('')
  const [posting, setPosting] = useState(false)

  const load = useCallback((p) => {
    setLoadingMore(true)
    getComments(postId, p, SIZE).then((data) => {
      setList((prev) => (p === 0 ? data : [...prev, ...data]))
      setPage(p)
      setHasMore(data.length === SIZE)
      setLoading(false)
      setLoadingMore(false)
    }).catch(() => { setLoading(false); setLoadingMore(false) })
  }, [postId])

  useEffect(() => { load(0) }, [load])

  const onLoadMore = () => { if (!loadingMore && hasMore) load(page + 1) }

  const onSend = async () => {
    const text = draft.trim()
    if (!text || posting) return
    setPosting(true)
    try {
      const c = await createComment(postId, text)
      setList((prev) => [...prev, c])
      setDraft('')
    } finally {
      setPosting(false)
    }
  }

  if (loading) return <div className="comments">加载评论…</div>

  return (
    <div className="comments">
      <div className="comment-form">
        <input
          className="comment-input"
          placeholder="写下你的评论…"
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') onSend() }}
        />
        <button className="btn-primary sm" disabled={!draft.trim() || posting} onClick={onSend}>
          {posting ? '发送中' : '发送'}
        </button>
      </div>

      {list.length === 0 && <div className="meta" style={{ padding: '8px 0' }}>暂无评论，来抢沙发</div>}

      {list.map((c) => (
        <div className="comment" key={c.id}>
          <span className="c-author">{c.authorName}</span>
          <span className="meta"> · {new Date(c.createdAt).toLocaleString('zh-CN')}</span>
          <div>{c.content}</div>
        </div>
      ))}

      {!loading && hasMore && (
        <button className="load-more sm" onClick={onLoadMore} disabled={loadingMore}>
          {loadingMore ? '加载中…' : '查看更多评论'}
        </button>
      )}
    </div>
  )
}
