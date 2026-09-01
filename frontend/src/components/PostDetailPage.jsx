import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getPost, toggleLike } from '../api/client.js'
import CommentList from './CommentList.jsx'
import RichContent from './RichContent.jsx'
import { timeAgo, fmtChange } from './format.js'

export default function PostDetailPage() {
  const { id } = useParams()
  const [post, setPost] = useState(null)
  const [loading, setLoading] = useState(true)
  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(0)

  useEffect(() => {
    let alive = true
    getPost(id).then((p) => {
      if (!alive) return
      setPost(p)
      setLiked(!!p.liked)
      setLikeCount(p.likeCount || 0)
      setLoading(false)
    }).catch(() => setLoading(false))
    return () => { alive = false }
  }, [id])

  if (loading) return (
    <div className="card skeleton-card" style={{ gridColumn: '1 / -1' }}>
      <div className="post-head">
        <div className="skeleton-avatar" />
        <div style={{ flex: 1 }}>
          <div className="skeleton-bar" style={{ width: '40%' }} />
          <div className="skeleton-bar" style={{ width: '25%', marginTop: 6 }} />
        </div>
      </div>
      <div className="skeleton-bar" style={{ width: '95%', marginTop: 12 }} />
      <div className="skeleton-bar" style={{ width: '85%', marginTop: 8 }} />
      <div className="skeleton-bar" style={{ width: '70%', marginTop: 8 }} />
    </div>
  )
  if (!post) return <div className="card" style={{ gridColumn: '1 / -1' }}>未找到该帖子</div>

  const onLike = async () => {
    const cur = liked
    const curCount = likeCount
    setLiked(!cur)
    setLikeCount(curCount + (cur ? -1 : 1))
    try {
      const res = await toggleLike(post.id, cur, curCount)
      setLiked(!!res.liked)
      setLikeCount(res.likeCount)
    } catch (e) { /* ignore */ }
  }

  const chgClass = (p) => (p > 0 ? 'up' : p < 0 ? 'down' : '')
  const initial = (post.author?.name || '?').charAt(0)

  return (
    <div style={{ gridColumn: '1 / -1' }}>
      <Link to="/" className="back">← 返回首页</Link>

      <div className="card post">
        <div className="post-head">
          <Link to={`/user/${post.author?.id}`} className="avatar" style={{ background: post.author?.avatarColor || '#E64340' }}>{initial}</Link>
          <div>
            <Link to={`/user/${post.author?.id}`} className="author">{post.author?.name}</Link>
            <div className="meta">{post.author?.followers?.toLocaleString()} 关注 · {timeAgo(post.createdAt)}</div>
          </div>
        </div>

        <div className="content" style={{ fontSize: 16 }}>
          <RichContent text={post.content} stocks={post.stocks} />
        </div>

        {post.stocks?.length > 0 && (
          <div className="stocks">
            {post.stocks.map((s) => (
              <Link key={s.symbol} to={`/stock/${s.symbol}`} className="stock-tag">
                {s.name}
                <span className={chgClass(s.changePercent)}>{fmtChange(s.changePercent)}</span>
              </Link>
            ))}
          </div>
        )}

        <div className="actions">
          <button className={liked ? 'liked' : ''} onClick={onLike}>♥ {likeCount}</button>
          <span>💬 {post.commentCount || 0}</span>
          <button>↗ 转发</button>
        </div>

        <CommentList postId={post.id} />
      </div>
    </div>
  )
}
