/** 相对时间格式化（基于 ISO 字符串） */
export function timeAgo(iso) {
  const t = new Date(iso).getTime()
  if (isNaN(t)) return ''
  const diff = (Date.now() - t) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  return Math.floor(diff / 86400) + ' 天前'
}

/** 涨跌幅文本，带正负号 */
export function fmtChange(pct) {
  if (pct == null) return ''
  const n = typeof pct === 'string' ? parseFloat(pct) : pct
  const sign = n > 0 ? '+' : ''
  return sign + n.toFixed(2) + '%'
}
