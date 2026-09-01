import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { getUser, getFollowers, getFollowing, getCurrentUser } from '../api/client.js'
import PostCard from './PostCard.jsx'
import FollowButton from './FollowButton.jsx'
import { timeAgo } from './format.js'

function UserRow({ u }) {
  return (
    <div className="stock-row" key={u.id} style={{ cursor: 'default' }}>
      <Link to={`/user/${u.username || u.id}`} className="user-row-link">
        <div className="avatar" style={{ background: u.avatarColor || '#E64340' }}>
          {(u.name || '?').charAt(0)}
        </div>
        <div>
          <div className="name">{u.name}</div>
          <div className="sym">{u.bio ? u.bio.slice(0, 24) : '@' + (u.username || u.id)}</div>
        </div>
      </Link>
      <FollowButton userId={u.id} />
    </div>
  )
}

export default function UserPage() {
  const { id } = useParams()
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState('posts')
  const [list, setList] = useState([])
  const [listLoading, setListLoading] = useState(false)
  const me = getCurrentUser()

  useEffect(() => {
    let alive = true
    setLoading(true)
    getUser(id).then((u) => {
      if (!alive) return
      setUser(u)
      setLoading(false)
    }).catch(() => setLoading(false))
    return () => { alive = false }
  }, [id])

  // 切换「关注 / 粉丝」时加载列表
  useEffect(() => {
    if (tab === 'posts' || !user) { setList([]); return }
    let alive = true
    setListLoading(true)
    const fn = tab === 'following' ? getFollowing : getFollowers
    fn(user.id).then((l) => { if (alive) setList(l) }).catch(() => {}).finally(() => {
      if (alive) setListLoading(false)
    })
    return () => { alive = false }
  }, [tab, user])

  if (loading) return <div className="card" style={{ gridColumn: '1 / -1' }}>加载中…</div>
  if (!user) return <div className="card" style={{ gridColumn: '1 / -1' }}>未找到该用户</div>

  const isSelf = me && String(me.id) === String(user.id)
  const initial = (user.name || '?').charAt(0)

  return (
    <div style={{ gridColumn: '1 / -1' }}>
      <Link to="/" className="back">← 返回首页</Link>

      <div className="card user-hero">
        <div className="avatar lg" style={{ background: user.avatarColor || '#E64340' }}>{initial}</div>
        <div className="user-meta">
          <h2 style={{ margin: '0 0 4px' }}>{user.name}</h2>
          <div className="meta">{user.followers?.toLocaleString()} 粉丝 · {user.following} 关注 · {user.postCount} 帖子</div>
          {user.bio && <p className="bio">{user.bio}</p>}
        </div>
        {!isSelf && (
          <FollowButton userId={user.id} isFollowing={user.isFollowing} />
        )}
      </div>

      <div className="card">
        <div className="tabs">
          <button className={tab === 'posts' ? 'tab active' : 'tab'} onClick={() => setTab('posts')}>讨论</button>
          <button className={tab === 'following' ? 'tab active' : 'tab'} onClick={() => setTab('following')}>关注</button>
          <button className={tab === 'followers' ? 'tab active' : 'tab'} onClick={() => setTab('followers')}>粉丝</button>
        </div>

        {tab === 'posts' && (
          user.posts?.length ? (
            user.posts.map((p) => (
              <PostCard key={p.id} post={{ ...p, createdAt: p.createdAt || new Date().toISOString() }} />
            ))
          ) : <div className="meta">该用户还没有发布帖子</div>
        )}

        {tab !== 'posts' && (
          listLoading ? <div className="meta">加载中…</div>
            : list.length === 0 ? <div className="meta">暂无{user.following}数据</div>
              : list.map((u) => <UserRow key={u.id} u={u} />)
        )}
      </div>

      <div className="meta" style={{ fontSize: 12, textAlign: 'right' }}>
        数据更新于 {timeAgo(new Date().toISOString())}
      </div>
    </div>
  )
}
