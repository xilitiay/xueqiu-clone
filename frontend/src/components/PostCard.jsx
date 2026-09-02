import { memo, useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { timeAgo, fmtChange } from './format.js'
import CommentList from './CommentList.jsx'
import RichContent from './RichContent.jsx'
import { toggleLike, toggleFavorite } from '../api/client.js'

function PostCard({ post }) {
  const [liked, setLiked] = useState(!!post.liked)
  const [likeCount, setLikeCount] = useState(post.likeCount || 0)
  const [fav, setFav] = useState(!!post.favorited)
  const [showComments, setShowComments] = useState(false)
  const navigate = useNavigate()

  // 服务端返回的互动态变化时同步本地状态（如重新拉取信息流后）
  useEffect(() => { setLiked(!!post.liked); setLikeCount(post.likeCount || 0) }, [post.liked, post.likeCount])
  useEffect(() => { setFav(!!post.favorited) }, [post.favorited])

  const onLike = async () => {
    const cur = liked
    const curCount = likeCount
    // 乐观更新
    setLiked(!cur)
    setLikeCount(curCount + (cur ? -1 : 1))
    try {
      const res = await toggleLike(post.id, cur, curCount)
      setLiked(!!res.liked)
      setLikeCount(res.likeCount)
    } catch (e) { /* 忽略 */ }
  }

  const onFav = async () => {
    const cur = fav
    setFav(!cur)
    try {
      const res = await toggleFavorite(post.id)
      setFav(!!res.favorited)
    } catch (e) { /* 忽略 */ }
  }

  const chgClass = (p) => (p > 0 ? 'up' : p < 0 ? 'down' : '')
  const initial = (post.author?.name || '?').charAt(0)

  return (
    <div className="card post">
      <div className="post-head">
        <Link to={`/user/${post.author?.id}`} className="avatar" style={{ background: post.author?.avatarColor || '#E64340' }}>{initial}</Link>
        <div>
          <Link to={`/user/${post.author?.id}`} className="author">{post.author?.name}</Link>
          <div className="meta">{post.author?.followers?.toLocaleString()} 关注 · {timeAgo(post.createdAt)}</div>
        </div>
      </div>

      <div className="content clickable" onClick={() => navigate(`/post/${post.id}`)}>
        <RichContent text={post.content} stocks={post.stocks} />
      </div>

      {post.stocks?.length > 0 && (
        <div className="stocks">
          {post.stocks.map((s) => (
            <Link key={s.symbol} to={`/stock/${s.symbol}`} className="stock-tag" onClick={(e) => e.stopPropagation()}>
              {s.name}
              <span className={chgClass(s.changePercent)}>{fmtChange(s.changePercent)}</span>
            </Link>
          ))}
        </div>
      )}

      <div className="actions">
        <button className={liked ? 'liked' : ''} onClick={onLike}>♥ {likeCount}</button>
        <button onClick={() => setShowComments((v) => !v)}>💬 {post.commentCount || 0}</button>
        <button className={fav ? 'faved' : ''} onClick={onFav}>{fav ? '★ 已收藏' : '☆ 收藏'}</button>
      </div>

      {showComments && <CommentList postId={post.id} />}
    </div>
  )
}

export default memo(PostCard)
