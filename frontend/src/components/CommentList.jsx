import { useState, useEffect, useCallback } from 'react'
import { getComments, createComment, toggleCommentLike } from '../api/client.js'

/**
 * 单条评论（顶层 / 回复通用）。
 * replyTarget 形如 { itemId, topId, authorName }：itemId 决定回复框挂在谁下面，
 * topId 才是提交用的 parentId（回复的回复归入同一顶层，保持单层嵌套）。
 */
function CommentItem({ c, depth = 0, topId, onLike, replyTarget, setReplyTarget, replyDraft, setReplyDraft, onSendReply }) {
  const time = new Date(c.createdAt).toLocaleString('zh-CN')
  const replying = replyTarget?.itemId === c.id

  const startReply = () => {
    if (replying) { setReplyTarget(null); return }
    setReplyTarget({ itemId: c.id, topId, authorName: c.authorName })
    setReplyDraft(`回复 @${c.authorName} `)
  }

  return (
    <div className={`comment ${depth > 0 ? 'reply' : ''}`}>
      <div className="c-head">
        <span className="c-author">{c.authorName}</span>
        <span className="meta"> · {time}</span>
      </div>
      <div className="c-content">{c.content}</div>
      <div className="c-actions">
        <button className={c.liked ? 'liked' : ''} onClick={() => onLike(c)}>
          ♥ {c.likeCount || 0}
        </button>
        <button onClick={startReply}>💬 回复</button>
      </div>

      {replying && (
        <div className="comment-form reply-form">
          <input
            className="comment-input"
            placeholder="写下你的回复…"
            value={replyDraft}
            onChange={(e) => setReplyDraft(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') onSendReply(topId) }}
          />
          <button className="btn-primary sm" disabled={!replyDraft.trim()} onClick={() => onSendReply(topId)}>
            发送
          </button>
        </div>
      )}
    </div>
  )
}

export default function CommentList({ postId }) {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [draft, setDraft] = useState('')
  const [posting, setPosting] = useState(false)
  const [replyTarget, setReplyTarget] = useState(null)
  const [replyDraft, setReplyDraft] = useState('')

  const load = useCallback(() => {
    getComments(postId).then((data) => {
      setList(Array.isArray(data) ? data : [])
      setLoading(false)
    }).catch(() => setLoading(false))
  }, [postId])

  useEffect(() => { load() }, [load])

  /** 更新树中某条评论（点赞后原地刷新） */
  const patch = (id, updater) => {
    setList((prev) => prev.map((t) => {
      if (t.id === id) return { ...t, ...updater(t) }
      if (t.replies?.some((r) => r.id === id)) {
        return { ...t, replies: t.replies.map((r) => (r.id === id ? { ...r, ...updater(r) } : r)) }
      }
      return t
    }))
  }

  const onLike = async (c) => {
    const curLiked = !!c.liked
    const curCount = c.likeCount || 0
    // 乐观更新
    patch(c.id, () => ({ liked: !curLiked, likeCount: curCount + (curLiked ? -1 : 1) }))
    try {
      const res = await toggleCommentLike(c.id, curLiked, curCount)
      patch(c.id, () => ({ liked: !!res.liked, likeCount: res.likeCount }))
    } catch (e) { /* 忽略 */ }
  }

  const onSend = async () => {
    const text = draft.trim()
    if (!text || posting) return
    setPosting(true)
    try {
      const c = await createComment(postId, text, null)
      setList((prev) => [...prev, c])
      setDraft('')
    } finally {
      setPosting(false)
    }
  }

  /** topId 为所属顶层评论 id：回复与「回复的回复」都挂在它下面 */
  const onSendReply = async (topId) => {
    const text = replyDraft.trim()
    if (!text) return
    try {
      const c = await createComment(postId, text, topId)
      setList((prev) => prev.map((t) =>
        t.id === topId ? { ...t, replies: [...(t.replies || []), c] } : t))
      setReplyDraft('')
      setReplyTarget(null)
    } catch (e) { /* 忽略 */ }
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
        <div className="comment-group" key={c.id}>
          <CommentItem
            c={c}
            depth={0}
            topId={c.id}
            onLike={onLike}
            replyTarget={replyTarget}
            setReplyTarget={setReplyTarget}
            replyDraft={replyDraft}
            setReplyDraft={setReplyDraft}
            onSendReply={onSendReply}
          />
          {c.replies?.length > 0 && (
            <div className="replies">
              {c.replies.map((r) => (
                <CommentItem
                  key={r.id}
                  c={r}
                  depth={1}
                  topId={c.id}
                  onLike={onLike}
                  replyTarget={replyTarget}
                  setReplyTarget={setReplyTarget}
                  replyDraft={replyDraft}
                  setReplyDraft={setReplyDraft}
                  onSendReply={onSendReply}
                />
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}
