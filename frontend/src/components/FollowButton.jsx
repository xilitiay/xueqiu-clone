import { useState } from 'react'
import { followUser } from '../api/client.js'

/**
 * 关注 / 已关注 按钮。点击后乐观更新，并调用后端 /api/users/{id}/follow 落库；
 * 后端不可用时由 client 本地切换（演示模式）。查看自己主页时通过 disabled 隐藏。
 */
export default function FollowButton({ userId, isFollowing: initial = false, disabled = false }) {
  const [following, setFollowing] = useState(initial)
  const [busy, setBusy] = useState(false)

  const onClick = async () => {
    if (busy || disabled) return
    setBusy(true)
    const prev = following
    setFollowing(!prev) // 乐观更新
    try {
      const r = await followUser(userId)
      if (typeof r.following === 'boolean') setFollowing(r.following)
    } catch {
      setFollowing(prev) // 失败回滚
    } finally {
      setBusy(false)
    }
  }

  return (
    <button
      className={following ? 'follow-btn following' : 'follow-btn'}
      disabled={disabled || busy}
      onClick={onClick}
    >
      {following ? '已关注' : '+ 关注'}
    </button>
  )
}
