import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getUser } from '../api/client.js'
import PostCard from './PostCard.jsx'
import { timeAgo } from './format.js'

export default function UserPage() {
  const { id } = useParams()
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [following, setFollowing] = useState(false)

  useEffect(() => {
    let alive = true
    getUser(id).then((u) => {
      if (!alive) return
      setUser(u)
      setLoading(false)
    }).catch(() => setLoading(false))
    return () => { alive = false }
  }, [id])

  if (loading) return <div className="card" style={{ gridColumn: '1 / -1' }}>加载中…</div>
  if (!user) return <div className="card" style={{ gridColumn: '1 / -1' }}>未找到该用户</div>

  const initial = (user.name || '?').charAt(0)

  return (
    <div style={{ gridColumn: '1 / -1' }}>
      <Link to="/" className="back">← 返回首页</Link>

      <div className="card user-hero">
        <div className="avatar lg" style={{ background: user.avatarColor || '#E64340' }}>{initial}</div>
        <div className="user-meta">
          <h2 style={{ margin: '0 0 4px' }}>{user.name}</h2>
          <div className="meta">{user.followers?.toLocaleString()} 粉丝 · {user.postCount} 帖子</div>
          {user.bio && <p className="bio">{user.bio}</p>}
        </div>
        <button
          className={following ? 'follow-btn following' : 'follow-btn'}
          onClick={() => setFollowing((v) => !v)}
        >
          {following ? '已关注' : '+ 关注'}
        </button>
      </div>

      <div className="card">
        <h3>他的讨论（{user.posts?.length || 0}）</h3>
        {!user.posts || user.posts.length === 0 ? (
          <div className="meta">该用户还没有发布帖子</div>
        ) : (
          user.posts.map((p) => (
            <PostCard key={p.id} post={{ ...p, createdAt: p.createdAt || new Date().toISOString() }} />
          ))
        )}
      </div>

      <div className="meta" style={{ fontSize: 12, textAlign: 'right' }}>
        数据更新于 {timeAgo(new Date().toISOString())}
      </div>
    </div>
  )
}
