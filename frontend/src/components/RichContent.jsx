import { Link } from 'react-router-dom'

// 匹配正文中的 $代码 / $名称（雪球标志性 cashtag 写法）
const CASHTAG_RE = /\$([^\s$%，。、！？；：,!?;:]+)/g

/**
 * 富文本正文：把 $贵州茅台 / $SH600519 这类 cashtag 渲染为可点击的个股链接。
 * 仅当 token 能匹配到该帖关联的股票时才跳转，否则仅做高亮样式。
 * 换行与纯文本段原样保留（依赖祖先元素的 white-space: pre-wrap）。
 */
export default function RichContent({ text, stocks = [] }) {
  const byName = {}
  const bySymbol = {}
  stocks.forEach((s) => {
    if (s.name) byName[s.name] = s.symbol
    if (s.symbol) bySymbol[s.symbol] = s.symbol
  })

  const parts = (text || '').split(CASHTAG_RE)
  return (
    <span className="rich-content">
      {parts.map((seg, i) => {
        if (i % 2 === 0) return <span key={i}>{seg}</span>
        const sym = byName[seg] || bySymbol[seg] || null
        return sym ? (
          <Link key={i} to={`/stock/${sym}`} className="cashtag">${seg}</Link>
        ) : (
          <span key={i} className="cashtag">${seg}</span>
        )
      })}
    </span>
  )
}
